package com.avionics_systems.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request DTO for creating or updating a workflow post-function.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPostFunctionRequest {

    @NotNull(message = "{validation.postfunction.transition.required}")
    private UUID transitionId;

    @NotBlank(message = "{validation.postfunction.type.required}")
    private String postFunctionType;

    private String functionData;

    private Integer sequence;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean continueOnError = false;

    @Builder.Default
    private Boolean async = false;

    // Post-function type constants
    public static final String TYPE_ISSUE_ASSIGN = "ISSUE_ASSIGN";
    public static final String TYPE_ISSUE_MOVE = "ISSUE_MOVE";
    public static final String TYPE_NOTIFY_USER = "NOTIFY_USER";
    public static final String TYPE_UPDATE_FIELD = "UPDATE_FIELD";
    public static final String TYPE_ADD_LABEL = "ADD_LABEL";
    public static final String TYPE_REMOVE_LABEL = "REMOVE_LABEL";
    public static final String TYPE_CREATE_SUBTASK = "CREATE_SUBTASK";
    public static final String TYPE_CLONE_ISSUE = "CLONE_ISSUE";
    public static final String TYPE_LINK_ISSUE = "LINK_ISSUE";
    public static final String TYPE_ADD_WATCHER = "ADD_WATCHER";
    public static final String TYPE_REMOVE_WATCHER = "REMOVE_WATCHER";
    public static final String TYPE_FIRE_GLOBAL_EXTENSION = "FIRE_GLOBAL_EXTENSION";
    public static final String TYPE_SET_ISSUE_SECURITY = "SET_ISSUE_SECURITY";
    public static final String TYPE_TRIGGER_AUTOMATION = "TRIGGER_AUTOMATION";
    public static final String TYPE_GENERATE_AUTOMATIC_SUMMARY = "GENERATE_AUTOMATIC_SUMMARY";
}