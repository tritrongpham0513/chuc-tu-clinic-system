package com.chuctupk.controller;

import com.chuctupk.dto.request.AutoInviteRequest;
import com.chuctupk.dto.request.CancelAppointmentStaffRequest;
import com.chuctupk.dto.request.RescheduleAppointmentRequest;
import com.chuctupk.dto.request.StaffCreateAppointmentRequest;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.StaffService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class V1StaffControllerSliceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User staffUser;

    static class StubStaffService implements StaffService {
        private final PatientDto samplePatient = new PatientDto(
                "10", "CT-BN-26-0010", "Bác Lê Văn Tám", "0987654321",
                "1955-05-15", 1955, "NAM", "BAN_THAN", "Quận 3, TP.HCM",
                "", false, false, true, "2026-11-14T08:00:00", "2026-11-14T08:00:00", "2026-11-14T08:00:00"
        );

        private final AppointmentDto sampleAppt = new AppointmentDto(
                "100", "CT-PK-26-1411-001", "CT-PK-26-1411-001", samplePatient, "BS. Nguyễn Văn Minh",
                "CHAM_CUU", "2026-11-14", "08:00", "09:00", "08:00", "BOOKED", "ONLINE",
                null, "Bác Lê Văn Tám", "0987654321", "2026-11-14T07:30:00", "2026-11-14T07:30:00",
                null, null, Map.of("queueNo", 1, "priorityGroup", "PRIORITY")
        );

        @Override
        public StaffAppointmentListResponse getAppointments(LocalDate date) {
            StaffAppointmentSummaryDto summary = new StaffAppointmentSummaryDto(1, 0, 0, 0, 0, 1);
            return new StaffAppointmentListResponse(date != null ? date.toString() : "2026-11-14", List.of(sampleAppt), summary);
        }

        @Override
        public StaffAppointmentDatesResponse getAppointmentDates(String month) {
            return new StaffAppointmentDatesResponse(month != null ? month : "2026-11", List.of("2026-11-14", "2026-11-15"));
        }

        @Override
        public StaffAppointmentLookupResponse lookupAppointment(String code) {
            if ("CT-PK-26-1411-001".equalsIgnoreCase(code)) {
                return new StaffAppointmentLookupResponse(sampleAppt);
            }
            return new StaffAppointmentLookupResponse(null);
        }

        @Override
        public StaffAppointmentDetailResponse getAppointmentDetail(Long id) {
            MedicalRecordPatientDto rec = new MedicalRecordPatientDto(
                    "50", "100", "10", "BS. Nguyễn Văn Minh", "2026-11-14",
                    "Châm cứu giảm đau khớp gối", "75", "125/80", "75|125/80",
                    "Châm huyệt Huyết hải, Độc tỵ", Map.of("acupuncture", "Huyết hải, Độc tỵ"),
                    "Thuốc thang 5 thang", "Hạn chế mang vác nặng", "2026-11-21", "2026-11-14T08:30:00"
            );
            return new StaffAppointmentDetailResponse(sampleAppt, rec);
        }

        @Override
        public StaffAvailabilityResponse getAvailability(LocalDate date) {
            SlotAvailabilityDto s = new SlotAvailabilityDto("1", "08:00", "09:00", 2, 6, 4, true, null);
            return new StaffAvailabilityResponse("2026-11-14", List.of(s));
        }

        @Override
        public Map<String, Object> createCounterAppointment(User staff, StaffCreateAppointmentRequest request) {
            return Map.of("appointment", sampleAppt, "ticket", sampleAppt);
        }

        @Override
        public StaffAppointmentLookupResponse rescheduleAppointment(User staff, Long id, RescheduleAppointmentRequest request) {
            AppointmentDto rescheduled = new AppointmentDto(
                    "101", "CT-PK-26-1511-002", "CT-PK-26-1511-002", samplePatient, "BS. Nguyễn Văn Minh",
                    "CHAM_CUU", request.getSlotDate(), request.getSlotStart(), "10:00", request.getSlotStart(), "BOOKED", "ONLINE",
                    null, "Bác Lê Văn Tám", "0987654321", "2026-11-14T07:30:00", "2026-11-14T08:15:00",
                    null, null, null
            );
            return new StaffAppointmentLookupResponse(rescheduled);
        }

        @Override
        public StaffAppointmentLookupResponse cancelAppointment(User staff, Long id, CancelAppointmentStaffRequest request) {
            AppointmentDto cancelled = new AppointmentDto(
                    "100", "CT-PK-26-1411-001", "CT-PK-26-1411-001", samplePatient, null,
                    "CHAM_CUU", "2026-11-14", "08:00", "09:00", "08:00", "CANCELLED", "ONLINE",
                    request.reason(), "Bác Lê Văn Tám", "0987654321", "2026-11-14T07:30:00", "2026-11-14T08:20:00",
                    null, null, null
            );
            return new StaffAppointmentLookupResponse(cancelled);
        }

        @Override
        public StaffAppointmentLookupResponse checkin(User staff, Long id) {
            AppointmentDto checkedIn = new AppointmentDto(
                    "100", "CT-PK-26-1411-001", "CT-PK-26-1411-001", samplePatient, null,
                    "CHAM_CUU", "2026-11-14", "08:00", "09:00", "08:00", "CHECKED_IN", "ONLINE",
                    null, "Bác Lê Văn Tám", "0987654321", "2026-11-14T07:30:00", "2026-11-14T08:05:00",
                    "2026-11-14T08:05:00", null, Map.of("queueNo", 1, "priorityGroup", "PRIORITY")
            );
            return new StaffAppointmentLookupResponse(checkedIn);
        }

        @Override
        public StaffAppointmentLookupResponse undoCheckin(User staff, Long id) {
            return new StaffAppointmentLookupResponse(sampleAppt);
        }

        @Override
        public StaffAppointmentLookupResponse enterRoom(User staff, Long id) {
            AppointmentDto inRoom = new AppointmentDto(
                    "100", "CT-PK-26-1411-001", "CT-PK-26-1411-001", samplePatient, "BS. Nguyễn Văn Minh",
                    "CHAM_CUU", "2026-11-14", "08:00", "09:00", "08:00", "IN_PROGRESS", "ONLINE",
                    null, "Bác Lê Văn Tám", "0987654321", "2026-11-14T07:30:00", "2026-11-14T08:10:00",
                    "2026-11-14T08:05:00", "2026-11-14T08:10:00", Map.of("queueNo", 1, "seatNo", 1)
            );
            return new StaffAppointmentLookupResponse(inRoom);
        }

        @Override
        public QueueStateDto getQueueState() {
            List<QueueSeatDto> seats = new ArrayList<>();
            seats.add(new QueueSeatDto(
                    1, false, "1", "100", "CT-PK-26-1411-001", samplePatient, "BS. Nguyễn Văn Minh",
                    "2026-11-14T08:10:00", "PRIORITY", "ONLINE", "08:00", "2026-11-14T08:05:00"
            ));
            for (int s = 2; s <= 5; s++) {
                seats.add(new QueueSeatDto(s, true, null, null, null, null, "Bác sĩ trực", null, null, null, null, null));
            }

            List<QueueWaitingDto> waiting = List.of(
                    new QueueWaitingDto("2", "102", 2, "CT-PK-26-1411-002", samplePatient, "PRIORITY", "ONLINE", "08:00", "2026-11-14T08:06:00", 5)
            );

            return new QueueStateDto("2026-11-14", 5, 1, 1, 0, false, seats, waiting, Map.of("08:00", 1));
        }

        @Override
        public QueueStateDto inviteNext() {
            return getQueueState();
        }

        @Override
        public QueueStateDto undoEnter(Long id) {
            return getQueueState();
        }

        @Override
        public QueueStateDto setAutoInvite(boolean enabled) {
            return getQueueState();
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

        StaffController controller = new StaffController(new StubStaffService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/staff/appointments - Trả danh sách và thống kê hợp lệ")
    void testGetAppointments() throws Exception {
        mockMvc.perform(get("/api/v1/staff/appointments?date=2026-11-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.date", is("2026-11-14")))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.summary.total", is(1)))
                .andExpect(jsonPath("$.data.summary.booked", is(1)));
    }

    @Test
    @DisplayName("GET /api/v1/staff/appointments/dates - Trả danh sách ngày có lịch khám trong tháng")
    void testGetAppointmentDates() throws Exception {
        mockMvc.perform(get("/api/v1/staff/appointments/dates?month=2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.month", is("2026-11")))
                .andExpect(jsonPath("$.data.dates", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/v1/staff/appointments/lookup - Tra cứu phiếu thành công")
    void testLookupAppointment() throws Exception {
        mockMvc.perform(get("/api/v1/staff/appointments/lookup?code=CT-PK-26-1411-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.ticketCode", is("CT-PK-26-1411-001")))
                .andExpect(jsonPath("$.data.appointment.patient.fullName", is("Bác Lê Văn Tám")));
    }

    @Test
    @DisplayName("GET /api/v1/staff/appointments/{id} - Trả chi tiết phiếu và bệnh án")
    void testGetAppointmentDetail() throws Exception {
        mockMvc.perform(get("/api/v1/staff/appointments/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.id", is("100")))
                .andExpect(jsonPath("$.data.record.diagnosis", is("Châm cứu giảm đau khớp gối")));
    }

    @Test
    @DisplayName("GET /api/v1/staff/availability - Trả danh sách khung giờ khả dụng")
    void testGetAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/staff/availability?date=2026-11-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date", is("2026-11-14")))
                .andExpect(jsonPath("$.data.slots", hasSize(1)))
                .andExpect(jsonPath("$.data.slots[0].capacity", is(6)));
    }

    @Test
    @DisplayName("POST /api/v1/staff/appointments - Đặt lịch tại quầy thành công")
    void testCreateCounterAppointment() throws Exception {
        StaffCreateAppointmentRequest req = new StaffCreateAppointmentRequest(
                10L, "CHAM_CUU", "2026-11-14", "08:00", false
        );

        mockMvc.perform(post("/api/v1/staff/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.ticketCode", is("CT-PK-26-1411-001")))
                .andExpect(jsonPath("$.data.ticket.ticketCode", is("CT-PK-26-1411-001")));
    }

    @Test
    @DisplayName("POST /api/v1/staff/appointments/{id}/checkin - Check-in thành công")
    void testCheckin() throws Exception {
        mockMvc.perform(post("/api/v1/staff/appointments/100/checkin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.status", is("CHECKED_IN")))
                .andExpect(jsonPath("$.data.appointment.checkin.priorityGroup", is("PRIORITY")));
    }

    @Test
    @DisplayName("GET /api/v1/staff/queue - Trả đúng cấu trúc 5 ghế khám và hàng chờ")
    void testGetQueueState() throws Exception {
        mockMvc.perform(get("/api/v1/staff/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity", is(5)))
                .andExpect(jsonPath("$.data.inRoomCount", is(1)))
                .andExpect(jsonPath("$.data.waitingCount", is(1)))
                .andExpect(jsonPath("$.data.seats", hasSize(5)))
                .andExpect(jsonPath("$.data.seats[0].seatNo", is(1)))
                .andExpect(jsonPath("$.data.seats[0].empty", is(false)))
                .andExpect(jsonPath("$.data.seats[1].seatNo", is(2)))
                .andExpect(jsonPath("$.data.seats[1].empty", is(true)))
                .andExpect(jsonPath("$.data.waiting[0].priorityGroup", is("PRIORITY")));
    }

    @Test
    @DisplayName("POST /api/v1/staff/queue/invite - Mời người kế tiếp vào khám")
    void testInviteNext() throws Exception {
        mockMvc.perform(post("/api/v1/staff/queue/invite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity", is(5)));
    }

    @Test
    @DisplayName("PUT /api/v1/staff/queue/auto-invite - Cập nhật chế độ tự động mời")
    void testSetAutoInvite() throws Exception {
        AutoInviteRequest req = new AutoInviteRequest(true);
        mockMvc.perform(put("/api/v1/staff/queue/auto-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity", is(5)));
    }
}

