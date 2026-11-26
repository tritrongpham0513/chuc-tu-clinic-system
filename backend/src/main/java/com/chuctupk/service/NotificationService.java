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

    /**
     * Soạn nội dung nhắc lịch thủ công (1 click) cho nhân viên lễ tân.
     */
    String composeManualReminder(Appointment appointment);

    /**
     * Lấy danh sách nhật ký thông báo phục vụ màn hình Admin.
     *
     * @param status trạng thái lọc (SENT, FAILED, SKIPPED) hoặc null nếu lấy tất cả
     * @return danh sách AdminNotificationDto kèm errorCode đã dịch sang tiếng Việt
     */
    java.util.List<com.chuctupk.dto.response.AdminNotificationDto> getAdminNotifications(String status);

    /**
     * Lấy danh sách nhật ký thông báo có giới hạn trang phục vụ màn hình Admin.
     */
    default java.util.List<com.chuctupk.dto.response.AdminNotificationDto> getAdminNotifications(String status, int page, int limit) {
        return getAdminNotifications(status);
    }

    /**
     * Gửi lại 1 tin nhắn thông báo (tạo dòng log mới, không sửa dòng cũ).
     *
     * @param logId ID dòng log cũ cần gửi lại
     * @return AdminNotificationDto của dòng log mới vừa tạo
     */
    com.chuctupk.dto.response.AdminNotificationDto resendNotification(Long logId);
}
