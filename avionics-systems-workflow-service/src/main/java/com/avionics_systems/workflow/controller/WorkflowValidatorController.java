package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.dto.CreateValidatorRequest;
import com.avionics_systems.workflow.dto.ValidatorResponse;
import com.avionics_systems.workflow.service.WorkflowValidatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Workflow Validators
 * Handles CRUD operations for validators attached to workflow transitions
 */
@RestController
@RequestMapping("/api/workflow/validators")
@RequiredArgsConstructor
@Tag(name = "Workflow Validators", description = "Workflow validator management endpoints")
public class WorkflowValidatorController {

    private final WorkflowValidatorService validatorService;

    @GetMapping
    @Operation(
        summary = "Get all validators",
        description = "Returns all workflow validators, optionally filtered by transitionId"
    )
    public ResponseEntity<List<ValidatorResponse>> getAllValidators(
            @Parameter(description = "Filter by transition ID")
            @RequestParam(required = false) UUID transitionId) {

        List<ValidatorResponse> validators;
        if (transitionId != null) {
            validators = validatorService.getValidatorsByTransition(transitionId);
        } else {
            validators = validatorService.getAllValidators();
        }
        return ResponseEntity.ok(validators);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get validator by ID",
        description = "Returns a single validator by its ID"
    )
    public ResponseEntity<ValidatorResponse> getValidatorById(
            @Parameter(description = "Validator ID") @PathVariable UUID id) {

        ValidatorResponse validator = validatorService.getValidatorById(id);
        return ResponseEntity.ok(validator);
    }

    @GetMapping("/transition/{transitionId}")
    @Operation(
        summary = "Get validators by transition",
        description = "Returns all validators for a specific workflow transition, ordered by sequence"
    )
    public ResponseEntity<List<ValidatorResponse>> getValidatorsByTransition(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId) {

        List<ValidatorResponse> validators = validatorService.getValidatorsByTransition(transitionId);
        return ResponseEntity.ok(validators);
    }

    @PostMapping
    @Operation(
        summary = "Create validator",
        description = "Creates a new validator for a workflow transition"
    )
    public ResponseEntity<ValidatorResponse> createValidator(
            @Parameter(description = "Validator creation request")
            @Valid @RequestBody CreateValidatorRequest request) {

        ValidatorResponse validator = validatorService.createValidator(request.getTransitionId(), request);
        return new ResponseEntity<>(validator, HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @Operation(
        summary = "Bulk create validators",
        description = "Creates multiple validators for a transition. Replaces existing validators for the transition."
    )
    public ResponseEntity<List<ValidatorResponse>> bulkCreateValidators(
            @Parameter(description = "Bulk creation request")
            @Valid @RequestBody BulkValidatorRequest request) {

        List<ValidatorResponse> validators = validatorService.bulkCreateValidators(
                request.getTransitionId(), request.getValidators());
        return new ResponseEntity<>(validators, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update validator",
        description = "Updates an existing validator by ID"
    )
    public ResponseEntity<ValidatorResponse> updateValidator(
            @Parameter(description = "Validator ID") @PathVariable UUID id,
            @Parameter(description = "Validator update request")
            @Valid @RequestBody CreateValidatorRequest request) {

        ValidatorResponse validator = validatorService.updateValidator(id, request);
        return ResponseEntity.ok(validator);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete validator",
        description = "Deletes a validator by ID"
    )
    public ResponseEntity<Void> deleteValidator(
            @Parameter(description = "Validator ID") @PathVariable UUID id) {

        validatorService.deleteValidator(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DTO for bulk validator creation request
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkValidatorRequest {
        @jakarta.validation.constraints.NotNull(message = "Transition ID is required")
        private UUID transitionId;

        @jakarta.validation.constraints.NotEmpty(message = "At least one validator is required")
        private List<CreateValidatorRequest> validators;
    }
}