package com.chuctupk.controller;

import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.PostDto;
import com.chuctupk.dto.response.PublicPostListResponse;
import com.chuctupk.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Public Post API", description = "Đọc tin tức, cẩm nang sức khỏe và hoạt động thiện nguyện công khai")
public class PublicPostController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "Lấy danh sách bài viết đã xuất bản (hỗ trợ lọc chuyên mục và tách bài nổi bật)")
    public ResponseEntity<ApiResponse<PublicPostListResponse>> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int limit,
            @RequestParam(required = false) Integer featured) {
        boolean includeFeatured = featured != null && featured == 1;
        PublicPostListResponse response = adminService.getPublicPosts(category, page, limit, includeFeatured);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách bài viết thành công"));
    }

    @GetMapping("/{slugOrId}")
    @Operation(summary = "Xem chi tiết bài viết theo mã định danh hoặc slug")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostDetail(@PathVariable String slugOrId) {
        PostDto response = adminService.getPublicPost(slugOrId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("post", response), "Lấy thông tin bài viết thành công"));
    }
}

