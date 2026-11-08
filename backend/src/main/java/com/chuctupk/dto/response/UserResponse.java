package com.chuctupk.dto.response;

import com.chuctupk.entity.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private String email;
    private Role role;
}

