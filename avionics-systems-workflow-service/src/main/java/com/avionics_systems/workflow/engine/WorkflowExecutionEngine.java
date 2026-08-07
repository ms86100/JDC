package com.avionics_systems.workflow.engine;



import com.avionics_systems.workflow.dto.*;

import com.avionics_systems.workflow.entity.Workflow;

import com.avionics_systems.workflow.entity.WorkflowTransition;

import com.avionics_systems.workflow.entity.WorkflowTransitionHistory;

import com.avionics_systems.workflow.repository.WorkflowTransitionHistoryRepository;

import com.avionics_systems.workflow.repository.WorkflowTransitionRepository;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;

import java.util.ArrayList;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.UUID;



/**

 * Single entry point for all workflow transitions (Avionics Systems DC runtime engine).

 * Pipeline steps align with issueworkflow.md Phase 2 (17-step model).

 */

@Service

@RequiredArgsConstructor

@Slf4j

public class WorkflowExecutionEngine {



    private final WorkflowContextResolver contextResolver;

    private final ConditionEvaluator conditionEvaluator;

    private final ValidatorExecutor validatorExecutor;

    private final PostFunctionPipeline postFunctionPipeline;

    private final TransitionScreenService transitionScreenService;

    private final WorkflowTransitionRepository workflowTransitionRepository;

    private final WorkflowTransitionHistoryRepository historyRepository;

    private final ProjectPermissionClient projectPermissionClient;

    private final TransitionPermissionEvaluator transitionPermissionEvaluator;

    private final WorkflowStatusResolver workflowStatusResolver;

    private final AvailableTransitionFallbackService transitionFallbackService;

    private final WorkflowIntegrationClient integrationClient;

    private final TransitionIdempotencyService idempotencyService;

    private final WorkflowEventPublisher eventPublisher;



    @Value("${avionics-systems.workflow.transition-fallback:false}")

    private boolean transitionFallbackEnabled;

    @Value("${app.workflow.permission.bypass-edit:EDIT_ISSUES}")

    private String bypassEditPermission;



    @Transactional

    public TransitionExecutionResponse execute(ExecuteTransitionRequest request) {

        long start = System.currentTimeMillis();

        UUID issueId = request.getIssueId();

        UUID userId = request.getUserId();

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {

            TransitionExecutionResponse cached = idempotencyService.getIfPresent(request.getIdempotencyKey());

            if (cached != null) {

                return cached;

            }

        }



        if (request.getStatusId() != null && request.getTransitionId() != null

                && workflowTransitionRepository.findById(request.getTransitionId()).isEmpty()) {

            if (!transitionFallbackEnabled) {

                return fail(

                        "Unknown workflow transition. Status changes must use a valid workflow transition.",

                        start,

                        issueId,

                        request.getTransitionId(),

                        Map.of());

            }

            return executeCatalogStatusChange(request, start);

        }



        try {

            WorkflowContext ctx = resolveContext(request);

            WorkflowTransition transition = ctx.getTransition();



            TransitionExecutionResponse permissionFailure = checkPermissions(ctx, start);

            if (permissionFailure != null) {

                recordAllHistory(ctx, false, permissionFailure.getError());

                return permissionFailure;

            }



            TransitionExecutionResponse statusFailure = validateCurrentStatus(ctx, start);

            if (statusFailure != null) {

                recordAllHistory(ctx, false, statusFailure.getError());

                return statusFailure;

            }



            validateOptimisticVersion(request, ctx, start);



            TransitionExecutionResponse transitionPermFailure = checkTransitionPermission(ctx, start);

            if (transitionPermFailure != null) {

                recordAllHistory(ctx, false, transitionPermFailure.getError());

                return transitionPermFailure;

            }



            List<String> conditionErrors = conditionEvaluator.evaluateAll(transition.getId(), ctx);

            if (!conditionErrors.isEmpty()) {

                return blocked(ctx, start, "Transition blocked by conditions", conditionErrors, Map.of());

            }



            ValidatorExecutor.ValidationResult validation = validatorExecutor.validate(transition.getId(), ctx);

            if (!validation.isEmpty()) {

                return blocked(ctx, start, "Transition blocked by validators", validation.errors(), validation.fieldErrors());

            }



            postFunctionPipeline.execute(ctx);

            recordAllHistory(ctx, true, null);



            TransitionExecutionResponse success = TransitionExecutionResponse.builder()

                    .success(true)

                    .issueId(issueId)

                    .transitionId(transition.getId())

                    .newStatusId(transition.getToStatusId())

                    .executionTimeMs(System.currentTimeMillis() - start)

                    .build();

            if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {

                idempotencyService.store(request.getIdempotencyKey(), success);

            }

            return success;



        } catch (Exception e) {

            log.error("Transition execution failed for issue {}: {}", issueId, e.getMessage());

            try {

                if (request.getTransitionId() != null) {

                    historyRepository.save(WorkflowTransitionHistory.builder()

                            .issueId(issueId)

                            .projectId(request.getProjectId())

                            .transitionId(request.getTransitionId())

                            .userId(request.getUserId())

                            .success(false)

                            .errorMessage(e.getMessage())

                            .executedAt(LocalDateTime.now())

                            .build());

                }

            } catch (Exception historyEx) {

                log.warn("Failed to record error history: {}", historyEx.getMessage());

            }

            return TransitionExecutionResponse.builder()

                    .success(false)

                    .issueId(issueId)

                    .error(e.getMessage())

                    .executionTimeMs(System.currentTimeMillis() - start)

                    .build();

        }

    }



