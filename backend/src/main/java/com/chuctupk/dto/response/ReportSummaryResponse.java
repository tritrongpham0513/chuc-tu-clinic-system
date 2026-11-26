package com.chuctupk.dto.response;

import java.util.List;

/**
 * Phản hồi báo cáo tổng hợp hoạt động khám bệnh theo kỳ.
 * Mọi chuỗi ngày giờ theo chuẩn ISO không kèm timezone offset.
 */
public record ReportSummaryResponse(
        String from,
        String to,
        String generatedAt,
        Totals totals,
        List<DayItem> byDay,
        List<DoctorItem> byDoctor,
        List<SlotItem> bySlot
) {
    public record Totals(
            int booked,
            int completed,
            int cancelled,
            int noShow,
            int newPatients,
            int returningPatients
    ) {}

    public record DayItem(
            String date,
            int booked,
            int completed,
            int cancelled,
            int noShow,
            double fillRatePct
    ) {}

    public record DoctorItem(
            Long doctorId,
            String doctorName,
            int booked,
            int completed,
            int cancelled,
            int noShow
    ) {}

    public record SlotItem(
            Long slotId,
            String slotLabel,
            int booked,
            int completed,
            double fillRatePct
    ) {}
}
