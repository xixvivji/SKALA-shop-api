package com.skala.shopping.common.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.skala.shopping.common.ApiError;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
    );

    @Test
    void mapsUniqueConstraintViolationsToConflict() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(
                violationWithSqlState("23505")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("DATA_DUPLICATED");
    }

    @Test
    void mapsForeignKeyConstraintViolationsToInvalidParameter() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(
                violationWithSqlState("23503")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_PARAMETER");
        assertThat(response.getBody().getMessage()).contains("연결된 데이터");
    }

    @Test
    void mapsOtherDatabaseConstraintsToInvalidParameter() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrityViolation(
                violationWithSqlState("23514")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void includesObjectLevelValidationErrors() {
        BindException exception = new BindException(new Object(), "request");
        exception.getBindingResult().addError(
                new ObjectError("request", "요청 항목의 조합이 올바르지 않습니다.")
        );

        ResponseEntity<ApiError> response = handler.handleBindException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFieldErrors())
                .containsEntry("_global", "요청 항목의 조합이 올바르지 않습니다.");
    }

    private DataIntegrityViolationException violationWithSqlState(String sqlState) {
        return new DataIntegrityViolationException(
                "constraint violation",
                new SQLException("database detail must not be returned", sqlState)
        );
    }
}
