package com.chuctupk.controller;

import com.chuctupk.dto.request.ArticleRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.ArticleResponse;
import com.chuctupk.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    // Public API
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getPublishedArticles(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ArticleResponse> result = articleService.getPublishedArticles(category, pageable);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách bài viết thành công"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(articleService.getCategories(), "Lấy danh mục thành công"));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getArticleBySlug(slug), "Lấy chi tiết bài viết"));
    }

    // Admin CMS API
    @GetMapping("/admin/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    public ResponseEntity<ApiResponse<Page<ArticleResponse>>> getAllArticlesAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(articleService.getAllArticlesAdmin(pageable), "Lấy danh sách bài viết quản trị"));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    public ResponseEntity<ApiResponse<ArticleResponse>> createArticle(@Valid @RequestBody ArticleRequest request) {
        ArticleResponse created = articleService.createArticle(request);
        return ResponseEntity.created(URI.create("/api/v1/articles/" + created.getSlug()))
                .body(ApiResponse.success(created, "Tạo bài viết thành công"));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    public ResponseEntity<ApiResponse<ArticleResponse>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(articleService.updateArticle(id, request), "Cập nhật bài viết thành công"));
    }

    @PatchMapping("/admin/{id}/pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
    public ResponseEntity<ApiResponse<ArticleResponse>> togglePinArticle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(articleService.togglePinArticle(id), "Ghim / bỏ ghim bài viết thành công"));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa bài viết thành công"));
    }
}

