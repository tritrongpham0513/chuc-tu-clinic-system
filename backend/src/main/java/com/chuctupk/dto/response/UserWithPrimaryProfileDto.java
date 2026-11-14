package com.chuctupk.dto.response;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.User;

public record UserWithPrimaryProfileDto(
        String id,
        String username,
        String fullName,
        String phone,
        String email,
        String role,
        String dob,
        Integer birthYear,
        String gender,
        String address,
        boolean mustChangePassword,
        boolean isActive,
        boolean active,
        String infoUpdatedAt
) {
    public static UserWithPrimaryProfileDto of(User u, Patient primaryPatient) {
        if (u == null) return null;
        boolean act = Boolean.TRUE.equals(u.getIsActive());

        String dob = primaryPatient != null ? ContractMapper.formatDate(primaryPatient.getDob()) : null;
        Integer birthYear = primaryPatient != null && primaryPatient.getYearOfBirth() != null ? primaryPatient.getYearOfBirth() : 1970;
        String gender = primaryPatient != null ? ContractMapper.toFeGender(primaryPatient.getGender()) : "NAM";
        String address = primaryPatient != null && primaryPatient.getAddress() != null ? primaryPatient.getAddress() : "";
        String infoUpdatedAt = primaryPatient != null && primaryPatient.getInfoUpdatedAt() != null
                ? ContractMapper.formatIso(primaryPatient.getInfoUpdatedAt())
                : ContractMapper.formatIso(u.getUpdatedAt());

        return new UserWithPrimaryProfileDto(
                String.valueOf(u.getId()),
                u.getUsername() != null ? u.getUsername() : "",
                u.getFullName() != null ? u.getFullName() : "",
                u.getPhone() != null ? u.getPhone() : "",
                u.getEmail() != null ? u.getEmail() : "",
                ContractMapper.toFeRole(u.getRole()),
                dob,
                birthYear,
                gender,
                address,
                Boolean.TRUE.equals(u.getMustChangePassword()),
                act,
                act,
                infoUpdatedAt
        );
    }
}

