package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfileUpsertRequest {

    @NotBlank(message = "Cô chú vui lòng nhập họ và tên")
    private String fullName;

    private String phone;
    private Integer birthYear;
    private String dob;
    private String gender;
    private String relationship;
    private String address;
    private String medicalHistory;
    private String expectedUpdatedAt;
}

