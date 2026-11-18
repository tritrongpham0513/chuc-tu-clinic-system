package com.chuctupk.service;

import com.chuctupk.dto.response.AppointmentDto;
import com.chuctupk.dto.response.StaffAppointmentListResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.StaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaffAppointmentListTest {

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
    private Clock clock;

    private StaffServiceImpl staffService;

    private final LocalDate testDate = LocalDate.of(2026, 11, 23);
    private Doctor doc1;
    private Doctor doc2;
    private User userDoc1;
    private User userDoc2;
    private TimeSlot morningSlot;
    private TimeSlot afternoonSlot;

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
        clock = Clock.fixed(Instant.parse("2026-11-23T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

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
                clock
        );

        userDoc1 = User.builder().id(10L).role(Role.DOCTOR).isActive(true).build();
        userDoc2 = User.builder().id(20L).role(Role.DOCTOR).isActive(true).build();

        doc1 = Doctor.builder()
                .id(1L)
                .user(userDoc1)
                .fullName("Bác sĩ Nguyễn Văn A")
                .isActive(true)
                .build();

        doc2 = Doctor.builder()
                .id(2L)
                .user(userDoc2)
                .fullName("Bác sĩ Trần Thị B")
                .isActive(true)
                .build();

        morningSlot = TimeSlot.builder()
                .id(100L)
                .slotLabel("08:00 - 08:30")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(8, 30))
                .period(Period.MORNING)
                .build();

        afternoonSlot = TimeSlot.builder()
                .id(101L)
                .slotLabel("13:30 - 14:00")
                .startTime(LocalTime.of(13, 30))
                .endTime(LocalTime.of(14, 0))
                .period(Period.AFTERNOON)
                .build();

        when(checkinRepository.findByAppointmentIdIn(any())).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("a) Phiếu có bác sĩ nằm trong findOnDutyDoctorIds -> doctorOnDuty = true")
    void testDoctorOnDutyTrue() {
        Appointment appt = Appointment.builder()
                .id(101L)
                .bookingCode("CT-PK-01")
                .appointmentDate(testDate)
                .timeSlot(morningSlot)
                .doctor(doc1)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(testDate))
                .thenReturn(List.of(appt));
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING))
                .thenReturn(List.of(1L));
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.AFTERNOON))
                .thenReturn(List.of());

        StaffAppointmentListResponse res = staffService.getAppointments(testDate);

        assertNotNull(res);
        assertEquals(1, res.items().size());
        AppointmentDto item = res.items().get(0);
        assertEquals("Bác sĩ Nguyễn Văn A", item.doctor());
        assertEquals("1", item.doctorId());
        assertEquals(Boolean.TRUE, item.doctorOnDuty());
    }

    @Test
    @DisplayName("b) Phiếu có bác sĩ KHÔNG nằm trong danh sách trực -> doctorOnDuty = false")
    void testDoctorOnDutyFalse() {
        Appointment appt = Appointment.builder()
                .id(101L)
                .bookingCode("CT-PK-01")
                .appointmentDate(testDate)
                .timeSlot(morningSlot)
                .doctor(doc1)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(testDate))
                .thenReturn(List.of(appt));
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING))
                .thenReturn(List.of(2L)); // Chỉ có BS 2 trực, BS 1 không trực
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.AFTERNOON))
                .thenReturn(List.of());

        StaffAppointmentListResponse res = staffService.getAppointments(testDate);

        assertNotNull(res);
        assertEquals(1, res.items().size());
        AppointmentDto item = res.items().get(0);
        assertEquals(Boolean.FALSE, item.doctorOnDuty());
    }

    @Test
    @DisplayName("c) Phiếu không có bác sĩ -> doctorOnDuty = null")
    void testDoctorOnDutyNullWhenNoDoctor() {
        Appointment appt = Appointment.builder()
                .id(102L)
                .bookingCode("CT-PK-02")
                .appointmentDate(testDate)
                .timeSlot(morningSlot)
                .doctor(null)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(testDate))
                .thenReturn(List.of(appt));
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING))
                .thenReturn(List.of(1L));
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.AFTERNOON))
                .thenReturn(List.of());

        StaffAppointmentListResponse res = staffService.getAppointments(testDate);

        assertNotNull(res);
        assertEquals(1, res.items().size());
        AppointmentDto item = res.items().get(0);
        assertNull(item.doctor());
        assertNull(item.doctorOnDuty());
    }

    @Test
    @DisplayName("d) getMyAppointments chỉ trả phiếu của bác sĩ đang đăng nhập (tạo 2 bác sĩ, 2 phiếu)")
    void testGetMyAppointmentsOnlyReturnsCurrentDoctorAppointments() {
        Appointment appt1 = Appointment.builder()
                .id(101L)
                .bookingCode("CT-PK-01")
                .appointmentDate(testDate)
                .timeSlot(morningSlot)
                .doctor(doc1)
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment appt2 = Appointment.builder()
                .id(102L)
                .bookingCode("CT-PK-02")
                .appointmentDate(testDate)
                .timeSlot(afternoonSlot)
                .doctor(doc2)
                .status(AppointmentStatus.PENDING)
                .build();

        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(testDate))
                .thenReturn(List.of(appt1, appt2));
        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(doc1));
        when(doctorRepository.findByUserId(20L)).thenReturn(Optional.of(doc2));
        when(doctorAssignmentService.findOnDutyDoctorIds(eq(testDate), any())).thenReturn(List.of(1L, 2L));

        StaffAppointmentListResponse res = staffService.getMyAppointments(testDate, userDoc1);

        assertNotNull(res);
        assertEquals(1, res.items().size());
        assertEquals("CT-PK-01", res.items().get(0).ticketCode());
        assertEquals("Bác sĩ Nguyễn Văn A", res.items().get(0).doctor());
        assertEquals(1, res.summary().total());
    }

    @Test
    @DisplayName("e) getMyAppointments với user không có hồ sơ bác sĩ -> AppException (422), message tiếng Việt")
    void testGetMyAppointmentsWithoutDoctorProfileThrows422() {
        User receptionist = User.builder().id(99L).role(Role.STAFF).build();
        when(doctorRepository.findByUserId(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                staffService.getMyAppointments(testDate, receptionist));

        assertEquals(422, ex.getStatusCode());
        assertEquals("Tài khoản này không gắn với hồ sơ bác sĩ nào.", ex.getMessage());
    }

    @Test
    @DisplayName("f) findOnDutyDoctorIds chỉ được gọi 2 lần cho 1 lần tải (MORNING + AFTERNOON), không phải mỗi phiếu 1 lần")
    void testFindOnDutyDoctorIdsCalledOnlyTwicePerDay() {
        List<Appointment> appts = new ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            appts.add(Appointment.builder()
                    .id(200L + i)
                    .bookingCode("CT-PK-0" + i)
                    .appointmentDate(testDate)
                    .timeSlot(i % 2 == 0 ? afternoonSlot : morningSlot)
                    .doctor(i % 2 == 0 ? doc2 : doc1)
                    .status(AppointmentStatus.PENDING)
                    .build());
        }

        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(testDate))
                .thenReturn(appts);
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.MORNING))
                .thenReturn(List.of(1L));
        when(doctorAssignmentService.findOnDutyDoctorIds(testDate, Period.AFTERNOON))
                .thenReturn(List.of(2L));

        StaffAppointmentListResponse res = staffService.getAppointments(testDate);

        assertNotNull(res);
        assertEquals(6, res.items().size());

        // Kiểm tra đúng 2 lần gọi cho 2 ca MORNING và AFTERNOON, không phải 6 lần theo số phiếu
        verify(doctorAssignmentService, times(1)).findOnDutyDoctorIds(testDate, Period.MORNING);
        verify(doctorAssignmentService, times(1)).findOnDutyDoctorIds(testDate, Period.AFTERNOON);
        verify(doctorAssignmentService, times(2)).findOnDutyDoctorIds(any(), any());
    }
}
