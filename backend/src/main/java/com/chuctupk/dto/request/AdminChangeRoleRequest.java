package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminChangeRoleRequest(
        @NotBlank(message = "Vui lòng chọn vai trò")
        String role,

        String tempPassword,
        String email
) {}

