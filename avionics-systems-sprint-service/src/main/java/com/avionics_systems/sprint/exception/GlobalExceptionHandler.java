package com.avionics_systems.sprint.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SERVICE_NAME = "avionics-systems-sprint-service";
    private final MessageSource messageSource;

    @ExceptionHandler({ResourceNotFoundException.class, com.avionics_systems.board.exception.ResourceNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(
            RuntimeException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", messageSource.getMessage("error.label.not.found", null, Locale.ENGLISH));
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("service", SERVICE_NAME);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", messageSource.getMessage("error.label.bad.request", null, Locale.ENGLISH));
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("service", SERVICE_NAME);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            IllegalStateException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", messageSource.getMessage("error.label.conflict", null, Locale.ENGLISH));
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("service", SERVICE_NAME);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", messageSource.getMessage("error.label.not.found", null, Locale.ENGLISH));
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("service", SERVICE_NAME);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", messageSource.getMessage("error.label.internal", null, Locale.ENGLISH));
        body.put("message", messageSource.getMessage("error.unexpected", null, Locale.ENGLISH));
        body.put("path", request.getRequestURI());
        body.put("service", SERVICE_NAME);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
