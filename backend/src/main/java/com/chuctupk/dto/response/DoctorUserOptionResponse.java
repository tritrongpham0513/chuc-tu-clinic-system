package com.chuctupk.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorUserOptionResponse {

    private Long id;
    private String username;
    private String fullName;
    private String phone;
}
