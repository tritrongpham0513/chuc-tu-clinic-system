package com.chuctupk.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ArticleResponse {
    private Long id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String thumbnailUrl;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String authorName;
    private Boolean isPublished;
    private Boolean isPinned;
    private LocalDateTime publishedAt;
    private Integer viewCount;
    private LocalDateTime createdAt;
}

