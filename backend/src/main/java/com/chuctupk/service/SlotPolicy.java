package com.chuctupk.service;

import com.chuctupk.entity.TimeSlot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Nguồn sự thật duy nhất (Single Source of Truth) để đánh giá khung giờ có được phép đặt hay không.
 * Đồng bộ quy tắc cho cả Bệnh nhân (Patient Portal) và Tiếp tân tại quầy (Counter).
 */
@Service
@RequiredArgsConstructor
public class SlotPolicy {

    public enum SlotStatus {
        OK,
        PAST,
        TOO_LATE,
        TOO_FAR,
        OUTSIDE_RECEPTION,
        FULL;

        public String toReason() {
            return this == FULL ? "SLOT_FULL" : this.name();
        }
    }

    public enum BookingChannel {
        PATIENT_PORTAL,
        COUNTER
    }

    private final ClinicSettingService clinicSettingService;

    /**
     * Đánh giá tính khả dụng của khung giờ theo thứ tự ưu tiên chuẩn mực:
     * 1. date < today -> PAST
     * 2. date > today + maxAdvanceDays -> TOO_FAR
     * 3. slot.start < receptionOpen HOẶC slot.end > receptionClose -> OUTSIDE_RECEPTION
     * 4. date == today và slot.start < now + minHours*60p -> TOO_LATE
     * 5. remainingSeats <= 0 -> FULL
     * 6. Tất cả thỏa mãn -> OK
     */
    public SlotStatus evaluate(LocalDate date, TimeSlot slot, LocalDateTime now, BookingChannel channel, int remainingSeats) {
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        // 1. Ngày trong quá khứ
        if (date.isBefore(today)) {
            return SlotStatus.PAST;
        }

        // 2. Vượt quá số ngày đặt trước tối đa (user_max_days làm chuẩn, fallback booking_advance_days)
        int maxAdvanceDays = clinicSettingService.getUserMaxDays();
        if (maxAdvanceDays <= 0) {
            maxAdvanceDays = clinicSettingService.getBookingAdvanceDays();
        }
        if (date.isAfter(today.plusDays(maxAdvanceDays))) {
            return SlotStatus.TOO_FAR;
        }

        // 3. Ngoài khung giờ tiếp nhận của phòng khám (toàn bộ khung giờ phải nằm trong giờ mở-đóng)
        LocalTime receptionOpen = clinicSettingService.getReceptionOpen();
        LocalTime receptionClose = clinicSettingService.getReceptionClose();
        if (slot.getStartTime().isBefore(receptionOpen) || slot.getEndTime().isAfter(receptionClose)) {
            return SlotStatus.OUTSIDE_RECEPTION;
        }

        // 4. Trong ngày hôm nay: kiểm tra thời gian đặt trước tối thiểu
        if (date.isEqual(today)) {
            long minMinutes;
            if (channel == BookingChannel.PATIENT_PORTAL) {
                int minHours = clinicSettingService.getBookingMinHours();
                minMinutes = minHours * 60L;
            } else {
                minMinutes = 0L; // Lễ tân tại quầy được tiếp nhận sát giờ
            }

            LocalDateTime slotStartDateTime = LocalDateTime.of(date, slot.getStartTime());
            if (slotStartDateTime.isBefore(now.plusMinutes(minMinutes))) {
                return SlotStatus.TOO_LATE;
            }
        }

        // 5. Đã hết chỗ
        if (remainingSeats <= 0) {
            return SlotStatus.FULL;
        }

        return SlotStatus.OK;
    }

    public SlotStatus evaluate(LocalDate date, TimeSlot slot, LocalDateTime now, BookingChannel channel) {
        return evaluate(date, slot, now, channel, 1);
    }
}
