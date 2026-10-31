package com.chuctupk.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {
    private Long id;
    private String fullName;
    private String title;
    private String specialty;
    private Integer experienceYears;
    private String bio;
    private String avatarUrl;
}

