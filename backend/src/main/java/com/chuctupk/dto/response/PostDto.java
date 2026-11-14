package com.chuctupk.dto.response;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.Article;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostDto(
        String id,
        String slug,
        String title,
        String summary,
        String excerpt,
        String content,
        String thumbnail,
        String coverImage,
        String category,
        String categoryName,
        String categoryLabel,
        String author,
        boolean pinned,
        String status,
        String publishedAt,
        String updatedAt,
        String createdAt
) {
    public static PostDto from(Article art) {
        if (art == null) return null;
        String catSlug = art.getCategory() != null && art.getCategory().getSlug() != null
                ? art.getCategory().getSlug() : "y-hoc-co-truyen";
        String catName = art.getCategory() != null && art.getCategory().getName() != null
                ? art.getCategory().getName() : "Y học cổ truyền";
        String thumb = art.getThumbnailUrl() != null ? art.getThumbnailUrl() : "";
        String summary = art.getSummary() != null ? art.getSummary() : "";
        String status = Boolean.TRUE.equals(art.getIsPublished()) ? "PUBLISHED" : "DRAFT";

        return new PostDto(
                String.valueOf(art.getId()),
                art.getSlug() != null ? art.getSlug() : "bai-viet-" + art.getId(),
                art.getTitle() != null ? art.getTitle() : "",
                summary,
                summary,
                art.getContent() != null ? art.getContent() : "",
                thumb,
                thumb,
                catSlug,
                catName,
                catName,
                art.getAuthor() != null ? art.getAuthor().getFullName() : "Phòng khám Chúc Từ",
                Boolean.TRUE.equals(art.getIsPinned()),
                status,
                ContractMapper.formatIso(art.getPublishedAt()),
                ContractMapper.formatIso(art.getUpdatedAt()),
                ContractMapper.formatIso(art.getCreatedAt())
        );
    }
}

