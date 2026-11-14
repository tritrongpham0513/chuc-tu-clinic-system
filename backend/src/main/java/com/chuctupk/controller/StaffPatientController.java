package com.chuctupk.controller;

import com.chuctupk.dto.request.StaffCreatePatientRequest;
import com.chuctupk.dto.request.StaffUpdatePatientRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.StaffPatientDetailResponse;
import com.chuctupk.dto.response.StaffPatientListResponse;
import com.chuctupk.service.StaffPatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff/patients")
@RequiredArgsConstructor
@Tag(name = "Staff Patient Management API", description = "Quản lý hồ sơ bệnh nhân phía nhân viên: tra cứu, trùng SĐT, lập hồ sơ mới, sửa hồ sơ")
public class StaffPatientController {

    private final StaffPatientService staffPatientService;

    @GetMapping
    @Operation(summary = "Tìm kiếm và phân trang danh sách hồ sơ bệnh nhân")
    public ResponseEntity<ApiResponse<StaffPatientListResponse>> getPatients(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        StaffPatientListResponse response = staffPatientService.getPatients(q, locked, page, limit);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách bệnh nhân thành công"));
    }

    @GetMapping("/duplicates")
    @Operation(summary = "Kiểm tra trùng số điện thoại với các hồ sơ đang có")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDuplicates(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long excludeId) {
        Map<String, Object> response = staffPatientService.getDuplicatePatients(phone, excludeId);
        return ResponseEntity.ok(ApiResponse.success(response, "Kiểm tra trùng hồ sơ thành công"));
    }

    @PostMapping
    @Operation(summary = "Tạo hồ sơ bệnh nhân mới tại quầy tiếp tân (tự sinh mã BN)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPatient(
            @Valid @RequestBody StaffCreatePatientRequest request) {
        Map<String, Object> response = staffPatientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo hồ sơ bệnh nhân thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết hồ sơ bệnh nhân cùng toàn bộ lịch sử khám và phiếu kết quả")
    public ResponseEntity<ApiResponse<StaffPatientDetailResponse>> getPatientDetail(
            @PathVariable Long id) {
        StaffPatientDetailResponse response = staffPatientService.getPatientDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin hồ sơ bệnh nhân thành công"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Chỉnh sửa thông tin hồ sơ bệnh nhân (hỗ trợ khóa lạc quan STALE)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePatient(
            @PathVariable Long id,
            @RequestBody StaffUpdatePatientRequest request) {
        Map<String, Object> response = staffPatientService.updatePatient(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật hồ sơ bệnh nhân thành công"));
    }
}

