package com.chuctupk.dto.response;

import java.util.List;
import java.util.Map;

public record AdminPostListResponse(
        List<PostDto> items,
        long total,
        int pages,
        int page,
        int limit,
        Map<String, Integer> statusCounts
) {}

