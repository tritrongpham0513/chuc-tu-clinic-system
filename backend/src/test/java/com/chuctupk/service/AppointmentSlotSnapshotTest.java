package com.chuctupk.service;

import com.chuctupk.dto.response.AppointmentDto;
import com.chuctupk.entity.Appointment;
import com.chuctupk.entity.Patient;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.entity.enums.AppointmentSource;
import com.chuctupk.entity.enums.AppointmentStatus;
import com.chuctupk.entity.enums.Period;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Sprint 6 — Appointment Slot Snapshot Tests (Ticket 2, 3, 8)")
class AppointmentSlotSnapshotTest {

    @Test
    @DisplayName("Test 8: Sửa giờ khung -> phiếu cũ vẫn trả slotStart và slotEnd cũ qua AppointmentDto (Kèm bằng chứng)")
    void test8_UpdateSlotTime_OldAppointmentKeepsOldSnapshotTime() {
        // 1. Tạo khung giờ ban đầu: 08:00 - 09:00
        TimeSlot timeSlot = TimeSlot.builder()
                .id(1L)
                .slotLabel("08:00 - 09:00")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .period(Period.MORNING)
                .maxPatients(5)
                .sortOrder(1)
                .isActive(true)
                .build();

        Patient patient = Patient.builder()
                .id(100L)
                .patientCode("CT-BN-26-0001")
                .fullName("Nguyễn Văn An")
                .phone("0901234567")
                .yearOfBirth(1955)
                .build();

        // 2. Tạo phiếu khám cũ vào năm ngoái / trước khi sửa khung: có snapshot giờ lúc đặt
        Appointment appointment = Appointment.builder()
                .id(501L)
                .bookingCode("PKCT-20251120-0001")
                .patient(patient)
                .appointmentDate(LocalDate.of(2025, 11, 20))
                .timeSlot(timeSlot)
                .slotStartSnapshot(LocalTime.of(8, 0))
                .slotEndSnapshot(LocalTime.of(9, 0))
                .status(AppointmentStatus.COMPLETED)
                .source(AppointmentSource.WEB)
                .build();

        // 3. Đọc DTO TRƯỚC KHI SỬA KHUNG GIỜ
        AppointmentDto dtoBefore = AppointmentDto.from(appointment);
        System.out.println("=================================================================");
        System.out.println("[BẰNG CHỨNG TEST 8 - TRƯỚC KHI SỬA KHUNG GIỜ]");
        System.out.println("TimeSlot ID " + timeSlot.getId() + ": " + timeSlot.getStartTime() + " - " + timeSlot.getEndTime());
        System.out.println("Appointment ID " + appointment.getId() + " - slotStart: " + dtoBefore.slotStart() + ", slotEnd: " + dtoBefore.slotEnd());
        System.out.println("=================================================================");

        assertThat(dtoBefore.slotStart()).isEqualTo("08:00");
        assertThat(dtoBefore.slotEnd()).isEqualTo("09:00");

        // 4. Admin SỬA GIỜ KHUNG từ 08:00-09:00 thành 08:30-09:30
        timeSlot.setStartTime(LocalTime.of(8, 30));
        timeSlot.setEndTime(LocalTime.of(9, 30));
        timeSlot.setSlotLabel("08:30 - 09:30");

        // 5. Đọc lại DTO SAU KHI SỬA KHUNG GIỜ
        AppointmentDto dtoAfter = AppointmentDto.from(appointment);
        System.out.println("=================================================================");
        System.out.println("[BẰNG CHỨNG TEST 8 - SAU KHI SỬA KHUNG GIỜ THÀNH 08:30 - 09:30]");
        System.out.println("TimeSlot ID " + timeSlot.getId() + " (mới): " + timeSlot.getStartTime() + " - " + timeSlot.getEndTime());
        System.out.println("Appointment ID " + appointment.getId() + " - slotStart: " + dtoAfter.slotStart() + ", slotEnd: " + dtoAfter.slotEnd());
        System.out.println("KẾT QUẢ: Phiếu cũ vẫn giữ nguyên giờ 08:00 - 09:00, không bị đổi theo khung mới!");
        System.out.println("=================================================================");

        // BẢO ĐẢM TUYỆT ĐỐI: DTO của phiếu cũ vẫn là 08:00 và 09:00
        assertThat(dtoAfter.slotStart()).isEqualTo("08:00");
        assertThat(dtoAfter.slotEnd()).isEqualTo("09:00");
    }

    @Test
    @DisplayName("Test fallback: Phiếu cũ chưa có snapshot -> Đọc từ TimeSlot")
    void testFallback_WhenSnapshotNull_ReadsFromTimeSlot() {
        TimeSlot timeSlot = TimeSlot.builder()
                .id(2L)
                .slotLabel("10:00 - 11:00")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .period(Period.MORNING)
                .build();

        Patient patient = Patient.builder()
                .id(101L)
                .patientCode("CT-BN-26-0002")
                .fullName("Trần Thị Hoa")
                .phone("0912345678")
                .build();

        // Phiếu không có snapshot (null)
        Appointment appointment = Appointment.builder()
                .id(502L)
                .bookingCode("PKCT-20251121-0002")
                .patient(patient)
                .appointmentDate(LocalDate.of(2025, 11, 21))
                .timeSlot(timeSlot)
                .slotStartSnapshot(null)
                .slotEndSnapshot(null)
                .status(AppointmentStatus.CONFIRMED)
                .build();

        AppointmentDto dto = AppointmentDto.from(appointment);
        assertThat(dto.slotStart()).isEqualTo("10:00");
        assertThat(dto.slotEnd()).isEqualTo("11:00");
    }

    @Test
    @DisplayName("Test snapshot khi tạo phiếu và đổi giờ")
    void testSnapshot_WhenCreatedAndRescheduled() {
        TimeSlot slot1 = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();

        Appointment appt = Appointment.builder()
                .id(601L)
                .bookingCode("PKCT-20261120-0001")
                .timeSlot(slot1)
                .slotStartSnapshot(slot1.getStartTime())
                .slotEndSnapshot(slot1.getEndTime())
                .build();

        assertThat(appt.getSlotStartSnapshot()).isEqualTo(LocalTime.of(8, 0));
        assertThat(appt.getSlotEndSnapshot()).isEqualTo(LocalTime.of(9, 0));

        // Đổi giờ sang khung 13:00 - 14:00
        TimeSlot slot2 = TimeSlot.builder()
                .id(2L)
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(14, 0))
                .build();

        appt.setTimeSlot(slot2);
        appt.setSlotStartSnapshot(slot2.getStartTime());
        appt.setSlotEndSnapshot(slot2.getEndTime());

        assertThat(appt.getSlotStartSnapshot()).isEqualTo(LocalTime.of(13, 0));
        assertThat(appt.getSlotEndSnapshot()).isEqualTo(LocalTime.of(14, 0));

        AppointmentDto dto = AppointmentDto.from(appt);
        assertThat(dto.slotStart()).isEqualTo("13:00");
        assertThat(dto.slotEnd()).isEqualTo("14:00");
    }
}
