package com.chuctupk.dto.request;

import java.util.Map;

public record UpdateRecordRequest(
        String diagnosis,
        String pulse,
        String bloodPressure,
        Map<String, String> treatmentNotes,
        String treatment,
        String prescription,
        String advice,
        String medicalHistory,
        String followUpDate,
        String doctorName
) {}

