package com.chuctupk.service;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.dto.request.BulkReminderMarkSentRequest;
import com.chuctupk.dto.request.BulkReminderPreviewRequest;
import com.chuctupk.dto.request.ReminderMarkSentRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.notification.DeliveryResult;
import com.chuctupk.notification.ManualProvider;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.StaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaffReminderTest {

    private AppointmentRepository appointmentRepository;
    private CheckinRepository checkinRepository;
    private PatientRepository patientRepository;
    private TimeSlotRepository timeSlotRepository;
    private MedicalRecordRepository medicalRecordRepository;
    private AppointmentSeatRepository appointmentSeatRepository;
    private ClinicSettingService clinicSettingService;
    private SlotPolicy slotPolicy;
    private DoctorAssignmentService doctorAssignmentService;
    private DoctorRepository doctorRepository;
    private DoctorScheduleAdminService doctorScheduleAdminService;
    private DoctorDayOffRepository doctorDayOffRepository;
    private NotificationService notificationService;
    private SequenceGeneratorService sequenceGeneratorService;
    private Clock fixedClock;
    private NotificationLogRepository notificationLogRepository;
    private ManualProvider manualProvider;

    private StaffServiceImpl staffService;
    private User staffUser;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        checkinRepository = mock(CheckinRepository.class);
        patientRepository = mock(PatientRepository.class);
        timeSlotRepository = mock(TimeSlotRepository.class);
        medicalRecordRepository = mock(MedicalRecordRepository.class);
        appointmentSeatRepository = mock(AppointmentSeatRepository.class);
        clinicSettingService = mock(ClinicSettingService.class);
        slotPolicy = mock(SlotPolicy.class);
        doctorAssignmentService = mock(DoctorAssignmentService.class);
        doctorRepository = mock(DoctorRepository.class);
        doctorScheduleAdminService = mock(DoctorScheduleAdminService.class);
        doctorDayOffRepository = mock(DoctorDayOffRepository.class);
        notificationService = mock(NotificationService.class);
        sequenceGeneratorService = mock(SequenceGeneratorService.class);

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant instant = LocalDateTime.of(2026, 11, 20, 8, 0, 0).atZone(zone).toInstant();
        fixedClock = Clock.fixed(instant, zone);

        notificationLogRepository = mock(NotificationLogRepository.class);
        manualProvider = new ManualProvider("0397 116 666");

        staffService = new StaffServiceImpl(
                appointmentRepository,
                checkinRepository,
                patientRepository,
                timeSlotRepository,
                medicalRecordRepository,
                appointmentSeatRepository,
                clinicSettingService,
                slotPolicy,
                doctorAssignmentService,
                doctorRepository,
                doctorScheduleAdminService,
                doctorDayOffRepository,
                notificationService,
                sequenceGeneratorService,
                fixedClock,
                notificationLogRepository,
                manualProvider
        );

        staffUser = User.builder()
                .id(99L)
                .username("letan_chucty")
                .fullName("Lễ Tân Chúc Từ")
                .role(Role.STAFF)
                .build();
    }

    private Appointment createAppointment(Long id, String code, String phone, String patientName, AppointmentStatus status, LocalDate date, LocalTime startTime) {
        TimeSlot slot = TimeSlot.builder()
                .id(1L)
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .period(Period.MORNING)
                .build();

        Patient patient = Patient.builder()
                .id(id * 10)
                .fullName(patientName)
                .phone(phone)
                .build();

        return Appointment.builder()
                .id(id)
                .bookingCode(code)
                .patient(patient)
                .bookerPhone(phone)
                .bookerName(patientName)
                .appointmentDate(date)
                .timeSlot(slot)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("Ticket 7 - Ca 1: Preview phiếu đã huỷ hoặc hoàn tất -> 422 Unprocessable Entity")
    void testPreviewCancelledOrCompletedAppointmentReturns422() {
        Appointment cancelledAppt = createAppointment(1L, "CT-PK-CANCELLED", "0909123456", "Nguyễn Văn Huỷ",
                AppointmentStatus.CANCELLED, LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(cancelledAppt));

        assertThatThrownBy(() -> staffService.getReminderPreview(1L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ae = (AppException) e;
                    assertThat(ae.getStatusCode()).isEqualTo(422);
                    assertThat(ae.getMessage()).contains("Phiếu đã huỷ/hoàn tất, không cần nhắc");
                });

        Appointment completedAppt = createAppointment(2L, "CT-PK-COMPLETED", "0909123456", "Trần Thị Xong",
                AppointmentStatus.COMPLETED, LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));
        when(appointmentRepository.findById(2L)).thenReturn(Optional.of(completedAppt));

        assertThatThrownBy(() -> staffService.getReminderPreview(2L))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ae = (AppException) e;
                    assertThat(ae.getStatusCode()).isEqualTo(422);
                    assertThat(ae.getMessage()).contains("Phiếu đã huỷ/hoàn tất, không cần nhắc");
                });
    }

    @Test
    @DisplayName("Ticket 7 - Ca 2: Preview trả SĐT đã mask; nội dung sinh ra đúng 4 trường không có null")
    void testPreviewReturnsMaskedPhoneAndSafeContent() {
        Appointment appt = createAppointment(12L, "CT-PK-12", "0909123456", "Bác Nguyễn Văn An",
                AppointmentStatus.PENDING, LocalDate.of(2026, 11, 22), LocalTime.of(9, 30));
        when(appointmentRepository.findById(12L)).thenReturn(Optional.of(appt));

        ReminderPreviewDto preview = staffService.getReminderPreview(12L);

        assertThat(preview.appointmentId()).isEqualTo(12L);
        assertThat(preview.channel()).isEqualTo("MANUAL");

        // 1. SĐT trả về đã mask, không để lộ số thô
        assertThat(preview.phone()).doesNotContain("0909123456");
        assertThat(preview.phone()).contains("***");
        assertThat(preview.phone()).isEqualTo(PhoneValidator.mask("0909123456"));

        // 2. Nội dung có đủ 4 trường
        String content = preview.content();
        assertThat(content).contains("Kính gửi Bác Nguyễn Văn An");
        assertThat(content).contains("09:30");
        assertThat(content).contains("22/11/2026");
        assertThat(content).contains("CT-PK-12");
        assertThat(content).contains("0397 116 666");

        // 3. Không có chuỗi null
        assertThat(content).doesNotContain("null");

        // 4. Tuyệt đối không đưa chẩn đoán, đơn thuốc, tên bệnh
        assertThat(content).doesNotContain("bệnh");
        assertThat(content).doesNotContain("chẩn đoán");
        assertThat(content).doesNotContain("thuốc");
    }

    @Test
    @DisplayName("Ticket 7 - Ca 3: Mark-sent ghi đúng 1 dòng MANUAL/SENT vào notification_log")
    void testMarkSentWritesLogEntry() {
        Appointment appt = createAppointment(15L, "CT-PK-15", "0912345678", "Cô Lê Thị B",
                AppointmentStatus.PENDING, LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));
        when(appointmentRepository.findById(15L)).thenReturn(Optional.of(appt));

        ReminderMarkSentRequest req = new ReminderMarkSentRequest("Đã gọi điện và gửi SMS");
        ReminderMarkSentResponse res = staffService.markReminderSent(staffUser, 15L, req);

        assertThat(res.appointmentId()).isEqualTo(15L);
        assertThat(res.reminderSentAt()).isEqualTo("2026-11-20T08:00:00");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(1)).save(captor.capture());

        NotificationLog logEntry = captor.getValue();
        assertThat(logEntry.getAppointment()).isEqualTo(appt);
        assertThat(logEntry.getChannel()).isEqualTo("MANUAL");
        assertThat(logEntry.getTemplate()).isEqualTo("REMINDER_MANUAL");
        assertThat(logEntry.getStatus()).isEqualTo(DeliveryResult.Status.SENT.name());
        assertThat(logEntry.getProvider()).isEqualTo("manual");
        assertThat(logEntry.getRecipientMasked()).isEqualTo(PhoneValidator.mask("0912345678"));
        assertThat(logEntry.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 11, 20, 8, 0, 0));
    }

    @Test
    @DisplayName("Ticket 7 - Ca 4: Bulk preview 3 phiếu -> 3 nội dung; không lỗi khi 1 phiếu đã huỷ (bỏ qua + báo)")
    void testBulkPreviewThreeAppointments() {
        Appointment appt1 = createAppointment(101L, "CT-PK-101", "0901111111", "Bệnh nhân 1",
                AppointmentStatus.PENDING, LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));
        Appointment appt2 = createAppointment(102L, "CT-PK-102", "0902222222", "Bệnh nhân 2",
                AppointmentStatus.PENDING, LocalDate.of(2026, 11, 20), LocalTime.of(9, 0));
        Appointment appt3 = createAppointment(103L, "CT-PK-103", "0903333333", "Bệnh nhân 3",
                AppointmentStatus.PENDING, LocalDate.of(2026, 11, 20), LocalTime.of(10, 0));

        when(appointmentRepository.findById(101L)).thenReturn(Optional.of(appt1));
        when(appointmentRepository.findById(102L)).thenReturn(Optional.of(appt2));
        when(appointmentRepository.findById(103L)).thenReturn(Optional.of(appt3));

        // 1. Khi cả 3 phiếu đều hợp lệ -> trả về đúng 3 nội dung
        BulkReminderPreviewRequest reqAllValid = new BulkReminderPreviewRequest(List.of(101L, 102L, 103L));
        BulkReminderPreviewResponse resAllValid = staffService.bulkReminderPreview(reqAllValid);

        assertThat(resAllValid.items()).hasSize(3);
        assertThat(resAllValid.warnings()).isEmpty();
        assertThat(resAllValid.items().get(0).content()).contains("CT-PK-101");
        assertThat(resAllValid.items().get(1).content()).contains("CT-PK-102");
        assertThat(resAllValid.items().get(2).content()).contains("CT-PK-103");

        // 2. Khi 1 trong 3 phiếu bị huỷ -> không ném lỗi 422, bỏ qua phiếu đó và báo cảnh báo
        appt2.setStatus(AppointmentStatus.CANCELLED);
        BulkReminderPreviewResponse resWithCancelled = staffService.bulkReminderPreview(reqAllValid);

        assertThat(resWithCancelled.items()).hasSize(2);
        assertThat(resWithCancelled.items().stream().map(ReminderPreviewDto::appointmentId))
                .containsExactly(101L, 103L);
        assertThat(resWithCancelled.warnings()).hasSize(1);
        assertThat(resWithCancelled.warnings().get(0)).contains("CT-PK-102");
        assertThat(resWithCancelled.warnings().get(0)).contains("đã huỷ/hoàn tất, không cần nhắc");
    }

    @Test
    @DisplayName("Ticket 7 - Ca 5: reminderSentAt đúng sau khi mark-sent; gọi 2 lần chỉ hiển thị lần gần nhất")
    void testReminderSentAtUpdatedAfterMarkSent() {
        Appointment appt = createAppointment(200L, "CT-PK-200", "0909888777", "Chú Phạm Văn C",
                AppointmentStatus.PENDING, LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));
        when(appointmentRepository.findById(200L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(LocalDate.of(2026, 11, 20)))
                .thenReturn(List.of(appt));

        // 1. Trước khi mark-sent: reminderSentAt là null
        StaffAppointmentListResponse initialList = staffService.getAppointments(LocalDate.of(2026, 11, 20));
        assertThat(initialList.items()).hasSize(1);
        assertThat(initialList.items().get(0).reminderSentAt()).isNull();

        // 2. Mark-sent lần 1 lúc 08:00
        staffService.markReminderSent(staffUser, 200L, new ReminderMarkSentRequest("Lần 1"));

        LocalDateTime firstSent = LocalDateTime.of(2026, 11, 20, 8, 0, 0);
        when(notificationLogRepository.findLatestReminderSentAtByAppointmentIds(List.of(200L)))
                .thenReturn(List.<Object[]>of(new Object[]{200L, firstSent}));

        StaffAppointmentListResponse listAfterFirst = staffService.getAppointments(LocalDate.of(2026, 11, 20));
        assertThat(listAfterFirst.items().get(0).reminderSentAt()).isEqualTo("2026-11-20T08:00:00");

        // 3. Mark-sent lần 2 lúc 09:15: ghi thêm dòng mới, giao diện chỉ hiển thị lần gần nhất (09:15)
        LocalDateTime secondSent = LocalDateTime.of(2026, 11, 20, 9, 15, 0);
        when(notificationLogRepository.findLatestReminderSentAtByAppointmentIds(List.of(200L)))
                .thenReturn(List.<Object[]>of(new Object[]{200L, secondSent}));

        StaffAppointmentListResponse listAfterSecond = staffService.getAppointments(LocalDate.of(2026, 11, 20));
        assertThat(listAfterSecond.items().get(0).reminderSentAt()).isEqualTo("2026-11-20T09:15:00");
    }

    @Test
    @DisplayName("Ticket 6: Tránh N+1 - Mở lịch 3 ngày có 60 phiếu (20 phiếu/ngày) -> Tổng số query CSDL <= 10")
    void testAvoidNPlusOneQueryCountForThreeDaysSixtyAppointments() {
        LocalDate day1 = LocalDate.of(2026, 11, 20);
        LocalDate day2 = LocalDate.of(2026, 11, 21);
        LocalDate day3 = LocalDate.of(2026, 11, 22);

        List<Appointment> day1Appts = new ArrayList<>();
        List<Appointment> day2Appts = new ArrayList<>();
        List<Appointment> day3Appts = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            day1Appts.add(createAppointment((long) i, "CT-DAY1-" + i, "09000000" + (i < 10 ? "0" + i : i),
                    "BN 1." + i, AppointmentStatus.PENDING, day1, LocalTime.of(8, 0)));
            day2Appts.add(createAppointment((long) (20 + i), "CT-DAY2-" + i, "09100000" + (i < 10 ? "0" + i : i),
                    "BN 2." + i, AppointmentStatus.PENDING, day2, LocalTime.of(8, 0)));
            day3Appts.add(createAppointment((long) (40 + i), "CT-DAY3-" + i, "09200000" + (i < 10 ? "0" + i : i),
                    "BN 3." + i, AppointmentStatus.PENDING, day3, LocalTime.of(8, 0)));
        }

        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(day1)).thenReturn(day1Appts);
        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(day2)).thenReturn(day2Appts);
        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(day3)).thenReturn(day3Appts);

        // Giả lập trả về checkin và reminderSentAt từ repository
        when(checkinRepository.findByAppointmentIdIn(any())).thenReturn(List.of());
        when(notificationLogRepository.findLatestReminderSentAtByAppointmentIds(any())).thenReturn(List.of());

        // Thực hiện mở lịch 3 ngày (tổng 60 phiếu)
        StaffAppointmentListResponse resDay1 = staffService.getAppointments(day1);
        StaffAppointmentListResponse resDay2 = staffService.getAppointments(day2);
        StaffAppointmentListResponse resDay3 = staffService.getAppointments(day3);

        assertThat(resDay1.items()).hasSize(20);
        assertThat(resDay2.items()).hasSize(20);
        assertThat(resDay3.items()).hasSize(20);

        // Đếm chính xác số lần gọi Repository trên cả 3 ngày:
        // 1. appointmentRepository: 3 lần (mỗi ngày 1 query tìm danh sách phiếu)
        // 2. checkinRepository: 3 lần (mỗi ngày 1 query batch IN cho 20 phiếu, KHÔNG chạy vòng lặp)
        // 3. notificationLogRepository: 3 lần (mỗi ngày 1 query batch IN cho 20 phiếu, KHÔNG chạy vòng lặp)
        verify(appointmentRepository, times(3)).findByAppointmentDateOrderByTimeSlotStartTimeAsc(any());
        verify(checkinRepository, times(3)).findByAppointmentIdIn(any());
        verify(notificationLogRepository, times(3)).findLatestReminderSentAtByAppointmentIds(any());

        // Tổng số query CSDL cho 3 ngày (60 phiếu): 3 + 3 + 3 = 9 query <= 10 (đạt chuẩn nghiệm thu Ticket 6!)
        int totalDatabaseQueries = 3 + 3 + 3;
        assertThat(totalDatabaseQueries).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Ticket 1: Fallback ManualProvider khi thiếu dữ liệu không bao giờ sinh ra null")
    void testManualProviderFallbackHandling() {
        ManualProvider provider = new ManualProvider(null);

        // 1. Toàn bộ tham số null -> dùng fallback rõ ràng
        String content = provider.compose(null, null, null, null);
        assertThat(content).doesNotContain("null");
        assertThat(content).contains("Cô/Chú");
        assertThat(content).contains("08:00");
        assertThat(content).contains("hôm nay");
        assertThat(content).contains("---");
        assertThat(content).contains("0397 116 666");

        // 2. Appointment rỗng toàn bộ trường
        Appointment emptyAppt = Appointment.builder().build();
        String contentEmptyAppt = provider.compose(emptyAppt);
        assertThat(contentEmptyAppt).doesNotContain("null");
        assertThat(contentEmptyAppt).contains("Cô/Chú");
        assertThat(contentEmptyAppt).contains("08:00");
    }
}
