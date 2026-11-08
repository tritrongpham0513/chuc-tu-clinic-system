package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.OtpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendOtpRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[35789]\\d{8}|[35789]\\d{8})$", message = "Số điện thoại không hợp lệ, phải gồm 10 chữ số (hoặc 9 chữ số sau +84)")
    private String phone;

    @NotNull(message = "Loại OTP không được để trống")
    private OtpType otpType;
}

