package com.avionics_systems.workflow.engine;

import com.avionics_systems.workflow.entity.WorkflowPostFunction;
import com.avionics_systems.workflow.repository.WorkflowPostFunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Avionics Systems DC essential post-functions (ordered) plus configurable optional functions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostFunctionPipeline {

    private static final Set<String> ESSENTIAL_TYPES = Set.of(
            WorkflowPostFunction.TYPE_STORE_ISSUE,
            WorkflowPostFunction.TYPE_SET_ISSUE_STATUS,
            WorkflowPostFunction.TYPE_ADD_COMMENT,
            WorkflowPostFunction.TYPE_GENERATE_CHANGE_HISTORY,
            WorkflowPostFunction.TYPE_REINDEX_ISSUE,
            WorkflowPostFunction.TYPE_FIRE_EVENT
    );

    private final WorkflowPostFunctionRepository workflowPostFunctionRepository;
    private final PostFunctionExecutor postFunctionExecutor;

    public void execute(WorkflowContext ctx) {
        postFunctionExecutor.executeEssentialChain(ctx);

        List<WorkflowPostFunction> configured = workflowPostFunctionRepository
                .findByTransitionIdOrderBySequenceAsc(ctx.getTransition().getId());

        List<WorkflowPostFunction> syncFns = new ArrayList<>();
        List<WorkflowPostFunction> asyncFns = new ArrayList<>();
        for (WorkflowPostFunction pf : configured) {
            if (ESSENTIAL_TYPES.contains(pf.getFunctionType())) {
                continue;
            }
            if (Boolean.TRUE.equals(pf.getAsync())) {
                asyncFns.add(pf);
            } else {
                syncFns.add(pf);
            }
        }

        // Execute sync sequentially
        for (WorkflowPostFunction pf : syncFns) {
            executeOne(pf, ctx);
        }

        // Execute async fire-and-forget
        for (WorkflowPostFunction pf : asyncFns) {
            CompletableFuture.runAsync(() -> {
                try {
                    postFunctionExecutor.executeConfigured(pf, ctx);
                } catch (Exception e) {
                    log.warn("Async post-function {} failed: {}", pf.getFunctionType(), e.getMessage());
                }
            });
        }

        postFunctionExecutor.fireEvent(ctx);
    }

    private void executeOne(WorkflowPostFunction pf, WorkflowContext ctx) {
        try {
            postFunctionExecutor.executeConfigured(pf, ctx);
        } catch (Exception e) {
            if (Boolean.TRUE.equals(pf.getFailOnError())) {
                throw new IllegalStateException("Post-function failed: " + pf.getFunctionType(), e);
            }
            log.warn("Post-function {} failed (continue): {}", pf.getFunctionType(), e.getMessage());
        }
    }
}
