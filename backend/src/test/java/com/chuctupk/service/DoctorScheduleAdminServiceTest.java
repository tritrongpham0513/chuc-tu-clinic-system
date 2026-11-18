package com.chuctupk.service;

import com.chuctupk.dto.request.CreateDoctorDayOffRequest;
import com.chuctupk.dto.request.ScheduleItemRequest;
import com.chuctupk.dto.request.UpdateDoctorScheduleRequest;
import com.chuctupk.dto.response.CreateDoctorDayOffResult;
import com.chuctupk.dto.response.DoctorDayOffDto;
import com.chuctupk.dto.response.DoctorScheduleMatrixResponse;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.DoctorDayOff;
import com.chuctupk.entity.DoctorSchedule;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.repository.DoctorDayOffRepository;
import com.chuctupk.repository.DoctorRepository;
import com.chuctupk.repository.DoctorScheduleRepository;
import com.chuctupk.service.impl.DoctorScheduleAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DoctorScheduleAdminServiceTest {

    private DoctorRepository doctorRepository;
    private DoctorScheduleRepository doctorScheduleRepository;
    private DoctorDayOffRepository doctorDayOffRepository;
    private AppointmentRepository appointmentRepository;
    private Clock clock;
    private DoctorScheduleAdminServiceImpl service;

    private Doctor sampleDoctor;
    private final LocalDate today = LocalDate.of(2026, 11, 17);

    @BeforeEach
    void setUp() {
        doctorRepository = mock(DoctorRepository.class);
        doctorScheduleRepository = mock(DoctorScheduleRepository.class);
        doctorDayOffRepository = mock(DoctorDayOffRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        clock = Clock.fixed(Instant.parse("2026-11-17T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        service = new DoctorScheduleAdminServiceImpl(
                doctorRepository,
                doctorScheduleRepository,
                doctorDayOffRepository,
                appointmentRepository,
                clock
        );

        sampleDoctor = Doctor.builder()
                .id(1L)
                .fullName("Bác sĩ Nguyễn Văn A")
                .title("Lương y")
                .specialty("Y học cổ truyền")
                .isActive(true)
                .sortOrder(1)
                .build();
    }

    @Test
    @DisplayName("getWeeklyScheduleMatrix: chuẩn hoá FULL_DAY thành cả MORNING và AFTERNOON")
    void testGetWeeklyScheduleMatrix_normalizesFullDay() {
        when(doctorRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(sampleDoctor));

        DoctorSchedule schedMonday = DoctorSchedule.builder()
                .id(101L)
                .doctor(sampleDoctor)
                .dayOfWeek(DayOfWeek.MON)
                .period(Period.FULL_DAY)
                .isActive(true)
                .build();

        DoctorSchedule schedFriday = DoctorSchedule.builder()
                .id(102L)
                .doctor(sampleDoctor)
                .dayOfWeek(DayOfWeek.FRI)
                .period(Period.MORNING)
                .isActive(true)
                .build();

        when(doctorScheduleRepository.findByDoctorIdAndIsActiveTrue(1L))
                .thenReturn(List.of(schedMonday, schedFriday));

        DoctorScheduleMatrixResponse resp = service.getWeeklyScheduleMatrix();

        assertNotNull(resp);
        assertEquals(7, resp.getDays().size());
        assertEquals(1, resp.getDoctors().size());

        DoctorScheduleMatrixResponse.DoctorScheduleRow row = resp.getDoctors().get(0);
        assertEquals("Bác sĩ Nguyễn Văn A", row.getFullName());

        List<String> monPeriods = row.getSchedules().get("MON");
        assertEquals(List.of("MORNING", "AFTERNOON"), monPeriods);

        List<String> friPeriods = row.getSchedules().get("FRI");
        assertEquals(List.of("MORNING"), friPeriods);

        List<String> sunPeriods = row.getSchedules().get("SUN");
        assertTrue(sunPeriods.isEmpty());
    }

    @Test
    @DisplayName("updateDoctorSchedule: upsert đúng và tách FULL_DAY trong payload")
    void testUpdateDoctorSchedule_upsertAndNormalize() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));

        DoctorSchedule existingMonMorning = DoctorSchedule.builder()
                .id(201L)
                .doctor(sampleDoctor)
                .dayOfWeek(DayOfWeek.MON)
                .period(Period.MORNING)
                .isActive(true)
                .build();

        DoctorSchedule existingOldFullDay = DoctorSchedule.builder()
                .id(202L)
                .doctor(sampleDoctor)
                .dayOfWeek(DayOfWeek.WED)
                .period(Period.FULL_DAY)
                .isActive(true)
                .build();

        when(doctorScheduleRepository.findByDoctorId(1L))
                .thenReturn(new ArrayList<>(List.of(existingMonMorning, existingOldFullDay)));

        // Payload có FULL_DAY ở thứ Hai
        List<ScheduleItemRequest> items = List.of(
                new ScheduleItemRequest(DayOfWeek.MON, Period.FULL_DAY, true),
                new ScheduleItemRequest(DayOfWeek.TUE, Period.MORNING, true)
        );
        UpdateDoctorScheduleRequest req = new UpdateDoctorScheduleRequest(items);

        when(doctorRepository.findByIsActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(sampleDoctor));
        when(doctorScheduleRepository.save(any(DoctorSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(doctorScheduleRepository.findByDoctorIdAndIsActiveTrue(1L)).thenReturn(List.of());

        service.updateDoctorSchedule(1L, req);

        // Verify rằng existingMonMorning được giữ và cập nhật
        verify(doctorScheduleRepository, atLeastOnce()).save(existingMonMorning);

        // Verify rằng existingOldFullDay bị xoá vì không còn trong payload và là FULL_DAY cũ
        verify(doctorScheduleRepository).delete(existingOldFullDay);

        // Verify có flush
        verify(doctorScheduleRepository).flush();
    }

    @Test
    @DisplayName("updateDoctorSchedule: chặn payload có ca khám trùng lặp")
    void testUpdateDoctorSchedule_duplicateThrows() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));

        List<ScheduleItemRequest> items = List.of(
                new ScheduleItemRequest(DayOfWeek.MON, Period.MORNING, true),
                new ScheduleItemRequest(DayOfWeek.MON, Period.MORNING, false)
        );
        UpdateDoctorScheduleRequest req = new UpdateDoctorScheduleRequest(items);

        AppException ex = assertThrows(AppException.class, () -> service.updateDoctorSchedule(1L, req));
        assertEquals(400, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("trùng lặp"));
    }

    @Test
    @DisplayName("createDayOff: thành công khi thêm MORNING và AFTERNOON cùng ngày")
    void testCreateDayOff_morningAndAfternoonSuccess() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));
        LocalDate offDate = today.plusDays(2);

        when(doctorDayOffRepository.findByDoctorIdAndOffDate(1L, offDate))
                .thenReturn(List.of()); // chưa có ngày nghỉ nào

        when(doctorDayOffRepository.save(any(DoctorDayOff.class))).thenAnswer(inv -> {
            DoctorDayOff d = inv.getArgument(0);
            d.setId(999L);
            return d;
        });

        when(appointmentRepository.countByDoctorIdAndAppointmentDateAndStatusIn(eq(1L), eq(offDate), any()))
                .thenReturn(3L);

        CreateDoctorDayOffRequest req = new CreateDoctorDayOffRequest(
                1L, offDate, Period.MORNING, "Bận việc gia đình"
        );

        CreateDoctorDayOffResult res = service.createDayOff(req);

        assertNotNull(res);
        assertEquals(3, res.getAffectedAppointments());
        assertEquals("Bác sĩ Nguyễn Văn A", res.getItem().getDoctorName());
        assertEquals(Period.MORNING, res.getItem().getPeriod());
    }

    @Test
    @DisplayName("createDayOff: chặn 409 khi đã có FULL_DAY hoặc thêm trùng ca")
    void testCreateDayOff_conflictBranches() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));
        LocalDate offDate = today.plusDays(2);

        // Branch 1: đã có FULL_DAY -> chặn
        when(doctorDayOffRepository.findByDoctorIdAndOffDate(1L, offDate))
                .thenReturn(List.of(DoctorDayOff.builder().period(Period.FULL_DAY).offDate(offDate).build()));

        CreateDoctorDayOffRequest reqMorning = new CreateDoctorDayOffRequest(1L, offDate, Period.MORNING, "Nghỉ");
        AppException ex1 = assertThrows(AppException.class, () -> service.createDayOff(reqMorning));
        assertEquals(409, ex1.getStatusCode());
        assertTrue(ex1.getMessage().contains("đã có lịch nghỉ cả ngày"));

        // Branch 2: đang thêm FULL_DAY khi đã có ca khác -> chặn
        when(doctorDayOffRepository.findByDoctorIdAndOffDate(1L, offDate))
                .thenReturn(List.of(DoctorDayOff.builder().period(Period.MORNING).offDate(offDate).build()));

        CreateDoctorDayOffRequest reqFull = new CreateDoctorDayOffRequest(1L, offDate, Period.FULL_DAY, "Nghỉ");
        AppException ex2 = assertThrows(AppException.class, () -> service.createDayOff(reqFull));
        assertEquals(409, ex2.getStatusCode());
        assertTrue(ex2.getMessage().contains("không thể đăng ký thêm nghỉ cả ngày"));

        // Branch 3: thêm trùng đúng ca MORNING -> chặn
        AppException ex3 = assertThrows(AppException.class, () -> service.createDayOff(reqMorning));
        assertEquals(409, ex3.getStatusCode());
        assertTrue(ex3.getMessage().contains("đã có lịch nghỉ ca này"));
    }

    @Test
    @DisplayName("createDayOff: ngày nghỉ trong quá khứ ném lỗi 400")
    void testCreateDayOff_pastDate() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));
        LocalDate pastDate = today.minusDays(1);

        CreateDoctorDayOffRequest req = new CreateDoctorDayOffRequest(1L, pastDate, Period.MORNING, "Nghỉ");
        AppException ex = assertThrows(AppException.class, () -> service.createDayOff(req));
        assertEquals(400, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("quá khứ"));
    }

    @Test
    @DisplayName("deleteDayOff: xoá thành công khi tồn tại, ném 404 khi không tìm thấy")
    void testDeleteDayOff() {
        DoctorDayOff existing = DoctorDayOff.builder().id(55L).build();
        when(doctorDayOffRepository.findById(55L)).thenReturn(Optional.of(existing));

        service.deleteDayOff(55L);
        verify(doctorDayOffRepository).delete(existing);

        when(doctorDayOffRepository.findById(99L)).thenReturn(Optional.empty());
        AppException ex = assertThrows(AppException.class, () -> service.deleteDayOff(99L));
        assertEquals(404, ex.getStatusCode());
    }
}
