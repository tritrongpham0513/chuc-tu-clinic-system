package com.chuctupk.controller;

import com.chuctupk.dto.request.CreateDoctorDayOffRequest;
import com.chuctupk.dto.request.ScheduleItemRequest;
import com.chuctupk.dto.request.UpdateDoctorScheduleRequest;
import com.chuctupk.dto.response.CreateDoctorDayOffResult;
import com.chuctupk.dto.response.DoctorDayOffDto;
import com.chuctupk.dto.response.DoctorScheduleMatrixResponse;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.DoctorScheduleAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DoctorScheduleAdminControllerSliceTest {

    private MockMvc mockMvc;
    private DoctorScheduleAdminService service;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        service = mock(DoctorScheduleAdminService.class);
        DoctorScheduleAdminController controller = new DoctorScheduleAdminController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/doctor-schedules/matrix trả về 200 OK với đúng cấu trúc")
    void testGetMatrix() throws Exception {
        DoctorScheduleMatrixResponse resp = DoctorScheduleMatrixResponse.builder()
                .days(List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
                .doctors(List.of(
                        DoctorScheduleMatrixResponse.DoctorScheduleRow.builder()
                                .id(1L)
                                .fullName("Bác sĩ Nguyễn Văn A")
                                .title("Lương y")
                                .specialty("YHCT")
                                .isActive(true)
                                .schedules(Map.of("MON", List.of("MORNING", "AFTERNOON")))
                                .build()
                ))
                .build();

        when(service.getWeeklyScheduleMatrix()).thenReturn(resp);

        mockMvc.perform(get("/api/v1/admin/doctor-schedules/matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.days", hasSize(7)))
                .andExpect(jsonPath("$.data.doctors[0].fullName").value("Bác sĩ Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.doctors[0].schedules.MON", contains("MORNING", "AFTERNOON")));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/doctor-schedules/{doctorId} trả về 200 OK khi cập nhật thành công")
    void testUpdateSchedule() throws Exception {
        List<ScheduleItemRequest> items = List.of(
                new ScheduleItemRequest(DayOfWeek.MON, Period.MORNING, true),
                new ScheduleItemRequest(DayOfWeek.MON, Period.AFTERNOON, false)
        );
        UpdateDoctorScheduleRequest req = new UpdateDoctorScheduleRequest(items);

        DoctorScheduleMatrixResponse resp = DoctorScheduleMatrixResponse.builder()
                .days(List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
                .doctors(List.of())
                .build();

        when(service.updateDoctorSchedule(eq(1L), any(UpdateDoctorScheduleRequest.class))).thenReturn(resp);

        mockMvc.perform(put("/api/v1/admin/doctor-schedules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/admin/doctor-day-offs trả về danh sách ngày nghỉ")
    void testGetDayOffs() throws Exception {
        DoctorDayOffDto dto = DoctorDayOffDto.builder()
                .id(10L)
                .doctorId(1L)
                .doctorName("Bác sĩ A")
                .offDate(LocalDate.of(2026, 11, 20))
                .period(Period.FULL_DAY)
                .reason("Nghỉ phép")
                .createdAt(LocalDateTime.of(2026, 11, 17, 10, 0))
                .build();

        when(service.getUpcomingDayOffs(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/admin/doctor-day-offs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].doctorName").value("Bác sĩ A"))
                .andExpect(jsonPath("$.data[0].period").value("FULL_DAY"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/doctor-day-offs trả về 201 Created với số ca bị ảnh hưởng")
    void testCreateDayOff() throws Exception {
        CreateDoctorDayOffRequest req = new CreateDoctorDayOffRequest(
                1L, LocalDate.of(2026, 11, 25), Period.MORNING, "Việc bận"
        );

        CreateDoctorDayOffResult result = CreateDoctorDayOffResult.builder()
                .item(DoctorDayOffDto.builder()
                        .id(15L)
                        .doctorId(1L)
                        .doctorName("Bác sĩ A")
                        .offDate(LocalDate.of(2026, 11, 25))
                        .period(Period.MORNING)
                        .reason("Việc bận")
                        .build())
                .affectedAppointments(2)
                .build();

        when(service.createDayOff(any(CreateDoctorDayOffRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/doctor-day-offs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.affectedAppointments").value(2))
                .andExpect(jsonPath("$.data.item.id").value(15));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/doctor-day-offs/{id} trả về 200 OK")
    void testDeleteDayOff() throws Exception {
        doNothing().when(service).deleteDayOff(15L);

        mockMvc.perform(delete("/api/v1/admin/doctor-day-offs/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("huỷ ngày nghỉ")));
    }
}
