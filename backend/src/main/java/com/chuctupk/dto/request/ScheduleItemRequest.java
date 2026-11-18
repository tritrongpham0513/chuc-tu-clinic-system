package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Period;
import jakarta.validation.constraints.NotNull;

public record ScheduleItemRequest(
        @NotNull(message = "Ngày trong tuần không được để trống")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Ca trực không được để trống")
        Period period,

        Boolean isActive
) {}
