package com.chuctupk.dto.response;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.MedicalRecord;
import com.chuctupk.entity.MedicalRecordEdit;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MedicalRecordPatientDto(
        String id,
        String appointmentId,
        String patientId,
        String doctorName,
        String visitDate,
        String diagnosis,
        String pulse,
        String bloodPressure,
        String pulseBp,
        String treatment,
        Map<String, String> treatmentNotes,
        String prescription,
        String advice,
        String followUpDate,
        String createdAt,
        String editableUntil,
        Boolean editable,
        String medicalHistory,
        List<Map<String, Object>> editHistory
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public MedicalRecordPatientDto(
            String id, String appointmentId, String patientId, String doctorName, String visitDate,
            String diagnosis, String pulse, String bloodPressure, String pulseBp, String treatment,
            Map<String, String> treatmentNotes, String prescription, String advice, String followUpDate,
            String createdAt
    ) {
        this(id, appointmentId, patientId, doctorName, visitDate, diagnosis, pulse, bloodPressure, pulseBp,
                treatment, treatmentNotes, prescription, advice, followUpDate, createdAt, null, null, null, null);
    }

    public static MedicalRecordPatientDto from(MedicalRecord r) {
        return from(r, false, null, null);
    }

    public static MedicalRecordPatientDto from(MedicalRecord r, boolean forStaff, List<MedicalRecordEdit> edits, Clock clock) {
        if (r == null) return null;

        String pulse = r.getPulse() != null ? r.getPulse() : "";
        String bp = r.getBloodPressure() != null ? r.getBloodPressure() : "";
        String pulseBpStr = r.getPulseBp() != null ? r.getPulseBp() : "";
        if (pulse.isBlank() && bp.isBlank() && !pulseBpStr.isBlank()) {
            if (pulseBpStr.contains("|")) {
                String[] parts = pulseBpStr.split("\\|", 2);
                pulse = parts[0];
                bp = parts.length > 1 ? parts[1] : "";
            } else if (pulseBpStr.contains("/")) {
                bp = pulseBpStr;
            } else {
                pulse = pulseBpStr;
            }
        }

        Map<String, String> treatmentNotes = new LinkedHashMap<>();
        String treatmentRaw = r.getTreatment() != null ? r.getTreatment() : "";
        if (treatmentRaw.startsWith("{") && treatmentRaw.contains("\"")) {
            try {
                Map<String, String> parsed = OBJECT_MAPPER.readValue(treatmentRaw, new TypeReference<Map<String, String>>() {});
                treatmentNotes.putAll(parsed);
            } catch (Exception ignored) {
                treatmentNotes.put("acupuncture", treatmentRaw);
                treatmentNotes.put("acupressure", "");
                treatmentNotes.put("other", "");
            }
        } else {
            treatmentNotes.put("acupuncture", r.getTreatmentAcupuncture() != null ? r.getTreatmentAcupuncture() : treatmentRaw);
            treatmentNotes.put("acupressure", r.getTreatmentAcupressure() != null ? r.getTreatmentAcupressure() : "");
            treatmentNotes.put("other", r.getTreatmentOther() != null ? r.getTreatmentOther() : "");
        }

        String doctorName = r.getDoctorName() != null && !r.getDoctorName().isBlank()
                ? r.getDoctorName()
                : (r.getDoctor() != null ? r.getDoctor().getFullName() : "Bác sĩ Chúc Từ");

        String vDate = r.getVisitDate() != null
                ? r.getVisitDate().toString()
                : (r.getCreatedAt() != null ? r.getCreatedAt().toLocalDate().toString() : "");

        String editableUntil = null;
        Boolean editable = null;
        String medicalHistory = null;
        List<Map<String, Object>> editHistoryList = null;

        if (forStaff) {
            LocalDateTime created = r.getCreatedAt() != null ? r.getCreatedAt() : LocalDateTime.now();
            LocalDateTime until = created.plusHours(24);
            editableUntil = ContractMapper.formatIso(until);

            LocalDateTime now = clock != null ? LocalDateTime.now(clock) : LocalDateTime.now();
            boolean patientLocked = r.getPatient() != null && (Boolean.FALSE.equals(r.getPatient().getIsActive()) || r.getPatient().getLockedAt() != null);
            editable = !patientLocked && now.isBefore(until);

            medicalHistory = r.getPatient() != null ? r.getPatient().getMedicalHistory() : null;

            if (edits != null && !edits.isEmpty()) {
                editHistoryList = new ArrayList<>();
                for (MedicalRecordEdit edit : edits) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", String.valueOf(edit.getId()));
                    item.put("editedAt", ContractMapper.formatIso(edit.getEditedAt()));
                    item.put("editedByName", edit.getEditedBy() != null ? edit.getEditedBy().getFullName() : "Nhân viên");
                    try {
                        item.put("changes", OBJECT_MAPPER.readValue(edit.getChanges(), Object.class));
                    } catch (Exception e) {
                        item.put("changes", edit.getChanges());
                    }
                    editHistoryList.add(item);
                }
            }
        }

        return new MedicalRecordPatientDto(
                String.valueOf(r.getId()),
                r.getAppointment() != null ? String.valueOf(r.getAppointment().getId()) : null,
                r.getPatient() != null ? String.valueOf(r.getPatient().getId()) : null,
                doctorName,
                vDate,
                r.getDiagnosis() != null ? r.getDiagnosis() : "",
                pulse,
                bp,
                pulseBpStr,
                treatmentRaw,
                treatmentNotes,
                r.getPrescription() != null ? r.getPrescription() : "",
                r.getAdvice() != null ? r.getAdvice() : "",
                r.getFollowUpDate() != null ? r.getFollowUpDate().toString() : null,
                ContractMapper.formatIso(r.getCreatedAt()),
                editableUntil,
                editable,
                medicalHistory,
                editHistoryList
        );
    }
}
