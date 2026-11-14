package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateRecordRequest;
import com.chuctupk.dto.request.UpdateRecordRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.RecordDetailResponse;
import com.chuctupk.entity.User;
import com.chuctupk.service.StaffRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Medical Record API", description = "Quản lý phiếu kết quả khám bệnh và bệnh án Y học cổ truyền")
public class StaffRecordController {

    private final StaffRecordService staffRecordService;

    @PostMapping("/appointments/{id}/record")
    @Operation(summary = "Lập phiếu kết quả khám bệnh cho phiếu hẹn đang khám (chuyển trạng thái COMPLETED)")
    public ResponseEntity<ApiResponse<RecordDetailResponse>> createRecord(
            @AuthenticationPrincipal User staff,
            @PathVariable Long id,
            @Valid @RequestBody CreateRecordRequest request) {
        RecordDetailResponse response = staffRecordService.createRecord(staff, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Lập phiếu kết quả khám bệnh thành công"));
    }

    @GetMapping("/records/{id}")
    @Operation(summary = "Xem chi tiết phiếu kết quả khám bệnh kèm lịch sử chỉnh sửa")
    public ResponseEntity<ApiResponse<RecordDetailResponse>> getRecord(
            @PathVariable Long id) {
        RecordDetailResponse response = staffRecordService.getRecord(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin phiếu kết quả thành công"));
    }

    @PutMapping("/records/{id}")
    @Operation(summary = "Chỉnh sửa phiếu kết quả khám bệnh trong vòng 24 giờ kể từ khi lập")
    public ResponseEntity<ApiResponse<RecordDetailResponse>> updateRecord(
            @AuthenticationPrincipal User staff,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecordRequest request) {
        RecordDetailResponse response = staffRecordService.updateRecord(staff, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật phiếu kết quả thành công"));
    }
}

