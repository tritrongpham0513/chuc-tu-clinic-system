package com.chuctupk.service;

import com.chuctupk.notification.DeliveryResult;

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
     * Gửi tin nhắn thông báo trả về kết quả chi tiết thread-safe (status, errorCode, provider).
     */
    default DeliveryResult sendNotification(String phone, String message) {
        OtpSendResult result = sendMessage(phone, message);
        String err = getLastErrorCode();
        return switch (result) {
            case SENT -> new DeliveryResult(DeliveryResult.Status.SENT, null, getProviderName());
            case SKIPPED -> new DeliveryResult(DeliveryResult.Status.SKIPPED, err, getProviderName());
            case FAILED -> new DeliveryResult(
                    DeliveryResult.Status.FAILED,
                    err != null && !err.isBlank() ? err : "SMS_SEND_FAILED",
                    getProviderName()
            );
        };
    }

    /**
     * Cổng SMS có thật sự đang hoạt động (đã bật + có API Key) hay không.
     */
    default boolean isEnabled() {
        return false;
    }

    /**
     * Tên nhà cung cấp SMS đang được cấu hình (ví dụ: "esms", "speedsms").
     */
    default String getProviderName() {
        return "esms";
    }

    /**
     * Mã lỗi chi tiết của lần gửi gần nhất từ phía nhà cung cấp (nếu có).
     */
    default String getLastErrorCode() {
        return null;
    }
}
