package com.chuctupk.service;

import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.AppointmentStatusHistory;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.AppointmentStatusHistoryRepository;
import com.chuctupk.service.impl.AppointmentStateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppointmentTransitionTest {

    private AppointmentRepository appointmentRepository;
    private AppointmentStatusHistoryRepository statusHistoryRepository;
    private Clock clock;
    private AppointmentStateService appointmentStateService;

    private User staffUser;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        statusHistoryRepository = mock(AppointmentStatusHistoryRepository.class);
        clock = Clock.fixed(Instant.parse("2026-11-23T08:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        appointmentStateService = new AppointmentStateServiceImpl(
                appointmentRepository,
                statusHistoryRepository,
                clock
        );

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        staffUser = User.builder()
                .id(99L)
                .fullName("Lễ tân Nguyễn A")
                .role(Role.STAFF)
                .build();
    }

    @Test
    @DisplayName("Chuyển đổi hợp lệ: PENDING -> CONFIRMED (Check-in)")
    void testPendingToConfirmed() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment result = appointmentStateService.transition(appt, AppointmentStatus.CONFIRMED, staffUser, "Check-in tại quầy");

        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        assertNotNull(result.getConfirmedAt());
        assertNotNull(result.getCheckInAt());
        assertEquals(staffUser, result.getConfirmedBy());

        ArgumentCaptor<AppointmentStatusHistory> captor = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        AppointmentStatusHistory history = captor.getValue();
        assertEquals("PENDING", history.getOldStatus());
        assertEquals("CONFIRMED", history.getNewStatus());
        assertEquals(staffUser, history.getChangedBy());
        assertEquals("Check-in tại quầy", history.getReason());
    }

    @Test
    @DisplayName("Chuyển đổi hợp lệ: CONFIRMED -> PENDING (Hoàn tác check-in)")
    void testConfirmedToPending() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.CONFIRMED)
                .confirmedBy(staffUser)
                .build();

        Appointment result = appointmentStateService.transition(appt, AppointmentStatus.PENDING, staffUser, "Hoàn tác check-in");

        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        assertNull(result.getCheckInAt());
        assertNull(result.getConfirmedBy());
        assertNull(result.getConfirmedAt());

        ArgumentCaptor<AppointmentStatusHistory> captor = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        assertEquals("CONFIRMED", captor.getValue().getOldStatus());
        assertEquals("PENDING", captor.getValue().getNewStatus());
    }

    @Test
    @DisplayName("Chuyển đổi hợp lệ: CONFIRMED -> IN_PROGRESS (Mời vào khám)")
    void testConfirmedToInProgress() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        Appointment result = appointmentStateService.transition(appt, AppointmentStatus.IN_PROGRESS, staffUser, "Mời vào khám");

        assertEquals(AppointmentStatus.IN_PROGRESS, result.getStatus());
        assertNotNull(result.getEnteredAt());

        ArgumentCaptor<AppointmentStatusHistory> captor = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        assertEquals("CONFIRMED", captor.getValue().getOldStatus());
        assertEquals("IN_PROGRESS", captor.getValue().getNewStatus());
    }

    @Test
    @DisplayName("Chuyển đổi hợp lệ: IN_PROGRESS -> COMPLETED (Lập kết quả)")
    void testInProgressToCompleted() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        Appointment result = appointmentStateService.transition(appt, AppointmentStatus.COMPLETED, staffUser, "Khám hoàn tất");

        assertEquals(AppointmentStatus.COMPLETED, result.getStatus());
        ArgumentCaptor<AppointmentStatusHistory> captor = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        assertEquals("IN_PROGRESS", captor.getValue().getOldStatus());
        assertEquals("COMPLETED", captor.getValue().getNewStatus());
    }

    @Test
    @DisplayName("Chuyển đổi hợp lệ: IN_PROGRESS -> CONFIRMED (Hoàn tác mời vào khám)")
    void testInProgressToConfirmedUndoEnter() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        Appointment result = appointmentStateService.transition(appt, AppointmentStatus.CONFIRMED, null, "Hoàn tác mời vào khám");

        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        assertNull(result.getEnteredAt());
        ArgumentCaptor<AppointmentStatusHistory> captor = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        assertEquals("IN_PROGRESS", captor.getValue().getOldStatus());
        assertEquals("CONFIRMED", captor.getValue().getNewStatus());
    }

    @Test
    @DisplayName("Chuyển đổi sai: COMPLETED -> CONFIRMED ném lỗi 422 (Ticket 5 test 9)")
    void testInvalidTransitionCompletedToConfirmedThrows422() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.COMPLETED)
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                appointmentStateService.transition(appt, AppointmentStatus.CONFIRMED, staffUser, "Thử chuyển sai"));

        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Không thể chuyển từ COMPLETED sang CONFIRMED"));
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chuyển đổi sai: CANCELLED -> PENDING ném lỗi 422")
    void testInvalidTransitionCancelledToPendingThrows422() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.CANCELLED)
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                appointmentStateService.transition(appt, AppointmentStatus.PENDING, staffUser, "Thử chuyển sai"));

        assertEquals(422, ex.getStatusCode());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chuyển đổi sai: NO_SHOW -> IN_PROGRESS ném lỗi 422")
    void testInvalidTransitionNoShowToInProgressThrows422() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.NO_SHOW)
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                appointmentStateService.transition(appt, AppointmentStatus.IN_PROGRESS, staffUser, "Thử chuyển sai"));

        assertEquals(422, ex.getStatusCode());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Luồng 9: Đổi bác sĩ trạng thái không đổi (CONFIRMED -> CONFIRMED), vẫn ghi history")
    void testSameStatusDoctorChangeAudit() {
        Appointment appt = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        Appointment result = appointmentStateService.transition(appt, AppointmentStatus.CONFIRMED, staffUser, "Đổi bác sĩ: BS A → BS B");

        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        ArgumentCaptor<AppointmentStatusHistory> captor = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(statusHistoryRepository).save(captor.capture());
        assertEquals("CONFIRMED", captor.getValue().getOldStatus());
        assertEquals("CONFIRMED", captor.getValue().getNewStatus());
        assertEquals("Đổi bác sĩ: BS A → BS B", captor.getValue().getReason());
        assertEquals(staffUser, captor.getValue().getChangedBy());
    }

    @Test
    @DisplayName("Đổi bác sĩ và hoàn tác mời khám không ghi đè mốc thời gian cũ")
    void testSameStatusAndUndoEnterKeepOriginalTimestamps() {
        LocalDateTime originalConfirmedAt = LocalDateTime.of(2026, 11, 20, 8, 0);
        Appointment confirmed = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.CONFIRMED)
                .confirmedAt(originalConfirmedAt)
                .checkInAt(originalConfirmedAt)
                .build();

        appointmentStateService.transition(confirmed, AppointmentStatus.CONFIRMED, staffUser,
                "Đổi bác sĩ: BS A → BS B");

        assertEquals(originalConfirmedAt, confirmed.getConfirmedAt(),
                "Đổi bác sĩ không được ghi đè giờ xác nhận ban đầu");
        assertEquals(originalConfirmedAt, confirmed.getCheckInAt());

        Appointment inProgress = Appointment.builder()
                .id(2L)
                .status(AppointmentStatus.IN_PROGRESS)
                .confirmedAt(originalConfirmedAt)
                .enteredAt(LocalDateTime.of(2026, 11, 20, 8, 30))
                .build();

        appointmentStateService.transition(inProgress, AppointmentStatus.CONFIRMED, staffUser,
                "Hoàn tác mời vào khám");

        assertEquals(originalConfirmedAt, inProgress.getConfirmedAt(),
                "Hoàn tác mời vào khám không được ghi đè giờ xác nhận ban đầu");
        assertNull(inProgress.getEnteredAt());
    }
}
