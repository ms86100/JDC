package com.avionics_systems.migration.exception;

import lombok.*;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MigrationException.class)
    public ResponseEntity<ErrorResponse> handleMigrationException(MigrationException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(messageSource.getMessage("error.migration", null, "Migration Error", Locale.ENGLISH))
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .field(ex.getField())
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException ex) {
        ValidationErrorResponse error = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(messageSource.getMessage("error.validation", null, "Validation Error", Locale.ENGLISH))
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .field(ex.getField())
                .row(ex.getRow())
                .invalidValue(ex.getInvalidValue() != null ? ex.getInvalidValue().toString() : null)
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(messageSource.getMessage("error.bad_request", null, "Bad Request", Locale.ENGLISH))
                .message(ex.getMessage())
                .errorCode(messageSource.getMessage("error.code.invalid_request", null, "INVALID_REQUEST", Locale.ENGLISH))
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(messageSource.getMessage("error.invalid_state", null, "Invalid State", Locale.ENGLISH))
                .message(ex.getMessage())
                .errorCode(messageSource.getMessage("error.code.invalid_state", null, "INVALID_STATE", Locale.ENGLISH))
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(messageSource.getMessage("error.entity_not_found", null, "Entity Not Found", Locale.ENGLISH))
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .details(Map.of(
                        "entityType", ex.getEntityType(),
                        "entityKey", ex.getEntityKey()
                ))
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RollbackException.class)
    public ResponseEntity<ErrorResponse> handleRollbackException(RollbackException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(messageSource.getMessage("error.rollback", null, "Rollback Error", Locale.ENGLISH))
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(StorageException ex) {
        HttpStatus status = switch (ex.getErrorType()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case QUOTA_EXCEEDED -> HttpStatus.PAYLOAD_TOO_LARGE;
            case VIRUS_DETECTED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case INVALID_FILE -> HttpStatus.BAD_REQUEST;
            case STORAGE_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(messageSource.getMessage("error.storage", null, "Storage Error", Locale.ENGLISH))
                .message(ex.getMessage())
                .errorCode(ex.getErrorType().name())
                .details(Map.of(
                        "attachmentId", ex.getAttachmentId() != null ? ex.getAttachmentId() : "",
                        "storageType", ex.getStorageType() != null ? ex.getStorageType().name() : ""
                ))
                .build();
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(messageSource.getMessage("error.internal", null, "Internal Server Error", Locale.ENGLISH))
                .message(ex.getMessage())
                .errorCode(messageSource.getMessage("error.code.internal", null, "INTERNAL_ERROR", Locale.ENGLISH))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String errorCode;
        private String field;
        private Map<String, Object> details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String errorCode;
        private String field;
        private Integer row;
        private String invalidValue;
        private List<ValidationDetail> validationDetails;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ValidationDetail {
            private String field;
            private String errorCode;
            private String message;
        }
    }
}