package com.chuctupk.service;

import com.chuctupk.dto.response.AdminSettingsResponse;

import java.util.Map;

public interface AdminSettingsService {
    AdminSettingsResponse getSettings();
    AdminSettingsResponse updateSettings(Map<String, String> newSettings, String username);
    int getMaxFutureBookedPerSlot();
}
