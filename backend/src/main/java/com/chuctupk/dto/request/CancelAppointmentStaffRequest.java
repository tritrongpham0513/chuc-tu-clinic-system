package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelAppointmentStaffRequest(
        @NotBlank(message = "Vui lòng ghi rõ lý do huỷ")
        String reason
) {}

