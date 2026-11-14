package com.chuctupk.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthAndMetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health - Trả về thành công trạng thái hệ thống")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/v1/meta - Trả về đầy đủ cấu hình nền tảng 14 trường")
    void testMetaEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/meta").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.today").isNotEmpty())
                .andExpect(jsonPath("$.data.otp").exists())
                .andExpect(jsonPath("$.data.serviceTypes", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.slotCapacity").value(6))
                .andExpect(jsonPath("$.data.seats").value(5))
                .andExpect(jsonPath("$.data.slotMinutes").value(60))
                .andExpect(jsonPath("$.data.userMaxDays").value(30))
                .andExpect(jsonPath("$.data.counterMaxDays").value(7))
                .andExpect(jsonPath("$.data.priorityEarlyMinutes").value(15))
                .andExpect(jsonPath("$.data.maxProfiles").value(10))
                .andExpect(jsonPath("$.data.relationships.BAN_THAN").value("Bản thân"))
                .andExpect(jsonPath("$.data.statusLabels.BOOKED").value("Chưa đến"))
                .andExpect(jsonPath("$.data.cancelReasons", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.postCategories").isArray());
    }
}

