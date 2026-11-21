package com.chuctupk.service;

import com.chuctupk.dto.response.AdminSettingsResponse;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.service.impl.AdminSettingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSettingsService Unit Tests")
class AdminSettingsServiceTest {

    @Mock
    private ClinicSettingService clinicSettingService;

    @Mock
    private AppointmentRepository appointmentRepository;

    private AdminSettingsService adminSettingsService;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @BeforeEach
    void setUp() {
        adminSettingsService = new AdminSettingsServiceImpl(clinicSettingService, appointmentRepository, fixedClock);
    }

    @Test
    @DisplayName("updateSettings với khoá ngoài whitelist ném 422")
    void testUpdateSettings_UnknownKey_Throws422() {
        Map<String, String> payload = Map.of("hack_key", "value");
        AppException ex = assertThrows(AppException.class, () ->
                adminSettingsService.updateSettings(payload, "admin")
        );
        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("hack_key"));
    }

    @Test
    @DisplayName("updateSettings với giá trị số nguyên ngoài khoảng ném 422")
    void testUpdateSettings_IntOutOfRange_Throws422() {
        // slot_capacity > 50
        assertThrows(AppException.class, () ->
                adminSettingsService.updateSettings(Map.of("slot_capacity", "51"), "admin")
        );

        // slot_capacity < 1
        assertThrows(AppException.class, () ->
                adminSettingsService.updateSettings(Map.of("slot_capacity", "0"), "admin")
        );

        // patients_per_doctor > 10
        assertThrows(AppException.class, () ->
                adminSettingsService.updateSettings(Map.of("patients_per_doctor", "11"), "admin")
        );

        // seats > 20
        assertThrows(AppException.class, () ->
                adminSettingsService.updateSettings(Map.of("seats", "25"), "admin")
        );
    }

    @Test
    @DisplayName("updateSettings với giờ mở cửa sau giờ đóng cửa ném 422")
    void testUpdateSettings_OpenAfterClose_Throws422() {
        when(clinicSettingService.getReceptionClose()).thenReturn(LocalTime.of(16, 30));

        Map<String, String> payload = Map.of("reception_open", "17:00");
        AppException ex = assertThrows(AppException.class, () ->
                adminSettingsService.updateSettings(payload, "admin")
        );
        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("reception_open"));
    }

    @Test
    @DisplayName("updateSettings cảnh báo khi giảm slot_capacity thấp hơn số đã đặt trong tương lai")
    void testUpdateSettings_WarnsWhenLowerThanFutureBookings() {
        when(appointmentRepository.findFutureSlotBookingCounts(any(LocalDate.class), any(Pageable.class)))
                .thenReturn(List.of(7L));

        Map<String, String> current = new HashMap<>();
        current.put("slot_capacity", "8");
        when(clinicSettingService.getAllSettings()).thenReturn(current);

        AdminSettingsResponse res = adminSettingsService.updateSettings(Map.of("slot_capacity", "5"), "admin");
        assertNotNull(res.getWarnings());
        assertFalse(res.getWarnings().isEmpty());
        assertTrue(res.getWarnings().get(0).contains("7"));
        verify(clinicSettingService).setSettingValue("slot_capacity", "5");
    }
}
