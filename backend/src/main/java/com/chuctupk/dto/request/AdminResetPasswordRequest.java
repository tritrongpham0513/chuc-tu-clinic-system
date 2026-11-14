package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminResetPasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu tạm")
        String tempPassword
) {}

