package com.chuctupk.service;

import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.dto.response.UserDto;
import com.chuctupk.dto.response.UserResponse;
import com.chuctupk.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface AuthService {

    // === V1 Authentication APIs ===
    Map<String, Object> requestPatientLoginOtp(String phone);
    Map<String, Object> verifyPatientLogin(LoginVerifyRequest request, HttpServletResponse response);
    Map<String, Object> requestPatientRegisterOtp(PatientRegisterRequest request);
    Map<String, Object> verifyPatientRegister(RegisterVerifyRequest request, HttpServletResponse response);
    Map<String, Object> resendOtp(String phone, String purpose);
    Map<String, Object> loginStaff(StaffLoginRequest request, HttpServletResponse response);
    Map<String, Object> staffForgotPassword(String identifier);
    Map<String, Object> staffVerifyForgotPassword(StaffForgotPasswordVerifyRequest request);
    Map<String, Object> staffResetPassword(StaffResetPasswordRequest request);
    UserDto changeUserPassword(User currentUser, ChangePasswordRequest request);
    Map<String, Object> refreshToken(HttpServletRequest request, HttpServletResponse response);
    void logout(User currentUser, HttpServletRequest request, HttpServletResponse response);
    UserDto getCurrentUserDto(User currentUser);

    // === Tài khoản nội bộ (Admin / Lễ tân / Bác sĩ) ===
    UserDto updateStaffAccountName(User currentUser, String fullName);
    Map<String, Object> requestStaffEmailChange(User currentUser, String newEmail);
    Map<String, Object> verifyStaffEmailChange(User currentUser, String code);

    // === Legacy methods (preserved for backward compatibility & existing tests) ===
    void sendOtp(SendOtpRequest request);
    boolean verifyOtp(VerifyOtpRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse loginWithOtp(LoginOtpRequest request);
    AuthResponse quickLoginByPhone(String phone);
    UserResponse getCurrentUser(User currentUser);
    UserResponse updateMyProfile(User currentUser, UpdateUserProfileRequest request);
    void changePassword(User currentUser, ChangePasswordRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPasswordWithOtp(ResetPasswordWithOtpRequest request);
    UserResponse updateStaffProfile(User currentUser, UpdateStaffProfileRequest request);
}
