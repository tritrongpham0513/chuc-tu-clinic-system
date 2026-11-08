package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PatientProfileRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])\\d{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phone;

    @NotNull(message = "Năm sinh không được để trống")
    @Min(value = 1900, message = "Năm sinh không hợp lệ")
    @Max(value = 2025, message = "Năm sinh không hợp lệ")
    private Integer yearOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @NotBlank(message = "Quan hệ không được để trống")
    private String relationship;

    @Size(max = 500, message = "Tiền sử bệnh tối đa 500 ký tự")
    private String medicalHistory;
}
