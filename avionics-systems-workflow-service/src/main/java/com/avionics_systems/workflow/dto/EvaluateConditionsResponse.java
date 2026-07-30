package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for workflow condition evaluation.
 * Used by external services to get condition evaluation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateConditionsResponse {

    /**
     * Whether all conditions passed
     */
    private boolean conditionsMet;

    /**
     * Transition ID that was evaluated
     */
    private UUID transitionId;

    /**
     * List of condition errors if conditions were not met
     */
    private List<String> errors;

    /**
     * Optional list of failed condition details
     */
    private List<FailedConditionDetail> failedConditions;

    /**
     * Evaluation time in milliseconds
     */
    private long evaluationTimeMs;

    /**
     * Detailed information about a failed condition
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedConditionDetail {
        private String conditionType;
        private String fieldName;
        private String operator;
        private String expectedValue;
        private String actualValue;
        private String description;
    }

    /**
     * Create a success response
     */
    public static EvaluateConditionsResponse success(UUID transitionId, long evaluationTimeMs) {
        return EvaluateConditionsResponse.builder()
                .conditionsMet(true)
                .transitionId(transitionId)
                .errors(List.of())
                .failedConditions(List.of())
                .evaluationTimeMs(evaluationTimeMs)
                .build();
    }

    /**
     * Create a failure response
     */
    public static EvaluateConditionsResponse failure(UUID transitionId, List<String> errors, List<FailedConditionDetail> failedConditions, long evaluationTimeMs) {
        return EvaluateConditionsResponse.builder()
                .conditionsMet(false)
                .transitionId(transitionId)
                .errors(errors)
                .failedConditions(failedConditions)
                .evaluationTimeMs(evaluationTimeMs)
                .build();
    }
}