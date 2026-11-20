package com.chuctupk.controller;

import com.chuctupk.dto.response.*;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.repository.*;
import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.DoctorAssignmentService;
import com.chuctupk.service.StaffService;
import com.chuctupk.service.impl.StaffServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Sprint 3 - Phân quyền xem hồ sơ bệnh án theo Role (Ticket 1 & 2)")
class StaffRecordAccessTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AppointmentRepository appointmentRepository;
    private MedicalRecordRepository medicalRecordRepository;
    private CheckinRepository checkinRepository;
    private DoctorRepository doctorRepository;
    private DoctorScheduleRepository doctorScheduleRepository;
    private TimeSlotRepository timeSlotRepository;
    private DoctorAssignmentService doctorAssignmentService;
    private ClinicSettingService clinicSettingService;
    private NotificationLogRepository notificationLogRepository;

    private User staffUser;
    private User doctorUser;
    private User adminUser;
    private Appointment testAppointment;
    private MedicalRecord testRecord;

    @BeforeEach
    void setUp() {
        appointmentRepository = Mockito.mock(AppointmentRepository.class);
        medicalRecordRepository = Mockito.mock(MedicalRecordRepository.class);
        checkinRepository = Mockito.mock(CheckinRepository.class);
        doctorRepository = Mockito.mock(DoctorRepository.class);
        doctorScheduleRepository = Mockito.mock(DoctorScheduleRepository.class);
        timeSlotRepository = Mockito.mock(TimeSlotRepository.class);
        doctorAssignmentService = Mockito.mock(DoctorAssignmentService.class);
        clinicSettingService = Mockito.mock(ClinicSettingService.class);
        notificationLogRepository = Mockito.mock(NotificationLogRepository.class);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        StaffService staffService = new StaffServiceImpl(
                appointmentRepository,
                checkinRepository,
                null,
                null,
                medicalRecordRepository,
                null,
                clinicSettingService,
                null,
                doctorAssignmentService,
                doctorRepository,
                null,
                null,
                null,
                null,
                fixedClock,
                notificationLogRepository,
                null
        );

        StaffController staffController = new StaffController(staffService, doctorAssignmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(staffController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        staffUser = User.builder()
                .id(1L)
                .username("letan")
                .fullName("Tiếp tân Nguyễn Thị Mai")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        doctorUser = User.builder()
                .id(2L)
                .username("bacsi")
                .fullName("Bác sĩ Trần Văn Bình")
                .role(Role.DOCTOR)
                .isActive(true)
                .build();

        adminUser = User.builder()
                .id(3L)
                .username("admin")
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        Patient patient = Patient.builder()
                .id(10L)
                .patientCode("CT-BN-26-0010")
                .fullName("Bác Lê Văn Tám")
                .phone("0987654321")
                .gender(Gender.MALE)
                .yearOfBirth(1955)
                .medicalHistory("Tiền sử tăng huyết áp 5 năm")
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 11, 10, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 10, 8, 0))
                .build();

        Doctor doctor = Doctor.builder()
                .id(20L)
                .user(doctorUser)
                .fullName("Bác sĩ Trần Văn Bình")
                .specialty("Y học cổ truyền")
                .build();

        testAppointment = Appointment.builder()
                .id(100L)
                .bookingCode("CT-PK-26-2011-001")
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(LocalDate.of(2026, 11, 20))
                .status(AppointmentStatus.COMPLETED)
                .createdAt(LocalDateTime.of(2026, 11, 20, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 20, 9, 30))
                .build();

        testRecord = MedicalRecord.builder()
                .id(200L)
                .appointment(testAppointment)
                .patient(patient)
                .doctor(doctor)
                .doctorName("Bác sĩ Trần Văn Bình")
                .visitDate(LocalDate.of(2026, 11, 20))
                .diagnosis("Thoát vị đĩa đệm cột sống thắt lưng L4-L5 thể phong hàn thấp")
                .prescription("Thang Độc hoạt Ký sinh thang x 7 thang, sắc uống ngày 1 thang chia 2 lần")
                .advice("Hạn chế mang vác vật nặng, giữ ấm vùng thắt lưng khi trời trở lạnh")
                .pulse("80")
                .bloodPressure("130/85")
                .pulseBp("80|130/85")
                .treatment("Châm cứu các huyệt Thận du, Đại trường du; Xoa bóp bấm huyệt cột sống lưng")
                .treatmentAcupuncture("Châm cứu các huyệt Thận du, Đại trường du")
                .treatmentAcupressure("Xoa bóp bấm huyệt cột sống lưng")
                .followUpDate(LocalDate.of(2026, 11, 27))
                .createdAt(LocalDateTime.of(2026, 11, 20, 9, 30))
                .updatedAt(LocalDateTime.of(2026, 11, 20, 9, 30))
                .build();

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(testAppointment));
        when(medicalRecordRepository.findByAppointmentId(100L)).thenReturn(Optional.of(testRecord));
        when(checkinRepository.findByAppointmentId(100L)).thenReturn(Optional.empty());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Test 1: STAFF xem chi tiết phiếu -> JSON KHÔNG có diagnosis, prescription, advice, medicalHistory, treatment")
    void staffViewAppointmentDetail_ShouldReturnSummaryOnlyWithoutMedicalFields() throws Exception {
        authenticate(staffUser);

        MvcResult result = mockMvc.perform(get("/api/v1/staff/appointments/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appointment.ticketCode").value("CT-PK-26-2011-001"))
                .andExpect(jsonPath("$.data.record.id").value("200"))
                .andExpect(jsonPath("$.data.record.visitDate").value("2026-11-20"))
                .andExpect(jsonPath("$.data.record.doctorName").value("Bác sĩ Trần Văn Bình"))
                .andExpect(jsonPath("$.data.record.followUpDate").value("2026-11-27"))
                .andExpect(jsonPath("$.data.record.hasPrescription").value(true))
                // Bắt buộc KHÔNG được xuất hiện bất kỳ trường y tế nhạy cảm nào
                .andExpect(jsonPath("$.data.record.diagnosis").doesNotExist())
                .andExpect(jsonPath("$.data.record.prescription").doesNotExist())
                .andExpect(jsonPath("$.data.record.advice").doesNotExist())
                .andExpect(jsonPath("$.data.record.treatment").doesNotExist())
                .andExpect(jsonPath("$.data.record.treatmentNotes").doesNotExist())
                .andExpect(jsonPath("$.data.record.medicalHistory").doesNotExist())
                .andExpect(jsonPath("$.data.record.pulse").doesNotExist())
                .andExpect(jsonPath("$.data.record.bloodPressure").doesNotExist())
                .andExpect(jsonPath("$.data.record.pulseBp").doesNotExist())
                .andReturn();

        // Kiểm tra trực tiếp trên chuỗi JSON thô
        String jsonString = result.getResponse().getContentAsString();
        JsonNode rootNode = objectMapper.readTree(jsonString);
        JsonNode recordNode = rootNode.path("data").path("record");

        assertThat(recordNode.has("diagnosis")).isFalse();
        assertThat(recordNode.has("prescription")).isFalse();
        assertThat(recordNode.has("advice")).isFalse();
        assertThat(recordNode.has("treatment")).isFalse();
        assertThat(recordNode.has("medicalHistory")).isFalse();

        // Chỉ chứa đúng các trường tóm tắt hành chính
        assertThat(recordNode.has("id")).isTrue();
        assertThat(recordNode.has("visitDate")).isTrue();
        assertThat(recordNode.has("doctorName")).isTrue();
        assertThat(recordNode.has("followUpDate")).isTrue();
        assertThat(recordNode.has("hasPrescription")).isTrue();
    }

    @Test
    @DisplayName("Test 1b: DOCTOR xem chi tiết phiếu -> JSON có đầy đủ chẩn đoán, đơn thuốc, lời dặn")
    void doctorViewAppointmentDetail_ShouldReturnFullMedicalRecord() throws Exception {
        authenticate(doctorUser);

        MvcResult result = mockMvc.perform(get("/api/v1/staff/appointments/100")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.record.id").value("200"))
                .andExpect(jsonPath("$.data.record.diagnosis").value("Thoát vị đĩa đệm cột sống thắt lưng L4-L5 thể phong hàn thấp"))
                .andExpect(jsonPath("$.data.record.prescription").value("Thang Độc hoạt Ký sinh thang x 7 thang, sắc uống ngày 1 thang chia 2 lần"))
                .andExpect(jsonPath("$.data.record.advice").value("Hạn chế mang vác vật nặng, giữ ấm vùng thắt lưng khi trời trở lạnh"))
                .andExpect(jsonPath("$.data.record.treatment").value("Châm cứu các huyệt Thận du, Đại trường du; Xoa bóp bấm huyệt cột sống lưng"))
                .andExpect(jsonPath("$.data.record.pulse").value("80"))
                .andExpect(jsonPath("$.data.record.bloodPressure").value("130/85"))
                .andReturn();

        String jsonString = result.getResponse().getContentAsString();
        JsonNode rootNode = objectMapper.readTree(jsonString);
        JsonNode recordNode = rootNode.path("data").path("record");

        assertThat(recordNode.has("diagnosis")).isTrue();
        assertThat(recordNode.has("prescription")).isTrue();
        assertThat(recordNode.has("advice")).isTrue();
    }

    @Test
    @DisplayName("Nghiệm thu Ticket 2: Cùng 1 endpoint, 2 role khác nhau -> 2 payload cấu trúc khác nhau")
    void compareStaffAndDoctorPayloads_ShouldHaveDifferentKeySets() throws Exception {
        // 1. STAFF payload
        authenticate(staffUser);
        MvcResult staffRes = mockMvc.perform(get("/api/v1/staff/appointments/100")).andReturn();
        JsonNode staffRecord = objectMapper.readTree(staffRes.getResponse().getContentAsString())
                .path("data").path("record");

        // 2. DOCTOR payload
        authenticate(doctorUser);
        MvcResult doctorRes = mockMvc.perform(get("/api/v1/staff/appointments/100")).andReturn();
        JsonNode doctorRecord = objectMapper.readTree(doctorRes.getResponse().getContentAsString())
                .path("data").path("record");

        // So sánh: STAFF record có ít trường hơn nhiều so với DOCTOR record
        assertThat(staffRecord.size()).isEqualTo(5);
        assertThat(doctorRecord.size()).isGreaterThanOrEqualTo(10);

        assertThat(staffRecord.has("diagnosis")).isFalse();
        assertThat(doctorRecord.has("diagnosis")).isTrue();

        assertThat(staffRecord.has("prescription")).isFalse();
        assertThat(doctorRecord.has("prescription")).isTrue();

        assertThat(staffRecord.has("advice")).isFalse();
        assertThat(doctorRecord.has("advice")).isTrue();
    }
}
