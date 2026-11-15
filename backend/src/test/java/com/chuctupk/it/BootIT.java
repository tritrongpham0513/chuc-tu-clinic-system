package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BootIT {

    @Autowired
    private ApplicationContext applicationContext;

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
}
