package com.chuctupk.service;

public interface ClinicSettingService {
    int getBookingAdvanceDays();
    int getBookingMinHours();
    String getSettingValue(String key, String defaultValue);
    void setSettingValue(String key, String value);
}

