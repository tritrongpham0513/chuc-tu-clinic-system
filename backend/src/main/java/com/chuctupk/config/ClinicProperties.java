package com.chuctupk.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

/**
 * Cấu hình tham số nghiệp vụ của Phòng khám Chúc Từ, nạp từ application.yml hoặc clinic_settings trong DB.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.clinic")
public class ClinicProperties {

    /** Số bệnh nhân tối đa trong 1 khung giờ */
    private int slotCapacity = 6;

    /** Số ghế khám thực tế trong phòng khám */
    private int seats = 5;

    /** Thời lượng mỗi khung giờ khám (phút) */
    private int slotMinutes = 60;

    /** Số ngày người bệnh được phép đặt trước tối đa */
    private int userMaxDays = 30;

    /** Số ngày tiếp tân đặt hộ tại quầy tối đa */
    private int counterMaxDays = 7;

    /** Số phút đến sớm trước khung giờ để được xếp vào nhóm Ưu tiên (PRIORITY) */
    private int priorityEarlyMinutes = 15;

    /** Số hồ sơ người thân tối đa được tạo trên 1 tài khoản */
    private int maxProfiles = 10;

    /** Tự động mời người kế tiếp vào ghế khi có ghế trống */
    private boolean autoInvite = false;

    /** Giờ mở cửa tiếp nhận */
    private LocalTime receptionOpen = LocalTime.of(8, 0);

    /** Giờ đóng cửa tiếp nhận */
    private LocalTime receptionClose = LocalTime.of(16, 30);

    /** Giờ hệ thống tự động hủy các lịch chưa đến trong ngày */
    private LocalTime noShowCancelAt = LocalTime.of(17, 0);
}

