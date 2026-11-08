package com.chuctupk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStaffProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    private String email;
}
