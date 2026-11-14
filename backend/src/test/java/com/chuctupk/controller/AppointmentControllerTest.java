package com.chuctupk.controller;

import com.chuctupk.dto.request.AppointmentCancelRequest;
import com.chuctupk.dto.request.AppointmentCreateRequest;
import com.chuctupk.dto.response.AppointmentResponse;
import com.chuctupk.dto.response.ClinicServiceResponse;
import com.chuctupk.dto.response.DoctorResponse;
import com.chuctupk.dto.response.TimeSlotResponse;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Gender;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.AppException;
import com.chuctupk.service.AppointmentService;
import com.chuctupk.service.ClinicServiceService;
import com.chuctupk.service.DoctorService;
import com.chuctupk.service.TimeSlotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private ClinicServiceService clinicServiceService;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private TimeSlotService timeSlotService;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("GET /api/v1/services -> 200 OK")
    void testGetServices() throws Exception {
        when(clinicServiceService.getActiveServices()).thenReturn(List.of(
                ClinicServiceResponse.builder().id(1L).name("Châm cứu").slug("cham-cuu").build()
        ));

        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Châm cứu"));
    }

    @Test
    @DisplayName("GET /api/v1/doctors -> 200 OK")
    void testGetDoctors() throws Exception {
        when(doctorService.getActiveDoctors()).thenReturn(List.of(
                DoctorResponse.builder().id(1L).fullName("BS. Nguyễn Văn A").build()
        ));

        mockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].fullName").value("BS. Nguyễn Văn A"));
    }

    @Test
    @DisplayName("GET /api/v1/time-slots -> 200 OK và 400 nếu sai date")
    void testGetTimeSlots() throws Exception {
        when(timeSlotService.getAvailableTimeSlots(any(), any())).thenReturn(List.of(
                TimeSlotResponse.builder().id(1L).slotLabel("08:00 - 08:30")
                        .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(8, 30))
                        .period(Period.MORNING).remaining(2).build()
        ));

        mockMvc.perform(get("/api/v1/time-slots").param("date", "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].remaining").value(2));

        // Sai định dạng date -> 400 Bad Request
        mockMvc.perform(get("/api/v1/time-slots").param("date", "15-11-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/appointments -> 201 Created kèm header Location")
    void testCreateAppointment_CreatedWithLocationHeader() throws Exception {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Lê Văn T")
                .phone("0987654321")
                .yearOfBirth(1965)
                .gender(Gender.MALE)
                .appointmentDate(LocalDate.now().plusDays(2))
                .timeSlotId(1L)
                .build();

        AppointmentResponse res = AppointmentResponse.builder()
                .bookingCode("PKCT-20261102-1234")
                .patientName("Lê Văn T")
                .appointmentDate(req.getAppointmentDate())
                .status(AppointmentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(appointmentService.createAppointment(any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/appointments/PKCT-20261102-1234"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingCode").value("PKCT-20261102-1234"));
    }

    @Test
    @DisplayName("POST /api/v1/appointments -> 400 Bad Request khi sai số điện thoại")
    void testCreateAppointment_InvalidPhone() throws Exception {
        AppointmentCreateRequest req = AppointmentCreateRequest.builder()
                .fullName("Lê Văn T")
                .phone("0123456789") // Đầu số 012 cũ không hợp lệ
                .yearOfBirth(1965)
                .gender(Gender.MALE)
                .appointmentDate(LocalDate.now().plusDays(2))
                .timeSlotId(1L)
                .build();

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.fields.phone", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/appointments/{code} -> 400 khi thiếu header X-Phone, 404 khi không tìm thấy")
    void testGetAppointment() throws Exception {
        // Thiếu header X-Phone -> 400
        mockMvc.perform(get("/api/v1/appointments/PKCT-20261102-1234"))
                .andExpect(status().isBadRequest());

        // Có header X-Phone nhưng service trả 404
        when(appointmentService.getAppointmentByBookingCode(eq("PKCT-99999999-9999"), eq("0987654321")))
                .thenThrow(AppException.notFound("Không tìm thấy lịch hẹn"));

        mockMvc.perform(get("/api/v1/appointments/PKCT-99999999-9999")
                        .header("X-Phone", "0987654321"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy lịch hẹn"));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/{code} -> 400 khi sai status khác CANCELLED")
    void testCancelAppointment_InvalidStatus() throws Exception {
        AppointmentCancelRequest badReq = AppointmentCancelRequest.builder()
                .status("COMPLETED") // Bệnh nhân không được gửi status khác CANCELLED
                .build();

        mockMvc.perform(patch("/api/v1/appointments/PKCT-20261102-1234")
                        .header("X-Phone", "0987654321")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/{code} -> 400 khi thiếu trường status")
    void testCancelAppointment_MissingStatus() throws Exception {
        // Body rỗng không có status
        String badJson = "{\"cancelReason\": \"Bận đột xuất\"}";

        mockMvc.perform(patch("/api/v1/appointments/PKCT-20261102-1234")
                        .header("X-Phone", "0987654321")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/appointments/{code} -> 200 khi status đúng CANCELLED và có X-Phone")
    void testCancelAppointment_Success() throws Exception {
        AppointmentCancelRequest validReq = AppointmentCancelRequest.builder()
                .status("CANCELLED")
                .cancelReason("Bận việc đột xuất")
                .build();

        AppointmentResponse res = AppointmentResponse.builder()
                .bookingCode("PKCT-20261102-1234")
                .patientName("Trần Thị B")
                .appointmentDate(LocalDate.of(2026, 11, 2))
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentService.cancelAppointment(eq("PKCT-20261102-1234"), eq("0987654321"), any(AppointmentCancelRequest.class)))
                .thenReturn(res);

        mockMvc.perform(patch("/api/v1/appointments/PKCT-20261102-1234")
                        .header("X-Phone", "0987654321")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
