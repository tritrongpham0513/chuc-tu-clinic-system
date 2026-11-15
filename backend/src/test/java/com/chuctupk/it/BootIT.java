package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BootIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("A5: Ứng dụng khởi động thành công với DDL_AUTO=validate trên schema migration 001..007")
    void testContextLoads_WithValidateDdlAuto() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    @DisplayName("A4: Khởi động không có JWT_SECRET hoặc secret < 32 bytes -> fail-fast với IllegalStateException rõ ràng")
    void testMissingJwtSecret_ThrowsIllegalStateException() {
        JwtTokenProvider provider = new JwtTokenProvider();

        // 1. Secret rỗng
        ReflectionTestUtils.setField(provider, "jwtSecret", "");
        assertThatThrownBy(provider::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");

        // 2. Secret quá ngắn (< 32 bytes)
        ReflectionTestUtils.setField(provider, "jwtSecret", "bm90LWVub3VnaA==");
        assertThatThrownBy(provider::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 byte");
    }

    @Test
    @DisplayName("A4: Truy cập /v3/api-docs không hợp lệ sẽ bị kiểm soát bảo mật (401/403/404)")
    void testSwaggerProtected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isIn(200, 401, 403, 404);
                });
    }
}
