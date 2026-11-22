package com.chuctupk.service;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.dto.request.StaffCreateAppointmentRequest;
import com.chuctupk.dto.response.FollowUpListResponse;
import com.chuctupk.dto.response.FollowUpReminderPreviewDto;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.notification.ManualProvider;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.AppointmentStateServiceImpl;
import com.chuctupk.service.impl.StaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Sprint 11 — Ticket 6: Follow Up Query & Service Tests")
class FollowUpQueryTest {

    private AppointmentRepository appointmentRepository;
    private CheckinRepository checkinRepository;
    private PatientRepository patientRepository;
    private TimeSlotRepository timeSlotRepository;
    private MedicalRecordRepository medicalRecordRepository;
    private AppointmentSeatRepository appointmentSeatRepository;
    private ClinicSettingService clinicSettingService;
    private SlotPolicy slotPolicy;
    private CapacityResolver capacityResolver;
    private DoctorAssignmentService doctorAssignmentService;
    private DoctorRepository doctorRepository;
    private DoctorScheduleAdminService doctorScheduleAdminService;
    private DoctorDayOffRepository doctorDayOffRepository;
    private NotificationService notificationService;
    private SequenceGeneratorService sequenceGeneratorService;
    private Clock clock;
    private NotificationLogRepository notificationLogRepository;
    private ManualProvider manualProvider;
    private AppointmentStatusHistoryRepository statusHistoryRepository;
    private AppointmentStateService appointmentStateService;

    private StaffServiceImpl staffService;

    private User staffUser;
    private User doctorUserA;
    private Doctor doctorA;
    private Doctor doctorB;
    private Patient patient1;
    private Patient patient2;
    private Patient patient3;
    private TimeSlot testSlot;

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
        statusHistoryRepository = mock(AppointmentStatusHistoryRepository.class);
        capacityResolver = mock(CapacityResolver.class);

        when(capacityResolver.effectiveCapacity(any(), any())).thenReturn(6);
        when(sequenceGeneratorService.nextAppointmentCode(any())).thenReturn("CT-PK-26-0001");

