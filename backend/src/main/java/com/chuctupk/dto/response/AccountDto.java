package com.chuctupk.dto.response;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountDto(
        String id,
        String username,
        String fullName,
        String phone,
        String email,
        String role,
        boolean isActive,
        boolean mustChangePassword,
        String createdAt,
        String updatedAt,
        String infoUpdatedAt
) {
    public static AccountDto from(User u) {
        if (u == null) return null;
        return new AccountDto(
                String.valueOf(u.getId()),
                u.getUsername(),
                u.getFullName() != null ? u.getFullName() : "",
                u.getPhone() != null ? u.getPhone() : "",
                u.getEmail() != null ? u.getEmail() : "",
                ContractMapper.toFeRole(u.getRole()),
                Boolean.TRUE.equals(u.getIsActive()),
                Boolean.TRUE.equals(u.getMustChangePassword()),
                ContractMapper.formatIso(u.getCreatedAt()),
                ContractMapper.formatIso(u.getUpdatedAt()),
                ContractMapper.formatIso(u.getUpdatedAt())
        );
    }
}
