package com.chuctupk.dto.response;

import java.time.LocalDateTime;

public record AppointmentStatusHistoryItemDto(
        LocalDateTime at,
        String oldStatus,
        String oldStatusLabel,
        String newStatus,
        String newStatusLabel,
        String actorName,
        String reason
) {
    public static String getStatusLabel(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "PENDING" -> "Chưa đến";
            case "CONFIRMED" -> "Đã check-in";
            case "IN_PROGRESS" -> "Đang khám";
            case "COMPLETED" -> "Đã khám xong";
            case "CANCELLED" -> "Đã huỷ";
            case "NO_SHOW" -> "Không đến";
            default -> status;
        };
    }
}
