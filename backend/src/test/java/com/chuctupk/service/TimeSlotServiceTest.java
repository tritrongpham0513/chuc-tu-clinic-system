package com.chuctupk.service;

import com.chuctupk.dto.request.AppointmentCreateRequest;
import com.chuctupk.dto.response.AppointmentResponse;
import com.chuctupk.dto.response.TimeSlotResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TimeSlotServiceTest {

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private com.chuctupk.service.impl.AppointmentServiceImpl appointmentServiceImpl;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private DoctorDayOffRepository doctorDayOffRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClinicSettingRepository clinicSettingRepository;

    @Autowired
    private Clock clock;

    private Doctor doctorA;
    private Doctor doctorB;
    private TimeSlot morningSlot;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now(clock).plusDays(4);
        DayOfWeek dow = DayOfWeek.from(testDate.getDayOfWeek());

        // Lấy hoặc tạo slot sáng
        morningSlot = timeSlotRepository.findAll().stream()
                .filter(s -> s.getPeriod() == Period.MORNING && s.getIsActive() && s.getMaxPatients() == 3)
                .findFirst()
                .orElseGet(() -> timeSlotRepository.save(TimeSlot.builder()
                        .slotLabel("08:00 - 08:30")
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(8, 30))
                        .period(Period.MORNING)
                        .maxPatients(3)
                        .sortOrder(1)
                        .isActive(true)
                        .build()));

        // Tạo Bác sĩ A
        User userA = userRepository.save(User.builder()
                .username("bs_slot_a_" + System.currentTimeMillis())
                .passwordHash("secret")
                .fullName("Bác Sĩ A")
                .phone("0981111111")
                .isActive(true)
                .build());
        doctorA = doctorRepository.save(Doctor.builder()
                .user(userA)
                .fullName("Bác Sĩ A")
                .specialty("Châm cứu trị liệu")
                .phone("0981111111")
                .isActive(true)
                .build());

        // Tạo Bác sĩ B
        User userB = userRepository.save(User.builder()
                .username("bs_slot_b_" + System.currentTimeMillis())
                .passwordHash("secret")
                .fullName("Bác Sĩ B")
                .phone("0982222222")
                .isActive(true)
                .build());
        doctorB = doctorRepository.save(Doctor.builder()
                .user(userB)
                .fullName("Bác Sĩ B")
                .specialty("Bắt mạch kê đơn")
                .phone("0982222222")
                .isActive(true)
                .build());

        // Xếp lịch cho BS A trực MORNING
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctorA)
                .dayOfWeek(dow)
                .period(Period.MORNING)
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("1. Lọc slot đã đầy: Khi slot đạt tối đa số bệnh nhân (remaining = 0), slot bị loại khỏi kết quả")
    void testFilterFullSlot() {
        Patient p1 = createPatient("0971000001", "Bệnh nhân 1");
        Patient p2 = createPatient("0971000002", "Bệnh nhân 2");
        Patient p3 = createPatient("0971000003", "Bệnh nhân 3");

        // Tạo 3 lịch khám cho morningSlot (đạt max 3 của BS A)
        bookAppointment(p1, doctorA, morningSlot, testDate);
        bookAppointment(p2, doctorA, morningSlot, testDate);
        bookAppointment(p3, doctorA, morningSlot, testDate);

        List<TimeSlotResponse> slots = timeSlotService.getAvailableTimeSlots(testDate, doctorA.getId());
        boolean containsMorningSlot = slots.stream().anyMatch(s -> s.getId().equals(morningSlot.getId()));
        assertFalse(containsMorningSlot, "Slot buổi sáng đã đầy 3 chỗ của BS A nên phải bị loại khỏi danh sách");
    }

    @Test
    @DisplayName("2. Bác sĩ nghỉ ca FULL_DAY: Khi có đơn nghỉ ca FULL_DAY, slot bị loại do không có bác sĩ trực")
    void testDoctorDayOff_WithFullDay() {
        // Đăng ký BS A nghỉ ngày testDate với ca FULL_DAY
        doctorDayOffRepository.save(DoctorDayOff.builder()
                .doctor(doctorA)
                .offDate(testDate)
                .period(Period.FULL_DAY)
                .reason("Nghỉ phép việc gia đình")
                .build());

        List<TimeSlotResponse> slots = timeSlotService.getAvailableTimeSlots(testDate, doctorA.getId());
        boolean containsSlot = slots.stream().anyMatch(s -> s.getId().equals(morningSlot.getId()));
        assertFalse(containsSlot, "Bác sĩ A nghỉ FULL_DAY thì slot phải bị loại");
    }

    @Test
    @DisplayName("3. Đếm chỗ trộn lịch (có chọn BS và không chọn BS): Kiểm tra thỏa mãn cả điều kiện (a) và (b)")
    void testMixedCapacityCounting_ConditionA_And_ConditionB() {
        DayOfWeek dow = DayOfWeek.from(testDate.getDayOfWeek());
        // Cho cả BS B cũng trực sáng -> Tổng sức chứa slot = 3 * 2 = 6
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctorB)
                .dayOfWeek(dow)
                .period(Period.MORNING)
                .isActive(true)
                .build());

        Patient p1 = createPatient("0972000001", "Bệnh nhân 1");
        Patient p2 = createPatient("0972000002", "Bệnh nhân 2");
        Patient p3 = createPatient("0972000003", "Bệnh nhân không chọn BS");

        // 2 lịch đặt đích danh BS A
        bookAppointment(p1, doctorA, morningSlot, testDate);
        bookAppointment(p2, doctorA, morningSlot, testDate);
        // 1 lịch KHÔNG chọn bác sĩ (doctor = null) -> tính vào tổng ca (a)
        bookAppointment(p3, null, morningSlot, testDate);

        // Tổng đã đặt: 3 (sức chứa tổng: 6). BS A đã có 2 lịch (sức chứa BS A: 3).
        // Tra cứu không chọn BS: remaining = 6 - 3 = 3
        List<TimeSlotResponse> generalSlots = timeSlotService.getAvailableTimeSlots(testDate, null);
        TimeSlotResponse generalSlot = generalSlots.stream()
                .filter(s -> s.getId().equals(morningSlot.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, generalSlot.getRemaining(), "Tổng sức chứa còn lại phải là 3 chỗ");

        // Tra cứu chọn BS A: BS A còn 3 - 2 = 1 chỗ
        List<TimeSlotResponse> doctorASlots = timeSlotService.getAvailableTimeSlots(testDate, doctorA.getId());
        TimeSlotResponse doctorASlot = doctorASlots.stream()
                .filter(s -> s.getId().equals(morningSlot.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, doctorASlot.getRemaining(), "Chỗ còn lại của riêng BS A phải là 1 chỗ");

        // Đặt thêm 1 lịch cho BS A -> BS A đủ 3 chỗ
        Patient p4 = createPatient("0972000004", "Bệnh nhân 4");
        bookAppointment(p4, doctorA, morningSlot, testDate);

        // BS A đã hết chỗ -> tra cứu theo BS A không còn slot
        List<TimeSlotResponse> doctorASlotsAfter = timeSlotService.getAvailableTimeSlots(testDate, doctorA.getId());
        assertFalse(doctorASlotsAfter.stream().anyMatch(s -> s.getId().equals(morningSlot.getId())),
                "BS A đã nhận đủ 3 chỗ nên slot phải bị loại khi tra cứu riêng BS A");

        // Nhưng tra cứu tổng thể: vẫn còn chỗ (cho BS B) -> remaining = 6 - 4 = 2 chỗ
        List<TimeSlotResponse> generalSlotsAfter = timeSlotService.getAvailableTimeSlots(testDate, null);
        TimeSlotResponse generalSlotAfter = generalSlotsAfter.stream()
                .filter(s -> s.getId().equals(morningSlot.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, generalSlotAfter.getRemaining(), "Khung giờ chung vẫn còn 2 chỗ cho bác sĩ B");
    }

    @Test
    @DisplayName("4. Hôm nay: Loại các slot bắt đầu trước (now + booking_min_hours)")
    void testToday_ExcludeSlotsBeforeCutoffTime() {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        DayOfWeek todayDow = DayOfWeek.from(today.getDayOfWeek());

        // Xếp lịch cho BS A trực hôm nay
        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(doctorA)
                .dayOfWeek(todayDow)
                .period(Period.FULL_DAY)
                .isActive(true)
                .build());

        List<TimeSlotResponse> todaySlots = timeSlotService.getAvailableTimeSlots(today, doctorA.getId());
        LocalTime cutoffTime = now.plusHours(2); // booking_min_hours = 2

        for (TimeSlotResponse slot : todaySlots) {
            assertFalse(slot.getStartTime().isBefore(cutoffTime),
                    "Slot " + slot.getSlotLabel() + " có giờ bắt đầu " + slot.getStartTime()
                    + " trước cutoff " + cutoffTime + " là vi phạm quy tắc đặt trước tối thiểu 2 giờ");
        }
    }

    @Test
    @DisplayName("5. Cả ngày không có bác sĩ trực: Trả về mảng rỗng với HTTP 200 (không ném lỗi 422)")
    void testWholeDayNoDoctors_ReturnsEmptyList200() {
        // Ngày khám xa (sau 6 ngày) không có xếp lịch nào
        LocalDate futureDate = LocalDate.now(clock).plusDays(6);
        List<TimeSlotResponse> slots = timeSlotService.getAvailableTimeSlots(futureDate, null);
        assertNotNull(slots);
        assertTrue(slots.isEmpty(), "Ngày không có bác sĩ trực nào phải trả về mảng rỗng");
    }

    @Test
    @DisplayName("6. POST đặt lịch vào slot không có bác sĩ trực: Phải nhận lỗi 422 Unprocessable Entity")
    void testPostIntoSlotWithoutDoctor_Throws422() {
        LocalDate futureDate = LocalDate.now(clock).plusDays(6);
        // futureDate không có bác sĩ nào trực ca sáng
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Bệnh nhân Đặt Slot Trống")
                .phone("0983333333")
                .yearOfBirth(1980)
                .gender(Gender.MALE)
                .appointmentDate(futureDate)
                .timeSlotId(morningSlot.getId())
                .build();

        AppException ex = assertThrows(AppException.class, () -> appointmentService.createAppointment(req));
        assertEquals(422, ex.getStatusCode(), "Đặt vào slot không có bác sĩ trực phải trả mã lỗi 422");
    }

    @Test
    @DisplayName("7. Chuyển năm bằng Clock (31/12 sang 01/01): Tự động tạo bộ đếm năm mới trong đoạn code đã khóa")
    void testYearRollover_PatientCodeGeneration() {
        // Mã cho năm 2026
        String code2026 = appointmentServiceImpl.generateNextPatientCode(2026);
        assertTrue(code2026.startsWith("BN-2026-"), "Mã bệnh nhân năm 2026 phải bắt đầu bằng BN-2026-");

        // Khi bước sang năm mới 2027 (bộ đếm patient_seq_2027 chưa hề tồn tại trong CSDL)
        String code2027_1 = appointmentServiceImpl.generateNextPatientCode(2027);
        assertEquals("BN-2027-0001", code2027_1, "Lần đầu tạo mã cho năm 2027 phải tự khởi tạo bộ đếm và trả về BN-2027-0001");

        // Gọi tiếp lần 2 trong năm 2027
        String code2027_2 = appointmentServiceImpl.generateNextPatientCode(2027);
        assertEquals("BN-2027-0002", code2027_2, "Lần tiếp theo trong năm 2027 phải tuần tự tăng lên BN-2027-0002");

        // Bước sang năm 2028
        String code2028 = appointmentServiceImpl.generateNextPatientCode(2028);
        assertEquals("BN-2028-0001", code2028, "Lần đầu năm 2028 phải tự khởi tạo bộ đếm và trả về BN-2028-0001");
    }

    @Test
    @DisplayName("8. Tự phục hồi Mutex: Khi xóa sạch sys_patient_seq_mutex và booking_advance_days, sinh mã năm 2030 vẫn trả về BN-2030-0001")
    void testSelfHealingMutex_WhenMutexAndSettingsDeleted() {
        // Xóa cả sys_patient_seq_mutex và booking_advance_days (hoặc bất kỳ setting nào)
        clinicSettingRepository.deleteById(AppointmentServiceImpl.KEY_PATIENT_SEQ_MUTEX);
        clinicSettingRepository.deleteById("booking_advance_days");
        clinicSettingRepository.deleteById("patient_seq_2030");
        clinicSettingRepository.flush();

        // Kiểm tra chắc chắn đã bị xóa khỏi DB
        assertFalse(clinicSettingRepository.existsById(AppointmentServiceImpl.KEY_PATIENT_SEQ_MUTEX));
        assertFalse(clinicSettingRepository.existsById("booking_advance_days"));

        // Gọi sinh mã năm mới 2030: hệ thống phải tự tái tạo mutex và sinh đúng BN-2030-0001
        String code2030_1 = appointmentServiceImpl.generateNextPatientCode(2030);
        assertEquals("BN-2030-0001", code2030_1, "Phải tự phục hồi mutex và sinh mã đầu tiên của năm 2030 là BN-2030-0001");

        // Gọi tiếp lần 2
        String code2030_2 = appointmentServiceImpl.generateNextPatientCode(2030);
        assertEquals("BN-2030-0002", code2030_2, "Lần 2 sinh tiếp BN-2030-0002");
    }

    private Patient createPatient(String phone, String name) {
        String suffix = phone.length() >= 4 ? phone.substring(phone.length() - 4) : "0000";
        return patientRepository.save(Patient.builder()
                .patientCode("BN-T" + (System.currentTimeMillis() % 100000) + suffix)
                .fullName(name)
                .phone(phone)
                .yearOfBirth(1970)
                .gender(Gender.MALE)
                .build());
    }

    private Appointment bookAppointment(Patient patient, Doctor doctor, TimeSlot slot, LocalDate date) {
        return appointmentRepository.save(Appointment.builder()
                .bookingCode("PKCT-" + (System.currentTimeMillis() % 100000000) + "-" + (int) (Math.random() * 9000 + 1000))
                .patient(patient)
                .doctor(doctor)
                .timeSlot(slot)
                .appointmentDate(date)
                .status(AppointmentStatus.PENDING)
                .source(AppointmentSource.WEB)
                .build());
    }
}
