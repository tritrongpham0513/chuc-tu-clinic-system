package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentCreateRequest {

    @NotBlank(message = "Họ và tên bệnh nhân không được để trống")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(03|05|07|08|09)\\d{8}$", message = "Số điện thoại không hợp lệ, phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09")
    private String phone;

    @NotNull(message = "Năm sinh không được để trống")
    @Min(value = 1900, message = "Năm sinh phải từ năm 1900 trở lại đây")
    private Integer yearOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    @Size(max = 255, message = "Địa chỉ không vượt quá 255 ký tự")
    private String address;

    @Size(max = 1000, message = "Mô tả triệu chứng không vượt quá 1000 ký tự")
    private String symptoms;

    @NotNull(message = "Ngày khám không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate appointmentDate;

    @NotNull(message = "Khung giờ khám không được để trống")
    private Long timeSlotId;

    private Long doctorId; // Tùy chọn

    private Long serviceId; // Tùy chọn

    @Size(max = 100, message = "Tên người đặt hộ không vượt quá 100 ký tự")
    private String bookerName;

    @Pattern(regexp = "^(03|05|07|08|09)\\d{8}$|^$", message = "Số điện thoại người đặt hộ không hợp lệ (phải gồm 10 số đầu 03/05/07/08/09)")
    private String bookerPhone;

    @Size(max = 50, message = "Mối quan hệ không vượt quá 50 ký tự")
    private String relationship;
}

