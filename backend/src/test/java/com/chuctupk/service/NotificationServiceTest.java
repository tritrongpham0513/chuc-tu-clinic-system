package com.chuctupk.service;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.NotificationLog;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.notification.DeliveryResult;
import com.chuctupk.notification.NotificationProvider;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.NotificationLogRepository;
import com.chuctupk.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private AppointmentRepository appointmentRepository;
    private NotificationLogRepository notificationLogRepository;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        notificationLogRepository = mock(NotificationLogRepository.class);
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant instant = LocalDateTime.of(2026, 11, 20, 8, 0, 0).atZone(zone).toInstant();
        fixedClock = Clock.fixed(instant, zone);
    }

    private Appointment createTestAppointment(Long id, String code, String phone, LocalDate date, LocalTime startTime) {
        TimeSlot slot = TimeSlot.builder()
                .id(1L)
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .build();

        Patient patient = Patient.builder()
                .id(100L)
                .fullName("Nguyễn Văn A")
                .phone(phone)
                .build();

        return Appointment.builder()
                .id(id)
                .bookingCode(code)
                .patient(patient)
                .bookerPhone(phone)
                .appointmentDate(date)
                .timeSlot(slot)
                .status(AppointmentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Ticket 1 & 6: Chọn provider theo cấu hình property")
    void testProviderSelectionByProperty() {
        NotificationProvider logProvider = mock(NotificationProvider.class);
        when(logProvider.channel()).thenReturn("LOG");
        when(logProvider.send(any())).thenReturn(new DeliveryResult(DeliveryResult.Status.SKIPPED, null, "log"));

        NotificationProvider smsProvider = mock(NotificationProvider.class);
        when(smsProvider.channel()).thenReturn("SMS");
        when(smsProvider.send(any())).thenReturn(new DeliveryResult(DeliveryResult.Status.SENT, null, "sms"));

        Map<String, NotificationProvider> providers = Map.of(
                "log", logProvider,
                "sms", smsProvider
        );

        // 1. Cấu hình provider = sms -> gọi smsProvider
        NotificationServiceImpl smsService = new NotificationServiceImpl(
                providers, appointmentRepository, notificationLogRepository, fixedClock, "sms"
        );
        Appointment apt1 = createTestAppointment(1L, "CT-PK-01", "0912345678", LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));
        smsService.notifyBookingSuccess(apt1);

        verify(smsProvider, times(1)).send(any());
        verify(logProvider, never()).send(any());

        // 2. Cấu hình provider = log -> gọi logProvider
        NotificationServiceImpl logService = new NotificationServiceImpl(
                providers, appointmentRepository, notificationLogRepository, fixedClock, "log"
        );
        Appointment apt2 = createTestAppointment(2L, "CT-PK-02", "0912345678", LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));
        logService.notifyBookingSuccess(apt2);

        verify(logProvider, times(1)).send(any());
    }

    @Test
    @DisplayName("Ticket 3 & 6: Ba luồng ghi đúng template vào notification_log")
    void testThreeFlowsLogCorrectTemplates() {
        NotificationProvider provider = mock(NotificationProvider.class);
        when(provider.channel()).thenReturn("LOG");
        when(provider.send(any())).thenReturn(new DeliveryResult(DeliveryResult.Status.SKIPPED, null, "log"));

        Map<String, NotificationProvider> providers = Map.of("log", provider);
        NotificationServiceImpl service = new NotificationServiceImpl(
                providers, appointmentRepository, notificationLogRepository, fixedClock, "log"
        );

        Appointment apt = createTestAppointment(10L, "CT-PK-26-2011-001", "0912345678", LocalDate.of(2026, 11, 20), LocalTime.of(10, 0));

        // 1. Luồng đặt lịch thành công -> BOOKING_SUCCESS
        service.notifyBookingSuccess(apt);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(1)).save(captor.capture());
        NotificationLog log1 = captor.getValue();
        assertThat(log1.getTemplate()).isEqualTo("BOOKING_SUCCESS");
        assertThat(log1.getRecipientMasked()).isEqualTo(PhoneValidator.mask("0912345678"));
        assertThat(log1.getRecipientMasked()).doesNotContain("0912345678");
        assertThat(log1.getStatus()).isEqualTo("SKIPPED");
        assertThat(log1.getChannel()).isEqualTo("LOG");

        // 2. Luồng hủy lịch -> CANCELLED
        service.notifyCancellation(apt, "Bệnh nhân có việc bận");
        verify(notificationLogRepository, times(2)).save(captor.capture());
        NotificationLog log2 = captor.getValue();
        assertThat(log2.getTemplate()).isEqualTo("CANCELLED");
        assertThat(log2.getRecipientMasked()).isEqualTo(PhoneValidator.mask("0912345678"));

        // 3. Luồng nhắc trước 2h -> REMINDER_2H
        // fixedClock tại 08:00:00 -> cửa sổ [now + 105m, now + 135m] = [09:45, 10:15].
        // Slot lúc 10:00 nằm trong khoảng này.
        LocalDate today = LocalDate.now(fixedClock);
        when(appointmentRepository.findByAppointmentDateAndStatus(today, AppointmentStatus.PENDING))
                .thenReturn(List.of(apt));

        service.sendUpcomingReminders();
        verify(notificationLogRepository, times(3)).save(captor.capture());
        NotificationLog log3 = captor.getValue();
        assertThat(log3.getTemplate()).isEqualTo("REMINDER_2H");
        assertThat(log3.getRecipientMasked()).isEqualTo(PhoneValidator.mask("0912345678"));
    }

    @Test
    @DisplayName("Ticket 3 & 6: Lỗi provider hoặc lỗi ghi log không làm hỏng luồng gọi; errorCode được cắt <= 100 và mask SĐT")
    void testProviderFailureDoesNotBreakWorkflow() {
        NotificationProvider faultyProvider = mock(NotificationProvider.class);
        when(faultyProvider.channel()).thenReturn("SMS");
        // Giả lập message lỗi dài hơn 100 ký tự và chứa SĐT thô từ SMS aggregator
        String longErrorWithPhone = "Lỗi cổng SMS khi gửi tới 0987654321: Mã phản hồi HTTP 504 Gateway Timeout từ nhà mạng viễn thông Viettel, vui lòng thử lại sau ít phút hoặc liên hệ hỗ trợ.";
        when(faultyProvider.send(any())).thenThrow(new RuntimeException(longErrorWithPhone));

        Map<String, NotificationProvider> providers = Map.of("sms", faultyProvider);
        NotificationServiceImpl service = new NotificationServiceImpl(
                providers, appointmentRepository, notificationLogRepository, fixedClock, "sms"
        );

        Appointment apt = createTestAppointment(20L, "CT-PK-ERR", "0987654321", LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));

        // Không ném ngoại lệ dù provider ném RuntimeException
        assertThatCode(() -> service.notifyBookingSuccess(apt)).doesNotThrowAnyException();

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(1)).save(captor.capture());
        NotificationLog errorLog = captor.getValue();
        assertThat(errorLog.getStatus()).isEqualTo("FAILED");
        // Khẳng định error_code không vượt quá 100 ký tự
        assertThat(errorLog.getErrorCode().length()).isLessThanOrEqualTo(100);
        // Khẳng định SĐT thô đã bị mask trong error_code
        assertThat(errorLog.getErrorCode()).doesNotContain("0987654321");
        assertThat(errorLog.getErrorCode()).contains("0987 *** 321");

        // Giả lập tiếp: nếu cả repository.save cũng bị lỗi, service vẫn không làm hỏng luồng
        doThrow(new RuntimeException("Lỗi CSDL khi ghi log")).when(notificationLogRepository).save(any());
        assertThatCode(() -> service.notifyBookingSuccess(apt)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cấu hình provider không hợp lệ -> fallback về log và cảnh báo")
    void testInvalidProviderFallbackToLog() {
        NotificationProvider logProvider = mock(NotificationProvider.class);
        when(logProvider.channel()).thenReturn("LOG");
        when(logProvider.send(any())).thenReturn(new DeliveryResult(DeliveryResult.Status.SKIPPED, null, "log"));

        Map<String, NotificationProvider> providers = Map.of("log", logProvider);
        // Cấu hình sai: zns chưa hỗ trợ ở Sprint 1
        NotificationServiceImpl service = new NotificationServiceImpl(
                providers, appointmentRepository, notificationLogRepository, fixedClock, "zns"
        );

        Appointment apt = createTestAppointment(30L, "CT-PK-ZNS", "0912345678", LocalDate.of(2026, 11, 20), LocalTime.of(8, 0));
        service.notifyBookingSuccess(apt);

        verify(logProvider, times(1)).send(any());
    }
}
