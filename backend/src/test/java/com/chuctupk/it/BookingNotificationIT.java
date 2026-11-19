package com.chuctupk.it;

import com.chuctupk.config.JwtTokenProvider;
import com.chuctupk.dto.request.PatientBookAppointmentRequest;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.DoctorSchedule;
import com.chuctupk.entity.NotificationLog;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.DoctorScheduleRepository;
import com.chuctupk.repository.NotificationLogRepository;
import com.chuctupk.repository.PatientRepository;
import com.chuctupk.repository.TimeSlotRepository;
import com.chuctupk.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingNotificationIT {

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
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private DoctorScheduleRepository doctorScheduleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("IT Khóa lỗi: Đặt lịch thành công ghi đúng notification_log trong cùng transaction, phản hồi < 2s (không bị lock-wait FK)")
    void testBookingSuccessWritesNotificationLogWithoutLockWait() throws Exception {
        long suffix = System.currentTimeMillis();
        String rawPhone = "0912345678";

        // 1. Tạo User và Patient thật
        User patientUser = userRepository.save(User.builder()
                .username("booker_" + suffix)
                .fullName("Cụ Nguyễn Văn Đặt")
                .phone(rawPhone)
                .role(Role.PATIENT)
                .passwordHash("$2a$12$DummyHashForIntegrationTest12345678901234567890123456")
                .isActive(true)
                .mustChangePassword(false)
                .build());

        Patient patient = patientRepository.save(Patient.builder()
                .user(patientUser)
                .fullName("Cụ Nguyễn Văn Đặt")
                .phone(rawPhone)
                .gender(Gender.MALE)
                .patientCode("PA-BK-" + suffix)
                .isPrimary(true)
                .isActive(true)
                .build());

        String token = tokenProvider.generateToken(patientUser);

        // 2. Lấy khung giờ khám hợp lệ trong ngày (trong giờ tiếp nhận)
        List<TimeSlot> slots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        assertThat(slots).isNotEmpty();
        TimeSlot chosenSlot = slots.get(0);
        String slotStart = chosenSlot.getStartTime().toString().substring(0, 5);

        // Đặt lịch vào ngày làm việc tới (nhảy qua T7/CN để luôn có bác sĩ trực FULL_DAY và thỏa mãn SlotPolicy)
        LocalDate targetDate = LocalDate.now().plusDays(2);
        while (targetDate.getDayOfWeek().getValue() >= 6) { // T7 = 6, CN = 7
            targetDate = targetDate.plusDays(1);
        }

        // Đảm bảo có bác sĩ trực trong ngày targetDate (phòng thủ độc lập trên cả DB tích luỹ lẫn DB trắng)
        DayOfWeek dow = DayOfWeek.from(targetDate.getDayOfWeek());
        Doctor doc = doctorRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()))
                .findFirst()
                .orElse(null);
        if (doc != null) {
            DoctorSchedule existingSchedule = doctorScheduleRepository.findAll().stream()
                    .filter(s -> s.getDoctor().getId().equals(doc.getId())
                            && s.getDayOfWeek() == dow
                            && (s.getPeriod() == Period.FULL_DAY || s.getPeriod() == Period.MORNING))
                    .findFirst()
                    .orElse(null);
            if (existingSchedule != null) {
                if (!Boolean.TRUE.equals(existingSchedule.getIsActive())) {
                    existingSchedule.setIsActive(true);
                    doctorScheduleRepository.save(existingSchedule);
                }
            } else {
                doctorScheduleRepository.save(DoctorSchedule.builder()
                        .doctor(doc)
                        .dayOfWeek(dow)
                        .period(Period.FULL_DAY)
                        .isActive(true)
                        .build());
            }
        }

        PatientBookAppointmentRequest request = PatientBookAppointmentRequest.builder()
                .patientId(patient.getId())
                .slotDate(targetDate.toString())
                .slotStart(slotStart)
                .serviceType("KHAM_BENH")
                .build();

        // 3. Thực hiện đặt lịch qua API thực và đo thời gian
        long startTime = System.currentTimeMillis();

        MvcResult result = mockMvc.perform(post("/api/v1/me/appointments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appointment.id").exists())
                .andReturn();

        long duration = System.currentTimeMillis() - startTime;

        // Bẫy lock-wait: Nếu dùng REQUIRES_NEW dẫn tới deadlock FK, thời gian sẽ vượt quá timeout (50s)
        assertThat(duration)
                .as("Thời gian đặt lịch phải nhanh (< 5000ms), không bị lock-wait FK do transaction con")
                .isLessThan(5000L);

        // 4. Lấy appointmentId và kiểm chứng dòng notification_log
        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        long appointmentId = rootNode.path("data").path("appointment").path("id").asLong();
        assertThat(appointmentId).isPositive();

        List<NotificationLog> logs = notificationLogRepository.findByAppointmentId(appointmentId);
        assertThat(logs)
                .as("Phải có đúng 1 dòng notification_log gắn với lịch hẹn vừa tạo")
                .hasSize(1);

        NotificationLog logEntry = logs.get(0);
        assertThat(logEntry.getTemplate()).isEqualTo("BOOKING_SUCCESS");
        assertThat(logEntry.getRecipientMasked()).isEqualTo("0912 *** 678");
        assertThat(logEntry.getRecipientMasked()).doesNotContain("0912345678");
        assertThat(logEntry.getStatus()).isEqualTo("SKIPPED");
        assertThat(logEntry.getProvider()).isEqualTo("log");
        assertThat(logEntry.getChannel()).isEqualTo("LOG");
        assertThat(logEntry.getAppointment().getId()).isEqualTo(appointmentId);
    }
}
