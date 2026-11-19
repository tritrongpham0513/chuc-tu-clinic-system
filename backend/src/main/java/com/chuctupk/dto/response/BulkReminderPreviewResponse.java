package com.chuctupk.dto.response;

import java.util.List;

public record BulkReminderPreviewResponse(
        List<ReminderPreviewDto> items,
        List<String> warnings
) {
    public BulkReminderPreviewResponse(List<ReminderPreviewDto> items) {
        this(items, List.of());
    }
}
