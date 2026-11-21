package com.chuctupk.it;

import com.chuctupk.service.ClinicSettingService;
import com.chuctupk.service.PatientPortalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CapacityConsistencyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClinicSettingService clinicSettingService;

    @Autowired
    private com.chuctupk.service.CapacityResolver capacityResolver;

    @Autowired
    private com.chuctupk.repository.TimeSlotRepository timeSlotRepository;

    @Autowired
    private PatientPortalService patientPortalService;

    @Autowired
    private com.chuctupk.service.TimeSlotService timeSlotService;

    @Autowired
    private com.chuctupk.repository.DoctorRepository doctorRepository;

    @Autowired
    private com.chuctupk.repository.DoctorScheduleRepository doctorScheduleRepository;

    @Test
    @DisplayName("C1: Đổi slot_capacity trong cấu hình thành 4 -> getAvailability tính remaining theo 4")
    void testSlotCapacityDynamic() {
        int originalCapacity = clinicSettingService.getSlotCapacity();

        try {
            clinicSettingService.setSettingValue("slot_capacity", "4");
            assertThat(clinicSettingService.getSlotCapacity()).isEqualTo(4);

            LocalDate targetDate = LocalDate.now().plusDays(2);
            while (targetDate.getDayOfWeek().getValue() >= 6) {
                targetDate = targetDate.plusDays(1);
            }
            Map<String, Object> avail = patientPortalService.getAvailability(targetDate);
            @SuppressWarnings("unchecked")
            List<com.chuctupk.dto.response.SlotAvailabilityDto> slots =
                    (List<com.chuctupk.dto.response.SlotAvailabilityDto>) avail.get("slots");

            assertThat(slots).isNotEmpty();
            // Khung giờ chưa có ai đặt phải có remaining <= 4
            for (com.chuctupk.dto.response.SlotAvailabilityDto slot : slots) {
                assertThat(slot.remaining()).isLessThanOrEqualTo(4);
            }
        } finally {
            clinicSettingService.setSettingValue("slot_capacity", String.valueOf(originalCapacity));
        }
    }

    @Test
    @DisplayName("C1: Hàng đợi công khai hiển thị đúng số ghế (seats) theo cấu hình phòng khám")
    void testQueueSeatsCountMatchesConfiguration() throws Exception {
        int seats = clinicSettingService.getSeats();

        mockMvc.perform(get("/api/v1/staff/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats.length()").value(seats));
    }

    @Test
    @DisplayName("B2: So sánh /time-slots và /me/availability cùng ngày -> số lượng slot khả dụng và remaining đồng nhất")
    void testCompareTimeSlotsAndAvailabilityConsistency() {
        LocalDate targetDate = LocalDate.now().plusDays(2);
        while (targetDate.getDayOfWeek().getValue() >= 6) {
            targetDate = targetDate.plusDays(1);
        }
        com.chuctupk.entity.enums.DayOfWeek dow = com.chuctupk.entity.enums.DayOfWeek.from(targetDate.getDayOfWeek());

        // Đảm bảo có bác sĩ trực cả ngày để time-slots hiển thị các khung giờ khả dụng
        com.chuctupk.entity.Doctor doc = doctorRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()))
                .findFirst()
                .orElse(null);
        if (doc != null) {
            boolean hasSchedule = doctorScheduleRepository.findAll().stream()
                    .anyMatch(s -> s.getDoctor().getId().equals(doc.getId())
                            && s.getDayOfWeek() == dow
                            && (s.getPeriod() == com.chuctupk.entity.enums.Period.FULL_DAY || s.getPeriod() == com.chuctupk.entity.enums.Period.MORNING));
            if (!hasSchedule) {
                doctorScheduleRepository.save(com.chuctupk.entity.DoctorSchedule.builder()
                        .doctor(doc)
                        .dayOfWeek(dow)
                        .period(com.chuctupk.entity.enums.Period.FULL_DAY)
                        .isActive(true)
                        .build());
            }
        }

        List<com.chuctupk.dto.response.TimeSlotResponse> timeSlots = timeSlotService.getAvailableTimeSlots(targetDate, null);
        Map<String, Object> avail = patientPortalService.getAvailability(targetDate);
        @SuppressWarnings("unchecked")
        List<com.chuctupk.dto.response.SlotAvailabilityDto> availSlots =
                (List<com.chuctupk.dto.response.SlotAvailabilityDto>) avail.get("slots");

        assertThat(availSlots).isNotEmpty();

        // Lọc các slot khả dụng (available = true) từ /me/availability
        List<com.chuctupk.dto.response.SlotAvailabilityDto> activeAvailSlots = availSlots.stream()
                .filter(com.chuctupk.dto.response.SlotAvailabilityDto::available)
                .toList();

        assertThat(timeSlots).hasSameSizeAs(activeAvailSlots);

        Map<String, com.chuctupk.dto.response.SlotAvailabilityDto> availMap = activeAvailSlots.stream()
                .collect(java.util.stream.Collectors.toMap(com.chuctupk.dto.response.SlotAvailabilityDto::id, s -> s));

        for (com.chuctupk.dto.response.TimeSlotResponse ts : timeSlots) {
            com.chuctupk.dto.response.SlotAvailabilityDto matching = availMap.get(String.valueOf(ts.getId()));
            assertThat(matching).isNotNull();
            assertThat(ts.getRemaining()).isEqualTo(matching.remaining());
            com.chuctupk.entity.TimeSlot slot = timeSlotRepository.findById(ts.getId()).orElseThrow();
            assertThat(matching.capacity()).isEqualTo(capacityResolver.effectiveCapacity(targetDate, slot));
        }
    }
}
