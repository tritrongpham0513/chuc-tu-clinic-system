package com.chuctupk.config;

import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.User;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromJwt(jwt);

                // Luôn đọc lại user từ DB để đảm bảo trạng thái khóa và vai trò có hiệu lực ngay
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();

                    if (!Boolean.TRUE.equals(user.getIsActive())) {
                        sendError(response, HttpStatus.FORBIDDEN.value(), ErrorCode.ACCOUNT_LOCKED.getCode(),
                                "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên");
                        return;
                    }

                    // 1. Kiểm tra X-Session-User để phát hiện tab khác đổi tài khoản (chống thao tác nhầm danh tính)
                    String xSessionUser = request.getHeader("X-Session-User");
                    if (StringUtils.hasText(xSessionUser)
                            && !isAuthBypassSessionCheck(path)
                            && !xSessionUser.equals(String.valueOf(user.getId()))) {
                        sendError(response, HttpStatus.CONFLICT.value(), ErrorCode.SESSION_CHANGED.getCode(),
                                "Phiên đăng nhập đã thay đổi ở thẻ khác. Trang sẽ tự động tải lại");
                        return;
                    }

                    // 2. Chặn tài khoản nội bộ chưa đổi mật khẩu tạm vào các chức năng làm việc
                    if (Boolean.TRUE.equals(user.getMustChangePassword())
                            && isStaffOrAdminPath(path)
                            && !isPasswordChangePath(path)) {
                        sendError(response, HttpStatus.FORBIDDEN.value(), "MUST_CHANGE_PASSWORD",
                                "Tài khoản đang dùng mật khẩu tạm do quản trị viên cấp. Cô chú vui lòng đổi mật khẩu mới để bắt đầu làm việc");
                        return;
                    }

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singletonList(authority));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            log.error("Không thể thiết lập xác thực người dùng trong security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthBypassSessionCheck(String path) {
        return path.endsWith("/auth/login") || path.endsWith("/auth/login/verify")
                || path.endsWith("/auth/register") || path.endsWith("/auth/register/verify")
                || path.endsWith("/auth/logout") || path.endsWith("/auth/me")
                || path.endsWith("/auth/refresh");
    }

    private boolean isStaffOrAdminPath(String path) {
        return path.contains("/staff/") || path.contains("/admin/") || path.startsWith("/api/v1/staff") || path.startsWith("/api/v1/admin");
    }

    private boolean isPasswordChangePath(String path) {
        return path.endsWith("/auth/change-password") || path.endsWith("/auth/password") || path.endsWith("/auth/me");
    }

    private void sendError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        ApiResponse<Void> apiResponse = ApiResponse.error(status, code, message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 1. Thử lấy từ header Authorization: Bearer <token>
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Thử lấy từ cookie ct_session (chuẩn mới), ct_token hoặc token
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("ct_session".equals(c.getName()) || "ct_token".equals(c.getName()) || "token".equals(c.getName())) {
                    if (StringUtils.hasText(c.getValue())) {
                        return c.getValue();
                    }
                }
            }
        }

        return null;
    }
}
