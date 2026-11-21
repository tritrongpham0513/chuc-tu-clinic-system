package com.chuctupk.service.impl;

import com.chuctupk.dto.response.AdminSettingsResponse;
import com.chuctupk.dto.response.SettingMetadataDto;
import com.chuctupk.exception.AppException;
import com.chuctupk.repository.AppointmentRepository;
import com.chuctupk.service.AdminSettingsService;
import com.chuctupk.service.ClinicSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSettingsServiceImpl implements AdminSettingsService {

    private final ClinicSettingService clinicSettingService;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    private static final Set<String> WHITELIST_KEYS = Set.of(
            ClinicSettingService.KEY_SLOT_CAPACITY,
            ClinicSettingService.KEY_PATIENTS_PER_DOCTOR,
            ClinicSettingService.KEY_SEATS,
            ClinicSettingService.KEY_USER_MAX_DAYS,
            ClinicSettingService.KEY_COUNTER_MAX_DAYS,
            ClinicSettingService.KEY_MAX_PROFILES,
            ClinicSettingService.KEY_BOOKING_ADVANCE_DAYS,
            ClinicSettingService.KEY_BOOKING_MIN_HOURS,
            ClinicSettingService.KEY_PRIORITY_EARLY_MINUTES,
            ClinicSettingService.KEY_NO_SHOW_CANCEL_AT,
            ClinicSettingService.KEY_RECEPTION_OPEN,
            ClinicSettingService.KEY_RECEPTION_CLOSE,
            ClinicSettingService.KEY_AUTO_INVITE
    );

    @Override
    @Transactional(readOnly = true)
    public AdminSettingsResponse getSettings() {
        Map<String, String> settings = clinicSettingService.getAllSettings();
        Map<String, SettingMetadataDto> metadata = buildMetadata();
        int maxBooked = getMaxFutureBookedPerSlot();

        return AdminSettingsResponse.builder()
                .settings(settings)
                .metadata(metadata)
                .maxFutureBookedPerSlot(maxBooked)
                .warnings(new ArrayList<>())
                .build();
    }

    @Override
    @Transactional
    public AdminSettingsResponse updateSettings(Map<String, String> newSettings, String username) {
        if (newSettings == null || newSettings.isEmpty()) {
            throw AppException.badRequest("Dữ liệu cấu hình cập nhật không được để trống");
        }

        // 1. Kiểm tra danh sách trắng (Whitelist) - phát hiện khoá lạ
        for (String key : newSettings.keySet()) {
            if (!WHITELIST_KEYS.contains(key)) {
                throw AppException.unprocessable("Khoá cấu hình không hợp lệ: '" + key + "'");
            }
        }

        // 2. Validate từng khoá theo quy tắc
        Map<String, String> normalized = new HashMap<>();

        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            String key = entry.getKey();
            String rawVal = entry.getValue() != null ? entry.getValue().trim() : "";
            if (rawVal.isBlank()) {
                throw AppException.unprocessable("Giá trị của khoá '" + key + "' không được để trống");
            }

            switch (key) {
                case ClinicSettingService.KEY_SLOT_CAPACITY -> {
                    int val = parseInteger(key, rawVal, 1, 50);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_PATIENTS_PER_DOCTOR -> {
                    int val = parseInteger(key, rawVal, 1, 10);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_SEATS -> {
                    int val = parseInteger(key, rawVal, 1, 20);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_USER_MAX_DAYS -> {
                    int val = parseInteger(key, rawVal, 1, 365);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_COUNTER_MAX_DAYS -> {
                    int val = parseInteger(key, rawVal, 1, 365);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_MAX_PROFILES -> {
                    int val = parseInteger(key, rawVal, 1, 20);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_BOOKING_ADVANCE_DAYS -> {
                    int val = parseInteger(key, rawVal, 0, 365);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_BOOKING_MIN_HOURS -> {
                    int val = parseInteger(key, rawVal, 0, 168);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_PRIORITY_EARLY_MINUTES -> {
                    int val = parseInteger(key, rawVal, 0, 240);
                    normalized.put(key, String.valueOf(val));
                }
                case ClinicSettingService.KEY_NO_SHOW_CANCEL_AT -> {
                    LocalTime time = parseTime(key, rawVal);
                    normalized.put(key, time.toString());
                }
                case ClinicSettingService.KEY_RECEPTION_OPEN -> {
                    LocalTime time = parseTime(key, rawVal);
                    normalized.put(key, time.toString());
                }
                case ClinicSettingService.KEY_RECEPTION_CLOSE -> {
                    LocalTime time = parseTime(key, rawVal);
                    normalized.put(key, time.toString());
                }
                case ClinicSettingService.KEY_AUTO_INVITE -> {
                    if ("1".equals(rawVal) || "true".equalsIgnoreCase(rawVal)) {
                        normalized.put(key, "1");
                    } else if ("0".equals(rawVal) || "false".equalsIgnoreCase(rawVal)) {
                        normalized.put(key, "0");
                    } else {
                        throw AppException.unprocessable("Khoá 'auto_invite' chỉ nhận giá trị 0 hoặc 1");
                    }
                }
            }
        }

        // Validate ràng buộc reception_open < reception_close
        if (normalized.containsKey(ClinicSettingService.KEY_RECEPTION_OPEN) || normalized.containsKey(ClinicSettingService.KEY_RECEPTION_CLOSE)) {
            LocalTime open = normalized.containsKey(ClinicSettingService.KEY_RECEPTION_OPEN)
                    ? LocalTime.parse(normalized.get(ClinicSettingService.KEY_RECEPTION_OPEN))
                    : clinicSettingService.getReceptionOpen();
            LocalTime close = normalized.containsKey(ClinicSettingService.KEY_RECEPTION_CLOSE)
                    ? LocalTime.parse(normalized.get(ClinicSettingService.KEY_RECEPTION_CLOSE))
                    : clinicSettingService.getReceptionClose();

            if (!open.isBefore(close)) {
                throw AppException.unprocessable("Giờ mở cửa tiếp nhận (reception_open) phải trước giờ đóng cửa (reception_close)");
            }
        }

        // 3. Ghi audit log và lưu vào cơ sở dữ liệu
        Map<String, String> currentSettings = clinicSettingService.getAllSettings();
        for (Map.Entry<String, String> entry : normalized.entrySet()) {
            String key = entry.getKey();
            String newVal = entry.getValue();
            String oldVal = currentSettings.get(key);

            if (!Objects.equals(oldVal, newVal)) {
                log.info("[AUDIT] Người dùng '{}' thay đổi cấu hình '{}': '{}' -> '{}'",
                        username != null ? username : "anonymous", key, oldVal, newVal);
                clinicSettingService.setSettingValue(key, newVal);
            }
        }

        // 4. Kiểm tra cảnh báo nếu giảm slot_capacity thấp hơn số đã đặt trong tương lai
        List<String> warnings = new ArrayList<>();
        int maxBooked = getMaxFutureBookedPerSlot();
        if (normalized.containsKey(ClinicSettingService.KEY_SLOT_CAPACITY)) {
            int newCap = Integer.parseInt(normalized.get(ClinicSettingService.KEY_SLOT_CAPACITY));
            if (newCap < maxBooked) {
                warnings.add("Cảnh báo: Hiện có khung giờ trong tương lai đã có " + maxBooked
                        + " bệnh nhân đặt chỗ, lớn hơn sức chứa mới (" + newCap + ").");
            }
        }

        Map<String, String> updatedSettings = clinicSettingService.getAllSettings();
        return AdminSettingsResponse.builder()
                .settings(updatedSettings)
                .metadata(buildMetadata())
                .maxFutureBookedPerSlot(maxBooked)
                .warnings(warnings)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxFutureBookedPerSlot() {
        LocalDate today = LocalDate.now(clock);
        List<Long> counts = appointmentRepository.findFutureSlotBookingCounts(today, PageRequest.of(0, 1));
        if (counts != null && !counts.isEmpty() && counts.get(0) != null) {
            return counts.get(0).intValue();
        }
        return 0;
    }

    private int parseInteger(String key, String rawVal, int min, int max) {
        try {
            int val = Integer.parseInt(rawVal);
            if (val < min || val > max) {
                throw AppException.unprocessable("Khoá '" + key + "' phải là số nguyên trong khoảng " + min + "–" + max);
            }
            return val;
        } catch (NumberFormatException e) {
            throw AppException.unprocessable("Khoá '" + key + "' phải là số nguyên trong khoảng " + min + "–" + max);
        }
    }

    private LocalTime parseTime(String key, String rawVal) {
        try {
            return LocalTime.parse(rawVal);
        } catch (DateTimeParseException e) {
            throw AppException.unprocessable("Khoá '" + key + "' phải có định dạng giờ HH:mm (ví dụ 08:00)");
        }
    }

    private Map<String, SettingMetadataDto> buildMetadata() {
        Map<String, SettingMetadataDto> m = new LinkedHashMap<>();

        // Nhóm 1: Đặt lịch (BOOKING)
        m.put(ClinicSettingService.KEY_SLOT_CAPACITY, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_SLOT_CAPACITY)
                .label("Sức chứa tối đa mỗi khung giờ")
                .description("Số lượng bệnh nhân tiếp nhận tối đa trong 1 khung giờ khám")
                .type("int").min(1).max(50).unit("bệnh nhân/khung").group("BOOKING").build());

        m.put(ClinicSettingService.KEY_PATIENTS_PER_DOCTOR, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_PATIENTS_PER_DOCTOR)
                .label("Bệnh nhân tối đa mỗi bác sĩ")
                .description("Số bệnh nhân tối đa mỗi bác sĩ phụ trách trong 1 khung giờ")
                .type("int").min(1).max(10).unit("bệnh nhân/bác sĩ/khung").group("BOOKING").build());

        m.put(ClinicSettingService.KEY_BOOKING_ADVANCE_DAYS, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_BOOKING_ADVANCE_DAYS)
                .label("Số ngày đặt trước tối đa (Hệ thống)")
                .description("Khoảng thời gian tối đa cho phép đặt lịch trước trên toàn hệ thống")
                .type("int").min(0).max(365).unit("ngày").group("BOOKING").build());

        m.put(ClinicSettingService.KEY_USER_MAX_DAYS, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_USER_MAX_DAYS)
                .label("Số ngày bệnh nhân đặt trước")
                .description("Số ngày người bệnh được phép đặt lịch hẹn trực tuyến trước")
                .type("int").min(1).max(365).unit("ngày").group("BOOKING").build());

        m.put(ClinicSettingService.KEY_COUNTER_MAX_DAYS, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_COUNTER_MAX_DAYS)
                .label("Số ngày tiếp tân đặt tại quầy")
                .description("Số ngày tiếp tân có thể đặt hẹn trước cho bệnh nhân tại quầy")
                .type("int").min(1).max(365).unit("ngày").group("BOOKING").build());

        m.put(ClinicSettingService.KEY_BOOKING_MIN_HOURS, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_BOOKING_MIN_HOURS)
                .label("Thời gian tối thiểu đặt trước")
                .description("Khoảng thời gian tối thiểu người bệnh phải đặt trước giờ khám bắt đầu")
                .type("int").min(0).max(168).unit("giờ").group("BOOKING").build());

        m.put(ClinicSettingService.KEY_MAX_PROFILES, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_MAX_PROFILES)
                .label("Số hồ sơ người thân tối đa")
                .description("Số lượng hồ sơ thành viên người thân tối đa trên 1 tài khoản")
                .type("int").min(1).max(20).unit("hồ sơ").group("BOOKING").build());

        // Nhóm 2: Tiếp nhận (RECEPTION)
        m.put(ClinicSettingService.KEY_RECEPTION_OPEN, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_RECEPTION_OPEN)
                .label("Giờ mở cửa tiếp nhận")
                .description("Thời điểm phòng khám bắt đầu mở cửa tiếp đón bệnh nhân")
                .type("time").unit("HH:mm").group("RECEPTION").build());

        m.put(ClinicSettingService.KEY_RECEPTION_CLOSE, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_RECEPTION_CLOSE)
                .label("Giờ đóng cửa tiếp nhận")
                .description("Thời điểm phòng khám kết thúc tiếp đón bệnh nhân")
                .type("time").unit("HH:mm").group("RECEPTION").build());

        m.put(ClinicSettingService.KEY_PRIORITY_EARLY_MINUTES, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_PRIORITY_EARLY_MINUTES)
                .label("Thời gian đến sớm ưu tiên")
                .description("Số phút đến sớm trước khung giờ để được hưởng thứ tự ưu tiên")
                .type("int").min(0).max(240).unit("phút").group("RECEPTION").build());

        m.put(ClinicSettingService.KEY_NO_SHOW_CANCEL_AT, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_NO_SHOW_CANCEL_AT)
                .label("Giờ tự động hủy lịch chưa đến")
                .description("Thời điểm hệ thống tự động đánh dấu hủy các lịch chưa check-in trong ngày")
                .type("time").unit("HH:mm").group("RECEPTION").build());

        // Nhóm 3: Hàng đợi (QUEUE)
        m.put(ClinicSettingService.KEY_SEATS, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_SEATS)
                .label("Số ghế khám thực tế")
                .description("Tổng số ghế khám bệnh đang vận hành đồng thời trong phòng khám")
                .type("int").min(1).max(20).unit("ghế").group("QUEUE").build());

        m.put(ClinicSettingService.KEY_AUTO_INVITE, SettingMetadataDto.builder()
                .key(ClinicSettingService.KEY_AUTO_INVITE)
                .label("Tự động gọi vào ghế")
                .description("Tự động mời bệnh nhân tiếp theo vào ghế khám khi có ghế trống (0: Tắt, 1: Bật)")
                .type("boolean").group("QUEUE").build());

        return m;
    }
}
