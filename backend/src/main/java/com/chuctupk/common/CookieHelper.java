package com.chuctupk.common;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

/**
 * Tiện ích quản lý Cookie an toàn cho phiên đăng nhập (chống XSS, tuân thủ
 * SameSite=Lax, HttpOnly).
 */
public final class CookieHelper {

        public static final String SESSION_COOKIE_NAME = "ct_session";
        public static final String LEGACY_TOKEN_COOKIE_NAME = "ct_token";
        public static final String DEVICE_COOKIE_NAME = "ct_device";

        /**
         * Cookie chứa Refresh Token thật (HttpOnly, chỉ gửi tới endpoint làm mới phiên).
         * Tách riêng khỏi cookie phiên để tránh nhầm lẫn giữa Access Token (JWT) và Refresh Token.
         */
        public static final String REFRESH_COOKIE_NAME = "ct_refresh";
        public static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

        private CookieHelper() {
        }

        public static void addRefreshCookie(HttpServletResponse response, String refreshToken, int maxAgeSeconds,
                        boolean isSecure) {
                ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                                .httpOnly(true)
                                .secure(isSecure)
                                .path(REFRESH_COOKIE_PATH)
                                .maxAge(maxAgeSeconds)
                                .sameSite("Lax")
                                .build();
                response.addHeader("Set-Cookie", refreshCookie.toString());
        }

        public static void clearRefreshCookie(HttpServletResponse response, boolean isSecure) {
                ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                                .httpOnly(true)
                                .secure(isSecure)
                                .path(REFRESH_COOKIE_PATH)
                                .maxAge(0)
                                .sameSite("Lax")
                                .build();
                response.addHeader("Set-Cookie", refreshCookie.toString());
        }

        public static void addSessionCookie(HttpServletResponse response, String token, int maxAgeSeconds,
                        boolean isSecure) {
                ResponseCookie sessionCookie = ResponseCookie.from(SESSION_COOKIE_NAME, token)
                                .httpOnly(true)
                                .secure(isSecure)
                                .path("/")
                                .maxAge(maxAgeSeconds)
                                .sameSite("Lax")
                                .build();
                response.addHeader("Set-Cookie", sessionCookie.toString());

                ResponseCookie legacyCookie = ResponseCookie.from(LEGACY_TOKEN_COOKIE_NAME, token)
                                .httpOnly(false)
                                .secure(isSecure)
                                .path("/")
                                .maxAge(maxAgeSeconds)
                                .sameSite("Lax")
                                .build();
                response.addHeader("Set-Cookie", legacyCookie.toString());
        }

        public static void clearSessionCookie(HttpServletResponse response, boolean isSecure) {
                ResponseCookie sessionCookie = ResponseCookie.from(SESSION_COOKIE_NAME, "")
                                .httpOnly(true)
                                .secure(isSecure)
                                .path("/")
                                .maxAge(0)
                                .sameSite("Lax")
                                .build();
                response.addHeader("Set-Cookie", sessionCookie.toString());

                ResponseCookie legacyCookie = ResponseCookie.from(LEGACY_TOKEN_COOKIE_NAME, "")
                                .httpOnly(false)
                                .secure(isSecure)
                                .path("/")
                                .maxAge(0)
                                .sameSite("Lax")
                                .build();
                response.addHeader("Set-Cookie", legacyCookie.toString());
        }

        public static void addDeviceCookie(HttpServletResponse response, String deviceId, boolean isSecure) {
                ResponseCookie deviceCookie = ResponseCookie.from(DEVICE_COOKIE_NAME, deviceId)
                                .httpOnly(true)
                                .secure(isSecure)
                                .path("/")
                                .maxAge(365L * 24 * 3600) // 1 năm
                                .sameSite("Lax")
                                .build();
                response.addHeader("Set-Cookie", deviceCookie.toString());
        }

        public static void clearDeviceCookie(HttpServletResponse response, boolean isSecure) {
                ResponseCookie deviceCookie = ResponseCookie.from(DEVICE_COOKIE_NAME, "")
                                .httpOnly(true)
                                .secure(isSecure)
                                .path("/")
                                .maxAge(0)
                                .sameSite("Lax")
                                .build();
                response.addHeader("Set-Cookie", deviceCookie.toString());
        }
}
