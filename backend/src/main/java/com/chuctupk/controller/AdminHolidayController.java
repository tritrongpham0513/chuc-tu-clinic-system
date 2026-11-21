package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateHolidayRequest;
import com.chuctupk.dto.request.UpdateHolidayRequest;
import com.chuctupk.dto.response.AdminHolidayResponse;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.service.AdminHolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Admin Holidays", description = "Quản trị ngày nghỉ phòng khám")
@RestController
@RequestMapping("/api/v1/admin/holidays")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminHolidayController {

    private final AdminHolidayService holidayService;

    @Operation(summary = "Lấy danh sách ngày nghỉ theo năm")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminHolidayResponse>>> getHolidays(
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        List<AdminHolidayResponse> list = holidayService.getHolidays(year);
        return ResponseEntity.ok(ApiResponse.ok(list, "Lấy danh sách ngày nghỉ thành công"));
    }

    @Operation(summary = "Thêm ngày nghỉ mới")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminHolidayResponse>> createHoliday(
            @Valid @RequestBody CreateHolidayRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminHolidayResponse response = holidayService.createHoliday(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "Thêm ngày nghỉ thành công"));
    }

    @Operation(summary = "Cập nhật ngày nghỉ")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminHolidayResponse>> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHolidayRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        AdminHolidayResponse response = holidayService.updateHoliday(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật ngày nghỉ thành công"));
    }

    @Operation(summary = "Xoá ngày nghỉ")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xoá ngày nghỉ thành công"));
    }

    @Operation(summary = "Kiểm tra số lượng phiếu khám trong ngày")
    @GetMapping("/appointment-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAppointmentCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal User currentUser
    ) {
        checkAdmin(currentUser);
        long count = holidayService.getAppointmentCount(date);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("date", date.toString(), "count", count), "Lấy số lượng phiếu thành công"));
    }

    private void checkAdmin(User user) {
        if (user != null && user.getRole() != Role.ADMIN) {
            throw AppException.forbidden("Bạn không có quyền thực hiện thao tác này");
        }
    }
}
