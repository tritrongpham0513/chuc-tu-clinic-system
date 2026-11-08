package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[35789]\\d{8}|[35789]\\d{8})$", message = "Số điện thoại không hợp lệ, phải gồm 10 chữ số (hoặc 9 chữ số sau +84)")
    private String phone;

    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotNull(message = "Năm sinh không được để trống")
    @Min(value = 1900, message = "Năm sinh không hợp lệ")
    @Max(value = 2100, message = "Năm sinh không hợp lệ")
    private Integer yearOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    private String address;

    @NotBlank(message = "Mã OTP xác thực không được để trống")
    private String otpCode;
}
