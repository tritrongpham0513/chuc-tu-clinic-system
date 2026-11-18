package com.chuctupk.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SmsServiceTest {

    @Test
    @DisplayName("Ticket 8: buildEsmsContent khi otpCode null trả về nguyên văn content, không chứa chữ null")
    void testBuildEsmsContent_WhenOtpCodeNull_ReturnsContentDirectly() {
        SmsServiceImpl smsService = new SmsServiceImpl();
        ReflectionTestUtils.setField(smsService, "brandname", "Baotrixemay");

        String result = smsService.buildEsmsContent("Nhac lich 08:00", null);

        assertEquals("Nhac lich 08:00", result);
        assertFalse(result.contains("null"), "Nội dung không được chứa chuỗi 'null'");
    }

    @Test
    @DisplayName("Ticket 8: buildEsmsContent khi có otpCode và brandname thì dùng template OTP chứa mã xác thực")
    void testBuildEsmsContent_WhenOtpCodePresent_UsesTemplateWithOtp() {
        SmsServiceImpl smsService = new SmsServiceImpl();
        ReflectionTestUtils.setField(smsService, "brandname", "Baotrixemay");

        String result = smsService.buildEsmsContent("x", "123456");

        assertTrue(result.contains("123456"), "Nội dung phải chứa mã xác nhận 123456");
        assertEquals("Ma xac nhan cua ban la 123456", result);
        assertFalse(result.contains("null"), "Nội dung không được chứa chuỗi 'null'");
    }

    @Test
    @DisplayName("Ticket 8: buildEsmsContent khi không có brandname thì luôn trả về nguyên văn content")
    void testBuildEsmsContent_WhenBrandnameBlank_ReturnsContent() {
        SmsServiceImpl smsService = new SmsServiceImpl();
        ReflectionTestUtils.setField(smsService, "brandname", "");

        String resultWithOtp = smsService.buildEsmsContent("Thong bao kham", "654321");
        assertEquals("Thong bao kham", resultWithOtp);

        String resultWithoutOtp = smsService.buildEsmsContent("Thong bao kham", null);
        assertEquals("Thong bao kham", resultWithoutOtp);
    }
}
