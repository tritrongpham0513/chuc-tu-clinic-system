package com.chuctupk.dto.response;

public record FollowUpReminderPreviewDto(
        Long recordId,
        String patientName,
        String phone,
        String rawPhone,
        String content,
        String channel,
        String template
) {}
