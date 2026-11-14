package com.chuctupk.dto.response;

import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.entity.Patient;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatientDto(
        String id,
        String patientCode,
        String fullName,
        String phone,
        String dob,
        Integer birthYear,
        String gender,
        String relationship,
        String address,
        String medicalHistory,
        boolean isPrimary,
        boolean isLocked,
        boolean active,
        String createdAt,
        String updatedAt,
        String infoUpdatedAt,
        String lastVisitDate
) {
    public PatientDto(
            String id,
            String patientCode,
            String fullName,
            String phone,
            String dob,
            Integer birthYear,
            String gender,
            String relationship,
            String address,
            String medicalHistory,
            boolean isPrimary,
            boolean isLocked,
            boolean active,
            String createdAt,
            String updatedAt,
            String infoUpdatedAt
    ) {
        this(id, patientCode, fullName, phone, dob, birthYear, gender, relationship, address,
                medicalHistory, isPrimary, isLocked, active, createdAt, updatedAt, infoUpdatedAt, null);
    }

    public static PatientDto from(Patient p) {
        return from(p, (LocalDate) null);
    }

    public static PatientDto from(Patient p, LocalDate lastVisitDate) {
        if (p == null) return null;
        boolean locked = Boolean.FALSE.equals(p.getIsActive()) || p.getLockedAt() != null;
        return new PatientDto(
                String.valueOf(p.getId()),
                p.getPatientCode() != null ? p.getPatientCode() : "",
                p.getFullName() != null ? p.getFullName() : "",
                p.getPhone() != null ? p.getPhone() : "",
                ContractMapper.formatDate(p.getDob()),
                p.getYearOfBirth() != null ? p.getYearOfBirth() : 1970,
                ContractMapper.toFeGender(p.getGender()),
                p.getRelationship() != null && !p.getRelationship().isBlank() ? p.getRelationship() : "BAN_THAN",
                p.getAddress() != null ? p.getAddress() : "",
                p.getMedicalHistory() != null ? p.getMedicalHistory() : "",
                Boolean.TRUE.equals(p.getIsPrimary()),
                locked,
                !locked,
                ContractMapper.formatIso(p.getCreatedAt()),
                ContractMapper.formatIso(p.getUpdatedAt()),
                ContractMapper.formatIso(p.getInfoUpdatedAt()),
                lastVisitDate != null ? lastVisitDate.toString() : null
        );
    }
}

