package com.chuctupk.controller;

import com.chuctupk.dto.request.PatientBookAppointmentRequest;
import com.chuctupk.dto.request.PatientProfileUpsertRequest;
import com.chuctupk.dto.request.UpdateMeRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.User;
import com.chuctupk.service.PatientPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Phân hệ Người bệnh (Me v1)", description = "Quản lý thông tin cá nhân, hồ sơ người thân, kiểm tra lịch trống và đặt hẹn khám bệnh")
public class MeController {

    private final PatientPortalService patientPortalService;

    // 1. Thông tin tài khoản người dùng
    @GetMapping
    @Operation(summary = "Lấy thông tin tài khoản và hồ sơ chính chủ hiện tại")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyAccount(
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> data = patientPortalService.getMyAccount(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin tài khoản thành công"));
    }

    @PatchMapping
    @Operation(summary = "Cập nhật thông tin tài khoản và hồ sơ chính chủ")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMyAccount(
            @AuthenticationPrincipal User currentUser,
            @RequestBody UpdateMeRequest request) {
        Map<String, Object> data = patientPortalService.updateMyAccount(currentUser, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật thông tin thành công"));
    }

    // 2. Danh sách & chi tiết hồ sơ người khám
    @GetMapping("/patients")
    @Operation(summary = "Lấy danh sách hồ sơ khám của tài khoản (tối đa 10)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyPatients(
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> data = patientPortalService.getMyPatients(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách hồ sơ thành công"));
    }

    @PostMapping("/patients")
    @Operation(summary = "Thêm mới hồ sơ người thân")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPatient(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PatientProfileUpsertRequest request) {
        Map<String, Object> data = patientPortalService.createPatient(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data, "Tạo hồ sơ người bệnh thành công"));
    }

    @GetMapping("/patients/{id}")
    @Operation(summary = "Lấy chi tiết một hồ sơ bệnh nhân theo ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatient(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        Map<String, Object> data = patientPortalService.getPatient(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin hồ sơ thành công"));
    }

    @PatchMapping("/patients/{id}")
    @Operation(summary = "Cập nhật hồ sơ bệnh nhân")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePatient(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody PatientProfileUpsertRequest request) {
        Map<String, Object> data = patientPortalService.updatePatient(currentUser, id, request);
        return ResponseEntity.ok(ApiResponse.ok(data, "Cập nhật hồ sơ thành công"));
    }

    @DeleteMapping("/patients/{id}")
    @Operation(summary = "Xóa hồ sơ bệnh nhân (tách hồ sơ nếu đã có lịch sử)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePatient(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        Map<String, Object> data = patientPortalService.deletePatient(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Xóa hồ sơ thành công"));
    }

    // 3. Khung giờ & Đặt lịch khám
    @GetMapping("/availability")
    @Operation(summary = "Tra cứu tình trạng ghế trống các khung giờ trong ngày")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Map<String, Object> data = patientPortalService.getAvailability(date);
        return ResponseEntity.ok(ApiResponse.ok(data, "Tra cứu khung giờ thành công"));
    }

    @PostMapping("/appointments")
    @Operation(summary = "Đặt lịch khám mới (hỗ trợ Idempotency-Key và kiểm soát đồng thời)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bookAppointment(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody PatientBookAppointmentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Map<String, Object> data = patientPortalService.bookAppointment(currentUser, request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data, "Đặt lịch khám thành công"));
    }

    // 4. Phiếu khám của tôi
    @GetMapping("/appointments")
    @Operation(summary = "Lấy danh sách phiếu khám của tôi")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyAppointments(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status) {
        Map<String, Object> data = patientPortalService.getMyAppointments(currentUser, status);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy danh sách phiếu khám thành công"));
    }

    @GetMapping("/appointments/{id}")
    @Operation(summary = "Xem chi tiết phiếu khám theo ID")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAppointmentDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        Map<String, Object> data = patientPortalService.getAppointmentDetail(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy chi tiết phiếu khám thành công"));
    }

    @PostMapping("/appointments/{id}/cancel")
    @Operation(summary = "Bệnh nhân chủ động hủy phiếu khám trước giờ check-in")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelAppointment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        Map<String, Object> data = patientPortalService.cancelAppointment(currentUser, id, reason);
        return ResponseEntity.ok(ApiResponse.ok(data, "Hủy phiếu khám thành công"));
    }

    @GetMapping("/appointments/{id}/result")
    @Operation(summary = "Xem kết quả khám bệnh của phiếu khám")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAppointmentResult(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        Map<String, Object> data = patientPortalService.getAppointmentResult(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy kết quả khám bệnh thành công"));
    }

    @GetMapping("/history")
    @Operation(summary = "Xem lịch sử các lần khám trước đây của tài khoản")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyHistory(
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> data = patientPortalService.getMyHistory(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy lịch sử khám bệnh thành công"));
    }
}

