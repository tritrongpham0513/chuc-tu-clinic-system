package com.chuctupk.controller;

import com.chuctupk.dto.response.NotificationItemDto;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.exception.GlobalExceptionHandler;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.service.PatientPortalService;
import com.chuctupk.service.impl.PatientPortalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeNotificationControllerTest {

    private MockMvc mockMvc;
    private PatientPortalService patientPortalService;
    private AppointmentRepository appointmentRepository;
    private Clock fixedClock;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        patientPortalService = mock(PatientPortalService.class);
        appointmentRepository = mock(AppointmentRepository.class);
        fixedClock = Clock.fixed(Instant.parse("2026-11-20T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        userA = User.builder()
                .id(1L)
                .username("patient_A")
                .fullName("Nguyễn Văn A")
                .phone("0912345678")
                .role(Role.PATIENT)
                .isActive(true)
                .build();

        userB = User.builder()
                .id(2L)
                .username("patient_B")
                .fullName("Trần Thị B")
                .phone("0987654321")
                .role(Role.PATIENT)
                .isActive(true)
                .build();

        MeController meController = new MeController(patientPortalService);
        mockMvc = MockMvcBuilders.standaloneSetup(meController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/me/notifications - Trả cấu trúc chuẩn và thông báo của chính mình")
    void testGetMyNotifications_Success() throws Exception {
        NotificationItemDto item = new NotificationItemDto(
                "REMINDER",
                "Nhắc lịch khám",
                "Phiếu CT-PK-26-2011-001 lúc 08:00 ngày 20/11",
                12L,
                "2026-11-20T08:00:00",
                false
        );

        when(patientPortalService.getMyNotifications(eq(userA)))
                .thenReturn(Map.of("items", List.of(item)));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userA, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/v1/me/notifications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].type", is("REMINDER")))
                .andExpect(jsonPath("$.data.items[0].title", is("Nhắc lịch khám")))
                .andExpect(jsonPath("$.data.items[0].body", containsString("Phiếu CT-PK-26-2011-001")))
                .andExpect(jsonPath("$.data.items[0].appointmentId", is(12)))
                .andExpect(jsonPath("$.data.items[0].at", is("2026-11-20T08:00:00")))
                .andExpect(jsonPath("$.data.items[0].read", is(false)));
    }

    private PatientPortalServiceImpl createPortalService() {
        return new PatientPortalServiceImpl(
                mock(com.chuctupk.repository.UserRepository.class),
                mock(com.chuctupk.repository.PatientRepository.class),
                appointmentRepository,
                mock(com.chuctupk.repository.AppointmentSeatRepository.class),
                mock(com.chuctupk.repository.IdempotencyKeyRepository.class),
                mock(com.chuctupk.repository.MedicalRecordRepository.class),
                mock(com.chuctupk.repository.TimeSlotRepository.class),
                mock(com.chuctupk.service.SequenceGeneratorService.class),
                fixedClock,
                mock(com.chuctupk.service.ClinicSettingService.class),
                mock(com.chuctupk.service.SlotPolicy.class),
                mock(com.chuctupk.service.DoctorAssignmentService.class),
                mock(com.chuctupk.service.NotificationService.class)
        );
    }

    @Test
    @DisplayName("Ticket 4 & 6: Bệnh nhân A không thấy thông báo của bệnh nhân B")
    void testPatientIsolation_UserACannotSeeUserBNotifications() {
        PatientPortalServiceImpl portalService = createPortalService();

        TimeSlot slot = TimeSlot.builder().id(1L).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0)).build();
        Patient patientA = Patient.builder().id(101L).user(userA).fullName("Nguyễn Văn A").phone("0912345678").build();
        Appointment aptA = Appointment.builder()
                .id(12L)
                .bookingCode("CT-PK-A")
                .patient(patientA)
                .appointmentDate(LocalDate.of(2026, 11, 20))
                .timeSlot(slot)
                .status(AppointmentStatus.PENDING)
                .build();

        // Giả lập DB: khi query theo userId = 1L (userA) -> trả aptA; khi query theo userId = 2L (userB) -> trả rỗng
        when(appointmentRepository.findUpcomingRemindersByUser(
                eq(1L), any(), any(), any(), any()
        )).thenReturn(List.of(aptA));

        when(appointmentRepository.findUpcomingRemindersByUser(
                eq(2L), any(), any(), any(), any()
        )).thenReturn(List.of());

        // 1. User A gọi -> thấy phiếu của mình
        Map<String, Object> resA = portalService.getMyNotifications(userA);
        List<?> itemsA = (List<?>) resA.get("items");
        assertThat(itemsA).hasSize(1);
        NotificationItemDto itemA = (NotificationItemDto) itemsA.get(0);
        assertThat(itemA.appointmentId()).isEqualTo(12L);
        assertThat(itemA.body()).contains("CT-PK-A");

        // 2. User B gọi -> kết quả rỗng, KHÔNG thấy thông báo của User A
        Map<String, Object> resB = portalService.getMyNotifications(userB);
        List<?> itemsB = (List<?>) resB.get("items");
        assertThat(itemsB).isEmpty();

        // 3. Xác minh query parameter: đúng phạm vi 7 ngày và đúng enum DB PENDING, CONFIRMED
        ArgumentCaptor<LocalDate> startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endCaptor = ArgumentCaptor.forClass(LocalDate.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<AppointmentStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);

        verify(appointmentRepository).findUpcomingRemindersByUser(
                eq(1L), statusesCaptor.capture(), startCaptor.capture(), endCaptor.capture(), any()
        );

        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Ticket 4: Không đăng nhập gọi getMyNotifications -> Ném lỗi Unauthorized 401")
    void testGetMyNotifications_Unauthenticated_ThrowsException() {
        PatientPortalServiceImpl portalService = createPortalService();

        assertThatThrownBy(() -> portalService.getMyNotifications(null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Vui lòng đăng nhập");
    }
}
