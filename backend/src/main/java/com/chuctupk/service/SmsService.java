package com.chuctupk.service;

public interface SmsService {

    enum OtpSendResult {
        SENT,
        SKIPPED,
        FAILED
    }

    /**
     * Gửi mã OTP tới số điện thoại.
     *
     * @param phone số điện thoại người nhận
     * @param otpCode mã OTP xác thực
     * @return OtpSendResult (SENT, SKIPPED, FAILED)
     */
    OtpSendResult sendOtp(String phone, String otpCode);

    /**
     * Gửi tin nhắn thông báo (không phải mã OTP).
     */
    default OtpSendResult sendMessage(String phone, String message) {
        return OtpSendResult.SKIPPED;
    }

    /**
     * Cổng SMS có thật sự đang hoạt động (đã bật + có API Key) hay không.
     */
    default boolean isEnabled() {
        return false;
    }
}
