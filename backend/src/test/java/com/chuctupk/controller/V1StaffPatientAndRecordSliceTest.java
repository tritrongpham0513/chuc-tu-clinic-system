package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateRecordRequest;
import com.chuctupk.dto.request.StaffCreatePatientRequest;
import com.chuctupk.dto.request.StaffUpdatePatientRequest;
import com.chuctupk.dto.request.UpdateRecordRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.StaffPatientService;
import com.chuctupk.service.StaffRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class V1StaffPatientAndRecordSliceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User staffUser;

    static class StubStaffPatientService implements StaffPatientService {
        private final PatientDto samplePatient = new PatientDto(
                "10", "CT-BN-26-0010", "Bác Lê Văn Tám", "0987654321",
                "1955-05-15", 1955, "NAM", "BAN_THAN", "Quận 3, TP.HCM",
                "", false, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00",
                "2026-11-10"
        );

        @Override
        public StaffPatientListResponse getPatients(String q, Boolean locked, int page, int limit) {
            return new StaffPatientListResponse(List.of(samplePatient), 1, 1, 1, 10);
        }

        @Override
        public Map<String, Object> getDuplicatePatients(String phone, Long excludeId) {
            return Map.of("items", List.of(samplePatient));
        }

        @Override
        public Map<String, Object> createPatient(StaffCreatePatientRequest request) {
            PatientDto created = new PatientDto(
                    "11", "CT-BN-26-0011", request.fullName(), request.phone(),
                    request.dob(), request.birthYear(), request.gender() != null ? request.gender() : "NAM",
                    "BAN_THAN", request.address() != null ? request.address() : "",
                    request.medicalHistory() != null ? request.medicalHistory() : "",
                    false, false, true, "2026-11-14T09:00:00", "2026-11-14T09:00:00", "2026-11-14T09:00:00",
                    null
            );
            return Map.of("patient", created);
        }

        @Override
        public StaffPatientDetailResponse getPatientDetail(Long id) {
            AppointmentDto appt = new AppointmentDto(
                    "100", "CT-PK-26-1411-001", "CT-PK-26-1411-001", samplePatient, "BS. Nguyễn Văn Minh",
                    "CHAM_CUU", "2026-11-14", "08:00", "09:00", "08:00", "COMPLETED", "ONLINE",
                    null, "Bác Lê Văn Tám", "0987654321", "2026-11-14T07:30:00", "2026-11-14T09:00:00",
                    "2026-11-14T08:00:00", "2026-11-14T08:10:00", null
            );

            MedicalRecordPatientDto rec = new MedicalRecordPatientDto(
                    "50", "100", "10", "BS. Nguyễn Văn Minh", "2026-11-14",
                    "Đau nhức khớp gối thoái hóa", "72", "120/80", "72|120/80",
                    "Châm cứu và xoa bóp", Map.of("acupuncture", "Châm huyệt A Thị, Dương Lăng Tuyền"),
                    "Thuốc đông y 7 thang", "Giữ ấm khớp, hạn chế leo cầu thang", "2026-11-21", "2026-11-14T08:45:00"
            );

            return new StaffPatientDetailResponse(samplePatient, List.of(appt), List.of(rec));
        }

        @Override
        public Map<String, Object> updatePatient(Long id, StaffUpdatePatientRequest request) {
            PatientDto updated = new PatientDto(
                    "10", "CT-BN-26-0010", request.fullName() != null ? request.fullName() : "Bác Lê Văn Tám",
                    "0987654321", "1955-05-15", 1955, "NAM", "BAN_THAN",
                    request.address() != null ? request.address() : "Quận 3, TP.HCM",
                    "", false, false, true, "2026-11-14T08:00:00", "2026-11-14T09:15:00", "2026-11-14T09:15:00",
                    "2026-11-10"
            );
            return Map.of("patient", updated);
        }
    }

    static class StubStaffRecordService implements StaffRecordService {
        private final MedicalRecordPatientDto sampleRecord = new MedicalRecordPatientDto(
                "50", "100", "10", "BS. Nguyễn Văn Minh", "2026-11-14",
                "Đau nhức khớp gối thoái hóa", "72", "120/80", "72|120/80",
                "Châm cứu và xoa bóp", Map.of("acupuncture", "Châm huyệt A Thị, Dương Lăng Tuyền"),
                "Thuốc đông y 7 thang", "Giữ ấm khớp", "2026-11-21", "2026-11-14T08:45:00"
        );

        @Override
        public RecordDetailResponse createRecord(User staff, Long appointmentId, CreateRecordRequest request) {
            return new RecordDetailResponse(sampleRecord);
        }

        @Override
        public RecordDetailResponse getRecord(Long id) {
            return new RecordDetailResponse(sampleRecord);
        }

        @Override
        public RecordDetailResponse updateRecord(User staff, Long id, UpdateRecordRequest request) {
            return new RecordDetailResponse(sampleRecord);
        }
    }

    @BeforeEach
    void setUp() {
        staffUser = User.builder()
                .id(2L)
                .username("letan")
                .fullName("Tiếp tân Nguyễn")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                staffUser, null, Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        StaffPatientController patientController = new StaffPatientController(new StubStaffPatientService());
        StaffRecordController recordController = new StaffRecordController(new StubStaffRecordService());

        mockMvc = MockMvcBuilders.standaloneSetup(patientController, recordController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/staff/patients - Tìm kiếm và phân trang hồ sơ bệnh nhân")
    void testGetPatients() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients?q=Le&page=1&limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].patientCode", is("CT-BN-26-0010")))
                .andExpect(jsonPath("$.data.items[0].lastVisitDate", is("2026-11-10")));
    }

    @Test
    @DisplayName("GET /api/v1/staff/patients/duplicates - Tra cứu trùng số điện thoại")
    void testGetDuplicates() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/duplicates?phone=0987654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].phone", is("0987654321")));
    }

    @Test
    @DisplayName("POST /api/v1/staff/patients - Tạo hồ sơ bệnh nhân tại quầy (201 Created)")
    void testCreatePatient() throws Exception {
        StaffCreatePatientRequest req = new StaffCreatePatientRequest(
                "Bác Trần Thị Bảy", "0912345678", 1958, "1958-08-20", "NU",
                "Quận Bình Thạnh, TP.HCM", "Tiền sử đau dạ dày", false
        );

        mockMvc.perform(post("/api/v1/staff/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.patient.patientCode", is("CT-BN-26-0011")))
                .andExpect(jsonPath("$.data.patient.fullName", is("Bác Trần Thị Bảy")));
    }

    @Test
    @DisplayName("GET /api/v1/staff/patients/{id} - Lấy chi tiết hồ sơ, lịch hẹn và kết quả khám")
    void testGetPatientDetail() throws Exception {
        mockMvc.perform(get("/api/v1/staff/patients/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patient.id", is("10")))
                .andExpect(jsonPath("$.data.appointments", hasSize(1)))
                .andExpect(jsonPath("$.data.records", hasSize(1)));
    }

    @Test
    @DisplayName("PATCH /api/v1/staff/patients/{id} - Cập nhật hồ sơ bệnh nhân")
    void testUpdatePatient() throws Exception {
        StaffUpdatePatientRequest req = new StaffUpdatePatientRequest(
                "Bác Lê Văn Tám Đã Sửa", null, null, null, null, "Quận 1, TP.HCM", null, "2026-11-14T08:00:00"
        );

        mockMvc.perform(patch("/api/v1/staff/patients/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patient.fullName", is("Bác Lê Văn Tám Đã Sửa")));
    }

    @Test
    @DisplayName("POST /api/v1/staff/appointments/{id}/record - Lập phiếu kết quả khám bệnh")
    void testCreateRecord() throws Exception {
        CreateRecordRequest req = new CreateRecordRequest(
                "Đau thắt lưng thoái hóa", "75", "125/80",
                Map.of("acupuncture", "Thận du, Đại trường du"), null,
                "Thuốc đông y 5 thang", "Nghỉ ngơi, tránh mang nặng", null, "2026-11-21", "BS. Nguyễn Văn Minh"
        );

        mockMvc.perform(post("/api/v1/staff/appointments/100/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record.diagnosis", is("Đau nhức khớp gối thoái hóa")));
    }

    @Test
    @DisplayName("GET /api/v1/staff/records/{id} - Xem chi tiết phiếu kết quả khám bệnh")
    void testGetRecord() throws Exception {
        mockMvc.perform(get("/api/v1/staff/records/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record.id", is("50")));
    }

    @Test
    @DisplayName("PUT /api/v1/staff/records/{id} - Chỉnh sửa phiếu kết quả khám bệnh")
    void testUpdateRecord() throws Exception {
        UpdateRecordRequest req = new UpdateRecordRequest(
                "Đau nhức khớp gối thoái hóa (đã cập nhật)", "70", "120/80",
                null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/staff/records/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record.id", is("50")));
    }
}

