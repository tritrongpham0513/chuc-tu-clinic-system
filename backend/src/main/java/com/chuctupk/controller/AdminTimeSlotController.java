package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateTimeSlotRequest;
import com.chuctupk.dto.request.UpdateTimeSlotRequest;
import com.chuctupk.dto.response.AdminTimeSlotResponse;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.service.AdminTimeSlotService;
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
@RequestMapping("/api/v1/admin/time-slots")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin TimeSlot API", description = "Quản lý khung giờ khám bệnh (CRUD)")
public class AdminTimeSlotController {

    private final AdminTimeSlotService adminTimeSlotService;

    private void checkAdmin(User currentUser) {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            throw AppException.forbidden("Bạn không có quyền thực hiện thao tác này");
        }
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả khung giờ kèm số lượng phiếu khám sắp tới")
    public ResponseEntity<ApiResponse<List<AdminTimeSlotResponse>>> getAllTimeSlots(
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        List<AdminTimeSlotResponse> list = adminTimeSlotService.getAllTimeSlots();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách khung giờ thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo khung giờ khám mới")
    public ResponseEntity<ApiResponse<AdminTimeSlotResponse>> createTimeSlot(
            @Valid @RequestBody CreateTimeSlotRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminTimeSlotResponse created = adminTimeSlotService.createTimeSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Tạo khung giờ thành công"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin khung giờ (chặn đổi giờ khi còn phiếu tương lai)")
    public ResponseEntity<ApiResponse<AdminTimeSlotResponse>> updateTimeSlot(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTimeSlotRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminTimeSlotResponse updated = adminTimeSlotService.updateTimeSlot(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật khung giờ thành công"));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Kích hoạt khung giờ")
    public ResponseEntity<ApiResponse<AdminTimeSlotResponse>> activateTimeSlot(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminTimeSlotResponse updated = adminTimeSlotService.activateTimeSlot(id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Kích hoạt khung giờ thành công"));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Ngưng hoạt động khung giờ (chặn khi còn phiếu tương lai)")
    public ResponseEntity<ApiResponse<AdminTimeSlotResponse>> deactivateTimeSlot(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminTimeSlotResponse updated = adminTimeSlotService.deactivateTimeSlot(id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Ngưng hoạt động khung giờ thành công"));
    }
}
