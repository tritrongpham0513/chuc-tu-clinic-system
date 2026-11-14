package com.chuctupk.controller;

import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.User;
import com.chuctupk.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management API", description = "Phân hệ quản trị toàn quyền: Dashboard, tài khoản, bài viết, khoá hồ sơ và upload ảnh")
public class AdminController {

    private final AdminService adminService;

    // =========================================================================
    // 1. DASHBOARD
    // =========================================================================

    @GetMapping("/dashboard")
    @Operation(summary = "Lấy dữ liệu tổng quan cho trang Dashboard Admin")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse response = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy tổng quan dashboard thành công"));
    }

    // =========================================================================
    // 2. TÀI KHOẢN HỆ THỐNG
    // =========================================================================

    @GetMapping("/accounts")
    @Operation(summary = "Tìm kiếm và phân trang danh sách tài khoản hệ thống")
    public ResponseEntity<ApiResponse<AdminAccountListResponse>> getAccounts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        AdminAccountListResponse response = adminService.getAccounts(q, role, page, limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách tài khoản thành công"));
    }

    @GetMapping("/accounts/{id}")
    @Operation(summary = "Xem chi tiết một tài khoản")
    public ResponseEntity<ApiResponse<AccountDto>> getAccount(@PathVariable Long id) {
        AccountDto response = adminService.getAccount(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết tài khoản thành công"));
    }

    @PostMapping("/accounts")
    @Operation(summary = "Tạo tài khoản mới (nhân viên bắt buộc email và mật khẩu tạm ≥ 8 ký tự)")
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(
            @Valid @RequestBody AdminCreateAccountRequest request) {
        AccountDto response = adminService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo tài khoản thành công"));
    }

    @PatchMapping("/accounts/{id}")
    @Operation(summary = "Chỉnh sửa thông tin tài khoản (hỗ trợ khóa lạc quan STALE)")
    public ResponseEntity<ApiResponse<AccountDto>> updateAccount(
            @PathVariable Long id,
            @RequestBody AdminUpdateAccountRequest request) {
        AccountDto response = adminService.updateAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật tài khoản thành công"));
    }

    @RequestMapping(value = "/accounts/{id}/role", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @Operation(summary = "Đổi vai trò người dùng (USER, RECEPTIONIST, ADMIN)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody AdminChangeRoleRequest request) {
        AccountDto response = adminService.changeRole(id, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("account", response), "Đổi vai trò thành công"));
    }

    @RequestMapping(value = "/accounts/{id}/toggle-status", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Khóa hoặc mở khóa tài khoản")
    public ResponseEntity<ApiResponse<AccountDto>> toggleAccountStatus(
            @AuthenticationPrincipal User adminUser,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> body) {
        boolean active = body != null && body.containsKey("active") ? body.get("active") : false;
        AccountDto response = adminService.setAccountActive(adminUser, id, active);
        return ResponseEntity.ok(ApiResponse.success(response, active ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản"));
    }

    @PostMapping("/accounts/{id}/lock")
    @Operation(summary = "Khóa tài khoản (không thể tự khóa tài khoản chính mình)")
    public ResponseEntity<ApiResponse<AccountDto>> lockAccount(
            @AuthenticationPrincipal User adminUser,
            @PathVariable Long id) {
        AccountDto response = adminService.setAccountActive(adminUser, id, false);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã khóa tài khoản thành công"));
    }

    @PostMapping("/accounts/{id}/unlock")
    @Operation(summary = "Mở khóa tài khoản")
    public ResponseEntity<ApiResponse<AccountDto>> unlockAccount(
            @AuthenticationPrincipal User adminUser,
            @PathVariable Long id) {
        AccountDto response = adminService.setAccountActive(adminUser, id, true);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã mở khóa tài khoản thành công"));
    }

    @PostMapping("/accounts/{id}/reset-password")
    @Operation(summary = "Cấp mật khẩu tạm thời cho tài khoản nhân viên")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        adminService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đặt lại mật khẩu tạm thời thành công"));
    }

    // =========================================================================
    // 3. KHÓA / MỞ KHÓA HỒ SƠ BỆNH NHÂN (ADMIN)
    // =========================================================================

    @RequestMapping(value = "/patients/{id}/toggle-status", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Khóa hoặc mở khóa hồ sơ bệnh nhân")
    public ResponseEntity<ApiResponse<PatientDto>> togglePatientStatus(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> body) {
        boolean active = body != null && body.containsKey("active") ? body.get("active") : false;
        PatientDto response = adminService.setPatientActive(id, active);
        return ResponseEntity.ok(ApiResponse.success(response, active ? "Đã mở khóa hồ sơ bệnh nhân" : "Đã khóa hồ sơ bệnh nhân"));
    }

    @PostMapping("/patients/{id}/lock")
    @Operation(summary = "Khóa hồ sơ bệnh nhân")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lockPatient(@PathVariable Long id) {
        PatientDto response = adminService.setPatientActive(id, false);
        return ResponseEntity.ok(ApiResponse.success(Map.of("patient", response), "Đã khóa hồ sơ bệnh nhân thành công"));
    }

    @PostMapping("/patients/{id}/unlock")
    @Operation(summary = "Mở khóa hồ sơ bệnh nhân")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unlockPatient(@PathVariable Long id) {
        PatientDto response = adminService.setPatientActive(id, true);
        return ResponseEntity.ok(ApiResponse.success(Map.of("patient", response), "Đã mở khóa hồ sơ bệnh nhân thành công"));
    }

    // =========================================================================
    // 4. QUẢN LÝ BÀI VIẾT (CMS)
    // =========================================================================

    @GetMapping("/posts")
    @Operation(summary = "Danh sách bài viết quản trị kèm bộ lọc trạng thái và chuyên mục")
    public ResponseEntity<ApiResponse<AdminPostListResponse>> getAdminPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        AdminPostListResponse response = adminService.getAdminPosts(q, category, status, page, limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách bài viết thành công"));
    }

    @GetMapping("/posts/{id}")
    @Operation(summary = "Xem chi tiết một bài viết để chỉnh sửa")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminPost(@PathVariable Long id) {
        PostDto response = adminService.getAdminPost(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("post", response), "Lấy thông tin bài viết thành công"));
    }

    @PostMapping("/posts")
    @Operation(summary = "Tạo bài viết mới (bản nháp hoặc công khai)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAdminPost(
            @AuthenticationPrincipal User author,
            @Valid @RequestBody AdminSavePostRequest request) {
        PostDto response = adminService.createAdminPost(author, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(Map.of("post", response), "Tạo bài viết thành công"));
    }

    @PutMapping("/posts/{id}")
    @Operation(summary = "Cập nhật bài viết")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateAdminPost(
            @PathVariable Long id,
            @RequestBody AdminSavePostRequest request) {
        PostDto response = adminService.updateAdminPost(id, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("post", response), "Cập nhật bài viết thành công"));
    }

    @RequestMapping(value = "/posts/{id}/pin", method = {RequestMethod.POST, RequestMethod.PATCH})
    @Operation(summary = "Ghim bài viết lên đầu trang Tin tức (chỉ 1 bài duy nhất được ghim)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pinPost(@PathVariable Long id) {
        PostDto response = adminService.setPostPin(id, true);
        return ResponseEntity.ok(ApiResponse.success(Map.of("post", response), "Đã ghim bài viết lên đầu trang"));
    }

    @PostMapping("/posts/{id}/unpin")
    @Operation(summary = "Bỏ ghim bài viết")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unpinPost(@PathVariable Long id) {
        PostDto response = adminService.setPostPin(id, false);
        return ResponseEntity.ok(ApiResponse.success(Map.of("post", response), "Đã bỏ ghim bài viết"));
    }

    @DeleteMapping("/posts/{id}")
    @Operation(summary = "Xóa bài viết")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        adminService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa bài viết thành công"));
    }

    // =========================================================================
    // 5. UPLOAD MEDIA (≤ 3MB, WHITELIST MIME)
    // =========================================================================

    @PostMapping(value = "/uploads", consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE, "image/*", MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "Tải ảnh bài viết lên máy chủ (≤ 3MB, JPG/PNG/WEBP/GIF)")
    public ResponseEntity<Map<String, Object>> uploadImage(
            HttpServletRequest request,
            @RequestParam(value = "file", required = false) MultipartFile multipartFile,
            @RequestBody(required = false) byte[] rawBytes) throws IOException {

        byte[] data;
        String contentType;

        if (multipartFile != null && !multipartFile.isEmpty()) {
            data = multipartFile.getBytes();
            contentType = multipartFile.getContentType();
        } else if (rawBytes != null && rawBytes.length > 0) {
            data = rawBytes;
            contentType = request.getContentType();
        } else {
            // Đọc trực tiếp từ input stream nếu Spring MVC không map được rawBytes
            data = request.getInputStream().readAllBytes();
            contentType = request.getContentType();
        }

        Map<String, Object> result = adminService.saveUpload(contentType, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
