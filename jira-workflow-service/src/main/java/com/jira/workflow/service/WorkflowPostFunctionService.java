package com.jira.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.dto.WorkflowPostFunctionRequest;
import com.jira.workflow.dto.WorkflowPostFunctionResponse;
import com.jira.workflow.entity.WorkflowPostFunction;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.WorkflowPostFunctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing workflow post-functions.
 * Provides CRUD operations and execution of post-functions attached to transitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowPostFunctionService {

    private final WorkflowPostFunctionRepository workflowPostFunctionRepository;
    private final PostFunctionExecutionEngine executionEngine;
    private final ObjectMapper objectMapper;

    /**
     * Create a new post-function for a transition.
     */
    @Transactional
    public WorkflowPostFunctionResponse createPostFunction(UUID transitionId, WorkflowPostFunctionRequest request) {
        log.info("Creating post-function for transition: {} with type: {}", transitionId, request.getPostFunctionType());

        WorkflowPostFunction postFunction = WorkflowPostFunction.builder()
                .transitionId(transitionId)
                .functionType(request.getPostFunctionType())
                .functionData(request.getFunctionData())
                .sequence(request.getSequence() != null ? request.getSequence() : getNextSequence(transitionId))
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .continueOnError(request.getContinueOnError() != null ? request.getContinueOnError() : false)
                .async(request.getAsync() != null ? request.getAsync() : false)
                .failOnError(true)
                .build();

        postFunction = workflowPostFunctionRepository.save(postFunction);
        log.info("Post-function created: {}", postFunction.getId());

        return mapToResponse(postFunction);
    }

    /**
     * Update an existing post-function.
     */
    @Transactional
    public WorkflowPostFunctionResponse updatePostFunction(UUID id, WorkflowPostFunctionRequest request) {
        log.info("Updating post-function: {}", id);

        WorkflowPostFunction postFunction = workflowPostFunctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PostFunction", "id", id));

        if (request.getPostFunctionType() != null) {
            postFunction.setFunctionType(request.getPostFunctionType());
        }
        if (request.getFunctionData() != null) {
            postFunction.setFunctionData(request.getFunctionData());
        }
        if (request.getSequence() != null) {
            postFunction.setSequence(request.getSequence());
        }
        if (request.getEnabled() != null) {
            postFunction.setEnabled(request.getEnabled());
        }
        if (request.getContinueOnError() != null) {
            postFunction.setContinueOnError(request.getContinueOnError());
        }
        if (request.getAsync() != null) {
            postFunction.setAsync(request.getAsync());
        }

        postFunction = workflowPostFunctionRepository.save(postFunction);
        log.info("Post-function updated: {}", id);

        return mapToResponse(postFunction);
    }

    /**
     * Delete a post-function by ID.
     */
    @Transactional
    public void deletePostFunction(UUID id) {
        log.info("Deleting post-function: {}", id);

        WorkflowPostFunction postFunction = workflowPostFunctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PostFunction", "id", id));

        workflowPostFunctionRepository.delete(postFunction);
        log.info("Post-function deleted: {}", id);
    }

    /**
     * Get all post-functions for a transition, ordered by sequence.
     */
    @Transactional(readOnly = true)
    public List<WorkflowPostFunctionResponse> getPostFunctionsByTransition(UUID transitionId) {
        log.debug("Fetching post-functions for transition: {}", transitionId);

        List<WorkflowPostFunction> functions = workflowPostFunctionRepository
                .findByTransitionIdOrderBySequenceAsc(transitionId);

        return functions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a post-function by ID.
     */
    @Transactional(readOnly = true)
    public WorkflowPostFunctionResponse getPostFunctionById(UUID id) {
        log.debug("Fetching post-function: {}", id);

        WorkflowPostFunction postFunction = workflowPostFunctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PostFunction", "id", id));

        return mapToResponse(postFunction);
    }

    /**
     * Bulk create post-functions for a transition.
     */
    @Transactional
    public List<WorkflowPostFunctionResponse> bulkCreatePostFunctions(UUID transitionId,
                                                                      List<WorkflowPostFunctionRequest> requests) {
        log.info("Bulk creating {} post-functions for transition: {}", requests.size(), transitionId);

        int baseSequence = getNextSequence(transitionId);

        List<WorkflowPostFunction> functions = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            WorkflowPostFunctionRequest request = requests.get(i);
            WorkflowPostFunction pf = WorkflowPostFunction.builder()
                    .transitionId(transitionId)
                    .functionType(request.getPostFunctionType())
                    .functionData(request.getFunctionData())
                    .sequence(request.getSequence() != null ? request.getSequence() : baseSequence + i)
                    .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                    .continueOnError(request.getContinueOnError() != null ? request.getContinueOnError() : false)
                    .async(request.getAsync() != null ? request.getAsync() : false)
                    .failOnError(true)
                    .build();
            functions.add(pf);
        }

        List<WorkflowPostFunction> savedFunctions = workflowPostFunctionRepository.saveAll(functions);
        log.info("Bulk created {} post-functions", savedFunctions.size());

        return savedFunctions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete all post-functions for a transition.
     */
    @Transactional
    public void deletePostFunctionsByTransition(UUID transitionId) {
        log.info("Deleting all post-functions for transition: {}", transitionId);
        workflowPostFunctionRepository.deleteByTransitionId(transitionId);
        log.info("All post-functions deleted for transition: {}", transitionId);
    }

    /**
     * Execute all post-functions for a transition.
     * This is the key method called after a transition completes.
     *
     * @param transitionId the transition ID
     * @param context execution context with issue data
     * @return execution results for each post-function
     */
    @Transactional(readOnly = true)
    public List<PostFunctionExecutionResult> executePostFunctions(UUID transitionId, Map<String, Object> context) {
        log.info("Executing post-functions for transition: {}", transitionId);

        List<WorkflowPostFunction> functions = workflowPostFunctionRepository
                .findByTransitionIdOrderBySequenceAsc(transitionId);

        List<PostFunctionExecutionResult> results = new ArrayList<>();

        for (WorkflowPostFunction pf : functions) {
            if (!Boolean.TRUE.equals(pf.getEnabled())) {
                log.debug("Skipping disabled post-function: {} of type {}", pf.getId(), pf.getFunctionType());
                results.add(PostFunctionExecutionResult.builder()
                        .postFunctionId(pf.getId())
                        .functionType(pf.getFunctionType())
                        .executed(false)
                        .skipped(true)
                        .reason("Post-function is disabled")
                        .build());
                continue;
            }

            try {
                long start = System.currentTimeMillis();
                executionEngine.executePostFunction(pf, context);
                long duration = System.currentTimeMillis() - start;

                results.add(PostFunctionExecutionResult.builder()
                        .postFunctionId(pf.getId())
                        .functionType(pf.getFunctionType())
                        .executed(true)
                        .success(true)
                        .executionTimeMs(duration)
                        .build());

                log.info("Post-function {} ({}) executed successfully in {}ms",
                        pf.getId(), pf.getFunctionType(), duration);

            } catch (Exception e) {
                long errorId = UUID.randomUUID().getMostSignificantBits() >>> 32;
                log.error("Post-function {} ({}) failed: {} [error-{}]",
                        pf.getId(), pf.getFunctionType(), e.getMessage(), errorId);

                results.add(PostFunctionExecutionResult.builder()
                        .postFunctionId(pf.getId())
                        .functionType(pf.getFunctionType())
                        .executed(true)
                        .success(false)
                        .errorId(errorId)
                        .errorMessage(e.getMessage())
                        .build());

                if (!Boolean.TRUE.equals(pf.getContinueOnError()) && Boolean.TRUE.equals(pf.getFailOnError())) {
                    log.warn("Post-function {} failed with failOnError=true, stopping execution", pf.getId());
                    break;
                }
            }
        }

        log.info("Post-functions execution complete for transition: {}. Results: {} total, {} successful",
                transitionId, results.size(),
                results.stream().filter(PostFunctionExecutionResult::isSuccess).count());

        return results;
    }

    /**
     * Execute post-functions asynchronously (fire-and-forget).
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void executePostFunctionsAsync(UUID transitionId, Map<String, Object> context) {
        try {
            executePostFunctions(transitionId, context);
        } catch (Exception e) {
            log.error("Async post-function execution failed for transition {}: {}",
                    transitionId, e.getMessage());
        }
    }

    /**
     * Enable or disable a post-function.
     */
    @Transactional
    public WorkflowPostFunctionResponse togglePostFunction(UUID id, boolean enabled) {
        log.info("Setting post-function {} enabled={}", id, enabled);

        WorkflowPostFunction postFunction = workflowPostFunctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PostFunction", "id", id));

        postFunction.setEnabled(enabled);
        postFunction = workflowPostFunctionRepository.save(postFunction);

        return mapToResponse(postFunction);
    }

    /**
     * Reorder post-functions for a transition.
     */
    @Transactional
    public List<WorkflowPostFunctionResponse> reorderPostFunctions(UUID transitionId, List<UUID> orderedIds) {
        log.info("Reordering {} post-functions for transition: {}", orderedIds.size(), transitionId);

        List<WorkflowPostFunction> functions = workflowPostFunctionRepository
                .findByTransitionIdOrderBySequenceAsc(transitionId);

        Map<UUID, WorkflowPostFunction> functionMap = new HashMap<>();
        for (WorkflowPostFunction pf : functions) {
            functionMap.put(pf.getId(), pf);
        }

        List<WorkflowPostFunction> reordered = new ArrayList<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            WorkflowPostFunction pf = functionMap.get(id);
            if (pf != null) {
                pf.setSequence(i);
                reordered.add(pf);
            }
        }

        List<WorkflowPostFunction> saved = workflowPostFunctionRepository.saveAll(reordered);
        log.info("Reordered {} post-functions", saved.size());

        return saved.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Parse function data JSON to Map.
     */
    public Map<String, Object> parseFunctionData(String functionData) {
        if (functionData == null || functionData.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(functionData, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse function data JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private int getNextSequence(UUID transitionId) {
        List<WorkflowPostFunction> existing = workflowPostFunctionRepository
                .findByTransitionIdOrderBySequenceAsc(transitionId);
        return existing.stream()
                .mapToInt(WorkflowPostFunction::getSequence)
                .max()
                .orElse(-1) + 1;
    }

    private WorkflowPostFunctionResponse mapToResponse(WorkflowPostFunction pf) {
        return WorkflowPostFunctionResponse.builder()
                .id(pf.getId())
                .transitionId(pf.getTransitionId())
                .postFunctionType(pf.getFunctionType())
                .functionData(pf.getFunctionData())
                .sequence(pf.getSequence())
                .enabled(pf.getEnabled())
                .continueOnError(pf.getContinueOnError())
                .async(pf.getAsync())
                .createdAt(pf.getCreatedAt())
                .updatedAt(pf.getUpdatedAt())
                .build();
    }

    /**
     * Result of executing a single post-function.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PostFunctionExecutionResult {
        private UUID postFunctionId;
        private String functionType;
        private boolean executed;
        private boolean skipped;
        private String reason;
        private boolean success;
        private long executionTimeMs;
        private long errorId;
        private String errorMessage;
        private LocalDateTime executedAt;

        public boolean isSuccess() {
            return success;
        }
    }
}