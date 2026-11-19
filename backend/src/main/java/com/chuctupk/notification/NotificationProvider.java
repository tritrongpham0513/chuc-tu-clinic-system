package com.chuctupk.notification;

public interface NotificationProvider {
    String channel();                       // "LOG", "SMS", "MANUAL"
    DeliveryResult send(NotificationMessage message);
}
