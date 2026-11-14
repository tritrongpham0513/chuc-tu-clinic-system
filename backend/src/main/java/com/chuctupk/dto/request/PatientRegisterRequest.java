package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRegisterRequest {

    @NotBlank(message = "Cô chú vui lòng nhập số điện thoại")
    private String phone;

    private String fullName;
    private Integer birthYear;
    private String dob;
    private String gender;
    private String address;
}

