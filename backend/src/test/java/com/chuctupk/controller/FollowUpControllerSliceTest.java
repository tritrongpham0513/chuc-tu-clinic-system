package com.chuctupk.controller;

import com.chuctupk.dto.request.ReminderMarkSentRequest;
import com.chuctupk.dto.response.FollowUpItemDto;
import com.chuctupk.dto.response.FollowUpListResponse;
import com.chuctupk.dto.response.FollowUpReminderPreviewDto;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.service.DoctorAssignmentService;
import com.chuctupk.service.StaffService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Sprint 11 — Follow Up Controller Slice Tests (Ticket 1 & 4)")
class FollowUpControllerSliceTest {

    private MockMvc mockMvc;
    private StaffService staffService;
    private DoctorAssignmentService doctorAssignmentService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User staffUser;

    @BeforeEach
    void setUp() {
        staffService = mock(StaffService.class);
        doctorAssignmentService = mock(DoctorAssignmentService.class);

        StaffController staffController = new StaffController(staffService, doctorAssignmentService);
        mockMvc = MockMvcBuilders.standaloneSetup(staffController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        staffUser = User.builder()
                .id(10L)
                .username("letan")
                .fullName("Tiếp tân Nguyễn")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                staffUser, null, List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/v1/staff/follow-ups?days=7 - Thành công trả danh sách cần tái khám")
    void testGetFollowUps() throws Exception {
        FollowUpItemDto item = new FollowUpItemDto(
                1L, "Nguyễn Văn A", 1955, "0909 *** 456", "0909123456",
                LocalDate.now().minusDays(10), LocalDate.now().plusDays(3),
                "BS. Nguyễn Văn Minh", 2L, "CHAM_CUU",
                100L, 50L, 3L, null
        );
        FollowUpListResponse response = new FollowUpListResponse(List.of(item), 1, 0, 0, 1);

        when(staffService.getFollowUps(eq(7), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/staff/follow-ups")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.total", is(1)))
                .andExpect(jsonPath("$.data.items[0].patientName", is("Nguyễn Văn A")))
                .andExpect(jsonPath("$.data.items[0].daysUntil", is(3)));

        verify(staffService).getFollowUps(eq(7), any());
    }

    @Test
    @DisplayName("GET /api/v1/staff/follow-ups/{recordId}/reminder-preview - Thành công trả nội dung soạn sẵn")
    void testGetFollowUpReminderPreview() throws Exception {
        FollowUpReminderPreviewDto preview = new FollowUpReminderPreviewDto(
                100L, "Nguyễn Văn A", "0909 *** 456", "0909123456",
                "Phòng khám Chúc Từ nhắc Cô/chú tái khám ngày 25/11/2026 theo hẹn của bác sĩ BS. Nguyễn Văn Minh.",
                "MANUAL", "FOLLOW_UP"
        );

        when(staffService.getFollowUpReminderPreview(100L)).thenReturn(preview);

        mockMvc.perform(get("/api/v1/staff/follow-ups/100/reminder-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.recordId", is(100)))
                .andExpect(jsonPath("$.data.template", is("FOLLOW_UP")))
                .andExpect(jsonPath("$.data.content", is(preview.content())));

        verify(staffService).getFollowUpReminderPreview(100L);
    }

    @Test
    @DisplayName("POST /api/v1/staff/follow-ups/{recordId}/reminder-mark-sent - Thành công ghi nhận đã gửi")
    void testMarkFollowUpReminderSent() throws Exception {
        when(staffService.markFollowUpReminderSent(any(), eq(100L), any()))
                .thenReturn(Map.of("recordId", 100L, "sentAt", "2026-11-22T10:00:00", "status", "SENT"));

        mockMvc.perform(post("/api/v1/staff/follow-ups/100/reminder-mark-sent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReminderMarkSentRequest("Đã gửi SMS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.recordId", is(100)))
                .andExpect(jsonPath("$.data.status", is("SENT")));

        verify(staffService).markFollowUpReminderSent(any(), eq(100L), any());
    }
}
