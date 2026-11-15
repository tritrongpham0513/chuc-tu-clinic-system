package com.chuctupk.service;

import com.chuctupk.entity.TimeSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SlotPolicyTest {

    private ClinicSettingService clinicSettingService;
    private SlotPolicy slotPolicy;

    @BeforeEach
    void setUp() {
        clinicSettingService = Mockito.mock(ClinicSettingService.class);
        when(clinicSettingService.getUserMaxDays()).thenReturn(30);
        when(clinicSettingService.getBookingAdvanceDays()).thenReturn(14);
        when(clinicSettingService.getReceptionOpen()).thenReturn(LocalTime.of(8, 0));
        when(clinicSettingService.getReceptionClose()).thenReturn(LocalTime.of(16, 30));
        when(clinicSettingService.getBookingMinHours()).thenReturn(2);

        slotPolicy = new SlotPolicy(clinicSettingService);
    }

    @Test
    @DisplayName("15:20, booking_min_hours=2: slot 15:00-16:00 -> TOO_LATE; slot 16:00-16:30 -> TOO_LATE")
    void testSlot_TooLate_WhenWithinMinHours() {
        LocalDate today = LocalDate.of(2026, 11, 15);
        LocalDateTime now = LocalDateTime.of(today, LocalTime.of(15, 20));

        // Slot 15:00 - 16:00 đã bắt đầu trước 15:20
        TimeSlot slot1 = TimeSlot.builder()
                .startTime(LocalTime.of(15, 0))
                .endTime(LocalTime.of(16, 0))
                .build();
        assertThat(slotPolicy.evaluate(today, slot1, now, SlotPolicy.BookingChannel.PATIENT_PORTAL, 5))
                .isEqualTo(SlotPolicy.SlotStatus.TOO_LATE);

        // Slot 16:00 - 16:30 bắt đầu lúc 16:00, trong khi cutoff là 15:20 + 2h = 17:20
        TimeSlot slot2 = TimeSlot.builder()
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(16, 30))
                .build();
        assertThat(slotPolicy.evaluate(today, slot2, now, SlotPolicy.BookingChannel.PATIENT_PORTAL, 5))
                .isEqualTo(SlotPolicy.SlotStatus.TOO_LATE);
    }

    @Test
    @DisplayName("15:20, slot 18:00-19:00 -> OUTSIDE_RECEPTION (end > 16:30)")
    void testSlot_OutsideReception_WhenEndAfterClose() {
        LocalDate today = LocalDate.of(2026, 11, 15);
        LocalDateTime now = LocalDateTime.of(today, LocalTime.of(15, 20));

        TimeSlot slot = TimeSlot.builder()
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .build();

        assertThat(slotPolicy.evaluate(today, slot, now, SlotPolicy.BookingChannel.PATIENT_PORTAL, 5))
                .isEqualTo(SlotPolicy.SlotStatus.OUTSIDE_RECEPTION);
    }

    @Test
    @DisplayName("today+29 với user_max_days=30 -> OK; today+31 -> TOO_FAR")
    void testSlot_MaxAdvanceDays() {
        LocalDate today = LocalDate.of(2026, 11, 15);
        LocalDateTime now = LocalDateTime.of(today, LocalTime.of(9, 0));

        TimeSlot slot = TimeSlot.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        // today + 29
        assertThat(slotPolicy.evaluate(today.plusDays(29), slot, now, SlotPolicy.BookingChannel.PATIENT_PORTAL, 5))
                .isEqualTo(SlotPolicy.SlotStatus.OK);

        // today + 31
        assertThat(slotPolicy.evaluate(today.plusDays(31), slot, now, SlotPolicy.BookingChannel.PATIENT_PORTAL, 5))
                .isEqualTo(SlotPolicy.SlotStatus.TOO_FAR);
    }

    @Test
    @DisplayName("Hết chỗ (remainingSeats <= 0) -> FULL")
    void testSlot_Full() {
        LocalDate tomorrow = LocalDate.of(2026, 11, 16);
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 9, 0);

        TimeSlot slot = TimeSlot.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        assertThat(slotPolicy.evaluate(tomorrow, slot, now, SlotPolicy.BookingChannel.PATIENT_PORTAL, 0))
                .isEqualTo(SlotPolicy.SlotStatus.FULL);
    }
}
