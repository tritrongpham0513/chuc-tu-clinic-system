package com.chuctupk.service;

import com.chuctupk.dto.request.StaffCreatePatientRequest;
import com.chuctupk.dto.request.StaffUpdatePatientRequest;
import com.chuctupk.dto.response.StaffPatientDetailResponse;
import com.chuctupk.dto.response.StaffPatientListResponse;

import java.util.Map;

public interface StaffPatientService {

    StaffPatientListResponse getPatients(String q, Boolean locked, int page, int limit);

    Map<String, Object> getDuplicatePatients(String phone, Long excludeId);

    Map<String, Object> createPatient(StaffCreatePatientRequest request);

    StaffPatientDetailResponse getPatientDetail(Long id);

    Map<String, Object> updatePatient(Long id, StaffUpdatePatientRequest request);
}

