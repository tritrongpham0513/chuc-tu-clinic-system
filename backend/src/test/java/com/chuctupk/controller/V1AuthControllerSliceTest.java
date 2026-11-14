package com.chuctupk.controller;

import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.dto.response.UserDto;
import com.chuctupk.dto.response.UserResponse;
import com.chuctupk.entity.User;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class V1AuthControllerSliceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    static class StubAuthService implements AuthService {
        @Override
        public Map<String, Object> requestPatientLoginOtp(String phone) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("needCode", true);
            map.put("phone", phone);
            map.put("resendAfterSeconds", 60);
            map.put("devCode", "123456");
            return map;
        }

        @Override
        public Map<String, Object> verifyPatientLogin(LoginVerifyRequest request, HttpServletResponse response) {
            UserDto u = new UserDto("1", "patient_" + request.getPhone(), "Nguyễn Văn A", request.getPhone(), "", "USER", false, true, true, null);
            return Map.of("user", u, "token", "fake-jwt-token");
        }

        @Override
        public Map<String, Object> requestPatientRegisterOtp(PatientRegisterRequest request) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("needCode", true);
            map.put("phone", request.getPhone());
            map.put("resendAfterSeconds", 60);
            return map;
        }

        @Override
        public Map<String, Object> verifyPatientRegister(RegisterVerifyRequest request, HttpServletResponse response) {
            UserDto u = new UserDto("1", "patient_" + request.getPhone(), request.getFullName(), request.getPhone(), "", "USER", false, true, true, null);
            return Map.of("user", u, "token", "fake-jwt-token");
        }

        @Override
        public Map<String, Object> resendOtp(String phone, String purpose) {
            return Map.of("success", true, "resendAfterSeconds", 60);
        }

        @Override
        public Map<String, Object> loginStaff(StaffLoginRequest request, HttpServletResponse response) {
            UserDto u = new UserDto("2", request.getIdentifier(), "BS. Lê Thị Lan", "0988776655", "lan@chuctu.vn", "DOCTOR", false, true, true, null);
            return Map.of("user", u, "token", "staff-jwt-token");
        }

        @Override
        public Map<String, Object> staffForgotPassword(String identifier) {
            return Map.of("needCode", true, "identifier", identifier);
        }

        @Override
        public Map<String, Object> staffVerifyForgotPassword(StaffForgotPasswordVerifyRequest request) {
            return Map.of("success", true, "resetToken", "rst-token-123", "token", "rst-token-123");
        }

        @Override
        public Map<String, Object> staffResetPassword(StaffResetPasswordRequest request) {
            return Map.of("success", true, "message", "Đặt lại mật khẩu thành công");
        }

        @Override
        public UserDto changeUserPassword(User currentUser, ChangePasswordRequest request) {
            return new UserDto("2", "doctor", "BS. Lan", "0988776655", "lan@chuctu.vn", "DOCTOR", false, true, true, null);
        }

        @Override
        public Map<String, Object> refreshToken(HttpServletRequest request, HttpServletResponse response) {
            return Map.of("token", "new-jwt-token");
        }

        @Override
        public void logout(User currentUser, HttpServletRequest request, HttpServletResponse response) {}

        @Override
        public UserDto getCurrentUserDto(User currentUser) {
            if (currentUser == null) return null;
            return new UserDto("1", currentUser.getUsername(), currentUser.getFullName(), currentUser.getPhone(), "", "USER", false, true, true, null);
        }

        @Override public void sendOtp(SendOtpRequest request) {}
        @Override public boolean verifyOtp(VerifyOtpRequest request) { return true; }
        @Override public AuthResponse register(RegisterRequest request) { return null; }
        @Override public AuthResponse login(LoginRequest request) { return null; }
        @Override public AuthResponse loginWithOtp(LoginOtpRequest request) { return null; }
        @Override public AuthResponse quickLoginByPhone(String phone) { return null; }
        @Override public UserResponse getCurrentUser(User currentUser) { return null; }
        @Override public UserResponse updateMyProfile(User currentUser, UpdateUserProfileRequest request) { return null; }
        @Override public void changePassword(User currentUser, ChangePasswordRequest request) {}
        @Override public void forgotPassword(ForgotPasswordRequest request) {}
        @Override public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {}
        @Override public UserResponse updateStaffProfile(User currentUser, UpdateStaffProfileRequest request) { return null; }

        @Override
        public UserDto updateStaffAccountName(User currentUser, String fullName) {
            return new UserDto("2", "letan", fullName, "0901234567", "letan@chuctu.vn", "RECEPTIONIST", false, true, true, null);
        }

        @Override
        public Map<String, Object> requestStaffEmailChange(User currentUser, String newEmail) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("needCode", true);
            map.put("email", newEmail);
            map.put("resendAfterSeconds", 60);
            map.put("devCode", "123456");
            return map;
        }

        @Override
        public Map<String, Object> verifyStaffEmailChange(User currentUser, String code) {
            UserDto u = new UserDto("2", "letan", "Lễ tân Chúc Từ", "0901234567", "letan@chuctu.vn", "RECEPTIONIST", false, true, true, null);
            return Map.of("user", u);
        }
    }

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(new StubAuthService()))
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Chưa đăng nhập trả về 200 OK với user: null (không ném 401)")
    void testGetMe_Anonymous() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.user", nullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Yêu cầu mã OTP cho bệnh nhân")
    void testLogin_RequestOtp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", "0912345678"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.needCode", is(true)))
                .andExpect(jsonPath("$.data.phone", is("0912345678")))
                .andExpect(jsonPath("$.data.devCode", is("123456")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login/verify - Xác thực OTP thành công cấp token")
    void testLogin_VerifySuccess() throws Exception {
        LoginVerifyRequest req = LoginVerifyRequest.builder()
                .phone("0912345678")
                .code("123456")
                .build();

        mockMvc.perform(post("/api/v1/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.role", is("USER")))
                .andExpect(jsonPath("$.data.token", is("fake-jwt-token")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Đăng ký bước 1 gửi mã OTP")
    void testRegister_Step1() throws Exception {
        PatientRegisterRequest req = PatientRegisterRequest.builder()
                .phone("0912345678")
                .fullName("Trần Thị B")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.needCode", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/staff/login - Đăng nhập nhân sự thành công")
    void testStaffLogin_Success() throws Exception {
        StaffLoginRequest req = StaffLoginRequest.builder()
                .identifier("doctor_lan")
                .password("MatKhau@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.role", is("DOCTOR")))
                .andExpect(jsonPath("$.data.token", is("staff-jwt-token")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Đăng xuất thành công trả về 200 OK")
    void testLogout_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.success", is(true)));
    }
}
