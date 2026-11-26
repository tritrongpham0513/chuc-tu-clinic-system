package com.chuctupk.service.impl;

import com.chuctupk.common.PhoneValidator;
import com.chuctupk.notification.DeliveryResult;
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

    @Value("${app.sms.provider:esms}")
    private String provider; // "speedsms" hoặc "esms"

    @Value("${app.sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.secret-key:}")
    private String secretKey; // Dành riêng cho eSMS

    @Value("${app.sms.sms-type:8}")
    private int smsType; // 8: Đầu số cố định (test ngay không cần brandname), 2: Brandname CSKH/OTP

    @Value("${app.sms.brandname:}")
    private String brandname;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile String lastErrorCode;
    private long[] retryDelaysMs = new long[]{2000L, 5000L};

    private record SmsAttemptOutcome(boolean success, String errorCode) {}

    @Override
    public boolean isEnabled() {
        return smsEnabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String getProviderName() {
        return provider != null && !provider.isBlank() ? provider.trim().toLowerCase() : "esms";
    }

    @Override
    public String getLastErrorCode() {
        return this.lastErrorCode;
    }

    void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    void setRetryDelaysMs(long[] retryDelaysMs) {
        this.retryDelaysMs = retryDelaysMs;
    }

    private boolean isNetworkOrTimeoutError(Throwable t) {
        if (t == null) return false;
        if (t instanceof java.net.http.HttpTimeoutException
                || t instanceof java.net.ConnectException
                || t instanceof java.net.SocketTimeoutException
                || t instanceof java.io.IOException) {
            return true;
        }
        return t.getCause() != null && isNetworkOrTimeoutError(t.getCause());
    }

    private String classifyNetworkException(Throwable t) {
        Throwable curr = t;
        while (curr != null) {
            if (curr instanceof java.net.http.HttpTimeoutException
                    || curr instanceof java.net.SocketTimeoutException) {
                return "TIMEOUT";
            }
            if (curr instanceof java.net.ConnectException) {
                return "CONNECT_ERROR";
            }
            if (curr instanceof java.io.IOException) {
                return "NETWORK_ERROR";
            }
            curr = curr.getCause();
        }
        return "SMS_SEND_FAILED";
    }

    protected void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private DeliveryResult executeWithRetry(String phone, String content, String otpCode) {
        String providerName = getProviderName();
        int maxRetries = 2; // tối đa 2 lần retry (tổng 3 lần gọi)
        int attempt = 0;
        while (true) {
            try {
                SmsAttemptOutcome outcome = sendRealSms(phone, content, otpCode);
                if (outcome.success()) {
                    this.lastErrorCode = null;
                    return new DeliveryResult(DeliveryResult.Status.SENT, null, providerName);
                }
                // Gateway đã phản hồi mã lỗi nghiệp vụ (101, 103...) -> Không retry
                this.lastErrorCode = outcome.errorCode();
                return new DeliveryResult(DeliveryResult.Status.FAILED, outcome.errorCode(), providerName);
            } catch (Exception e) {
                boolean isNetError = isNetworkOrTimeoutError(e);
                if (isNetError && attempt < maxRetries) {
                    long delay = (attempt < retryDelaysMs.length) ? retryDelaysMs[attempt] : 5000L;
                    attempt++;
                    log.warn("[SMS] Lỗi mạng/timeout khi gửi SMS lần {} tới SĐT {} (thử lại sau {}ms): {}",
                            attempt, PhoneValidator.mask(phone), delay, e.getMessage());
                    sleep(delay);
                    continue;
                }
                log.error("[SMS] Lỗi khi kết nối cổng SMS Gateway ({}) sau {} lần thử lại: {}",
                        providerName, attempt, e.getMessage());
                String errorCode = classifyNetworkException(e);
                this.lastErrorCode = errorCode;
                return new DeliveryResult(DeliveryResult.Status.FAILED, errorCode, providerName);
            }
        }
    }

    @Override
    public OtpSendResult sendOtp(String phone, String otpCode) {
        String cleanPhone = cleanPhone(phone);
        String maskedPhone = PhoneValidator.mask(cleanPhone);

        if (!PhoneValidator.isValid(cleanPhone)) {
            log.warn("[SMS OTP] Số điện thoại không đúng định dạng, bỏ qua gửi: {}", maskedPhone);
            this.lastErrorCode = "INVALID_PHONE";
            return OtpSendResult.SKIPPED;
        }

        // 1. Nếu cổng SMS đang tắt hoặc chưa cấu hình API key -> SKIPPED (giả lập/test)
        if (!smsEnabled || apiKey == null || apiKey.isBlank()) {
            log.info("🔑 [SMS OTP - CHẾ ĐỘ THỬ NGHIỆM] SĐT: {} | Mã OTP: {}", maskedPhone, otpCode);
            this.lastErrorCode = null;
            return OtpSendResult.SKIPPED;
        }

        // 2. Cổng SMS được kích hoạt -> gửi tin nhắn thật qua retry
        String message = String.format("Ma xac thuc phong kham Chuc Tu cua ban la %s. Hieu luc trong 5 phut.", otpCode);
        DeliveryResult res = executeWithRetry(cleanPhone, message, otpCode);
        if (res.status() == DeliveryResult.Status.SENT) {
            log.info("[SMS OTP] Đã gửi mã xác thực thành công tới SĐT: {}", maskedPhone);
            return OtpSendResult.SENT;
        } else {
            log.warn("[SMS OTP] Gửi SMS qua {} không thành công tới SĐT: {}", provider, maskedPhone);
            return OtpSendResult.FAILED;
        }
    }

    @Override
    public OtpSendResult sendMessage(String phone, String message) {
        DeliveryResult res = sendNotification(phone, message);
        return switch (res.status()) {
            case SENT -> OtpSendResult.SENT;
            case SKIPPED -> OtpSendResult.SKIPPED;
            case FAILED -> OtpSendResult.FAILED;
        };
    }

    @Override
    public DeliveryResult sendNotification(String phone, String message) {
        String providerName = getProviderName();
        String cleanPhone = cleanPhone(phone);
        String maskedPhone = PhoneValidator.mask(cleanPhone);

        if (!PhoneValidator.isValid(cleanPhone)) {
            log.warn("[SMS NOTIF] Số điện thoại không đúng định dạng, bỏ qua gửi API: {}", maskedPhone);
            this.lastErrorCode = "INVALID_PHONE";
            return new DeliveryResult(DeliveryResult.Status.SKIPPED, "INVALID_PHONE", providerName);
        }

        if (!smsEnabled || apiKey == null || apiKey.isBlank()) {
            log.info("[SMS NOTIF] Cổng SMS đang tắt (chế độ giả lập) cho SĐT {}: {}", maskedPhone, message);
            this.lastErrorCode = null;
            return new DeliveryResult(DeliveryResult.Status.SKIPPED, null, providerName);
        }

        DeliveryResult res = executeWithRetry(cleanPhone, message, null);
        if (res.status() == DeliveryResult.Status.SENT) {
            log.info("[SMS NOTIF] Đã gửi thông báo thành công tới SĐT: {}", maskedPhone);
        } else {
            log.warn("[SMS NOTIF] Gửi thông báo không thành công tới SĐT: {} (errorCode={})", maskedPhone, res.errorCode());
        }
        return res;
    }

    String buildEsmsContent(String content, String otpCode) {
        return (otpCode != null && brandname != null && !brandname.isBlank())
                ? String.format("Ma xac nhan cua ban la %s", otpCode)
                : content;
    }

    private SmsAttemptOutcome sendRealSms(String phone, String content, String otpCode) throws Exception {
        log.info("Đang gửi SMS thật qua nhà cung cấp [{}] đến số {}...", provider, PhoneValidator.mask(phone));

        if ("esms".equalsIgnoreCase(provider)) {
            // eSMS Brandname CSKH yêu cầu đúng cú pháp template đã duyệt với nhà mạng: "Ma xac nhan cua ban la {P}"
            String esmsContent = buildEsmsContent(content, otpCode);
            return sendViaEsms(phone, esmsContent);
        } else {
            return sendViaSpeedSms(phone, content);
        }
    }

    /**
     * Tích hợp cổng SpeedSMS (https://speedsms.vn)
     * Endpoint: POST https://api.speedsms.vn/index.php/sms/send
     */
    private SmsAttemptOutcome sendViaSpeedSms(String phone, String content) throws Exception {
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

        String errCode;
        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText();
            if ("success".equalsIgnoreCase(status)) {
                log.info("Đã gửi tin nhắn SMS OTP thành công đến số {} qua SpeedSMS!", PhoneValidator.mask(phone));
                return new SmsAttemptOutcome(true, null);
            }
            errCode = root.path("status").asText("SPEEDSMS_FAILED");
        } else {
            errCode = "HTTP_" + response.statusCode();
        }

        log.warn("Gửi SMS qua SpeedSMS thất bại: HTTP {}, Body: {}", response.statusCode(), response.body());
        return new SmsAttemptOutcome(false, errCode);
    }

    int resolveEffectiveSmsType() {
        int effective = smsType;
        if ((brandname == null || brandname.isBlank()) && (effective == 1 || effective == 2)) {
            return 8;
        }
        return effective;
    }

    /**
     * Tích hợp cổng eSMS.vn (https://esms.vn)
     * Endpoint: POST https://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_post_json/
     */
    private SmsAttemptOutcome sendViaEsms(String phone, String content) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ApiKey", apiKey.trim());
        payload.put("SecretKey", secretKey != null ? secretKey.trim() : "");
        payload.put("Phone", phone);
        payload.put("Content", content);
        payload.put("IsUnicode", "0"); // 0: không dấu (chuẩn SMS OTP), 1: có dấu

        // Trên eSMS:
        // - SmsType = 8: Tin nhắn qua đầu số cố định (test ngay được, KHÔNG CẦN Brandname)
        // - SmsType = 2: Tin nhắn Brandname CSKH/OTP (bắt buộc phải có Brandname đã đăng ký)
        int effectiveSmsType = resolveEffectiveSmsType();
        payload.put("SmsType", String.valueOf(effectiveSmsType));
        if (brandname != null && !brandname.isBlank()) {
            payload.put("Brandname", brandname.trim());
        }

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_post_json/"))
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
                log.info(">>> [eSMS THÀNH CÔNG] Đã gửi tin nhắn SMS OTP tới số {} (SMSID: {})", PhoneValidator.mask(phone), smsId);
                return new SmsAttemptOutcome(true, null);
            } else {
                String reason = switch (codeResult) {
                    case "101" -> "Đăng nhập thất bại (sai ApiKey hoặc SecretKey)";
                    case "102" -> "Tài khoản eSMS đã bị khóa";
                    case "103" -> "Số dư tài khoản eSMS không đủ";
                    case "104" -> "Brandname chưa đăng ký hoặc chưa được duyệt";
                    default -> errorMessage != null && !errorMessage.isBlank() ? errorMessage : "Mã phản hồi " + codeResult;
                };
                log.error(">>> [eSMS THẤT BẠI] Mã lỗi [{}]: {}", codeResult, reason);
                return new SmsAttemptOutcome(false, codeResult);
            }
        }

        String errCode = "HTTP_" + response.statusCode();
        log.warn("Gửi SMS qua eSMS thất bại: HTTP {}, Body: {}", response.statusCode(), response.body());
        return new SmsAttemptOutcome(false, errCode);
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
