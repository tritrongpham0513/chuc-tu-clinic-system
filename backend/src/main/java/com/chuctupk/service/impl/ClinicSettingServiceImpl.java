package com.chuctupk.service.impl;

import com.chuctupk.entity.ClinicSetting;
import com.chuctupk.repository.ClinicSettingRepository;
import com.chuctupk.service.ClinicSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicSettingServiceImpl implements ClinicSettingService {

    public static final String KEY_SLOT_CAPACITY = "slot_capacity";
    public static final String KEY_PATIENTS_PER_DOCTOR = "patients_per_doctor";
    public static final String KEY_SEATS = "seats";
    public static final String KEY_USER_MAX_DAYS = "user_max_days";
    public static final String KEY_COUNTER_MAX_DAYS = "counter_max_days";
    public static final String KEY_MAX_PROFILES = "max_profiles";
    public static final String KEY_BOOKING_ADVANCE_DAYS = "booking_advance_days";
    public static final String KEY_BOOKING_MIN_HOURS = "booking_min_hours";
    public static final String KEY_PRIORITY_EARLY_MINUTES = "priority_early_minutes";
    public static final String KEY_NO_SHOW_CANCEL_AT = "no_show_cancel_at";
    public static final String KEY_RECEPTION_OPEN = "reception_open";
    public static final String KEY_RECEPTION_CLOSE = "reception_close";
    public static final String KEY_AUTO_INVITE = "auto_invite";

    public static final int DEFAULT_SLOT_CAPACITY = 6;
    public static final int DEFAULT_PATIENTS_PER_DOCTOR = 3;
    public static final int DEFAULT_SEATS = 5;
    public static final int DEFAULT_USER_MAX_DAYS = 30;
    public static final int DEFAULT_COUNTER_MAX_DAYS = 7;
    public static final int DEFAULT_MAX_PROFILES = 10;
    public static final int DEFAULT_BOOKING_ADVANCE_DAYS = 14;
    public static final int DEFAULT_BOOKING_MIN_HOURS = 1;
    public static final int DEFAULT_PRIORITY_EARLY_MINUTES = 15;
    public static final LocalTime DEFAULT_NO_SHOW_CANCEL_AT = LocalTime.of(17, 0);
    public static final LocalTime DEFAULT_RECEPTION_OPEN = LocalTime.of(8, 0);
    public static final LocalTime DEFAULT_RECEPTION_CLOSE = LocalTime.of(16, 30);
    public static final boolean DEFAULT_AUTO_INVITE = false;

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L; // 5 phút

    private record CacheEntry(String value, long expiresAt) {}
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private final ClinicSettingRepository clinicSettingRepository;

    @Override
    public int getSlotCapacity() {
        return getIntSetting(KEY_SLOT_CAPACITY, DEFAULT_SLOT_CAPACITY);
    }

    @Override
    public int getPatientsPerDoctor() {
        return getIntSetting(KEY_PATIENTS_PER_DOCTOR, DEFAULT_PATIENTS_PER_DOCTOR);
    }

    @Override
    public int getSeats() {
        return getIntSetting(KEY_SEATS, DEFAULT_SEATS);
    }

    @Override
    public int getUserMaxDays() {
        return getIntSetting(KEY_USER_MAX_DAYS, DEFAULT_USER_MAX_DAYS);
    }

    @Override
    public int getCounterMaxDays() {
        return getIntSetting(KEY_COUNTER_MAX_DAYS, DEFAULT_COUNTER_MAX_DAYS);
    }

    @Override
    public int getMaxProfiles() {
        return getIntSetting(KEY_MAX_PROFILES, DEFAULT_MAX_PROFILES);
    }

    @Override
    public int getBookingAdvanceDays() {
        return getIntSetting(KEY_BOOKING_ADVANCE_DAYS, DEFAULT_BOOKING_ADVANCE_DAYS);
    }

    @Override
    public int getBookingMinHours() {
        return getIntSetting(KEY_BOOKING_MIN_HOURS, DEFAULT_BOOKING_MIN_HOURS);
    }

    @Override
    public int getPriorityEarlyMinutes() {
        return getIntSetting(KEY_PRIORITY_EARLY_MINUTES, DEFAULT_PRIORITY_EARLY_MINUTES);
    }

    @Override
    public LocalTime getNoShowCancelAt() {
        String val = getSettingValue(KEY_NO_SHOW_CANCEL_AT, "17:00");
        try {
            return LocalTime.parse(val.trim());
        } catch (DateTimeParseException e) {
            log.warn("Không thể parse no_show_cancel_at '{}', dùng mặc định: {}", val, DEFAULT_NO_SHOW_CANCEL_AT);
            return DEFAULT_NO_SHOW_CANCEL_AT;
        }
    }

    @Override
    public LocalTime getReceptionOpen() {
        String val = getSettingValue(KEY_RECEPTION_OPEN, "08:00");
        try {
            return LocalTime.parse(val.trim());
        } catch (DateTimeParseException e) {
            log.warn("Không thể parse reception_open '{}', dùng mặc định: {}", val, DEFAULT_RECEPTION_OPEN);
            return DEFAULT_RECEPTION_OPEN;
        }
    }

    @Override
    public LocalTime getReceptionClose() {
        String val = getSettingValue(KEY_RECEPTION_CLOSE, "16:30");
        try {
            return LocalTime.parse(val.trim());
        } catch (DateTimeParseException e) {
            log.warn("Không thể parse reception_close '{}', dùng mặc định: {}", val, DEFAULT_RECEPTION_CLOSE);
            return DEFAULT_RECEPTION_CLOSE;
        }
    }

    @Override
    public boolean isAutoInvite() {
        String val = getSettingValue(KEY_AUTO_INVITE, "0");
        return "1".equals(val.trim()) || "true".equalsIgnoreCase(val.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        Map<String, String> result = new java.util.LinkedHashMap<>();
        result.put(KEY_SLOT_CAPACITY, String.valueOf(getSlotCapacity()));
        result.put(KEY_PATIENTS_PER_DOCTOR, String.valueOf(getPatientsPerDoctor()));
        result.put(KEY_SEATS, String.valueOf(getSeats()));
        result.put(KEY_USER_MAX_DAYS, String.valueOf(getUserMaxDays()));
        result.put(KEY_COUNTER_MAX_DAYS, String.valueOf(getCounterMaxDays()));
        result.put(KEY_MAX_PROFILES, String.valueOf(getMaxProfiles()));
        result.put(KEY_BOOKING_ADVANCE_DAYS, String.valueOf(getBookingAdvanceDays()));
        result.put(KEY_BOOKING_MIN_HOURS, String.valueOf(getBookingMinHours()));
        result.put(KEY_PRIORITY_EARLY_MINUTES, String.valueOf(getPriorityEarlyMinutes()));
        result.put(KEY_NO_SHOW_CANCEL_AT, getNoShowCancelAt().toString());
        result.put(KEY_RECEPTION_OPEN, getReceptionOpen().toString());
        result.put(KEY_RECEPTION_CLOSE, getReceptionClose().toString());
        result.put(KEY_AUTO_INVITE, isAutoInvite() ? "1" : "0");
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(key);
        if (entry != null && entry.expiresAt() > now) {
            return entry.value();
        }

        String dbVal = clinicSettingRepository.findById(key)
                .map(ClinicSetting::getSettingValue)
                .filter(val -> val != null && !val.isBlank())
                .orElse(defaultValue);

        cache.put(key, new CacheEntry(dbVal, now + CACHE_TTL_MS));
        return dbVal;
    }

    @Override
    @Transactional
    public void setSettingValue(String key, String value) {
        ClinicSetting setting = clinicSettingRepository.findById(key)
                .orElseGet(() -> ClinicSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        clinicSettingRepository.save(setting);
        invalidateCache();
    }

    @Override
    public void invalidateCache() {
        cache.clear();
    }

    private int getIntSetting(String key, int defaultValue) {
        String rawValue = getSettingValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException e) {
            log.warn("Không thể parse cấu hình '{}' với giá trị '{}'. Sử dụng mặc định: {}", key, rawValue, defaultValue);
            return defaultValue;
        }
    }
}
