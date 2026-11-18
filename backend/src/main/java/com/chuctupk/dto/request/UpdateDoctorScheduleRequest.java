package com.chuctupk.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Yêu cầu cập nhật lịch trực tuần của bác sĩ.
 * PUT là THAY THẾ TOÀN BỘ tuần, FE phải gửi đủ 7 ngày.
 */
public record UpdateDoctorScheduleRequest(
        @NotEmpty(message = "Lịch trực không được để trống")
        List<@Valid ScheduleItemRequest> schedules
) {}
