package com.chuctupk.notification;

import com.chuctupk.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("sms")
@ConditionalOnProperty(name = "notification.provider", havingValue = "sms")
@RequiredArgsConstructor
public class SmsNotificationProvider implements NotificationProvider {

    private final SmsService smsService;

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        String content = "";
        if (message.data() != null && message.data().containsKey("message")) {
            Object msgObj = message.data().get("message");
            if (msgObj != null) {
                content = msgObj.toString();
            }
        }
        SmsService.OtpSendResult result = smsService.sendMessage(message.phone(), content);
        return switch (result) {
            case SENT -> new DeliveryResult(DeliveryResult.Status.SENT, null, "sms");
            case SKIPPED -> new DeliveryResult(DeliveryResult.Status.SKIPPED, null, "sms");
            case FAILED -> new DeliveryResult(DeliveryResult.Status.FAILED, "SMS_SEND_FAILED", "sms");
        };
    }
}
