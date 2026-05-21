package com.jira.test.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineDurationPolicyRequest {

    @NotNull(message = "Policy name is required")
    private String policyName;

    private String description;

    @NotNull(message = "Policy type is required")
    private PolicyType policyType;

    private DurationRule durationRule;

    private AutoRestoreConfig autoRestoreConfig;

    private ReviewConfig reviewConfig;

    private EscalationConfig escalationConfig;

    private Map<String, Object> conditions; // When to apply this policy

    private Boolean isDefault;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DurationRule {
        private DurationType type;
        private Integer durationDays;
        private Integer maxDurationDays;
        private Boolean allowPermanent;

        public enum DurationType {
            TEMPORARY,
            PERMANENT,
            CONDITIONAL,
            EXTENDABLE
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AutoRestoreConfig {
        private Boolean enabled;
        private Integer minPassCount;
        private Integer minDaysElapsed;
        private String restoreCondition; // pass_percentage, consecutive_passes, etc.
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewConfig {
        private Boolean required;
        private Integer reviewAfterDays;
        private String[] approvers; // Role or user IDs
        private Boolean autoEscalateAfterDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EscalationConfig {
        private Boolean enabled;
        private Integer escalateAfterDays;
        private String escalateToRole;
        private String[] notifyEmails;
    }

    public enum PolicyType {
        FLAKY_TEST,
        ENVIRONMENTAL,
        DATA_DEPENDENCY,
        INFRASTRUCTURE,
        THIRD_PARTY,
        MANUAL_OVERRIDE,
        CUSTOM
    }
}