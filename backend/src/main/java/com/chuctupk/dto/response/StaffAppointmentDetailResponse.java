package com.chuctupk.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record StaffAppointmentDetailResponse(
        AppointmentDto appointment,
        Object record
) {
    public StaffAppointmentDetailResponse(AppointmentDto appointment, MedicalRecordPatientDto record) {
        this(appointment, (Object) record);
    }
}
