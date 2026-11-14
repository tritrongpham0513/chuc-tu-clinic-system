package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpResendRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phone;

    private String purpose;
}

