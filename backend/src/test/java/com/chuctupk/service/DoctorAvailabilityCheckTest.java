package com.chuctupk.service;

import com.chuctupk.dto.response.SlotAvailabilityDto;
import com.chuctupk.dto.response.StaffAvailabilityResponse;
import com.chuctupk.entity.Doctor;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.User;
import com.chuctupk.entity.enums.DayOfWeek;
import com.chuctupk.entity.enums.Period;
import com.chuctupk.entity.enums.Role;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.*;
import com.chuctupk.service.impl.DoctorAssignmentServiceImpl;
import com.chuctupk.service.impl.PatientPortalServiceImpl;
import com.chuctupk.service.impl.StaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DoctorAvailabilityCheckTest {

    private DoctorScheduleRepository doctorScheduleRepository;
    private DoctorDayOffRepository doctorDayOffRepository;
    private DoctorRepository doctorRepository;
    private AppointmentRepository appointmentRepository;
    private TimeSlotRepository timeSlotRepository;
    private AppointmentSeatRepository appointmentSeatRepository;
    private ClinicSettingService clinicSettingService;
    private SlotPolicy slotPolicy;
    private Clock clock;

    private DoctorAssignmentServiceImpl doctorAssignmentService;
    private Doctor sampleDoctor;
    private TimeSlot morningSlot;
    private TimeSlot afternoonSlot;
    private final LocalDate nextMonday = LocalDate.of(2026, 11, 23);

    @BeforeEach
    void setUp() {
        doctorScheduleRepository = mock(DoctorScheduleRepository.class);
        doctorDayOffRepository = mock(DoctorDayOffRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        timeSlotRepository = mock(TimeSlotRepository.class);
        appointmentSeatRepository = mock(AppointmentSeatRepository.class);
        clinicSettingService = mock(ClinicSettingService.class);
        slotPolicy = mock(SlotPolicy.class);
        clock = Clock.fixed(Instant.parse("2026-11-17T08:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        doctorAssignmentService = new DoctorAssignmentServiceImpl(
                doctorScheduleRepository,
                doctorDayOffRepository,
                doctorRepository,
                appointmentRepository
        );

        User doctorUser = User.builder()
                .id(10L)
                .role(Role.DOCTOR)
                .isActive(true)
                .build();

        sampleDoctor = Doctor.builder()
                .id(1L)
                .user(doctorUser)
                .fullName("Bác sĩ A")
                .isActive(true)
                .build();

        morningSlot = TimeSlot.builder()
                .id(100L)
                .slotLabel("08:00 - 08:30")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(8, 30))
                .period(Period.MORNING)
                .maxPatients(3)
                .isActive(true)
                .build();

        afternoonSlot = TimeSlot.builder()
                .id(101L)
                .slotLabel("13:30 - 14:00")
                .startTime(LocalTime.of(13, 30))
                .endTime(LocalTime.of(14, 0))
                .period(Period.AFTERNOON)
                .maxPatients(3)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Test 7 & 8: findOnDutyDoctorIds & hasDoctorOnDuty phản ánh đúng lịch trực và ngày nghỉ")
    void testDoctorOnDutyLogic() {
        // Thứ 2 sáng có BS 1 trực
        when(doctorScheduleRepository.findScheduledDoctorIds(DayOfWeek.MON, Period.MORNING))
                .thenReturn(List.of(1L));
        // Thứ 2 chiều không có BS nào
        when(doctorScheduleRepository.findScheduledDoctorIds(DayOfWeek.MON, Period.AFTERNOON))
                .thenReturn(List.of());

        when(doctorDayOffRepository.findDoctorIdsOffOnDateAndPeriod(nextMonday, Period.MORNING))
                .thenReturn(List.of());
        when(doctorDayOffRepository.findDoctorIdsOffOnDateAndPeriod(nextMonday, Period.AFTERNOON))
                .thenReturn(List.of());

        when(doctorRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(sampleDoctor));

        // Ca sáng: có bác sĩ trực
        assertTrue(doctorAssignmentService.hasDoctorOnDuty(nextMonday, morningSlot));
        assertEquals(List.of(1L), doctorAssignmentService.findOnDutyDoctorIds(nextMonday, Period.MORNING));

        // Ca chiều: không có bác sĩ trực
        assertFalse(doctorAssignmentService.hasDoctorOnDuty(nextMonday, afternoonSlot));
        assertTrue(doctorAssignmentService.findOnDutyDoctorIds(nextMonday, Period.AFTERNOON).isEmpty());

        // Test 9: Thêm ngày nghỉ cho BS 1 vào sáng thứ Hai -> hasDoctorOnDuty chuyển thành false
        when(doctorDayOffRepository.findDoctorIdsOffOnDateAndPeriod(nextMonday, Period.MORNING))
                .thenReturn(List.of(1L));

        assertFalse(doctorAssignmentService.hasDoctorOnDuty(nextMonday, morningSlot));
        assertTrue(doctorAssignmentService.findOnDutyDoctorIds(nextMonday, Period.MORNING).isEmpty());
    }

    @Test
    @DisplayName("Test 10: pickDoctor ném lỗi 422 khi không có bác sĩ trực")
    void testPickDoctor_throws422WhenNoDoctor() {
        when(doctorScheduleRepository.findScheduledDoctorIds(any(), any())).thenReturn(List.of());
        when(doctorDayOffRepository.findDoctorIdsOffOnDateAndPeriod(any(), any())).thenReturn(List.of());

        AppException ex = assertThrows(AppException.class, () ->
                doctorAssignmentService.pickDoctor(nextMonday, morningSlot)
        );
        assertEquals(422, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("chưa có bác sĩ trực"));
    }

    @Test
    @DisplayName("PatientPortalServiceImpl.getAvailability: gắn reason=NO_DOCTOR và available=false khi không có bác sĩ trực")
    void testPatientPortalAvailability_noDoctorReason() {
        when(timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(morningSlot, afternoonSlot));

        when(clinicSettingService.getSlotCapacity()).thenReturn(6);
        when(appointmentSeatRepository.countByAppointmentDateAndTimeSlotId(any(), any())).thenReturn(0L);

        when(slotPolicy.evaluate(any(), any(), any(), eq(SlotPolicy.BookingChannel.PATIENT_PORTAL), anyInt()))
                .thenReturn(SlotPolicy.SlotStatus.OK);

        // Giả lập ca sáng CÓ bác sĩ, ca chiều KHÔNG CÓ bác sĩ
        DoctorAssignmentService mockDoctorAssignmentService = mock(DoctorAssignmentService.class);
        when(mockDoctorAssignmentService.hasDoctorOnDuty(nextMonday, morningSlot)).thenReturn(true);
        when(mockDoctorAssignmentService.hasDoctorOnDuty(nextMonday, afternoonSlot)).thenReturn(false);

        PatientPortalServiceImpl portalService = new PatientPortalServiceImpl(
                mock(UserRepository.class),
                mock(PatientRepository.class),
                appointmentRepository,
                appointmentSeatRepository,
                mock(IdempotencyKeyRepository.class),
                mock(MedicalRecordRepository.class),
                timeSlotRepository,
                mock(SequenceGeneratorService.class),
                clock,
                clinicSettingService,
                slotPolicy,
                mockDoctorAssignmentService,
                mock(NotificationService.class)
        );

        Map<String, Object> result = portalService.getAvailability(nextMonday);
        assertNotNull(result);

        @SuppressWarnings("unchecked")
        List<SlotAvailabilityDto> slots =
                (List<SlotAvailabilityDto>) result.get("slots");
        assertEquals(2, slots.size());

        SlotAvailabilityDto morningDto = slots.get(0);
        assertTrue(morningDto.available());
        assertNull(morningDto.reason());

        SlotAvailabilityDto afternoonDto = slots.get(1);
        assertFalse(afternoonDto.available());
        assertEquals("NO_DOCTOR", afternoonDto.reason());
    }

    @Test
    @DisplayName("StaffServiceImpl.getAvailability: gắn reason=NO_DOCTOR và dùng clinicSettingService.getSlotCapacity()")
    void testStaffAvailability_noDoctorAndCapacity() {
        when(timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc())
                .thenReturn(List.of(morningSlot, afternoonSlot));

        when(clinicSettingService.getSlotCapacity()).thenReturn(5);
        when(appointmentSeatRepository.countByAppointmentDateAndTimeSlotId(any(), any())).thenReturn(0L);

        DoctorAssignmentService mockDoctorAssignmentService = mock(DoctorAssignmentService.class);
        when(mockDoctorAssignmentService.hasDoctorOnDuty(nextMonday, morningSlot)).thenReturn(true);
        when(mockDoctorAssignmentService.hasDoctorOnDuty(nextMonday, afternoonSlot)).thenReturn(false);

        StaffServiceImpl staffService = new StaffServiceImpl(
                appointmentRepository,
                mock(CheckinRepository.class),
                mock(PatientRepository.class),
                timeSlotRepository,
                mock(MedicalRecordRepository.class),
                appointmentSeatRepository,
                clinicSettingService,
                slotPolicy,
                mockDoctorAssignmentService,
                doctorRepository,
                mock(DoctorScheduleAdminService.class),
                doctorDayOffRepository,
                mock(NotificationService.class),
                mock(SequenceGeneratorService.class),
                clock
        );

        StaffAvailabilityResponse response = staffService.getAvailability(nextMonday);
        assertNotNull(response);
        assertEquals(2, response.slots().size());

        var morningDto = response.slots().get(0);
        assertEquals(5, morningDto.capacity());
        assertTrue(morningDto.available());
        assertNull(morningDto.reason());

        var afternoonDto = response.slots().get(1);
        assertEquals(5, afternoonDto.capacity());
        assertFalse(afternoonDto.available());
        assertEquals("NO_DOCTOR", afternoonDto.reason());
    }
}
