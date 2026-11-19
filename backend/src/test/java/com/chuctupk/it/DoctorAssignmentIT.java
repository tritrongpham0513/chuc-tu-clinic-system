package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.DoctorAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DoctorAssignmentIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DoctorAssignmentService doctorAssignmentService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private DoctorDayOffRepository doctorDayOffRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User adminUser;
    private User staffUser;
    private Doctor doctorA;
    private Doctor doctorB;
    private TimeSlot morningSlot;
    private Patient patient;

    @BeforeEach
    void setUp() {
        // Tạo Users
        adminUser = userRepository.save(User.builder()
                .username("admintest_" + System.currentTimeMillis())
                .phone("0900000001")
                .fullName("Quản trị viên Test")
                .passwordHash("DummyHash123")
                .role(Role.ADMIN)
                .isActive(true)
                .mustChangePassword(false)
                .build());

        staffUser = userRepository.save(User.builder()
                .username("stafftest_" + System.currentTimeMillis())
                .phone("0900000002")
                .fullName("Nhân viên Test")
                .passwordHash("DummyHash123")
                .role(Role.STAFF)
                .isActive(true)
                .mustChangePassword(false)
                .build());

        User docUserA = userRepository.save(User.builder()
                .username("docAtest_" + System.currentTimeMillis())
                .phone("0900000003")
                .fullName("Bác sĩ A")
                .passwordHash("DummyHash123")
                .role(Role.DOCTOR)
                .isActive(true)
                .build());

        User docUserB = userRepository.save(User.builder()
                .username("docBtest_" + System.currentTimeMillis())
                .phone("0900000004")
                .fullName("Bác sĩ B")
                .passwordHash("DummyHash123")
                .role(Role.DOCTOR)
                .isActive(true)
                .build());

        doctorA = doctorRepository.save(Doctor.builder()
                .user(docUserA)
                .fullName("Bác sĩ A")
                .phone("0900000003")
                .specialty("Y học cổ truyền")
                .isActive(true)
                .sortOrder(1)
                .build());

        doctorB = doctorRepository.save(Doctor.builder()
                .user(docUserB)
                .fullName("Bác sĩ B")
                .phone("0900000004")
                .specialty("Châm cứu")
                .isActive(true)
                .sortOrder(2)
                .build());

        List<TimeSlot> slots = timeSlotRepository.findAll();
        morningSlot = slots.stream()
                .filter(s -> s.getPeriod() == Period.MORNING)
                .findFirst()
                .orElseGet(() -> timeSlotRepository.save(TimeSlot.builder()
                        .slotLabel("08:00 - 09:00")
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(9, 0))
                        .period(Period.MORNING)
                        .maxPatients(5)
                        .isActive(true)
                        .sortOrder(1)
                        .build()));

        patient = patientRepository.save(Patient.builder()
                .patientCode("CT-BN-26-9999")
                .user(adminUser)
                .fullName("Bệnh nhân Test")
                .phone("0988888888")
                .gender(Gender.MALE)
                .yearOfBirth(1960)
                .isPrimary(true)
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("A2-1: Không có bác sĩ trực -> ném lỗi 422 kèm câu tiếng Việt giản dị")
    void testNoDoctorScheduled_Throws422() {
        // Ngày Chủ nhật không có lịch
        LocalDate sundayDate = LocalDate.now().plusDays(1);
        while (sundayDate.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
            sundayDate = sundayDate.plusDays(1);
        }

        final LocalDate testDate = sundayDate;
        AppException ex = assertThrows(AppException.class, () ->
                doctorAssignmentService.pickDoctor(testDate, morningSlot)
        );

        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("chưa có bác sĩ trực"));
    }

    @Test
    @DisplayName("A2-2: Bác sĩ xin nghỉ phép trong ngày -> không được phân công")
    void testDoctorDayOff_NotPicked() {
        LocalDate mondayDate = LocalDate.now().plusDays(1);
        while (mondayDate.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            mondayDate = mondayDate.plusDays(1);
        }

        // Đăng ký lịch trực cho DocA vào thứ Hai buổi sáng
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctorA)
                .dayOfWeek(DayOfWeek.MON)
                .period(Period.MORNING)
                .isActive(true)
                .build());

        // DocA xin nghỉ phép ngày hôm đó
        doctorDayOffRepository.save(DoctorDayOff.builder()
                .doctor(doctorA)
                .offDate(mondayDate)
                .period(Period.MORNING)
                .reason("Nghỉ phép")
                .build());

        // Vô hiệu hoá mọi lịch trực khác để bài test không phụ thuộc dữ liệu seed
        for (DoctorSchedule other : doctorScheduleRepository.findAll()) {
            if (!other.getDoctor().getId().equals(doctorA.getId())
                    && Boolean.TRUE.equals(other.getIsActive())) {
                other.setIsActive(false);
            }
        }
        doctorScheduleRepository.flush();

        final LocalDate testDate = mondayDate;
        AppException ex = assertThrows(AppException.class, () ->
                doctorAssignmentService.pickDoctor(testDate, morningSlot)
        );

        assertEquals(422, ex.getStatusCode());
    }

    @Test
    @DisplayName("A2-3: Cân bằng tải: 2 bác sĩ trực -> chọn bác sĩ ít lịch khám hơn hoặc tie-break theo ID")
    void testLoadBalancing_PicksDoctorWithFewerAppointments() {
        LocalDate tuesdayDate = LocalDate.now().plusDays(1);
        while (tuesdayDate.getDayOfWeek() != java.time.DayOfWeek.TUESDAY) {
            tuesdayDate = tuesdayDate.plusDays(1);
        }

        // Cả 2 bác sĩ trực thứ Ba sáng
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctorA)
                .dayOfWeek(DayOfWeek.TUE)
                .period(Period.MORNING)
                .isActive(true)
                .build());

        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctorB)
                .dayOfWeek(DayOfWeek.TUE)
                .period(Period.MORNING)
                .isActive(true)
                .build());

        // Cho các bác sĩ khác nghỉ phép ngày này để cô lập kiểm thử giữa doctorA và doctorB
        final LocalDate targetTuesday = tuesdayDate;
        doctorRepository.findAll().stream()
                .filter(d -> !d.getId().equals(doctorA.getId()) && !d.getId().equals(doctorB.getId()))
                .forEach(od -> doctorDayOffRepository.save(DoctorDayOff.builder()
                        .doctor(od)
                        .offDate(targetTuesday)
                        .period(Period.FULL_DAY)
                        .reason("Nghỉ phép cô lập test")
                        .build()));

        // Khi cả 2 chưa có ai đặt: chọn Doctor có ID nhỏ hơn (doctorA.id < doctorB.id)
        Doctor picked1 = doctorAssignmentService.pickDoctor(tuesdayDate, morningSlot);
        Long smallerId = Math.min(doctorA.getId(), doctorB.getId());
        assertEquals(smallerId, picked1.getId());

        // Đặt 1 lịch cho bác sĩ vừa được chọn
        appointmentRepository.save(Appointment.builder()
                .bookingCode("PKCT-TEST-0001")
                .patient(patient)
                .doctor(picked1)
                .appointmentDate(tuesdayDate)
                .timeSlot(morningSlot)
                .source(AppointmentSource.WEB)
                .status(AppointmentStatus.PENDING)
                .build());

        // Lần tiếp theo phải chọn bác sĩ còn lại (số lịch = 0 < số lịch = 1)
        Doctor picked2 = doctorAssignmentService.pickDoctor(tuesdayDate, morningSlot);
        Long largerId = Math.max(doctorA.getId(), doctorB.getId());
        assertEquals(largerId, picked2.getId());
    }

    @Test
    @DisplayName("A2-4: Admin / Staff manual assignment endpoint PATCH")
    void testManualAssignDoctor_AdminAndStaff() throws Exception {
        LocalDate wedDate = LocalDate.now().plusDays(1);
        while (wedDate.getDayOfWeek() != java.time.DayOfWeek.WEDNESDAY) {
            wedDate = wedDate.plusDays(1);
        }

        // Đăng ký lịch cho doctorA thứ Tư sáng
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctorA)
                .dayOfWeek(DayOfWeek.WED)
                .period(Period.MORNING)
                .isActive(true)
                .build());

        Appointment appt = appointmentRepository.save(Appointment.builder()
                .bookingCode("PKCT-TEST-MANUAL")
                .patient(patient)
                .appointmentDate(wedDate)
                .timeSlot(morningSlot)
                .source(AppointmentSource.WEB)
                .status(AppointmentStatus.PENDING)
                .build());

        String adminToken = tokenProvider.generateToken(adminUser);

        // 1. Gán doctorA (hợp lệ) qua Admin PATCH
        mockMvc.perform(patch("/api/v1/admin/appointments/" + appt.getId() + "/doctor")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("doctorId", String.valueOf(doctorA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.doctorId").value(doctorA.getId()));

        Appointment updated = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(doctorA.getId(), updated.getDoctor().getId());

        // 2. Gán doctorB (không có lịch trực thứ Tư sáng) -> 422
        mockMvc.perform(patch("/api/v1/admin/appointments/" + appt.getId() + "/doctor")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("doctorId", String.valueOf(doctorB.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bác sĩ không có lịch trực trong ca khám này"));
    }
}
