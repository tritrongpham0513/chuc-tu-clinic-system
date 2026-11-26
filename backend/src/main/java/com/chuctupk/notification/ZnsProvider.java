package com.chuctupk.notification;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Khung NotificationProvider cho Zalo ZNS (S12-4).
 * Hỗ trợ ánh xạ template phòng khám sang template ZNS và tự động fallback sang SMS
 * khi ZNS chưa kích hoạt hoặc chưa duyệt mẫu tin.
 */
@Slf4j
@Component("zns")
@ConditionalOnProperty(name = "notification.provider", havingValue = "zns")
public class ZnsProvider implements NotificationProvider {

    public record ZnsTemplatePayload(String templateId, Map<String, String> templateData) {}

    private final SmsService smsService;

    @Value("${app.zns.enabled:false}")
    private boolean znsEnabled;

    @Value("${app.zns.oa-id:}")
    private String oaId;

    @Value("${app.zns.access-token:}")
    private String accessToken;

    @Value("${app.zns.template-booking-success:}")
    private String templateBookingSuccess;

    @Value("${app.zns.template-cancelled:}")
    private String templateCancelled;

    @Value("${app.zns.template-reminder:}")
    private String templateReminder;

    @Value("${app.zns.fallback-sms:true}")
    private boolean fallbackSms = true;

    @Autowired
    public ZnsProvider(@Autowired(required = false) SmsService smsService) {
        this.smsService = smsService;
    }

    @Override
    public String channel() {
        return "ZNS";
    }

    public ZnsTemplatePayload mapTemplate(NotificationMessage message) {
        if (message == null) {
            return new ZnsTemplatePayload("", Map.of());
        }
        String tpl = message.template() != null ? message.template().trim().toUpperCase() : "";
        String templateId = switch (tpl) {
            case "BOOKING_SUCCESS" -> templateBookingSuccess;
            case "CANCELLED" -> templateCancelled;
            case "REMINDER_2H" -> templateReminder;
            default -> "";
        };

        Map<String, String> templateData = new LinkedHashMap<>();
        if (message.data() != null) {
            putIfPresent(templateData, "booking_code", message.data().get("code"));
            putIfPresent(templateData, "date", message.data().get("date"));
            putIfPresent(templateData, "time", message.data().get("time"));
            putIfPresent(templateData, "reason", message.data().get("reason"));
        }
        return new ZnsTemplatePayload(templateId != null ? templateId.trim() : "", templateData);
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        String phone = message != null ? message.phone() : null;
        if (!PhoneValidator.isValid(phone)) {
            log.warn("[ZNS] Số điện thoại không đúng định dạng, bỏ qua gửi ZNS: {}", PhoneValidator.mask(phone));
            return new DeliveryResult(DeliveryResult.Status.SKIPPED, "INVALID_PHONE", "zns");
        }

        ZnsTemplatePayload znsPayload = mapTemplate(message);
        boolean ready = znsEnabled
                && oaId != null && !oaId.isBlank()
                && accessToken != null && !accessToken.isBlank()
                && !znsPayload.templateId().isBlank();

        if (!ready) {
            String reasonCode = !znsEnabled ? "ZNS_DISABLED" : "ZNS_TEMPLATE_MISSING";
            log.info("[ZNS] Cổng ZNS chưa sẵn sàng ({}) cho mẫu {}, fallbackSms={}",
                    reasonCode, message != null ? message.template() : "", fallbackSms);
            return fallbackToSmsOrSkip(message, reasonCode);
        }

        // Khung tích hợp Zalo ZNS API (chờ duyệt Zalo OA & template_id ở giai đoạn sau S12)
        try {
            DeliveryResult znsResult = sendViaZnsGateway(phone, znsPayload);
            if (znsResult.status() == DeliveryResult.Status.SENT) {
                return znsResult;
            }
            if (fallbackSms) {
                log.warn("[ZNS] Gửi ZNS thất bại ({}), chuyển sang fallback SMS", znsResult.errorCode());
                return fallbackToSmsOrSkip(message, znsResult.errorCode());
            }
            return znsResult;
        } catch (Exception e) {
            log.warn("[ZNS] Lỗi khi gửi ZNS: {}", e.getMessage());
            if (fallbackSms) {
                return fallbackToSmsOrSkip(message, "ZNS_SEND_FAILED");
            }
            return new DeliveryResult(DeliveryResult.Status.FAILED, "ZNS_SEND_FAILED", "zns");
        }
    }

    protected DeliveryResult sendViaZnsGateway(String phone, ZnsTemplatePayload payload) {
        log.info("[ZNS] Khung gửi ZNS tới SĐT {} với template_id={}", PhoneValidator.mask(phone), payload.templateId());
        return new DeliveryResult(DeliveryResult.Status.SKIPPED, "ZNS_NOT_IMPLEMENTED", "zns");
    }

    private DeliveryResult fallbackToSmsOrSkip(NotificationMessage message, String znsReason) {
        if (!fallbackSms || smsService == null) {
            return new DeliveryResult(DeliveryResult.Status.SKIPPED, znsReason, "zns");
        }

        String smsContent = "";
        if (message != null && message.data() != null && message.data().get("message") != null) {
            smsContent = message.data().get("message").toString();
        }

        DeliveryResult smsResult = smsService.sendNotification(message != null ? message.phone() : null, smsContent);
        String smsProvider = smsResult.providerRef() != null && !smsResult.providerRef().isBlank()
                ? smsResult.providerRef()
                : "sms";
        String combinedRef = "zns->" + smsProvider;
        if (combinedRef.length() > 30) {
            combinedRef = combinedRef.substring(0, 30);
        }

        if (smsResult.status() == DeliveryResult.Status.SENT) {
            return new DeliveryResult(DeliveryResult.Status.SENT, null, combinedRef);
        }
        if (smsResult.status() == DeliveryResult.Status.SKIPPED) {
            String err = smsResult.errorCode() != null ? smsResult.errorCode() : znsReason;
            return new DeliveryResult(DeliveryResult.Status.SKIPPED, err, combinedRef);
        }
        return new DeliveryResult(DeliveryResult.Status.FAILED, smsResult.errorCode(), combinedRef);
    }

    private void putIfPresent(Map<String, String> target, String key, Object val) {
        if (val != null) {
            String s = val.toString().trim();
            if (!s.isBlank()) {
                target.put(key, s);
            }
        }
    }
}
