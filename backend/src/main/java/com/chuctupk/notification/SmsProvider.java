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
public class SmsProvider implements NotificationProvider {

    private final SmsService smsService;

    @Override
    public String channel() {
        return "SMS";
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        String content = "";
        if (message != null && message.data() != null && message.data().containsKey("message")) {
            Object msgObj = message.data().get("message");
            if (msgObj != null) {
                content = msgObj.toString();
            }
        }

        String phone = message != null ? message.phone() : null;
        if (smsService == null) {
            return new DeliveryResult(DeliveryResult.Status.FAILED, "NO_PROVIDER", "sms");
        }

        return smsService.sendNotification(phone, content);
    }
}
