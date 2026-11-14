package com.chuctupk.dto.response;

import java.util.List;

public record StaffAppointmentDatesResponse(
        String month,
        List<String> dates
) {}

