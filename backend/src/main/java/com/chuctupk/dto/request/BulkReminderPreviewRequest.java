package com.chuctupk.dto.request;

import java.util.List;

public record BulkReminderPreviewRequest(
        List<Long> ids
) {
    public BulkReminderPreviewRequest() {
        this(List.of());
    }
}
