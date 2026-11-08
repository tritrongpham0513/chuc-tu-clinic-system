package com.chuctupk.dto.response;

import com.chuctupk.entity.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserAdminDetailResponse {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private String email;
    private Role role;
    private String roleLabel;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // Thông tin bác sĩ (nếu role == DOCTOR)
    private Long doctorId;
    private String doctorTitle;
    private String doctorSpecialty;
}

