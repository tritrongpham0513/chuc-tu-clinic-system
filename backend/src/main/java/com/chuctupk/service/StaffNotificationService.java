package com.chuctupk.service;

import com.chuctupk.dto.response.StaffNotificationDto;
import com.chuctupk.dto.response.StaffNotificationListResponse;
import com.chuctupk.entity.User;
import com.chuctupk.event.AppointmentCancelledEvent;
import com.chuctupk.event.AppointmentCreatedEvent;

public interface StaffNotificationService {

    void onAppointmentCreated(AppointmentCreatedEvent event);

    void onAppointmentCancelled(AppointmentCancelledEvent event);

    StaffNotificationListResponse getNotifications(User currentUser, int page, int limit);

    long getUnreadCount(User currentUser);

    StaffNotificationDto markRead(User currentUser, Long notificationId);

    int markAllRead(User currentUser);
}
