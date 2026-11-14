package com.chuctupk.controller.compat;

import com.chuctupk.dto.request.PatientProfileRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.PatientProfileResponse;
import com.chuctupk.entity.Patient;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.PatientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Deprecated
@RestController
@RequestMapping("/api/v1/admin/patients")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'DOCTOR')")
public class AdminPatientController {

    private final PatientRepository patientRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientProfileResponse>>> searchPatients(
            @RequestParam(required = false) String search
    ) {
        List<Patient> list;
        if (search != null && !search.isBlank()) {
            String q = search.trim();
            list = patientRepository.findAll().stream()
                    .filter(p -> (p.getPhone() != null && p.getPhone().contains(q)) ||
                                 (p.getFullName() != null && p.getFullName().toLowerCase().contains(q.toLowerCase())) ||
                                 (p.getPatientCode() != null && p.getPatientCode().toLowerCase().contains(q.toLowerCase())))
                    .toList();
        } else {
            list = patientRepository.findAll();
        }

        List<PatientProfileResponse> responses = list.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "Lấy danh sách bệnh nhân thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getPatient(@PathVariable Long id) {
        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bệnh nhân"));
        return ResponseEntity.ok(ApiResponse.success(toResponse(p), "Lấy thông tin bệnh nhân thành công"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<PatientProfileResponse>> createPatient(
            @Valid @RequestBody PatientProfileRequest req
    ) {
        int year = LocalDate.now().getYear();
        long count = patientRepository.countByPatientCodeStartingWith("BN-" + year + "-");
        String code = String.format("BN-%d-%04d", year, count + 1);

        Patient patient = Patient.builder()
                .patientCode(code)
                .fullName(req.getFullName().trim())
                .phone(req.getPhone().trim())
                .yearOfBirth(req.getYearOfBirth())
                .gender(req.getGender())
                .address(req.getAddress())
                .relationship(req.getRelationship())
                .medicalHistory(req.getMedicalHistory())
                .isPrimary(true)
                .isActive(true)
                .build();

        patient = patientRepository.save(patient);
        return ResponseEntity.ok(ApiResponse.success(toResponse(patient), "Tạo hồ sơ bệnh nhân thành công"));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<PatientProfileResponse>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientProfileRequest req
    ) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bệnh nhân"));

        patient.setFullName(req.getFullName().trim());
        patient.setPhone(req.getPhone().trim());
        patient.setYearOfBirth(req.getYearOfBirth());
        patient.setGender(req.getGender());
        patient.setAddress(req.getAddress());
        patient.setRelationship(req.getRelationship());
        patient.setMedicalHistory(req.getMedicalHistory());

        patient = patientRepository.save(patient);
        return ResponseEntity.ok(ApiResponse.success(toResponse(patient), "Cập nhật hồ sơ bệnh nhân thành công"));
    }

    /**
     * F.9.6 - Khóa / mở khóa hồ sơ bệnh nhân
     */
    @PatchMapping("/{id}/status")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> togglePatientStatus(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy bệnh nhân"));

        boolean currentStatus = Boolean.TRUE.equals(patient.getIsActive());
        patient.setIsActive(!currentStatus);
        patient = patientRepository.save(patient);

        String msg = patient.getIsActive() ? "Mở khóa hồ sơ bệnh nhân thành công" : "Khóa hồ sơ bệnh nhân thành công";
        return ResponseEntity.ok(ApiResponse.success(toResponse(patient), msg));
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


