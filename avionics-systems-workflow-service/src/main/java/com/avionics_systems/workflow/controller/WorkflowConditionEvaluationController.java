package com.avionics_systems.workflow.controller;

import com.avionics_systems.workflow.dto.EvaluateConditionsRequest;
import com.avionics_systems.workflow.dto.EvaluateConditionsResponse;
import com.avionics_systems.workflow.entity.WorkflowCondition;
import com.avionics_systems.workflow.repository.WorkflowConditionRepository;
import com.avionics_systems.workflow.service.ConditionEvaluationContext;
import com.avionics_systems.workflow.service.WorkflowConditionEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST API for workflow condition evaluation.
 * Allows external services (like avionics-systems-issue-service) to evaluate
 * workflow conditions without executing the full transition.
 */
@RestController
@RequestMapping("/api/workflows/conditions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Condition Evaluation", description = "Workflow condition evaluation API")
public class WorkflowConditionEvaluationController {

    private final WorkflowConditionEvaluationService evaluationService;
    private final WorkflowConditionRepository conditionRepository;

    /**
     * Evaluate conditions for a specific transition.
     * Returns whether all conditions are met and any errors if not.
     */
    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate workflow conditions for a transition",
               description = "Checks if all conditions for a transition are satisfied")
    public ResponseEntity<EvaluateConditionsResponse> evaluateConditions(
            @RequestBody EvaluateConditionsRequest request) {
        long start = System.currentTimeMillis();

        try {
            // Build evaluation context from request
            ConditionEvaluationContext context = buildContext(request);

            // Fetch conditions for the transition
            List<WorkflowCondition> conditions = conditionRepository
                    .findByTransitionIdOrderBySequenceAsc(request.getTransitionId());

            // Evaluate conditions
            boolean allPassed = evaluationService.evaluateConditions(conditions, context);

            long evaluationTime = System.currentTimeMillis() - start;

            if (allPassed) {
                return ResponseEntity.ok(EvaluateConditionsResponse.success(
                        request.getTransitionId(), evaluationTime));
            } else {
                // Collect failed condition details
                List<String> errors = new ArrayList<>();
                List<EvaluateConditionsResponse.FailedConditionDetail> failedConditions = new ArrayList<>();

                for (WorkflowCondition condition : conditions) {
                    boolean result = evaluationService.evaluateCondition(condition, context);
                    boolean passed = condition.getNegate() != null && condition.getNegate() ? !result : result;

                    if (!passed) {
                        errors.add("Condition not met: " + condition.getConditionType());

                        failedConditions.add(EvaluateConditionsResponse.FailedConditionDetail.builder()
                                .conditionType(condition.getConditionType())
                                .fieldName(condition.getFieldName())
                                .operator(condition.getOperator())
                                .expectedValue(condition.getValue())
                                .actualValue(context.getFieldValue(condition.getFieldName()) != null
                                        ? context.getFieldValue(condition.getFieldName()).toString() : null)
                                .description(buildConditionDescription(condition))
                                .build());
                    }
                }

                return ResponseEntity.ok(EvaluateConditionsResponse.failure(
                        request.getTransitionId(), errors, failedConditions, evaluationTime));
            }

        } catch (Exception e) {
            log.error("Condition evaluation failed for transition {}: {}",
                    request.getTransitionId(), e.getMessage());
            return ResponseEntity.internalServerError().body(EvaluateConditionsResponse.failure(
                    request.getTransitionId(),
                    List.of("Evaluation error: " + e.getMessage()),
                    List.of(),
                    System.currentTimeMillis() - start));
        }
    }

    /**
     * Quick check if user can perform a transition.
     * Returns 200 if conditions are met, 403 if not.
     */
    @GetMapping("/transitions/{transitionId}/can-execute")
    @Operation(summary = "Check if conditions allow transition execution")
    public ResponseEntity<CanExecuteResponse> canExecuteTransition(
            @PathVariable UUID transitionId,
            @RequestParam UUID userId,
            @RequestParam UUID issueId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID previousStatusId,
            @RequestParam(required = false) UUID currentStatusId,
            @RequestHeader(value = "X-User-Groups", required = false) String groupsHeader) {

        long start = System.currentTimeMillis();

        try {
            // Build minimal request
            EvaluateConditionsRequest request = EvaluateConditionsRequest.builder()
                    .transitionId(transitionId)
                    .userId(userId)
                    .issueId(issueId)
                    .projectId(projectId)
                    .previousStatusId(previousStatusId)
                    .currentStatusId(currentStatusId)
                    .build();

            // Parse groups if provided
            if (groupsHeader != null && !groupsHeader.isBlank()) {
                java.util.Set<String> groups = new java.util.HashSet<>();
                for (String g : groupsHeader.split(",")) {
                    groups.add(g.trim());
                }
                request.setUserGroups(groups);
            }

            ConditionEvaluationContext context = buildContext(request);
            List<WorkflowCondition> conditions = conditionRepository
                    .findByTransitionIdOrderBySequenceAsc(transitionId);
            boolean allPassed = evaluationService.evaluateConditions(conditions, context);

            long evaluationTime = System.currentTimeMillis() - start;

            if (allPassed) {
                return ResponseEntity.ok(CanExecuteResponse.builder()
                        .canExecute(true)
                        .transitionId(transitionId)
                        .evaluationTimeMs(evaluationTime)
                        .build());
            } else {
                return ResponseEntity.status(403).body(CanExecuteResponse.builder()
                        .canExecute(false)
                        .transitionId(transitionId)
                        .evaluationTimeMs(evaluationTime)
                        .reason("Conditions not satisfied")
                        .build());
            }

        } catch (Exception e) {
            log.error("Can execute check failed for transition {}: {}", transitionId, e.getMessage());
            return ResponseEntity.internalServerError().body(CanExecuteResponse.builder()
                    .canExecute(false)
                    .transitionId(transitionId)
                    .reason("Evaluation error: " + e.getMessage())
                    .build());
        }
    }

    private ConditionEvaluationContext buildContext(EvaluateConditionsRequest request) {
        return ConditionEvaluationContext.builder()
                .currentUserId(request.getUserId())
                .currentUserGroups(request.getUserGroups())
                .projectId(request.getProjectId())
                .issueFields(request.getFields())
                .issue(request.getFields())
                .previousStatusId(request.getPreviousStatusId())
                .currentStatusId(request.getCurrentStatusId())
                .transitionId(request.getTransitionId())
                .reporterId(request.getReporterId())
                .assigneeId(request.getAssigneeId())
                .screenInput(request.getScreenInput())
                .build();
    }

    private String buildConditionDescription(WorkflowCondition condition) {
        StringBuilder desc = new StringBuilder();
        desc.append(condition.getConditionType());
        if (condition.getFieldName() != null) {
            desc.append(" on field '").append(condition.getFieldName()).append("'");
        }
        if (condition.getOperator() != null) {
            desc.append(" [").append(condition.getOperator()).append("]");
        }
        if (condition.getValue() != null) {
            desc.append(" '").append(condition.getValue()).append("'");
        }
        return desc.toString();
    }

    /**
     * Simple response for can-execute check.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CanExecuteResponse {
        private boolean canExecute;
        private UUID transitionId;
        private String reason;
        private long evaluationTimeMs;
    }
}