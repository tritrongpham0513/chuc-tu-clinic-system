package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserAccountRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 - 50 ký tự")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 100, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])\\d{8}$", message = "Số điện thoại không hợp lệ (10 chữ số)")
    private String phone;

    private String email;

    @NotNull(message = "Vai trò (Role) không được để trống")
    private Role role; // ADMIN, STAFF, DOCTOR, PATIENT

    // Thông tin bổ sung nếu là Bác sĩ
    private String title; // "Bác sĩ YHCT", "Lương y", "Bác sĩ CKI"
    private String specialty; // "Cơ xương khớp", "Châm cứu bấm huyệt"
}

