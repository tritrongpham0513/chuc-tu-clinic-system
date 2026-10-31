package com.chuctupk.controller;

import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.TimeSlotResponse;
import com.chuctupk.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/time-slots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getTimeSlots(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "doctorId", required = false) Long doctorId
    ) {
        List<TimeSlotResponse> slots = timeSlotService.getAvailableTimeSlots(date, doctorId);
        return ResponseEntity.ok(ApiResponse.success(slots, "Lấy danh sách khung giờ thành công"));
    }
}

