package com.chuctupk.controller;

import com.chuctupk.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Tag(name = "Health", description = "Kiểm tra tình trạng hoạt động của hệ thống")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Operation(summary = "Kiểm tra sức khỏe hệ thống")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of(
                        "ok", true,
                        "timestamp", Instant.now().toString(),
                        "status", "UP"
                ),
                "Hệ thống Phòng Khám Chúc Từ đang hoạt động bình thường"
        ));
    }
}

