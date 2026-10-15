package com.chuctupk.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final int statusCode;

    public AppException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public static AppException badRequest(String message) {
        return new AppException(400, message);
    }

    public static AppException notFound(String message) {
        return new AppException(404, message);
    }

    public static AppException conflict(String message) {
        return new AppException(409, message);
    }

    public static AppException internal(String message) {
        return new AppException(500, message);
    }
}
