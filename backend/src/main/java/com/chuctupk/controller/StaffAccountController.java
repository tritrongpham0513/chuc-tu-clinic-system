package com.chuctupk.controller;

import com.chuctupk.dto.request.ChangeStaffEmailRequest;
import com.chuctupk.dto.request.UpdateStaffAccountRequest;
import com.chuctupk.dto.request.VerifyStaffEmailRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.UserDto;
import com.chuctupk.entity.User;
import com.chuctupk.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thông tin tài khoản của nhân viên nội bộ (Admin / Lễ tân / Bác sĩ).
 *
 * <p>Ba thao tác: xem thông tin, đổi họ tên, đổi email (bắt buộc xác minh bằng mã 6 số).
 * Mật khẩu đổi qua {@code POST /api/v1/auth/change-password}.</p>
 */
@RestController
@RequestMapping("/api/v1/staff/account")
@RequiredArgsConstructor
@Tag(name = "Tài khoản nội bộ (Staff Account v1)",
        description = "Xem và cập nhật họ tên, email của tài khoản lễ tân / bác sĩ / quản trị viên")
public class StaffAccountController {

    private final AuthService authService;

    @GetMapping
    @Operation(summary = "Xem thông tin tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccount(
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", authService.getCurrentUserDto(currentUser));
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin tài khoản thành công"));
    }

    @PatchMapping
    @Operation(summary = "Cập nhật họ tên của tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAccount(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateStaffAccountRequest request) {
        UserDto updated = authService.updateStaffAccountName(currentUser, request.getFullName());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", updated);
        return ResponseEntity.ok(ApiResponse.ok(data, "Đã lưu thay đổi thông tin tài khoản"));
    }

    @PostMapping("/email")
    @Operation(summary = "Yêu cầu đổi email — gửi mã xác minh 6 số tới email mới")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestEmailChange(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangeStaffEmailRequest request) {
        Map<String, Object> data = authService.requestStaffEmailChange(currentUser, request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(data, "Mã xác minh đã được gửi tới email mới"));
    }

    @PostMapping("/email/verify")
    @Operation(summary = "Xác minh mã 6 số và cập nhật email mới")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmailChange(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody VerifyStaffEmailRequest request) {
        Map<String, Object> data = authService.verifyStaffEmailChange(currentUser, request.getCode());
        return ResponseEntity.ok(ApiResponse.ok(data, "Đã xác minh và cập nhật email mới"));
    }
}
