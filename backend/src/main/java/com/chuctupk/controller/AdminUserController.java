package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateUserAccountRequest;
import com.chuctupk.dto.request.ResetPasswordRequest;
import com.chuctupk.dto.request.UpdateUserRoleRequest;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.dto.response.UserAdminDetailResponse;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Danh sách tài khoản (lọc theo role hoặc tìm kiếm)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAdminDetailResponse>>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search
    ) {
        List<User> list = role != null
                ? userRepository.findByRoleOrderByCreatedAtDesc(role)
                : userRepository.findByOrderByCreatedAtDesc();

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            list = list.stream().filter(u ->
                    u.getUsername().toLowerCase().contains(q) ||
                    u.getFullName().toLowerCase().contains(q) ||
                    u.getPhone().contains(q)
            ).toList();
        }

        List<UserAdminDetailResponse> responses = list.stream().map(this::toAdminResponse).toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "Lấy danh sách tài khoản thành công"));
    }

    /**
     * Tạo tài khoản mới cho nhân viên / bác sĩ / quản trị viên
     */
    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<UserAdminDetailResponse>> createUser(
            @Valid @RequestBody CreateUserAccountRequest request
    ) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw AppException.conflict("Tên đăng nhập '" + username + "' đã tồn tại");
        }

        String phone = request.getPhone().trim();
        if (userRepository.existsByPhone(phone)) {
            throw AppException.conflict("Số điện thoại '" + phone + "' đã được đăng ký tài khoản khác");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword().trim()))
                .fullName(request.getFullName().trim())
                .phone(phone)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .role(request.getRole())
                .isActive(true)
                .mustChangePassword(false)
                .build();

        user = userRepository.save(user);

        // Nếu phân quyền là Bác sĩ -> đồng bộ hồ sơ Bác sĩ (Doctor)
        if (request.getRole() == Role.DOCTOR) {
            syncDoctorProfile(user, request.getTitle(), request.getSpecialty());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(toAdminResponse(user), "Tạo tài khoản và phân quyền thành công"));
    }

    /**
     * Phân quyền (Đổi Role) cho tài khoản
     */
    @PatchMapping("/{id}/role")
    @Transactional
    public ResponseEntity<ApiResponse<UserAdminDetailResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));

        Role oldRole = user.getRole();
        user.setRole(request.getRole());
        user = userRepository.save(user);

        // Xử lý đồng bộ bảng doctors
        if (request.getRole() == Role.DOCTOR) {
            syncDoctorProfile(user, request.getTitle(), request.getSpecialty());
        } else if (oldRole == Role.DOCTOR) {
            // Hạ cấp từ Doctor xuống vai trò khác -> tắt trạng thái hoạt động bác sĩ
            doctorRepository.findByUserId(user.getId()).ifPresent(d -> {
                d.setIsActive(false);
                doctorRepository.save(d);
            });
        }

        return ResponseEntity.ok(ApiResponse.success(toAdminResponse(user), "Phân quyền tài khoản thành công"));
    }

    /**
     * Khóa hoặc Kích hoạt tài khoản
     */
    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<UserAdminDetailResponse>> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));

        if ("admin".equalsIgnoreCase(user.getUsername()) && Boolean.FALSE.equals(isActive)) {
            throw AppException.badRequest("Không được phép khóa tài khoản quản trị viên gốc (admin)");
        }

        user.setIsActive(isActive);
        user = userRepository.save(user);

        // Đồng bộ trạng thái bác sĩ nếu có
        doctorRepository.findByUserId(user.getId()).ifPresent(d -> {
            d.setIsActive(isActive);
            doctorRepository.save(d);
        });

        String msg = Boolean.TRUE.equals(isActive) ? "Kích hoạt tài khoản thành công" : "Khóa tài khoản thành công";
        return ResponseEntity.ok(ApiResponse.success(toAdminResponse(user), msg));
    }

    /**
     * Đặt lại mật khẩu cho tài khoản
     */
    @PostMapping("/{id}/reset-password")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(null, "Đặt lại mật khẩu thành công cho tài khoản " + user.getUsername()));
    }

    /**
     * F.8.3 - Cập nhật thông tin tài khoản nhân sự (Họ tên, SĐT, Email)
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<UserAdminDetailResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody com.chuctupk.dto.request.UpdateUserAccountRequest req
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản"));

        user.setFullName(req.getFullName().trim());
        user.setPhone(req.getPhone().trim());
        user.setEmail(req.getEmail() != null && !req.getEmail().isBlank() ? req.getEmail().trim() : null);
        final User savedUser = userRepository.save(user);

        // Đồng bộ họ tên và SĐT sang bảng Doctor nếu là Bác sĩ
        if (savedUser.getRole() == Role.DOCTOR) {
            doctorRepository.findByUserId(savedUser.getId()).ifPresent(doc -> {
                doc.setFullName(savedUser.getFullName());
                doc.setPhone(savedUser.getPhone());
                doctorRepository.save(doc);
            });
        }

        return ResponseEntity.ok(ApiResponse.success(toAdminResponse(savedUser), "Cập nhật thông tin tài khoản thành công"));
    }

    private void syncDoctorProfile(User user, String title, String specialty) {
        Optional<Doctor> existingOpt = doctorRepository.findByUserId(user.getId());
        Doctor doc;
        if (existingOpt.isPresent()) {
            doc = existingOpt.get();
            doc.setIsActive(true);
            doc.setFullName(user.getFullName());
            doc.setPhone(user.getPhone());
            if (title != null && !title.isBlank()) doc.setTitle(title.trim());
            if (specialty != null && !specialty.isBlank()) doc.setSpecialty(specialty.trim());
        } else {
            doc = Doctor.builder()
                    .user(user)
                    .fullName(user.getFullName())
                    .phone(user.getPhone())
                    .title(title != null && !title.isBlank() ? title.trim() : "Bác sĩ YHCT")
                    .specialty(specialty != null && !specialty.isBlank() ? specialty.trim() : "Y học cổ truyền")
                    .isActive(true)
                    .sortOrder(1)
                    .build();
        }
        doctorRepository.save(doc);
    }

    private UserAdminDetailResponse toAdminResponse(User u) {
        String roleLabel = switch (u.getRole()) {
            case ADMIN -> "Quản trị viên";
            case DOCTOR -> "Bác sĩ YHCT";
            case STAFF -> "Tiếp tân / Lễ tân";
            case PATIENT -> "Bệnh nhân";
        };

        Doctor doc = doctorRepository.findByUserId(u.getId()).orElse(null);

        return UserAdminDetailResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .email(u.getEmail())
                .role(u.getRole())
                .roleLabel(roleLabel)
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .doctorId(doc != null ? doc.getId() : null)
                .doctorTitle(doc != null ? doc.getTitle() : null)
                .doctorSpecialty(doc != null ? doc.getSpecialty() : null)
                .build();
    }
}

