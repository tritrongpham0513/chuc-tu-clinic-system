package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StaffCreatePatientRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên bệnh nhân")
        String fullName,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        String phone,

        @NotNull(message = "Vui lòng nhập năm sinh")
        Integer birthYear,

        String dob,
        String gender,
        String address,
        String medicalHistory,
        Boolean confirmDuplicate
) {}

