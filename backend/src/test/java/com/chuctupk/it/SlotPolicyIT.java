package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.dto.request.PatientBookAppointmentRequest;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.repository.UserRepository;
import com.chuctupk.service.ClinicSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SlotPolicyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private ClinicSettingService clinicSettingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User patientUser;
    private Patient patient;
    private String patientToken;
    private TimeSlot outsideSlot;
    private int originalBookingMinHours;

    @BeforeEach
    void setUp() {
        originalBookingMinHours = clinicSettingService.getBookingMinHours();
        String suffix = String.valueOf(System.currentTimeMillis());
        patientUser = userRepository.save(User.builder()
                .username("patient_" + suffix)
                .passwordHash("DummyHashForTest123")
                .fullName("Cụ Nguyễn Văn A")
                .phone("09" + suffix.substring(Math.max(0, suffix.length() - 8)))
                .role(Role.PATIENT)
                .isActive(true)
                .mustChangePassword(false)
                .build());

        patient = patientRepository.save(Patient.builder()
                .user(patientUser)
                .patientCode("BN" + suffix.substring(Math.max(0, suffix.length() - 6)))
                .fullName("Cụ Nguyễn Văn A")
                .phone(patientUser.getPhone())
                .gender(Gender.MALE)
                .isPrimary(true)
                .build());

        patientToken = tokenProvider.generateToken(patientUser);

        // Tạo 1 slot ngoài giờ tiếp nhận (18:00 - 19:00, trong khi reception_close là 16:30)
        outsideSlot = timeSlotRepository.save(TimeSlot.builder()
                .slotLabel("18:00 - 19:00")
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .sortOrder(99)
                .isActive(true)
                .period(com.chuctupk.entity.enums.Period.AFTERNOON)
                .build());
    }

    @AfterEach
    void tearDown() {
        clinicSettingService.setSettingValue("booking_min_hours", String.valueOf(originalBookingMinHours));
        clinicSettingService.invalidateCache();
        if (outsideSlot != null && outsideSlot.getId() != null) {
            timeSlotRepository.delete(outsideSlot);
        }
        if (patient != null && patient.getId() != null) {
            patientRepository.delete(patient);
        }
        if (patientUser != null && patientUser.getId() != null) {
            userRepository.delete(patientUser);
        }
    }

    @Test
    @DisplayName("A1: POST /api/v1/me/appointments với slot OUTSIDE_RECEPTION -> 422")
    void testBookAppointment_OutsideReception_Returns422() throws Exception {
        PatientBookAppointmentRequest req = PatientBookAppointmentRequest.builder()
                .patientId(patient.getId())
                .slotDate(LocalDate.now().plusDays(2).toString())
                .slotStart("18:00")
                .serviceType("KHAM_BENH")
                .build();

        mockMvc.perform(post("/api/v1/me/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message", containsString("Phòng khám chỉ tiếp nhận")));
    }

    @Test
    @DisplayName("A1: POST /api/v1/me/appointments với slot TOO_LATE -> 422")
    void testBookAppointment_TooLate_Returns422() throws Exception {
        // Đặt booking_min_hours = 24 để đảm bảo bất kỳ khung giờ nào trong ngày hôm nay cũng rơi vào TOO_LATE
        clinicSettingService.setSettingValue("booking_min_hours", "24");
        clinicSettingService.invalidateCache();

        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        TimeSlot earlySlot = slots.get(0); // 08:00 - 09:00

        PatientBookAppointmentRequest req = PatientBookAppointmentRequest.builder()
                .patientId(patient.getId())
                .slotDate(LocalDate.now().toString())
                .slotStart(earlySlot.getStartTime().toString().substring(0, 5))
                .serviceType("KHAM_BENH")
                .build();

        mockMvc.perform(post("/api/v1/me/appointments")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message", containsString("Khung giờ này đã bắt đầu")));
    }
}
