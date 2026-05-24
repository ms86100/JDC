package com.jira.project.controller;

import com.jira.project.dto.CreateSecurityLevelRequest;
import com.jira.project.dto.SecurityLevelResponse;
import com.jira.project.dto.UpdateSecurityLevelRequest;
import com.jira.project.entity.SecurityLevel;
import com.jira.project.repository.SecurityLevelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Security Level management.
 * Provides endpoints for listing security levels across all schemes.
 */
@RestController
@RequestMapping("/api/security-levels")
@RequiredArgsConstructor
@Tag(name = "Security Levels", description = "Security level management endpoints")
@CrossOrigin(origins = "*")
public class SecurityLevelController {

    private final SecurityLevelRepository securityLevelRepository;

    @GetMapping
    @Operation(summary = "Get all security levels", description = "Returns all available security levels")
    public ResponseEntity<List<SecurityLevelResponse>> getAllSecurityLevels() {
        List<SecurityLevelResponse> levels = securityLevelRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(levels);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get security level by ID", description = "Returns security level details by ID")
    public ResponseEntity<SecurityLevelResponse> getSecurityLevel(
            @Parameter(description = "Security Level ID") @PathVariable UUID id) {
        return securityLevelRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/scheme/{schemeId}")
    @Operation(summary = "Get security levels by scheme", description = "Returns all security levels for a given scheme")
    public ResponseEntity<List<SecurityLevelResponse>> getSecurityLevelsByScheme(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId) {
        List<SecurityLevelResponse> levels = securityLevelRepository.findBySchemeId(schemeId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(levels);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get security levels for project", description = "Returns security levels available for a project")
    public ResponseEntity<List<SecurityLevelResponse>> getSecurityLevelsForProject(
            @Parameter(description = "Project ID") @PathVariable UUID projectId) {
        // For now, return all levels - in production, filter by project's security scheme
        List<SecurityLevelResponse> levels = securityLevelRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(levels);
    }

    @PostMapping
    @Operation(summary = "Create security level", description = "Creates a new security level")
    public ResponseEntity<SecurityLevelResponse> createSecurityLevel(
            @Valid @RequestBody CreateSecurityLevelRequest request) {
        SecurityLevel entity = SecurityLevel.builder()
                .schemeId(request.getSchemeId())
                .name(request.getName())
                .description(request.getDescription())
                .levelType(request.getLevelType() != null ? request.getLevelType() : "RESTRICTED")
                .sequence(request.getSequence() != null ? request.getSequence() : 0)
                .createdAt(LocalDateTime.now())
                .build();
        SecurityLevel saved = securityLevelRepository.save(entity);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping("/scheme/{schemeId}")
    @Operation(summary = "Create security level in scheme", description = "Creates a new security level in a specific scheme")
    public ResponseEntity<SecurityLevelResponse> createSecurityLevelInScheme(
            @Parameter(description = "Scheme ID") @PathVariable UUID schemeId,
            @Valid @RequestBody CreateSecurityLevelRequest request) {
        SecurityLevel entity = SecurityLevel.builder()
                .schemeId(schemeId)
                .name(request.getName())
                .description(request.getDescription())
                .levelType(request.getLevelType() != null ? request.getLevelType() : "RESTRICTED")
                .sequence(request.getSequence() != null ? request.getSequence() : 0)
                .createdAt(LocalDateTime.now())
                .build();
        SecurityLevel saved = securityLevelRepository.save(entity);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update security level", description = "Updates an existing security level")
    public ResponseEntity<SecurityLevelResponse> updateSecurityLevel(
            @Parameter(description = "Security Level ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateSecurityLevelRequest request) {
        return securityLevelRepository.findById(id)
                .map(existing -> {
                    if (request.getName() != null) {
                        existing.setName(request.getName());
                    }
                    if (request.getDescription() != null) {
                        existing.setDescription(request.getDescription());
                    }
                    if (request.getLevelType() != null) {
                        existing.setLevelType(request.getLevelType());
                    }
                    if (request.getSequence() != null) {
                        existing.setSequence(request.getSequence());
                    }
                    SecurityLevel saved = securityLevelRepository.save(existing);
                    return ResponseEntity.ok(toResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete security level", description = "Deletes a security level by ID")
    public ResponseEntity<Void> deleteSecurityLevel(
            @Parameter(description = "Security Level ID") @PathVariable UUID id) {
        if (securityLevelRepository.existsById(id)) {
            securityLevelRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private SecurityLevelResponse toResponse(SecurityLevel entity) {
        return SecurityLevelResponse.builder()
                .id(entity.getId())
                .schemeId(entity.getSchemeId())
                .name(entity.getName())
                .description(entity.getDescription())
                .levelType(entity.getLevelType())
                .sequence(entity.getSequence())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}