package com.chuctupk.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDoctorListResponse {

    private Long id;
    private String fullName;
    private String title;
    private String specialty;
    private Integer experienceYears;
    private String phone; // Đã che số điện thoại (mask)
    private String avatarUrl;
    private Integer sortOrder;
    private Boolean isActive;
    private Long accountId;
    private String accountUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
