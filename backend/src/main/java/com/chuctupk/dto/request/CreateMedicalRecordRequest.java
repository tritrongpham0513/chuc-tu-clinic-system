package com.chuctupk.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMedicalRecordRequest {

    @NotNull(message = "Lịch khám không được để trống")
    private Long appointmentId;

    // Sinh hiệu & Tứ chẩn
    private String pulseBp; // Ví dụ: "120/80 mmHg, 72 l/p, Mạch Trầm Nhược"
    private String bloodPressure; // "120/80"
    private Integer pulseRate; // 75
    private String pulseDiagnosis; // "Trầm trì vô lực"
    private String tongueDiagnosis; // "Chất lưỡi nhạt, rêu trắng mỏng"

    // Chẩn đoán YHCT & YHHĐ
    private String diagnosis; // Bệnh danh Đông & Tây y
    private String traditionalDiagnosis; // Hội chứng bát cương, tạng phủ

    // Phương pháp điều trị & Toa thuốc thảo mộc
    private String treatment; // Phương pháp (Châm cứu, Xoa bóp, Thủy châm)
    private String acupointTherapy; // Liệu trình châm cứu, huyệt vị
    private String prescription; // Toa thuốc nam / bài thuốc YHCT
    private String herbalPrescription; // Chi tiết thành phần thang thuốc

    // Dặn dò & Tái khám
    private String advice; // Lời khuyên kiêng cữ dinh dưỡng
    private String dietaryAdvice; // Dặn dò ăn uống, kiêng khem
    private Integer followUpDays; // Hẹn tái khám sau số ngày (ví dụ: 7, 10, 14)
    private LocalDate followUpDate; // Ngày hẹn cụ thể
}
