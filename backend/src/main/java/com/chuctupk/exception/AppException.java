package com.chuctupk.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Ngoại lệ nghiệp vụ thống nhất trong toàn bộ hệ thống.
 */
@Getter
public class AppException extends RuntimeException {

    private final int statusCode;
    private final String code;
    private final Object data;
    private final Map<String, String> fields;
    private final Integer retryAfter;

    public AppException(int statusCode, String message) {
        this(statusCode, message, null, null, null, null);
    }

    public AppException(int statusCode, String message, String code) {
        this(statusCode, message, code, null, null, null);
    }

    public AppException(ErrorCode errorCode) {
        this(errorCode.getStatusCode(), errorCode.getDefaultMessage(), errorCode.getCode(), null, null, null);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode.getStatusCode(), message != null ? message : errorCode.getDefaultMessage(), errorCode.getCode(), null, null, null);
    }

    public AppException(ErrorCode errorCode, String message, Map<String, String> fields) {
        this(errorCode.getStatusCode(), message != null ? message : errorCode.getDefaultMessage(), errorCode.getCode(), null, fields, null);
    }

    public AppException(ErrorCode errorCode, String message, Object data) {
        this(errorCode.getStatusCode(), message != null ? message : errorCode.getDefaultMessage(), errorCode.getCode(), data, null, null);
    }

    public AppException(ErrorCode errorCode, String message, Map<String, String> fields, Integer retryAfter) {
        this(errorCode.getStatusCode(), message != null ? message : errorCode.getDefaultMessage(), errorCode.getCode(), null, fields, retryAfter);
    }

    public AppException(ErrorCode errorCode, String message, Integer retryAfter) {
        this(errorCode.getStatusCode(), message != null ? message : errorCode.getDefaultMessage(), errorCode.getCode(), null, null, retryAfter);
    }

    public AppException(int statusCode, String message, String code, Map<String, String> fields, Integer retryAfter) {
        this(statusCode, message, code, null, fields, retryAfter);
    }

    public AppException(int statusCode, String message, String code, Object data, Map<String, String> fields, Integer retryAfter) {
        super(message);
        this.statusCode = statusCode;
        this.code = code != null ? code : "ERROR";
        this.data = data;
        this.fields = fields;
        this.retryAfter = retryAfter;
    }

    public static AppException of(ErrorCode code) {
        return new AppException(code);
    }

    public static AppException of(ErrorCode code, String message) {
        return new AppException(code, message);
    }

    public static AppException of(ErrorCode code, String message, Map<String, String> fields) {
        return new AppException(code, message, fields);
    }

    public static AppException badRequest(String message) {
        return new AppException(ErrorCode.BAD_REQUEST, message);
    }

    public static AppException badRequest(String message, Map<String, String> fields) {
        return new AppException(ErrorCode.BAD_REQUEST, message, fields);
    }

    public static AppException unauthorized(String message) {
        return new AppException(ErrorCode.UNAUTHORIZED, message);
    }

    public static AppException forbidden(String message) {
        return new AppException(ErrorCode.FORBIDDEN, message);
    }

    public static AppException notFound(String message) {
        return new AppException(ErrorCode.NOT_FOUND, message);
    }

    public static AppException conflict(String message) {
        return new AppException(ErrorCode.CONFLICT, message);
    }

    public static AppException slotFull(String message) {
        return new AppException(ErrorCode.SLOT_FULL, message);
    }

    public static AppException stale(String message) {
        return new AppException(ErrorCode.STALE, message);
    }

    public static AppException sessionChanged(String message) {
        return new AppException(ErrorCode.SESSION_CHANGED, message);
    }

    public static AppException tooManyRequests(String message, int retryAfterSeconds) {
        return new AppException(ErrorCode.TOO_MANY_REQUESTS.getStatusCode(), message, ErrorCode.TOO_MANY_REQUESTS.getCode(), null, retryAfterSeconds);
    }

    public static AppException unprocessable(String message) {
        return new AppException(ErrorCode.UNPROCESSABLE, message);
    }

    public static AppException internal(String message) {
        return new AppException(ErrorCode.INTERNAL_ERROR, message);
    }
}
