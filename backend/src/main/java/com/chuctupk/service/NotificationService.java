package com.chuctupk.service;

import com.chuctupk.entity.Appointment;

public interface NotificationService {

    /**
     * Gửi thông báo SMS xác nhận đặt lịch khám thành công.
     */
    void notifyBookingSuccess(Appointment appointment);

    /**
     * Gửi thông báo SMS khi phiếu khám bị hủy hoặc ghi nhận vắng mặt (no-show).
     */
    void notifyCancellation(Appointment appointment, String reason);

    /**
     * Quét và gửi tin nhắn SMS nhắc lịch khám trước giờ hẹn 2 tiếng.
     */
    void sendUpcomingReminders();
}
