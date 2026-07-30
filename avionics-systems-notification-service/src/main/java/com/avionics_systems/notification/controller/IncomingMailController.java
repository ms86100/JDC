package com.avionics_systems.notification.controller;

import com.avionics_systems.notification.dto.IncomingMailHandlerRequest;
import com.avionics_systems.notification.dto.IncomingMailHandlerResponse;
import com.avionics_systems.notification.service.IncomingMailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/mail-handlers")
@RequiredArgsConstructor
@Slf4j
public class IncomingMailController {

    private final IncomingMailService incomingMailService;

    @GetMapping
    public ResponseEntity<List<IncomingMailHandlerResponse>> getAllHandlers() {
        log.info("GET /api/admin/mail-handlers - Fetching all mail handlers");
        List<IncomingMailHandlerResponse> response = incomingMailService.getAllHandlers();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<IncomingMailHandlerResponse> createHandler(
            @Valid @RequestBody IncomingMailHandlerRequest request) {
        log.info("POST /api/admin/mail-handlers - Creating mail handler: {}", request.getName());
        IncomingMailHandlerResponse response = incomingMailService.createHandler(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncomingMailHandlerResponse> getHandler(@PathVariable UUID id) {
        log.info("GET /api/admin/mail-handlers/{} - Fetching mail handler", id);
        IncomingMailHandlerResponse response = incomingMailService.getHandler(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomingMailHandlerResponse> updateHandler(
            @PathVariable UUID id,
            @Valid @RequestBody IncomingMailHandlerRequest request) {
        log.info("PUT /api/admin/mail-handlers/{} - Updating mail handler", id);
        IncomingMailHandlerResponse response = incomingMailService.updateHandler(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHandler(@PathVariable UUID id) {
        log.info("DELETE /api/admin/mail-handlers/{} - Deleting mail handler", id);
        incomingMailService.deleteHandler(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable UUID id) {
        log.info("POST /api/admin/mail-handlers/{}/test - Testing connection", id);
        boolean success = incomingMailService.testConnection(id);
        return ResponseEntity.ok(Map.of("success", success, "message", "Connection test successful"));
    }
}
