package com.chuctupk.service;

import com.chuctupk.dto.request.CreateHolidayRequest;
import com.chuctupk.dto.request.UpdateHolidayRequest;
import com.chuctupk.dto.response.AdminHolidayResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdminHolidayService {
    List<AdminHolidayResponse> getHolidays(Integer year);
    AdminHolidayResponse createHoliday(CreateHolidayRequest request);
    AdminHolidayResponse updateHoliday(Long id, UpdateHolidayRequest request);
    void deleteHoliday(Long id);
    long getAppointmentCount(LocalDate date);
}
