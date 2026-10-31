package com.chuctupk.service.impl;

import com.chuctupk.dto.response.ClinicServiceResponse;
import com.chuctupk.entity.ClinicService;
import com.chuctupk.repository.ClinicServiceRepository;
import com.chuctupk.service.ClinicServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicServiceServiceImpl implements ClinicServiceService {

    private final ClinicServiceRepository clinicServiceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClinicServiceResponse> getActiveServices() {
        return clinicServiceRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private ClinicServiceResponse toResponse(ClinicService service) {
        return ClinicServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .slug(service.getSlug())
                .description(service.getDescription())
                .durationMin(service.getDurationMin())
                .price(service.getPrice())
                .iconUrl(service.getIconUrl())
                .build();
    }
}

