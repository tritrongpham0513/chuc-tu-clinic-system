package com.chuctupk.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record AdminNotificationDto(
        Long id,
        Long appointmentId,
        String bookingCode,
        String patientName,
        String channel,
        String template,
        String templateLabel,
        String recipientMasked,
        String status,
        String errorCode,
        String errorMessage,
        String provider,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
