package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateClinicServiceRequest;
import com.chuctupk.dto.request.UpdateClinicServiceRequest;
import com.chuctupk.dto.response.AdminServiceResponse;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.service.AdminServiceService;
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

@Tag(name = "Admin Services", description = "Quản trị dịch vụ khám chữa bệnh")
@RestController
@RequestMapping("/api/v1/admin/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceController {

    private final AdminServiceService adminServiceService;

    @Operation(summary = "Lấy danh sách tất cả dịch vụ (kể cả đã ẩn)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminServiceResponse>>> getServices(
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        List<AdminServiceResponse> list = adminServiceService.getAllServices();
        return ResponseEntity.ok(ApiResponse.ok(list, "Lấy danh sách dịch vụ thành công"));
    }

    @Operation(summary = "Thêm dịch vụ khám mới")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminServiceResponse>> createService(
            @Valid @RequestBody CreateClinicServiceRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminServiceResponse response = adminServiceService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Thêm dịch vụ thành công"));
    }

    @Operation(summary = "Cập nhật dịch vụ khám")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminServiceResponse>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClinicServiceRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminServiceResponse response = adminServiceService.updateService(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật dịch vụ thành công"));
    }

    @Operation(summary = "Xoá dịch vụ khám")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        adminServiceService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xoá dịch vụ thành công"));
    }

    private void checkAdmin(User user) {
        if (user != null && user.getRole() != Role.ADMIN) {
            throw AppException.forbidden("Bạn không có quyền thực hiện thao tác này");
        }
    }
}
