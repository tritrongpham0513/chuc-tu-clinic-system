package com.chuctupk.service.impl;

import com.chuctupk.dto.response.DoctorResponse;
import com.chuctupk.entity.Doctor;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getActiveDoctors() {
        return doctorRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .fullName(doctor.getFullName())
                .title(doctor.getTitle())
                .specialty(doctor.getSpecialty())
                .experienceYears(doctor.getExperienceYears())
                .bio(doctor.getBio())
                .avatarUrl(doctor.getAvatarUrl())
                .build();
    }
}

