package com.chuctupk.service;

import com.chuctupk.dto.request.LoginRequest;
import com.chuctupk.dto.request.RegisterRequest;
import com.chuctupk.dto.request.SendOtpRequest;
import com.chuctupk.dto.request.VerifyOtpRequest;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.dto.response.UserResponse;
import com.chuctupk.entity.User;

public interface AuthService {
    void sendOtp(SendOtpRequest request);
    boolean verifyOtp(VerifyOtpRequest request);
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse loginWithOtp(com.chuctupk.dto.request.LoginOtpRequest request);
    AuthResponse quickLoginByPhone(String phone);
    UserResponse getCurrentUser(User currentUser);

    // Bổ sung các chức năng theo Function List Excel:
    UserResponse updateMyProfile(User currentUser, com.chuctupk.dto.request.UpdateUserProfileRequest request);
    void changePassword(User currentUser, com.chuctupk.dto.request.ChangePasswordRequest request);
    void forgotPassword(com.chuctupk.dto.request.ForgotPasswordRequest request);
    void resetPasswordWithOtp(com.chuctupk.dto.request.ResetPasswordWithOtpRequest request);
    UserResponse updateStaffProfile(User currentUser, com.chuctupk.dto.request.UpdateStaffProfileRequest request);
}
