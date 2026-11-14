package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/v1/staff/account/email/verify — xác minh mã 6 số để hoàn tất đổi email. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyStaffEmailRequest {

    @NotBlank(message = "Vui lòng nhập mã xác minh 6 số")
    @Pattern(regexp = "^\\d{6}$", message = "Mã xác minh gồm đúng 6 chữ số")
    private String code;
}
