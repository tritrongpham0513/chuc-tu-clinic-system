package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffLoginRequest {

    @NotBlank(message = "Vui lòng nhập tài khoản (tên đăng nhập, email hoặc số điện thoại)")
    private String identifier;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;
}

