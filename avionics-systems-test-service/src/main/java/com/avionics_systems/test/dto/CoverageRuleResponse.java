package com.avionics_systems.test.dto;

import com.avionics_systems.test.entity.CoverageRule;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageRuleResponse {
    private UUID id;
    private UUID projectId;
    private String name;
    private String description;
    private CoverageRule.RuleType ruleType;
    private BigDecimal threshold;
    private CoverageRule.Scope scope;
    private UUID scopeId;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CoverageRuleResponse from(CoverageRule rule) {
        return CoverageRuleResponse.builder()
                .id(rule.getId())
                .projectId(rule.getProjectId())
                .name(rule.getName())
                .description(rule.getDescription())
                .ruleType(rule.getRuleType())
                .threshold(rule.getThreshold())
                .scope(rule.getScope())
                .scopeId(rule.getScopeId())
                .enabled(rule.getEnabled())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
