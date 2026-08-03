package com.skala.shopping.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(name = "ApiError", description = "모든 API 오류의 공통 응답")
public final class ApiError {

    private final String code;
    private final String message;
    private final int status;
    private final Instant timestamp;
    private final Map<String, String> fieldErrors;

    public ApiError(
            String code,
            String message,
            int status,
            Instant timestamp,
            Map<String, String> fieldErrors
    ) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.timestamp = timestamp;
        this.fieldErrors = fieldErrors;
    }

    public static ApiError from(ErrorCode errorCode) {
        return from(errorCode, errorCode.message(), Map.of());
    }

    public static ApiError from(
            ErrorCode errorCode,
            String message,
            Map<String, String> fieldErrors
    ) {
        return new ApiError(
                errorCode.code(),
                message,
                errorCode.status().value(),
                Instant.now(),
                fieldErrors
        );
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
