package com.chuctupk.service.impl;

import com.chuctupk.common.util.SlugUtils;
import com.chuctupk.dto.request.CreateClinicServiceRequest;
import com.chuctupk.dto.request.UpdateClinicServiceRequest;
import com.chuctupk.dto.response.AdminServiceResponse;
import com.chuctupk.entity.ClinicService;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.ClinicServiceRepository;
import com.chuctupk.service.AdminServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceServiceImpl implements AdminServiceService {

    private final ClinicServiceRepository clinicServiceRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminServiceResponse> getAllServices() {
        List<ClinicService> services = clinicServiceRepository.findAllByOrderBySortOrderAscIdAsc();
        return services.stream().map(s -> {
            String slug = s.getSlug() != null ? s.getSlug() : "";
            String slugUnderscore = slug.replace("-", "_");
            long count = appointmentRepository.countAppointmentsForService(s.getId(), slug, slugUnderscore);
            return AdminServiceResponse.from(s, count);
        }).toList();
    }

    @Override
    public AdminServiceResponse createService(CreateClinicServiceRequest request) {
        if (request.getName() == null || request.getName().trim().isBlank()) {
            throw AppException.unprocessable("Tên dịch vụ không được để trống");
        }

        String rawSlug = request.getSlug() != null && !request.getSlug().trim().isBlank()
                ? request.getSlug()
                : request.getName();
        String slug = SlugUtils.toSlug(rawSlug);

        if (slug.isBlank()) {
            throw AppException.unprocessable("Slug không hợp lệ");
        }

        if (clinicServiceRepository.findBySlug(slug).isPresent()) {
            throw AppException.unprocessable("Slug đã tồn tại");
        }

        int durationMin = request.getDurationMin() != null ? request.getDurationMin() : 30;
        if (durationMin < 5 || durationMin > 240) {
            throw AppException.unprocessable("Thời lượng khám phải từ 5 đến 240 phút");
        }

        BigDecimal price = request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO;
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw AppException.unprocessable("Giá dịch vụ không được âm");
        }

        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : 0;
        boolean isActive = request.getIsActive() != null ? request.getIsActive() : true;

        ClinicService service = ClinicService.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .durationMin(durationMin)
                .price(price)
                .iconUrl(request.getIconUrl())
                .sortOrder(sortOrder)
                .isActive(isActive)
                .build();

        ClinicService saved = clinicServiceRepository.save(service);
        return AdminServiceResponse.from(saved, 0L);
    }

    @Override
    public AdminServiceResponse updateService(Long id, UpdateClinicServiceRequest request) {
        ClinicService service = clinicServiceRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy dịch vụ"));

        // Khóa lạc quan (Optimistic Locking) bằng updatedAt (STALE)
        if (request.getUpdatedAt() != null && service.getUpdatedAt() != null) {
            if (!service.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS).isEqual(request.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS))) {
                throw AppException.stale("Dữ liệu đã được người khác cập nhật trước đó. Vui lòng tải lại trang");
            }
        }

        if (request.getName() != null) {
            if (request.getName().trim().isBlank()) {
                throw AppException.unprocessable("Tên dịch vụ không được để trống");
            }
            service.setName(request.getName().trim());
        }

        if (request.getSlug() != null && !request.getSlug().trim().isBlank()) {
            String slug = SlugUtils.toSlug(request.getSlug());
            if (slug.isBlank()) {
                throw AppException.unprocessable("Slug không hợp lệ");
            }
            Optional<ClinicService> existing = clinicServiceRepository.findBySlug(slug);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw AppException.unprocessable("Slug đã tồn tại");
            }
            service.setSlug(slug);
        }

        if (request.getDurationMin() != null) {
            if (request.getDurationMin() < 5 || request.getDurationMin() > 240) {
                throw AppException.unprocessable("Thời lượng khám phải từ 5 đến 240 phút");
            }
            service.setDurationMin(request.getDurationMin());
        }

        if (request.getPrice() != null) {
            if (request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw AppException.unprocessable("Giá dịch vụ không được âm");
            }
            service.setPrice(request.getPrice());
        }

        if (request.getDescription() != null) {
            service.setDescription(request.getDescription());
        }

        if (request.getIconUrl() != null) {
            service.setIconUrl(request.getIconUrl());
        }

        if (request.getSortOrder() != null) {
            service.setSortOrder(request.getSortOrder());
        }

        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        ClinicService saved = clinicServiceRepository.save(service);
        String slug = saved.getSlug() != null ? saved.getSlug() : "";
        String slugUnderscore = slug.replace("-", "_");
        long count = appointmentRepository.countAppointmentsForService(saved.getId(), slug, slugUnderscore);
        return AdminServiceResponse.from(saved, count);
    }

    @Override
    public void deleteService(Long id) {
        ClinicService service = clinicServiceRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy dịch vụ"));

        String slug = service.getSlug() != null ? service.getSlug() : "";
        String slugUnderscore = slug.replace("-", "_");
        boolean isUsed = appointmentRepository.isServiceUsed(id, slug, slugUnderscore);

        if (isUsed) {
            throw AppException.unprocessable("Dịch vụ đã được dùng, hãy ẩn thay vì xoá");
        }

        clinicServiceRepository.delete(service);
    }
}