    private WorkflowContext resolveContext(ExecuteTransitionRequest request) {

        WorkflowContext ctx = contextResolver.resolve(

                request.getIssueId(),

                request.getProjectId(),

                request.getUserId(),

                request.getTransitionId(),

                request.getStatusId());

        ctx.setComment(request.getComment());

        ctx.setResolutionId(request.getResolutionId());

        Map<String, Object> mergedScreenInput = request.getScreenInput() != null
                ? new java.util.HashMap<>(request.getScreenInput()) : new java.util.HashMap<>();
        if (request.getTimeSpentSeconds() != null && request.getTimeSpentSeconds() > 0) {
            mergedScreenInput.put("timeSpentSeconds", request.getTimeSpentSeconds());
        }
        if (request.getWorkDescription() != null) {
            mergedScreenInput.put("workDescription", request.getWorkDescription());
        }
        ctx.setScreenInput(mergedScreenInput);

        return ctx;

    }



    private TransitionExecutionResponse checkPermissions(WorkflowContext ctx, long start) {

        UUID userId = ctx.getUserId();

        if (userId != null && ctx.getProjectId() != null

                && !projectPermissionClient.hasPermission(userId, ctx.getProjectId(), bypassEditPermission)) {

            @SuppressWarnings("unchecked")

            List<String> granted = (List<String>) ctx.getUserData().get("permissions");

            boolean hasEdit = granted != null && granted.contains(bypassEditPermission);

            if (!hasEdit && !projectPermissionClient.isFailOpen()) {

                return fail("User lacks EDIT_ISSUES permission for this project", start, ctx.getIssueId(), ctx.getTransition().getId(), Map.of());

            }

        }

        return null;

    }



    private TransitionExecutionResponse validateCurrentStatus(WorkflowContext ctx, long start) {

        UUID requiredFrom = ctx.getTransition().getFromStatusId();

        boolean ok = workflowStatusResolver.statusesMatchForTransition(

                ctx.getWorkflow().getId(),

                ctx.getCurrentStatusId(),

                requiredFrom,

                ctx.getIssueData());

        if (!ok) {

            return fail("Invalid transition: issue is not in required source status", start, ctx.getIssueId(), ctx.getTransition().getId(), Map.of());

        }

        return null;

    }



    private TransitionExecutionResponse validateOptimisticVersion(ExecuteTransitionRequest request, WorkflowContext ctx, long start) {

        if (request.getExpectedVersion() == null) {

            return null;

        }

        Object rawVersion = ctx.getIssueData() != null ? ctx.getIssueData().get("version") : null;

        Long current = rawVersion instanceof Number n ? n.longValue() : null;

        if (current != null && !request.getExpectedVersion().equals(current)) {

            throw new com.avionics_systems.workflow.exception.TransitionConflictException(

                    "Issue was updated by another user. Refresh the page and try again (expected version "

                            + request.getExpectedVersion() + ", current " + current + ").");

        }

        return null;

    }



