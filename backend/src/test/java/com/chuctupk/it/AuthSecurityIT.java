package com.chuctupk.it;

import com.chuctupk.dto.request.StaffLoginRequest;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("P0-1: POST /api/v1/auth/quick-login backdoor đã bị xoá hoàn toàn -> 404")
    void testQuickLogin_NotFound() throws Exception {
        mockMvc.perform(post("/api/v1/auth/quick-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0912345678\",\"otpType\":\"LOGIN\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("P1-1: 5 lần sai mật khẩu liên tiếp -> lần 6 bị khóa 423 ACCOUNT_LOCKED (kể cả khi nhập đúng mật khẩu)")
    void testAccountLockout_AfterFiveFailedAttempts() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String username = "lockout_user_" + suffix;
        String correctPassword = "CorrectPassword@2026";
        String wrongPassword = "WrongPassword@9999";

        User user = userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(correctPassword))
                .fullName("Người dùng thử nghiệm")
                .phone("09" + suffix.substring(Math.max(0, suffix.length() - 8)))
                .email(username + "@chuctu.test")
                .role(Role.STAFF)
                .isActive(true)
                .mustChangePassword(false)
                .failedLoginCount(0)
                .build());

        try {
            StaffLoginRequest wrongReq = new StaffLoginRequest(username, wrongPassword);

            // 5 lần thử sai liên tiếp
            for (int i = 1; i <= 5; i++) {
                mockMvc.perform(post("/api/v1/auth/staff/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongReq)))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
            }

            // Lần 6: Kể cả khi nhập đúng mật khẩu, tài khoản vẫn đang bị khóa
            StaffLoginRequest correctReq = new StaffLoginRequest(username, correctPassword);
            mockMvc.perform(post("/api/v1/auth/staff/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(correctReq)))
                    .andExpect(status().is(423))
                    .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                    .andExpect(jsonPath("$.message", containsString("bị tạm khóa")));

        } finally {
            userRepository.delete(user);
        }
    }

    @Test
    @DisplayName("P0-2: Gửi OTP đăng nhập với SĐT hợp lệ, không bị rate-limit bởi số cố định")
    void testSendOtp_ValidPhone() throws Exception {
        String uniquePhone = "098" + String.format("%07d", Math.abs(System.nanoTime() % 10000000));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + uniquePhone + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
