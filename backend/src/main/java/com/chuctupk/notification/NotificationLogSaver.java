package com.chuctupk.notification;

import com.chuctupk.entity.NotificationLog;

/**
 * Interface trừu tượng hóa việc lưu trữ notification_log.
 * Cho phép tách biệt transaction ghi log ra khỏi transaction nghiệp vụ của người gọi.
 */
public interface NotificationLogSaver {
    void save(NotificationLog logEntry);
}
