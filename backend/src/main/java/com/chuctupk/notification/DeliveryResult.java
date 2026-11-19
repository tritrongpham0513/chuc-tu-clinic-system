package com.chuctupk.notification;

public record DeliveryResult(
        Status status,
        String errorCode,
        String providerRef
) {
    public enum Status {
        SENT,
        SKIPPED,
        FAILED
    }
}
