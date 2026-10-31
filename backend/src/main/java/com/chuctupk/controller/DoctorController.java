package com.chuctupk.controller;

import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.DoctorResponse;
import com.chuctupk.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getDoctors() {
        List<DoctorResponse> doctors = doctorService.getActiveDoctors();
        return ResponseEntity.ok(ApiResponse.success(doctors, "Lấy danh sách bác sĩ thành công"));
    }
}

