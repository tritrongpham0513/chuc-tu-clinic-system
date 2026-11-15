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
    private PatientPortalService patientPortalService;

    @Test
    @DisplayName("C1: Đổi slot_capacity trong cấu hình thành 4 -> getAvailability tính remaining theo 4")
    void testSlotCapacityDynamic() {
        int originalCapacity = clinicSettingService.getSlotCapacity();

        try {
            clinicSettingService.setSettingValue("slot_capacity", "4");
            assertThat(clinicSettingService.getSlotCapacity()).isEqualTo(4);

            LocalDate targetDate = LocalDate.now().plusDays(2);
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
}
