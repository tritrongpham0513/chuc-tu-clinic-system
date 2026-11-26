package com.chuctupk.controller;

import com.chuctupk.dto.response.AdminNotificationDto;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Notifications API", description = "Quản trị nhật ký tin nhắn SMS/thông báo và gửi lại tin lỗi")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách nhật ký thông báo (hỗ trợ lọc theo trạng thái status=FAILED và phân trang)")
    public ResponseEntity<ApiResponse<List<AdminNotificationDto>>> getNotifications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int limit) {
        log.info("Admin truy vấn nhật ký thông báo với trạng thái: {}, page={}, limit={}", status, page, limit);
        List<AdminNotificationDto> result = notificationService.getAdminNotifications(status, page, limit);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách nhật ký thông báo thành công"));
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Gửi lại 1 tin nhắn thông báo (tạo dòng log mới, không sửa dòng cũ)")
    public ResponseEntity<ApiResponse<AdminNotificationDto>> resendNotification(@PathVariable Long id) {
        log.info("Admin yêu cầu gửi lại thông báo log ID: {}", id);
        AdminNotificationDto result = notificationService.resendNotification(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Gửi lại thông báo thành công"));
    }
}
