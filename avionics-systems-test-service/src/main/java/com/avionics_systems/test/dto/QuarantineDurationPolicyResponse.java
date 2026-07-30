package com.avionics_systems.test.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantineDurationPolicyResponse {

    private UUID id;
    private UUID projectId;

    private String policyName;
    private String description;

    private PolicyType policyType;

    private DurationRule durationRule;
    private AutoRestoreConfig autoRestoreConfig;
    private ReviewConfig reviewConfig;
    private EscalationConfig escalationConfig;

    private Map<String, Object> conditions;

    private Boolean isDefault;
    private Boolean isActive;

    private Integer currentUsageCount;
    private Integer historicalUsageCount;

    private LocalDateTime createdAt;
    private UUID createdBy;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DurationRule {
        private DurationType type;
        private Integer durationDays;
        private Integer maxDurationDays;
        private Boolean allowPermanent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AutoRestoreConfig {
        private Boolean enabled;
        private Integer minPassCount;
        private Integer minDaysElapsed;
        private String restoreCondition;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewConfig {
        private Boolean required;
        private Integer reviewAfterDays;
        private String[] approvers;
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

    public enum DurationType {
        TEMPORARY,
        PERMANENT,
        CONDITIONAL,
        EXTENDABLE
    }
}