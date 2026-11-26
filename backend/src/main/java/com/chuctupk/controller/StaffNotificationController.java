package com.chuctupk.controller;

import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.StaffNotificationDto;
import com.chuctupk.dto.response.StaffNotificationListResponse;
import com.chuctupk.entity.User;
import com.chuctupk.service.StaffNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff/notifications")
@RequiredArgsConstructor
@Tag(name = "Staff Notifications API", description = "Hộp thư thông báo nội bộ cho nhân viên và bác sĩ")
public class StaffNotificationController {

    private final StaffNotificationService staffNotificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo nội bộ của tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<StaffNotificationListResponse>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        StaffNotificationListResponse response = staffNotificationService.getNotifications(currentUser, page, limit);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy danh sách thông báo thành công"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Đếm số thông báo chưa đọc của tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User currentUser
    ) {
        long unread = staffNotificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unread", unread), "Lấy số thông báo chưa đọc thành công"));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo đã đọc")
    public ResponseEntity<ApiResponse<StaffNotificationDto>> markRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("id") Long id
    ) {
        StaffNotificationDto dto = staffNotificationService.markRead(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Đã đánh dấu thông báo đã đọc"));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo của mình đã đọc")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(
            @AuthenticationPrincipal User currentUser
    ) {
        int updated = staffNotificationService.markAllRead(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", updated), "Đã đánh dấu tất cả thông báo đã đọc"));
    }
}
