package com.chuctupk;

import com.chuctupk.dto.request.StaffLoginRequest;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đăng nhập khu vực nội bộ bằng tài khoản/mật khẩu (luồng thật của lễ tân, bác sĩ, admin).
 */
@SpringBootTest
@ActiveProfiles("test")
class AdminDirectLoginTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Đăng nhập nội bộ bằng tài khoản/mật khẩu trả về JWT hợp lệ")
    void testAdminLogin() {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "test_letan_" + suffix;
        String rawPassword = "MatKhauTest@2026";

        User staff = userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName("Lễ tân kiểm thử")
                .phone("09" + suffix.substring(suffix.length() - 8))
                .email(username + "@chuctu.test")
                .role(Role.STAFF)
                .isActive(true)
                .mustChangePassword(false)
                .failedLoginCount(0)
                .build());

        try {
            Map<String, Object> res = authService.loginStaff(
                    new StaffLoginRequest(username, rawPassword), null);

            assertThat(res.get("token")).as("Phải trả về JWT").isNotNull();
            assertThat(res.get("user")).isNotNull();
        } finally {
            userRepository.delete(staff);
        }
    }
}
