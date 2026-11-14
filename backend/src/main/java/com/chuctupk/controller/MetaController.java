package com.chuctupk.controller;

import com.chuctupk.config.ClinicProperties;
import com.chuctupk.dto.response.ApiResponse;
import com.chuctupk.entity.ArticleCategory;
import com.chuctupk.entity.ClinicSetting;
import com.chuctupk.entity.TimeSlot;
import com.chuctupk.repository.ArticleCategoryRepository;
import com.chuctupk.repository.ClinicSettingRepository;
import com.chuctupk.repository.TimeSlotRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Tag(name = "Meta", description = "Thông tin cấu hình và dữ liệu nền tảng của phòng khám")
@RestController
@RequestMapping("/api/v1/meta")
@RequiredArgsConstructor
public class MetaController {

    private final ClinicSettingRepository settingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ArticleCategoryRepository categoryRepository;
    private final ClinicProperties clinicProperties;
    private final Clock clock;

    @Operation(summary = "Lấy dữ liệu cấu hình hệ thống phục vụ hiển thị cho Frontend")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMeta() {
        Map<String, String> settings = new HashMap<>();
        for (ClinicSetting s : settingRepository.findAll()) {
            if (s.getSettingKey() != null && s.getSettingValue() != null) {
                settings.put(s.getSettingKey(), s.getSettingValue());
            }
        }

        LocalDate today = LocalDate.now(clock);

        // 1. Dịch vụ khám
        List<Map<String, String>> serviceTypes = List.of(
                Map.of("value", "CHAM_CUU", "label", "Châm cứu"),
                Map.of("value", "XOA_BOP_BAM_HUYET", "label", "Xoa bóp — bấm huyệt"),
                Map.of("value", "BOC_THUOC", "label", "Bốc thuốc — thuốc Nam, thuốc Bắc"),
                Map.of("value", "VAT_LY_TRI_LIEU", "label", "Vật lý trị liệu — phục hồi chức năng"),
                Map.of("value", "CHUA_RO", "label", "Chưa rõ — nhờ bác sĩ khám và chỉ định")
        );

        // 2. Khung giờ khám đang hoạt động
        List<TimeSlot> dbSlots = timeSlotRepository.findByIsActiveTrueOrderBySortOrderAsc();
        List<Map<String, Object>> slots = new ArrayList<>();
        for (TimeSlot ts : dbSlots) {
            String start = ts.getStartTime() != null ? ts.getStartTime().toString().substring(0, 5) : "";
            String end = ts.getEndTime() != null ? ts.getEndTime().toString().substring(0, 5) : "";
            slots.add(Map.of(
                    "id", String.valueOf(ts.getId()),
                    "label", ts.getSlotLabel() != null ? ts.getSlotLabel() : (start + " - " + end),
                    "start", start,
                    "end", end,
                    "period", ts.getPeriod() != null ? ts.getPeriod().name() : "MORNING",
                    "capacity", ts.getMaxPatients() != null ? ts.getMaxPatients() : clinicProperties.getSlotCapacity()
            ));
        }

        // 3. Quan hệ thân nhân
        Map<String, String> relationships = new LinkedHashMap<>();
        relationships.put("BAN_THAN", "Bản thân");
        relationships.put("VO_CHONG", "Vợ / chồng");
        relationships.put("CON", "Con");
        relationships.put("CHA_ME", "Cha / mẹ");
        relationships.put("ONG_BA", "Ông / bà");
        relationships.put("ANH_CHI_EM", "Anh / chị / em");
        relationships.put("KHAC", "Khác");

        // 4. Nhãn trạng thái phiếu khám
        Map<String, String> statusLabels = new LinkedHashMap<>();
        statusLabels.put("BOOKED", "Chưa đến");
        statusLabels.put("CHECKED_IN", "Đã check-in");
        statusLabels.put("IN_PROGRESS", "Đã vào khám");
        statusLabels.put("COMPLETED", "Đã hoàn thành");
        statusLabels.put("CANCELLED", "Đã huỷ");

        // 5. Danh sách lý do hủy mẫu
        List<String> cancelReasons = List.of(
                "Bận việc đột xuất",
                "Sức khoẻ không đi được",
                "Đã khám ở nơi khác",
                "Muốn đổi sang ngày giờ khác",
                "Lý do khác"
        );

        // 6. Danh mục bài viết (value = slug để khớp hợp đồng Frontend, label = tên hiển thị tiếng Việt)
        List<ArticleCategory> categories = categoryRepository.findAllByOrderBySortOrderAsc();
        List<Map<String, Object>> postCategories = new ArrayList<>();
        for (ArticleCategory c : categories) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", c.getSlug());
            item.put("label", c.getName());
            item.put("id", String.valueOf(c.getId()));
            item.put("slug", c.getSlug());
            item.put("name", c.getName());
            item.put("description", c.getDescription() != null ? c.getDescription() : "");
            postCategories.add(item);
        }

        // 7. Cấu hình OTP
        Map<String, Object> otpConfig = Map.of(
                "provider", "console",
                "firebase", false
        );

        int slotCapacity = getInt(settings, "slot_capacity", clinicProperties.getSlotCapacity());
        int seats = getInt(settings, "seats", clinicProperties.getSeats());
        int slotMinutes = getInt(settings, "slot_minutes", clinicProperties.getSlotMinutes());
        int userMaxDays = getInt(settings, "user_max_days", clinicProperties.getUserMaxDays());
        int counterMaxDays = getInt(settings, "counter_max_days", clinicProperties.getCounterMaxDays());
        int priorityEarlyMinutes = getInt(settings, "priority_early_minutes", clinicProperties.getPriorityEarlyMinutes());
        int maxProfiles = getInt(settings, "max_profiles", clinicProperties.getMaxProfiles());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("today", today.toString());
        data.put("otp", otpConfig);
        data.put("serviceTypes", serviceTypes);
        data.put("slots", slots);
        data.put("slotCapacity", slotCapacity);
        data.put("userMaxDays", userMaxDays);
        data.put("counterMaxDays", counterMaxDays);
        data.put("seats", seats);
        data.put("priorityEarlyMinutes", priorityEarlyMinutes);
        data.put("slotMinutes", slotMinutes);
        data.put("maxProfiles", maxProfiles);
        data.put("relationships", relationships);
        data.put("statusLabels", statusLabels);
        data.put("cancelReasons", cancelReasons);
        data.put("postCategories", postCategories);

        return ResponseEntity.ok(ApiResponse.ok(data, "Lấy thông tin cấu hình thành công"));
    }

    private int getInt(Map<String, String> map, String key, int defaultValue) {
        String val = map.get(key);
        if (val == null || val.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

