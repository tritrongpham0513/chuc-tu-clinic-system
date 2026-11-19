package com.chuctupk.dto.response;

public record ReminderMarkSentResponse(
        Long appointmentId,
        String reminderSentAt
) {}
