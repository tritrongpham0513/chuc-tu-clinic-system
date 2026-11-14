package com.chuctupk.controller;

import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.dto.response.UserDto;
import com.chuctupk.dto.response.UserResponse;
import com.chuctupk.entity.User;
import com.chuctupk.exception.AppException;
import com.chuctupk.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Xác thực & Phiên làm việc (Auth v1)", description = "Các API đăng ký, đăng nhập OTP, đăng nhập nội bộ, đổi mật khẩu và quản lý phiên")
public class AuthController {

    private final AuthService authService;

    // 1. Lấy thông tin phiên hiện tại (không ném 401 khi chưa đăng nhập)
    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản đang đăng nhập (trả về user: null nếu khách)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMe(@AuthenticationPrincipal User currentUser) {
        UserDto userDto = authService.getCurrentUserDto(currentUser);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", userDto);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin tài khoản thành công"));
    }

    // 2. Đăng nhập bệnh nhân (bước 1: gửi OTP)
    @PostMapping("/login")
    @Operation(summary = "Yêu cầu gửi mã OTP đăng nhập cho bệnh nhân")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @RequestBody Map<String, Object> body) {
        // Hỗ trợ cả đăng nhập bằng mật khẩu (nếu có trường password)
        if (body.containsKey("password") && body.get("password") != null && !body.get("password").toString().isBlank()) {
            String usernameOrPhone = (String) (body.get("usernameOrPhone") != null ? body.get("usernameOrPhone") : body.get("phone"));
            AuthResponse authRes = authService.login(new LoginRequest(usernameOrPhone, (String) body.get("password")));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("user", authRes.getUser());
            data.put("token", authRes.getAccessToken());
            return ResponseEntity.ok(ApiResponse.ok(data, "Đăng nhập thành công"));
        }

        String phone = (String) (body.get("phone") != null ? body.get("phone") : body.get("usernameOrPhone"));
        if (phone == null || phone.isBlank()) {
            throw AppException.badRequest("Cô chú vui lòng nhập số điện thoại");
        }
        Map<String, Object> data = authService.requestPatientLoginOtp(phone);
        return ResponseEntity.ok(ApiResponse.ok(data, "Mã xác thực đã được gửi đến số điện thoại"));
    }

    // 3. Đăng nhập bệnh nhân (bước 2: xác thực OTP)
    @PostMapping("/login/verify")
    @Operation(summary = "Xác thực mã OTP đăng nhập bệnh nhân")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyLogin(
            @Valid @RequestBody LoginVerifyRequest request,
            HttpServletResponse response) {
        Map<String, Object> data = authService.verifyPatientLogin(request, response);
        return ResponseEntity.ok(ApiResponse.ok(data, "Đăng nhập thành công"));
    }

    // 4. Đăng ký tài khoản bệnh nhân (bước 1: lưu thông tin & gửi OTP)
    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản bệnh nhân mới (gửi mã OTP)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody PatientRegisterRequest request) {
        Map<String, Object> data = authService.requestPatientRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Mã xác thực đăng ký đã được gửi"));
    }

    // 5. Đăng ký tài khoản bệnh nhân (bước 2: xác thực OTP & tạo hồ sơ)
    @PostMapping("/register/verify")
    @Operation(summary = "Xác thực mã OTP hoàn tất đăng ký tài khoản")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyRegister(
            @Valid @RequestBody RegisterVerifyRequest request,
            HttpServletResponse response) {
        Map<String, Object> data = authService.verifyPatientRegister(request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data, "Đăng ký tài khoản thành công"));
    }

    // 6. Gửi lại mã OTP
    @PostMapping("/otp/resend")
    @Operation(summary = "Gửi lại mã OTP qua SMS (cooldown 60 giây)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendOtp(
            @Valid @RequestBody OtpResendRequest request) {
        Map<String, Object> data = authService.resendOtp(request.getPhone(), request.getPurpose());
        return ResponseEntity.ok(ApiResponse.ok(data, "Đã gửi lại mã xác nhận"));
    }

    // 7. Đăng nhập nhân viên nội bộ
    @PostMapping("/staff/login")
    @Operation(summary = "Đăng nhập dành cho Bác sĩ, Điều dưỡng, Tiếp tân, Quản trị viên")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginStaff(
            @Valid @RequestBody StaffLoginRequest request,
            HttpServletResponse response) {
        Map<String, Object> data = authService.loginStaff(request, response);
        return ResponseEntity.ok(ApiResponse.ok(data, "Đăng nhập thành công"));
    }

    // 8. Nhân viên quên mật khẩu (bước 1: gửi mã xác thực)
    @PostMapping("/staff/forgot-password")
    @Operation(summary = "Nhân viên yêu cầu mã khôi phục mật khẩu qua Email / SMS")
    public ResponseEntity<ApiResponse<Map<String, Object>>> staffForgotPassword(
            @RequestBody ForgotPasswordRequest request) {
        Map<String, Object> data = authService.staffForgotPassword(request.getEffectiveIdentifier());
        return ResponseEntity.ok(ApiResponse.ok(data, "Mã xác thực đã được gửi"));
    }

    // 9. Nhân viên quên mật khẩu (bước 2: xác thực mã lấy reset token)
    @PostMapping("/staff/forgot-password/verify")
    @Operation(summary = "Xác thực mã OTP khôi phục mật khẩu nhân viên")
    public ResponseEntity<ApiResponse<Map<String, Object>>> staffVerifyForgotPassword(
            @Valid @RequestBody StaffForgotPasswordVerifyRequest request) {
        Map<String, Object> data = authService.staffVerifyForgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Xác thực mã thành công"));
    }

    // 10. Nhân viên đặt lại mật khẩu bằng reset token
    @PostMapping("/staff/reset-password")
    @Operation(summary = "Đặt lại mật khẩu nhân viên bằng reset token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> staffResetPassword(
            @Valid @RequestBody StaffResetPasswordRequest request) {
        Map<String, Object> data = authService.staffResetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Đặt lại mật khẩu thành công"));
    }

    // 11. Đổi mật khẩu tài khoản hiện tại
    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản hiện tại")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        UserDto updated = authService.changeUserPassword(currentUser, request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", updated);
        data.put("message", "Đổi mật khẩu thành công");
        return ResponseEntity.ok(ApiResponse.ok(data, "Đổi mật khẩu thành công"));
    }

    // 12. Làm mới Access Token (Rotate Refresh Token)
    @RequestMapping(value = "/refresh", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Làm mới Access Token bằng Refresh Token (Cookie hoặc Header)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        Map<String, Object> data = authService.refreshToken(request, response);
        return ResponseEntity.ok(ApiResponse.ok(data, "Làm mới phiên thành công"));
    }

    // 13. Đăng xuất
    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản và thu hồi Refresh Token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(currentUser, request, response);
        return ResponseEntity.ok(ApiResponse.ok(Collections.singletonMap("success", true), "Đăng xuất thành công"));
    }

    // =========================================================================
    // CÁC ENDPOINT BỔ TRỢ & TƯƠNG THÍCH CŨ
    // =========================================================================

    @PostMapping("/send-otp")
    @Operation(summary = "Gửi mã xác thực OTP qua SMS (tương thích cũ)")
    public ResponseEntity<ApiResponse<Void>> sendOtpLegacy(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Mã xác thực OTP đã được gửi đến số điện thoại"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xác thực mã OTP (tương thích cũ)")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtpLegacy(@Valid @RequestBody VerifyOtpRequest request) {
        boolean verified = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(verified, "Xác thực mã OTP thành công"));
    }

    @PostMapping("/quick-login")
    @Operation(summary = "Đăng nhập nhanh trên thiết bị đã ghi nhớ bằng số điện thoại")
    public ResponseEntity<ApiResponse<AuthResponse>> quickLoginLegacy(@Valid @RequestBody SendOtpRequest request) {
        AuthResponse response = authService.quickLoginByPhone(request.getPhone());
        return ResponseEntity.ok(ApiResponse.ok(response, "Đăng nhập thành công"));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin tài khoản người dùng")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserResponse response = authService.updateMyProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật thông tin tài khoản thành công"));
    }

    @PutMapping("/profile")
    @Operation(summary = "Nhân sự cập nhật họ tên & email nội bộ")
    public ResponseEntity<ApiResponse<UserResponse>> updateStaffProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateStaffProfileRequest request) {
        UserResponse response = authService.updateStaffProfile(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật thông tin nội bộ thành công"));
    }
}