    private TransitionExecutionResponse checkTransitionPermission(WorkflowContext ctx, long start) {

        if (!transitionPermissionEvaluator.canPerformTransition(ctx.getTransition(), ctx)) {

            String perm = transitionPermissionEvaluator.requiredPermissionLabel(ctx.getTransition());

            String msg = perm != null

                    ? "Transition requires permission: " + perm

                    : "Transition not allowed for your role or group";

            return fail(msg, start, ctx.getIssueId(), ctx.getTransition().getId(), Map.of());

        }

        return null;

    }



    private TransitionExecutionResponse blocked(

            WorkflowContext ctx,

            long start,

            String error,

            List<String> errors,

            Map<String, String> fieldErrors) {

        recordAllHistory(ctx, false, String.join("; ", errors));

        return TransitionExecutionResponse.builder()

                .success(false)

                .issueId(ctx.getIssueId())

                .transitionId(ctx.getTransition().getId())

                .error(error)

                .errors(errors)

                .validationErrors(fieldErrors != null ? new LinkedHashMap<>(fieldErrors) : Map.of())

                .executionTimeMs(System.currentTimeMillis() - start)

                .build();

    }



    @Transactional(readOnly = true)

    public AvailableTransitionResponse getAvailableTransitions(UUID issueId, UUID projectId, UUID userId) {

        WorkflowContext ctx = contextResolver.resolveForIssue(issueId, projectId, userId);

        Workflow workflow = ctx.getWorkflow();

        UUID fromStatusId = workflowStatusResolver.resolveForTransitions(

                workflow.getId(), ctx.getCurrentStatusId(), ctx.getIssueData());

        List<WorkflowTransition> outgoing = workflowTransitionRepository

                .findByWorkflowIdAndFromStatusId(workflow.getId(), fromStatusId);



        List<AvailableTransitionResponse.AvailableTransitionItem> items = new ArrayList<>();

        boolean canEdit = userId == null

                || ctx.getProjectId() == null

                || projectPermissionClient.hasPermission(userId, ctx.getProjectId(), bypassEditPermission)

                || hasGrantedPermission(ctx, bypassEditPermission)

                || projectPermissionClient.isFailOpen();



        for (WorkflowTransition t : outgoing) {

            if (!canEdit) {

                continue;

            }

            WorkflowContext probe = WorkflowContext.builder()

                    .issueId(issueId)

                    .projectId(ctx.getProjectId())

                    .userId(userId)

                    .currentStatusId(ctx.getCurrentStatusId())

                    .issueData(ctx.getIssueData())

                    .userData(ctx.getUserData())

                    .transition(t)

                    .build();

            if (!transitionPermissionEvaluator.canPerformTransition(t, probe)) {

                continue;

            }

            if (conditionEvaluator.evaluateAll(t.getId(), probe).isEmpty()) {

                items.add(transitionScreenService.enrichTransitionItem(

                        t,

                        transitionPermissionEvaluator.requiredPermissionLabel(t)));

            }

        }



        if (items.isEmpty() && canEdit && transitionFallbackEnabled) {

            items.addAll(transitionFallbackService.buildFallbackItems(ctx, workflow));

        }



        return AvailableTransitionResponse.builder()

                .issueId(issueId)

                .workflowId(workflow.getId())

                .currentStatusId(ctx.getCurrentStatusId())

                .transitions(items)

                .build();

    }



    private boolean hasGrantedPermission(WorkflowContext ctx, String permission) {

        if (ctx.getUserData() == null || permission == null) {

            return false;

        }

        Object raw = ctx.getUserData().get("permissions");

        if (raw instanceof List<?> list) {

            return list.stream().anyMatch(permission::equals);

        }

        return false;

    }



    private void recordAllHistory(WorkflowContext ctx, boolean success, String error) {

        recordWorkflowHistory(ctx, success, error);

        integrationClient.recordIssueTransitionHistory(

                ctx.getIssueId(),

                ctx.getProjectId(),

                ctx.getWorkflow().getId(),

                ctx.getTransition().getId(),

                ctx.getTransition().getName(),

                ctx.getCurrentStatusId(),

                ctx.getTransition().getToStatusId(),

                ctx.getUserId(),

                ctx.getComment(),

                success,

                error);

    }



