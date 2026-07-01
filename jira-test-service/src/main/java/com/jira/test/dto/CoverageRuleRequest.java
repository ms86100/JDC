package com.jira.test.dto;

import com.jira.test.entity.CoverageRule;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoverageRuleRequest {
    private UUID projectId;
    private String name;
    private String description;
    private CoverageRule.RuleType ruleType;
    private BigDecimal threshold;
    private CoverageRule.Scope scope;
    private UUID scopeId;
    private Boolean enabled;
}