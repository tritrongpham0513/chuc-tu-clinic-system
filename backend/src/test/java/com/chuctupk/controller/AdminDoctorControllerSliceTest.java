package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateDoctorRequest;
import com.chuctupk.dto.request.UpdateDoctorRequest;
import com.chuctupk.dto.response.AdminDoctorDetailResponse;
import com.chuctupk.dto.response.DoctorResponse;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.AdminDoctorService;
import com.chuctupk.service.DoctorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 4 — Admin Doctor Controller Slice Tests (Ticket 5)")
class AdminDoctorControllerSliceTest {

    private MockMvc mockMvc;
    private MockMvc publicMockMvc;

    @Mock
    private AdminDoctorService adminDoctorService;

    @Mock
    private DoctorService publicDoctorService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User adminUser;
    private User staffUser;

    @BeforeEach
    void setUp() {
        AdminDoctorController adminDoctorController = new AdminDoctorController(adminDoctorService);
        DoctorController doctorController = new DoctorController(publicDoctorService);

        mockMvc = MockMvcBuilders.standaloneSetup(adminDoctorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        publicMockMvc = MockMvcBuilders.standaloneSetup(doctorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        staffUser = User.builder()
                .id(2L)
                .username("letan")
                .fullName("Lễ tân phòng khám")
                .role(Role.STAFF)
                .isActive(true)
                .build();
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Test 1: ADMIN tạo bác sĩ không gán tài khoản -> 201 Created")
    void test1_AdminCreateDoctor_WithoutAccount_Returns201() throws Exception {
        authenticateAs(adminUser);

        CreateDoctorRequest request = CreateDoctorRequest.builder()
                .fullName("Lê Văn Bình")
                .title("Bác sĩ")
                .specialty("Y học cổ truyền")
                .experienceYears(10)
                .bio("Chuyên bắt mạch bốc thuốc")
                .phone("0909123456")
                .avatarUrl(null)
                .sortOrder(3)
                .accountId(null)
                .build();

        AdminDoctorDetailResponse response = AdminDoctorDetailResponse.builder()
                .id(3L)
                .fullName("Lê Văn Bình")
                .title("Bác sĩ")
                .specialty("Y học cổ truyền")
                .experienceYears(10)
                .bio("Chuyên bắt mạch bốc thuốc")
                .phone("0909123456")
                .sortOrder(3)
                .isActive(true)
                .accountId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(adminDoctorService.createDoctor(any(CreateDoctorRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.fullName").value("Lê Văn Bình"))
                .andExpect(jsonPath("$.data.accountId").doesNotExist());
    }

    @Test
    @DisplayName("Test 2: Tạo với accountId có vai trò USER/PATIENT -> 422 Unprocessable")
    void test2_CreateDoctor_RoleUser_Returns422() throws Exception {
        authenticateAs(adminUser);

        CreateDoctorRequest request = CreateDoctorRequest.builder()
                .fullName("Nguyễn Văn B")
                .specialty("Y học cổ truyền")
                .phone("0909123456")
                .accountId(99L)
                .build();

        when(adminDoctorService.createDoctor(any(CreateDoctorRequest.class)))
                .thenThrow(AppException.unprocessable("Tài khoản được gán phải có vai trò Bác sĩ (DOCTOR)"));

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("vai trò Bác sĩ (DOCTOR)")));
    }

    @Test
    @DisplayName("Test 3: Tạo với accountId đã gán cho bác sĩ khác -> 422 Unprocessable")
    void test3_CreateDoctor_AlreadyAssignedAccount_Returns422() throws Exception {
        authenticateAs(adminUser);

        CreateDoctorRequest request = CreateDoctorRequest.builder()
                .fullName("Nguyễn Văn C")
                .specialty("Y học cổ truyền")
                .phone("0909123456")
                .accountId(12L)
                .build();

        when(adminDoctorService.createDoctor(any(CreateDoctorRequest.class)))
                .thenThrow(AppException.unprocessable("Tài khoản này đã được gán cho một bác sĩ khác"));

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tài khoản này đã được gán cho một bác sĩ khác"));
    }

    @Test
    @DisplayName("Test 4: PATCH ẩn bác sĩ còn 3 phiếu tương lai -> 422, message chứa '3'")
    void test4_PatchHideDoctor_With3FutureAppointments_Returns422With3() throws Exception {
        authenticateAs(adminUser);

        UpdateDoctorRequest request = UpdateDoctorRequest.builder()
                .isActive(false)
                .build();

        when(adminDoctorService.updateDoctor(eq(5L), any(UpdateDoctorRequest.class)))
                .thenThrow(AppException.unprocessable("Bác sĩ còn 3 phiếu khám sắp tới, không thể ẩn"));

        mockMvc.perform(patch("/api/v1/admin/doctors/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("3")));
    }

    @Test
    @DisplayName("Test 5: PATCH với updatedAt cũ -> lỗi STALE (409 Conflict)")
    void test5_PatchDoctor_StaleUpdatedAt_ReturnsStaleError() throws Exception {
        authenticateAs(adminUser);

        UpdateDoctorRequest request = UpdateDoctorRequest.builder()
                .fullName("Tên Mới")
                .updatedAt(LocalDateTime.of(2026, 11, 19, 10, 0))
                .build();

        when(adminDoctorService.updateDoctor(eq(5L), any(UpdateDoctorRequest.class)))
                .thenThrow(AppException.stale("Dữ liệu đã được người khác cập nhật trước đó. Vui lòng tải lại trang"));

        mockMvc.perform(patch("/api/v1/admin/doctors/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.STALE.getCode()));
    }

    @Test
    @DisplayName("Test 6: STAFF gọi POST /api/v1/admin/doctors -> 403 Forbidden")
    void test6_StaffCallCreateDoctor_Returns403() throws Exception {
        authenticateAs(staffUser);

        CreateDoctorRequest request = CreateDoctorRequest.builder()
                .fullName("Bác sĩ Mới")
                .specialty("Y học cổ truyền")
                .phone("0909123456")
                .build();

        mockMvc.perform(post("/api/v1/admin/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));

        verify(adminDoctorService, never()).createDoctor(any());
    }

    @Test
    @DisplayName("Test 7: GET /api/v1/doctors công khai không trả bác sĩ isActive=false")
    void test7_PublicGetDoctors_DoesNotReturnInactiveDoctor() throws Exception {
        DoctorResponse activeDoc = DoctorResponse.builder()
                .id(1L)
                .fullName("Bác sĩ Đang Hoạt Động")
                .specialty("Y học cổ truyền")
                .build();

        when(publicDoctorService.getActiveDoctors()).thenReturn(List.of(activeDoc));

        publicMockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fullName").value("Bác sĩ Đang Hoạt Động"));

        verify(publicDoctorService).getActiveDoctors();
    }
}
