package com.chuctupk.controller;

import com.chuctupk.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
@Tag(name = "Media Storage API", description = "Tải và phục vụ ảnh media công khai với bộ nhớ đệm immutable")
public class MediaController {

    private final AdminService adminService;

    @GetMapping("/{id}")
    @Operation(summary = "Xem ảnh media công khai")
    public ResponseEntity<byte[]> getMedia(@PathVariable String id) {
        byte[] data = adminService.getUpload(id);
        String contentType = adminService.getUploadContentType(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    @PostMapping(consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE, "image/*", MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(summary = "Tải ảnh media lên máy chủ (≤ 3MB, JPG/PNG/WEBP/GIF)")
    public ResponseEntity<Map<String, Object>> uploadImage(
            HttpServletRequest request,
            @RequestParam(value = "file", required = false) MultipartFile multipartFile,
            @RequestBody(required = false) byte[] rawBytes) throws IOException {

        byte[] data;
        String contentType;

        if (multipartFile != null && !multipartFile.isEmpty()) {
            data = multipartFile.getBytes();
            contentType = multipartFile.getContentType();
        } else if (rawBytes != null && rawBytes.length > 0) {
            data = rawBytes;
            contentType = request.getContentType();
        } else {
            data = request.getInputStream().readAllBytes();
            contentType = request.getContentType();
        }

        Map<String, Object> result = adminService.saveUpload(contentType, data);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
