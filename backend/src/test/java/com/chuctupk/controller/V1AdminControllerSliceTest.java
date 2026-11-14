package com.chuctupk.controller;

import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.AdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class V1AdminControllerSliceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User adminUser;

    static class StubAdminService implements AdminService {

        private final AccountDto sampleAccount = new AccountDto(
                "2", "letan_mai", "Mai Thị Lan", "0912345678", "letan@chuctu.vn",
                "RECEPTIONIST", true, false, "2026-11-01T08:00:00", "2026-11-01T08:00:00", null
        );

        private final PostDto samplePost = new PostDto(
                "10", "bai-thuoc-dan-gian", "Bài thuốc dân gian trị đau khớp",
                "Tổng hợp các bài thuốc thảo mộc...", "Tổng hợp các bài thuốc thảo mộc...",
                "<p>Nội dung chi tiết...</p>",
                "/uploads/khop.jpg", "/uploads/khop.jpg",
                "khoa-hoc", "Y học cổ truyền", "Y học cổ truyền",
                "Lương y Nguyễn Văn Tuệ", true, "PUBLISHED",
                "2026-11-10T08:00:00", "2026-11-10T08:00:00", "2026-11-10T08:00:00"
        );

        private final PatientDto samplePatient = new PatientDto(
                "10", "CT-BN-26-0010", "Bác Lê Văn Tám", "0987654321",
                "1955-05-15", 1955, "NAM", "BAN_THAN", "Quận 3, TP.HCM",
                "", false, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00",
                "2026-11-10"
        );

        @Override
        public AdminDashboardResponse getDashboard() {
            return new AdminDashboardResponse(
                    "2026-11-14",
                    new AdminDashboardResponse.AdminDashboardAppointments(12, 10, Map.of("CHO_KHAM", 4, "DANG_KHAM", 2, "HOAN_THANH", 4, "DA_HUY", 2)),
                    new AdminDashboardResponse.AdminDashboardPatients(45),
                    new AdminDashboardResponse.AdminDashboardPosts(8, 2, 1),
                    Map.of("USER", 35, "RECEPTIONIST", 3, "ADMIN", 2),
                    List.of(samplePost),
                    null
            );
        }

        @Override
        public AdminAccountListResponse getAccounts(String q, String role, int page, int limit) {
            return new AdminAccountListResponse(List.of(sampleAccount), 1, 1, 1, 10, Map.of("USER", 35, "RECEPTIONIST", 3, "ADMIN", 2));
        }

        @Override
        public AccountDto getAccount(Long id) {
            return sampleAccount;
        }

        @Override
        public AccountDto createAccount(AdminCreateAccountRequest request) {
            return sampleAccount;
        }

        @Override
        public AccountDto updateAccount(Long id, AdminUpdateAccountRequest request) {
            return sampleAccount;
        }

        @Override
        public AccountDto changeRole(Long id, AdminChangeRoleRequest request) {
            return new AccountDto(
                    "2", "letan_mai", "Mai Thị Lan", "0912345678", "letan@chuctu.vn",
                    request.role(), true, false, "2026-11-01T08:00:00", "2026-11-01T08:00:00", null
            );
        }

        @Override
        public AccountDto setAccountActive(User adminUser, Long id, boolean active) {
            if (adminUser != null && adminUser.getId().equals(id) && !active) {
                throw new AppException(ErrorCode.CANNOT_LOCK_SELF, "Không thể tự khóa tài khoản quản trị của chính mình");
            }
            return new AccountDto(
                    String.valueOf(id), "letan_mai", "Mai Thị Lan", "0912345678", "letan@chuctu.vn",
                    "RECEPTIONIST", active, false, "2026-11-01T08:00:00", "2026-11-01T08:00:00", null
            );
        }

        @Override
        public void resetPassword(Long id, AdminResetPasswordRequest request) {
            // no-op
        }

        @Override
        public PatientDto setPatientActive(Long patientId, boolean active) {
            return samplePatient;
        }

        @Override
        public AdminPostListResponse getAdminPosts(String q, String category, String status, int page, int limit) {
            return new AdminPostListResponse(List.of(samplePost), 1, 1, 1, 10, Map.of("PUBLISHED", 1, "DRAFT", 0));
        }

        @Override
        public PostDto getAdminPost(Long id) {
            return samplePost;
        }

        @Override
        public PostDto createAdminPost(User author, AdminSavePostRequest request) {
            return samplePost;
        }

        @Override
        public PostDto updateAdminPost(Long id, AdminSavePostRequest request) {
            return samplePost;
        }

        @Override
        public PostDto setPostPin(Long id, boolean pin) {
            return samplePost;
        }

        @Override
        public void deletePost(Long id) {
            // no-op
        }

        @Override
        public PublicPostListResponse getPublicPosts(String category, int page, int limit, boolean includeFeatured) {
            return new PublicPostListResponse(List.of(samplePost), samplePost, 1, 1, 1, 10);
        }

        @Override
        public PostDto getPublicPost(String slugOrId) {
            return samplePost;
        }

        @Override
        public Map<String, Object> saveUpload(String contentType, byte[] data) {
            return Map.of("id", "upl-12345", "url", "/uploads/upl-12345.jpg");
        }

        @Override
        public byte[] getUpload(String id) {
            return new byte[]{1, 2, 3, 4};
        }

        @Override
        public String getUploadContentType(String id) {
            return "image/jpeg";
        }
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setFullName("Quản trị viên Chúc Từ");
        adminUser.setRole(Role.ADMIN);
        adminUser.setIsActive(true);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(adminUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AdminService adminService = new StubAdminService();
        AdminController adminController = new AdminController(adminService);
        PublicPostController publicPostController = new PublicPostController(adminService);
        MediaController mediaController = new MediaController(adminService);

        mockMvc = MockMvcBuilders.standaloneSetup(adminController, publicPostController, mediaController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/admin/dashboard - Trả về đủ 7 phần số liệu")
    void testGetDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.date", is("2026-11-14")))
                .andExpect(jsonPath("$.data.appointments.total", is(12)))
                .andExpect(jsonPath("$.data.patients.total", is(45)))
                .andExpect(jsonPath("$.data.posts.published", is(8)))
                .andExpect(jsonPath("$.data.accounts.ADMIN", is(2)))
                .andExpect(jsonPath("$.data.drafts", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/admin/accounts - Danh sách tài khoản")
    void testGetAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/accounts")
                        .param("q", "mai")
                        .param("role", "RECEPTIONIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].username", is("letan_mai")));
    }

    @Test
    @DisplayName("POST /api/v1/admin/accounts - Tạo tài khoản mới")
    void testCreateAccount() throws Exception {
        AdminCreateAccountRequest request = new AdminCreateAccountRequest(
                "Bác sĩ Nam", "0901234567", "bsnam@chuctu.vn", "RECEPTIONIST", "ChucTu@2026"
        );

        mockMvc.perform(post("/api/v1/admin/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("letan_mai")));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/accounts/{id}/role - Thay đổi quyền tài khoản")
    void testChangeRole() throws Exception {
        AdminChangeRoleRequest request = new AdminChangeRoleRequest("ADMIN", null, null);

        mockMvc.perform(patch("/api/v1/admin/accounts/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.account.role", is("ADMIN")));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/accounts/{id}/toggle-status - Chặn tự khóa chính mình (CANNOT_LOCK_SELF)")
    void testCannotLockSelf() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/accounts/1/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("CANNOT_LOCK_SELF")));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/accounts/{id}/toggle-status - Khóa tài khoản khác thành công")
    void testLockOtherAccount() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/accounts/2/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isActive", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/admin/accounts/{id}/reset-password - Đặt lại mật khẩu")
    void testResetPassword() throws Exception {
        AdminResetPasswordRequest request = new AdminResetPasswordRequest("MatKhauMoi@2026");

        mockMvc.perform(post("/api/v1/admin/accounts/2/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/patients/{id}/toggle-status - Khóa / Mở khóa bệnh nhân")
    void testTogglePatientStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/patients/10/toggle-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Bác Lê Văn Tám")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/posts - Quản lý bài viết")
    void testGetAdminPosts() throws Exception {
        mockMvc.perform(get("/api/v1/admin/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/admin/posts - Tạo bài viết mới")
    void testCreateAdminPost() throws Exception {
        AdminSavePostRequest request = new AdminSavePostRequest(
                "Bài thuốc mới", "<p>Chi tiết bài thuốc...</p>", "Tóm tắt bài thuốc", "Trích dẫn",
                "khoa-hoc", "PUBLISHED", "/uploads/img.jpg", true
        );

        mockMvc.perform(post("/api/v1/admin/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.post.title", is("Bài thuốc dân gian trị đau khớp")));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/posts/{id}/pin - Ghim bài viết")
    void testSetPostPin() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/posts/10/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.post.pinned", is(true)));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/posts/{id} - Xóa bài viết")
    void testDeletePost() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/posts - Danh sách bài viết công khai có featured tách riêng")
    void testGetPublicPosts() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.featured.title", is("Bài thuốc dân gian trị đau khớp")))
                .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{slugOrId} - Chi tiết bài viết công khai")
    void testGetPublicPostDetail() throws Exception {
        mockMvc.perform(get("/api/v1/posts/bai-thuoc-dan-gian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.post.slug", is("bai-thuoc-dan-gian")));
    }

    @Test
    @DisplayName("POST /api/v1/uploads - Tải ảnh lên thành công")
    void testUploadImageSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}
        );

        mockMvc.perform(multipart("/api/v1/uploads").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", is("/uploads/upl-12345.jpg")));
    }

    @Test
    @DisplayName("GET /api/v1/uploads/{id} - Lấy file ảnh binary")
    void testGetUploadBinary() throws Exception {
        mockMvc.perform(get("/api/v1/uploads/upl-12345.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));
    }
}
