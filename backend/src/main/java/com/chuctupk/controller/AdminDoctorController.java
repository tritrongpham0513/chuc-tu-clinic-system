package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateDoctorRequest;
import com.chuctupk.dto.request.UpdateDoctorRequest;
import com.chuctupk.dto.response.AdminDoctorDetailResponse;
import com.chuctupk.dto.response.AdminDoctorListResponse;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.DoctorUserOptionResponse;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.service.AdminDoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/doctors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Doctor API", description = "Quản lý hồ sơ bác sĩ (CRUD)")
public class AdminDoctorController {

    private final AdminDoctorService adminDoctorService;

    private void checkAdmin(User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw AppException.forbidden("Bạn không có quyền thực hiện thao tác này");
        }
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả bác sĩ cho phân hệ quản trị")
    public ResponseEntity<ApiResponse<List<AdminDoctorListResponse>>> getAllDoctors(
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        List<AdminDoctorListResponse> list = adminDoctorService.getAllDoctors();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách bác sĩ thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết hồ sơ bác sĩ")
    public ResponseEntity<ApiResponse<AdminDoctorDetailResponse>> getDoctorById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminDoctorDetailResponse doctor = adminDoctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.success(doctor, "Lấy thông tin bác sĩ thành công"));
    }

    @GetMapping("/unassigned-accounts")
    @Operation(summary = "Lấy danh sách tài khoản vai trò DOCTOR chưa liên kết bác sĩ")
    public ResponseEntity<ApiResponse<List<DoctorUserOptionResponse>>> getUnassignedDoctorAccounts(
            @RequestParam(required = false) Long currentDoctorId,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        List<DoctorUserOptionResponse> accounts = adminDoctorService.getUnassignedDoctorAccounts(currentDoctorId);
        return ResponseEntity.ok(ApiResponse.success(accounts, "Lấy danh sách tài khoản thành công"));
    }

    @PostMapping
    @Operation(summary = "Thêm mới hồ sơ bác sĩ")
    public ResponseEntity<ApiResponse<AdminDoctorDetailResponse>> createDoctor(
            @Valid @RequestBody CreateDoctorRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminDoctorDetailResponse created = adminDoctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Thêm bác sĩ thành công"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin / ẩn / hiện bác sĩ (hỗ trợ khóa lạc quan)")
    public ResponseEntity<ApiResponse<AdminDoctorDetailResponse>> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDoctorRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminDoctorDetailResponse updated = adminDoctorService.updateDoctor(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật thông tin bác sĩ thành công"));
    }
}
