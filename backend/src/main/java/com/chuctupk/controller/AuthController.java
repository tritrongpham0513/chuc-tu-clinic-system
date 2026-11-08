package com.chuctupk.controller;

import com.chuctupk.dto.request.LoginOtpRequest;
import com.chuctupk.dto.request.LoginRequest;
import com.chuctupk.dto.request.RegisterRequest;
import com.chuctupk.dto.request.SendOtpRequest;
import com.chuctupk.dto.request.VerifyOtpRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.dto.response.UserResponse;
import com.chuctupk.entity.User;
import com.chuctupk.exception.AppException;
import com.chuctupk.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Xác thực & Tài khoản (Auth)", description = "Các API đăng ký, đăng nhập và xác thực OTP qua SMS")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    @Operation(summary = "Gửi mã xác thực OTP qua SMS")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Mã xác thực OTP đã được gửi đến số điện thoại của bạn"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xác thực mã OTP")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean verified = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(verified, "Xác thực mã OTP thành công"));
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản bệnh nhân mới")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Đăng ký tài khoản thành công"));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập tài khoản bằng số điện thoại và mật khẩu")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Đăng nhập thành công"));
    }

    @PostMapping("/login-otp")
    @Operation(summary = "Đăng nhập tài khoản bằng số điện thoại và mã OTP")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithOtp(@Valid @RequestBody LoginOtpRequest request) {
        AuthResponse response = authService.loginWithOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Đăng nhập thành công"));
    }

    @PostMapping("/quick-login")
    @Operation(summary = "Đăng nhập nhanh trên thiết bị đã ghi nhớ bằng số điện thoại")
    public ResponseEntity<ApiResponse<AuthResponse>> quickLogin(@Valid @RequestBody SendOtpRequest request) {
        AuthResponse response = authService.quickLoginByPhone(request.getPhone());
        return ResponseEntity.ok(ApiResponse.ok(response, "Đăng nhập thành công"));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản đang đăng nhập")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw AppException.unauthorized("Chưa đăng nhập hoặc phiên làm việc đã hết hạn");
        }
        UserResponse response = authService.getCurrentUser(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy thông tin tài khoản thành công"));
    }

    @PutMapping("/me")
    @Operation(summary = "F.1.5 - Cập nhật thông tin tài khoản người dùng")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody com.chuctupk.dto.request.UpdateUserProfileRequest request
    ) {
        UserResponse response = authService.updateMyProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật thông tin tài khoản thành công"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "F.3.4 - Đổi mật khẩu tài khoản hiện tại")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody com.chuctupk.dto.request.ChangePasswordRequest request
    ) {
        authService.changePassword(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đổi mật khẩu thành công"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "F.3.3 - Quên mật khẩu: gửi mã OTP xác thực qua SMS")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody com.chuctupk.dto.request.ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Mã xác thực khôi phục mật khẩu đã được gửi đến số điện thoại"));
    }

    @PostMapping("/reset-password-otp")
    @Operation(summary = "F.3.3 - Đặt lại mật khẩu mới bằng mã OTP")
    public ResponseEntity<ApiResponse<Void>> resetPasswordOtp(
            @Valid @RequestBody com.chuctupk.dto.request.ResetPasswordWithOtpRequest request
    ) {
        authService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới"));
    }

    @PutMapping("/profile")
    @Operation(summary = "F.3.5 - Nhân sự cập nhật họ tên & email nội bộ")
    public ResponseEntity<ApiResponse<UserResponse>> updateStaffProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody com.chuctupk.dto.request.UpdateStaffProfileRequest request
    ) {
        UserResponse response = authService.updateStaffProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật thông tin nội bộ thành công"));
    }
}
