package com.chuctupk.service;

import com.chuctupk.dto.request.BreakGlassRequest;
import com.chuctupk.dto.request.StaffCreatePatientRequest;
import com.chuctupk.dto.request.StaffUpdatePatientRequest;
import com.chuctupk.dto.response.StaffPatientDetailResponse;
import com.chuctupk.dto.response.StaffPatientListResponse;
import com.chuctupk.entity.User;

import java.util.Map;

public interface StaffPatientService {

    StaffPatientListResponse getPatients(String q, Boolean locked, int page, int limit);

    default StaffPatientListResponse getPatients(String q, Boolean locked, int page, int limit, User currentUser) {
        return getPatients(q, locked, page, limit);
    }

    Map<String, Object> getDuplicatePatients(String phone, Long excludeId);

    Map<String, Object> createPatient(StaffCreatePatientRequest request);

    StaffPatientDetailResponse getPatientDetail(Long id);

    default StaffPatientDetailResponse getPatientDetail(Long id, User currentUser) {
        return getPatientDetail(id);
    }

    default StaffPatientDetailResponse breakGlass(Long id, BreakGlassRequest request, User currentUser) {
        return getPatientDetail(id, currentUser);
    }

    Map<String, Object> updatePatient(Long id, StaffUpdatePatientRequest request);
}
