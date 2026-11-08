package com.chuctupk.service;

public interface SmsService {
    boolean sendOtp(String phone, String otpCode);
}

