package com.chuctupk.controller;

import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.ClinicServiceResponse;
import com.chuctupk.service.ClinicServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ClinicServiceController {

    private final ClinicServiceService clinicServiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClinicServiceResponse>>> getServices() {
        List<ClinicServiceResponse> services = clinicServiceService.getActiveServices();
        return ResponseEntity.ok(ApiResponse.success(services, "Lấy danh sách dịch vụ thành công"));
    }
}

