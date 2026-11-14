package com.chuctupk.controller.compat;

import com.chuctupk.dto.request.PatientProfileRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.PatientProfileResponse;
import com.chuctupk.service.PatientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Deprecated
@RestController
@RequestMapping("/api/v1/patient-profiles")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<PatientProfileResponse>>> getMyProfiles() {
        List<PatientProfileResponse> profiles = patientProfileService.getMyProfiles();
        return ResponseEntity.ok(ApiResponse.success(profiles, "Lấy danh sách hồ sơ thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> getProfileById(@PathVariable Long id) {
        PatientProfileResponse profile = patientProfileService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success(profile, "Lấy thông tin hồ sơ thành công"));
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> addProfile(
            @Valid @RequestBody PatientProfileRequest request) {
        PatientProfileResponse profile = patientProfileService.addProfile(request);
        URI location = URI.create("/api/v1/patient-profiles/" + profile.getId());
        return ResponseEntity.created(location)
                .body(ApiResponse.<PatientProfileResponse>builder()
                        .success(true)
                        .statusCode(201)
                        .message("Thêm hồ sơ thành công")
                        .data(profile)
                        .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PatientProfileResponse>> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody PatientProfileRequest request) {
        PatientProfileResponse profile = patientProfileService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success(profile, "Cập nhật hồ sơ thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(@PathVariable Long id) {
        patientProfileService.deleteOrUnlinkProfile(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa hồ sơ thành công"));
    }
}

