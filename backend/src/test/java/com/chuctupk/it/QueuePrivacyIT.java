package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.AppointmentSeat;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.AppointmentSeatRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QueuePrivacyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentSeatRepository appointmentSeatRepository;

    @Autowired
    private com.chuctupk.repository.CheckinRepository checkinRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private UserRepository userRepository;

    /** Tài khoản seed phải đổi mật khẩu lần đầu; test này cần quyền làm việc bình thường. */
    @BeforeEach
    void clearSeedPasswordFlag() {
        userRepository.findByUsername("letan").ifPresent(u -> {
            u.setMustChangePassword(false);
            userRepository.save(u);
        });
    }

    @Test
    @DisplayName("P0-3: GET /api/v1/queue/today đã xoá hoàn toàn -> 404")
    void testLegacyQueueToday_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/queue/today"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("P0-3: GET /api/v1/staff/queue không token che tên bệnh nhân, không lộ phone/address/medicalHistory; có token lễ tân trả đầy đủ")
    void testQueuePrivacy_AnonymousMaskedAndStaffFull() throws Exception {
        LocalDate today = LocalDate.now();

        // Chuẩn bị bệnh nhân mẫu có thông tin cá nhân
        Patient patient = patientRepository.save(Patient.builder()
                .fullName("Nguyễn Văn An")
                .phone("0988776655")
                .yearOfBirth(1960)
                .gender(Gender.MALE)
                .address("123 Điện Biên Phủ, Bình Thạnh")
                .medicalHistory("Tăng huyết áp vô căn")
                .patientCode("BNTEST01")
                .isActive(true)
                .build());

        TimeSlot slot = timeSlotRepository.findAll().stream().findFirst().orElseThrow();

        Appointment appt = appointmentRepository.save(Appointment.builder()
                .bookingCode("PKCT-TEST-0001")
                .patient(patient)
                .appointmentDate(today)
                .timeSlot(slot)
                .status(AppointmentStatus.CONFIRMED)
                .checkInAt(LocalDateTime.now())
                .build());

        AppointmentSeat seat = appointmentSeatRepository.save(AppointmentSeat.builder()
                .appointment(appt)
                .appointmentDate(today)
                .timeSlot(slot)
                .seatNo(1)
                .build());

        com.chuctupk.entity.Checkin checkin = checkinRepository.save(com.chuctupk.entity.Checkin.builder()
                .appointment(appt)
                .seatNo(1)
                .queueNo(1)
                .status(com.chuctupk.entity.enums.CheckinStatus.IN_ROOM)
                .priorityGroup(com.chuctupk.entity.enums.PriorityGroup.NORMAL)
                .checkInTime(LocalDateTime.now())
                .enteredAt(LocalDateTime.now())
                .build());

        try {
            // 1. Người gọi ẩn danh (màn hình TV công khai)
            mockMvc.perform(get("/api/v1/staff/queue"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // Trong danh sách seats, patient fullName phải bị che và các trường nhạy cảm KHÔNG xuất hiện
                    .andExpect(jsonPath("$.data.seats[0].patient.phone").doesNotExist())
                    .andExpect(jsonPath("$.data.seats[0].patient.address").doesNotExist())
                    .andExpect(jsonPath("$.data.seats[0].patient.medicalHistory").doesNotExist())
                    .andExpect(jsonPath("$.data.seats[0].patient.fullName", containsString("Nguyễn V. An")));

            // 2. Nhân viên lễ tân có token (letan)
            com.chuctupk.entity.User staffUser = com.chuctupk.entity.User.builder()
                    .id(2L).username("letan").role(com.chuctupk.entity.enums.Role.STAFF).build();
            String staffToken = tokenProvider.generateToken(staffUser);

            mockMvc.perform(get("/api/v1/staff/queue")
                            .header("Authorization", "Bearer " + staffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.seats[0].patient.fullName").value("Nguyễn Văn An"));

        } finally {
            checkinRepository.delete(checkin);
            appointmentSeatRepository.delete(seat);
            appointmentRepository.delete(appt);
            patientRepository.delete(patient);
        }
    }
}