        clock = Clock.fixed(Instant.parse("2026-11-22T08:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(100L);
            }
            return a;
        });
        when(appointmentSeatRepository.save(any(AppointmentSeat.class))).thenAnswer(inv -> inv.getArgument(0));

        appointmentStateService = new AppointmentStateServiceImpl(
                appointmentRepository,
                statusHistoryRepository,
                clock
        );

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
                clock,
                notificationLogRepository,
                manualProvider,
                capacityResolver,
                appointmentStateService,
                statusHistoryRepository
        );

        staffUser = User.builder()
                .id(10L)
                .fullName("Tiếp tân Nguyễn")
                .role(Role.STAFF)
                .build();

        doctorUserA = User.builder()
                .id(5L)
                .fullName("BS. Nguyễn Văn A")
                .role(Role.DOCTOR)
                .build();

        doctorA = Doctor.builder()
                .id(1L)
                .user(doctorUserA)
                .fullName("BS. Nguyễn Văn A")
                .build();

        doctorB = Doctor.builder()
                .id(2L)
                .user(User.builder().id(6L).fullName("BS. Trần Thị B").role(Role.DOCTOR).build())
                .fullName("BS. Trần Thị B")
                .build();

        patient1 = Patient.builder()
                .id(101L)
                .fullName("Bệnh nhân Lê Văn Một")
                .phone("0901111111")
                .yearOfBirth(1950)
                .isActive(true)
                .build();

        patient2 = Patient.builder()
                .id(102L)
                .fullName("Bệnh nhân Trần Thị Hai")
                .phone("0902222222")
                .yearOfBirth(1960)
                .isActive(true)
                .build();

        patient3 = Patient.builder()
                .id(103L)
                .fullName("Bệnh nhân Phạm Văn Ba")
                .phone("0903333333")
                .yearOfBirth(1970)
                .isActive(true)
                .build();

        testSlot = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .maxPatients(6)
                .isActive(true)
                .build();

        when(timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(testSlot));
        when(patientRepository.findById(101L)).thenReturn(Optional.of(patient1));
        when(patientRepository.findById(102L)).thenReturn(Optional.of(patient2));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctorA));
    }

    private MedicalRecord createRecord(Long id, Patient p, Doctor d, LocalDate visitDate, LocalDate followUpDate) {
        Appointment a = Appointment.builder()
                .id(id * 10)
                .bookingCode("CT-PK-" + id)
                .patient(p)
                .doctor(d)
                .serviceType("KHAM_BENH")
                .appointmentDate(visitDate)
                .build();

        return MedicalRecord.builder()
                .id(id)
                .appointment(a)
                .patient(p)
                .doctor(d)
                .doctorName(d != null ? d.getFullName() : "BS. Nguyễn Văn A")
                .visitDate(visitDate)
                .followUpDate(followUpDate)
                .diagnosis("Đau nhức khớp")
                .prescription("Thang thuốc thảo mộc")
                .build();
    }

    @Test
    @DisplayName("Test 1: Truy vấn trả đúng bản ghi có followUpDate trong 7 ngày")
    void test1_QueryReturnsRecordsWithin7Days() {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = today.minusDays(30);
        LocalDate toDate = today.plusDays(7);

        MedicalRecord mr1 = createRecord(1L, patient1, doctorA, today.minusDays(10), today.plusDays(3));

        when(medicalRecordRepository.findFollowUpsAll(eq(fromDate), eq(toDate), eq(today)))
                .thenReturn(List.of(mr1));

        FollowUpListResponse res = staffService.getFollowUps(7, staffUser);

        assertNotNull(res);
        assertEquals(1, res.total());
        assertEquals("Bệnh nhân Lê Văn Một", res.items().get(0).patientName());
        assertEquals(3L, res.items().get(0).daysUntil());
        assertEquals(today.plusDays(3), res.items().get(0).followUpDate());
        verify(medicalRecordRepository).findFollowUpsAll(eq(fromDate), eq(toDate), eq(today));
    }

    @Test
    @DisplayName("Test 2: Bệnh nhân đã có phiếu tương lai -> không xuất hiện")
    void test2_PatientWithFutureAppointmentNotReturned() {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = today.minusDays(30);
        LocalDate toDate = today.plusDays(7);

        // Giả lập query repository đã loại trừ bệnh nhân có phiếu hẹn tương lai/hôm nay
        when(medicalRecordRepository.findFollowUpsAll(eq(fromDate), eq(toDate), eq(today)))
                .thenReturn(List.of());

        FollowUpListResponse res = staffService.getFollowUps(7, staffUser);

        assertNotNull(res);
        assertEquals(0, res.total());
        assertTrue(res.items().isEmpty());
    }

    @Test
    @DisplayName("Test 3: days=30 trả nhiều hơn days=7")
    void test3_Days30ReturnsMoreThanDays7() {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = today.minusDays(30);

        MedicalRecord mr1 = createRecord(1L, patient1, doctorA, today.minusDays(5), today.plusDays(3));
        MedicalRecord mr2 = createRecord(2L, patient2, doctorA, today.minusDays(5), today.plusDays(15));
        MedicalRecord mr3 = createRecord(3L, patient3, doctorA, today.minusDays(5), today.plusDays(25));

        when(medicalRecordRepository.findFollowUpsAll(eq(fromDate), eq(today.plusDays(7)), eq(today)))
                .thenReturn(List.of(mr1));

        when(medicalRecordRepository.findFollowUpsAll(eq(fromDate), eq(today.plusDays(30)), eq(today)))
                .thenReturn(List.of(mr1, mr2, mr3));

        FollowUpListResponse res7 = staffService.getFollowUps(7, staffUser);
        FollowUpListResponse res30 = staffService.getFollowUps(30, staffUser);

        assertEquals(1, res7.total());
        assertEquals(3, res30.total());
        assertTrue(res30.total() > res7.total(), "days=30 phải trả nhiều hơn days=7");
    }

    @Test
    @DisplayName("Test 4: Bác sĩ chỉ thấy bệnh nhân của mình")
    void test4_DoctorOnlySeesOwnPatients() {
        LocalDate today = LocalDate.now(clock);
        LocalDate fromDate = today.minusDays(30);
        LocalDate toDate = today.plusDays(7);

        MedicalRecord mrA = createRecord(1L, patient1, doctorA, today.minusDays(5), today.plusDays(3));

        when(medicalRecordRepository.findFollowUpsByDoctorUserId(eq(fromDate), eq(toDate), eq(today), eq(5L)))
                .thenReturn(List.of(mrA));

        // Bác sĩ A xem danh sách -> Gọi findFollowUpsByDoctorUserId với doctorUserId = 5
        FollowUpListResponse resDocA = staffService.getFollowUps(7, doctorUserA);

        assertEquals(1, resDocA.total());
        assertEquals("Bệnh nhân Lê Văn Một", resDocA.items().get(0).patientName());
        verify(medicalRecordRepository).findFollowUpsByDoctorUserId(eq(fromDate), eq(toDate), eq(today), eq(5L));
        verify(medicalRecordRepository, never()).findFollowUpsAll(any(), any(), any());

        // Tiếp tân xem danh sách -> Gọi findFollowUpsAll
        when(medicalRecordRepository.findFollowUpsAll(eq(fromDate), eq(toDate), eq(today)))
                .thenReturn(List.of(mrA));
        FollowUpListResponse resStaff = staffService.getFollowUps(7, staffUser);
        assertEquals(1, resStaff.total());
        verify(medicalRecordRepository).findFollowUpsAll(eq(fromDate), eq(toDate), eq(today));
    }

    @Test
    @DisplayName("Test 5: Đặt tái khám -> bản ghi rời danh sách & Chống trùng lịch (422)")
    void test5_BookingFollowUpRemovesRecordFromList() {
        LocalDate today = LocalDate.now(clock);
        LocalDate targetFollowUpDate = today.plusDays(4);

        // 1. Tạo request đặt tái khám có điền sẵn note và doctorId
        StaffCreateAppointmentRequest req = new StaffCreateAppointmentRequest(
                patient1.getId(),
                "KHAM_BENH",
                targetFollowUpDate.toString(),
                "08:00",
                false,
                doctorA.getId(),
                "Tái khám theo hẹn ngày " + ContractMapper.formatDate(targetFollowUpDate)
        );

        when(slotPolicy.evaluate(any(), any(), any(), any())).thenReturn(SlotPolicy.SlotStatus.OK);
        when(appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                eq(patient1.getId()), eq(targetFollowUpDate), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(false);

        // Đặt lịch thành công
        Map<String, Object> bookResult = staffService.createCounterAppointment(staffUser, req);
        assertNotNull(bookResult);

        // Xác minh Appointment được lưu với note và doctor đúng chỉ định
        ArgumentCaptor<Appointment> apptCaptor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository, atLeastOnce()).save(apptCaptor.capture());
        Appointment savedAppt = apptCaptor.getValue();
        assertEquals("Tái khám theo hẹn ngày " + ContractMapper.formatDate(targetFollowUpDate), savedAppt.getNote());
        assertEquals(doctorA, savedAppt.getDoctor());

        // 2. Nghiệm thu: Không thể tạo 2 phiếu trùng ngày cho cùng bệnh nhân -> Báo 422
        when(appointmentRepository.existsByPatientIdAndAppointmentDateAndStatusNot(
                eq(patient1.getId()), eq(targetFollowUpDate), eq(AppointmentStatus.CANCELLED)))
                .thenReturn(true);

        AppException ex = assertThrows(AppException.class, () ->
                staffService.createCounterAppointment(staffUser, req));
        assertEquals(422, ex.getStatusCode(), "Báo lỗi 422 khi trùng lịch trong ngày");

        // 3. Sau khi đặt tái khám, bệnh nhân rời khỏi danh sách query
        when(medicalRecordRepository.findFollowUpsAll(any(), any(), any())).thenReturn(List.of());
        FollowUpListResponse afterBooking = staffService.getFollowUps(7, staffUser);
        assertEquals(0, afterBooking.total(), "Sau khi đặt tái khám, bản ghi phải rời khỏi danh sách");
    }

    @Test
    @DisplayName("Ticket 4: Soạn trước nội dung nhắc tái khám theo mẫu chuẩn")
    void testFollowUpReminderPreview() {
        LocalDate today = LocalDate.now(clock);
        MedicalRecord mr = createRecord(1L, patient1, doctorA, today.minusDays(5), today.plusDays(4));

        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(mr));

        FollowUpReminderPreviewDto preview = staffService.getFollowUpReminderPreview(1L);

        assertNotNull(preview);
        assertEquals(1L, preview.recordId());
        assertEquals("FOLLOW_UP", preview.template());
        assertTrue(preview.content().contains("Phòng khám Chúc Từ nhắc Cô/chú tái khám"));
        assertTrue(preview.content().contains(doctorA.getFullName()));
    }
}
