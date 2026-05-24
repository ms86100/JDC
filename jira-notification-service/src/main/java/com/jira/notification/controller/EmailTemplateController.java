package com.jira.notification.controller;

import com.jira.notification.dto.CreateEmailTemplateRequest;
import com.jira.notification.dto.EmailTemplateResponse;
import com.jira.notification.service.EmailTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/email-templates")
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateController {

    private final EmailTemplateService templateService;

    @PostMapping
    public ResponseEntity<EmailTemplateResponse> createTemplate(@Valid @RequestBody CreateEmailTemplateRequest request) {
        log.info("POST /api/email-templates - Creating email template: {}", request.getTemplateKey());
        EmailTemplateResponse response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateResponse> getTemplate(@PathVariable UUID id) {
        log.info("GET /api/email-templates/{} - Fetching email template", id);
        EmailTemplateResponse response = templateService.getTemplate(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/key/{templateKey}")
    public ResponseEntity<EmailTemplateResponse> getTemplateByKey(@PathVariable String templateKey) {
        log.info("GET /api/email-templates/key/{} - Fetching template by key", templateKey);
        EmailTemplateResponse response = templateService.getTemplateByKey(templateKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<EmailTemplateResponse>> getAllTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/email-templates - Fetching all email templates");
        Page<EmailTemplateResponse> response = templateService.getAllTemplates(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/event-type/{eventType}")
    public ResponseEntity<List<EmailTemplateResponse>> getTemplatesByEventType(@PathVariable String eventType) {
        log.info("GET /api/email-templates/event-type/{} - Fetching templates for event type", eventType);
        List<EmailTemplateResponse> response = templateService.getTemplatesByEventType(eventType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/default/{eventType}")
    public ResponseEntity<EmailTemplateResponse> getDefaultTemplateForEventType(@PathVariable String eventType) {
        log.info("GET /api/email-templates/default/{} - Fetching default template for event type", eventType);
        EmailTemplateResponse response = templateService.getDefaultTemplateForEventType(eventType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<EmailTemplateResponse>> getActiveTemplates() {
        log.info("GET /api/email-templates/active - Fetching all active templates");
        List<EmailTemplateResponse> response = templateService.getActiveTemplates();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEmailTemplateRequest request) {
        log.info("PUT /api/email-templates/{} - Updating email template", id);
        EmailTemplateResponse response = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        log.info("DELETE /api/email-templates/{} - Deleting email template", id);
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<EmailTemplateResponse> toggleTemplate(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        log.info("PATCH /api/email-templates/{}/toggle - Toggling template to enabled={}", id, enabled);
        EmailTemplateResponse response = templateService.toggleTemplate(id, enabled);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeDefaultTemplates() {
        log.info("POST /api/email-templates/initialize - Initializing default templates");
        templateService.initializeDefaultTemplates();
        return ResponseEntity.ok().build();
    }
}