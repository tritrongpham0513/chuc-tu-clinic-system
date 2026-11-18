package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateDoctorDayOffRequest;
import com.chuctupk.dto.request.UpdateDoctorScheduleRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.CreateDoctorDayOffResult;
import com.chuctupk.dto.response.DoctorDayOffDto;
import com.chuctupk.dto.response.DoctorScheduleMatrixResponse;
import com.chuctupk.service.DoctorScheduleAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Doctor Schedule Admin API", description = "Quản lý lịch trực tuần và ngày nghỉ phép của bác sĩ")
public class DoctorScheduleAdminController {

    private final DoctorScheduleAdminService doctorScheduleAdminService;

    @GetMapping("/doctor-schedules/matrix")
    @Operation(summary = "Lấy ma trận lịch trực tuần của toàn bộ bác sĩ")
    public ResponseEntity<ApiResponse<DoctorScheduleMatrixResponse>> getWeeklyMatrix() {
        DoctorScheduleMatrixResponse response = doctorScheduleAdminService.getWeeklyScheduleMatrix();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy ma trận lịch trực thành công"));
    }

    @PutMapping("/doctor-schedules/{doctorId}")
    @Operation(summary = "Cập nhật lịch trực tuần của bác sĩ")
    public ResponseEntity<ApiResponse<DoctorScheduleMatrixResponse>> updateSchedule(
            @PathVariable Long doctorId,
            @Valid @RequestBody UpdateDoctorScheduleRequest request
    ) {
        DoctorScheduleMatrixResponse response = doctorScheduleAdminService.updateDoctorSchedule(doctorId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật lịch trực bác sĩ thành công"));
    }

    @GetMapping("/doctor-day-offs")
    @Operation(summary = "Lấy danh sách ngày nghỉ sắp tới của bác sĩ")
    public ResponseEntity<ApiResponse<List<DoctorDayOffDto>>> getUpcomingDayOffs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate
    ) {
        List<DoctorDayOffDto> list = doctorScheduleAdminService.getUpcomingDayOffs(fromDate);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách ngày nghỉ thành công"));
    }

    @PostMapping("/doctor-day-offs")
    @Operation(summary = "Đăng ký ngày nghỉ phép mới cho bác sĩ")
    public ResponseEntity<ApiResponse<CreateDoctorDayOffResult>> createDayOff(
            @Valid @RequestBody CreateDoctorDayOffRequest request
    ) {
        CreateDoctorDayOffResult result = doctorScheduleAdminService.createDayOff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Đã ghi nhận ngày nghỉ phép thành công"));
    }

    @DeleteMapping("/doctor-day-offs/{id}")
    @Operation(summary = "Xoá / thu hồi ngày nghỉ phép của bác sĩ")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteDayOff(@PathVariable Long id) {
        doctorScheduleAdminService.deleteDayOff(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("id", id), "Đã huỷ ngày nghỉ thành công"));
    }
}
