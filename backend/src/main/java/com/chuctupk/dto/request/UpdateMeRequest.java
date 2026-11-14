package com.chuctupk.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMeRequest {
    private String fullName;
    private String dob;
    private String gender;
    private String address;
    private String email;
    private String expectedUpdatedAt;
}

