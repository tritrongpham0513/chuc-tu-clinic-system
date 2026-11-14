package com.chuctupk.controller;

import com.chuctupk.dto.request.PatientBookAppointmentRequest;
import com.chuctupk.dto.request.PatientProfileUpsertRequest;
import com.chuctupk.dto.request.UpdateMeRequest;
import com.chuctupk.dto.response.AppointmentDto;
import com.chuctupk.dto.response.PatientDto;
import com.chuctupk.dto.response.SlotAvailabilityDto;
import com.chuctupk.dto.response.UserWithPrimaryProfileDto;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.PatientPortalService;
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

class V1MeControllerSliceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;

    static class StubPatientPortalService implements PatientPortalService {
        @Override
        public Map<String, Object> getMyAccount(User currentUser) {
            UserWithPrimaryProfileDto u = new UserWithPrimaryProfileDto(
                    "1", "patient_0912345678", "Nguyễn Văn A", "0912345678", "", "USER",
                    "1960-01-01", 1960, "NAM", "TP.HCM", false, true, true, "2026-11-14T08:00:00"
            );
            return Map.of("user", u);
        }

        @Override
        public Map<String, Object> updateMyAccount(User currentUser, UpdateMeRequest request) {
            UserWithPrimaryProfileDto u = new UserWithPrimaryProfileDto(
                    "1", "patient_0912345678", request.getFullName(), "0912345678", request.getEmail(), "USER",
                    request.getDob(), 1960, request.getGender(), request.getAddress(), false, true, true, "2026-11-14T09:00:00"
            );
            return Map.of("user", u);
        }

        @Override
        public Map<String, Object> getMyPatients(User currentUser) {
            PatientDto p = new PatientDto("1", "CT-BN-26-0001", "Nguyễn Văn A", "0912345678", "1960-01-01", 1960, "NAM", "BAN_THAN", "TP.HCM", "", true, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00");
            return Map.of("items", List.of(p), "max", 10);
        }

        @Override
        public Map<String, Object> createPatient(User currentUser, PatientProfileUpsertRequest request) {
            PatientDto p = new PatientDto("2", "CT-BN-26-0002", request.getFullName(), request.getPhone(), request.getDob(), request.getBirthYear(), request.getGender(), request.getRelationship(), request.getAddress(), "", false, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00");
            return Map.of("patient", p);
        }

        @Override
        public Map<String, Object> getPatient(User currentUser, Long patientId) {
            PatientDto p = new PatientDto(String.valueOf(patientId), "CT-BN-26-0001", "Nguyễn Văn A", "0912345678", "1960-01-01", 1960, "NAM", "BAN_THAN", "TP.HCM", "", true, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00");
            return Map.of("patient", p);
        }

        @Override
        public Map<String, Object> updatePatient(User currentUser, Long patientId, PatientProfileUpsertRequest request) {
            PatientDto p = new PatientDto(String.valueOf(patientId), "CT-BN-26-0001", request.getFullName(), request.getPhone(), request.getDob(), request.getBirthYear(), request.getGender(), request.getRelationship(), request.getAddress(), "", true, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00");
            return Map.of("patient", p);
        }

        @Override
        public Map<String, Object> deletePatient(User currentUser, Long patientId) {
            return Map.of("success", true, "detached", false);
        }

        @Override
        public Map<String, Object> getAvailability(LocalDate date) {
            SlotAvailabilityDto s = new SlotAvailabilityDto("1", "08:00", "09:00", 2, 6, 4, true, null);
            return Map.of("date", date.toString(), "slots", List.of(s));
        }

        @Override
        public Map<String, Object> bookAppointment(User currentUser, PatientBookAppointmentRequest request, String idempotencyKey) {
            PatientDto p = new PatientDto("1", "CT-BN-26-0001", "Nguyễn Văn A", "0912345678", "1960-01-01", 1960, "NAM", "BAN_THAN", "TP.HCM", "", true, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00");
            AppointmentDto a = new AppointmentDto("10", "CT-PK-26-2011-001", "CT-PK-26-2011-001", p, null, "CHAM_CUU", request.getSlotDate(), request.getSlotStart(), "09:00", "08:00", "BOOKED", "ONLINE", null, "Nguyễn Văn A", "0912345678", "2026-11-14T08:00:00", "2026-11-14T08:00:00", null, null, null);
            return Map.of("appointment", a);
        }

        @Override
        public Map<String, Object> getMyAppointments(User currentUser, String statusFilter) {
            PatientDto p = new PatientDto("1", "CT-BN-26-0001", "Nguyễn Văn A", "0912345678", "1960-01-01", 1960, "NAM", "BAN_THAN", "TP.HCM", "", true, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00");
            AppointmentDto a = new AppointmentDto("10", "CT-PK-26-2011-001", "CT-PK-26-2011-001", p, null, "CHAM_CUU", "2026-11-20", "08:00", "09:00", "08:00", "BOOKED", "ONLINE", null, "Nguyễn Văn A", "0912345678", "2026-11-14T08:00:00", "2026-11-14T08:00:00", null, null, null);
            return Map.of("items", List.of(a));
        }

        @Override
        public Map<String, Object> getAppointmentDetail(User currentUser, Long id) {
            PatientDto p = new PatientDto("1", "CT-BN-26-0001", "Nguyễn Văn A", "0912345678", "1960-01-01", 1960, "NAM", "BAN_THAN", "TP.HCM", "", true, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00");
            AppointmentDto a = new AppointmentDto(String.valueOf(id), "CT-PK-26-2011-001", "CT-PK-26-2011-001", p, null, "CHAM_CUU", "2026-11-20", "08:00", "09:00", "08:00", "BOOKED", "ONLINE", null, "Nguyễn Văn A", "0912345678", "2026-11-14T08:00:00", "2026-11-14T08:00:00", null, null, null);
            return Map.of("appointment", a);
        }

        @Override
        public Map<String, Object> cancelAppointment(User currentUser, Long id, String reason) {
            return Map.of("success", true, "message", "Hủy phiếu khám thành công");
        }

        @Override
        public Map<String, Object> getAppointmentResult(User currentUser, Long id) {
            return getAppointmentDetail(currentUser, id);
        }

        @Override
        public Map<String, Object> getMyHistory(User currentUser) {
            return getMyAppointments(currentUser, "HISTORY");
        }
    }

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("patient_0912345678")
                .fullName("Nguyễn Văn A")
                .phone("0912345678")
                .role(Role.PATIENT)
                .isActive(true)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(testUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc = MockMvcBuilders.standaloneSetup(new MeController(new StubPatientPortalService()))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/me - Lấy thông tin tài khoản và hồ sơ chính chủ")
    void testGetMyAccount() throws Exception {
        mockMvc.perform(get("/api/v1/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.user.fullName", is("Nguyễn Văn A")))
                .andExpect(jsonPath("$.data.user.role", is("USER")));
    }

    @Test
    @DisplayName("GET /api/v1/me/patients - Lấy danh sách hồ sơ (tối đa 10)")
    void testGetMyPatients() throws Exception {
        mockMvc.perform(get("/api/v1/me/patients").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.max", is(10)));
    }

    @Test
    @DisplayName("POST /api/v1/me/patients - Thêm mới hồ sơ người thân")
    void testCreatePatient() throws Exception {
        PatientProfileUpsertRequest req = PatientProfileUpsertRequest.builder()
                .fullName("Nguyễn Thị Lan")
                .phone("0988112233")
                .birthYear(1965)
                .gender("NU")
                .relationship("VO_CHONG")
                .address("Quận 1, TP.HCM")
                .build();

        mockMvc.perform(post("/api/v1/me/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(201)))
                .andExpect(jsonPath("$.data.patient.fullName", is("Nguyễn Thị Lan")));
    }

    @Test
    @DisplayName("GET /api/v1/me/availability - Tra cứu khung giờ trống")
    void testGetAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/me/availability?date=2026-11-20").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.slots", hasSize(1)))
                .andExpect(jsonPath("$.data.slots[0].capacity", is(6)))
                .andExpect(jsonPath("$.data.slots[0].remaining", is(4)));
    }

    @Test
    @DisplayName("POST /api/v1/me/appointments - Đặt lịch khám mới với Idempotency-Key")
    void testBookAppointment() throws Exception {
        PatientBookAppointmentRequest req = PatientBookAppointmentRequest.builder()
                .patientId(1L)
                .serviceType("CHAM_CUU")
                .slotDate("2026-11-20")
                .slotStart("08:00")
                .build();

        mockMvc.perform(post("/api/v1/me/appointments")
                        .header("Idempotency-Key", "idem-uuid-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(201)))
                .andExpect(jsonPath("$.data.appointment.ticketCode", is("CT-PK-26-2011-001")))
                .andExpect(jsonPath("$.data.appointment.status", is("BOOKED")));
    }

    @Test
    @DisplayName("POST /api/v1/me/appointments/{id}/cancel - Hủy phiếu khám thành công")
    void testCancelAppointment() throws Exception {
        mockMvc.perform(post("/api/v1/me/appointments/10/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Bận việc đột xuất"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.success", is(true)));
    }
}

