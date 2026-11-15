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

    public static PatientDto masked(Patient p) {
        if (p == null) return null;
        String maskedName = maskFullName(p.getFullName());
        return new PatientDto(
                "",
                "",
                maskedName,
                null,
                null,
                null,
                p.getGender() != null ? ContractMapper.toFeGender(p.getGender()) : null,
                null,
                null,
                null,
                false,
                false,
                true,
                null,
                null,
                null,
                null
        );
    }

    public static String maskFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String trimmed = fullName.trim();
        if (trimmed.matches("(?i)^bệnh nhân(\\s+\\d+)?$")) {
            return "Bệnh nhân";
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) return parts[0];
        if (parts.length == 2) {
            if (parts[1].matches("\\d+")) {
                return parts[0];
            }
            return parts[0] + " " + parts[1];
        }
        if (parts[parts.length - 1].matches("\\d+")) {
            if (parts.length == 3 && (parts[0] + " " + parts[1]).equalsIgnoreCase("bệnh nhân")) {
                return "Bệnh nhân";
            }
        }
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(" ").append(Character.toUpperCase(parts[i].charAt(0))).append(".");
            }
        }
        sb.append(" ").append(parts[parts.length - 1]);
        return sb.toString();
    }
}

