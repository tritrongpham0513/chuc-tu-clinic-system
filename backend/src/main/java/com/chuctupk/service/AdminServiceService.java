package com.chuctupk.service;

import com.chuctupk.dto.request.CreateClinicServiceRequest;
import com.chuctupk.dto.request.UpdateClinicServiceRequest;
import com.chuctupk.dto.response.AdminServiceResponse;

import java.util.List;

public interface AdminServiceService {
    List<AdminServiceResponse> getAllServices();
    AdminServiceResponse createService(CreateClinicServiceRequest request);
    AdminServiceResponse updateService(Long id, UpdateClinicServiceRequest request);
    void deleteService(Long id);
}
