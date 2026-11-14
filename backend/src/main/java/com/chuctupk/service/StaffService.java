package com.chuctupk.service;

import com.chuctupk.dto.request.CancelAppointmentStaffRequest;
import com.chuctupk.dto.request.RescheduleAppointmentRequest;
import com.chuctupk.dto.request.StaffCreateAppointmentRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.User;

import java.time.LocalDate;
import java.util.Map;

public interface StaffService {

    StaffAppointmentListResponse getAppointments(LocalDate date);

    StaffAppointmentDatesResponse getAppointmentDates(String month);

    StaffAppointmentLookupResponse lookupAppointment(String code);

    StaffAppointmentDetailResponse getAppointmentDetail(Long id);

    StaffAvailabilityResponse getAvailability(LocalDate date);

    Map<String, Object> createCounterAppointment(User staff, StaffCreateAppointmentRequest request);

    StaffAppointmentLookupResponse rescheduleAppointment(User staff, Long id, RescheduleAppointmentRequest request);

    StaffAppointmentLookupResponse cancelAppointment(User staff, Long id, CancelAppointmentStaffRequest request);

    StaffAppointmentLookupResponse checkin(User staff, Long id);

    StaffAppointmentLookupResponse undoCheckin(User staff, Long id);

    StaffAppointmentLookupResponse enterRoom(User staff, Long id);

    QueueStateDto getQueueState();

    QueueStateDto inviteNext();

    QueueStateDto undoEnter(Long id);

    QueueStateDto setAutoInvite(boolean enabled);
}

