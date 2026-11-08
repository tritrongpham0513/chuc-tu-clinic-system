package com.chuctupk.service.impl;

import com.chuctupk.dto.request.ArticleRequest;
import com.chuctupk.dto.response.ArticleResponse;
import com.chuctupk.entity.Article;
import com.chuctupk.entity.ArticleCategory;
import com.chuctupk.entity.User;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.ArticleCategoryRepository;
import com.chuctupk.repository.ArticleRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thông tin người dùng"));
    }

    @Override
    public Page<ArticleResponse> getPublishedArticles(String categorySlug, Pageable pageable) {
        if (categorySlug != null && !categorySlug.isBlank()) {
            ArticleCategory cat = categoryRepository.findBySlug(categorySlug)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy danh mục"));
            return articleRepository.findByCategoryIdAndIsPublishedTrueOrderByPublishedAtDesc(cat.getId(), pageable)
                    .map(this::toResponse);
        }
        return articleRepository.findByIsPublishedTrueOrderByPublishedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ArticleResponse getArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bài viết"));
        article.setViewCount(article.getViewCount() + 1);
        articleRepository.save(article);
        return toResponse(article);
    }

    @Override
    public List<Map<String, Object>> getCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "slug", c.getSlug(),
                        "description", c.getDescription() != null ? c.getDescription() : ""
                ))
                .toList();
    }

    @Override
    public Page<ArticleResponse> getAllArticlesAdmin(Pageable pageable) {
        return articleRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public ArticleResponse createArticle(ArticleRequest request) {
        User author = getCurrentUser();
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = toSlug(request.getTitle());
        }
        if (articleRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        ArticleCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        Article article = Article.builder()
                .title(request.getTitle())
                .slug(slug)
                .summary(request.getSummary())
                .content(request.getContent())
                .thumbnailUrl(request.getThumbnailUrl())
                .category(category)
                .author(author)
                .isPublished(Boolean.TRUE.equals(request.getIsPublished()))
                .publishedAt(Boolean.TRUE.equals(request.getIsPublished()) ? LocalDateTime.now() : null)
                .viewCount(0)
                .build();

        return toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleRequest request) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bài viết"));

        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setThumbnailUrl(request.getThumbnailUrl());

        if (request.getCategoryId() != null) {
            article.setCategory(categoryRepository.findById(request.getCategoryId()).orElse(null));
        }

        if (Boolean.TRUE.equals(request.getIsPublished()) && !Boolean.TRUE.equals(article.getIsPublished())) {
            article.setIsPublished(true);
            article.setPublishedAt(LocalDateTime.now());
        } else if (Boolean.FALSE.equals(request.getIsPublished())) {
            article.setIsPublished(false);
        }

        return toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public ArticleResponse togglePinArticle(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bài viết"));
        boolean currentPin = Boolean.TRUE.equals(article.getIsPinned());
        article.setIsPinned(!currentPin);
        return toResponse(articleRepository.save(article));
    }

    @Override
    @Transactional
    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw AppException.notFound("Không tìm thấy bài viết để xóa");
        }
        articleRepository.deleteById(id);
    }

    private ArticleResponse toResponse(Article a) {
        return ArticleResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .slug(a.getSlug())
                .summary(a.getSummary())
                .content(a.getContent())
                .thumbnailUrl(a.getThumbnailUrl())
                .categoryId(a.getCategory() != null ? a.getCategory().getId() : null)
                .categoryName(a.getCategory() != null ? a.getCategory().getName() : null)
                .categorySlug(a.getCategory() != null ? a.getCategory().getSlug() : null)
                .authorName(a.getAuthor() != null ? a.getAuthor().getFullName() : null)
                .isPublished(a.getIsPublished())
                .isPinned(Boolean.TRUE.equals(a.getIsPinned()))
                .publishedAt(a.getPublishedAt())
                .viewCount(a.getViewCount())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}

