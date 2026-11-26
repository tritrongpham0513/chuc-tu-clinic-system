package com.chuctupk.service;

import com.chuctupk.dto.response.ReportSummaryResponse;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.TimeSlotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@DisplayName("Sprint 13 — ReportService Integration & Query Budget Tests")
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private Clock clock;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private Patient createPatient(String suffix) {
        return patientRepository.save(Patient.builder()
                .patientCode("TEST-BN-" + suffix)
                .fullName("Bệnh nhân Test " + suffix)
                .phone("090" + String.format("%07d", Math.abs(suffix.hashCode()) % 10_000_000))
                .gender(Gender.MALE)
                .isActive(true)
                .build());
    }

    private Appointment createAppointment(Patient patient, Doctor doctor, TimeSlot slot, LocalDate date, AppointmentStatus status) {
        return Appointment.builder()
                .bookingCode("TEST-" + UUID.randomUUID().toString().substring(0, 18))
                .patient(patient)
                .doctor(doctor)
                .timeSlot(slot)
                .appointmentDate(date)
                .source(AppointmentSource.WEB)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("1. from > to -> AppException 422")
    void testFromAfterToThrows422() {
        LocalDate from = LocalDate.of(2026, 11, 20);
        LocalDate to = LocalDate.of(2026, 11, 10);

        assertThatThrownBy(() -> reportService.getSummary(from, to))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getStatusCode()).isEqualTo(422);
                    assertThat(appEx.getMessage()).contains("Ngày bắt đầu phải trước ngày kết thúc");
                });
    }

    @Test
    @DisplayName("2. Khoảng 400 ngày -> AppException 422 tối đa 366 ngày")
    void testRangeOver366DaysThrows422() {
        LocalDate from = LocalDate.of(2025, 10, 1);
        LocalDate to = from.plusDays(400);

        assertThatThrownBy(() -> reportService.getSummary(from, to))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getStatusCode()).isEqualTo(422);
                    assertThat(appEx.getMessage()).contains("tối đa 366 ngày");
                });
    }

    @Test
    @DisplayName("3. totals.booked khớp countByAppointmentDate cộng tay trên toàn bộ kỳ")
    void testTotalsBookedMatchesManualCountByDate() {
        LocalDate baseDate = LocalDate.now(clock).minusDays(120);
        LocalDate from = baseDate;
        LocalDate to = baseDate.plusDays(6);

        Doctor doctor = doctorRepository.findAll().stream().findFirst().orElse(null);
        TimeSlot slot = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc().get(0);
        Patient p1 = createPatient("M1");
        Patient p2 = createPatient("M2");

        appointmentRepository.saveAll(List.of(
                createAppointment(p1, doctor, slot, baseDate, AppointmentStatus.COMPLETED),
                createAppointment(p2, doctor, slot, baseDate, AppointmentStatus.PENDING),
                createAppointment(p1, null, slot, baseDate.plusDays(2), AppointmentStatus.CONFIRMED),
                createAppointment(p2, doctor, slot, baseDate.plusDays(5), AppointmentStatus.CANCELLED)
        ));
        entityManager.flush();
        entityManager.clear();

        ReportSummaryResponse summary = reportService.getSummary(from, to);

        long manualTotal = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            manualTotal += appointmentRepository.countByAppointmentDate(d);
        }

        assertThat(summary.totals().booked()).isEqualTo((int) manualTotal);
        assertThat(summary.byDoctor())
                .anyMatch(doc -> doc.doctorId() == null && "Chưa gán".equals(doc.doctorName()));
    }

    @Test
    @DisplayName("4 & 5. NO_SHOW tách riêng khỏi CANCELLED và byDay đủ mọi ngày trong kỳ (kể cả ngày 0 phiếu)")
    void testNoShowSeparateFromCancelledAndFullByDayRange() {
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 2, 5); // 5 ngày

        Doctor doctor = doctorRepository.findAll().stream().findFirst().orElse(null);
        TimeSlot slot = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc().get(0);
        Patient p1 = createPatient("NS1");
        Patient p2 = createPatient("NS2");

        appointmentRepository.saveAll(List.of(
                createAppointment(p1, doctor, slot, from, AppointmentStatus.CANCELLED),
                createAppointment(p2, doctor, slot, from, AppointmentStatus.NO_SHOW)
        ));
        entityManager.flush();
        entityManager.clear();

        ReportSummaryResponse summary = reportService.getSummary(from, to);

        assertThat(summary.totals().cancelled()).isGreaterThanOrEqualTo(1);
        assertThat(summary.totals().noShow()).isGreaterThanOrEqualTo(1);
        // Hai ngày 2024-02-01 có đúng 1 CANCELLED và 1 NO_SHOW riêng biệt
        ReportSummaryResponse.DayItem day1 = summary.byDay().get(0);
        assertThat(day1.date()).isEqualTo("2024-02-01");
        assertThat(day1.cancelled()).isEqualTo(1);
        assertThat(day1.noShow()).isEqualTo(1);
        assertThat(day1.booked()).isEqualTo(2);

        // byDay chứa đủ 5 ngày kể cả các ngày 0 phiếu
        assertThat(summary.byDay()).hasSize(5);
        assertThat(summary.byDay().get(1).date()).isEqualTo("2024-02-02");
        assertThat(summary.byDay().get(1).booked()).isEqualTo(0);
    }

    @Test
    @DisplayName("6. Kỳ 31 ngày có >= 500 phiếu -> <= 10 query (Hibernate Statistics)")
    void test31DayPeriodWith500AppointmentsUsesAtMost10Queries() {
        LocalDate from = LocalDate.of(2023, 5, 1);
        LocalDate to = from.plusDays(30); // 31 ngày

        Doctor doctor = doctorRepository.findAll().stream().findFirst().orElse(null);
        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        assertThat(slots).isNotEmpty();

        List<Patient> patients = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            patients.add(createPatient("PERF" + i));
        }

        List<Appointment> batch = new ArrayList<>(527);
        AppointmentStatus[] statuses = {
                AppointmentStatus.COMPLETED,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.CANCELLED,
                AppointmentStatus.NO_SHOW,
                AppointmentStatus.PENDING
        };
        for (int dayOffset = 0; dayOffset < 31; dayOffset++) {
            LocalDate date = from.plusDays(dayOffset);
            for (int j = 0; j < 17; j++) { // 31 * 17 = 527 phiếu >= 500
                Patient p = patients.get((dayOffset + j) % patients.size());
                TimeSlot ts = slots.get(j % slots.size());
                AppointmentStatus st = statuses[(dayOffset + j) % statuses.length];
                batch.add(createAppointment(p, j % 5 == 0 ? null : doctor, ts, date, st));
            }
        }
        appointmentRepository.saveAll(batch);
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        ReportSummaryResponse summary = reportService.getSummary(from, to);
        long queryCount = stats.getPrepareStatementCount();

        System.out.println("[Sprint 13] Kỳ 31 ngày (>=500 phiếu) số query đo được = " + queryCount);

        assertThat(summary.totals().booked()).isGreaterThanOrEqualTo(500);
        assertThat(summary.byDay()).hasSize(31);
        assertThat(queryCount).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("8. Export CSV thực tế: có BOM UTF-8, ';', ngày dd/MM/yyyy, không có cột tiền/CCCD")
    void testExportCsvRealOutput() {
        LocalDate from = LocalDate.of(2024, 3, 1);
        LocalDate to = LocalDate.of(2024, 3, 3);

        Doctor doctor = doctorRepository.findAll().stream().findFirst().orElse(null);
        TimeSlot slot = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc().get(0);
        Patient p = createPatient("CSV1");
        appointmentRepository.save(createAppointment(p, doctor, slot, from, AppointmentStatus.COMPLETED));
        entityManager.flush();
        entityManager.clear();

        ReportService.CsvExportResult csv = reportService.exportCsv(from, to, "summary");
        byte[] bytes = csv.content();
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);

        String text = new String(bytes, StandardCharsets.UTF_8);
        assertThat(text).startsWith("\uFEFFNgày;Lượt đặt;Lượt khám;Đã huỷ;Không đến\r\n");
        assertThat(text).contains("01/03/2024;");
        assertThat(text.toLowerCase()).doesNotContain("doanh thu");
        assertThat(text.toLowerCase()).doesNotContain("tiền");
        assertThat(text).doesNotContain("CCCD");
    }

    @Test
    @DisplayName("9. Bệnh nhân mới & quay lại chỉ tính trên lượt khám COMPLETED, loại trừ CANCELLED/NO_SHOW")
    void testNewAndReturningPatients_OnlyCountsCompletedVisits() {
        LocalDate from = LocalDate.of(2022, 6, 10);
        LocalDate to = LocalDate.of(2022, 6, 16);

        Doctor doctor = doctorRepository.findAll().stream().findFirst().orElse(null);
        TimeSlot slot = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc().get(0);

        Patient newCompleted = createPatient("NR_NEW");
        Patient returningCompleted = createPatient("NR_RET");
        Patient cancelledOnlyInPeriod = createPatient("NR_CANC");
        Patient noShowAfterPriorVisit = createPatient("NR_NS");
        Patient priorCancelThenCompleted = createPatient("NR_PC_COMP");

        appointmentRepository.saveAll(List.of(
                // 1. Lần khám COMPLETED đầu tiên trong kỳ -> Bệnh nhân mới
                createAppointment(newCompleted, doctor, slot, from, AppointmentStatus.COMPLETED),
                // 2. Có COMPLETED trước kỳ và COMPLETED trong kỳ -> Bệnh nhân quay lại
                createAppointment(returningCompleted, doctor, slot, from.minusDays(10), AppointmentStatus.COMPLETED),
                createAppointment(returningCompleted, doctor, slot, from.plusDays(1), AppointmentStatus.COMPLETED),
                // 3. Chỉ huỷ trong kỳ -> KHÔNG tính mới hay quay lại
                createAppointment(cancelledOnlyInPeriod, doctor, slot, from.plusDays(2), AppointmentStatus.CANCELLED),
                // 4. Có khám trước kỳ nhưng trong kỳ chỉ NO_SHOW -> KHÔNG tính quay lại
                createAppointment(noShowAfterPriorVisit, doctor, slot, from.minusDays(15), AppointmentStatus.COMPLETED),
                createAppointment(noShowAfterPriorVisit, doctor, slot, from.plusDays(3), AppointmentStatus.NO_SHOW),
                // 5. Trước kỳ chỉ CANCELLED, trong kỳ lần đầu COMPLETED -> Bệnh nhân mới
                createAppointment(priorCancelThenCompleted, doctor, slot, from.minusDays(5), AppointmentStatus.CANCELLED),
                createAppointment(priorCancelThenCompleted, doctor, slot, from.plusDays(4), AppointmentStatus.COMPLETED)
        ));
        entityManager.flush();
        entityManager.clear();

        ReportSummaryResponse summary = reportService.getSummary(from, to);

        assertThat(summary.totals().completed()).isEqualTo(3);
        assertThat(summary.totals().newPatients()).isEqualTo(2);
        assertThat(summary.totals().returningPatients()).isEqualTo(1);
    }

    @Test
    @DisplayName("10. Spring Security filter chain + @PreAuthorize: STAFF gọi /admin/reports/** -> 403 Forbidden")
    void testSecurityChain_StaffForbiddenOnReportEndpoints() throws Exception {
        MockMvc securedMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        User staffUser = User.builder()
                .id(999L)
                .username("staff_test")
                .fullName("Nhân viên Tiếp tân")
                .role(Role.STAFF)
                .isActive(true)
                .build();
        UsernamePasswordAuthenticationToken staffAuth = new UsernamePasswordAuthenticationToken(
                staffUser, null, List.of(new SimpleGrantedAuthority("ROLE_STAFF")));

        securedMvc.perform(get("/api/v1/admin/reports/summary").with(authentication(staffAuth)))
                .andExpect(status().isForbidden());

        securedMvc.perform(get("/api/v1/admin/reports/export").with(authentication(staffAuth)))
                .andExpect(status().isForbidden());
    }
}
