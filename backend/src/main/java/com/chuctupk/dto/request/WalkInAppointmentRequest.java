package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WalkInAppointmentRequest {

    @NotBlank(message = "Họ tên bệnh nhân không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])\\d{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotNull(message = "Năm sinh không được để trống")
    private Integer yearOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    private String address;

    private Long timeSlotId;

    private Long doctorId;

    private Long serviceId;

    private String symptoms;
}

