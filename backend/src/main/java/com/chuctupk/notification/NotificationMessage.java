package com.chuctupk.notification;

import java.util.Map;

public record NotificationMessage(
        Long appointmentId,
        String phone,
        String template,
        Map<String, Object> data
) {}
