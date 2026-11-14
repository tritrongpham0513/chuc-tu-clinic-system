package com.chuctupk.dto.request;

public record AdminSavePostRequest(
        String title,
        String content,
        String summary,
        String excerpt,
        String category,
        String status,
        String coverImage,
        Boolean pinned
) {}

