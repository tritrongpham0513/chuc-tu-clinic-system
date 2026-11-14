package com.chuctupk.dto.response;

public record StaffAppointmentSummaryDto(
        int total,
        int checkedIn,
        int inProgress,
        int completed,
        int cancelled,
        int booked
) {}

