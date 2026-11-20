package com.chuctupk.dto.response;

import com.chuctupk.entity.MedicalRecord;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecordSummaryDto(
        String id,
        String visitDate,
        String doctorName,
        String followUpDate,
        Boolean hasPrescription
) {
    public static RecordSummaryDto from(MedicalRecord r) {
        if (r == null) return null;

        String doctorName = r.getDoctorName() != null && !r.getDoctorName().isBlank()
                ? r.getDoctorName()
                : (r.getDoctor() != null ? r.getDoctor().getFullName() : "Bác sĩ Chúc Từ");

        String vDate = r.getVisitDate() != null
                ? r.getVisitDate().toString()
                : (r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "");

        String followUp = r.getFollowUpDate() != null ? r.getFollowUpDate().toString() : null;
        boolean hasRx = r.getPrescription() != null && !r.getPrescription().isBlank();

        return new RecordSummaryDto(
                String.valueOf(r.getId()),
                vDate,
                doctorName,
                followUp,
                hasRx
        );
    }
}
