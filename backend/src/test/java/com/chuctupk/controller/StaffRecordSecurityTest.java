package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateRecordRequest;
import com.chuctupk.dto.request.UpdateRecordRequest;
import com.chuctupk.entity.*;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.repository.*;
import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.StaffService;
import com.chuctupk.service.impl.StaffRecordServiceImpl;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaffRecordSecurityTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MedicalRecordRepository medicalRecordRepository;
    private MedicalRecordEditRepository medicalRecordEditRepository;
    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private DoctorRepository doctorRepository;
    private CheckinRepository checkinRepository;
    private ClinicSettingService clinicSettingService;
    private StaffService staffService;
    private Clock fixedClock;

    private User doctorUserA;
    private User doctorUserB;
    private User staffUser;
    private User adminUser;

    private Doctor doctorA;
    private Doctor doctorB;
    private Patient samplePatient;
    private Appointment appointmentA;
    private MedicalRecord medicalRecordA;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        medicalRecordRepository = Mockito.mock(MedicalRecordRepository.class);
        medicalRecordEditRepository = Mockito.mock(MedicalRecordEditRepository.class);
        appointmentRepository = Mockito.mock(AppointmentRepository.class);
        patientRepository = Mockito.mock(PatientRepository.class);
        doctorRepository = Mockito.mock(DoctorRepository.class);
        checkinRepository = Mockito.mock(CheckinRepository.class);
        clinicSettingService = Mockito.mock(ClinicSettingService.class);
        staffService = Mockito.mock(StaffService.class);

        StaffRecordServiceImpl service = new StaffRecordServiceImpl(
                medicalRecordRepository,
                medicalRecordEditRepository,
                appointmentRepository,
                patientRepository,
                doctorRepository,
                checkinRepository,
                clinicSettingService,
                fixedClock,
                staffService
        );

        StaffRecordController controller = new StaffRecordController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        doctorUserA = User.builder().id(101L).username("bacsi").fullName("BS. Nguyễn Văn Minh").role(Role.DOCTOR).isActive(true).build();
        doctorUserB = User.builder().id(102L).username("bacsi2").fullName("BS. Lê Thị Hoa").role(Role.DOCTOR).isActive(true).build();
        staffUser = User.builder().id(201L).username("letan").fullName("Tiếp tân Lan").role(Role.STAFF).isActive(true).build();
        adminUser = User.builder().id(301L).username("admin").fullName("Quản trị viên").role(Role.ADMIN).isActive(true).build();

        doctorA = Doctor.builder().id(1L).user(doctorUserA).fullName("BS. Nguyễn Văn Minh").isActive(true).build();
        doctorB = Doctor.builder().id(2L).user(doctorUserB).fullName("BS. Lê Thị Hoa").isActive(true).build();

        samplePatient = Patient.builder().id(10L).fullName("Bác Lê Văn Tám").phone("0987654321").isActive(true).build();

        appointmentA = Appointment.builder()
                .id(100L)
                .bookingCode("CT-PK-26-1811-001")
                .patient(samplePatient)
                .doctor(doctorA)
                .status(AppointmentStatus.IN_PROGRESS)
                .appointmentDate(LocalDate.of(2026, 11, 18))
                .build();

        medicalRecordA = MedicalRecord.builder()
                .id(50L)
                .appointment(appointmentA)
                .doctor(doctorA)
                .patient(samplePatient)
                .diagnosis("Đau nhức thoái hóa khớp gối")
                .createdAt(LocalDateTime.now(fixedClock).minusHours(1))
                .build();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Ticket 7 Test 1: STAFF gọi POST /staff/appointments/{id}/record -> 403 Forbidden")
    void testCreateRecord_StaffForbidden() throws Exception {
        authenticateAs(staffUser);

        CreateRecordRequest req = new CreateRecordRequest(
                "Đau khớp gối", "75", "120/80", null, null, "Thuốc 5 thang", "Nghỉ ngơi", null, null, null
        );

        mockMvc.perform(post("/api/v1/staff/appointments/100/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Chỉ bác sĩ mới được lập phiếu kết quả")));
    }

    @Test
    @DisplayName("Ticket 7 Test 2: DOCTOR B gọi PUT /staff/records/{id} của bác sĩ A -> 403 Forbidden")
    void testUpdateRecord_DoctorBForbiddenOnDoctorARecord() throws Exception {
        authenticateAs(doctorUserB);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(medicalRecordA));

        UpdateRecordRequest req = new UpdateRecordRequest(
                "Chẩn đoán sửa đổi", null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/staff/records/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Bạn không phụ trách phiếu khám này")));
    }

    @Test
    @DisplayName("Ticket 7 Test 3: DOCTOR A gọi đúng phiếu của mình POST /staff/appointments/{id}/record -> 200 OK")
    void testCreateRecord_DoctorASuccess() throws Exception {
        authenticateAs(doctorUserA);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointmentA));
        when(medicalRecordRepository.findByAppointmentId(100L)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(inv -> {
            MedicalRecord saved = inv.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        CreateRecordRequest req = new CreateRecordRequest(
                "Đau khớp gối YHCT", "72", "120/80", Map.of("acupuncture", "A Thị"), null,
                "Thuốc 7 thang", "Giữ ấm khớp", null, "2026-11-25", "BS. Nguyễn Văn Minh"
        );

        mockMvc.perform(post("/api/v1/staff/appointments/100/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.record.diagnosis", is("Đau khớp gối YHCT")));
    }

    @Test
    @DisplayName("Ticket 7: DOCTOR A cập nhật đúng phiếu của mình PUT /staff/records/{id} -> 200 OK")
    void testUpdateRecord_DoctorASuccess() throws Exception {
        authenticateAs(doctorUserA);

        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(medicalRecordA));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRecordRequest req = new UpdateRecordRequest(
                "Đau nhức thoái hóa khớp gối (đã cập nhật)", null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/staff/records/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.record.diagnosis", is("Đau nhức thoái hóa khớp gối (đã cập nhật)")));
    }

    @Test
    @DisplayName("Ticket 7: Lập bệnh án khi phiếu chưa được gán bác sĩ -> 422 Unprocessable")
    void testCreateRecord_UnassignedDoctorThrows422() throws Exception {
        authenticateAs(doctorUserA);

        Appointment unassignedAppt = Appointment.builder()
                .id(101L)
                .bookingCode("CT-PK-26-1811-002")
                .patient(samplePatient)
                .doctor(null)
                .status(AppointmentStatus.IN_PROGRESS)
                .build();

        when(appointmentRepository.findById(101L)).thenReturn(Optional.of(unassignedAppt));

        CreateRecordRequest req = new CreateRecordRequest("Khám", null, null, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/staff/appointments/101/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", is("Phiếu khám chưa được gán bác sĩ")));
    }

    @Test
    @DisplayName("Ticket 7: STAFF gọi GET /staff/records/{id} -> 403 Forbidden")
    void testGetRecord_StaffForbidden() throws Exception {
        authenticateAs(staffUser);

        mockMvc.perform(get("/api/v1/staff/records/50"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Bạn không có quyền xem phiếu kết quả khám này")));
    }

    @Test
    @DisplayName("Ticket 7: ADMIN gọi GET /staff/records/{id} -> 200 OK")
    void testGetRecord_AdminSuccess() throws Exception {
        authenticateAs(adminUser);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(medicalRecordA));

        mockMvc.perform(get("/api/v1/staff/records/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.record.id", is("50")));
    }

    @Test
    @DisplayName("Ticket 7: DOCTOR A gọi GET /staff/records/{id} của mình -> 200 OK")
    void testGetRecord_DoctorAOwnRecord_Success() throws Exception {
        authenticateAs(doctorUserA);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(medicalRecordA));

        mockMvc.perform(get("/api/v1/staff/records/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.record.id", is("50")));
    }

    @Test
    @DisplayName("Ticket 7: DOCTOR B gọi GET /staff/records/{id} của A khi chưa từng khám BN -> 403 Forbidden")
    void testGetRecord_DoctorBUnrelated_Forbidden() throws Exception {
        authenticateAs(doctorUserB);
        when(medicalRecordRepository.findById(50L)).thenReturn(Optional.of(medicalRecordA));
        when(doctorRepository.findByUserId(102L)).thenReturn(Optional.of(doctorB));
        when(appointmentRepository.findByPatientId(10L)).thenReturn(List.of(appointmentA)); // chỉ có bác sĩ A

        mockMvc.perform(get("/api/v1/staff/records/50"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Bạn không có quyền xem phiếu kết quả khám này")));
    }
}
