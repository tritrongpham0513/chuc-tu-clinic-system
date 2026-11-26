package com.chuctupk.dto.response;

import java.util.List;

public record StaffNotificationListResponse(
        List<StaffNotificationDto> items,
        int page,
        int limit,
        long total,
        long unread
) {
}
