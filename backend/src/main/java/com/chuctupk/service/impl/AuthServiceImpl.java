package com.chuctupk.service.impl;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.dto.request.LoginOtpRequest;
import com.chuctupk.dto.request.LoginRequest;
import com.chuctupk.dto.request.RegisterRequest;
import com.chuctupk.dto.request.SendOtpRequest;
import com.chuctupk.dto.request.VerifyOtpRequest;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.dto.response.UserResponse;
import com.chuctupk.entity.OtpVerification;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.OtpType;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.OtpVerificationRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.AuthService;
import com.chuctupk.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Random RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final com.chuctupk.service.AppointmentService appointmentService;

    @Override
    @Transactional
    public void sendOtp(SendOtpRequest request) {
        String cleanPhone = cleanPhone(request.getPhone());
        OtpType type = request.getOtpType();

        if (type == OtpType.REGISTER) {
            if (userRepository.existsByPhone(cleanPhone)) {
                throw AppException.conflict("Số điện thoại này đã được đăng ký tài khoản. Vui lòng đăng nhập.");
            }
        } else if (type == OtpType.LOGIN) {
            if (!userRepository.existsByPhone(cleanPhone)) {
                throw AppException.notFound("Số điện thoại chưa được đăng ký tài khoản. Vui lòng đăng ký.");
            }
        }

        // Sinh mã OTP 6 chữ số ngẫu nhiên
        String otpCode = String.format("%06d", RANDOM.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        OtpVerification verification = OtpVerification.builder()
                .phone(cleanPhone)
                .otpCode(otpCode)
                .otpType(type)
                .expiresAt(expiresAt)
                .isVerified(false)
                .attemptCount(0)
                .build();

        otpVerificationRepository.save(verification);

        // Gửi qua SMS Gateway
        boolean sent = smsService.sendOtp(cleanPhone, otpCode);
        if (!sent) {
            log.warn("Không thể gửi tin nhắn SMS tới số {}", cleanPhone);
        }
    }

    @Override
    @Transactional
    public boolean verifyOtp(VerifyOtpRequest request) {
        String cleanPhone = cleanPhone(request.getPhone());
        String code = request.getOtpCode().trim();
        OtpType type = request.getOtpType();

        OtpVerification verification = otpVerificationRepository
                .findTopByPhoneAndOtpTypeAndIsVerifiedFalseOrderByCreatedAtDesc(cleanPhone, type)
                .orElseThrow(() -> AppException.badRequest("Mã xác thực không tồn tại hoặc đã được sử dụng"));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (verification.getAttemptCount() >= 5) {
            throw AppException.badRequest("Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã mới.");
        }

        if (!verification.getOtpCode().equals(code)) {
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            otpVerificationRepository.save(verification);
            throw AppException.badRequest("Mã xác thực không chính xác. Vui lòng thử lại.");
        }

        verification.setIsVerified(true);
        // Kéo dài thêm 15 phút để người dùng kịp điền biểu mẫu hoàn tất đăng ký
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        otpVerificationRepository.save(verification);

        return true;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanPhone = cleanPhone(request.getPhone());

        // 1. Kiểm tra hoặc thực hiện xác thực mã OTP
        Optional<OtpVerification> verifiedOpt = otpVerificationRepository
                .findTopByPhoneAndOtpTypeAndIsVerifiedTrueAndExpiresAtAfterOrderByCreatedAtDesc(
                        cleanPhone, OtpType.REGISTER, LocalDateTime.now());

        if (verifiedOpt.isEmpty()) {
            if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
                throw AppException.badRequest("Mã xác thực OTP không được để trống.");
            }
            VerifyOtpRequest verifyRequest = new VerifyOtpRequest(cleanPhone, request.getOtpCode().trim(), OtpType.REGISTER);
            boolean ok = verifyOtp(verifyRequest);
            if (!ok) {
                throw AppException.badRequest("Mã xác thực không chính xác hoặc đã hết hạn.");
            }
        }

        // 2. Kiểm tra tài khoản đã tồn tại chưa
        if (userRepository.existsByPhone(cleanPhone)) {
            throw AppException.conflict("Số điện thoại này đã được đăng ký tài khoản. Vui lòng đăng nhập.");
        }

        // 3. Tạo tài khoản User (Role = PATIENT)
        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : UUID.randomUUID().toString().substring(0, 12);

        User user = User.builder()
                .username(cleanPhone) // Dùng luôn SĐT làm username
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(request.getFullName().trim().replaceAll("\\s+", " "))
                .phone(cleanPhone)
                .role(Role.PATIENT)
                .isActive(true)
                .mustChangePassword(false)
                .build();

        user = userRepository.save(user);

        // 4. Liên kết hoặc tạo mới hồ sơ Bệnh nhân (Patient)
        List<Patient> existingPatients = patientRepository.findByPhone(cleanPhone);
        if (!existingPatients.isEmpty()) {
            for (Patient p : existingPatients) {
                if (p.getUser() == null) {
                    p.setUser(user);
                    patientRepository.save(p);
                }
            }
        } else {
            // Chưa từng đi khám -> tạo trước hồ sơ rỗng gắn vào tài khoản
            String patientCode = appointmentService.generateNextPatientCode();
            Patient newPatient = Patient.builder()
                    .user(user)
                    .patientCode(patientCode)
                    .fullName(user.getFullName())
                    .phone(cleanPhone)
                    .yearOfBirth(request.getYearOfBirth())
                    .gender(request.getGender())
                    .address(request.getAddress())
                    .build();
            patientRepository.save(newPatient);
        }

        // 5. Sinh JWT Token
        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String input = request.getUsernameOrPhone().trim();
        String cleanPhone = cleanPhone(input);

        // Tìm tài khoản theo username hoặc số điện thoại
        User user = userRepository.findByUsername(input)
                .or(() -> userRepository.findByPhone(cleanPhone))
                .orElseThrow(() -> AppException.badRequest("Số điện thoại hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginCount(user.getFailedLoginCount() + 1);
            userRepository.save(user);
            throw AppException.badRequest("Số điện thoại hoặc mật khẩu không chính xác");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw AppException.forbidden("Tài khoản của bạn hiện đang bị tạm khóa. Vui lòng liên hệ phòng khám.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginCount(0);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse loginWithOtp(LoginOtpRequest request) {
        String cleanPhone = cleanPhone(request.getPhone());

        // 1. Xác thực OTP
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(cleanPhone, request.getOtpCode(), OtpType.LOGIN);
        boolean ok = verifyOtp(verifyRequest);
        if (!ok) {
            throw AppException.badRequest("Mã xác nhận không chính xác hoặc đã hết hạn.");
        }

        // 2. Tìm tài khoản người dùng theo SĐT
        User user = userRepository.findByPhone(cleanPhone)
                .or(() -> userRepository.findByUsername(cleanPhone))
                .orElseThrow(() -> AppException.badRequest("Số điện thoại này chưa được đăng ký tài khoản. Cô chú vui lòng chọn Tạo tài khoản mới."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw AppException.forbidden("Tài khoản của bạn hiện đang bị tạm khóa. Vui lòng liên hệ phòng khám.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginCount(0);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse quickLoginByPhone(String phone) {
        String cleanPhone = cleanPhone(phone);
        User user = userRepository.findByPhone(cleanPhone)
                .or(() -> userRepository.findByUsername(cleanPhone))
                .orElseThrow(() -> AppException.badRequest("Số điện thoại này chưa được đăng ký tài khoản. Cô chú vui lòng tạo tài khoản mới."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw AppException.forbidden("Tài khoản của bạn hiện đang bị tạm khóa. Vui lòng liên hệ phòng khám.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginCount(0);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(User currentUser) {
        if (currentUser == null) {
            throw AppException.unauthorized("Chưa đăng nhập");
        }
        return toUserResponse(currentUser);
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(User currentUser, com.chuctupk.dto.request.UpdateUserProfileRequest request) {
        if (currentUser == null) {
            throw AppException.unauthorized("Chưa đăng nhập");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thông tin tài khoản"));

        user.setFullName(request.getFullName().trim());
        user = userRepository.save(user);

        // Cập nhật hồ sơ bệnh nhân chính chủ (isPrimary = true)
        patientRepository.findByUserIdAndIsPrimaryTrue(user.getId()).ifPresent(patient -> {
            patient.setFullName(request.getFullName().trim());
            patient.setYearOfBirth(request.getYearOfBirth());
            patient.setGender(request.getGender());
            patient.setAddress(request.getAddress());
            patientRepository.save(patient);
        });

        return toUserResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(User currentUser, com.chuctupk.dto.request.ChangePasswordRequest request) {
        if (currentUser == null) {
            throw AppException.unauthorized("Chưa đăng nhập");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thông tin tài khoản"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw AppException.badRequest("Mật khẩu hiện tại không chính xác. Vui lòng kiểm tra lại.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(com.chuctupk.dto.request.ForgotPasswordRequest request) {
        String cleanPhone = cleanPhone(request.getPhone());
        if (!userRepository.existsByPhone(cleanPhone) && !userRepository.existsByUsername(cleanPhone)) {
            throw AppException.notFound("Số điện thoại chưa được đăng ký trong hệ thống.");
        }

        String otpCode = String.format("%06d", RANDOM.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        OtpVerification verification = OtpVerification.builder()
                .phone(cleanPhone)
                .otpCode(otpCode)
                .otpType(OtpType.FORGOT_PASSWORD)
                .expiresAt(expiresAt)
                .isVerified(false)
                .attemptCount(0)
                .build();

        otpVerificationRepository.save(verification);
        smsService.sendOtp(cleanPhone, otpCode);
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(com.chuctupk.dto.request.ResetPasswordWithOtpRequest request) {
        String cleanPhone = cleanPhone(request.getPhone());
        String code = request.getOtp().trim();

        OtpVerification verification = otpVerificationRepository
                .findTopByPhoneAndOtpTypeAndIsVerifiedFalseOrderByCreatedAtDesc(cleanPhone, OtpType.FORGOT_PASSWORD)
                .orElseThrow(() -> AppException.badRequest("Mã OTP không hợp lệ hoặc đã được sử dụng."));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (!verification.getOtpCode().equals(code)) {
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            otpVerificationRepository.save(verification);
            throw AppException.badRequest("Mã OTP không chính xác.");
        }

        verification.setIsVerified(true);
        otpVerificationRepository.save(verification);

        User user = userRepository.findByPhone(cleanPhone)
                .or(() -> userRepository.findByUsername(cleanPhone))
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tài khoản với số điện thoại này."));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse updateStaffProfile(User currentUser, com.chuctupk.dto.request.UpdateStaffProfileRequest request) {
        if (currentUser == null) {
            throw AppException.unauthorized("Chưa đăng nhập");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thông tin tài khoản"));

        user.setFullName(request.getFullName().trim());
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim());
        }
        user = userRepository.save(user);
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .email(u.getEmail())
                .role(u.getRole())
                .build();
    }

    private String cleanPhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "").trim();
        if (digits.startsWith("84") && digits.length() == 11) {
            return "0" + digits.substring(2);
        }
        if (digits.length() == 9 && !digits.startsWith("0")) {
            return "0" + digits;
        }
        return digits;
    }
}
