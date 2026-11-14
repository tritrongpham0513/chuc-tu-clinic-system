package com.chuctupk.dto.response;

import java.util.List;

public record StaffAppointmentListResponse(
        String date,
        List<AppointmentDto> items,
        StaffAppointmentSummaryDto summary
) {}

