package com.chuctupk.dto.response;

import java.util.List;

public record StaffPatientListResponse(
        List<PatientDto> items,
        long total,
        int pages,
        int page,
        int limit
) {}

