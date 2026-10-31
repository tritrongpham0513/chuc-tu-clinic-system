package com.chuctupk.service;

import com.chuctupk.dto.response.TimeSlotResponse;
import java.time.LocalDate;
import java.util.List;

public interface TimeSlotService {
    List<TimeSlotResponse> getAvailableTimeSlots(LocalDate date, Long doctorId);
}

