package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MeNotificationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    @DisplayName("IT Ticket 4 & D5: JPQL findUpcomingRemindersByUser thực tế cách ly bệnh nhân A và B, kèm người thân")
    void testRealDatabaseNotificationIsolation() throws Exception {
        long suffix = System.currentTimeMillis();
        LocalDate today = LocalDate.now();

        // 1. Tạo 2 tài khoản bệnh nhân độc lập trên CSDL thật
        User userA = userRepository.save(User.builder()
                .username("patient_a_" + suffix)
                .fullName("Bệnh nhân A")
                .phone("09" + String.valueOf(suffix).substring(5))
                .role(Role.PATIENT)
                .passwordHash("$2a$12$DummyHashForIntegrationTest12345678901234567890123456")
                .isActive(true)
                .mustChangePassword(false)
                .build());

        User userB = userRepository.save(User.builder()
                .username("patient_b_" + suffix)
                .fullName("Bệnh nhân B")
                .phone("08" + String.valueOf(suffix).substring(5))
                .role(Role.PATIENT)
                .passwordHash("$2a$12$DummyHashForIntegrationTest12345678901234567890123456")
                .isActive(true)
                .mustChangePassword(false)
                .build());

        // 2. Tạo hồ sơ: A chính chủ + A người thân + B chính chủ
        Patient patientA_Self = patientRepository.save(Patient.builder()
                .user(userA)
                .fullName("Bệnh nhân A (Chính chủ)")
                .phone(userA.getPhone())
                .gender(Gender.MALE)
                .patientCode("PA-" + suffix)
                .isPrimary(true)
                .isActive(true)
                .build());

        Patient patientA_Relative = patientRepository.save(Patient.builder()
                .user(userA)
                .fullName("Bệnh nhân A (Người thân)")
                .phone(userA.getPhone())
                .gender(Gender.FEMALE)
                .relationship("ME")
                .patientCode("PAR-" + suffix)
                .isPrimary(false)
                .isActive(true)
                .build());

        Patient patientB_Self = patientRepository.save(Patient.builder()
                .user(userB)
                .fullName("Bệnh nhân B (Chính chủ)")
                .phone(userB.getPhone())
                .gender(Gender.FEMALE)
                .patientCode("PB-" + suffix)
                .isPrimary(true)
                .isActive(true)
                .build());

        List<TimeSlot> slots = timeSlotRepository.findAll();
        TimeSlot slot1 = slots.get(0);
        TimeSlot slot2 = slots.size() > 1 ? slots.get(1) : slots.get(0);

        // 3. Tạo các phiếu khám cho User A:
        // - aptA1: ngày mai (trong 7 ngày), PENDING -> HỢP LỆ
        Appointment aptA1 = appointmentRepository.save(Appointment.builder()
                .bookingCode("A1-" + suffix)
                .patient(patientA_Self)
                .appointmentDate(today.plusDays(1))
                .timeSlot(slot1)
                .status(AppointmentStatus.PENDING)
                .build());

        // - aptA2: 3 ngày nữa (người thân, trong 7 ngày), CONFIRMED -> HỢP LỆ
        Appointment aptA2 = appointmentRepository.save(Appointment.builder()
                .bookingCode("A2-" + suffix)
                .patient(patientA_Relative)
                .appointmentDate(today.plusDays(3))
                .timeSlot(slot2)
                .status(AppointmentStatus.CONFIRMED)
                .build());

        // - aptA_Far: 10 ngày nữa (ngoài 7 ngày), PENDING -> KHÔNG ĐƯỢC XUẤT HIỆN
        appointmentRepository.save(Appointment.builder()
                .bookingCode("AF-" + suffix)
                .patient(patientA_Self)
                .appointmentDate(today.plusDays(10))
                .timeSlot(slot1)
                .status(AppointmentStatus.PENDING)
                .build());

        // - aptA_Cancelled: ngày mai nhưng đã CANCELLED -> KHÔNG ĐƯỢC XUẤT HIỆN
        appointmentRepository.save(Appointment.builder()
                .bookingCode("AC-" + suffix)
                .patient(patientA_Self)
                .appointmentDate(today.plusDays(1))
                .timeSlot(slot1)
                .status(AppointmentStatus.CANCELLED)
                .build());

        // 4. Tạo phiếu cho User B: ngày mai, CONFIRMED
        Appointment aptB1 = appointmentRepository.save(Appointment.builder()
                .bookingCode("B1-" + suffix)
                .patient(patientB_Self)
                .appointmentDate(today.plusDays(1))
                .timeSlot(slot1)
                .status(AppointmentStatus.CONFIRMED)
                .build());

        // 5. Kiểm tra truy cập khi chưa có Token -> 401 hoặc 403
        mockMvc.perform(get("/api/v1/me/notifications"))
                .andExpect(status().is4xxClientError());

        // 6. User A đăng nhập gọi API -> Chỉ thấy aptA1 và aptA2 (bao gồm cả người thân)
        String tokenA = tokenProvider.generateToken(userA);
        mockMvc.perform(get("/api/v1/me/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.items[0].appointmentId", hasToString(String.valueOf(aptA1.getId()))))
                .andExpect(jsonPath("$.data.items[0].body", containsString("A1-" + suffix)))
                .andExpect(jsonPath("$.data.items[1].appointmentId", hasToString(String.valueOf(aptA2.getId()))))
                .andExpect(jsonPath("$.data.items[1].body", containsString("A2-" + suffix)));

        // 7. User B đăng nhập gọi API -> Chỉ thấy aptB1, TUYỆT ĐỐI không thấy aptA1 hay aptA2
        String tokenB = tokenProvider.generateToken(userB);
        mockMvc.perform(get("/api/v1/me/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].appointmentId", hasToString(String.valueOf(aptB1.getId()))))
                .andExpect(jsonPath("$.data.items[0].body", containsString("B1-" + suffix)));
    }
}
