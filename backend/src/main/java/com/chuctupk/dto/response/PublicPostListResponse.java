package com.chuctupk.dto.response;

import java.util.List;

public record PublicPostListResponse(
        List<PostDto> items,
        PostDto featured,
        long total,
        int pages,
        int page,
        int limit
) {}

