package com.chuctupk.dto.response;

public record NoShowPreviewItemDto(
        Long id,
        String bookingCode,
        String ticketCode,
        String patientName,
        String patientPhone,
        String slotTime,
        String doctorName,
        long minutesUntilCancellation
) {}
