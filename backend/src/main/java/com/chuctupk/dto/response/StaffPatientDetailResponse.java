package com.chuctupk.dto.response;

import java.util.List;

public record StaffPatientDetailResponse(
        PatientDto patient,
        List<AppointmentDto> appointments,
        List<MedicalRecordPatientDto> records
) {}

