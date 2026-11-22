package com.chuctupk.service;

import com.chuctupk.dto.request.CancelAppointmentStaffRequest;
import com.chuctupk.dto.request.RescheduleAppointmentRequest;
import com.chuctupk.dto.request.StaffCreateAppointmentRequest;
import com.chuctupk.dto.response.AppointmentHistoryResponse;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.notification.ManualProvider;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.AppointmentStateServiceImpl;
import com.chuctupk.service.impl.StaffServiceImpl;
import com.chuctupk.service.scheduled.ClinicScheduledTasks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaffStatusHistoryTest {

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
    private Patient testPatient;
    private TimeSlot testSlot;
    private Doctor testDoctor;

    private List<AppointmentStatusHistory> savedHistories;

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
        manualProvider = mock(ManualProvider.class);
        statusHistoryRepository = mock(AppointmentStatusHistoryRepository.class);
        capacityResolver = mock(CapacityResolver.class);
        when(capacityResolver.effectiveCapacity(any(), any())).thenReturn(6);

        clock = Clock.fixed(Instant.parse("2026-11-23T08:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        savedHistories = new ArrayList<>();
        when(statusHistoryRepository.save(any(AppointmentStatusHistory.class))).thenAnswer(inv -> {
            AppointmentStatusHistory h = inv.getArgument(0);
            savedHistories.add(h);
            return h;
        });

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(100L);
            }
            return a;
        });

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
                .fullName("Lễ tân Nguyễn A")
                .role(Role.STAFF)
                .build();

        testPatient = Patient.builder()
                .id(1L)
                .fullName("Bệnh nhân Lê Văn B")
                .phone("0987654321")
                .build();

        testSlot = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .maxPatients(6)
                .isActive(true)
                .build();

        testDoctor = Doctor.builder()
                .id(1L)
                .fullName("BS. Trần C")
                .build();
    }

    @Test
    @DisplayName("1. Đặt online → đúng 1 dòng, changedBy = null")
    void test1_DatOnline_DungMotDong_ChangedByNull() {
        Appointment newAppt = Appointment.builder()
                .bookingCode("CT-PK-26-2311-001")
                .patient(testPatient)
                .appointmentDate(LocalDate.of(2026, 11, 23))
                .timeSlot(testSlot)
                .source(AppointmentSource.WEB)
                .build();

        Appointment result = appointmentStateService.transition(
                newAppt,
                AppointmentStatus.PENDING,
                null,
                "Bệnh nhân đặt lịch trực tuyến"
        );

        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        assertEquals(1, savedHistories.size());
        AppointmentStatusHistory history = savedHistories.get(0);
        assertNull(history.getOldStatus());
        assertEquals("PENDING", history.getNewStatus());
        assertNull(history.getChangedBy());
        assertEquals("Bệnh nhân đặt lịch trực tuyến", history.getReason());
    }

    @Test
    @DisplayName("2. Huỷ online → đúng lý do bệnh nhân nhập")
    void test2_HuyOnline_DungLyDoBenhNhanNhap() {
        Appointment appt = Appointment.builder()
                .id(20L)
                .bookingCode("CT-PK-26-2311-002")
                .status(AppointmentStatus.PENDING)
                .build();

        String patientReason = "Bận việc gia đình đột xuất";
        Appointment result = appointmentStateService.transition(
                appt,
                AppointmentStatus.CANCELLED,
                null,
                patientReason
        );

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertEquals(1, savedHistories.size());
        AppointmentStatusHistory history = savedHistories.get(0);
        assertEquals("PENDING", history.getOldStatus());
        assertEquals("CANCELLED", history.getNewStatus());
        assertNull(history.getChangedBy());
        assertEquals(patientReason, history.getReason());
    }

    @Test
    @DisplayName("3. Tạo tại quầy → changedBy = nhân viên")
    void test3_TaoTaiQuay_ChangedByNhanVien() {
        LocalDate date = LocalDate.of(2026, 11, 23);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(testPatient));
        when(timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(testSlot));
        when(sequenceGeneratorService.nextAppointmentCode(date)).thenReturn("CT-PK-26-2311-003");
        when(doctorAssignmentService.pickDoctor(date, testSlot)).thenReturn(testDoctor);
        when(appointmentSeatRepository.findByAppointmentDateAndTimeSlotIdOrderBySeatNoAsc(date, 1L))
                .thenReturn(List.of());
        when(appointmentSeatRepository.save(any(AppointmentSeat.class))).thenAnswer(inv -> inv.getArgument(0));

        StaffCreateAppointmentRequest req = new StaffCreateAppointmentRequest(
                1L, "CHAM_CUU", "2026-11-23", "08:00", true
        );

        staffService.createCounterAppointment(staffUser, req);

        assertEquals(1, savedHistories.size());
        AppointmentStatusHistory history = savedHistories.get(0);
        assertNull(history.getOldStatus());
        assertEquals("CONFIRMED", history.getNewStatus());
        assertEquals(staffUser, history.getChangedBy());
        assertEquals("Lễ tân tạo phiếu tại quầy", history.getReason());
    }

    @Test
    @DisplayName("4. Huỷ tại quầy → có lý do")
    void test4_HuyTaiQuay_CoLyDo() {
        Appointment appt = Appointment.builder()
                .id(40L)
                .bookingCode("CT-PK-26-2311-004")
                .status(AppointmentStatus.PENDING)
                .build();
        when(appointmentRepository.findById(40L)).thenReturn(Optional.of(appt));
        when(checkinRepository.findByAppointmentId(40L)).thenReturn(Optional.empty());

        staffService.cancelAppointment(staffUser, 40L, new CancelAppointmentStaffRequest("Bệnh nhân đổi ý"));

        assertEquals(1, savedHistories.size());
        AppointmentStatusHistory history = savedHistories.get(0);
        assertEquals("PENDING", history.getOldStatus());
        assertEquals("CANCELLED", history.getNewStatus());
        assertEquals(staffUser, history.getChangedBy());
        assertEquals("Bệnh nhân đổi ý", history.getReason());
    }

    @Test
    @DisplayName("5. Đổi giờ → 2 dòng (phiếu cũ huỷ + phiếu mới tạo) với lý do nêu mã mới")
    void test5_DoiGio_HaiDong_NeuMaMoi() {
        LocalDate newDate = LocalDate.of(2026, 11, 24);
        Appointment oldAppt = Appointment.builder()
                .id(50L)
                .bookingCode("CT-PK-26-2311-050")
                .status(AppointmentStatus.PENDING)
                .patient(testPatient)
                .source(AppointmentSource.WEB)
                .build();
        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(oldAppt));
        when(timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(testSlot));
        when(sequenceGeneratorService.nextAppointmentCode(newDate)).thenReturn("CT-PK-26-2411-099");
        when(doctorAssignmentService.pickDoctor(newDate, testSlot)).thenReturn(testDoctor);
        when(appointmentSeatRepository.findByAppointmentDateAndTimeSlotIdOrderBySeatNoAsc(newDate, 1L))
                .thenReturn(List.of());
        when(appointmentSeatRepository.save(any(AppointmentSeat.class))).thenAnswer(inv -> inv.getArgument(0));

        RescheduleAppointmentRequest req = new RescheduleAppointmentRequest();
        req.setNewDate(newDate);
        req.setNewTimeSlotId(1L);

        staffService.rescheduleAppointment(staffUser, 50L, req);

        // Đảm bảo có đúng 2 dòng: phiếu mới tạo + phiếu cũ huỷ
        assertEquals(2, savedHistories.size());

        AppointmentStatusHistory newApptHistory = savedHistories.get(0);
        assertEquals("PENDING", newApptHistory.getNewStatus());
        assertTrue(newApptHistory.getReason().contains("CT-PK-26-2411-099"),
                "Lý do phiếu mới phải nêu mã mới: " + newApptHistory.getReason());
        assertEquals(staffUser, newApptHistory.getChangedBy());

        AppointmentStatusHistory oldApptHistory = savedHistories.get(1);
        assertEquals("CANCELLED", oldApptHistory.getNewStatus());
        assertTrue(oldApptHistory.getReason().contains("CT-PK-26-2411-099"),
                "Lý do phiếu cũ huỷ phải nêu mã mới: " + oldApptHistory.getReason());
        assertEquals(staffUser, oldApptHistory.getChangedBy());
    }

    @Test
    @DisplayName("6. Check-in → CONFIRMED, changedBy đúng")
    void test6_Checkin_Confirmed_ChangedByDung() {
        LocalDate today = LocalDate.of(2026, 11, 23);
        Appointment appt = Appointment.builder()
                .id(60L)
                .bookingCode("CT-PK-26-2311-060")
                .appointmentDate(today)
                .status(AppointmentStatus.PENDING)
                .build();
        when(appointmentRepository.findById(60L)).thenReturn(Optional.of(appt));
        when(clinicSettingService.getReceptionOpen()).thenReturn(LocalTime.of(7, 0));
        when(clinicSettingService.getReceptionClose()).thenReturn(LocalTime.of(17, 0));
        when(checkinRepository.save(any(Checkin.class))).thenAnswer(inv -> inv.getArgument(0));

        staffService.checkin(staffUser, 60L);

        assertEquals(1, savedHistories.size());
        AppointmentStatusHistory history = savedHistories.get(0);
        assertEquals("PENDING", history.getOldStatus());
        assertEquals("CONFIRMED", history.getNewStatus());
        assertEquals(staffUser, history.getChangedBy());
        assertEquals("Check-in tại quầy", history.getReason());
    }

    @Test
    @DisplayName("7. Hoàn tác check-in → có dòng CONFIRMED → PENDING")
    void test7_HoanTacCheckin_ConfirmedSangPending() {
        Appointment appt = Appointment.builder()
                .id(70L)
                .bookingCode("CT-PK-26-2311-070")
                .status(AppointmentStatus.CONFIRMED)
                .build();
        when(appointmentRepository.findById(70L)).thenReturn(Optional.of(appt));
        when(checkinRepository.findByAppointmentId(70L)).thenReturn(Optional.empty());

        staffService.undoCheckin(staffUser, 70L);

        assertEquals(1, savedHistories.size());
        AppointmentStatusHistory history = savedHistories.get(0);
        assertEquals("CONFIRMED", history.getOldStatus());
        assertEquals("PENDING", history.getNewStatus());
        assertEquals(staffUser, history.getChangedBy());
        assertEquals("Hoàn tác check-in", history.getReason());
    }

    @Test
    @DisplayName("8. Task no-show → changedBy = null, lý do 'Không đến khám (tự động)'")
    void test8_TaskNoShow_ChangedByNull_LyDoTuDong() {
        LocalDate today = LocalDate.of(2026, 11, 23);
        Appointment appt = Appointment.builder()
                .id(80L)
                .bookingCode("CT-PK-26-2311-080")
                .appointmentDate(today)
                .status(AppointmentStatus.PENDING)
                .timeSlot(TimeSlot.builder().startTime(LocalTime.of(7, 0)).endTime(LocalTime.of(8, 0)).build())
                .build();

        when(clinicSettingService.getNoShowCancelAt()).thenReturn(LocalTime.of(8, 0));
        when(appointmentRepository.findByAppointmentDateAndStatus(today, AppointmentStatus.PENDING))
                .thenReturn(List.of(appt));

        ClinicScheduledTasks tasks = new ClinicScheduledTasks(
                appointmentRepository,
                appointmentSeatRepository,
                mock(RefreshTokenRepository.class),
                mock(OtpVerificationRepository.class),
                clinicSettingService,
                notificationService,
                appointmentStateService,
                clock
        );

        tasks.autoCancelNoShows();

        assertEquals(1, savedHistories.size());
        AppointmentStatusHistory history = savedHistories.get(0);
        assertEquals("PENDING", history.getOldStatus());
        assertEquals("NO_SHOW", history.getNewStatus());
        assertNull(history.getChangedBy());
        assertEquals("Không đến khám (tự động)", history.getReason());
    }

    @Test
    @DisplayName("9. Chuyển đổi sai (ví dụ COMPLETED → CONFIRMED) → 422")
    void test9_ChuyenDoiSai_CompletedSangConfirmed_422() {
        Appointment appt = Appointment.builder()
                .id(90L)
                .status(AppointmentStatus.COMPLETED)
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                appointmentStateService.transition(appt, AppointmentStatus.CONFIRMED, staffUser, "Test chuyển sai"));

        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Không thể chuyển từ COMPLETED sang CONFIRMED"));
    }

    @Test
    @DisplayName("10. Phiếu NO_SHOW → history trả newStatus = 'NO_SHOW', newStatusLabel = 'Không đến'")
    void test10_PhieuNoShow_HistoryTraNoShowVaKhongDen() {
        Appointment appt = Appointment.builder()
                .id(100L)
                .bookingCode("CT-PK-26-2311-100")
                .status(AppointmentStatus.NO_SHOW)
                .build();
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appt));

        AppointmentStatusHistory noShowHistory = AppointmentStatusHistory.builder()
                .id(1L)
                .appointment(appt)
                .oldStatus("PENDING")
                .newStatus("NO_SHOW")
                .changedBy(null)
                .reason("Không đến khám (tự động)")
                .changedAt(clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime())
                .build();

        when(statusHistoryRepository.findByAppointmentIdOrderByChangedAtAsc(100L))
                .thenReturn(List.of(noShowHistory));

        AppointmentHistoryResponse response = staffService.getAppointmentHistory(100L);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        var item = response.items().get(0);
        assertEquals("PENDING", item.oldStatus());
        assertEquals("Chưa đến", item.oldStatusLabel());
        assertEquals("NO_SHOW", item.newStatus(), "Phải giữ nguyên DB enum NO_SHOW, không được map thành CANCELLED");
        assertEquals("Không đến", item.newStatusLabel(), "Label phải là 'Không đến', không bị gộp thành 'Đã huỷ'");
        assertEquals("Hệ thống", item.actorName());
        assertEquals("Không đến khám (tự động)", item.reason());
    }
}
