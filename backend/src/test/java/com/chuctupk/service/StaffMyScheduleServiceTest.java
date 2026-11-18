package com.chuctupk.service;

import com.chuctupk.dto.response.DoctorScheduleMatrixResponse;
import com.chuctupk.dto.response.MyScheduleResponse;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.DoctorDayOff;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.StaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StaffMyScheduleServiceTest {

    private DoctorRepository doctorRepository;
    private DoctorScheduleAdminService doctorScheduleAdminService;
    private DoctorDayOffRepository doctorDayOffRepository;
    private Clock clock;
    private StaffServiceImpl staffService;

    private User doctorUser;
    private Doctor sampleDoctor;
    private final LocalDate today = LocalDate.of(2026, 11, 17);

    @BeforeEach
    void setUp() {
        doctorRepository = mock(DoctorRepository.class);
        doctorScheduleAdminService = mock(DoctorScheduleAdminService.class);
        doctorDayOffRepository = mock(DoctorDayOffRepository.class);
        clock = Clock.fixed(Instant.parse("2026-11-17T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        staffService = new StaffServiceImpl(
                mock(AppointmentRepository.class),
                mock(CheckinRepository.class),
                mock(PatientRepository.class),
                mock(TimeSlotRepository.class),
                mock(MedicalRecordRepository.class),
                mock(AppointmentSeatRepository.class),
                mock(ClinicSettingService.class),
                mock(SlotPolicy.class),
                mock(DoctorAssignmentService.class),
                doctorRepository,
                doctorScheduleAdminService,
                doctorDayOffRepository,
                mock(NotificationService.class),
                mock(SequenceGeneratorService.class),
                clock
        );

        doctorUser = User.builder()
                .id(10L)
                .username("bacsi")
                .role(Role.DOCTOR)
                .isActive(true)
                .build();

        sampleDoctor = Doctor.builder()
                .id(1L)
                .user(doctorUser)
                .fullName("Bác sĩ Nguyễn Văn Minh")
                .title("Lương y")
                .specialty("Y học cổ truyền")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Case a: user DOCTOR có hồ sơ bác sĩ -> trả đúng doctorId, fullName, days có 7 phần tử")
    void testGetMySchedule_success() {
        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(sampleDoctor));

        DoctorScheduleMatrixResponse.DoctorScheduleRow row = DoctorScheduleMatrixResponse.DoctorScheduleRow.builder()
                .id(1L)
                .fullName("Bác sĩ Nguyễn Văn Minh")
                .title("Lương y")
                .specialty("Y học cổ truyền")
                .isActive(true)
                .schedules(Map.of(
                        "MON", List.of("MORNING"),
                        "TUE", List.of("AFTERNOON"),
                        "WED", List.of(),
                        "THU", List.of(),
                        "FRI", List.of(),
                        "SAT", List.of(),
                        "SUN", List.of()
                ))
                .build();

        DoctorScheduleMatrixResponse matrix = DoctorScheduleMatrixResponse.builder()
                .days(List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
                .doctors(List.of(row))
                .build();

        when(doctorScheduleAdminService.getWeeklyScheduleMatrix()).thenReturn(matrix);
        when(doctorDayOffRepository.findByDoctorIdAndOffDateGreaterThanEqualOrderByOffDateAsc(eq(1L), eq(today)))
                .thenReturn(List.of());

        MyScheduleResponse resp = staffService.getMySchedule(doctorUser);

        assertNotNull(resp);
        assertEquals(1L, resp.getDoctorId());
        assertEquals("Bác sĩ Nguyễn Văn Minh", resp.getFullName());
        assertEquals("Lương y", resp.getTitle());
        assertEquals("Y học cổ truyền", resp.getSpecialty());
        assertEquals(7, resp.getDays().size());
        assertEquals(List.of("MORNING"), resp.getShifts().get("MON"));
        assertTrue(resp.getDayOffs().isEmpty());
    }

    @Test
    @DisplayName("Case b: hồ sơ bác sĩ có ca FULL_DAY -> shifts của ngày đó chứa cả MORNING và AFTERNOON (tái sử dụng ma trận)")
    void testGetMySchedule_normalizesFullDayViaMatrix() {
        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(sampleDoctor));

        // Giả lập ma trận admin đã chuẩn hoá FULL_DAY thành MORNING + AFTERNOON
        DoctorScheduleMatrixResponse.DoctorScheduleRow row = DoctorScheduleMatrixResponse.DoctorScheduleRow.builder()
                .id(1L)
                .fullName("Bác sĩ Nguyễn Văn Minh")
                .schedules(Map.of(
                        "MON", List.of("MORNING", "AFTERNOON"),
                        "TUE", List.of(),
                        "WED", List.of(),
                        "THU", List.of(),
                        "FRI", List.of(),
                        "SAT", List.of(),
                        "SUN", List.of()
                ))
                .build();

        DoctorScheduleMatrixResponse matrix = DoctorScheduleMatrixResponse.builder()
                .days(List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
                .doctors(List.of(row))
                .build();

        when(doctorScheduleAdminService.getWeeklyScheduleMatrix()).thenReturn(matrix);
        when(doctorDayOffRepository.findByDoctorIdAndOffDateGreaterThanEqualOrderByOffDateAsc(eq(1L), eq(today)))
                .thenReturn(List.of());

        MyScheduleResponse resp = staffService.getMySchedule(doctorUser);

        assertNotNull(resp);
        List<String> monShifts = resp.getShifts().get("MON");
        assertNotNull(monShifts);
        assertTrue(monShifts.contains("MORNING"));
        assertTrue(monShifts.contains("AFTERNOON"));
        assertEquals(2, monShifts.size());
    }

    @Test
    @DisplayName("Case c: user không có hồ sơ bác sĩ -> AppException (422) kèm thông báo tiếng Việt")
    void testGetMySchedule_noDoctorProfile_throws422() {
        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> staffService.getMySchedule(doctorUser));

        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("không gắn với hồ sơ bác sĩ"));
    }

    @Test
    @DisplayName("Case d: ngày nghỉ sắp tới từ hôm nay trở đi được trả về đúng, repository lọc theo today")
    void testGetMySchedule_dayOffsFromTodayOnwards() {
        when(doctorRepository.findByUserId(10L)).thenReturn(Optional.of(sampleDoctor));

        DoctorScheduleMatrixResponse matrix = DoctorScheduleMatrixResponse.builder()
                .days(List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
                .doctors(List.of())
                .build();
        when(doctorScheduleAdminService.getWeeklyScheduleMatrix()).thenReturn(matrix);

        DoctorDayOff offToday = DoctorDayOff.builder()
                .id(101L)
                .doctor(sampleDoctor)
                .offDate(today)
                .period(Period.MORNING)
                .reason("Khám sức khoẻ")
                .createdAt(LocalDateTime.of(2026, 11, 16, 10, 0))
                .build();

        DoctorDayOff offFuture = DoctorDayOff.builder()
                .id(102L)
                .doctor(sampleDoctor)
                .offDate(today.plusDays(2))
                .period(Period.FULL_DAY)
                .reason("Việc gia đình")
                .createdAt(LocalDateTime.of(2026, 11, 16, 11, 0))
                .build();

        // Repository được gọi với today, trả về 2 bản ghi từ today trở đi
        when(doctorDayOffRepository.findByDoctorIdAndOffDateGreaterThanEqualOrderByOffDateAsc(1L, today))
                .thenReturn(List.of(offToday, offFuture));

        MyScheduleResponse resp = staffService.getMySchedule(doctorUser);

        assertNotNull(resp);
        assertEquals(2, resp.getDayOffs().size());
        assertEquals(today, resp.getDayOffs().get(0).getOffDate());
        assertEquals(Period.MORNING, resp.getDayOffs().get(0).getPeriod());
        assertEquals("Khám sức khoẻ", resp.getDayOffs().get(0).getReason());
        assertEquals(today.plusDays(2), resp.getDayOffs().get(1).getOffDate());
        assertEquals(Period.FULL_DAY, resp.getDayOffs().get(1).getPeriod());

        verify(doctorDayOffRepository).findByDoctorIdAndOffDateGreaterThanEqualOrderByOffDateAsc(1L, today);
    }
}
