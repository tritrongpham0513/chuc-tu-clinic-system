package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CreateRecordRequest(
        @NotBlank(message = "Vui lòng nhập chẩn đoán")
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

