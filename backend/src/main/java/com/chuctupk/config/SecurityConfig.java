package com.chuctupk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cấu hình bảo mật cho toàn bộ API v1.
 *
 * <p>Quy ước vai trò (khớp enum {@code Role}): ADMIN, DOCTOR, STAFF, PATIENT.
 * Lễ tân dùng vai trò STAFF nên các đường dẫn {@code /api/v1/staff/**}
 * phải cho phép {@code ADMIN} và {@code STAFF}.</p>
 *
 * <p>Chỉ tồn tại một tầng URL chính thức là {@code /api/v1/**}; các alias cũ
 * ({@code /api/**}, {@code /posts}, {@code /uploads}) đã được loại bỏ và sẽ
 * trả về 404.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonMapper jsonMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JsonMapper jsonMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Công khai: xác thực, meta, health
                        .requestMatchers("/api/v1/auth/**", "/api/v1/meta", "/api/v1/health").permitAll()

                        // 2. Công khai (GET): bài viết cẩm nang, ảnh, dịch vụ, bác sĩ, khung giờ,
                        //    khung giờ trống, màn hình hàng đợi TV
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/posts",
                                "/api/v1/posts/**",
                                "/api/v1/uploads/**",
                                "/api/v1/services/**",
                                "/api/v1/doctors/**",
                                "/api/v1/time-slots/**",
                                "/api/v1/booking/slots",
                                "/api/v1/me/availability",
                                "/api/v1/queue/**",
                                "/api/v1/staff/queue",
                                "/api/v1/articles",
                                "/api/v1/articles/**"
                        ).permitAll()

                        // 3. Công khai: đặt lịch, tra cứu và hủy lịch trực tuyến của người bệnh
                        .requestMatchers("/api/v1/appointments/**").permitAll()

                        // 4. Công khai: tài liệu Swagger / OpenAPI
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html").permitAll()

                        // 5. Quản trị: chỉ ADMIN
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 6. Khu vực nội bộ: ADMIN + STAFF (lễ tân) + DOCTOR (bác sĩ xem lịch/phiếu)
                        .requestMatchers("/api/v1/staff/**").hasAnyRole("ADMIN", "STAFF", "DOCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/uploads").hasAnyRole("ADMIN", "STAFF", "DOCTOR")

                        // 7. Mọi đường dẫn chính thức còn lại của v1 đều phải đăng nhập
                        .requestMatchers("/api/v1/**").authenticated()

                        // 8. Alias cũ (/api/**, /posts/**, /uploads/**) đã bị loại bỏ hoàn toàn.
                        //    Cho phép đi tiếp tới bộ định tuyến để Spring MVC trả về 404
                        //    (thay vì 401 do thiếu đăng nhập), đúng hợp đồng v1.
                        .requestMatchers("/api/**", "/posts/**", "/uploads/**").permitAll()

                        // 9. Còn lại phải đăng nhập
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, 401, "UNAUTHORIZED",
                                        "Yêu cầu đăng nhập để thực hiện thao tác này"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, 403, "FORBIDDEN",
                                        "Bạn không có quyền thực hiện thao tác này"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Ghi lỗi theo đúng envelope ApiResponse để Frontend luôn đọc được `code`. */
    private void writeError(jakarta.servlet.http.HttpServletResponse response,
                            int status,
                            String code,
                            String message) throws java.io.IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("statusCode", status);
        body.put("code", code);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));

        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://localhost",
                "http://127.0.0.1",
                "https://*.onrender.com",
                "https://*.vercel.app",
                "https://*.railway.app"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
