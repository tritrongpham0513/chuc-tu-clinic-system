package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StaffCreateAppointmentRequest(
        @NotNull(message = "Mã bệnh nhân không được để trống")
        Long patientId,

        String serviceType,

        @NotNull(message = "Ngày khám không được để trống")
        String slotDate,

        @NotBlank(message = "Khung giờ khám không được để trống")
        String slotStart,

        Boolean checkInNow
) {}

