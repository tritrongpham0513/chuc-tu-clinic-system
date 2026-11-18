package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Period;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateDoctorDayOffRequest(
        @NotNull(message = "Mã bác sĩ không được để trống")
        Long doctorId,

        @NotNull(message = "Ngày nghỉ không được để trống")
        LocalDate offDate,

        Period period,

        @Size(max = 255, message = "Lý do tối đa 255 ký tự")
        String reason
) {}
