package com.chuctupk.dto.response;

import java.util.List;
import java.util.Map;

public record AdminAccountListResponse(
        List<AccountDto> items,
        long total,
        int pages,
        int page,
        int limit,
        Map<String, Integer> roleCounts
) {}

