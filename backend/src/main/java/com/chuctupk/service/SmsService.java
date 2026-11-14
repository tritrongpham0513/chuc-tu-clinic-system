package com.chuctupk.service;

public interface SmsService {

    /**
     * Gửi mã OTP tới số điện thoại.
     *
     * @return true nếu đã gửi (hoặc đã ghi log ở chế độ giả lập)
     */
    boolean sendOtp(String phone, String otpCode);

    /**
     * Cổng SMS có thật sự đang hoạt động (đã bật + có API Key) hay không.
     *
     * <p>Khi trả về {@code false}, hệ thống đang ở chế độ giả lập/kiểm thử và
     * được phép trả thêm {@code devCode} trong phản hồi để đội phát triển thử
     * nghiệm. Khi trả về {@code true}, TUYỆT ĐỐI không trả mã OTP ra ngoài.</p>
     */
    default boolean isEnabled() {
        return false;
    }
}
