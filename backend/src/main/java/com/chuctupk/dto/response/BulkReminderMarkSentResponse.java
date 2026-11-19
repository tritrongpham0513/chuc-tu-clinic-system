package com.chuctupk.dto.response;

import java.util.List;

public record BulkReminderMarkSentResponse(
        List<Long> updatedAppointmentIds,
        String reminderSentAt
) {}
