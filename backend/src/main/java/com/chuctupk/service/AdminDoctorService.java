package com.chuctupk.service;

import com.chuctupk.dto.request.CreateDoctorRequest;
import com.chuctupk.dto.request.UpdateDoctorRequest;
import com.chuctupk.dto.response.AdminDoctorDetailResponse;
import com.chuctupk.dto.response.AdminDoctorListResponse;
import com.chuctupk.dto.response.DoctorUserOptionResponse;

import java.util.List;

public interface AdminDoctorService {

    List<AdminDoctorListResponse> getAllDoctors();

    AdminDoctorDetailResponse getDoctorById(Long id);

    AdminDoctorDetailResponse createDoctor(CreateDoctorRequest request);

    AdminDoctorDetailResponse updateDoctor(Long id, UpdateDoctorRequest request);

    List<DoctorUserOptionResponse> getUnassignedDoctorAccounts(Long currentDoctorId);
}
