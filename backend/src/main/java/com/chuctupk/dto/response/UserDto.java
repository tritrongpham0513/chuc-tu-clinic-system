package com.chuctupk.dto.response;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.User;

/**
 * DTO chuẩn hóa trả về thông tin tài khoản người dùng cho Frontend.
 */
public record UserDto(
        String id,
        String username,
        String fullName,
        String phone,
        String email,
        String role,
        boolean mustChangePassword,
        boolean isActive,
        boolean active,
        String createdAt
) {
    public static UserDto from(User u) {
        if (u == null) return null;
        boolean act = Boolean.TRUE.equals(u.getIsActive());
        return new UserDto(
                String.valueOf(u.getId()),
                u.getUsername() != null ? u.getUsername() : "",
                u.getFullName() != null ? u.getFullName() : "",
                u.getPhone() != null ? u.getPhone() : "",
                u.getEmail() != null ? u.getEmail() : "",
                ContractMapper.toFeRole(u.getRole()),
                Boolean.TRUE.equals(u.getMustChangePassword()),
                act,
                act,
                ContractMapper.formatIso(u.getCreatedAt())
        );
    }
}

