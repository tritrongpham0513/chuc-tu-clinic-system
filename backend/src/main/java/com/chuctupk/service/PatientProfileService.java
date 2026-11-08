package com.chuctupk.service;

import com.chuctupk.dto.request.PatientProfileRequest;
import com.chuctupk.dto.response.PatientProfileResponse;

import java.util.List;

public interface PatientProfileService {
    List<PatientProfileResponse> getMyProfiles();
    PatientProfileResponse getProfileById(Long id);
    PatientProfileResponse addProfile(PatientProfileRequest request);
    PatientProfileResponse updateProfile(Long id, PatientProfileRequest request);
    void deleteOrUnlinkProfile(Long id);
}
