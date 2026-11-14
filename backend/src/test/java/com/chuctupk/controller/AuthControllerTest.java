package com.chuctupk.controller;

import com.chuctupk.dto.request.LoginOtpRequest;
import com.chuctupk.dto.request.LoginRequest;
import com.chuctupk.dto.request.LoginVerifyRequest;
import com.chuctupk.dto.request.PatientRegisterRequest;
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
    @DisplayName("POST /api/v1/auth/register - Bước 1: gửi mã OTP đăng ký, trả về 200 + needCode")
    void testRegister_Success() throws Exception {
        PatientRegisterRequest request = PatientRegisterRequest.builder()
                .phone("0912345678")
                .fullName("Nguyễn Thị Hoa")
                .build();

        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("needCode", true);
        data.put("phone", "0912345678");
        data.put("resendAfterSeconds", 60);
        when(authService.requestPatientRegisterOtp(any(PatientRegisterRequest.class))).thenReturn(data);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.needCode", is(true)))
                .andExpect(jsonPath("$.data.phone", is("0912345678")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Bước 1: yêu cầu mã OTP đăng nhập, trả về 200 + needCode")
    void testLogin_Success() throws Exception {
        LoginOtpRequest request = new LoginOtpRequest("0912345678", null);

        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("needCode", true);
        data.put("phone", "0912345678");
        data.put("resendAfterSeconds", 60);
        when(authService.requestPatientLoginOtp("0912345678")).thenReturn(data);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.needCode", is(true)))
                .andExpect(jsonPath("$.data.phone", is("0912345678")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login/verify - Mã OTP sai trả về 400 kèm code CODE_WRONG")
    void testLogin_WrongPassword() throws Exception {
        LoginVerifyRequest request = LoginVerifyRequest.builder()
                .phone("0912345678")
                .code("000000")
                .build();

        when(authService.verifyPatientLogin(any(LoginVerifyRequest.class), any()))
                .thenThrow(new AppException(com.chuctupk.exception.ErrorCode.CODE_WRONG,
                        "Mã xác nhận không đúng. Cô chú kiểm tra lại tin nhắn nhé"));

        mockMvc.perform(post("/api/v1/auth/login/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(400)))
                .andExpect(jsonPath("$.code", is("CODE_WRONG")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Chưa đăng nhập trả về 200 với user: null")
    void testGetMe_Unauthorized() throws Exception {
        when(authService.getCurrentUserDto(null)).thenReturn(null);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user").doesNotExist());
    }
}
