package com.chuctupk.service;

import com.chuctupk.dto.request.LoginOtpRequest;
import com.chuctupk.dto.request.LoginRequest;
import com.chuctupk.dto.request.RegisterRequest;
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

import java.util.Map;
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

    /** Gửi OTP ở chế độ giả lập (chưa nối SMS) để lấy mã thật dùng cho kiểm thử. */
    private String requestRegisterCode() {
        Map<String, Object> res = authService.resendOtp(TEST_PHONE, "REGISTER");
        String devCode = (String) res.get("devCode");
        assertNotNull(devCode, "Ở chế độ giả lập, phản hồi phải có devCode để kiểm thử");
        assertEquals(6, devCode.length(), "Mã xác thực phải gồm đúng 6 chữ số");
        return devCode;
    }

    @Test
    @DisplayName("Gửi OTP -> chỉ lưu hash trong DB -> xác thực đúng mã thành công")
    void testSendAndVerifyOtp() {
        String otpCode = requestRegisterCode();

        Optional<OtpVerification> opt = otpVerificationRepository
                .findTopByPhoneAndOtpTypeOrderByCreatedAtDesc(TEST_PHONE, OtpType.REGISTER);

        assertTrue(opt.isPresent(), "Mã OTP phải được lưu vào CSDL");
        assertNotEquals(otpCode, opt.get().getOtpCode(),
                "CSDL chỉ được lưu SHA-256 hash, không lưu mã thô");

        boolean verified = authService.verifyOtp(new VerifyOtpRequest(TEST_PHONE, otpCode, OtpType.REGISTER));
        assertTrue(verified, "Xác thực OTP phải thành công");
    }

    @Test
    @DisplayName("Xác thực OTP sai phải bị từ chối")
    void testVerifyOtp_WrongCode() {
        requestRegisterCode();
        assertThrows(AppException.class, () ->
                authService.verifyOtp(new VerifyOtpRequest(TEST_PHONE, "000000", OtpType.REGISTER)));
    }

    @Test
    @DisplayName("Đăng ký tài khoản sau khi xác thực OTP -> Tạo User và Patient -> Đăng nhập OTP thành công")
    void testRegisterAndLoginFlow() {
        String code = requestRegisterCode();

        AuthResponse authResponse = authService.register(RegisterRequest.builder()
                .phone(TEST_PHONE)
                .fullName("Trần Thị Mai")
                .yearOfBirth(1955)
                .gender(Gender.FEMALE)
                .address("Phường 25, Quận Bình Thạnh, TP.HCM")
                .otpCode(code)
                .password("MatKhau@2026")
                .build());

        assertNotNull(authResponse.getAccessToken(), "Phải trả về Access Token sau khi đăng ký");
        assertEquals("Trần Thị Mai", authResponse.getUser().getFullName());
        assertEquals(Role.PATIENT, authResponse.getUser().getRole());

        Optional<User> userOpt = userRepository.findByPhone(TEST_PHONE);
        assertTrue(userOpt.isPresent());
        assertEquals("Trần Thị Mai", userOpt.get().getFullName());

        var patients = patientRepository.findByPhone(TEST_PHONE);
        assertFalse(patients.isEmpty(), "Hồ sơ bệnh nhân phải được tự động liên kết với SĐT");
        assertNotNull(patients.get(0).getPatientCode());

        // Đăng nhập nhanh bằng SĐT (máy quen)
        AuthResponse quickResponse = authService.quickLoginByPhone(TEST_PHONE);
        assertNotNull(quickResponse.getAccessToken());

        // Đăng nhập máy mới qua OTP
        // Xóa dấu vết OTP cũ để không vướng quy tắc chờ 60 giây giữa 2 lần gửi mã
        otpVerificationRepository.deleteAll();
        Map<String, Object> loginOtpRes = authService.resendOtp(TEST_PHONE, "LOGIN");
        String loginCode = (String) loginOtpRes.get("devCode");
        AuthResponse otpLoginResponse = authService.loginWithOtp(new LoginOtpRequest(TEST_PHONE, loginCode));
        assertNotNull(otpLoginResponse.getAccessToken());
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu ném ngoại lệ")
    void testLoginWithWrongPassword() {
        String code = requestRegisterCode();

        authService.register(RegisterRequest.builder()
                .phone(TEST_PHONE)
                .fullName("Bác Ba")
                .yearOfBirth(1950)
                .gender(Gender.MALE)
                .otpCode(code)
                .password("MatKhauDung@123")
                .build());

        assertThrows(AppException.class, () ->
                authService.login(new LoginRequest(TEST_PHONE, "MatKhauSai@999")));
    }
}
