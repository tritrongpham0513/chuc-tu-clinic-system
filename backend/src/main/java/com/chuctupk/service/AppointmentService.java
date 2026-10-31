package com.chuctupk.service;

import com.chuctupk.dto.request.AppointmentCancelRequest;
import com.chuctupk.dto.request.AppointmentCreateRequest;
import com.chuctupk.dto.response.AppointmentResponse;

public interface AppointmentService {
    AppointmentResponse createAppointment(AppointmentCreateRequest request);
    AppointmentResponse getAppointmentByBookingCode(String bookingCode, String xPhone);
    AppointmentResponse cancelAppointment(String bookingCode, String xPhone, AppointmentCancelRequest request);
}

