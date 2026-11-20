package com.chuctupk.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDoctorRequest {

    @NotBlank(message = "Họ và tên bác sĩ không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @Size(max = 50, message = "Chức danh tối đa 50 ký tự")
    private String title;

    @NotBlank(message = "Chuyên khoa không được để trống")
    @Size(max = 100, message = "Chuyên khoa tối đa 100 ký tự")
    private String specialty;

    @Min(value = 0, message = "Số năm kinh nghiệm không được âm")
    @Max(value = 80, message = "Số năm kinh nghiệm không hợp lệ")
    private Integer experienceYears;

    private String bio;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(03|05|07|08|09)\\d{8}$", message = "Số điện thoại không đúng định dạng Việt Nam (10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09)")
    private String phone;

    @Size(max = 500, message = "Đường dẫn ảnh đại diện tối đa 500 ký tự")
    private String avatarUrl;

    private Integer sortOrder;

    private Long accountId;
}
