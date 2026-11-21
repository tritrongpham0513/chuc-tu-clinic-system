package com.chuctupk.service;

import com.chuctupk.dto.response.QueueSeatDto;
import com.chuctupk.dto.response.QueueStateDto;
import com.chuctupk.dto.response.QueueWaitingDto;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.CheckinStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.PriorityGroup;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.notification.ManualProvider;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.DoctorAssignmentServiceImpl;
import com.chuctupk.service.impl.StaffServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Ticket 2, 3 & 5 — Kiểm tra Hàng đợi riêng của Bác sĩ & Báo đổi Bác sĩ (Sprint 9)")
class DoctorQueueScopeTest {

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
    private DoctorScheduleRepository doctorScheduleRepository;
    private DoctorScheduleAdminService doctorScheduleAdminService;
    private DoctorDayOffRepository doctorDayOffRepository;
    private NotificationService notificationService;
    private SequenceGeneratorService sequenceGeneratorService;
    private Clock fixedClock;
    private NotificationLogRepository notificationLogRepository;
    private ManualProvider manualProvider;

    private StaffServiceImpl staffService;
    private DoctorAssignmentServiceImpl doctorAssignmentServiceImpl;

    private User doctorAUser;
    private User doctorBUser;
    private User staffUser;

    private Doctor doctorA;
    private Doctor doctorB;

    private LocalDate today;

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
        doctorRepository = mock(DoctorRepository.class);
        doctorScheduleRepository = mock(DoctorScheduleRepository.class);
        doctorScheduleAdminService = mock(DoctorScheduleAdminService.class);
        doctorDayOffRepository = mock(DoctorDayOffRepository.class);
        notificationService = mock(NotificationService.class);
        sequenceGeneratorService = mock(SequenceGeneratorService.class);
        notificationLogRepository = mock(NotificationLogRepository.class);
        manualProvider = new ManualProvider("0397 116 666");

        today = LocalDate.of(2026, 11, 21);
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant instant = today.atTime(9, 0).atZone(zone).toInstant();
        fixedClock = Clock.fixed(instant, zone);

        when(clinicSettingService.getSettingValue(eq("seats"), any())).thenReturn("5");
        when(clinicSettingService.getSettingValue(eq("auto_invite"), any())).thenReturn("0");

        doctorAssignmentServiceImpl = new DoctorAssignmentServiceImpl(
                doctorScheduleRepository,
                doctorDayOffRepository,
                doctorRepository,
                appointmentRepository
        );
        doctorAssignmentService = doctorAssignmentServiceImpl;

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

        doctorAUser = User.builder().id(101L).username("bs_minh").fullName("BS. Minh").role(Role.DOCTOR).build();
        doctorBUser = User.builder().id(102L).username("bs_hoa").fullName("BS. Hoa").role(Role.DOCTOR).build();
        staffUser = User.builder().id(201L).username("letan").fullName("Tiếp tân").role(Role.STAFF).build();

        doctorA = Doctor.builder().id(1L).fullName("BS. Minh").user(doctorAUser).isActive(true).build();
        doctorB = Doctor.builder().id(2L).fullName("BS. Hoa").user(doctorBUser).isActive(true).build();

