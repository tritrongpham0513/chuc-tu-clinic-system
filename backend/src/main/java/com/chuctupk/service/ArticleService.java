package com.chuctupk.service;

import com.chuctupk.dto.request.ArticleRequest;
import com.chuctupk.dto.response.ArticleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ArticleService {
    Page<ArticleResponse> getPublishedArticles(String categorySlug, Pageable pageable);
    ArticleResponse getArticleBySlug(String slug);
    List<Map<String, Object>> getCategories();

    // Admin methods
    Page<ArticleResponse> getAllArticlesAdmin(Pageable pageable);
    ArticleResponse createArticle(ArticleRequest request);
    ArticleResponse updateArticle(Long id, ArticleRequest request);
    ArticleResponse togglePinArticle(Long id);
    void deleteArticle(Long id);
}

