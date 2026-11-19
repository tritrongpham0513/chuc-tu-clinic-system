package com.chuctupk.dto.request;

import java.util.List;

public record BulkReminderMarkSentRequest(
        List<Long> ids,
        String note
) {
    public BulkReminderMarkSentRequest(List<Long> ids) {
        this(ids, null);
    }

    public BulkReminderMarkSentRequest() {
        this(List.of(), null);
    }
}
