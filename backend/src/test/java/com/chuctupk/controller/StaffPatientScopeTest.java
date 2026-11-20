package com.chuctupk.controller;

import com.chuctupk.dto.request.BreakGlassRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.repository.*;
import com.chuctupk.service.SequenceGeneratorService;
import com.chuctupk.service.StaffPatientService;
import com.chuctupk.service.impl.StaffPatientServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Sprint 3 - Giới hạn phạm vi bệnh nhân của Bác sĩ & Break-glass (Ticket 3, 4, 5)")
class StaffPatientScopeTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private PatientRepository patientRepository;
    private AppointmentRepository appointmentRepository;
    private MedicalRecordRepository medicalRecordRepository;
    private RecordAccessLogRepository recordAccessLogRepository;
    private UserRepository userRepository;
    private SequenceGeneratorService sequenceGeneratorService;

    private User doctorA;
    private User doctorB;
    private User staffUser;
    private User adminUser;

    private Patient patientOfDoctorA;
    private Patient patientOfDoctorB;

    private Appointment apptPatientA;
    private MedicalRecord recordPatientA;

    @BeforeEach
    void setUp() {
        patientRepository = Mockito.mock(PatientRepository.class);
        appointmentRepository = Mockito.mock(AppointmentRepository.class);
        medicalRecordRepository = Mockito.mock(MedicalRecordRepository.class);
        recordAccessLogRepository = Mockito.mock(RecordAccessLogRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        sequenceGeneratorService = Mockito.mock(SequenceGeneratorService.class);

        Clock fixedClock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        StaffPatientService staffPatientService = new StaffPatientServiceImpl(
                patientRepository,
                appointmentRepository,
                medicalRecordRepository,
                recordAccessLogRepository,
                userRepository,
                sequenceGeneratorService,
                fixedClock
        );

        StaffPatientController controller = new StaffPatientController(staffPatientService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        doctorA = User.builder()
                .id(10L)
                .username("bacsiA")
                .fullName("BS. Trần Văn Bình")
                .role(Role.DOCTOR)
                .isActive(true)
                .build();

        doctorB = User.builder()
                .id(20L)
                .username("bacsiB")
                .fullName("BS. Lê Thị Hoa")
                .role(Role.DOCTOR)
                .isActive(true)
                .build();

        staffUser = User.builder()
                .id(30L)
                .username("letan")
                .fullName("Tiếp tân Mai")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        adminUser = User.builder()
                .id(40L)
                .username("admin")
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        patientOfDoctorA = Patient.builder()
                .id(1L)
                .patientCode("CT-BN-26-0001")
                .fullName("Nguyễn Văn An")
                .phone("0901234567")
                .yearOfBirth(1960)
                .gender(Gender.MALE)
                .medicalHistory("Dị ứng phấn hoa")
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 11, 1, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 1, 8, 0))
                .build();

        patientOfDoctorB = Patient.builder()
                .id(2L)
                .patientCode("CT-BN-26-0002")
                .fullName("Trần Thị Bích")
                .phone("0907654321")
                .yearOfBirth(1965)
                .gender(Gender.FEMALE)
                .medicalHistory("Tiền sử tiểu đường tuýp 2")
                .isActive(true)
                .createdAt(LocalDateTime.of(2026, 11, 2, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 2, 8, 0))
                .build();

        Doctor docEntityA = Doctor.builder()
                .id(100L)
                .user(doctorA)
                .fullName("BS. Trần Văn Bình")
                .build();

        apptPatientA = Appointment.builder()
                .id(101L)
                .bookingCode("CT-PK-26-01")
                .patient(patientOfDoctorA)
                .doctor(docEntityA)
                .appointmentDate(LocalDate.of(2026, 11, 15))
                .status(AppointmentStatus.COMPLETED)
                .createdAt(LocalDateTime.of(2026, 11, 15, 8, 0))
                .build();

        recordPatientA = MedicalRecord.builder()
                .id(201L)
                .appointment(apptPatientA)
                .patient(patientOfDoctorA)
                .doctor(docEntityA)
                .doctorName("BS. Trần Văn Bình")
                .visitDate(LocalDate.of(2026, 11, 15))
                .diagnosis("Thoát vị đĩa đệm cột sống")
                .prescription("Độc hoạt Ký sinh thang 7 thang")
                .advice("Tập vật lý trị liệu nhẹ nhàng")
                .createdAt(LocalDateTime.of(2026, 11, 15, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 11, 15, 9, 0))
                .build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patientOfDoctorA));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patientOfDoctorB));
        when(appointmentRepository.findByPatientId(1L)).thenReturn(List.of(apptPatientA));
        when(medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(recordPatientA));
        when(appointmentRepository.findByPatientId(2L)).thenReturn(Collections.emptyList());
        when(medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(2L)).thenReturn(Collections.emptyList());
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Test 2: DOCTOR xem bệnh nhân của mình -> 200 đầy đủ thông tin y tế")
    void doctorViewOwnPatient_ShouldReturn200AndFullMedicalDetails() throws Exception {
        authenticate(doctorA);
        when(appointmentRepository.existsByPatientIdAndDoctorUserId(1L, doctorA.getId())).thenReturn(true);

        mockMvc.perform(get("/api/v1/staff/patients/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patient.id").value("1"))
                .andExpect(jsonPath("$.data.patient.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.records[0].diagnosis").value("Thoát vị đĩa đệm cột sống"))
                .andExpect(jsonPath("$.data.records[0].prescription").value("Độc hoạt Ký sinh thang 7 thang"))
                .andExpect(jsonPath("$.data.records[0].advice").value("Tập vật lý trị liệu nhẹ nhàng"));
    }

    @Test
    @DisplayName("Test 3: DOCTOR xem bệnh nhân của người khác, không break-glass -> 403 Forbidden")
    void doctorViewOtherDoctorPatientWithoutBreakGlass_ShouldReturn403() throws Exception {
        authenticate(doctorA);
        // Bác sĩ A chưa từng có phiếu nào với bệnh nhân 2
        when(appointmentRepository.existsByPatientIdAndDoctorUserId(2L, doctorA.getId())).thenReturn(false);
        // Bác sĩ A cũng chưa break-glass bệnh nhân 2
        when(recordAccessLogRepository.existsByUserIdAndPatientIdAndScope(doctorA.getId(), 2L, "BREAK_GLASS")).thenReturn(false);

        mockMvc.perform(get("/api/v1/staff/patients/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.message").value("Bạn chưa từng phụ trách bệnh nhân này"))
                .andExpect(jsonPath("$.data.canBreakGlass").value(true));
    }

    @Test
    @DisplayName("Test 4: Break-glass lý do ngắn (< 10 ký tự) -> 422 Unprocessable Entity")
    void breakGlassWithShortReason_ShouldReturn422() throws Exception {
        authenticate(doctorA);
        when(appointmentRepository.existsByPatientIdAndDoctorUserId(2L, doctorA.getId())).thenReturn(false);

        BreakGlassRequest request = new BreakGlassRequest("Khẩn cấp"); // 8 ký tự

        mockMvc.perform(post("/api/v1/staff/patients/2/break-glass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.statusCode").value(422))
                .andExpect(jsonPath("$.message").value("Vui lòng ghi rõ lý do (ít nhất 10 ký tự)"));

        verify(recordAccessLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 5: Break-glass hợp lệ (>= 10 ký tự) -> 200 + ghi 1 dòng record_access_log")
    void breakGlassWithValidReason_ShouldReturn200AndSaveLog() throws Exception {
        authenticate(doctorA);
        when(appointmentRepository.existsByPatientIdAndDoctorUserId(2L, doctorA.getId())).thenReturn(false);

        BreakGlassRequest request = new BreakGlassRequest("Bệnh nhân ngất xỉu tại sảnh, cần xem tiền sử khẩn cấp");

        mockMvc.perform(post("/api/v1/staff/patients/2/break-glass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patient.id").value("2"))
                .andExpect(jsonPath("$.data.patient.fullName").value("Trần Thị Bích"));

        ArgumentCaptor<RecordAccessLog> captor = ArgumentCaptor.forClass(RecordAccessLog.class);
        verify(recordAccessLogRepository, times(1)).save(captor.capture());

        RecordAccessLog savedLog = captor.getValue();
        assertThat(savedLog.getUser().getId()).isEqualTo(doctorA.getId());
        assertThat(savedLog.getPatientId()).isEqualTo(2L);
        assertThat(savedLog.getScope()).isEqualTo("BREAK_GLASS");
        assertThat(savedLog.getReason()).isEqualTo("Bệnh nhân ngất xỉu tại sảnh, cần xem tiền sử khẩn cấp");
        assertThat(savedLog.getRecordId()).isNull();
        // Kiểm tra log không chứa dữ liệu y tế nhạy cảm
        assertThat(savedLog.getReason()).doesNotContain("Thoát vị");
    }

    @Test
    @DisplayName("Test 6: Danh sách bệnh nhân của DOCTOR chỉ chứa bệnh nhân có phiếu với mình (query DB, không findAll)")
    void doctorListPatients_ShouldQueryDbOnlyForDoctorPatients() throws Exception {
        authenticate(doctorA);

        // Giả lập DB trả về chỉ bệnh nhân 1 cho Bác sĩ A
        when(patientRepository.findDistinctPatientsByDoctorUserId(doctorA.getId()))
                .thenReturn(List.of(patientOfDoctorA));

        mockMvc.perform(get("/api/v1/staff/patients")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value("1"))
                .andExpect(jsonPath("$.data.items[0].fullName").value("Nguyễn Văn An"));

        // Tuyệt đối không được gọi findAll() trong RAM khi DOCTOR truy vấn
        verify(patientRepository, times(1)).findDistinctPatientsByDoctorUserId(doctorA.getId());
        verify(patientRepository, never()).findAll();
    }

    @Test
    @DisplayName("Test 6b: STAFF xem danh sách bệnh nhân -> gọi findAll() bình thường")
    void staffListPatients_ShouldCallFindAll() throws Exception {
        authenticate(staffUser);
        when(patientRepository.findAll()).thenReturn(List.of(patientOfDoctorA, patientOfDoctorB));

        mockMvc.perform(get("/api/v1/staff/patients")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2));

        verify(patientRepository, times(1)).findAll();
        verify(patientRepository, never()).findDistinctPatientsByDoctorUserId(any());
    }

    @Test
    @DisplayName("Nghiệm thu STAFF xem chi tiết bệnh nhân: records là RecordSummaryDto, không có diagnosis/prescription")
    void staffViewPatientDetail_ShouldReturnSummaryRecordsWithoutMedicalHistory() throws Exception {
        authenticate(staffUser);

        MvcResult result = mockMvc.perform(get("/api/v1/staff/patients/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.patient.id").value("1"))
                // Lễ tân không thấy medicalHistory trong patient DTO
                .andExpect(jsonPath("$.data.patient.medicalHistory").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].id").value("201"))
                .andExpect(jsonPath("$.data.records[0].doctorName").value("BS. Trần Văn Bình"))
                .andExpect(jsonPath("$.data.records[0].hasPrescription").value(true))
                // Không có trường y tế
                .andExpect(jsonPath("$.data.records[0].diagnosis").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].prescription").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].advice").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].treatment").doesNotExist())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode record0 = root.path("data").path("records").get(0);
        assertThat(record0.has("diagnosis")).isFalse();
        assertThat(record0.has("prescription")).isFalse();
        assertThat(record0.has("advice")).isFalse();
    }
}
