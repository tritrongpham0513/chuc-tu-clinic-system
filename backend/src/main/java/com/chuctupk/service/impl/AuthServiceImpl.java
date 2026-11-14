package com.chuctupk.service.impl;

import com.chuctupk.common.CookieHelper;
import com.chuctupk.common.HashUtils;
import com.chuctupk.common.PhoneValidator;
import com.chuctupk.common.mapping.ContractMapper;
import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.dto.response.UserDto;
import com.chuctupk.dto.response.UserResponse;
import com.chuctupk.entity.OtpVerification;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.RefreshToken;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.OtpType;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.ErrorCode;
import com.chuctupk.repository.OtpVerificationRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.RefreshTokenRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.AuthService;
import com.chuctupk.service.SequenceGeneratorService;
import com.chuctupk.service.SmsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Random RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final com.chuctupk.service.AppointmentService appointmentService;

    // Cache tạm cho đăng ký bệnh nhân trước khi xác thực mã OTP
    public record PendingRegistration(
            String phone,
            String fullName,
            LocalDate dob,
            Integer yearOfBirth,
            Gender gender,
            String address,
            LocalDateTime expiresAt
    ) {}
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    // Cache tạm cho token đặt lại mật khẩu nhân viên (10 phút)
    public record ResetTokenInfo(Long userId, LocalDateTime expiresAt) {}
    private final Map<String, ResetTokenInfo> resetTokens = new ConcurrentHashMap<>();

    // Cache tạm cho yêu cầu đổi email của tài khoản nội bộ (10 phút)
    public record PendingEmailChange(
            Long userId,
            String newEmail,
            String codeHash,
            LocalDateTime expiresAt,
            int attempts
    ) {}
    private final Map<Long, PendingEmailChange> pendingEmailChanges = new ConcurrentHashMap<>();

    /** Bật khi đã nối dịch vụ gửi email thật; khi tắt, mã xác minh được trả về trong `devCode`. */
    @org.springframework.beans.factory.annotation.Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    // =========================================================================
    // V1 API IMPLEMENTATIONS
    // =========================================================================

    @Override
    @Transactional
    public Map<String, Object> requestPatientLoginOtp(String phone) {
        String cleanPhone = PhoneValidator.validate(phone);
        checkOtpRateLimitAndCooldown(cleanPhone);

        String rawCode = String.format("%06d", RANDOM.nextInt(1000000));
        String codeHash = HashUtils.sha256(rawCode);

        OtpVerification otp = OtpVerification.builder()
                .phone(cleanPhone)
                .otpCode(codeHash)
                .otpType(OtpType.LOGIN)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isVerified(false)
                .attemptCount(0)
                .build();
        otpVerificationRepository.save(otp);

        boolean sent = false;
        if (smsService != null) {
            sent = smsService.sendOtp(cleanPhone, rawCode);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("needCode", true);
        res.put("phone", cleanPhone);
        res.put("resendAfterSeconds", 60);
        putDevCodeIfSimulated(res, rawCode, sent);
        res.put("message", "Mã xác nhận 6 chữ số đã được gửi tới số điện thoại " + PhoneValidator.mask(cleanPhone));
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> verifyPatientLogin(LoginVerifyRequest request, HttpServletResponse response) {
        String cleanPhone = PhoneValidator.validate(request.getPhone());
        verifyOtpInternal(cleanPhone, request.getCode(), OtpType.LOGIN);

        List<User> userList = userRepository.findAllByPhone(cleanPhone);
        User user = userList.stream()
                .filter(u -> u.getRole() == Role.PATIENT)
                .findFirst()
                .orElseGet(() -> createDefaultPatientUser(cleanPhone, null, null, null, null, null));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED,
                    "Tài khoản đang bị khóa. Vui lòng liên hệ Phòng khám Chúc Từ");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String jwt = jwtTokenProvider.generateToken(user);
        issueRefreshTokenAndCookie(user, Boolean.TRUE.equals(request.getRemember()), response, jwt);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user", UserDto.from(user));
        res.put("token", jwt);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> requestPatientRegisterOtp(PatientRegisterRequest request) {
        String cleanPhone = PhoneValidator.validate(request.getPhone());
        checkOtpRateLimitAndCooldown(cleanPhone);

        LocalDate dob = parseDob(request.getDob(), request.getBirthYear());
        Integer yearOfBirth = dob != null ? dob.getYear() : (request.getBirthYear() != null ? request.getBirthYear() : 1970);
        Gender gender = ContractMapper.toDbGender(request.getGender());
        if (gender == null) gender = Gender.MALE;

        String name = request.getFullName() != null && !request.getFullName().isBlank()
                ? request.getFullName().trim()
                : "Bệnh nhân " + (cleanPhone.length() >= 4 ? cleanPhone.substring(cleanPhone.length() - 4) : cleanPhone);

        pendingRegistrations.put(cleanPhone, new PendingRegistration(
                cleanPhone,
                name,
                dob,
                yearOfBirth,
                gender,
                request.getAddress() != null ? request.getAddress().trim() : "TP. Hồ Chí Minh",
                LocalDateTime.now().plusMinutes(15)
        ));

        String rawCode = String.format("%06d", RANDOM.nextInt(1000000));
        String codeHash = HashUtils.sha256(rawCode);

        OtpVerification otp = OtpVerification.builder()
                .phone(cleanPhone)
                .otpCode(codeHash)
                .otpType(OtpType.REGISTER)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isVerified(false)
                .attemptCount(0)
                .build();
        otpVerificationRepository.save(otp);

        boolean sent = false;
        if (smsService != null) {
            sent = smsService.sendOtp(cleanPhone, rawCode);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("needCode", true);
        res.put("phone", cleanPhone);
        res.put("resendAfterSeconds", 60);
        putDevCodeIfSimulated(res, rawCode, sent);
        res.put("message", "Mã xác nhận 6 chữ số đã được gửi tới số điện thoại " + PhoneValidator.mask(cleanPhone));
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> verifyPatientRegister(RegisterVerifyRequest request, HttpServletResponse response) {
        String cleanPhone = PhoneValidator.validate(request.getPhone());
        verifyOtpInternal(cleanPhone, request.getCode(), OtpType.REGISTER);

        PendingRegistration pending = pendingRegistrations.remove(cleanPhone);

        String fullName = request.getFullName() != null && !request.getFullName().isBlank()
                ? request.getFullName().trim()
                : (pending != null ? pending.fullName() : "Bệnh nhân " + (cleanPhone.length() >= 4 ? cleanPhone.substring(cleanPhone.length() - 4) : cleanPhone));

        LocalDate parsedDob = parseDob(request.getDob(), request.getBirthYear());
        final LocalDate finalDob = (parsedDob != null) ? parsedDob : (pending != null ? pending.dob() : null);

        Integer yearOfBirth = finalDob != null ? finalDob.getYear() : (request.getBirthYear() != null ? request.getBirthYear() : (pending != null ? pending.yearOfBirth() : 1970));

        Gender parsedGender = ContractMapper.toDbGender(request.getGender());
        final Gender finalGender = (parsedGender != null) ? parsedGender : (pending != null && pending.gender() != null ? pending.gender() : Gender.MALE);

        String address = request.getAddress() != null && !request.getAddress().isBlank()
                ? request.getAddress().trim()
                : (pending != null ? pending.address() : "TP. Hồ Chí Minh");

        List<User> userList = userRepository.findAllByPhone(cleanPhone);
        User user = userList.stream()
                .filter(u -> u.getRole() == Role.PATIENT)
                .findFirst()
                .orElseGet(() -> createDefaultPatientUser(cleanPhone, fullName, finalDob, yearOfBirth, finalGender, address));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED,
                    "Tài khoản đang bị khóa. Vui lòng liên hệ Phòng khám Chúc Từ");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String jwt = jwtTokenProvider.generateToken(user);
        issueRefreshTokenAndCookie(user, Boolean.TRUE.equals(request.getRemember()), response, jwt);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user", UserDto.from(user));
        res.put("token", jwt);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> resendOtp(String phone, String purpose) {
        String cleanPhone = PhoneValidator.validate(phone);
        checkOtpRateLimitAndCooldown(cleanPhone);

        OtpType type = "REGISTER".equalsIgnoreCase(purpose) ? OtpType.REGISTER : OtpType.LOGIN;
        String rawCode = String.format("%06d", RANDOM.nextInt(1000000));
        String codeHash = HashUtils.sha256(rawCode);

        OtpVerification otp = OtpVerification.builder()
                .phone(cleanPhone)
                .otpCode(codeHash)
                .otpType(type)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isVerified(false)
                .attemptCount(0)
                .build();
        otpVerificationRepository.save(otp);

        boolean sent = false;
        if (smsService != null) {
            sent = smsService.sendOtp(cleanPhone, rawCode);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("resendAfterSeconds", 60);
        putDevCodeIfSimulated(res, rawCode, sent);
        res.put("message", "Đã gửi lại mã xác nhận tới số điện thoại " + PhoneValidator.mask(cleanPhone));
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> loginStaff(StaffLoginRequest request, HttpServletResponse response) {
        String identifier = request.getIdentifier() != null ? request.getIdentifier().trim() : "";
        String digits = identifier.replaceAll("\\D", "");

        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> {
                    if (digits.length() >= 10) {
                        return userRepository.findAllByPhone(digits).stream()
                                .filter(u -> u.getRole() != Role.PATIENT)
                                .findFirst();
                    }
                    return Optional.empty();
                })
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED,
                        "Tài khoản hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản đang bị khóa. Vui lòng liên hệ Quản trị viên");
        }

        if (user.getRole() == Role.PATIENT) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản này không có quyền truy cập khu vực nội bộ");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String jwt = jwtTokenProvider.generateToken(user);
        issueRefreshTokenAndCookie(user, false, response, jwt);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user", UserDto.from(user));
        res.put("token", jwt);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> staffForgotPassword(String identifier) {
        String cleanId = identifier != null ? identifier.trim() : "";
        String digits = cleanId.replaceAll("\\D", "");

        Optional<User> userOpt = userRepository.findByUsername(cleanId)
                .or(() -> userRepository.findByEmail(cleanId))
                .or(() -> {
                    if (digits.length() >= 10) {
                        return userRepository.findAllByPhone(digits).stream()
                                .filter(u -> u.getRole() != Role.PATIENT)
                                .findFirst();
                    }
                    return Optional.empty();
                });

        // Anti-enumeration: luôn trả về needCode = true
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("needCode", true);
        res.put("identifier", cleanId);
        res.put("resendAfterSeconds", 60);

        String rawCode = String.format("%06d", RANDOM.nextInt(1000000));
        String codeHash = HashUtils.sha256(rawCode);
        putDevCodeIfSimulated(res, rawCode);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            res.put("email", user.getEmail());

            OtpVerification otp = OtpVerification.builder()
                    .phone(user.getPhone() != null ? user.getPhone() : cleanId)
                    .otpCode(codeHash)
                    .otpType(OtpType.FORGOT_PASSWORD)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .isVerified(false)
                    .attemptCount(0)
                    .build();
            otpVerificationRepository.save(otp);
        }

        res.put("message", "Mã xác thực đã được gửi tới email hoặc số điện thoại của tài khoản");
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> staffVerifyForgotPassword(StaffForgotPasswordVerifyRequest request) {
        String cleanId = request.getEffectiveIdentifier();
        String digits = cleanId.replaceAll("\\D", "");

        User user = userRepository.findByUsername(cleanId)
                .or(() -> userRepository.findByEmail(cleanId))
                .or(() -> {
                    if (digits.length() >= 10) {
                        return userRepository.findAllByPhone(digits).stream()
                                .filter(u -> u.getRole() != Role.PATIENT)
                                .findFirst();
                    }
                    return Optional.empty();
                })
                .orElseThrow(() -> new AppException(ErrorCode.CODE_EXPIRED,
                        "Mã xác thực không hợp lệ hoặc đã hết hạn"));

        String phone = user.getPhone() != null ? user.getPhone() : cleanId;
        verifyOtpInternal(phone, request.getCode(), OtpType.FORGOT_PASSWORD);

        String resetToken = "RST-" + UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
        resetTokens.put(resetToken, new ResetTokenInfo(user.getId(), LocalDateTime.now().plusMinutes(10)));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("resetToken", resetToken);
        res.put("token", resetToken);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> staffResetPassword(StaffResetPasswordRequest request) {
        ResetTokenInfo info = resetTokens.remove(request.getToken());
        if (info == null || info.expiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS,
                    "Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
        }

        User user = userRepository.findById(info.userId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy tài khoản người dùng"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới");
        return res;
    }

    @Override
    @Transactional
    public UserDto changeUserPassword(User currentUser, ChangePasswordRequest request) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập lại");
        }

        String currentPw = request.getEffectiveCurrentPassword();
        if (currentPw == null || !passwordEncoder.matches(currentPw, currentUser.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS,
                    "Mật khẩu hiện tại không đúng", Map.of("currentPassword", "Mật khẩu hiện tại không đúng"));
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Mật khẩu mới tối thiểu 6 ký tự", Map.of("newPassword", "Mật khẩu mới tối thiểu 6 ký tự"));
        }

        currentUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setMustChangePassword(false);
        User saved = userRepository.save(currentUser);

        refreshTokenRepository.revokeAllByUserId(currentUser.getId());
        return UserDto.from(saved);
    }

    @Override
    @Transactional
    public Map<String, Object> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String tokenStr = extractRefreshToken(request);
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED,
                    "Không tìm thấy phiên làm việc để làm mới");
        }

        String tokenHash = HashUtils.sha256(tokenStr);
        RefreshToken rt = refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_EXPIRED,
                        "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại"));

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        User user = rt.getUser();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED, "Tài khoản đang bị khóa");
        }

        String newJwt = jwtTokenProvider.generateToken(user);
        issueRefreshTokenAndCookie(user, false, response, newJwt);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("token", newJwt);
        res.put("user", UserDto.from(user));
        return res;
    }

    @Override
    @Transactional
    public void logout(User currentUser, HttpServletRequest request, HttpServletResponse response) {
        if (currentUser != null) {
            refreshTokenRepository.revokeAllByUserId(currentUser.getId());
        }
        SecurityContextHolder.clearContext();
        if (response != null) {
            CookieHelper.clearSessionCookie(response, false);
            CookieHelper.clearRefreshCookie(response, false);
            CookieHelper.clearDeviceCookie(response, false);
        }
    }

    @Override
    public UserDto getCurrentUserDto(User currentUser) {
        if (currentUser == null) return null;
        return UserDto.from(currentUser);
    }

    // =========================================================================
    // TÀI KHOẢN NỘI BỘ: ĐỔI HỌ TÊN & ĐỔI EMAIL CÓ XÁC MINH
    // =========================================================================

    @Override
    @Transactional
    public UserDto updateStaffAccountName(User currentUser, String fullName) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập để cập nhật thông tin tài khoản");
        }
        if (fullName == null || fullName.trim().length() < 2) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Vui lòng nhập họ và tên (ít nhất 2 ký tự)");
        }
        currentUser.setFullName(fullName.trim());
        User saved = userRepository.save(currentUser);
        return UserDto.from(saved);
    }

    @Override
    @Transactional
    public Map<String, Object> requestStaffEmailChange(User currentUser, String newEmail) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập để đổi email");
        }
        String email = newEmail != null ? newEmail.trim().toLowerCase() : "";
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Email chưa đúng định dạng. Ví dụ: ten@email.com");
        }

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent() && !existing.get().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_PHONE,
                    "Email này đã được dùng cho một tài khoản khác");
        }

        String rawCode = String.format("%06d", RANDOM.nextInt(1000000));
        pendingEmailChanges.put(currentUser.getId(), new PendingEmailChange(
                currentUser.getId(),
                email,
                HashUtils.sha256(rawCode),
                LocalDateTime.now().plusMinutes(10),
                0
        ));

        // Chưa nối dịch vụ email: ghi log để đội phát triển đối chiếu
        log.info("[EMAIL VERIFY] Đã tạo mã xác minh đổi email cho tài khoản #{}", currentUser.getId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("needCode", true);
        res.put("email", email);
        res.put("resendAfterSeconds", 60);
        if (!mailEnabled) {
            res.put("devCode", rawCode);
        }
        res.put("message", "Mã xác minh 6 số đã được gửi tới email " + email);
        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> verifyStaffEmailChange(User currentUser, String code) {
        if (currentUser == null) {
            throw AppException.unauthorized("Vui lòng đăng nhập để xác minh email");
        }

        PendingEmailChange pending = pendingEmailChanges.get(currentUser.getId());
        if (pending == null) {
            throw new AppException(ErrorCode.CODE_EXPIRED,
                    "Chưa có yêu cầu đổi email hoặc mã đã hết hiệu lực. Vui lòng gửi lại mã mới");
        }
        if (pending.expiresAt().isBefore(LocalDateTime.now())) {
            pendingEmailChanges.remove(currentUser.getId());
            throw new AppException(ErrorCode.CODE_EXPIRED,
                    "Mã xác minh đã hết hạn. Vui lòng gửi lại mã mới");
        }
        if (pending.attempts() >= 5) {
            pendingEmailChanges.remove(currentUser.getId());
            throw new AppException(ErrorCode.CODE_LOCKED,
                    "Đã nhập sai mã quá 5 lần. Vui lòng gửi lại mã mới");
        }

        String rawCode = code != null ? code.trim() : "";
        if (!HashUtils.sha256(rawCode).equals(pending.codeHash())) {
            pendingEmailChanges.put(currentUser.getId(), new PendingEmailChange(
                    pending.userId(), pending.newEmail(), pending.codeHash(),
                    pending.expiresAt(), pending.attempts() + 1));
            throw new AppException(ErrorCode.CODE_WRONG,
                    "Mã xác minh 6 số không đúng. Vui lòng kiểm tra lại email");
        }

        // Không cho trùng email với tài khoản khác tại thời điểm xác minh
        Optional<User> existing = userRepository.findByEmail(pending.newEmail());
        if (existing.isPresent() && !existing.get().getId().equals(currentUser.getId())) {
            pendingEmailChanges.remove(currentUser.getId());
            throw new AppException(ErrorCode.DUPLICATE_PHONE,
                    "Email này đã được dùng cho một tài khoản khác");
        }

        currentUser.setEmail(pending.newEmail());
        User saved = userRepository.save(currentUser);
        pendingEmailChanges.remove(currentUser.getId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user", UserDto.from(saved));
        res.put("message", "Đã xác minh và cập nhật email mới");
        return res;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Chỉ trả mã OTP ra ngoài khi cổng SMS chưa gửi được (ở chế độ giả lập hoặc SMS thật gửi thất bại).
     * Khi đã gửi SMS thật thành công đến thiết bị, mã OTP tuyệt đối không xuất hiện trong phản hồi HTTP.
     */
    private void putDevCodeIfSimulated(Map<String, Object> res, String rawCode) {
        putDevCodeIfSimulated(res, rawCode, smsService != null && smsService.isEnabled());
    }

    private void putDevCodeIfSimulated(Map<String, Object> res, String rawCode, boolean realSent) {
        if (!realSent) {
            res.put("devCode", rawCode);
        }
    }

    private void checkOtpRateLimitAndCooldown(String cleanPhone) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long countInHour = otpVerificationRepository.countByPhoneAndCreatedAtAfter(cleanPhone, oneHourAgo);
        if (countInHour >= 3) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS,
                    "Cô chú đã yêu cầu gửi mã quá 3 lần trong 1 giờ. Vui lòng thử lại sau ít phút", 3600);
        }

        Optional<OtpVerification> lastOtpOpt = otpVerificationRepository.findTopByPhoneOrderByCreatedAtDesc(cleanPhone);
        if (lastOtpOpt.isPresent()) {
            LocalDateTime lastCreated = lastOtpOpt.get().getCreatedAt();
            long elapsed = Duration.between(lastCreated, LocalDateTime.now()).getSeconds();
            if (elapsed < 60) {
                int retryAfter = (int) (60 - elapsed);
                throw new AppException(ErrorCode.TOO_MANY_REQUESTS,
                        "Vui lòng đợi " + retryAfter + " giây trước khi yêu cầu mã mới", retryAfter);
            }
        }
    }

    private void verifyOtpInternal(String cleanPhone, String inputCode, OtpType type) {
        OtpVerification otp = otpVerificationRepository
                .findTopByPhoneAndOtpTypeAndIsVerifiedFalseOrderByCreatedAtDesc(cleanPhone, type)
                .or(() -> otpVerificationRepository.findTopByPhoneAndOtpTypeAndIsVerifiedFalseOrderByCreatedAtDesc(cleanPhone, OtpType.LOGIN))
                .or(() -> otpVerificationRepository.findTopByPhoneAndOtpTypeAndIsVerifiedFalseOrderByCreatedAtDesc(cleanPhone, OtpType.REGISTER))
                .orElseThrow(() -> new AppException(ErrorCode.CODE_EXPIRED,
                        "Mã xác thực không tồn tại hoặc đã hết hạn"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.CODE_EXPIRED,
                    "Mã xác thực đã hết hạn. Cô chú vui lòng nhấn gửi lại mã mới");
        }

        if (otp.getAttemptCount() != null && otp.getAttemptCount() >= 5) {
            otp.setIsVerified(true);
            otpVerificationRepository.save(otp);
            throw new AppException(ErrorCode.CODE_LOCKED,
                    "Cô chú đã nhập sai mã quá 5 lần. Vui lòng bấm gửi lại mã mới", 60);
        }

        String rawCode = inputCode != null ? inputCode.trim() : "";
        String inputHash = HashUtils.sha256(rawCode);
        boolean matches = inputHash.equals(otp.getOtpCode());

        if (!matches) {
            otp.setAttemptCount((otp.getAttemptCount() != null ? otp.getAttemptCount() : 0) + 1);
            otpVerificationRepository.save(otp);
            throw new AppException(ErrorCode.CODE_WRONG,
                    "Mã xác nhận 6 số không đúng. Cô chú vui lòng kiểm tra lại");
        }

        otp.setIsVerified(true);
        otpVerificationRepository.save(otp);
    }

    private User createDefaultPatientUser(String cleanPhone, String fullName, LocalDate dob, Integer yearOfBirth, Gender gender, String address) {
        String name = fullName != null && !fullName.isBlank()
                ? fullName
                : "Bệnh nhân " + (cleanPhone.length() >= 4 ? cleanPhone.substring(cleanPhone.length() - 4) : cleanPhone);

        User newUser = User.builder()
                .username("patient_" + cleanPhone)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(name)
                .phone(cleanPhone)
                .role(Role.PATIENT)
                .isActive(true)
                .mustChangePassword(false)
                .failedLoginCount(0)
                .build();
        User saved = userRepository.save(newUser);

        String patientCode = sequenceGeneratorService.nextPatientCode();
        Patient patient = Patient.builder()
                .user(saved)
                .patientCode(patientCode)
                .fullName(saved.getFullName())
                .phone(cleanPhone)
                .dob(dob)
                .yearOfBirth(yearOfBirth != null ? yearOfBirth : 1970)
                .gender(gender != null ? gender : Gender.MALE)
                .relationship("BAN_THAN")
                .address(address != null ? address : "TP. Hồ Chí Minh")
                .isPrimary(true)
                .isActive(true)
                .build();
        patientRepository.save(patient);
        return saved;
    }

    private void issueRefreshTokenAndCookie(User user, boolean remember, HttpServletResponse response, String jwt) {
        int refreshDays = remember ? 30 : 7;
        String rawRefreshToken = "RT-" + UUID.randomUUID() + "-" + System.currentTimeMillis();
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtils.sha256(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plusDays(refreshDays))
                .revoked(false)
                .build();
        refreshTokenRepository.save(rt);

        if (response != null) {
            CookieHelper.addSessionCookie(response, jwt, 24 * 3600, false);

            // Cookie Refresh Token thật: HttpOnly, chỉ gửi tới /api/v1/auth để làm mới phiên
            CookieHelper.addRefreshCookie(response, rawRefreshToken, refreshDays * 24 * 3600, false);

            if (remember) {
                CookieHelper.addDeviceCookie(response, "dev_" + user.getId() + "_" + System.currentTimeMillis(), false);
            }
        }
    }

    /**
     * Lấy Refresh Token theo thứ tự ưu tiên:
     * 1. Cookie HttpOnly {@code ct_refresh} (luồng chuẩn của trình duyệt)
     * 2. Header {@code X-Refresh-Token} (ứng dụng di động / công cụ kiểm thử)
     * 3. Tham số {@code refreshToken} trên query string hoặc form
     */
    private String extractRefreshToken(HttpServletRequest request) {
        if (request == null) return null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (CookieHelper.REFRESH_COOKIE_NAME.equals(c.getName())
                        && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }

        String header = request.getHeader("X-Refresh-Token");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }

        String param = request.getParameter("refreshToken");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }

        return null;
    }

    private LocalDate parseDob(String dobStr, Integer birthYear) {
        if (dobStr != null && !dobStr.isBlank()) {
            String clean = dobStr.trim();
            try {
                if (clean.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    return LocalDate.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE);
                } else if (clean.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                    return LocalDate.parse(clean, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else if (clean.matches("^\\d{4}$")) {
                    return LocalDate.of(Integer.parseInt(clean), 1, 1);
                }
            } catch (Exception ignored) {}
        }
        if (birthYear != null && birthYear >= 1900 && birthYear <= 2100) {
            return LocalDate.of(birthYear, 1, 1);
        }
        return null;
    }

    // =========================================================================
    // LEGACY METHODS (preserved for tests and compatibility)
    // =========================================================================

    @Override
    @Transactional
    public void sendOtp(SendOtpRequest request) {
        requestPatientLoginOtp(request.getPhone());
    }

    @Override
    @Transactional
    public boolean verifyOtp(VerifyOtpRequest request) {
        verifyOtpInternal(request.getPhone(), request.getOtpCode(), request.getOtpType());
        return true;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        RegisterVerifyRequest req = RegisterVerifyRequest.builder()
                .phone(request.getPhone())
                .code(request.getOtpCode())
                .fullName(request.getFullName())
                .birthYear(request.getYearOfBirth())
                .gender(request.getGender() != null ? request.getGender().name() : "MALE")
                .address(request.getAddress())
                .build();
        Map<String, Object> map = verifyPatientRegister(req, null);
        String token = (String) map.get("token");
        UserDto u = (UserDto) map.get("user");
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(UserResponse.builder()
                        .id(Long.parseLong(u.id()))
                        .username(u.username())
                        .fullName(u.fullName())
                        .phone(u.phone())
                        .email(u.email())
                        .role(ContractMapper.toDbRole(u.role()))
                        .build())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String cleanPhone = PhoneValidator.validate(request.getUsernameOrPhone());
        User user = userRepository.findByUsername(request.getUsernameOrPhone())
                .or(() -> userRepository.findByPhone(cleanPhone))
                .orElseThrow(() -> AppException.badRequest("Tài khoản hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AppException.badRequest("Tài khoản hoặc mật khẩu không chính xác");
        }

        String token = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse loginWithOtp(LoginOtpRequest request) {
        LoginVerifyRequest req = LoginVerifyRequest.builder()
                .phone(request.getPhone())
                .code(request.getOtpCode())
                .build();
        Map<String, Object> map = verifyPatientLogin(req, null);
        String token = (String) map.get("token");
        UserDto u = (UserDto) map.get("user");
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(UserResponse.builder()
                        .id(Long.parseLong(u.id()))
                        .username(u.username())
                        .fullName(u.fullName())
                        .phone(u.phone())
                        .email(u.email())
                        .role(ContractMapper.toDbRole(u.role()))
                        .build())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse quickLoginByPhone(String phone) {
        String cleanPhone = PhoneValidator.validate(phone);
        User user = userRepository.findByPhone(cleanPhone)
                .orElseThrow(() -> AppException.notFound("Số điện thoại chưa từng được đăng ký"));

        String token = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
    }

    @Override
    public UserResponse getCurrentUser(User currentUser) {
        if (currentUser == null) return null;
        return UserResponse.builder()
                .id(currentUser.getId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .phone(currentUser.getPhone())
                .email(currentUser.getEmail())
                .role(currentUser.getRole())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(User currentUser, UpdateUserProfileRequest request) {
        if (currentUser == null) throw AppException.unauthorized("Chưa đăng nhập");
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            currentUser.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null) {
            currentUser.setEmail(request.getEmail().trim());
        }
        User saved = userRepository.save(currentUser);
        return getCurrentUser(saved);
    }

    @Override
    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        changeUserPassword(currentUser, request);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        staffForgotPassword(request.getEffectiveIdentifier());
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        StaffForgotPasswordVerifyRequest req = StaffForgotPasswordVerifyRequest.builder()
                .identifier(request.getPhone())
                .code(request.getOtp())
                .build();
        Map<String, Object> verifyRes = staffVerifyForgotPassword(req);
        String resetToken = (String) verifyRes.get("resetToken");
        staffResetPassword(StaffResetPasswordRequest.builder()
                .token(resetToken)
                .newPassword(request.getNewPassword())
                .build());
    }

    @Override
    @Transactional
    public UserResponse updateStaffProfile(User currentUser, UpdateStaffProfileRequest request) {
        if (currentUser == null) throw AppException.unauthorized("Chưa đăng nhập");
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            currentUser.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null) {
            currentUser.setEmail(request.getEmail().trim());
        }
        User saved = userRepository.save(currentUser);
        return getCurrentUser(saved);
    }
}
