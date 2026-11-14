package com.chuctupk.service.impl;

import com.chuctupk.entity.ClinicSetting;
import com.chuctupk.repository.ClinicSettingRepository;
import com.chuctupk.service.ClinicSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicSettingServiceImpl implements ClinicSettingService {

    public static final String KEY_BOOKING_ADVANCE_DAYS = "booking_advance_days";
    public static final String KEY_BOOKING_MIN_HOURS = "booking_min_hours";

    public static final int DEFAULT_BOOKING_ADVANCE_DAYS = 14;
    public static final int DEFAULT_BOOKING_MIN_HOURS = 2;

    private final ClinicSettingRepository clinicSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public int getBookingAdvanceDays() {
        return getIntSetting(KEY_BOOKING_ADVANCE_DAYS, DEFAULT_BOOKING_ADVANCE_DAYS);
    }

    @Override
    @Transactional(readOnly = true)
    public int getBookingMinHours() {
        return getIntSetting(KEY_BOOKING_MIN_HOURS, DEFAULT_BOOKING_MIN_HOURS);
    }

    @Override
    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        return clinicSettingRepository.findById(key)
                .map(ClinicSetting::getSettingValue)
                .filter(val -> val != null && !val.isBlank())
                .orElse(defaultValue);
    }

    @Override
    @Transactional
    public void setSettingValue(String key, String value) {
        ClinicSetting setting = clinicSettingRepository.findById(key)
                .orElseGet(() -> ClinicSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        clinicSettingRepository.save(setting);
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

