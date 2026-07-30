package com.avionics_systems.workflow.service;

import com.avionics_systems.workflow.dto.CreateValidatorRequest;
import com.avionics_systems.workflow.dto.ValidatorResponse;
import com.avionics_systems.workflow.entity.WorkflowValidator;
import com.avionics_systems.workflow.exception.ResourceNotFoundException;
import com.avionics_systems.workflow.repository.WorkflowValidatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Workflow Validators
 * Handles CRUD operations for validators that check transition conditions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowValidatorService {

    private final WorkflowValidatorRepository validatorRepository;

    /**
     * Create a new validator for a transition
     */
    @Transactional
    public ValidatorResponse createValidator(UUID transitionId, CreateValidatorRequest request) {
        log.info("Creating validator for transition: {}, type: {}", transitionId, request.getValidatorType());

        WorkflowValidator validator = WorkflowValidator.builder()
                .transitionId(transitionId)
                .validatorType(request.getValidatorType())
                .fieldName(request.getFieldName())
                .validatorData(request.getValidatorData())
                .errorMessage(request.getErrorMessage())
                .sequence(request.getSequence() != null ? request.getSequence() : 0)
                .continueOnError(request.getContinueOnError() != null ? request.getContinueOnError() : false)
                .build();

        validator = validatorRepository.save(validator);
        log.info("Validator created: {}", validator.getId());

        return mapToResponse(validator);
    }

    /**
     * Update an existing validator
     */
    @Transactional
    public ValidatorResponse updateValidator(UUID id, CreateValidatorRequest request) {
        log.info("Updating validator: {}", id);

        WorkflowValidator validator = validatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowValidator", "id", id));

        validator.setValidatorType(request.getValidatorType());
        validator.setFieldName(request.getFieldName());
        validator.setValidatorData(request.getValidatorData());
        validator.setErrorMessage(request.getErrorMessage());
        if (request.getSequence() != null) {
            validator.setSequence(request.getSequence());
        }
        if (request.getContinueOnError() != null) {
            validator.setContinueOnError(request.getContinueOnError());
        }

        validator = validatorRepository.save(validator);
        log.info("Validator updated: {}", validator.getId());

        return mapToResponse(validator);
    }

    /**
     * Delete a validator by ID
     */
    @Transactional
    public void deleteValidator(UUID id) {
        log.info("Deleting validator: {}", id);

        if (!validatorRepository.existsById(id)) {
            throw new ResourceNotFoundException("WorkflowValidator", "id", id);
        }

        validatorRepository.deleteById(id);
        log.info("Validator deleted: {}", id);
    }

    /**
     * Get all validators for a transition, ordered by sequence
     */
    @Transactional(readOnly = true)
    public List<ValidatorResponse> getValidatorsByTransition(UUID transitionId) {
        log.debug("Getting validators for transition: {}", transitionId);

        return validatorRepository.findByTransitionIdOrderBySequenceAsc(transitionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a validator by ID
     */
    @Transactional(readOnly = true)
    public ValidatorResponse getValidatorById(UUID id) {
        log.debug("Getting validator: {}", id);

        WorkflowValidator validator = validatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowValidator", "id", id));

        return mapToResponse(validator);
    }

    /**
     * Bulk create validators for a transition
     * Replaces any existing validators for the transition
     */
    @Transactional
    public List<ValidatorResponse> bulkCreateValidators(UUID transitionId, List<CreateValidatorRequest> requests) {
        log.info("Bulk creating {} validators for transition: {}", requests.size(), transitionId);

        // Delete existing validators for this transition
        validatorRepository.deleteByTransitionId(transitionId);

        // Create new validators
        List<WorkflowValidator> validators = requests.stream()
                .map(request -> WorkflowValidator.builder()
                        .transitionId(transitionId)
                        .validatorType(request.getValidatorType())
                        .fieldName(request.getFieldName())
                        .validatorData(request.getValidatorData())
                        .errorMessage(request.getErrorMessage())
                        .sequence(request.getSequence() != null ? request.getSequence() : 0)
                        .continueOnError(request.getContinueOnError() != null ? request.getContinueOnError() : false)
                        .build())
                .collect(Collectors.toList());

        validators = validatorRepository.saveAll(validators);
        log.info("Bulk created {} validators", validators.size());

        return validators.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all validators (admin/list view)
     */
    @Transactional(readOnly = true)
    public List<ValidatorResponse> getAllValidators() {
        log.debug("Getting all validators");

        return validatorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map entity to response DTO
     */
    private ValidatorResponse mapToResponse(WorkflowValidator validator) {
        return ValidatorResponse.builder()
                .id(validator.getId())
                .transitionId(validator.getTransitionId())
                .validatorType(validator.getValidatorType())
                .fieldName(validator.getFieldName())
                .validatorData(validator.getValidatorData())
                .errorMessage(validator.getErrorMessage())
                .sequence(validator.getSequence())
                .continueOnError(validator.getContinueOnError())
                .createdAt(validator.getCreatedAt())
                .build();
    }
}
