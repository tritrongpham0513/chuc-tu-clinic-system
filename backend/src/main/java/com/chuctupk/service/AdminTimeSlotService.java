package com.chuctupk.service;

import com.chuctupk.dto.request.CreateTimeSlotRequest;
import com.chuctupk.dto.request.UpdateTimeSlotRequest;
import com.chuctupk.dto.response.AdminTimeSlotResponse;

import java.util.List;

public interface AdminTimeSlotService {
    List<AdminTimeSlotResponse> getAllTimeSlots();
    AdminTimeSlotResponse createTimeSlot(CreateTimeSlotRequest request);
    AdminTimeSlotResponse updateTimeSlot(Long id, UpdateTimeSlotRequest request);
    AdminTimeSlotResponse activateTimeSlot(Long id);
    AdminTimeSlotResponse deactivateTimeSlot(Long id);
}
