package com.chuctupk.dto.response;

import java.util.List;

public record StaffAvailabilityResponse(
        String date,
        List<SlotAvailabilityDto> slots
) {}

