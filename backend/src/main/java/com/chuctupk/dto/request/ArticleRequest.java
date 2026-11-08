package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleRequest {

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    private String title;

    private String slug;

    @NotBlank(message = "Tóm tắt không được để trống")
    private String summary;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private String thumbnailUrl;

    private Long categoryId;

    private Boolean isPublished = true;
}

