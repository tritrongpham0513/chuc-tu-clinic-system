package com.chuctupk.service;

import com.chuctupk.dto.request.AppointmentCancelByPatientRequest;
import com.chuctupk.dto.request.BookingRequest;
import com.chuctupk.dto.response.AppointmentDetailResponse;
import com.chuctupk.dto.response.SlotAvailabilityResponse;

import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    List<SlotAvailabilityResponse> getAvailableSlots(LocalDate date);
    AppointmentDetailResponse bookAppointment(BookingRequest request);
    List<AppointmentDetailResponse> getMyUpcoming();
    List<AppointmentDetailResponse> getMyHistory();
    AppointmentDetailResponse getMyAppointmentById(Long id);
    AppointmentDetailResponse cancelMyAppointment(Long id, AppointmentCancelByPatientRequest request);
}
