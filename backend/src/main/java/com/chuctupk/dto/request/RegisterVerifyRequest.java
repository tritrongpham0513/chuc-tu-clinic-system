package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterVerifyRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    @NotBlank(message = "Mã xác thực không được để trống")
    private String code;

    private String fullName;
    private Integer birthYear;
    private String dob;
    private String gender;
    private String address;
    private Boolean remember;
}

