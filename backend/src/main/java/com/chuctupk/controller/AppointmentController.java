package com.chuctupk.controller;

import com.chuctupk.dto.request.AppointmentCancelRequest;
import com.chuctupk.dto.request.AppointmentCreateRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.AppointmentResponse;
import com.chuctupk.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            @Valid @RequestBody AppointmentCreateRequest request
    ) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        URI location = URI.create("/api/v1/appointments/" + response.getBookingCode());
        return ResponseEntity.created(location)
                .body(ApiResponse.<AppointmentResponse>builder()
                        .success(true)
                        .statusCode(201)
                        .message("Đặt lịch khám thành công")
                        .data(response)
                        .build());
    }

    @GetMapping("/{bookingCode}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(
            @PathVariable("bookingCode") String bookingCode,
            @RequestHeader("X-Phone") String xPhone
    ) {
        AppointmentResponse response = appointmentService.getAppointmentByBookingCode(bookingCode, xPhone);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin lịch hẹn thành công"));
    }

    @PatchMapping("/{bookingCode}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable("bookingCode") String bookingCode,
            @RequestHeader("X-Phone") String xPhone,
            @Valid @RequestBody AppointmentCancelRequest request
    ) {
        AppointmentResponse response = appointmentService.cancelAppointment(bookingCode, xPhone, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hủy lịch hẹn thành công"));
    }
}

