package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminCreateAccountRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên")
        String fullName,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        String phone,

        String email,
        String role,
        String tempPassword,
        Integer birthYear,
        String gender
) {
    public AdminCreateAccountRequest(String fullName, String phone, String email, String role, String tempPassword) {
        this(fullName, phone, email, role, tempPassword, null, null);
    }
}

