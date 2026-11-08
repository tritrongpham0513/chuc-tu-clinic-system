package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Số điện thoại hoặc tên đăng nhập không được để trống")
    private String usernameOrPhone;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}

