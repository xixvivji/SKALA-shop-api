package com.skala.shopping.common.internal;

import com.skala.shopping.common.ApiError;
import com.skala.shopping.common.BusinessException;
import com.skala.shopping.common.ErrorCode;
import com.skala.shopping.common.RateLimitExceededException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.sql.SQLException;
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
        exception.getBindingResult().getGlobalErrors().forEach(error ->
                fieldErrors.putIfAbsent("_global", error.getDefaultMessage())
        );
        return build(ErrorCode.INVALID_PARAMETER, ErrorCode.INVALID_PARAMETER.message(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fieldErrors.putIfAbsent(lastPathSegment(violation.getPropertyPath()), violation.getMessage())
        );
        return build(
                ErrorCode.INVALID_PARAMETER,
                ErrorCode.INVALID_PARAMETER.message(),
                fieldErrors
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return invalidRequest(Map.of(exception.getName(), "요청 값의 형식이 올바르지 않습니다."));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException exception) {
        return invalidRequest(Map.of(exception.getHeaderName(), "필수 요청 헤더입니다."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException exception) {
        return invalidRequest(Map.of(exception.getParameterName(), "필수 요청 파라미터입니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return invalidRequest(Map.of("_request", "요청 본문의 JSON 형식이 올바르지 않습니다."));
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiError> handleBindException(BindException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        exception.getBindingResult().getGlobalErrors().forEach(error ->
                fieldErrors.putIfAbsent("_global", error.getDefaultMessage())
        );
        return invalidRequest(fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            String key = parameterName == null ? "_parameter" : parameterName;
            result.getResolvableErrors().forEach(error ->
                    fieldErrors.putIfAbsent(key, error.getDefaultMessage())
            );
        });
        return invalidRequest(fieldErrors);
    }

    private ResponseEntity<ApiError> invalidRequest(Map<String, String> fieldErrors) {
        return build(
                ErrorCode.INVALID_PARAMETER,
                ErrorCode.INVALID_PARAMETER.message(),
                fieldErrors
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException exception) {
        return build(ErrorCode.DATA_NOT_FOUND, ErrorCode.DATA_NOT_FOUND.message(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String sqlState = findSqlState(exception);
        if ("23505".equals(sqlState)) {
            log.warn("Database unique constraint violation (SQL state: {})", sqlState);
            return build(ErrorCode.DATA_DUPLICATED, ErrorCode.DATA_DUPLICATED.message(), Map.of());
        }
        if ("23503".equals(sqlState)) {
            log.warn("Database foreign key constraint violation (SQL state: {})", sqlState);
            return build(
                    ErrorCode.INVALID_PARAMETER,
                    "연결된 데이터 제약 조건을 충족하지 못했습니다.",
                    Map.of()
            );
        }
        log.warn("Database constraint violation (SQL state: {})", sqlState == null ? "unknown" : sqlState);
        return build(ErrorCode.INVALID_PARAMETER, "데이터 제약 조건을 충족하지 못했습니다.", Map.of());
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

    private String lastPathSegment(Path path) {
        String name = "_parameter";
        for (Path.Node node : path) {
            if (node.getName() != null) {
                name = node.getName();
            }
        }
        return name;
    }

    private String findSqlState(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
            current = current.getCause();
        }
        return null;
    }
}