    private void recordWorkflowHistory(WorkflowContext ctx, boolean success, String error) {

        historyRepository.save(WorkflowTransitionHistory.builder()

                .issueId(ctx.getIssueId())

                .projectId(ctx.getProjectId())

                .workflowId(ctx.getWorkflow().getId())

                .transitionId(ctx.getTransition().getId())

                .transitionName(ctx.getTransition().getName())

                .fromStatusId(ctx.getCurrentStatusId())

                .toStatusId(ctx.getTransition().getToStatusId())

                .userId(ctx.getUserId())

                .comment(ctx.getComment())

                .screenInput(ctx.getScreenInput())

                .success(success)

                .errorMessage(error)

                .executedAt(LocalDateTime.now())

                .build());

    }



    private TransitionExecutionResponse executeCatalogStatusChange(ExecuteTransitionRequest request, long start) {

        UUID issueId = request.getIssueId();

        UUID userId = request.getUserId();



        // Permission check: require EDIT_ISSUES permission

        if (userId != null && request.getProjectId() != null

                && !projectPermissionClient.hasPermission(userId, request.getProjectId(), bypassEditPermission)

                && !projectPermissionClient.isFailOpen()) {

            return fail("User lacks EDIT_ISSUES permission for catalog status change",

                    start, issueId, request.getTransitionId(), Map.of());

        }



        Map<String, Object> extra = new java.util.HashMap<>();

        if (request.getComment() != null) {

            extra.put("comment", request.getComment());

        }

        if (request.getResolutionId() != null) {

            extra.put("resolutionId", request.getResolutionId().toString());

        }

        if (request.getScreenInput() != null) {

            extra.put("screenInput", request.getScreenInput());

        }

        integrationClient.updateIssueStatusInternal(

                issueId, request.getProjectId(), request.getStatusId(), extra);



        // Record transition history for the catalog fallback path

        try {

            historyRepository.save(WorkflowTransitionHistory.builder()

                    .issueId(issueId)

                    .projectId(request.getProjectId())

                    .workflowId(request.getTransitionId()) // use transitionId as workflow reference for catalog path

                    .transitionId(request.getTransitionId())

                    .transitionName("Catalog Status Change")

                    .fromStatusId(request.getStatusId()) // best-effort: actual from-status not known in catalog path

                    .toStatusId(request.getStatusId())

                    .userId(userId)

                    .comment(request.getComment())

                    .screenInput(request.getScreenInput())

                    .success(true)

                    .executedAt(LocalDateTime.now())

                    .build());

        } catch (Exception e) {

            log.warn("Failed to record catalog status change history: {}", e.getMessage());

        }



        // Publish event via generic publish (no transition object available in catalog path)

        try {

            Map<String, Object> payload = new java.util.HashMap<>();

            payload.put("issueId", issueId.toString());

            payload.put("projectId", request.getProjectId() != null ? request.getProjectId().toString() : null);

            payload.put("statusId", request.getStatusId() != null ? request.getStatusId().toString() : null);

            payload.put("userId", userId != null ? userId.toString() : null);

            payload.put("catalogFallback", true);

            eventPublisher.publish(issueId, "CATALOG_STATUS_CHANGED", payload);

        } catch (Exception e) {

            log.warn("Failed to publish catalog status change event: {}", e.getMessage());

        }



        TransitionExecutionResponse success = TransitionExecutionResponse.builder()

                .success(true)

                .issueId(issueId)

                .newStatusId(request.getStatusId())

                .executionTimeMs(System.currentTimeMillis() - start)

                .build();



        // Cache in idempotencyService if the request has an idempotency key

        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {

            idempotencyService.store(request.getIdempotencyKey(), success);

        }



        return success;

    }



    private TransitionExecutionResponse fail(String msg, long start, UUID issueId, UUID transitionId, Map<String, String> fieldErrors) {

        return TransitionExecutionResponse.builder()

                .success(false)

                .issueId(issueId)

                .transitionId(transitionId)

                .error(msg)

                .validationErrors(fieldErrors != null ? fieldErrors : Map.of())

                .executionTimeMs(System.currentTimeMillis() - start)

                .build();

    }

}


