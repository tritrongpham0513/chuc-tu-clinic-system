package com.chuctupk.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReminderPreviewDto(
        Long appointmentId,
        String phone,
        String content,
        String channel,
        String ticketCode,
        String patientName,
        String rawPhone
) {
    public ReminderPreviewDto(Long appointmentId, String phone, String content, String channel) {
        this(appointmentId, phone, content, channel, null, null, null);
    }
}