        when(doctorRepository.findByUserId(101L)).thenReturn(Optional.of(doctorA));
        when(doctorRepository.findByUserId(102L)).thenReturn(Optional.of(doctorB));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctorA));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctorB));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Test 3: queue?mine=1 với DOCTOR chỉ trả hàng đợi của chính mình")
    void queue_withDoctorAndMine1_onlyReturnsDoctorOwnQueue() {
        Patient patientA = Patient.builder().id(10L).fullName("Bệnh nhân của BS Minh").phone("0901111111").build();
        Patient patientB = Patient.builder().id(20L).fullName("Bệnh nhân của BS Hoa").phone("0902222222").build();

        Appointment apptA = Appointment.builder()
                .id(1001L)
                .bookingCode("CT-PK-26-0001")
                .appointmentDate(today)
                .patient(patientA)
                .doctor(doctorA)
                .build();

        Appointment apptB = Appointment.builder()
                .id(1002L)
                .bookingCode("CT-PK-26-0002")
                .appointmentDate(today)
                .patient(patientB)
                .doctor(doctorB)
                .build();

        Checkin checkinA = Checkin.builder()
                .id(1L)
                .appointment(apptA)
                .queueNo(1)
                .priorityGroup(PriorityGroup.NORMAL)
                .status(CheckinStatus.WAITING)
                .checkInTime(LocalDateTime.of(2026, 11, 21, 8, 30))
                .build();

        Checkin checkinB = Checkin.builder()
                .id(2L)
                .appointment(apptB)
                .queueNo(2)
                .priorityGroup(PriorityGroup.NORMAL)
                .status(CheckinStatus.WAITING)
                .checkInTime(LocalDateTime.of(2026, 11, 21, 8, 35))
                .build();

        when(checkinRepository.findInRoomToday(any(), any())).thenReturn(Collections.emptyList());
        when(checkinRepository.findWaitingTodayOrdered(any(), any())).thenReturn(List.of(checkinA, checkinB));
        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(today)).thenReturn(List.of(apptA, apptB));

        // Bác sĩ Minh gọi GET /queue?mine=1
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(doctorAUser, null, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")))
        );
        QueueStateDto queueDoctorA = staffService.getQueueState(doctorAUser, 1);

        assertThat(queueDoctorA.waiting()).hasSize(1);
        assertThat(queueDoctorA.waiting().get(0).patient().fullName()).isEqualTo("Bệnh nhân của BS Minh");
        assertThat(queueDoctorA.waiting().get(0).appointmentId()).isEqualTo("1001");

        // Bác sĩ Hoa gọi GET /queue?mine=1
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(doctorBUser, null, List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")))
        );
        QueueStateDto queueDoctorB = staffService.getQueueState(doctorBUser, 1);

        assertThat(queueDoctorB.waiting()).hasSize(1);
        assertThat(queueDoctorB.waiting().get(0).patient().fullName()).isEqualTo("Bệnh nhân của BS Hoa");
        assertThat(queueDoctorB.waiting().get(0).appointmentId()).isEqualTo("1002");
    }

    @Test
    @DisplayName("Test 4: mine=1 với STAFF -> bỏ qua mine=1, trả hàng đợi chung")
    void queue_withStaffAndMine1_returnsAllQueue() {
        Patient patientA = Patient.builder().id(10L).fullName("Bệnh nhân của BS Minh").phone("0901111111").build();
        Patient patientB = Patient.builder().id(20L).fullName("Bệnh nhân của BS Hoa").phone("0902222222").build();

        Appointment apptA = Appointment.builder().id(1001L).appointmentDate(today).patient(patientA).doctor(doctorA).build();
        Appointment apptB = Appointment.builder().id(1002L).appointmentDate(today).patient(patientB).doctor(doctorB).build();

        Checkin checkinA = Checkin.builder().id(1L).appointment(apptA).queueNo(1).priorityGroup(PriorityGroup.NORMAL).status(CheckinStatus.WAITING).build();
        Checkin checkinB = Checkin.builder().id(2L).appointment(apptB).queueNo(2).priorityGroup(PriorityGroup.NORMAL).status(CheckinStatus.WAITING).build();

        when(checkinRepository.findInRoomToday(any(), any())).thenReturn(Collections.emptyList());
        when(checkinRepository.findWaitingTodayOrdered(any(), any())).thenReturn(List.of(checkinA, checkinB));
        when(appointmentRepository.findByAppointmentDateOrderByTimeSlotStartTimeAsc(today)).thenReturn(List.of(apptA, apptB));

        // Lễ tân gọi với mine=1 -> hệ thống bỏ qua và trả cả 2 bệnh nhân
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(staffUser, null, List.of(new SimpleGrantedAuthority("ROLE_STAFF")))
        );
        QueueStateDto queueStaff = staffService.getQueueState(staffUser, 1);

        assertThat(queueStaff.waiting()).hasSize(2);
        List<String> names = queueStaff.waiting().stream().map(w -> w.patient().fullName()).toList();
        assertThat(names).containsExactly("Bệnh nhân của BS Minh", "Bệnh nhân của BS Hoa");
    }

    @Test
    @DisplayName("Test 5: DOCTOR báo đổi bác sĩ cho phiếu không phải của mình -> 403 Forbidden")
    void doctorChangeRequest_byDoctorNotAssignedToAppointment_throwsForbidden() {
        // Phiếu được phân công cho BS. Minh (doctorA)
        Appointment appt = Appointment.builder()
                .id(1001L)
                .bookingCode("CT-PK-26-0001")
                .appointmentDate(today)
                .doctor(doctorA)
                .build();

        when(appointmentRepository.findById(1001L)).thenReturn(Optional.of(appt));

        // BS. Hoa (doctorB) cố tình gửi yêu cầu đổi bác sĩ cho phiếu của BS. Minh
        assertThatThrownBy(() -> staffService.requestDoctorChange(1001L, "Yêu cầu đổi", doctorBUser))
                .isInstanceOf(AppException.class)
                .satisfies(e -> {
                    AppException ae = (AppException) e;
                    assertThat(ae.getStatusCode()).isEqualTo(403);
                    assertThat(ae.getMessage()).contains("Chỉ bác sĩ được phân công");
                });
    }

    @Test
    @DisplayName("Test 6: Đổi bác sĩ thành công -> 2 cột doctor_change_* được reset về NULL")
    void assignDoctor_success_clearsDoctorChangeFlags() {
        TimeSlot slot = TimeSlot.builder().id(1L).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0)).period(Period.MORNING).build();

        Appointment appt = Appointment.builder()
                .id(1001L)
                .bookingCode("CT-PK-26-0001")
                .appointmentDate(today)
                .timeSlot(slot)
                .doctor(doctorA)
                .doctorChangeRequestedAt(LocalDateTime.now(fixedClock).minusHours(1))
                .doctorChangeNote("Bệnh nhân cần chuyển sang chuyên khoa châm cứu")
                .build();

        when(appointmentRepository.findById(1001L)).thenReturn(Optional.of(appt));
        when(doctorScheduleRepository.isDoctorScheduled(eq(2L), any(DayOfWeek.class), eq(Period.MORNING))).thenReturn(true);
        when(doctorDayOffRepository.isDoctorOff(eq(2L), eq(today), eq(Period.MORNING))).thenReturn(false);

        // Đổi sang BS. Hoa (doctorB - id 2)
        doctorAssignmentService.assignDoctor(1001L, 2L);

        assertThat(appt.getDoctor()).isEqualTo(doctorB);
        assertThat(appt.getDoctorChangeRequestedAt()).isNull();
        assertThat(appt.getDoctorChangeNote()).isNull();

        verify(appointmentRepository).save(appt);
    }
}
