package com.chuctupk.service;

import com.chuctupk.dto.request.AppointmentCancelRequest;
import com.chuctupk.dto.request.AppointmentCreateRequest;
import com.chuctupk.dto.response.AppointmentResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppointmentServiceTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    @Autowired
    private ClinicHolidayRepository clinicHolidayRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Clock clock;

    private Doctor testDoctor;
    private TimeSlot testSlotMorning;
    private TimeSlot testSlotAfternoon;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        // Ngày khám hợp lệ trong tương lai (sau 3 ngày)
        targetDate = LocalDate.now(clock).plusDays(3);
        DayOfWeek dow = DayOfWeek.from(targetDate.getDayOfWeek());

        // Tạo slot nếu chưa có
        testSlotMorning = timeSlotRepository.findAll().stream()
                .filter(s -> s.getPeriod() == Period.MORNING && s.getIsActive())
                .findFirst()
                .orElseGet(() -> timeSlotRepository.save(TimeSlot.builder()
                        .slotLabel("08:00 - 08:30")
                        .startTime(LocalTime.of(8, 0))
                        .endTime(LocalTime.of(8, 30))
                        .period(Period.MORNING)
                        .maxPatients(3)
                        .isActive(true)
                        .build()));

        testSlotAfternoon = timeSlotRepository.findAll().stream()
                .filter(s -> s.getPeriod() == Period.AFTERNOON && s.getIsActive())
                .findFirst()
                .orElseGet(() -> timeSlotRepository.save(TimeSlot.builder()
                        .slotLabel("14:00 - 14:30")
                        .startTime(LocalTime.of(14, 0))
                        .endTime(LocalTime.of(14, 30))
                        .period(Period.AFTERNOON)
                        .maxPatients(3)
                        .isActive(true)
                        .build()));

        // Tạo bác sĩ và xếp lịch cho ngày dow
        User doctorUser = userRepository.save(User.builder()
                .username("bs_test_" + System.currentTimeMillis())
                .passwordHash("secret")
                .fullName("Bác sĩ Nguyễn Văn A")
                .phone("0912345678")
                .isActive(true)
                .build());

        testDoctor = doctorRepository.save(Doctor.builder()
                .user(doctorUser)
                .fullName("BS. Nguyễn Văn A")
                .specialty("Y học cổ truyền")
                .phone("0912345678")
                .isActive(true)
                .build());

        doctorScheduleRepository.save(DoctorSchedule.builder()
                .doctor(testDoctor)
                .dayOfWeek(dow)
                .period(Period.FULL_DAY)
                .isActive(true)
                .build());
    }

    @Test
    @DisplayName("Đặt lịch thành công: tạo mã PKCT-... và tạo BN-YYYY-XXXX mới")
    void testCreateAppointment_Success() {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Trần Thị B")
                .phone("0988776655")
                .yearOfBirth(1960)
                .gender(Gender.FEMALE)
                .symptoms("Đau thắt lưng lan xuống chân")
                .appointmentDate(targetDate)
                .timeSlotId(testSlotMorning.getId())
                .doctorId(testDoctor.getId())
                .build();

        AppointmentResponse res = appointmentService.createAppointment(req);

        assertNotNull(res);
        assertNotNull(res.getBookingCode());
        assertTrue(res.getBookingCode().startsWith("PKCT-"));
        assertEquals("Trần Thị B", res.getPatientName());
        assertEquals(AppointmentStatus.PENDING, res.getStatus());
        assertEquals(targetDate, res.getAppointmentDate());
        assertEquals(testSlotMorning.getId(), res.getTimeSlot().getId());
        assertEquals(testDoctor.getFullName(), res.getDoctor().getFullName());
    }

    @Test
    @DisplayName("Chống trùng lịch 409: Bệnh nhân cùng ngày cùng khung giờ đã có lịch chưa hủy")
    void testCreateAppointment_ConflictSamePatientSameSlot() {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Trần Thị B")
                .phone("0988776655")
                .yearOfBirth(1960)
                .gender(Gender.FEMALE)
                .appointmentDate(targetDate)
                .timeSlotId(testSlotMorning.getId())
                .doctorId(testDoctor.getId())
                .build();

        appointmentService.createAppointment(req);

        // Đặt lại đúng slot đó
        AppException ex = assertThrows(AppException.class, () -> appointmentService.createAppointment(req));
        assertEquals(409, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("đã có lịch hẹn"));
    }

    @Test
    @DisplayName("Hết chỗ 409: Slot đã đủ max_patients của bác sĩ")
    void testCreateAppointment_SlotFull() {
        for (int i = 1; i <= testSlotMorning.getMaxPatients(); i++) {
            AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                    .fullName("Bệnh nhân số " + i)
                    .phone("091234560" + i)
                    .yearOfBirth(1950 + i)
                    .gender(Gender.MALE)
                    .appointmentDate(targetDate)
                    .timeSlotId(testSlotMorning.getId())
                    .doctorId(testDoctor.getId())
                    .build();
            appointmentService.createAppointment(req);
        }

        // Người thứ max_patients + 1 đặt tiếp -> 409 Conflict
        AppointmentCreateRequest reqOver = AppointmentCreateRequest.builder()
                .fullName("Bệnh nhân vượt quá")
                .phone("0912345699")
                .yearOfBirth(1955)
                .gender(Gender.MALE)
                .appointmentDate(targetDate)
                .timeSlotId(testSlotMorning.getId())
                .doctorId(testDoctor.getId())
                .build();

        AppException ex = assertThrows(AppException.class, () -> appointmentService.createAppointment(reqOver));
        assertEquals(409, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("đã đầy"));
    }

    @Test
    @DisplayName("Nghiệp vụ 422: Đặt lịch vào ngày quá khứ")
    void testCreateAppointment_PastDate() {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Bệnh nhân quá khứ")
                .phone("0912345678")
                .yearOfBirth(1970)
                .gender(Gender.MALE)
                .appointmentDate(LocalDate.now(clock).minusDays(1))
                .timeSlotId(testSlotMorning.getId())
                .build();

        AppException ex = assertThrows(AppException.class, () -> appointmentService.createAppointment(req));
        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("quá khứ"));
    }

    @Test
    @DisplayName("Nghiệp vụ 422: Đặt lịch vào ngày nghỉ lễ")
    void testCreateAppointment_Holiday() {
        LocalDate holidayDate = targetDate.plusDays(1);
        clinicHolidayRepository.save(ClinicHoliday.builder()
                .holidayDate(holidayDate)
                .name("Nghỉ lễ thử nghiệm")
                .isClosed(true)
                .build());

        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Bệnh nhân nghỉ lễ")
                .phone("0912345678")
                .yearOfBirth(1970)
                .gender(Gender.MALE)
                .appointmentDate(holidayDate)
                .timeSlotId(testSlotMorning.getId())
                .build();

        AppException ex = assertThrows(AppException.class, () -> appointmentService.createAppointment(req));
        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("nghỉ lễ"));
    }

    @Test
    @DisplayName("Hủy lịch thành công khi đủ điều kiện: lưu bền vững CANCELLED và ghi 2 bản ghi status_history")
    void testCancelAppointment_Success() {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Phạm Văn C")
                .phone("0933445566")
                .yearOfBirth(1965)
                .gender(Gender.MALE)
                .appointmentDate(targetDate)
                .timeSlotId(testSlotMorning.getId())
                .doctorId(testDoctor.getId())
                .build();

        AppointmentResponse created = appointmentService.createAppointment(req);

        AppointmentCancelRequest cancelReq = AppointmentCancelRequest.builder()
                .status("CANCELLED")
                .cancelReason("Bận việc đột xuất")
                .build();

        AppointmentResponse cancelled = appointmentService.cancelAppointment(
                created.getBookingCode(), "0933445566", cancelReq);

        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());

        // Kiểm tra lưu bền vững trong CSDL
        Appointment reloaded = appointmentRepository.findByBookingCode(created.getBookingCode()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, reloaded.getStatus(), "Trạng thái trong CSDL phải là CANCELLED");
        assertEquals("Bận việc đột xuất", reloaded.getCancelReason(), "Lý do hủy trong CSDL phải được cập nhật");

        // Kiểm tra đúng 2 bản ghi lịch sử trạng thái: PENDING -> CANCELLED
        var historyList = statusHistoryRepository.findAll().stream()
                .filter(h -> h.getAppointment().getId().equals(reloaded.getId()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();

        assertEquals(2, historyList.size(), "Phải có đúng 2 bản ghi lịch sử thay đổi trạng thái");
        assertNull(historyList.get(0).getOldStatus());
        assertEquals("PENDING", historyList.get(0).getNewStatus());

        assertEquals("PENDING", historyList.get(1).getOldStatus());
        assertEquals("CANCELLED", historyList.get(1).getNewStatus());
        assertEquals("Bận việc đột xuất", historyList.get(1).getReason());
    }

    @Test
    @DisplayName("Hủy lịch thất bại 409: Cố hủy lịch đã ở trạng thái CANCELLED trả thông báo tiếng Việt thuần")
    void testCancelAppointment_AlreadyCancelled() {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Phạm Văn C")
                .phone("0933445566")
                .yearOfBirth(1965)
                .gender(Gender.MALE)
                .appointmentDate(targetDate)
                .timeSlotId(testSlotMorning.getId())
                .doctorId(testDoctor.getId())
                .build();

        AppointmentResponse created = appointmentService.createAppointment(req);

        AppointmentCancelRequest cancelReq = AppointmentCancelRequest.builder()
                .status("CANCELLED")
                .cancelReason("Lý do 1")
                .build();

        appointmentService.cancelAppointment(created.getBookingCode(), "0933445566", cancelReq);

        // Hủy lần 2 -> 409 với câu thông báo tiếng Việt thuần, không có chữ CANCELLED
        AppException ex = assertThrows(AppException.class, () ->
                appointmentService.cancelAppointment(created.getBookingCode(), "0933445566", cancelReq));
        assertEquals(409, ex.getStatusCode());
        assertEquals("Lịch hẹn này đã được hủy trước đó", ex.getMessage());
    }

    @Test
    @DisplayName("Hủy lịch thất bại 409: Lịch đã COMPLETED hoặc IN_PROGRESS trả thông báo tiếng Việt tương ứng")
    void testCancelAppointment_CompletedOrInProgress() {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Nguyễn Khám Xong")
                .phone("0933445588")
                .yearOfBirth(1960)
                .gender(Gender.MALE)
                .appointmentDate(targetDate)
                .timeSlotId(testSlotMorning.getId())
                .doctorId(testDoctor.getId())
                .build();

        AppointmentResponse created = appointmentService.createAppointment(req);
        Appointment appointment = appointmentRepository.findByBookingCode(created.getBookingCode()).orElseThrow();

        // 1. Giả lập trạng thái COMPLETED
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        AppointmentCancelRequest cancelReq = AppointmentCancelRequest.builder()
                .status("CANCELLED")
                .cancelReason("Muốn hủy khi đã khám")
                .build();

        AppException exCompleted = assertThrows(AppException.class, () ->
                appointmentService.cancelAppointment(created.getBookingCode(), "0933445588", cancelReq));
        assertEquals(409, exCompleted.getStatusCode());
        assertEquals("Lịch hẹn này đã được khám hoàn tất, không thể hủy", exCompleted.getMessage());

        // 2. Giả lập trạng thái IN_PROGRESS
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointmentRepository.save(appointment);

        AppException exInProgress = assertThrows(AppException.class, () ->
                appointmentService.cancelAppointment(created.getBookingCode(), "0933445588", cancelReq));
        assertEquals(409, exInProgress.getStatusCode());
        assertEquals("Lịch hẹn đang trong quá trình khám, không thể hủy", exInProgress.getMessage());
    }

    @Test
    @DisplayName("Tra cứu lịch: 404 giống hệt nhau khi sai mã hẹn hoặc sai số điện thoại")
    void testGetAppointment_NotFoundSameForBadCodeOrBadPhone() {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Đỗ Thị D")
                .phone("0944556677")
                .yearOfBirth(1975)
                .gender(Gender.FEMALE)
                .appointmentDate(targetDate)
                .timeSlotId(testSlotMorning.getId())
                .doctorId(testDoctor.getId())
                .build();

        AppointmentResponse created = appointmentService.createAppointment(req);

        // Trường hợp 1: Sai số điện thoại
        AppException exWrongPhone = assertThrows(AppException.class, () ->
                appointmentService.getAppointmentByBookingCode(created.getBookingCode(), "0911111111"));
        assertEquals(404, exWrongPhone.getStatusCode());

        // Trường hợp 2: Sai mã hẹn
        AppException exWrongCode = assertThrows(AppException.class, () ->
                appointmentService.getAppointmentByBookingCode("PKCT-99999999-0000", "0944556677"));
        assertEquals(404, exWrongCode.getStatusCode());

        // Cùng một câu thông báo
        assertEquals(exWrongPhone.getMessage(), exWrongCode.getMessage());
    }
}

