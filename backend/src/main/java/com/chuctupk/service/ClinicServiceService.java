package com.chuctupk.service;

import com.chuctupk.dto.response.ClinicServiceResponse;
import java.util.List;

public interface ClinicServiceService {
    List<ClinicServiceResponse> getActiveServices();
}

