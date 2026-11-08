package com.chuctupk.controller;

import com.chuctupk.dto.request.LoginRequest;
import com.chuctupk.dto.request.RegisterRequest;
import com.chuctupk.dto.request.SendOtpRequest;
import com.chuctupk.dto.request.VerifyOtpRequest;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.dto.response.UserResponse;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.OtpType;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/v1/auth/send-otp - Gửi mã thành công trả về 200")
    void testSendOtp_Success() throws Exception {
        SendOtpRequest request = new SendOtpRequest("0912345678", OtpType.REGISTER);
        doNothing().when(authService).sendOtp(any(SendOtpRequest.class));

        mockMvc.perform(post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)));

        verify(authService, times(1)).sendOtp(any(SendOtpRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/send-otp - SĐT sai định dạng trả về 400")
    void testSendOtp_InvalidPhone() throws Exception {
        SendOtpRequest request = new SendOtpRequest("123", OtpType.REGISTER);

        mockMvc.perform(post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/verify-otp - Xác thực OTP thành công trả về 200")
    void testVerifyOtp_Success() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("0912345678", "123456", OtpType.REGISTER);
        when(authService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(true)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Đăng ký thành công trả về 201")
    void testRegister_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .phone("0912345678")
                .fullName("Nguyễn Thị Hoa")
                .password("MatKhau@123")
                .otpCode("123456")
                .yearOfBirth(1960)
                .gender(Gender.FEMALE)
                .address("Quận Bình Thạnh, TP.HCM")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .phone("0912345678")
                .fullName("Nguyễn Thị Hoa")
                .role(Role.PATIENT)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .user(userResponse)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(201)))
                .andExpect(jsonPath("$.data.accessToken", is("mock-jwt-token")))
                .andExpect(jsonPath("$.data.user.fullName", is("Nguyễn Thị Hoa")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Đăng nhập đúng trả về 200")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest("0912345678", "MatKhau@123");

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .phone("0912345678")
                .fullName("Nguyễn Thị Hoa")
                .role(Role.PATIENT)
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-jwt-token")
                .tokenType("Bearer")
                .user(userResponse)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.accessToken", is("mock-jwt-token")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Sai mật khẩu trả về 401")
    void testLogin_WrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("0912345678", "WrongPass");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(AppException.unauthorized("Mật khẩu không chính xác"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(401)))
                .andExpect(jsonPath("$.message", is("Mật khẩu không chính xác")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Chưa đăng nhập trả về 401")
    void testGetMe_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(401)));
    }
}
