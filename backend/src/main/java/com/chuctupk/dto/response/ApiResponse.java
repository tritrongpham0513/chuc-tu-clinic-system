package com.chuctupk.dto.response;

import com.chuctupk.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Envelope chuẩn hóa duy nhất cho toàn bộ response HTTP của hệ thống Phòng Khám Chúc Từ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    private boolean success = true;

    @Builder.Default
    private int statusCode = 200;

    @Builder.Default
    private String code = "OK";

    private String message;

    private T data;

    private Map<String, String> fields;

    private Integer retryAfter;

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(200)
                .code("OK")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Thao tác thành công");
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ok(data, message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(201)
                .code("OK")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return created(data, "Khởi tạo thành công");
    }

    public static <T> ApiResponse<T> error(int statusCode, String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return error(statusCode, "ERROR", message);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return error(errorCode.getStatusCode(), errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return error(errorCode.getStatusCode(), errorCode.getCode(), message != null ? message : errorCode.getDefaultMessage());
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, Map<String, String> fields) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(errorCode.getStatusCode())
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getDefaultMessage())
                .fields(fields)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
