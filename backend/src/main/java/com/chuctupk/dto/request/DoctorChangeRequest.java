package com.chuctupk.dto.request;

import jakarta.validation.constraints.Size;

public record DoctorChangeRequest(
        @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
        String note
) {}
