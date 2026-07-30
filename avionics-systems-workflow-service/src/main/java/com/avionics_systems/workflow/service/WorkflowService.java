package com.avionics_systems.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.workflow.dto.*;
import com.avionics_systems.workflow.entity.*;
import com.avionics_systems.workflow.exception.DuplicateResourceException;
import com.avionics_systems.workflow.exception.ResourceNotFoundException;
import com.avionics_systems.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {

    private final com.avionics_systems.workflow.engine.WorkflowExecutionEngine workflowExecutionEngine;

    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowConditionRepository workflowConditionRepository;
    private final WorkflowValidatorRepository workflowValidatorRepository;
    private final WorkflowPostFunctionRepository workflowPostFunctionRepository;
    private final WorkflowLayoutEdgeSyncService workflowLayoutEdgeSyncService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${avionics-systems.services.issue-url:http://avionics-systems-issue-service:8084}")
    private String issueServiceUrl;

    @Value("${avionics-systems.services.user-url:http://avionics-systems-user-service:8082}")
    private String userServiceUrl;

    @Transactional
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        log.info("Creating workflow for project: {}", request.getProjectId());

        if (request.isDefault()) {
            workflowRepository.findByProjectIdAndIsDefaultTrue(request.getProjectId())
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        workflowRepository.save(existing);
                    });
        }

        Workflow workflow = Workflow.builder()
                .projectId(request.getProjectId())
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(request.isDefault())
                .build();

        workflow = workflowRepository.save(workflow);
        UUID workflowId = workflow.getId();

        if (request.getStatusIds() != null && !request.getStatusIds().isEmpty()) {
            List<WorkflowStatus> statuses = IntStream.range(0, request.getStatusIds().size())
                    .mapToObj(i -> WorkflowStatus.builder()
                            .workflowId(workflowId)
                            .statusId(request.getStatusIds().get(i))
                            .sequence(i)
                            .build())
                    .collect(Collectors.toList());
            workflowStatusRepository.saveAll(statuses);
        }

        log.info("Workflow created successfully: {}", workflow.getId());
        return mapToWorkflowResponse(workflow);
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflowsForProject(UUID projectId) {
        log.debug("Fetching workflows for project: {}", projectId);
        return workflowRepository.findByProjectId(projectId).stream()
                .map(this::mapToWorkflowResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> listAllWorkflows() {
        log.debug("Listing all workflows");
        return workflowRepository.findAll().stream()
                .map(this::mapToWorkflowResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(UUID workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));
        return mapToWorkflowResponse(workflow);
    }

    @Transactional
    public WorkflowResponse updateWorkflow(UUID workflowId, UpdateWorkflowRequest request) {
        log.info("Updating workflow: {}", workflowId);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        if (request.getName() != null) {
            workflow.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workflow.setDescription(request.getDescription());
        }
        if (request.isDefault()) {
            if (workflow.getProjectId() != null) {
                workflowRepository.findByProjectIdAndIsDefaultTrue(workflow.getProjectId())
                        .filter(w -> !w.getId().equals(workflowId))
                        .ifPresent(existing -> {
                            existing.setIsDefault(false);
                            workflowRepository.save(existing);
                        });
            }
            workflow.setIsDefault(true);
        }

        workflow = workflowRepository.save(workflow);
        log.info("Workflow updated: {}", workflowId);

        return mapToWorkflowResponse(workflow);
    }

    @Transactional
    public void deleteWorkflow(UUID workflowId) {
        log.info("Deleting workflow: {}", workflowId);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        // Delete workflow statuses first
        workflowStatusRepository.deleteByWorkflowId(workflowId);

        // Delete transitions and related data
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        for (WorkflowTransition transition : transitions) {
            workflowConditionRepository.deleteByTransitionId(transition.getId());
            workflowValidatorRepository.deleteByTransitionId(transition.getId());
            workflowPostFunctionRepository.deleteByTransitionId(transition.getId());
        }
        workflowTransitionRepository.deleteByWorkflowId(workflowId);

        workflowRepository.delete(workflow);
        log.info("Workflow deleted: {}", workflowId);
    }

    @Transactional
    public TransitionResponse addTransition(CreateTransitionRequest request) {
        log.info("Adding transition to workflow: {}", request.getWorkflowId());

        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", request.getWorkflowId()));

        workflowTransitionRepository.findByWorkflowIdAndFromStatusIdAndToStatusId(
                        request.getWorkflowId(), request.getFromStatusId(), request.getToStatusId())
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Transition already exists");
                });

        WorkflowTransition transition = WorkflowTransition.builder()
                .workflowId(request.getWorkflowId())
                .name(request.getName())
                .fromStatusId(request.getFromStatusId())
                .toStatusId(request.getToStatusId())
                .requiresApproval(request.isRequiresApproval())
                .build();

        transition = workflowTransitionRepository.save(transition);
        workflowLayoutEdgeSyncService.syncLayoutEdges(request.getWorkflowId());

        log.info("Transition added successfully: {}", transition.getId());
        return mapToTransitionResponse(transition);
    }

    @Transactional(readOnly = true)
    public TransitionResponse getTransition(UUID transitionId) {
        log.debug("Fetching transition: {}", transitionId);
        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transition", "id", transitionId));
        return mapToTransitionResponse(transition);
    }

    @Transactional
    public TransitionResponse updateTransition(UUID transitionId, UpdateTransitionRequest request) {
        log.info("Updating transition: {}", transitionId);

        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transition", "id", transitionId));

        if (request.getName() != null) transition.setName(request.getName());
        if (request.getDescription() != null) transition.setDescription(request.getDescription());
        if (request.getIcon() != null) transition.setIcon(request.getIcon());
        if (request.getType() != null) transition.setType(request.getType());
        if (request.getTriggerType() != null) transition.setTriggerType(request.getTriggerType());
        if (request.getDisplayOrder() != null) transition.setDisplayOrder(request.getDisplayOrder());
        if (request.getRequiresApproval() != null) transition.setRequiresApproval(request.getRequiresApproval());
        if (request.getApprovalGroupId() != null) transition.setApprovalGroupId(request.getApprovalGroupId());
        if (request.getAllowAssigneeOverride() != null) transition.setAllowAssigneeOverride(request.getAllowAssigneeOverride());
        if (request.getAllowUnassign() != null) transition.setAllowUnassign(request.getAllowUnassign());
        if (request.getFieldsRequired() != null) transition.setFieldsRequired(request.getFieldsRequired());
        if (request.getFieldsHidden() != null) transition.setFieldsHidden(request.getFieldsHidden());
        if (request.getPermissionCheck() != null) transition.setPermissionCheck(request.getPermissionCheck());
        if (request.getUserGroupIds() != null) transition.setUserGroupIds(request.getUserGroupIds());
        if (request.getAllowLoop() != null) transition.setAllowLoop(request.getAllowLoop());
        if (request.getMaxLoopCount() != null) transition.setMaxLoopCount(request.getMaxLoopCount());
        if (request.getScreenId() != null) transition.setScreenId(request.getScreenId());

        transition = workflowTransitionRepository.save(transition);
        log.info("Transition updated: {}", transitionId);

        return mapToTransitionResponse(transition);
    }

    @Transactional
    public void deleteTransition(UUID transitionId) {
        log.info("Deleting transition: {}", transitionId);

        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transition", "id", transitionId));

        UUID workflowId = transition.getWorkflowId();
        workflowTransitionRepository.delete(transition);
        workflowLayoutEdgeSyncService.syncLayoutEdges(workflowId);
        log.info("Transition deleted: {}", transitionId);
    }

    @Transactional(readOnly = true)
    public List<TransitionResponse> getTransitionsForProject(UUID projectId) {
        log.debug("Fetching transitions for project: {}", projectId);

        Workflow workflow = workflowRepository.findByProjectIdAndIsDefaultTrue(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Default workflow not found for project: " + projectId));

        return workflowTransitionRepository.findByWorkflowId(workflow.getId()).stream()
                .map(this::mapToTransitionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ValidateTransitionResponse validateTransition(UUID projectId, UUID fromStatusId, UUID toStatusId) {
        log.debug("Validating transition for project {}: {} -> {}", projectId, fromStatusId, toStatusId);

        List<Workflow> workflows = workflowRepository.findByProjectId(projectId);
        if (workflows.isEmpty()) {
            return ValidateTransitionResponse.builder()
                    .fromStatusId(fromStatusId)
                    .toStatusId(toStatusId)
                    .valid(true)
                    .message("No workflow defined, allowing by default")
                    .build();
        }

        Workflow workflow = workflows.stream()
                .filter(Workflow::getIsDefault)
                .findFirst()
                .orElse(workflows.get(0));

        // Orphan Status Detection: Verify both statuses are part of this workflow
        List<WorkflowStatus> workflowStatuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflow.getId());
        Set<UUID> statusIds = workflowStatuses.stream()
                .map(WorkflowStatus::getStatusId)
                .collect(Collectors.toSet());

        // Check if fromStatus is part of workflow
        if (!statusIds.contains(fromStatusId)) {
            return ValidateTransitionResponse.builder()
                    .fromStatusId(fromStatusId)
                    .toStatusId(toStatusId)
                    .valid(false)
                    .message("Invalid transition - source status is not part of this workflow")
                    .build();
        }

        // Check if toStatus is part of workflow
        if (!statusIds.contains(toStatusId)) {
            return ValidateTransitionResponse.builder()
                    .fromStatusId(fromStatusId)
                    .toStatusId(toStatusId)
                    .valid(false)
                    .message("Invalid transition - destination status is not part of this workflow")
                    .build();
        }

        boolean valid = workflowTransitionRepository
                .findByWorkflowIdAndFromStatusIdAndToStatusId(workflow.getId(), fromStatusId, toStatusId)
                .isPresent();

        String message = valid
                ? "Transition is allowed"
                : "Transition is not allowed - no path exists from source to destination";

        return ValidateTransitionResponse.builder()
                .fromStatusId(fromStatusId)
                .toStatusId(toStatusId)
                .valid(valid)
                .message(message)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TransitionResponse> getAllowedTransitions(UUID workflowId, UUID fromStatusId) {
        log.debug("Fetching allowed transitions for workflow {} from status {}", workflowId, fromStatusId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        return workflowTransitionRepository.findByWorkflowIdAndFromStatusId(workflowId, fromStatusId).stream()
                .map(this::mapToTransitionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all transitions with conditions, validators, and post-functions.
     *
     * Uses bulk fetching to avoid N+1 queries - loads all conditions, validators,
     * and post-functions in 3 queries instead of 3N where N is the number of transitions.
     */
    @Transactional(readOnly = true)
    public List<TransitionDetailResponse> getTransitionsWithDetails(UUID workflowId) {
        log.debug("Fetching transitions with details for workflow {}", workflowId);

        // Fetch transitions and all related data in bulk queries
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        if (transitions.isEmpty()) {
            return List.of();
        }

        // Get all transition IDs in one go
        List<UUID> transitionIds = transitions.stream()
                .map(WorkflowTransition::getId)
                .collect(Collectors.toList());

        // Bulk fetch conditions, validators, and post-functions (3 queries instead of 3N)
        Map<UUID, List<WorkflowCondition>> conditionsMap = workflowConditionRepository
                .findByTransitionIdsOrderBySequenceAsc(transitionIds).stream()
                .collect(Collectors.groupingBy(WorkflowCondition::getTransitionId));

        Map<UUID, List<WorkflowValidator>> validatorsMap = workflowValidatorRepository
                .findByTransitionIdsOrderBySequenceAsc(transitionIds).stream()
                .collect(Collectors.groupingBy(WorkflowValidator::getTransitionId));

        Map<UUID, List<WorkflowPostFunction>> postFunctionsMap = workflowPostFunctionRepository
                .findByTransitionIdsOrderBySequenceAsc(transitionIds).stream()
                .collect(Collectors.groupingBy(WorkflowPostFunction::getTransitionId));

        // Build responses by looking up in the maps
        return transitions.stream()
                .map(t -> mapTransitionDetailWithMaps(t, conditionsMap, validatorsMap, postFunctionsMap))
                .collect(Collectors.toList());
    }

    /**
     * Add condition to transition
     */
    @Transactional
    public ConditionResponse addCondition(CreateConditionRequest request) {
        log.info("Adding condition to transition: {}", request.getTransitionId());

        WorkflowCondition condition = WorkflowCondition.builder()
                .transitionId(request.getTransitionId())
                .conditionType(request.getConditionType())
                .fieldName(request.getFieldName())
                .operator(request.getOperator())
                .value(request.getValue())
                .conditionData(request.getConditionData())
                .negate(request.getNegate() != null && request.getNegate())
                .sequence(request.getSequence() != null ? request.getSequence() : 0)
                .build();

        condition = workflowConditionRepository.save(condition);
        return mapToConditionResponse(condition);
    }

    /**
     * Add validator to transition
     */
    @Transactional
    public ValidatorResponse addValidator(CreateValidatorRequest request) {
        log.info("Adding validator to transition: {}", request.getTransitionId());

        WorkflowValidator validator = WorkflowValidator.builder()
                .transitionId(request.getTransitionId())
                .validatorType(request.getValidatorType())
                .fieldName(request.getFieldName())
                .validatorData(request.getValidatorData())
                .errorMessage(request.getErrorMessage())
                .sequence(request.getSequence() != null ? request.getSequence() : 0)
                .continueOnError(request.getContinueOnError() != null && request.getContinueOnError())
                .build();

        validator = workflowValidatorRepository.save(validator);
        return mapToValidatorResponse(validator);
    }

    /**
     * Add post-function to transition
     */
    @Transactional
    public PostFunctionResponse addPostFunction(CreatePostFunctionRequest request) {
        log.info("Adding post-function to transition: {}", request.getTransitionId());

        WorkflowPostFunction postFunction = WorkflowPostFunction.builder()
                .transitionId(request.getTransitionId())
                .functionType(request.getFunctionType())
                .functionData(request.getFunctionData())
                .sequence(request.getSequence() != null ? request.getSequence() : 0)
                .async(request.getAsync() != null && request.getAsync())
                .failOnError(request.getFailOnError() != null ? request.getFailOnError() : true)
                .build();

        postFunction = workflowPostFunctionRepository.save(postFunction);
        return mapToPostFunctionResponse(postFunction);
    }

    @Transactional
    public void deleteCondition(UUID conditionId) {
        log.info("Deleting condition: {}", conditionId);
        WorkflowCondition condition = workflowConditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Condition", "id", conditionId));
        workflowConditionRepository.delete(condition);
        log.info("Condition deleted: {}", conditionId);
    }

    @Transactional
    public void deleteValidator(UUID validatorId) {
        log.info("Deleting validator: {}", validatorId);
        WorkflowValidator validator = workflowValidatorRepository.findById(validatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Validator", "id", validatorId));
        workflowValidatorRepository.delete(validator);
        log.info("Validator deleted: {}", validatorId);
    }

    @Transactional
    public void deletePostFunction(UUID functionId) {
        log.info("Deleting post-function: {}", functionId);
        WorkflowPostFunction postFunction = workflowPostFunctionRepository.findById(functionId)
                .orElseThrow(() -> new ResourceNotFoundException("PostFunction", "id", functionId));
        workflowPostFunctionRepository.delete(postFunction);
        log.info("Post-function deleted: {}", functionId);
    }

    /**
     * Validate if a transition can be performed
     */
    @Transactional(readOnly = true)
    public TransitionValidationResponse validateTransitionExecution(UUID transitionId, UUID userId, UUID issueId) {
        log.debug("Validating transition execution: {} for user {} on issue {}", transitionId, userId, issueId);

        WorkflowTransition transition = workflowTransitionRepository.findById(transitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transition", "id", transitionId));

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check conditions
        List<WorkflowCondition> conditions = workflowConditionRepository.findByTransitionIdOrderBySequenceAsc(transitionId);
        for (WorkflowCondition condition : conditions) {
            if (!evaluateCondition(condition, userId, issueId)) {
                boolean isNegated = Boolean.TRUE.equals(condition.getNegate());
                if (!isNegated) {
                    errors.add("Condition not met: " + condition.getConditionType());
                }
                // Negated condition that fails = condition passes, so no error
            } else {
                boolean isNegated = Boolean.TRUE.equals(condition.getNegate());
                if (isNegated) {
                    errors.add("Negated condition met: " + condition.getConditionType());
                }
            }
        }

        return TransitionValidationResponse.builder()
                .transitionId(transitionId)
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private boolean evaluateCondition(WorkflowCondition condition, UUID userId, UUID issueId) {
        // Fetch context data from external services
        Map<String, Object> issueData = fetchIssueData(issueId);
        Map<String, Object> userData = fetchUserData(userId);

        String conditionType = condition.getConditionType();

        try {
            switch (conditionType) {
                case WorkflowCondition.TYPE_USER_IS_CURRENT_USER:
                    // Check if user is the current authenticated user
                    return userData.containsKey("id") &&
                           userId.equals(parseUUID(userData.get("id").toString()));

                case WorkflowCondition.TYPE_USER_IS_REPORTER:
                    // Check if user is the issue reporter
                    if (issueData.containsKey("reporterId")) {
                        UUID reporterId = parseUUID(issueData.get("reporterId").toString());
                        return userId.equals(reporterId);
                    }
                    return false;

                case WorkflowCondition.TYPE_USER_IS_ASSIGNEE:
                    // Check if user is the issue assignee
                    if (issueData.containsKey("assigneeId")) {
                        Object assigneeIdObj = issueData.get("assigneeId");
                        if (assigneeIdObj != null) {
                            UUID assigneeId = parseUUID(assigneeIdObj.toString());
                            return userId.equals(assigneeId);
                        }
                    }
                    return false;

                case WorkflowCondition.TYPE_USER_GROUP:
                    // Check if user is in required group
                    String requiredGroup = condition.getValue();
                    if (requiredGroup != null && userData.containsKey("groups")) {
                        try {
                            List<String> userGroups = objectMapper.readValue(
                                userData.get("groups").toString(),
                                List.class
                            );
                            return userGroups.contains(requiredGroup);
                        } catch (Exception e) {
                            log.warn("Failed to parse user groups: {}", e.getMessage());
                        }
                    }
                    return false;

                case WorkflowCondition.TYPE_FIELD_VALUE:
                    // Check if issue field matches expected value
                    String fieldName = condition.getFieldName();
                    String operator = condition.getOperator();
                    String expectedValue = condition.getValue();
                    Object actualValue = issueData.get(fieldName);
                    return evaluateOperator(actualValue, operator, expectedValue);

                case WorkflowCondition.TYPE_FIELD_CHANGED:
                    // Check if field was changed (requires history - simplified)
                    log.debug("Field changed condition requires ChangeHistoryService");
                    return true; // Requires additional implementation

                case WorkflowCondition.TYPE_LINKED_ISSUE_STATUS:
                    // Check if linked issue has specific status
                    log.debug("Linked issue status condition not yet implemented");
                    return true; // Requires additional implementation

                case WorkflowCondition.TYPE_PREVIOUS_STATUS:
                    // Check if issue was in previous status
                    log.debug("Previous status condition not yet implemented");
                    return true; // Requires additional implementation

                case WorkflowCondition.TYPE_SPRINT_STATUS:
                    // Check sprint status condition
                    log.debug("Sprint status condition not yet implemented");
                    return true; // Requires additional implementation

                case WorkflowCondition.TYPE_SUBTASK_STATUS:
                    // Check subtask status condition
                    log.debug("Subtask status condition not yet implemented");
                    return true; // Requires additional implementation

                case WorkflowCondition.TYPE_SCRIPT:
                    // Groovy script condition - would need script engine
                    log.debug("Script condition not yet implemented");
                    return true; // Requires additional implementation

                default:
                    // FAIL-SAFE: Unknown conditions should block by default for security
                    log.warn("Unknown condition type: {}, blocking for safety", conditionType);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error evaluating condition {}: {}", conditionType, e.getMessage());
            return false;
        }
    }

    private UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean evaluateOperator(Object actual, String operator, String expected) {
        if (actual == null) {
            return "IS".equals(operator) && "NULL".equalsIgnoreCase(expected);
        }

        String actualStr = actual.toString();

        switch (operator != null ? operator.toUpperCase() : "=") {
            case "=":
            case "EQUALS":
                return actualStr.equalsIgnoreCase(expected);
            case "!=":
            case "NOT_EQUALS":
                return !actualStr.equalsIgnoreCase(expected);
            case "~":
            case "CONTAINS":
                return actualStr.toLowerCase().contains(expected.toLowerCase());
            case "!~":
            case "NOT_CONTAINS":
                return !actualStr.toLowerCase().contains(expected.toLowerCase());
            case ">":
            case "GREATER_THAN":
                try {
                    return Double.parseDouble(actualStr) > Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return actualStr.compareTo(expected) > 0;
                }
            case "<":
            case "LESS_THAN":
                try {
                    return Double.parseDouble(actualStr) < Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    return actualStr.compareTo(expected) < 0;
                }
            case "IN":
                return Arrays.asList(expected.split(",")).stream()
                    .map(String::trim)
                    .anyMatch(v -> actualStr.equalsIgnoreCase(v));
            case "NOT IN":
                return Arrays.asList(expected.split(",")).stream()
                    .map(String::trim)
                    .noneMatch(v -> actualStr.equalsIgnoreCase(v));
            default:
                return actualStr.equalsIgnoreCase(expected);
        }
    }

    private Map<String, Object> fetchIssueData(UUID issueId) {
        try {
            String url = issueServiceUrl + "/api/issues/" + issueId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? response : new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to fetch issue data for {}: {}", issueId, e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> fetchUserData(UUID userId) {
        try {
            String url = userServiceUrl + "/api/users/" + userId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? response : new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to fetch user data for {}: {}", userId, e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Execute a workflow transition on an issue
     * This is the main entry point for runtime workflow execution
     */
    @Transactional
    public TransitionExecutionResponse executeTransition(UUID issueId, UUID transitionId, UUID userId) {
        Map<String, Object> issueData = fetchIssueData(issueId);
        UUID projectId = parseUUID(issueData.get("projectId") != null ? issueData.get("projectId").toString() : null);
        ExecuteTransitionRequest request = new ExecuteTransitionRequest();
        request.setIssueId(issueId);
        request.setTransitionId(transitionId);
        request.setProjectId(projectId);
        request.setUserId(userId);
        return workflowExecutionEngine.execute(request);
    }

    private List<String> executeValidators(UUID transitionId, Map<String, Object> issueData, UUID userId) {
        List<String> errors = new ArrayList<>();
        List<WorkflowValidator> validators = workflowValidatorRepository
                .findByTransitionIdOrderBySequenceAsc(transitionId);

        for (WorkflowValidator validator : validators) {
            boolean valid = evaluateValidator(validator, issueData, userId);
            if (!valid && !Boolean.TRUE.equals(validator.getContinueOnError())) {
                errors.add(validator.getErrorMessage() != null ?
                          validator.getErrorMessage() :
                          "Validation failed for: " + validator.getValidatorType());
            }
        }

        return errors;
    }

    private boolean evaluateValidator(WorkflowValidator validator, Map<String, Object> issueData, UUID userId) {
        // Basic validator implementation
        String validatorType = validator.getValidatorType();

        switch (validatorType) {
            case "REQUIRED_FIELD":
                String fieldName = validator.getFieldName();
                Object value = issueData.get(fieldName);
                return value != null && !value.toString().isEmpty();

            case "PERMISSION":
                // Would check if user has specific permission
                log.debug("Permission validator not yet fully implemented");
                return true;

            case "DATE_RANGE":
                // Would validate date is within range
                log.debug("Date range validator not yet implemented");
                return true;

            case "REGEX":
                // Would validate field against regex pattern
                log.debug("Regex validator not yet implemented");
                return true;

            case "USER_PERMISSION":
                // Would check user permission
                log.debug("User permission validator not yet implemented");
                return true;

            case "SUBTASK_RESOLUTION":
                // Would validate subtask resolution
                log.debug("Subtask resolution validator not yet implemented");
                return true;

            case "LINKED_ISSUE_RESOLUTION":
                // Would validate linked issue resolution
                log.debug("Linked issue resolution validator not yet implemented");
                return true;

            case "ATTACHMENT_COUNT":
                // Would validate attachment count
                log.debug("Attachment count validator not yet implemented");
                return true;

            case "COMMENT_REQUIRED":
                // Would require comment for transition
                log.debug("Comment required validator not yet implemented");
                return true;

            case "TIME_TRACKING":
                // Would validate time tracking
                log.debug("Time tracking validator not yet implemented");
                return true;

            case "SCRIPT":
                // Script-based validator
                log.debug("Script validator not yet implemented");
                return true;

            default:
                // FAIL-SAFE: Unknown validators should block by default for security
                log.warn("Unknown validator type: {}, blocking for safety", validatorType);
                return false;
        }
    }

    private void executePostFunctionsAsync(UUID transitionId, UUID issueId, UUID userId) {
        List<WorkflowPostFunction> postFunctions = workflowPostFunctionRepository
                .findByTransitionIdOrderBySequenceAsc(transitionId);

        for (WorkflowPostFunction postFunction : postFunctions) {
            if (Boolean.TRUE.equals(postFunction.getAsync())) {
                // Execute asynchronously
                java.util.concurrent.CompletableFuture.runAsync(() ->
                    executePostFunction(postFunction, issueId, userId));
            } else {
                // Execute synchronously
                executePostFunction(postFunction, issueId, userId);
            }
        }
    }

    private void executePostFunction(WorkflowPostFunction postFunction, UUID issueId, UUID userId) {
        String functionType = postFunction.getFunctionType();
        log.info("Executing post-function {} of type {} for issue {}", postFunction.getId(), functionType, issueId);

        try {
            switch (functionType) {
                case "SET_FIELD_VALUE":
                    // Set issue field value
                    String fieldData = postFunction.getFunctionData();
                    if (fieldData != null) {
                        Map<String, Object> data = objectMapper.readValue(fieldData, Map.class);
                        String fieldName = (String) data.get("field");
                        Object fieldValue = data.get("value");

                        String url = issueServiceUrl + "/api/issues/" + issueId;
                        restTemplate.put(url, Map.of(fieldName, fieldValue));
                    }
                    break;

                case "ASSIGN_TO_REPORTER":
                    // Reassign issue to reporter
                    Map<String, Object> issueData = fetchIssueData(issueId);
                    if (issueData.containsKey("reporterId")) {
                        String assigneeId = issueData.get("reporterId").toString();
                        restTemplate.put(issueServiceUrl + "/api/issues/" + issueId,
                                       Map.of("assigneeId", assigneeId));
                    }
                    break;

                case "ASSIGN_TO_USER":
                    String userData = postFunction.getFunctionData();
                    if (userData != null) {
                        Map<String, Object> data = objectMapper.readValue(userData, Map.class);
                        String assigneeId = (String) data.get("userId");
                        if (assigneeId != null) {
                            restTemplate.put(issueServiceUrl + "/api/issues/" + issueId,
                                           Map.of("assigneeId", assigneeId));
                        }
                    }
                    break;

                case "FIRE_EVENT":
                    // Fire notification event
                    log.debug("Fire event post-function not yet implemented");
                    break;

                case "CREATE_SUBTASK":
                    // Create subtask based on configuration
                    log.debug("Create subtask post-function not yet implemented");
                    break;

                default:
                    log.warn("Unknown post-function type: {}", functionType);
            }
        } catch (Exception e) {
            log.error("Error executing post-function {}: {}", postFunction.getId(), e.getMessage());
            if (Boolean.TRUE.equals(postFunction.getFailOnError())) {
                throw new RuntimeException("Post-function execution failed: " + e.getMessage());
            }
        }
    }

    private WorkflowResponse mapToWorkflowResponse(Workflow workflow) {
        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflow.getId());
        List<UUID> statusIds = statuses.stream()
                .map(WorkflowStatus::getStatusId)
                .collect(Collectors.toList());
        int transitionCount = workflowTransitionRepository.findByWorkflowId(workflow.getId()).size();

        return WorkflowResponse.builder()
                .id(workflow.getId())
                .projectId(workflow.getProjectId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .isDefault(workflow.getIsDefault())
                .isDraft(workflow.getIsDraft())
                .isActive(workflow.getIsActive())
                .isSystem(workflow.getIsSystem())
                .statusIds(statusIds)
                .statusCount(statuses.size())
                .transitionCount(transitionCount)
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .build();
    }

    private TransitionResponse mapToTransitionResponse(WorkflowTransition transition) {
        return TransitionResponse.builder()
                .id(transition.getId())
                .workflowId(transition.getWorkflowId())
                .name(transition.getName())
                .description(transition.getDescription())
                .fromStatusId(transition.getFromStatusId())
                .toStatusId(transition.getToStatusId())
                .displayOrder(transition.getDisplayOrder())
                .icon(transition.getIcon())
                .requiresApproval(transition.getRequiresApproval())
                .approvalGroupId(transition.getApprovalGroupId())
                .allowAssigneeOverride(transition.getAllowAssigneeOverride())
                .allowUnassign(transition.getAllowUnassign())
                .fieldsRequired(transition.getFieldsRequired())
                .fieldsUpdated(transition.getFieldsUpdated())
                .fieldsHidden(transition.getFieldsHidden())
                .permissionCheck(transition.getPermissionCheck())
                .createdAt(transition.getCreatedAt())
                .build();
    }

    public TransitionDetailResponse mapTransitionDetail(WorkflowTransition transition) {
        List<WorkflowCondition> conditions = workflowConditionRepository.findByTransitionIdOrderBySequenceAsc(transition.getId());
        List<WorkflowValidator> validators = workflowValidatorRepository.findByTransitionIdOrderBySequenceAsc(transition.getId());
        List<WorkflowPostFunction> postFunctions = workflowPostFunctionRepository.findByTransitionIdOrderBySequenceAsc(transition.getId());

        return buildTransitionDetailResponse(transition, conditions, validators, postFunctions);
    }

    /**
     * Optimized version of mapTransitionDetail that uses pre-fetched data to avoid N+1 queries.
     * Used by getTransitionsWithDetails which already has the related data in memory.
     */
    private TransitionDetailResponse mapTransitionDetailWithMaps(
            WorkflowTransition transition,
            Map<UUID, List<WorkflowCondition>> conditionsMap,
            Map<UUID, List<WorkflowValidator>> validatorsMap,
            Map<UUID, List<WorkflowPostFunction>> postFunctionsMap) {

        List<WorkflowCondition> conditions = conditionsMap.getOrDefault(transition.getId(), List.of());
        List<WorkflowValidator> validators = validatorsMap.getOrDefault(transition.getId(), List.of());
        List<WorkflowPostFunction> postFunctions = postFunctionsMap.getOrDefault(transition.getId(), List.of());

        return buildTransitionDetailResponse(transition, conditions, validators, postFunctions);
    }

    private TransitionDetailResponse buildTransitionDetailResponse(
            WorkflowTransition transition,
            List<WorkflowCondition> conditions,
            List<WorkflowValidator> validators,
            List<WorkflowPostFunction> postFunctions) {
        return TransitionDetailResponse.builder()
                .id(transition.getId())
                .workflowId(transition.getWorkflowId())
                .name(transition.getName())
                .description(transition.getDescription())
                .fromStatusId(transition.getFromStatusId())
                .toStatusId(transition.getToStatusId())
                .displayOrder(transition.getDisplayOrder())
                .icon(transition.getIcon())
                .requiresApproval(transition.getRequiresApproval())
                .approvalGroupId(transition.getApprovalGroupId())
                .allowAssigneeOverride(transition.getAllowAssigneeOverride())
                .allowUnassign(transition.getAllowUnassign())
                .fieldsRequired(transition.getFieldsRequired())
                .fieldsUpdated(transition.getFieldsUpdated())
                .fieldsHidden(transition.getFieldsHidden())
                .fieldsAutoSubmit(transition.getFieldsAutoSubmit())
                .permissionCheck(transition.getPermissionCheck())
                .userGroupIds(transition.getUserGroupIds())
                .remoteLinkTransition(transition.getRemoteLinkTransition())
                .remoteLinkDirection(transition.getRemoteLinkDirection())
                .remoteLinkIssueLinkType(transition.getRemoteLinkIssueLinkType())
                .allowLoop(transition.getAllowLoop())
                .maxLoopCount(transition.getMaxLoopCount())
                .createdAt(transition.getCreatedAt())
                .conditions(conditions.stream().map(this::mapToConditionResponse).collect(Collectors.toList()))
                .validators(validators.stream().map(this::mapToValidatorResponse).collect(Collectors.toList()))
                .postFunctions(postFunctions.stream().map(this::mapToPostFunctionResponse).collect(Collectors.toList()))
                .build();
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

    private ValidatorResponse mapToValidatorResponse(WorkflowValidator validator) {
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

    private PostFunctionResponse mapToPostFunctionResponse(WorkflowPostFunction postFunction) {
        return PostFunctionResponse.builder()
                .id(postFunction.getId())
                .transitionId(postFunction.getTransitionId())
                .functionType(postFunction.getFunctionType())
                .functionData(postFunction.getFunctionData())
                .sequence(postFunction.getSequence())
                .async(postFunction.getAsync())
                .failOnError(postFunction.getFailOnError())
                .createdAt(postFunction.getCreatedAt())
                .build();
    }
}