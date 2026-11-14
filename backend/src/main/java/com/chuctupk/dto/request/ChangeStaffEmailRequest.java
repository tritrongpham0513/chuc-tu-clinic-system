package com.chuctupk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/v1/staff/account/email — yêu cầu đổi email, hệ thống gửi mã xác minh. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeStaffEmailRequest {

    @NotBlank(message = "Vui lòng nhập email mới")
    @Email(message = "Email chưa đúng định dạng")
    private String email;
}
