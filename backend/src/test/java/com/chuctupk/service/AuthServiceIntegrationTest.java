package com.chuctupk.service;

import com.chuctupk.dto.request.LoginOtpRequest;
import com.chuctupk.dto.request.LoginRequest;
import com.chuctupk.dto.request.RegisterRequest;
import com.chuctupk.dto.request.SendOtpRequest;
import com.chuctupk.dto.request.VerifyOtpRequest;
import com.chuctupk.dto.response.AuthResponse;
import com.chuctupk.entity.OtpVerification;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.OtpType;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.OtpVerificationRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    private final String TEST_PHONE = "0987654321";

    @BeforeEach
    void cleanUp() {
        otpVerificationRepository.deleteAll();
        userRepository.findByPhone(TEST_PHONE).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Gửi OTP -> Lưu DB -> Xác thực OTP thành công")
    void testSendAndVerifyOtp() {
        // 1. Gửi OTP
        authService.sendOtp(new SendOtpRequest(TEST_PHONE, OtpType.REGISTER));

        // 2. Lấy OTP từ DB
        Optional<OtpVerification> opt = otpVerificationRepository
                .findTopByPhoneAndOtpTypeOrderByCreatedAtDesc(TEST_PHONE, OtpType.REGISTER);

        assertTrue(opt.isPresent(), "Mã OTP phải được lưu vào CSDL");
        String otpCode = opt.get().getOtpCode();
        assertNotNull(otpCode);
        assertEquals(6, otpCode.length());

        // 3. Xác thực OTP
        boolean verified = authService.verifyOtp(new VerifyOtpRequest(TEST_PHONE, otpCode, OtpType.REGISTER));
        assertTrue(verified, "Xác thực OTP phải thành công");
    }

    @Test
    @DisplayName("Đăng ký tài khoản sau khi xác thực OTP -> Tạo User và Patient -> Đăng nhập thành công")
    void testRegisterAndLoginFlow() {
        // 1. Gửi và xác thực OTP
        authService.sendOtp(new SendOtpRequest(TEST_PHONE, OtpType.REGISTER));
        OtpVerification otp = otpVerificationRepository
                .findTopByPhoneAndOtpTypeOrderByCreatedAtDesc(TEST_PHONE, OtpType.REGISTER).orElseThrow();
        authService.verifyOtp(new VerifyOtpRequest(TEST_PHONE, otp.getOtpCode(), OtpType.REGISTER));

        // 2. Đăng ký tài khoản
        RegisterRequest registerRequest = RegisterRequest.builder()
                .phone(TEST_PHONE)
                .fullName("Trần Thị Mai")
                .yearOfBirth(1955)
                .gender(Gender.FEMALE)
                .address("Phường 25, Quận Bình Thạnh, TP.HCM")
                .otpCode(otp.getOtpCode())
                .password("MatKhau@2026")
                .build();

        AuthResponse authResponse = authService.register(registerRequest);
        assertNotNull(authResponse.getAccessToken(), "Phải trả về Access Token sau khi đăng ký");
        assertEquals("Trần Thị Mai", authResponse.getUser().getFullName());
        assertEquals(Role.PATIENT, authResponse.getUser().getRole());

        // 3. Kiểm tra DB xem User và Patient đã tạo chưa
        Optional<User> userOpt = userRepository.findByPhone(TEST_PHONE);
        assertTrue(userOpt.isPresent());
        assertEquals("Trần Thị Mai", userOpt.get().getFullName());

        var patients = patientRepository.findByPhone(TEST_PHONE);
        assertFalse(patients.isEmpty(), "Hồ sơ bệnh nhân phải được tự động liên kết với SĐT");
        assertNotNull(patients.get(0).getPatientCode());

        // 4. Đăng nhập bằng mật khẩu
        AuthResponse loginResponse = authService.login(new LoginRequest(TEST_PHONE, "MatKhau@2026"));
        assertNotNull(loginResponse.getAccessToken());
        assertEquals(TEST_PHONE, loginResponse.getUser().getPhone());

        // 5. Đăng nhập nhanh bằng SĐT (máy quen)
        AuthResponse quickResponse = authService.quickLoginByPhone(TEST_PHONE);
        assertNotNull(quickResponse.getAccessToken());

        // 6. Đăng nhập máy mới qua OTP
        authService.sendOtp(new SendOtpRequest(TEST_PHONE, OtpType.LOGIN));
        OtpVerification loginOtp = otpVerificationRepository
                .findTopByPhoneAndOtpTypeOrderByCreatedAtDesc(TEST_PHONE, OtpType.LOGIN).orElseThrow();
        AuthResponse otpLoginResponse = authService.loginWithOtp(new LoginOtpRequest(TEST_PHONE, loginOtp.getOtpCode()));
        assertNotNull(otpLoginResponse.getAccessToken());
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu ném ngoại lệ 400/401")
    void testLoginWithWrongPassword() {
        // Gửi và xác thực OTP
        authService.sendOtp(new SendOtpRequest(TEST_PHONE, OtpType.REGISTER));
        OtpVerification otp = otpVerificationRepository
                .findTopByPhoneAndOtpTypeOrderByCreatedAtDesc(TEST_PHONE, OtpType.REGISTER).orElseThrow();
        authService.verifyOtp(new VerifyOtpRequest(TEST_PHONE, otp.getOtpCode(), OtpType.REGISTER));

        // Đăng ký
        authService.register(RegisterRequest.builder()
                .phone(TEST_PHONE)
                .fullName("Bác Ba")
                .yearOfBirth(1950)
                .gender(Gender.MALE)
                .otpCode(otp.getOtpCode())
                .password("MatKhauDung@123")
                .build());

        // Đăng nhập sai pass
        assertThrows(AppException.class, () ->
                authService.login(new LoginRequest(TEST_PHONE, "MatKhauSai@999")));
    }
}

