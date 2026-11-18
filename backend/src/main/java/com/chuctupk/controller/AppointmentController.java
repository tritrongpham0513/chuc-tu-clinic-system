package com.chuctupk.controller;

import com.chuctupk.dto.request.AppointmentCancelRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.AppointmentResponse;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ConcurrentHashMap<String, Deque<Long>> lookupRateLimit = new ConcurrentHashMap<>();

    @GetMapping("/{bookingCode}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointment(
            @PathVariable("bookingCode") String bookingCode,
            @RequestHeader("X-Phone") String xPhone,
            HttpServletRequest request
    ) {
        checkLookupRateLimit(request, bookingCode);
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

    private void checkLookupRateLimit(HttpServletRequest request, String bookingCode) {
        String clientIp = getClientIp(request);
        String key = clientIp + ":" + (bookingCode != null ? bookingCode.trim().toUpperCase() : "");
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L; // 1 phút

        // Dọn dẹp các entry hết hạn nếu bảng theo dõi lớn (thay vì clear toàn bộ làm hổng rate-limit)
        if (lookupRateLimit.size() > 2000) {
            lookupRateLimit.entrySet().removeIf(entry -> {
                Deque<Long> queue = entry.getValue();
                return queue == null || queue.isEmpty() || queue.peekLast() < windowStart;
            });
        }

        Deque<Long> timestamps = lookupRateLimit.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= 20) {
                throw new AppException(ErrorCode.TOO_MANY_REQUESTS,
                        "Bạn đã tra cứu quá 20 lần trong 1 phút. Vui lòng thử lại sau.", 60);
            }
            timestamps.addLast(now);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
