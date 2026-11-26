package com.chuctupk.repository;

import com.chuctupk.entity.User;
import com.chuctupk.entity.UserNotification;
import com.chuctupk.entity.enums.NotificationInboxType;
import com.chuctupk.entity.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserNotificationRepositoryTest {

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User createUser(String prefix, Role role) {
        long suffix = System.nanoTime();
        return userRepository.save(User.builder()
                .username(prefix + "_" + suffix)
                .passwordHash("$2a$12$DummyHashForTest12345678901234567890123456789012")
                .fullName("Nhân viên " + prefix)
                .phone("09" + String.valueOf(suffix).substring(String.valueOf(suffix).length() - 8))
                .role(role)
                .isActive(true)
                .mustChangePassword(false)
                .build());
    }

    @Test
    @DisplayName("countByUserIdAndReadAtIsNull đúng sau khi đánh dấu đã đọc và markAllRead trả đúng số dòng")
    void testUnreadCountAndMarkAllRead() {
        User userA = createUser("staff_a", Role.STAFF);
        User userB = createUser("staff_b", Role.STAFF);

        UserNotification n1 = userNotificationRepository.save(UserNotification.builder()
                .user(userA)
                .type(NotificationInboxType.APPOINTMENT_CREATED)
                .title("Lịch khám mới")
                .body("Bệnh nhân A đặt lịch 08:30 ngày 27/11/2026")
                .link("/noi-bo/lich-trong-ngay?ngay=2026-11-27")
                .build());

        UserNotification n2 = userNotificationRepository.save(UserNotification.builder()
                .user(userA)
                .type(NotificationInboxType.APPOINTMENT_CANCELLED)
                .title("Lịch khám đã huỷ")
                .body("Bệnh nhân B đã huỷ lịch 09:00 ngày 27/11/2026")
                .link("/noi-bo/lich-trong-ngay?ngay=2026-11-27")
                .build());

        userNotificationRepository.save(UserNotification.builder()
                .user(userB)
                .type(NotificationInboxType.APPOINTMENT_CREATED)
                .title("Lịch khám mới")
                .body("Bệnh nhân C đặt lịch 10:00 ngày 27/11/2026")
                .link("/noi-bo/lich-trong-ngay?ngay=2026-11-27")
                .build());

        assertThat(userNotificationRepository.countByUserIdAndReadAtIsNull(userA.getId())).isEqualTo(2);
        assertThat(userNotificationRepository.countByUserIdAndReadAtIsNull(userB.getId())).isEqualTo(1);

        // Đánh dấu 1 dòng của userA đã đọc
        n1.setReadAt(LocalDateTime.now());
        userNotificationRepository.saveAndFlush(n1);

        assertThat(userNotificationRepository.countByUserIdAndReadAtIsNull(userA.getId())).isEqualTo(1);

        // markAllRead cho userA phải cập nhật đúng 1 dòng (n2), không ảnh hưởng userB
        int updated = userNotificationRepository.markAllRead(userA.getId(), LocalDateTime.now());
        entityManager.flush();
        entityManager.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(userNotificationRepository.countByUserIdAndReadAtIsNull(userA.getId())).isEqualTo(0);
        assertThat(userNotificationRepository.countByUserIdAndReadAtIsNull(userB.getId())).isEqualTo(1);
        assertThat(userNotificationRepository.findById(n2.getId())).get()
                .satisfies(found -> assertThat(found.getReadAt()).isNotNull());
    }
}
