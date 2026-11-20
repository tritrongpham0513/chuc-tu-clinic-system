package com.chuctupk.service.impl;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.dto.request.CreateDoctorRequest;
import com.chuctupk.dto.request.UpdateDoctorRequest;
import com.chuctupk.dto.response.AdminDoctorDetailResponse;
import com.chuctupk.dto.response.AdminDoctorListResponse;
import com.chuctupk.dto.response.DoctorUserOptionResponse;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.AdminDoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDoctorServiceImpl implements AdminDoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    private static final List<AppointmentStatus> TERMINAL_APPOINTMENT_STATUSES = List.of(
            AppointmentStatus.CANCELLED,
            AppointmentStatus.COMPLETED
    );

    @Override
    @Transactional(readOnly = true)
    public List<AdminDoctorListResponse> getAllDoctors() {
        return doctorRepository.findAllByOrderBySortOrderAscFullNameAsc().stream()
                .map(this::toListResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDoctorDetailResponse getDoctorById(Long id) {
        Doctor doctor = findDoctorOrThrow(id);
        return toDetailResponse(doctor);
    }

    @Override
    @Transactional
    public AdminDoctorDetailResponse createDoctor(CreateDoctorRequest request) {
        if (!PhoneValidator.isValid(request.getPhone())) {
            throw AppException.badRequest("Số điện thoại không đúng định dạng Việt Nam (10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09)");
        }

        User user = null;
        if (request.getAccountId() != null) {
            user = userRepository.findById(request.getAccountId())
                    .orElseThrow(() -> AppException.unprocessable("Tài khoản không tồn tại trong hệ thống"));

            if (user.getRole() != Role.DOCTOR) {
                throw AppException.unprocessable("Tài khoản được gán phải có vai trò Bác sĩ (DOCTOR)");
            }

            if (doctorRepository.existsByUserId(request.getAccountId())) {
                throw AppException.unprocessable("Tài khoản này đã được gán cho một bác sĩ khác");
            }
        }

        int sortOrder = request.getSortOrder() != null
                ? request.getSortOrder()
                : doctorRepository.findMaxSortOrder() + 1;

        Doctor doctor = Doctor.builder()
                .user(user)
                .fullName(request.getFullName().trim())
                .title(request.getTitle() != null && !request.getTitle().isBlank() ? request.getTitle().trim() : null)
                .specialty(request.getSpecialty().trim())
                .experienceYears(request.getExperienceYears())
                .bio(request.getBio() != null ? request.getBio().trim() : null)
                .phone(PhoneValidator.clean(request.getPhone()))
                .avatarUrl(request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank() ? request.getAvatarUrl().trim() : null)
                .sortOrder(sortOrder)
                .isActive(true)
                .build();

        Doctor saved = doctorRepository.save(doctor);
        log.info("Admin đã tạo bác sĩ mới ID {} - {} (accountId: {})", saved.getId(), saved.getFullName(), request.getAccountId());
        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public AdminDoctorDetailResponse updateDoctor(Long id, UpdateDoctorRequest request) {
        Doctor doctor = findDoctorOrThrow(id);

        // Khóa lạc quan (Optimistic Locking): so sánh updatedAt
        if (request.getUpdatedAt() != null) {
            if (doctor.getUpdatedAt() != null &&
                    !doctor.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS).isEqual(request.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS))) {
                throw AppException.stale("Dữ liệu đã được người khác cập nhật trước đó. Vui lòng tải lại trang");
            }
        }

        // Ràng buộc ẩn bác sĩ: nếu chuyển từ hoạt động sang ẩn (isActive = false)
        if (request.getIsActive() != null && !request.getIsActive() && Boolean.TRUE.equals(doctor.getIsActive())) {
            LocalDate today = LocalDate.now(clock);
            long futureCount = appointmentRepository.countFutureAppointmentsForDoctor(id, today, TERMINAL_APPOINTMENT_STATUSES);
            if (futureCount > 0) {
                throw AppException.unprocessable("Bác sĩ còn " + futureCount + " phiếu khám sắp tới, không thể ẩn");
            }
            doctor.setIsActive(false);
        } else if (request.getIsActive() != null) {
            doctor.setIsActive(request.getIsActive());
        }

        // Cập nhật liên kết tài khoản
        if (Boolean.TRUE.equals(request.getUnassignAccount()) || Long.valueOf(0L).equals(request.getAccountId())) {
            doctor.setUser(null);
        } else if (request.getAccountId() != null) {
            User user = userRepository.findById(request.getAccountId())
                    .orElseThrow(() -> AppException.unprocessable("Tài khoản không tồn tại trong hệ thống"));

            if (user.getRole() != Role.DOCTOR) {
                throw AppException.unprocessable("Tài khoản được gán phải có vai trò Bác sĩ (DOCTOR)");
            }

            if (doctorRepository.existsByUserIdAndIdNot(request.getAccountId(), id)) {
                throw AppException.unprocessable("Tài khoản này đã được gán cho một bác sĩ khác");
            }
            doctor.setUser(user);
        }

        // Cập nhật các trường thông tin khác nếu có
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            doctor.setFullName(request.getFullName().trim());
        }
        if (request.getTitle() != null) {
            doctor.setTitle(request.getTitle().isBlank() ? null : request.getTitle().trim());
        }
        if (request.getSpecialty() != null && !request.getSpecialty().isBlank()) {
            doctor.setSpecialty(request.getSpecialty().trim());
        }
        if (request.getExperienceYears() != null) {
            doctor.setExperienceYears(request.getExperienceYears());
        }
        if (request.getBio() != null) {
            doctor.setBio(request.getBio().isBlank() ? null : request.getBio().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (!PhoneValidator.isValid(request.getPhone())) {
                throw AppException.badRequest("Số điện thoại không đúng định dạng Việt Nam (10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09)");
            }
            doctor.setPhone(PhoneValidator.clean(request.getPhone()));
        }
        if (request.getAvatarUrl() != null) {
            doctor.setAvatarUrl(request.getAvatarUrl().isBlank() ? null : request.getAvatarUrl().trim());
        }
        if (request.getSortOrder() != null) {
            doctor.setSortOrder(request.getSortOrder());
        }

        doctor.setUpdatedAt(LocalDateTime.now(clock));
        Doctor updated = doctorRepository.save(doctor);
        log.info("Admin đã cập nhật bác sĩ ID {} - {}", updated.getId(), updated.getFullName());
        return toDetailResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorUserOptionResponse> getUnassignedDoctorAccounts(Long currentDoctorId) {
        List<User> doctorUsers = userRepository.findByRole(Role.DOCTOR);
        Map<Long, Long> userToDoctorMap = doctorRepository.findAll().stream()
                .filter(d -> d.getUser() != null)
                .collect(Collectors.toMap(d -> d.getUser().getId(), Doctor::getId, (existing, replacement) -> existing));

        return doctorUsers.stream()
                .filter(u -> {
                    Long assignedDoctorId = userToDoctorMap.get(u.getId());
                    return assignedDoctorId == null || (currentDoctorId != null && assignedDoctorId.equals(currentDoctorId));
                })
                .map(u -> DoctorUserOptionResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName())
                        .phone(u.getPhone())
                        .build())
                .toList();
    }

    private Doctor findDoctorOrThrow(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thông tin bác sĩ"));
    }

    private AdminDoctorListResponse toListResponse(Doctor doctor) {
        return AdminDoctorListResponse.builder()
                .id(doctor.getId())
                .fullName(doctor.getFullName())
                .title(doctor.getTitle())
                .specialty(doctor.getSpecialty())
                .experienceYears(doctor.getExperienceYears())
                .phone(PhoneValidator.mask(doctor.getPhone()))
                .avatarUrl(doctor.getAvatarUrl())
                .sortOrder(doctor.getSortOrder())
                .isActive(doctor.getIsActive())
                .accountId(doctor.getUser() != null ? doctor.getUser().getId() : null)
                .accountUsername(doctor.getUser() != null ? doctor.getUser().getUsername() : null)
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .build();
    }

    private AdminDoctorDetailResponse toDetailResponse(Doctor doctor) {
        return AdminDoctorDetailResponse.builder()
                .id(doctor.getId())
                .fullName(doctor.getFullName())
                .title(doctor.getTitle())
                .specialty(doctor.getSpecialty())
                .experienceYears(doctor.getExperienceYears())
                .bio(doctor.getBio())
                .phone(doctor.getPhone())
                .avatarUrl(doctor.getAvatarUrl())
                .sortOrder(doctor.getSortOrder())
                .isActive(doctor.getIsActive())
                .accountId(doctor.getUser() != null ? doctor.getUser().getId() : null)
                .accountUsername(doctor.getUser() != null ? doctor.getUser().getUsername() : null)
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .build();
    }
}
