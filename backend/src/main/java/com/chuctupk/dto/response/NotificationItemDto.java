package com.chuctupk.dto.response;

public record NotificationItemDto(
        String type,
        String title,
        String body,
        Long appointmentId,
        String at,
        boolean read
) {}
