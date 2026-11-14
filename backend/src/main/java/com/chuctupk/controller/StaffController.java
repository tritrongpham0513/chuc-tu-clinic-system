package com.chuctupk.controller;

import com.chuctupk.dto.request.AutoInviteRequest;
import com.chuctupk.dto.request.CancelAppointmentStaffRequest;
import com.chuctupk.dto.request.RescheduleAppointmentRequest;
import com.chuctupk.dto.request.StaffCreateAppointmentRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.User;
import com.chuctupk.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Portal API", description = "Phân hệ nội bộ: Lịch khám, Đặt hộ, Check-in, Hàng đợi 5 ghế")
public class StaffController {

    private final StaffService staffService;

    // =========================================================================
    // 1. LỊCH KHÁM & ĐẶT HỘ TẠI QUẦY
    // =========================================================================

    @GetMapping("/appointments")
    @Operation(summary = "Lấy danh sách lịch khám theo ngày kèm thống kê")
    public ResponseEntity<ApiResponse<StaffAppointmentListResponse>> getAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        StaffAppointmentListResponse response = staffService.getAppointments(date);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách lịch hẹn thành công"));
    }

    @GetMapping("/appointments/dates")
    @Operation(summary = "Lấy danh sách các ngày có lịch khám trong tháng")
    public ResponseEntity<ApiResponse<StaffAppointmentDatesResponse>> getAppointmentDates(
            @RequestParam(required = false) String month) {
        StaffAppointmentDatesResponse response = staffService.getAppointmentDates(month);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách ngày thành công"));
    }

    @GetMapping("/appointments/lookup")
    @Operation(summary = "Tra cứu phiếu khám theo mã phiếu, SĐT hoặc mã bệnh nhân")
    public ResponseEntity<ApiResponse<StaffAppointmentLookupResponse>> lookupAppointment(
            @RequestParam(required = false) String code) {
        StaffAppointmentLookupResponse response = staffService.lookupAppointment(code);
        return ResponseEntity.ok(ApiResponse.success(response, "Tra cứu phiếu khám thành công"));
    }

    @GetMapping("/appointments/{id}")
    @Operation(summary = "Xem chi tiết phiếu khám và hồ sơ bệnh án")
    public ResponseEntity<ApiResponse<StaffAppointmentDetailResponse>> getAppointmentDetail(
            @PathVariable Long id) {
        StaffAppointmentDetailResponse response = staffService.getAppointmentDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết phiếu khám thành công"));
    }

    @GetMapping("/availability")
    @Operation(summary = "Kiểm tra tình trạng khung giờ khả dụng tại quầy (tối đa 7 ngày)")
    public ResponseEntity<ApiResponse<StaffAvailabilityResponse>> getAvailability(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        StaffAvailabilityResponse response = staffService.getAvailability(date);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách khung giờ thành công"));
    }

    @PostMapping("/appointments")
    @Operation(summary = "Tiếp tân tạo phiếu hẹn khám tại quầy (tuỳ chọn check-in ngay)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createCounterAppointment(
            @AuthenticationPrincipal User staff,
            @Valid @RequestBody StaffCreateAppointmentRequest request) {
        Map<String, Object> response = staffService.createCounterAppointment(staff, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo phiếu khám tại quầy thành công"));
    }

    @PostMapping("/appointments/{id}/reschedule")
    @Operation(summary = "Đổi giờ khám sang ngày/khung giờ mới (cấp phiếu mới)")
    public ResponseEntity<ApiResponse<StaffAppointmentLookupResponse>> rescheduleAppointment(
            @AuthenticationPrincipal User staff,
            @PathVariable Long id,
            @RequestBody RescheduleAppointmentRequest request) {
        StaffAppointmentLookupResponse response = staffService.rescheduleAppointment(staff, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đổi giờ khám thành công"));
    }

    @PostMapping("/appointments/{id}/cancel")
    @Operation(summary = "Huỷ phiếu khám tại quầy (bắt buộc nêu lý do)")
    public ResponseEntity<ApiResponse<StaffAppointmentLookupResponse>> cancelAppointment(
            @AuthenticationPrincipal User staff,
            @PathVariable Long id,
            @RequestBody CancelAppointmentStaffRequest request) {
        StaffAppointmentLookupResponse response = staffService.cancelAppointment(staff, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Huỷ phiếu khám thành công"));
    }

    // =========================================================================
    // 2. CHECK-IN & TIẾP NHẬN BỆNH NHÂN
    // =========================================================================

    @PostMapping("/appointments/{id}/checkin")
    @Operation(summary = "Tiếp nhận check-in bệnh nhân (kiểm tra ngày và giờ tiếp nhận 08:00 - 16:30)")
    public ResponseEntity<ApiResponse<StaffAppointmentLookupResponse>> checkin(
            @AuthenticationPrincipal User staff,
            @PathVariable Long id) {
        StaffAppointmentLookupResponse response = staffService.checkin(staff, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Check-in bệnh nhân thành công"));
    }

    @PostMapping("/appointments/{id}/undo-checkin")
    @Operation(summary = "Hoàn tác check-in khi tiếp nhận nhầm người")
    public ResponseEntity<ApiResponse<StaffAppointmentLookupResponse>> undoCheckin(
            @AuthenticationPrincipal User staff,
            @PathVariable Long id) {
        StaffAppointmentLookupResponse response = staffService.undoCheckin(staff, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Hoàn tác check-in thành công"));
    }

    @PostMapping("/appointments/{id}/enter")
    @Operation(summary = "Mời đích danh một người vào khám trước (xếp vào ghế trống tiếp theo)")
    public ResponseEntity<ApiResponse<StaffAppointmentLookupResponse>> enterRoom(
            @AuthenticationPrincipal User staff,
            @PathVariable Long id) {
        StaffAppointmentLookupResponse response = staffService.enterRoom(staff, id);
        return ResponseEntity.ok(ApiResponse.success(response, "Mời bệnh nhân vào khám thành công"));
    }

    // =========================================================================
    // 3. HÀNG ĐỢI & MÀN HÌNH CHỜ (5 GHẾ)
    // =========================================================================

    @GetMapping("/queue")
    @Operation(summary = "Lấy trạng thái hàng đợi 5 ghế khám và danh sách hàng chờ")
    public ResponseEntity<ApiResponse<QueueStateDto>> getQueueState() {
        QueueStateDto response = staffService.getQueueState();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy trạng thái hàng đợi thành công"));
    }

    @PostMapping("/queue/invite")
    @Operation(summary = "Mời người kế tiếp trong hàng chờ vào ghế khám còn trống")
    public ResponseEntity<ApiResponse<QueueStateDto>> inviteNext() {
        QueueStateDto response = staffService.inviteNext();
        return ResponseEntity.ok(ApiResponse.success(response, "Đã mời người kế tiếp vào khám"));
    }

    @PostMapping("/queue/{id}/undo-enter")
    @Operation(summary = "Hoàn tác vào khám (đưa người bệnh trở lại hàng chờ)")
    public ResponseEntity<ApiResponse<QueueStateDto>> undoEnter(
            @PathVariable Long id) {
        QueueStateDto response = staffService.undoEnter(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Hoàn tác vào khám thành công"));
    }

    @PutMapping("/queue/auto-invite")
    @Operation(summary = "Bật/tắt chế độ tự động mời người kế tiếp khi có ghế trống")
    public ResponseEntity<ApiResponse<QueueStateDto>> setAutoInvite(
            @RequestBody AutoInviteRequest request) {
        boolean enabled = request.enabled() != null && request.enabled();
        QueueStateDto response = staffService.setAutoInvite(enabled);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật chế độ tự động mời thành công"));
    }
}

