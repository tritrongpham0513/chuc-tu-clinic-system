package com.chuctupk.service;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.dto.response.NoShowPreviewItemDto;
import com.chuctupk.dto.response.StaffAppointmentLookupResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.notification.ManualProvider;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.StaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Ticket 1 & 5 — Kiểm tra No-Show Preview và Check-in (Sprint 9)")
class NoShowPreviewTest {

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

    private LocalDate today;
    private LocalTime nowTime;

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
        notificationLogRepository = mock(NotificationLogRepository.class);
        manualProvider = new ManualProvider("0397 116 666");

        // Giả lập thời gian hiện tại: 16:30 ngày 2026-11-21
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        today = LocalDate.of(2026, 11, 21);
        nowTime = LocalTime.of(16, 30);
        Instant instant = today.atTime(nowTime).atZone(zone).toInstant();
        fixedClock = Clock.fixed(instant, zone);

        // Mốc no_show_cancel_at mặc định là 17:00, giờ tiếp nhận 08:00 - 16:30
        when(clinicSettingService.getNoShowCancelAt()).thenReturn(LocalTime.of(17, 0));
        when(clinicSettingService.getReceptionOpen()).thenReturn(LocalTime.of(8, 0));
        when(clinicSettingService.getReceptionClose()).thenReturn(LocalTime.of(16, 30));

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
                .id(1L)
                .username("receptionist")
                .fullName("Lễ tân Chúc Từ")
                .role(Role.STAFF)
                .build();
    }

    @Test
    @DisplayName("Test 1: no-show-preview chỉ trả phiếu đúng tiêu chí (hôm nay, PENDING, slot đã kết thúc; không trả phiếu đã check-in)")
    void noShowPreview_onlyReturnsAppointmentsMatchingCriteria() {
        Patient patientA = Patient.builder().id(10L).fullName("Nguyễn Văn A").phone("0901234567").build();
        Patient patientB = Patient.builder().id(20L).fullName("Trần Thị B").phone("0912345678").build();
        Patient patientC = Patient.builder().id(30L).fullName("Lê Văn C").phone("0988776655").build();

        Doctor doctorMinh = Doctor.builder().id(1L).fullName("BS. Minh").build();

        // Slot sáng 08:00 - 09:00 (đã kết thúc lúc 09:00 < 16:30)
        TimeSlot slotMorning = TimeSlot.builder().id(1L).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0)).build();
        // Slot chiều muộn 16:45 - 17:45 (chưa kết thúc lúc 16:30)
        TimeSlot slotLate = TimeSlot.builder().id(2L).startTime(LocalTime.of(16, 45)).endTime(LocalTime.of(17, 45)).build();

        // 1. Phiếu A: HỢP LỆ (hôm nay, PENDING, slot đã kết thúc lúc 09:00)
        Appointment apptA = Appointment.builder()
                .id(101L)
                .bookingCode("CT-PK-26-0001")
                .appointmentDate(today)
                .status(AppointmentStatus.PENDING)
                .patient(patientA)
                .doctor(doctorMinh)
                .timeSlot(slotMorning)
                .slotStartSnapshot(LocalTime.of(8, 0))
                .slotEndSnapshot(LocalTime.of(9, 0))
                .build();

        // 2. Phiếu B: ĐÃ CHECK-IN (CONFIRMED) -> Không được trả về trong preview
        Appointment apptB = Appointment.builder()
                .id(102L)
                .bookingCode("CT-PK-26-0002")
                .appointmentDate(today)
                .status(AppointmentStatus.CONFIRMED)
                .patient(patientB)
                .doctor(doctorMinh)
                .timeSlot(slotMorning)
                .slotStartSnapshot(LocalTime.of(8, 0))
                .slotEndSnapshot(LocalTime.of(9, 0))
                .build();

        // 3. Phiếu C: PENDING nhưng slot chưa kết thúc (16:45 - 17:45) -> Không được trả về
        Appointment apptC = Appointment.builder()
                .id(103L)
                .bookingCode("CT-PK-26-0003")
                .appointmentDate(today)
                .status(AppointmentStatus.PENDING)
                .patient(patientC)
                .doctor(doctorMinh)
                .timeSlot(slotLate)
                .slotStartSnapshot(LocalTime.of(16, 45))
                .slotEndSnapshot(LocalTime.of(17, 45))
                .build();

        // Giả lập repository trả về danh sách PENDING
        when(appointmentRepository.findWithDetailsByAppointmentDateAndStatus(today, AppointmentStatus.PENDING))
                .thenReturn(List.of(apptA, apptC));

        List<NoShowPreviewItemDto> preview = staffService.getNoShowPreview(today);

        assertThat(preview).hasSize(1);
        NoShowPreviewItemDto item = preview.get(0);
        assertThat(item.id()).isEqualTo(101L);
        assertThat(item.bookingCode()).isEqualTo("CT-PK-26-0001");
        assertThat(item.patientName()).isEqualTo("Nguyễn Văn A");
        assertThat(item.patientPhone()).isEqualTo(PhoneValidator.mask("0901234567"));
        assertThat(item.slotTime()).isEqualTo("08:00 - 09:00");
        assertThat(item.doctorName()).isEqualTo("BS. Minh");
        // Từ 16:30 đến 17:00 còn đúng 30 phút
        assertThat(item.minutesUntilCancellation()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Test 2: Sau khi check-in, phiếu chuyển trạng thái và biến mất khỏi danh sách no-show-preview")
    void afterCheckin_appointmentIsRemovedFromPreview() {
        Patient patientA = Patient.builder().id(10L).fullName("Nguyễn Văn A").phone("0901234567").build();
        Doctor doctorMinh = Doctor.builder().id(1L).fullName("BS. Minh").build();
        TimeSlot slotMorning = TimeSlot.builder().id(1L).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0)).build();

        Appointment appt = Appointment.builder()
                .id(101L)
                .bookingCode("CT-PK-26-0001")
                .appointmentDate(today)
                .status(AppointmentStatus.PENDING)
                .patient(patientA)
                .doctor(doctorMinh)
                .timeSlot(slotMorning)
                .slotStartSnapshot(LocalTime.of(8, 0))
                .slotEndSnapshot(LocalTime.of(9, 0))
                .build();

        // Lần đầu: Phiếu đang PENDING -> xuất hiện trong preview
        when(appointmentRepository.findWithDetailsByAppointmentDateAndStatus(today, AppointmentStatus.PENDING))
                .thenReturn(new ArrayList<>(List.of(appt)));

        List<NoShowPreviewItemDto> initialPreview = staffService.getNoShowPreview(today);
        assertThat(initialPreview).hasSize(1);
        assertThat(initialPreview.get(0).id()).isEqualTo(101L);

        // Lễ tân check-in cho phiếu khám
        when(appointmentRepository.findById(101L)).thenReturn(Optional.of(appt));
        when(sequenceGeneratorService.nextQueueNumber(today)).thenReturn(1);
        when(clinicSettingService.getSettingValue(eq("auto_invite"), any())).thenReturn("0");
        when(clinicSettingService.getSettingValue(eq("seats"), any())).thenReturn("5");

        StaffAppointmentLookupResponse checkinResult = staffService.checkin(staffUser, 101L);
        assertThat(checkinResult).isNotNull();
        assertThat(appt.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        // Sau khi check-in, phiếu không còn PENDING -> không xuất hiện trong preview nữa
        when(appointmentRepository.findWithDetailsByAppointmentDateAndStatus(today, AppointmentStatus.PENDING))
                .thenReturn(Collections.emptyList());

        List<NoShowPreviewItemDto> afterCheckinPreview = staffService.getNoShowPreview(today);
        assertThat(afterCheckinPreview).isEmpty();
    }
}
