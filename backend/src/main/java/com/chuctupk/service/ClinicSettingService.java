package com.chuctupk.service;

import java.time.LocalTime;
import java.util.Map;

public interface ClinicSettingService {
    String KEY_SLOT_CAPACITY = "slot_capacity";
    String KEY_PATIENTS_PER_DOCTOR = "patients_per_doctor";
    String KEY_SEATS = "seats";
    String KEY_USER_MAX_DAYS = "user_max_days";
    String KEY_COUNTER_MAX_DAYS = "counter_max_days";
    String KEY_MAX_PROFILES = "max_profiles";
    String KEY_BOOKING_ADVANCE_DAYS = "booking_advance_days";
    String KEY_BOOKING_MIN_HOURS = "booking_min_hours";
    String KEY_PRIORITY_EARLY_MINUTES = "priority_early_minutes";
    String KEY_NO_SHOW_CANCEL_AT = "no_show_cancel_at";
    String KEY_RECEPTION_OPEN = "reception_open";
    String KEY_RECEPTION_CLOSE = "reception_close";
    String KEY_AUTO_INVITE = "auto_invite";

    int getSlotCapacity();
    int getPatientsPerDoctor();
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

    Map<String, String> getAllSettings();
    String getSettingValue(String key, String defaultValue);
    void setSettingValue(String key, String value);
    void invalidateCache();
}
