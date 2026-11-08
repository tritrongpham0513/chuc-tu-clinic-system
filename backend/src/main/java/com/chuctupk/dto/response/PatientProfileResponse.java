package com.chuctupk.dto.response;

import com.chuctupk.entity.enums.Gender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PatientProfileResponse {
    private Long id;
    private String patientCode;
    private String fullName;
    private String phone;
    private Integer yearOfBirth;
    private Gender gender;
    private String address;
    private String relationship;
    private String medicalHistory;
    private Boolean isPrimary;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
