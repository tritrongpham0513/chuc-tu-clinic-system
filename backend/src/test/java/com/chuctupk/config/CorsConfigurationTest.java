package com.chuctupk.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigurationTest {

    @Test
    @DisplayName("CORS SecurityConfig - Cho phép Vercel preview domain và trả về đúng origin")
    void testSecurityCorsAllowsVercelDomain() {
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsStr",
                "http://localhost:*,http://127.0.0.1:*,http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,https://*.vercel.app,https://*.onrender.com");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.addHeader("Origin", "https://chuc-tu-clinic-system-frontend-36dy-git-de0ba2-tritrongpham3105.vercel.app");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(config.getAllowCredentials()).isTrue();

        String allowedOrigin = config.checkOrigin("https://chuc-tu-clinic-system-frontend-36dy-git-de0ba2-tritrongpham3105.vercel.app");
        assertThat(allowedOrigin).isEqualTo("https://chuc-tu-clinic-system-frontend-36dy-git-de0ba2-tritrongpham3105.vercel.app");
    }

    @Test
    @DisplayName("CORS SecurityConfig - Cho phép localhost và Render domain")
    void testSecurityCorsAllowsLocalhostAndRender() {
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsStr",
                "http://localhost:*,http://127.0.0.1:*,http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,https://*.vercel.app,https://*.onrender.com");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();

        assertThat(config.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(config.checkOrigin("https://chuc-tu-clinic-system.onrender.com")).isEqualTo("https://chuc-tu-clinic-system.onrender.com");
        assertThat(config.checkOrigin("https://malicious-site.com")).isNull();
    }
}
