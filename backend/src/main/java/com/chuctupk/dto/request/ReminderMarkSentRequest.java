package com.chuctupk.dto.request;

public record ReminderMarkSentRequest(
        String note
) {
    public ReminderMarkSentRequest() {
        this(null);
    }
}
