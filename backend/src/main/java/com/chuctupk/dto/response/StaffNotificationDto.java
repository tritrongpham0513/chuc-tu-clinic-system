package com.chuctupk.dto.response;

import com.chuctupk.entity.UserNotification;

import java.time.format.DateTimeFormatter;

public record StaffNotificationDto(
        Long id,
        String type,
        String title,
        String body,
        String link,
        Long appointmentId,
        String bookingCode,
        String at,
        boolean read
) {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static StaffNotificationDto from(UserNotification entity) {
        return new StaffNotificationDto(
                entity.getId(),
                entity.getType().name(),
                entity.getTitle(),
                entity.getBody(),
                entity.getLink(),
                entity.getAppointment() != null ? entity.getAppointment().getId() : null,
                entity.getAppointment() != null ? entity.getAppointment().getBookingCode() : null,
                entity.getCreatedAt() != null ? entity.getCreatedAt().format(ISO_FORMATTER) : null,
                entity.getReadAt() != null
        );
    }
}
