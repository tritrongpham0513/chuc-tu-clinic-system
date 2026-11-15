package com.chuctupk.service;

import java.time.LocalTime;

public interface ClinicSettingService {
    int getSlotCapacity();
    int getSeats();
    int getUserMaxDays();
    int getCounterMaxDays();
    int getMaxProfiles();
    int getBookingAdvanceDays();
    int getBookingMinHours();
    int getPriorityEarlyMinutes();
    LocalTime getNoShowCancelAt();
    LocalTime getReceptionOpen();
    LocalTime getReceptionClose();
    boolean isAutoInvite();

    String getSettingValue(String key, String defaultValue);
    void setSettingValue(String key, String value);
    void invalidateCache();
}
