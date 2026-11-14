package com.chuctupk.dto.request;

public record StaffUpdatePatientRequest(
        String fullName,
        String phone,
        Integer birthYear,
        String dob,
        String gender,
        String address,
        String medicalHistory,
        String expectedUpdatedAt
) {}

