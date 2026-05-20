package com.jira.workflow.engine;

import com.jira.workflow.dto.*;
import com.jira.workflow.entity.Workflow;
import com.jira.workflow.entity.WorkflowTransition;
import com.jira.workflow.entity.WorkflowTransitionHistory;
import com.jira.workflow.repository.WorkflowTransitionHistoryRepository;
import com.jira.workflow.repository.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Single entry point for all workflow transitions (Jira DC runtime engine).
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

    @Transactional
    public TransitionExecutionResponse execute(ExecuteTransitionRequest request) {
        long start = System.currentTimeMillis();
        UUID issueId = request.getIssueId();
        UUID userId = request.getUserId();

        try {
            WorkflowContext ctx = contextResolver.resolve(
                    issueId,
                    request.getProjectId(),
                    userId,
                    request.getTransitionId(),
                    request.getStatusId());
            ctx.setComment(request.getComment());
            ctx.setResolutionId(request.getResolutionId());
            ctx.setScreenInput(request.getScreenInput());

            WorkflowTransition transition = ctx.getTransition();
            if (!transition.getFromStatusId().equals(ctx.getCurrentStatusId())) {
                return fail("Invalid transition: issue is not in required source status", start, issueId, transition.getId());
            }

            List<String> conditionErrors = conditionEvaluator.evaluateAll(transition.getId(), ctx);
            if (!conditionErrors.isEmpty()) {
                recordHistory(ctx, false, String.join("; ", conditionErrors));
                return TransitionExecutionResponse.builder()
                        .success(false)
                        .issueId(issueId)
                        .transitionId(transition.getId())
                        .error("Transition blocked by conditions")
                        .errors(conditionErrors)
                        .executionTimeMs(System.currentTimeMillis() - start)
                        .build();
            }

            List<String> validationErrors = validatorExecutor.validate(transition.getId(), ctx);
            if (!validationErrors.isEmpty()) {
                recordHistory(ctx, false, String.join("; ", validationErrors));
                return TransitionExecutionResponse.builder()
                        .success(false)
                        .issueId(issueId)
                        .transitionId(transition.getId())
                        .error("Transition blocked by validators")
                        .errors(validationErrors)
                        .executionTimeMs(System.currentTimeMillis() - start)
                        .build();
            }

            postFunctionPipeline.execute(ctx);
            recordHistory(ctx, true, null);

            return TransitionExecutionResponse.builder()
                    .success(true)
                    .issueId(issueId)
                    .transitionId(transition.getId())
                    .newStatusId(transition.getToStatusId())
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("Transition execution failed for issue {}: {}", issueId, e.getMessage());
            return TransitionExecutionResponse.builder()
                    .success(false)
                    .issueId(issueId)
                    .error(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public AvailableTransitionResponse getAvailableTransitions(UUID issueId, UUID projectId, UUID userId) {
        WorkflowContext ctx = contextResolver.resolveForIssue(issueId, projectId, userId);
        Workflow workflow = ctx.getWorkflow();
        List<WorkflowTransition> outgoing = workflowTransitionRepository
                .findByWorkflowIdAndFromStatusId(workflow.getId(), ctx.getCurrentStatusId());

        List<AvailableTransitionResponse.AvailableTransitionItem> items = new ArrayList<>();
        for (WorkflowTransition t : outgoing) {
            WorkflowContext probe = WorkflowContext.builder()
                    .issueId(issueId)
                    .projectId(ctx.getProjectId())
                    .userId(userId)
                    .currentStatusId(ctx.getCurrentStatusId())
                    .issueData(ctx.getIssueData())
                    .userData(ctx.getUserData())
                    .transition(t)
                    .build();
            if (conditionEvaluator.evaluateAll(t.getId(), probe).isEmpty()) {
                items.add(transitionScreenService.enrichTransitionItem(t));
            }
        }

        return AvailableTransitionResponse.builder()
                .issueId(issueId)
                .workflowId(workflow.getId())
                .currentStatusId(ctx.getCurrentStatusId())
                .transitions(items)
                .build();
    }

    private void recordHistory(WorkflowContext ctx, boolean success, String error) {
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

    private TransitionExecutionResponse fail(String msg, long start, UUID issueId, UUID transitionId) {
        return TransitionExecutionResponse.builder()
                .success(false)
                .issueId(issueId)
                .transitionId(transitionId)
                .error(msg)
                .executionTimeMs(System.currentTimeMillis() - start)
                .build();
    }
}
