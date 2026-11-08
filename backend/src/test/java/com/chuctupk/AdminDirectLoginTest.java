package com.chuctupk;

import com.chuctupk.dto.request.LoginRequest;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AdminDirectLoginTest {

    @Autowired
    private AuthService authService;

    @Test
    void testAdminLogin() {
        LoginRequest req = new LoginRequest("admin", "MatKhauAdmin@2026");
        try {
            AuthResponse res = authService.login(req);
            System.out.println("LOGIN_SUCCESS: token=" + res.getAccessToken());
            System.out.println("USER_ROLE: " + res.getUser().getRole());
            assertThat(res.getAccessToken()).isNotNull();
        } catch (Exception e) {
            System.out.println("LOGIN_ERROR: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

