package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientLoginRequest {

    @NotBlank(message = "Cô chú vui lòng nhập số điện thoại")
    private String phone;
}

