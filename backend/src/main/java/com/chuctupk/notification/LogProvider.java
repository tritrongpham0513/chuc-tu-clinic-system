package com.chuctupk.notification;

import com.chuctupk.common.PhoneValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("log")
public class LogProvider implements NotificationProvider {

    @Override
    public String channel() {
        return "LOG";
    }

    @Override
    public DeliveryResult send(NotificationMessage message) {
        String masked = PhoneValidator.mask(message.phone());
        log.info("[LOG NOTIF] Gửi thông báo tới SĐT {} (template: {}, appointmentId: {})",
                masked, message.template(), message.appointmentId());
        return new DeliveryResult(DeliveryResult.Status.SKIPPED, null, "log");
    }
}
