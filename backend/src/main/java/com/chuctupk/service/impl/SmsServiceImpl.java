package com.chuctupk.service.impl;

import com.chuctupk.service.SmsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.sms.provider:speedsms}")
    private String provider; // "speedsms" hoặc "esms"

    @Value("${app.sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.secret-key:}")
    private String secretKey; // Dành riêng cho eSMS

    @Value("${app.sms.sms-type:2}")
    private int smsType; // 2: Đầu số ngẫu nhiên/cố định, 1 hoặc 3: Brandname

    @Value("${app.sms.brandname:}")
    private String brandname;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean isEnabled() {
        return smsEnabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public boolean sendOtp(String phone, String otpCode) {
        String cleanPhone = cleanPhone(phone);
        String message = String.format("Ma xac thuc phong kham Chuc Tu cua ban la %s. Hieu luc trong 5 phut.", otpCode);

        // 1. Nếu bật gửi SMS thật và đã có API Key
        if (smsEnabled && apiKey != null && !apiKey.isBlank()) {
            try {
                boolean sent = sendRealSms(cleanPhone, message, otpCode);
                if (sent) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Lỗi khi kết nối cổng SMS Gateway ({}): {}", provider, e.getMessage(), e);
            }
        }

        // 2. Mặc định / Fallback: Hiển thị OTP trực quan trên console để kiểm thử
        log.info("=================================================");
        log.info(">>> [SMS OTP GATEWAY - PHÒNG KHÁM CHÚC TỪ] <<<");
        log.info("Số điện thoại nhận: {}", cleanPhone);
        log.info("Mã xác thực OTP: [{}]", otpCode);
        log.info("Nội dung: {}", message);
        if (!smsEnabled) {
            log.info("Chế độ: TEST / GIẢ LẬP (app.sms.enabled = false)");
            log.info("Gợi ý: Đặt app.sms.enabled=true và cấu hình app.sms.api-key để gửi SMS thật đến điện thoại.");
        }
        log.info("=================================================");

        return false;
    }

    private boolean sendRealSms(String phone, String content, String otpCode) throws Exception {
        log.info("Đang gửi SMS thật qua nhà cung cấp [{}] đến số {}...", provider, phone);

        if ("esms".equalsIgnoreCase(provider)) {
            // eSMS Brandname CSKH yêu cầu đúng cú pháp template đã duyệt với nhà mạng: "Ma xac nhan cua ban la {P}"
            String esmsContent = (brandname != null && !brandname.isBlank())
                    ? String.format("Ma xac nhan cua ban la %s", otpCode)
                    : content;
            return sendViaEsms(phone, esmsContent);
        } else {
            return sendViaSpeedSms(phone, content);
        }
    }

    /**
     * Tích hợp cổng SpeedSMS (https://speedsms.vn)
     * Endpoint: POST https://api.speedsms.vn/index.php/sms/send
     */
    private boolean sendViaSpeedSms(String phone, String content) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("to", Collections.singletonList(phone));
        payload.put("content", content);
        payload.put("sms_type", smsType); // 2: đầu số CSKH, 3: brandname
        if (brandname != null && !brandname.isBlank()) {
            payload.put("brandname", brandname);
        }

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.speedsms.vn/index.php/sms/send"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Phản hồi từ SpeedSMS (HTTP {}): {}", response.statusCode(), response.body());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText();
            if ("success".equalsIgnoreCase(status)) {
                log.info("Đã gửi tin nhắn SMS OTP thành công đến số {} qua SpeedSMS!", phone);
                return true;
            }
        }

        log.warn("Gửi SMS qua SpeedSMS thất bại: HTTP {}, Body: {}", response.statusCode(), response.body());
        return false;
    }

    /**
     * Tích hợp cổng eSMS.vn (https://esms.vn)
     * Endpoint: POST https://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_post_json
     */
    private boolean sendViaEsms(String phone, String content) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ApiKey", apiKey.trim());
        payload.put("SecretKey", secretKey != null ? secretKey.trim() : "");
        payload.put("Phone", phone);
        payload.put("Content", content);
        payload.put("IsUnicode", "0"); // 0: không dấu (chuẩn SMS OTP), 1: có dấu

        // Trên eSMS:
        // - SmsType = 8: Tin nhắn qua đầu số cố định (test ngay được, KHÔNG CẦN Brandname)
        // - SmsType = 2: Tin nhắn Brandname CSKH/OTP (bắt buộc phải có Brandname đã đăng ký)
        // Tự động chuyển về 8 nếu chưa cấu hình Brandname để tránh lỗi 104 (Brand name does not exist)
        int effectiveSmsType = smsType;
        if ((brandname == null || brandname.isBlank()) && (effectiveSmsType == 1 || effectiveSmsType == 2)) {
            effectiveSmsType = 8;
        }
        payload.put("SmsType", String.valueOf(effectiveSmsType));
        if (brandname != null && !brandname.isBlank()) {
            payload.put("Brandname", brandname.trim());
        }

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_post_json"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Phản hồi từ eSMS (HTTP {}): {}", response.statusCode(), response.body());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String codeResult = root.path("CodeResult").asText();
            String errorMessage = root.path("ErrorMessage").asText();
            String smsId = root.path("SMSID").asText();

            if ("100".equals(codeResult)) {
                log.info(">>> [eSMS THÀNH CÔNG] Đã gửi tin nhắn SMS OTP tới số {} (SMSID: {})", phone, smsId);
                return true;
            } else {
                String reason = switch (codeResult) {
                    case "101" -> "Sai ApiKey hoặc SecretKey. Vui lòng kiểm tra lại cấu hình.";
                    case "102" -> "Tài khoản eSMS hiện đang bị khóa.";
                    case "103" -> "Số dư tài khoản eSMS không đủ để gửi tin nhắn. Vui lòng nạp thêm tiền.";
                    case "104" -> "Tên Brandname không đúng hoặc chưa được duyệt.";
                    case "105" -> "Nội dung tin nhắn không hợp lệ với quy định nhà mạng.";
                    default -> "Lỗi từ eSMS: " + errorMessage;
                };
                log.error(">>> [eSMS THẤT BẠI] Mã lỗi [{}]: {}", codeResult, reason);
            }
        }

        log.warn("Gửi SMS qua eSMS thất bại: HTTP {}, Body: {}", response.statusCode(), response.body());
        return false;
    }

    private String cleanPhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "").trim();
        if (digits.startsWith("84") && digits.length() == 11) {
            return "0" + digits.substring(2);
        }
        if (digits.length() == 9 && !digits.startsWith("0")) {
            return "0" + digits;
        }
        return digits;
    }
}
