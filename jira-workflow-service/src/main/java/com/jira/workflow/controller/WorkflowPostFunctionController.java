package com.jira.workflow.controller;

import com.jira.workflow.dto.WorkflowPostFunctionRequest;
import com.jira.workflow.dto.WorkflowPostFunctionResponse;
import com.jira.workflow.service.WorkflowPostFunctionService;
import com.jira.workflow.service.WorkflowPostFunctionService.PostFunctionExecutionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing workflow post-functions.
 * Provides CRUD operations for post-functions attached to transitions.
 * Base path: /api/workflow/post-functions
 */
@RestController
@RequestMapping("/api/workflow/post-functions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Post-Functions", description = "CRUD operations for workflow transition post-functions")
public class WorkflowPostFunctionController {

    private final WorkflowPostFunctionService workflowPostFunctionService;

    @GetMapping
    @Operation(
            summary = "List all post-functions",
            description = "Returns all post-functions. Optionally filter by transitionId."
    )
    public ResponseEntity<List<WorkflowPostFunctionResponse>> listAllPostFunctions(
            @Parameter(description = "Filter by transition ID")
            @RequestParam(required = false) UUID transitionId) {

        if (transitionId != null) {
            return ResponseEntity.ok(workflowPostFunctionService.getPostFunctionsByTransition(transitionId));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get post-function by ID",
            description = "Returns a specific post-function by its ID"
    )
    public ResponseEntity<WorkflowPostFunctionResponse> getPostFunctionById(
            @Parameter(description = "Post-function ID") @PathVariable UUID id) {

        WorkflowPostFunctionResponse response = workflowPostFunctionService.getPostFunctionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transition/{transitionId}")
    @Operation(
            summary = "Get post-functions by transition",
            description = "Returns all post-functions for a specific transition, ordered by sequence"
    )
    public ResponseEntity<List<WorkflowPostFunctionResponse>> getPostFunctionsByTransition(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId) {

        List<WorkflowPostFunctionResponse> functions =
                workflowPostFunctionService.getPostFunctionsByTransition(transitionId);
        return ResponseEntity.ok(functions);
    }

    @PostMapping
    @Operation(
            summary = "Create post-function",
            description = "Creates a new post-function. The transitionId must be provided in the request body."
    )
    public ResponseEntity<WorkflowPostFunctionResponse> createPostFunction(
            @Valid @RequestBody WorkflowPostFunctionRequest request) {

        WorkflowPostFunctionResponse response = workflowPostFunctionService
                .createPostFunction(request.getTransitionId(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @Operation(
            summary = "Bulk create post-functions",
            description = "Creates multiple post-functions for a transition in a single request. " +
                    "Useful for importing workflow descriptors."
    )
    public ResponseEntity<List<WorkflowPostFunctionResponse>> bulkCreatePostFunctions(
            @Parameter(description = "Transition ID for all post-functions")
            @RequestParam UUID transitionId,
            @Valid @RequestBody List<WorkflowPostFunctionRequest> requests) {

        List<WorkflowPostFunctionResponse> responses =
                workflowPostFunctionService.bulkCreatePostFunctions(transitionId, requests);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update post-function",
            description = "Updates an existing post-function. Only provided fields will be updated."
    )
    public ResponseEntity<WorkflowPostFunctionResponse> updatePostFunction(
            @Parameter(description = "Post-function ID") @PathVariable UUID id,
            @Valid @RequestBody WorkflowPostFunctionRequest request) {

        WorkflowPostFunctionResponse response = workflowPostFunctionService.updatePostFunction(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle")
    @Operation(
            summary = "Enable or disable post-function",
            description = "Toggles the enabled state of a post-function without requiring full update."
    )
    public ResponseEntity<WorkflowPostFunctionResponse> togglePostFunction(
            @Parameter(description = "Post-function ID") @PathVariable UUID id,
            @Parameter(description = "Enable (true) or disable (false)") @RequestParam boolean enabled) {

        WorkflowPostFunctionResponse response = workflowPostFunctionService.togglePostFunction(id, enabled);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/transition/{transitionId}/reorder")
    @Operation(
            summary = "Reorder post-functions",
            description = "Reorders post-functions for a transition by providing the new sequence of IDs."
    )
    public ResponseEntity<List<WorkflowPostFunctionResponse>> reorderPostFunctions(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId,
            @Valid @RequestBody List<UUID> orderedIds) {

        List<WorkflowPostFunctionResponse> responses =
                workflowPostFunctionService.reorderPostFunctions(transitionId, orderedIds);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete post-function",
            description = "Deletes a post-function by ID"
    )
    public ResponseEntity<Void> deletePostFunction(
            @Parameter(description = "Post-function ID") @PathVariable UUID id) {

        workflowPostFunctionService.deletePostFunction(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/transition/{transitionId}")
    @Operation(
            summary = "Delete all post-functions for transition",
            description = "Deletes all post-functions associated with a specific transition."
    )
    public ResponseEntity<Void> deletePostFunctionsByTransition(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId) {

        workflowPostFunctionService.deletePostFunctionsByTransition(transitionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/execute/{transitionId}")
    @Operation(
            summary = "Execute post-functions",
            description = "Manually execute all post-functions for a transition. " +
                    "Used for testing or manual triggering with provided context."
    )
    public ResponseEntity<Map<String, Object>> executePostFunctions(
            @Parameter(description = "Transition ID") @PathVariable UUID transitionId,
            @RequestBody(required = false) Map<String, Object> context) {

        log.info("Manual execution of post-functions for transition: {}", transitionId);

        // Use provided context or create empty one
        Map<String, Object> executionContext = context != null ? context : new HashMap<>();

        List<PostFunctionExecutionResult> results =
                workflowPostFunctionService.executePostFunctions(transitionId, executionContext);

        long successCount = results.stream()
                .filter(PostFunctionExecutionResult::isSuccess)
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("transitionId", transitionId);
        response.put("totalCount", results.size());
        response.put("successCount", successCount);
        response.put("failureCount", results.size() - successCount);
        response.put("results", results);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/types")
    @Operation(
            summary = "List available post-function types",
            description = "Returns all available post-function types with their descriptions."
    )
    public ResponseEntity<List<Map<String, String>>> getPostFunctionTypes() {
        List<Map<String, String>> types = List.of(
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_ISSUE_ASSIGN,
                        "Assign the issue to a user or role"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_ISSUE_MOVE,
                        "Move issue to a different status"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_NOTIFY_USER,
                        "Send a notification to a user"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_UPDATE_FIELD,
                        "Update a field value on the issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_ADD_LABEL,
                        "Add a label to the issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_REMOVE_LABEL,
                        "Remove a label from the issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_CREATE_SUBTASK,
                        "Create a subtask for the issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_CLONE_ISSUE,
                        "Clone the issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_LINK_ISSUE,
                        "Link the issue to another issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_ADD_WATCHER,
                        "Add a watcher to the issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_REMOVE_WATCHER,
                        "Remove a watcher from the issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_FIRE_GLOBAL_EXTENSION,
                        "Trigger a global extension or webhook"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_SET_ISSUE_SECURITY,
                        "Set the security level on the issue"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_TRIGGER_AUTOMATION,
                        "Trigger an automation rule"),
                createTypeInfo(WorkflowPostFunctionRequest.TYPE_GENERATE_AUTOMATIC_SUMMARY,
                        "Generate an automatic description from a template")
        );
        return ResponseEntity.ok(types);
    }

    private Map<String, String> createTypeInfo(String type, String description) {
        Map<String, String> info = new HashMap<>();
        info.put("type", type);
        info.put("description", description);
        return info;
    }
}