package com.chuctupk.controller;

import com.chuctupk.dto.request.AppointmentCancelByPatientRequest;
import com.chuctupk.dto.request.BookingRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.AppointmentDetailResponse;
import com.chuctupk.dto.response.SlotAvailabilityResponse;
import com.chuctupk.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * GET /api/v1/booking/slots?date=2025-11-10
     * Lấy danh sách khung giờ khả dụng theo ngày (công khai — không cần đăng nhập).
     */
    @GetMapping("/slots")
    public ResponseEntity<ApiResponse<List<SlotAvailabilityResponse>>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SlotAvailabilityResponse> slots = bookingService.getAvailableSlots(date);
        return ResponseEntity.ok(ApiResponse.success(slots, "Lấy khung giờ thành công"));
    }

    /**
     * POST /api/v1/booking
     * Đặt lịch khám (yêu cầu đăng nhập vai trò PATIENT).
     */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> bookAppointment(
            @Valid @RequestBody BookingRequest request) {
        AppointmentDetailResponse response = bookingService.bookAppointment(request);
        URI location = URI.create("/api/v1/booking/" + response.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.<AppointmentDetailResponse>builder()
                        .success(true)
                        .statusCode(201)
                        .message("Đặt lịch khám thành công! Mã phiếu: " + response.getBookingCode())
                        .data(response)
                        .build());
    }

    /**
     * GET /api/v1/booking/my/upcoming
     * Phiếu sắp tới của tôi (PENDING, CONFIRMED, IN_PROGRESS).
     */
    @GetMapping("/my/upcoming")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<AppointmentDetailResponse>>> getMyUpcoming() {
        List<AppointmentDetailResponse> list = bookingService.getMyUpcoming();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách phiếu khám thành công"));
    }

    /**
     * GET /api/v1/booking/my/history
     * Lịch sử khám (COMPLETED, CANCELLED, NO_SHOW).
     */
    @GetMapping("/my/history")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<AppointmentDetailResponse>>> getMyHistory() {
        List<AppointmentDetailResponse> list = bookingService.getMyHistory();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy lịch sử khám thành công"));
    }

    /**
     * GET /api/v1/booking/my/{id}
     * Chi tiết phiếu khám theo id.
     */
    @GetMapping("/my/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> getMyAppointmentById(@PathVariable Long id) {
        AppointmentDetailResponse response = bookingService.getMyAppointmentById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết phiếu khám thành công"));
    }

    /**
     * POST /api/v1/booking/my/{id}/cancel
     * Hủy phiếu khám (chỉ được hủy khi status = PENDING).
     */
    @PostMapping("/my/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentDetailResponse>> cancelMyAppointment(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) AppointmentCancelByPatientRequest request) {
        if (request == null) request = new AppointmentCancelByPatientRequest();
        AppointmentDetailResponse response = bookingService.cancelMyAppointment(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hủy phiếu khám thành công"));
    }
}
