package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffForgotPasswordVerifyRequest {

    private String email;
    private String identifier;

    @NotBlank(message = "Mã xác thực không được để trống")
    private String code;

    public String getEffectiveIdentifier() {
        if (identifier != null && !identifier.isBlank()) return identifier.trim();
        if (email != null && !email.isBlank()) return email.trim();
        return "";
    }
}

