package com.chuctupk.notification;

import com.chuctupk.service.SmsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ZnsProviderTest {

    static class StubSmsService implements SmsService {
        DeliveryResult toReturn = new DeliveryResult(DeliveryResult.Status.SENT, null, "esms");
        int callCount = 0;

        @Override
        public OtpSendResult sendOtp(String phone, String otpCode) {
            return OtpSendResult.SKIPPED;
        }

        @Override
        public DeliveryResult sendNotification(String phone, String message) {
            callCount++;
            return toReturn;
        }
    }

    @Test
    @DisplayName("S12-4: ZnsProvider ánh xạ template BOOKING_SUCCESS, CANCELLED, REMINDER_2H")
    void testTemplateMapping() {
        ZnsProvider zns = new ZnsProvider(new StubSmsService());
        ReflectionTestUtils.setField(zns, "templateBookingSuccess", "tpl_book_01");
        ReflectionTestUtils.setField(zns, "templateCancelled", "tpl_cancel_02");
        ReflectionTestUtils.setField(zns, "templateReminder", "tpl_remind_03");

        NotificationMessage msg = new NotificationMessage(
                1L, "0912345678", "BOOKING_SUCCESS",
                Map.of("code", "CT-001", "date", "25/11/2026", "time", "08:30", "message", "Noi dung SMS")
        );

        ZnsProvider.ZnsTemplatePayload payload = zns.mapTemplate(msg);
        assertThat(payload.templateId()).isEqualTo("tpl_book_01");
        assertThat(payload.templateData())
                .containsEntry("booking_code", "CT-001")
                .containsEntry("date", "25/11/2026")
                .containsEntry("time", "08:30");
    }

    @Test
    @DisplayName("S12-4: ZNS chưa bật + fallbackSms=true -> tự fallback sang SMS")
    void testFallbackToSmsWhenZnsDisabled() {
        StubSmsService stubSms = new StubSmsService();
        ZnsProvider zns = new ZnsProvider(stubSms);
        ReflectionTestUtils.setField(zns, "znsEnabled", false);
        ReflectionTestUtils.setField(zns, "fallbackSms", true);

        NotificationMessage msg = new NotificationMessage(
                1L, "0912345678", "BOOKING_SUCCESS",
                Map.of("code", "CT-001", "message", "Noi dung SMS")
        );

        DeliveryResult result = zns.send(msg);
        assertThat(zns.channel()).isEqualTo("ZNS");
        assertThat(stubSms.callCount).isEqualTo(1);
        assertThat(result.status()).isEqualTo(DeliveryResult.Status.SENT);
        assertThat(result.providerRef()).isEqualTo("zns->esms");
    }

    @Test
    @DisplayName("S12-4: ZNS chưa bật + fallbackSms=false -> trả về SKIPPED (ZNS_DISABLED)")
    void testSkippedWhenZnsDisabledAndNoFallback() {
        StubSmsService stubSms = new StubSmsService();
        ZnsProvider zns = new ZnsProvider(stubSms);
        ReflectionTestUtils.setField(zns, "znsEnabled", false);
        ReflectionTestUtils.setField(zns, "fallbackSms", false);

        NotificationMessage msg = new NotificationMessage(
                1L, "0912345678", "BOOKING_SUCCESS", Map.of("message", "Test")
        );

        DeliveryResult result = zns.send(msg);
        assertThat(stubSms.callCount).isEqualTo(0);
        assertThat(result.status()).isEqualTo(DeliveryResult.Status.SKIPPED);
        assertThat(result.errorCode()).isEqualTo("ZNS_DISABLED");
    }
}
