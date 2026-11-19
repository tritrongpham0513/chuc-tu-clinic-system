package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UploadIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    /** Tài khoản seed phải đổi mật khẩu lần đầu; test này cần quyền làm việc bình thường. */
    @BeforeEach
    void clearSeedPasswordFlag() {
        userRepository.findByUsername("admin").ifPresent(u -> {
            u.setMustChangePassword(false);
            userRepository.save(u);
        });
    }

    @Test
    @DisplayName("B6: Anonymous gọi POST /api/v1/uploads -> 401 Unauthorized")
    void testUpload_AnonymousForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/uploads").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("B6: Upload file PNG thật -> 201 Created, trả {id, url}")
    void testUpload_RealPngSuccess() throws Exception {
        com.chuctupk.entity.User staffUser = com.chuctupk.entity.User.builder()
                .id(2L).username("letan").role(com.chuctupk.entity.enums.Role.STAFF).build();
        String staffToken = tokenProvider.generateToken(staffUser);

        // Header chuẩn PNG: 89 50 4E 47 0D 0A 1A 0A
        byte[] validPng = new byte[]{
                (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D,
                'I', 'H', 'D', 'R',
                0, 0, 0, 1,
                0, 0, 0, 1,
                8, 6, 0, 0, 0
        };

        MockMultipartFile file = new MockMultipartFile("file", "valid.png", "image/png", validPng);

        mockMvc.perform(multipart("/api/v1/uploads")
                        .file(file)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.url", containsString("/api/v1/uploads/")));
    }

    @Test
    @DisplayName("B6: Upload file text giả mạo PNG -> 400 Bad Request ('Định dạng ảnh không hợp lệ')")
    void testUpload_FakePngRejected() throws Exception {
        com.chuctupk.entity.User staffUser = com.chuctupk.entity.User.builder()
                .id(2L).username("letan").role(com.chuctupk.entity.enums.Role.STAFF).build();
        String staffToken = tokenProvider.generateToken(staffUser);

        byte[] fakeContent = "<?php echo 'hacker'; ?>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fakeFile = new MockMultipartFile("file", "shell.png", "image/png", fakeContent);

        mockMvc.perform(multipart("/api/v1/uploads")
                        .file(fakeFile)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Định dạng ảnh không hợp lệ")));
    }

    @Test
    @DisplayName("B6: POST /api/v1/admin/uploads endpoint trùng đã bị xoá hoàn toàn -> 404")
    void testDuplicateAdminUploads_NotFound() throws Exception {
        com.chuctupk.entity.User adminUser = com.chuctupk.entity.User.builder()
                .id(1L).username("admin").role(com.chuctupk.entity.enums.Role.ADMIN).build();
        String adminToken = tokenProvider.generateToken(adminUser);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
