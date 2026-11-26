package com.chuctupk.controller;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.entity.User;
import com.chuctupk.entity.UserNotification;
import com.chuctupk.entity.enums.NotificationInboxType;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.UserNotificationRepository;
import com.chuctupk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    private User staffA;
    private User staffB;
    private User patientUser;
    private UserNotification notifA;
    private UserNotification notifB;

    @BeforeEach
    void setUp() {
        long suffix = System.nanoTime();
        staffA = createUser("ctrl_staff_a_" + suffix, Role.STAFF);
        staffB = createUser("ctrl_staff_b_" + suffix, Role.STAFF);
        patientUser = createUser("ctrl_patient_" + suffix, Role.PATIENT);

        notifA = userNotificationRepository.save(UserNotification.builder()
                .user(staffA)
                .type(NotificationInboxType.APPOINTMENT_CREATED)
                .title("Lịch khám mới")
                .body("Nguyễn Văn An đặt lịch 08:30 ngày 27/11/2026")
                .link("/noi-bo/lich-trong-ngay?ngay=2026-11-27")
                .build());

        notifB = userNotificationRepository.save(UserNotification.builder()
                .user(staffB)
                .type(NotificationInboxType.APPOINTMENT_CREATED)
                .title("Lịch khám mới")
                .body("Trần Thị Bình đặt lịch 09:00 ngày 27/11/2026")
                .link("/noi-bo/lich-trong-ngay?ngay=2026-11-27")
                .build());
    }

    private User createUser(String username, Role role) {
        long suffix = System.nanoTime();
        return userRepository.save(User.builder()
                .username(username)
                .passwordHash("$2a$12$DummyHashForTest12345678901234567890123456789012")
                .fullName(username)
                .phone("09" + String.valueOf(suffix).substring(String.valueOf(suffix).length() - 8))
                .role(role)
                .isActive(true)
                .mustChangePassword(false)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/staff/notifications?limit=500 -> 422")
    void testLimitOver100Returns422() throws Exception {
        String token = jwtTokenProvider.generateToken(staffA);
        mockMvc.perform(get("/api/v1/staff/notifications")
                        .param("limit", "500")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Số dòng mỗi trang tối đa 100"));
    }

    @Test
    @DisplayName("POST /{id}/read của người khác -> 404")
    void testMarkReadOtherUsersNotificationReturns404() throws Exception {
        String tokenA = jwtTokenProvider.generateToken(staffA);
        mockMvc.perform(post("/api/v1/staff/notifications/{id}/read", notifB.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy thông báo"));
    }

    @Test
    @DisplayName("POST /{id}/read gọi 2 lần -> lần 2 vẫn 200 và read_at không đổi")
    void testMarkReadIdempotent() throws Exception {
        String tokenA = jwtTokenProvider.generateToken(staffA);

        mockMvc.perform(post("/api/v1/staff/notifications/{id}/read", notifA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        LocalDateTime firstReadAt = userNotificationRepository.findById(notifA.getId())
                .orElseThrow()
                .getReadAt();
        assertThat(firstReadAt).isNotNull();

        mockMvc.perform(post("/api/v1/staff/notifications/{id}/read", notifA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        LocalDateTime secondReadAt = userNotificationRepository.findById(notifA.getId())
                .orElseThrow()
                .getReadAt();
        assertThat(secondReadAt).isEqualTo(firstReadAt);
    }

    @Test
    @DisplayName("PATIENT gọi /api/v1/staff/notifications -> 403")
    void testPatientForbidden403() throws Exception {
        String patientToken = jwtTokenProvider.generateToken(patientUser);
        mockMvc.perform(get("/api/v1/staff/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET danh sách, unread-count và POST read-all hoạt động đúng")
    void testListUnreadCountAndReadAll() throws Exception {
        String tokenA = jwtTokenProvider.generateToken(staffA);

        mockMvc.perform(get("/api/v1/staff/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unread").value(1));

        mockMvc.perform(get("/api/v1/staff/notifications")
                        .param("page", "1")
                        .param("limit", "20")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(notifA.getId()))
                .andExpect(jsonPath("$.data.unread").value(1));

        mockMvc.perform(post("/api/v1/staff/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(1));

        mockMvc.perform(get("/api/v1/staff/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unread").value(0));
    }
}
