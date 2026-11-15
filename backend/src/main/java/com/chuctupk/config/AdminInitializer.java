package com.chuctupk.config;

import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Tự tạo tài khoản ADMIN đầu tiên khi khởi động, nếu hệ thống CHƯA có admin nào.
 * Mật khẩu lấy từ biến môi trường, không nằm trong code hay SQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String username;

    @Value("${app.admin.password:}")
    private String password;

    @Value("${app.admin.full-name:Quản trị viên}")
    private String fullName;

    @Value("${app.admin.phone:}")
    private String phone;

    @Override
    public void run(ApplicationArguments args) {
        if (!userRepository.findByRole(Role.ADMIN).isEmpty()) {
            return; // Đã có admin, không làm gì
        }

        if (username.isBlank() || password.length() < MIN_PASSWORD_LENGTH || phone.isBlank()) {
            throw new IllegalStateException(
                "Chưa có tài khoản ADMIN. Hãy đặt biến môi trường ADMIN_USERNAME, "
              + "ADMIN_PASSWORD (tối thiểu " + MIN_PASSWORD_LENGTH + " ký tự) và ADMIN_PHONE rồi chạy lại.");
        }

        userRepository.save(User.builder()
                .username(username.trim())
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .phone(phone.trim())
                .role(Role.ADMIN)
                .mustChangePassword(true)
                .build());

        // Tuyệt đối không log mật khẩu
        log.info("Đã tạo tài khoản ADMIN '{}'. Cần đổi mật khẩu ở lần đăng nhập đầu tiên.", username);
    }
}
