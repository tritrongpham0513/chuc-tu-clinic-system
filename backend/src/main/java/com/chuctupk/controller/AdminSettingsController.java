package com.chuctupk.controller;

import com.chuctupk.dto.response.AdminSettingsResponse;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.User;
import com.chuctupk.service.AdminSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Settings API", description = "Quản lý và điều chỉnh các tham số cấu hình vận hành phòng khám")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    private void checkAdmin(User currentUser) {
        if (currentUser == null || currentUser.getRole() != com.chuctupk.entity.enums.Role.ADMIN) {
            throw com.chuctupk.exception.AppException.forbidden("Bạn không có quyền thực hiện thao tác này");
        }
    }

    @GetMapping
    @Operation(summary = "Lấy toàn bộ cấu hình phòng khám kèm metadata để dựng giao diện")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> getSettings(
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminSettingsResponse response = adminSettingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách cấu hình phòng khám thành công"));
    }

    @PutMapping
    @Operation(summary = "Cập nhật các tham số cấu hình phòng khám (hỗ trợ cập nhật từng phần hoặc toàn bộ)")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> updateSettings(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        String username = currentUser != null ? currentUser.getUsername() : "admin";
        AdminSettingsResponse response = adminSettingsService.updateSettings(body, username);
        String msg = (response.getWarnings() != null && !response.getWarnings().isEmpty())
                ? "Đã lưu cấu hình phòng khám kèm cảnh báo"
                : "Cập nhật cấu hình phòng khám thành công";
        return ResponseEntity.ok(ApiResponse.success(response, msg));
    }
}
