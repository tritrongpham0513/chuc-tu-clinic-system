package com.chuctupk.dto.response;

import java.util.List;

public record FollowUpListResponse(
        List<FollowUpItemDto> items,
        int total,
        int overdueCount,
        int todayCount,
        int upcomingCount
) {}
