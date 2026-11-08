package com.chuctupk.dto.request;

import com.chuctupk.entity.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "Vai trò (Role) không được để trống")
    private Role role; // ADMIN, STAFF, DOCTOR, PATIENT

    private String title;
    private String specialty;
}

