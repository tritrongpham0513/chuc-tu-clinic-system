package com.chuctupk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PATCH /api/v1/staff/account — cập nhật họ tên của tài khoản nội bộ. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaffAccountRequest {

    @NotBlank(message = "Vui lòng nhập họ và tên")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự")
    private String fullName;
}
