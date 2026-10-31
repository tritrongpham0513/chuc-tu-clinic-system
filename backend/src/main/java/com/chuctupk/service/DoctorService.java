package com.chuctupk.service;

import com.chuctupk.dto.response.DoctorResponse;
import java.util.List;

public interface DoctorService {
    List<DoctorResponse> getActiveDoctors();
}

