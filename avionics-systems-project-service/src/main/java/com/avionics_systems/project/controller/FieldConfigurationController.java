package com.avionics_systems.project.controller;

import com.avionics_systems.project.dto.FieldConfigurationRuleResponse;
import com.avionics_systems.project.dto.ValidateCreateIssueFieldsRequest;
import com.avionics_systems.project.service.FieldConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Field configuration", description = "Project field configuration scheme rules")
public class FieldConfigurationController {

    private final FieldConfigurationService fieldConfigurationService;

    @GetMapping("/{projectId}/field-configuration")
    @Operation(summary = "Resolve field rules for project", description = "Returns required/visible/hidden field rules for create/edit")
    public ResponseEntity<List<FieldConfigurationRuleResponse>> getFieldConfiguration(
            @PathVariable UUID projectId,
            @RequestParam(required = false) UUID issueTypeId) {
        return ResponseEntity.ok(fieldConfigurationService.resolveForProject(projectId, issueTypeId));
    }

    @PostMapping("/{projectId}/field-configuration/validate-create")
    @Operation(summary = "Validate create-issue fields", description = "Returns validation error messages for required fields")
    public ResponseEntity<Map<String, Object>> validateCreate(
            @PathVariable UUID projectId,
            @RequestBody ValidateCreateIssueFieldsRequest request) {
        List<String> errors = fieldConfigurationService.validateCreateFields(projectId, request);
        return ResponseEntity.ok(Map.of(
                "valid", errors.isEmpty(),
                "errors", errors
        ));
    }
}
