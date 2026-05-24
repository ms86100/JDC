package com.jira.workflow.service;

import com.jira.workflow.dto.ConditionResponse;
import com.jira.workflow.dto.CreateConditionRequest;
import com.jira.workflow.entity.WorkflowCondition;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.WorkflowConditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing workflow conditions.
 * Provides CRUD operations for conditions attached to transitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowConditionService {

    private final WorkflowConditionRepository workflowConditionRepository;

    /**
     * Create a new condition for a transition.
     *
     * @param transitionId the transition ID
     * @param request the condition details
     * @return the created condition
     */
    @Transactional
    public ConditionResponse createCondition(UUID transitionId, CreateConditionRequest request) {
        log.info("Creating condition for transition: {}", transitionId);

        WorkflowCondition condition = WorkflowCondition.builder()
                .transitionId(transitionId)
                .conditionType(request.getConditionType())
                .fieldName(request.getFieldName())
                .operator(request.getOperator())
                .value(request.getValue())
                .conditionData(request.getConditionData())
                .negate(request.getNegate() != null ? request.getNegate() : false)
                .sequence(request.getSequence() != null ? request.getSequence() : 0)
                .build();

        condition = workflowConditionRepository.save(condition);
        log.info("Condition created: {}", condition.getId());

        return mapToConditionResponse(condition);
    }

    /**
     * Update an existing condition.
     *
     * @param id the condition ID
     * @param request the updated condition details
     * @return the updated condition
     */
    @Transactional
    public ConditionResponse updateCondition(UUID id, CreateConditionRequest request) {
        log.info("Updating condition: {}", id);

        WorkflowCondition condition = workflowConditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition", "id", id));

        if (request.getConditionType() != null) {
            condition.setConditionType(request.getConditionType());
        }
        if (request.getFieldName() != null) {
            condition.setFieldName(request.getFieldName());
        }
        if (request.getOperator() != null) {
            condition.setOperator(request.getOperator());
        }
        if (request.getValue() != null) {
            condition.setValue(request.getValue());
        }
        if (request.getConditionData() != null) {
            condition.setConditionData(request.getConditionData());
        }
        if (request.getNegate() != null) {
            condition.setNegate(request.getNegate());
        }
        if (request.getSequence() != null) {
            condition.setSequence(request.getSequence());
        }

        condition = workflowConditionRepository.save(condition);
        log.info("Condition updated: {}", id);

        return mapToConditionResponse(condition);
    }

    /**
     * Delete a condition by ID.
     *
     * @param id the condition ID
     */
    @Transactional
    public void deleteCondition(UUID id) {
        log.info("Deleting condition: {}", id);

        WorkflowCondition condition = workflowConditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition", "id", id));

        workflowConditionRepository.delete(condition);
        log.info("Condition deleted: {}", id);
    }

    /**
     * Get all conditions for a transition, ordered by sequence.
     *
     * @param transitionId the transition ID
     * @return list of conditions
     */
    @Transactional(readOnly = true)
    public List<ConditionResponse> getConditionsByTransition(UUID transitionId) {
        log.debug("Fetching conditions for transition: {}", transitionId);

        List<WorkflowCondition> conditions = workflowConditionRepository
                .findByTransitionIdOrderBySequenceAsc(transitionId);

        return conditions.stream()
                .map(this::mapToConditionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a condition by ID.
     *
     * @param id the condition ID
     * @return the condition
     */
    @Transactional(readOnly = true)
    public ConditionResponse getConditionById(UUID id) {
        log.debug("Fetching condition: {}", id);

        WorkflowCondition condition = workflowConditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition", "id", id));

        return mapToConditionResponse(condition);
    }

    /**
     * Bulk create conditions for a transition.
     * Useful for importing conditions from workflow descriptors.
     *
     * @param transitionId the transition ID
     * @param requests list of condition requests
     * @return list of created conditions
     */
    @Transactional
    public List<ConditionResponse> bulkCreateConditions(UUID transitionId, List<CreateConditionRequest> requests) {
        log.info("Bulk creating {} conditions for transition: {}", requests.size(), transitionId);

        List<WorkflowCondition> conditions = requests.stream()
                .map(request -> WorkflowCondition.builder()
                        .transitionId(transitionId)
                        .conditionType(request.getConditionType())
                        .fieldName(request.getFieldName())
                        .operator(request.getOperator())
                        .value(request.getValue())
                        .conditionData(request.getConditionData())
                        .negate(request.getNegate() != null ? request.getNegate() : false)
                        .sequence(request.getSequence() != null ? request.getSequence() : 0)
                        .build())
                .collect(Collectors.toList());

        List<WorkflowCondition> savedConditions = workflowConditionRepository.saveAll(conditions);
        log.info("Bulk created {} conditions", savedConditions.size());

        return savedConditions.stream()
                .map(this::mapToConditionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete all conditions for a transition.
     *
     * @param transitionId the transition ID
     */
    @Transactional
    public void deleteConditionsByTransition(UUID transitionId) {
        log.info("Deleting all conditions for transition: {}", transitionId);
        workflowConditionRepository.deleteByTransitionId(transitionId);
        log.info("All conditions deleted for transition: {}", transitionId);
    }

    private ConditionResponse mapToConditionResponse(WorkflowCondition condition) {
        return ConditionResponse.builder()
                .id(condition.getId())
                .transitionId(condition.getTransitionId())
                .conditionType(condition.getConditionType())
                .fieldName(condition.getFieldName())
                .operator(condition.getOperator())
                .value(condition.getValue())
                .conditionData(condition.getConditionData())
                .negate(condition.getNegate())
                .sequence(condition.getSequence())
                .createdAt(condition.getCreatedAt())
                .build();
    }
}