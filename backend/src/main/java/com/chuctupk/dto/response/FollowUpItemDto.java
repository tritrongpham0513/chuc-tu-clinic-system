package com.chuctupk.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FollowUpItemDto(
        Long patientId,
        String patientName,
        Integer yearOfBirth,
        String phone,
        String rawPhone,
        LocalDate lastVisitDate,
        LocalDate followUpDate,
        String doctorName,
        Long doctorId,
        String serviceType,
        Long recordId,
        Long appointmentId,
        long daysUntil,
        LocalDateTime reminderSentAt
) {}
