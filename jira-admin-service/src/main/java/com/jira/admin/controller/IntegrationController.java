package com.jira.admin.controller;

import com.jira.admin.dto.ApplicationLinkResponse;
import com.jira.admin.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
@Tag(name = "Integration", description = "Application links and cross-product integration")
public class IntegrationController {

    private final IntegrationService integrationService;

    @GetMapping("/applinks")
    @Operation(summary = "List application links")
    public ResponseEntity<List<ApplicationLinkResponse>> listApplicationLinks() {
        return ResponseEntity.ok(integrationService.listApplicationLinks());
    }

    @PostMapping("/applinks")
    @Operation(summary = "Create application link")
    public ResponseEntity<ApplicationLinkResponse> createApplicationLink(@RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(integrationService.createApplicationLink(body));
    }

    @DeleteMapping("/applinks/{id}")
    @Operation(summary = "Delete application link")
    public ResponseEntity<Void> deleteApplicationLink(@PathVariable String id) {
        integrationService.deleteApplicationLink(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/applinks/{id}/primary")
    @Operation(summary = "Set primary application link")
    public ResponseEntity<ApplicationLinkResponse> setPrimary(@PathVariable String id) {
        return ResponseEntity.ok(integrationService.setPrimary(id));
    }

    @GetMapping("/applinks/{id}/health")
    @Operation(summary = "Test application link connection")
    public ResponseEntity<Map<String, String>> testConnection(@PathVariable String id) {
        return ResponseEntity.ok(integrationService.testConnection(id));
    }
}