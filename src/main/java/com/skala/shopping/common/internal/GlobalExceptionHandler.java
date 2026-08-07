package com.skala.shopping.common.internal;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.RateLimitExceededException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ApiError> handleRateLimitExceeded(RateLimitExceededException exception) {
        ErrorCode errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.status())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(ApiError.from(errorCode, exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusinessException(BusinessException exception) {
        var errorCode = exception.errorCode();
        return build(errorCode, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        return build(ErrorCode.INVALID_PARAMETER, ErrorCode.INVALID_PARAMETER.message(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        return build(ErrorCode.INVALID_PARAMETER, exception.getMessage(), Map.of());
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            BindException.class,
            HandlerMethodValidationException.class
    })
    ResponseEntity<ApiError> handleInvalidRequest(Exception exception) {
        return build(
                ErrorCode.INVALID_PARAMETER,
                ErrorCode.INVALID_PARAMETER.message(),
                Map.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException exception) {
        return build(ErrorCode.DATA_NOT_FOUND, ErrorCode.DATA_NOT_FOUND.message(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Database constraint violation");
        return build(
                ErrorCode.DATA_DUPLICATED,
                "데이터 제약 조건을 충족하지 못했습니다.",
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message(), Map.of());
    }

    private ResponseEntity<ApiError> build(
            ErrorCode errorCode,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(errorCode.status()).body(
                ApiError.from(errorCode, message, fieldErrors)
        );
    }
}
