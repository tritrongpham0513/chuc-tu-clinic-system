package com.chuctupk.service.impl;

import com.chuctupk.dto.request.PatientProfileRequest;
import com.chuctupk.dto.response.PatientProfileResponse;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.User;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.PatientProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientProfileServiceImpl implements PatientProfileService {

    private static final int MAX_PROFILES = 10;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));
    }

    @Override
    public List<PatientProfileResponse> getMyProfiles() {
        User user = getCurrentUser();
        List<Patient> profiles = patientRepository.findByUserIdAndIsActiveTrue(user.getId());
        return profiles.stream().map(this::toResponse).toList();
    }

    @Override
    public PatientProfileResponse getProfileById(Long id) {
        User user = getCurrentUser();
        Patient patient = patientRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hồ sơ"));
        return toResponse(patient);
    }

    @Override
    @Transactional
    public PatientProfileResponse addProfile(PatientProfileRequest request) {
        User user = getCurrentUser();

        long count = patientRepository.countByUserIdAndIsActiveTrue(user.getId());
        if (count >= MAX_PROFILES) {
            throw AppException.unprocessable("Tài khoản đã có tối đa " + MAX_PROFILES + " hồ sơ");
        }

        // Normalize phone
        String phone = request.getPhone().trim();

        // Determine patientCode - reuse existing patient if same phone+name+year
        Patient patient = patientRepository
                .findByPhoneAndFullNameAndYearOfBirth(phone, request.getFullName().trim(), request.getYearOfBirth())
                .map(existing -> {
                    // Link existing patient to this user
                    existing.setUser(user);
                    existing.setIsActive(true);
                    existing.setRelationship(request.getRelationship());
                    if (request.getAddress() != null) existing.setAddress(request.getAddress());
                    if (request.getMedicalHistory() != null) existing.setMedicalHistory(request.getMedicalHistory());
                    return existing;
                })
                .orElseGet(() -> {
                    // Create new patient
                    String code = generatePatientCode();
                    return Patient.builder()
                            .user(user)
                            .patientCode(code)
                            .fullName(request.getFullName().trim())
                            .phone(phone)
                            .yearOfBirth(request.getYearOfBirth())
                            .gender(request.getGender())
                            .address(request.getAddress())
                            .relationship(request.getRelationship())
                            .medicalHistory(request.getMedicalHistory())
                            .isPrimary(false)
                            .isActive(true)
                            .build();
                });

        patient = patientRepository.save(patient);
        return toResponse(patient);
    }

    @Override
    @Transactional
    public PatientProfileResponse updateProfile(Long id, PatientProfileRequest request) {
        User user = getCurrentUser();
        Patient patient = patientRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hồ sơ"));

        patient.setFullName(request.getFullName().trim());
        patient.setPhone(request.getPhone().trim());
        patient.setYearOfBirth(request.getYearOfBirth());
        patient.setGender(request.getGender());
        patient.setAddress(request.getAddress());
        patient.setRelationship(request.getRelationship());
        patient.setMedicalHistory(request.getMedicalHistory());

        patient = patientRepository.save(patient);
        return toResponse(patient);
    }

    @Override
    @Transactional
    public void deleteOrUnlinkProfile(Long id) {
        User user = getCurrentUser();
        Patient patient = patientRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy hồ sơ"));

        if (Boolean.TRUE.equals(patient.getIsPrimary())) {
            throw AppException.unprocessable("Không thể xóa hồ sơ chính chủ");
        }

        // Kiểm tra có lịch sử khám không
        boolean hasHistory = appointmentRepository.existsByPatientId(patient.getId());
        if (hasHistory) {
            // Chỉ unlink — ngắt liên kết tài khoản, giữ lại data
            patient.setUser(null);
            patient.setIsActive(false);
        } else {
            // Xóa mềm
            patient.setIsActive(false);
            patient.setUser(null);
        }
        patientRepository.save(patient);
    }

    private String generatePatientCode() {
        int year = java.time.LocalDate.now().getYear();
        String prefix = "BN-" + year + "-";
        long count = patientRepository.countByPatientCodeStartingWith(prefix);
        for (int i = 0; i < 10; i++) {
            String code = String.format("%s%04d", prefix, count + 1 + i);
            if (patientRepository.findByPatientCode(code).isEmpty()) {
                return code;
            }
        }
        throw AppException.internal("Không thể sinh mã bệnh nhân, vui lòng thử lại");
    }

    private PatientProfileResponse toResponse(Patient p) {
        return PatientProfileResponse.builder()
                .id(p.getId())
                .patientCode(p.getPatientCode())
                .fullName(p.getFullName())
                .phone(p.getPhone())
                .yearOfBirth(p.getYearOfBirth())
                .gender(p.getGender())
                .address(p.getAddress())
                .relationship(p.getRelationship())
                .medicalHistory(p.getMedicalHistory())
                .isPrimary(p.getIsPrimary())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
