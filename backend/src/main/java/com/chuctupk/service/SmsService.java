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
     * Cổng SMS có thật sự đang hoạt động (đã bật + có API Key) hay không.
     */
    default boolean isEnabled() {
        return false;
    }
}
