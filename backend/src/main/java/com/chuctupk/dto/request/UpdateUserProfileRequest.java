package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotNull(message = "Năm sinh không được để trống")
    private Integer yearOfBirth;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    private String address;

    private String email;
}
