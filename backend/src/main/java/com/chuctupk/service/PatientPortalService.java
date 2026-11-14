package com.chuctupk.service;

import com.chuctupk.dto.request.PatientBookAppointmentRequest;
import com.chuctupk.dto.request.PatientProfileUpsertRequest;
import com.chuctupk.dto.request.UpdateMeRequest;
import com.chuctupk.entity.User;

import java.time.LocalDate;
import java.util.Map;

public interface PatientPortalService {

    // 1. Tài khoản & thông tin cá nhân
    Map<String, Object> getMyAccount(User currentUser);
    Map<String, Object> updateMyAccount(User currentUser, UpdateMeRequest request);

    // 2. Hồ sơ bệnh nhân (Bản thân & Người thân, tối đa 10)
    Map<String, Object> getMyPatients(User currentUser);
    Map<String, Object> createPatient(User currentUser, PatientProfileUpsertRequest request);
    Map<String, Object> getPatient(User currentUser, Long patientId);
    Map<String, Object> updatePatient(User currentUser, Long patientId, PatientProfileUpsertRequest request);
    Map<String, Object> deletePatient(User currentUser, Long patientId);

    // 3. Khung giờ & Đặt lịch khám bệnh (Đồng thời an toàn <= 6/khung + Idempotency)
    Map<String, Object> getAvailability(LocalDate date);
    Map<String, Object> bookAppointment(User currentUser, PatientBookAppointmentRequest request, String idempotencyKey);

    // 4. Quản lý phiếu khám của tôi
    Map<String, Object> getMyAppointments(User currentUser, String statusFilter);
    Map<String, Object> getAppointmentDetail(User currentUser, Long id);
    Map<String, Object> cancelAppointment(User currentUser, Long id, String reason);
    Map<String, Object> getAppointmentResult(User currentUser, Long id);
    Map<String, Object> getMyHistory(User currentUser);
}

