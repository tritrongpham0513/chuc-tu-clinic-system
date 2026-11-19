package com.chuctupk.notification;

import com.chuctupk.entity.NotificationLog;
import com.chuctupk.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triển khai lưu notification_log vào CSDL hoà vào transaction hiện tại (REQUIRED).
 * Dùng chung transaction với nghiệp vụ (đặt lịch, hủy lịch) để tránh deadlock / lock wait
 * khi kiểm tra khóa ngoại (FK) trỏ tới dòng appointment vừa insert trong transaction cha
 * mà chưa commit.
 */
@Component
@RequiredArgsConstructor
public class JpaNotificationLogSaver implements NotificationLogSaver {

    private final NotificationLogRepository notificationLogRepository;

    @Override
    @Transactional
    public void save(NotificationLog logEntry) {
        notificationLogRepository.save(logEntry);
    }
}
