package com.avionics_systems.plan.service;

import com.avionics_systems.plan.entity.ExclusionRule;
import com.avionics_systems.plan.entity.Plan;
import com.avionics_systems.plan.entity.PlanItem;
import com.avionics_systems.plan.exception.ResourceNotFoundException;
import com.avionics_systems.plan.repository.ExclusionRuleRepository;
import com.avionics_systems.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExclusionRuleService {

    private final ExclusionRuleRepository exclusionRuleRepository;
    private final PlanRepository planRepository;

    @Transactional
    public ExclusionRule createExclusionRule(UUID planId, String fieldName,
                                            ExclusionRule.Operator operator, String fieldValue) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));

        ExclusionRule rule = ExclusionRule.builder()
                .plan(plan)
                .fieldName(fieldName)
                .operator(operator)
                .fieldValue(fieldValue)
                .isActive(true)
                .build();

        return exclusionRuleRepository.save(rule);
    }

    @Transactional
    public ExclusionRule updateExclusionRule(UUID ruleId, String fieldName,
                                            ExclusionRule.Operator operator, String fieldValue) {
        ExclusionRule rule = exclusionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("ExclusionRule", "id", ruleId));

        rule.setFieldName(fieldName);
        rule.setOperator(operator);
        rule.setFieldValue(fieldValue);

        return exclusionRuleRepository.save(rule);
    }

    @Transactional
    public void deleteExclusionRule(UUID ruleId) {
        ExclusionRule rule = exclusionRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("ExclusionRule", "id", ruleId));
        rule.setIsActive(false);
        exclusionRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<ExclusionRule> getActiveRules(UUID planId) {
        return exclusionRuleRepository.findByPlanIdAndIsActiveTrue(planId);
    }

    @Transactional(readOnly = true)
    public String generateExclusionJql(UUID planId) {
        List<ExclusionRule> rules = exclusionRuleRepository.findByPlanIdAndIsActiveTrue(planId);

        if (rules.isEmpty()) {
            return "";
        }

        String jql = rules.stream()
                .map(ExclusionRule::toJqlFragment)
                .collect(Collectors.joining(" AND "));

        return jql;
    }

    public boolean shouldExcludeItem(PlanItem item, List<ExclusionRule> rules) {
        for (ExclusionRule rule : rules) {
            if (matchesRule(item, rule)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRule(PlanItem item, ExclusionRule rule) {
        String fieldName = rule.getFieldName().toLowerCase();
        String ruleValue = rule.getFieldValue().toLowerCase();
        String itemValue = getFieldValue(item, fieldName);

        if (itemValue == null) {
            return switch (rule.getOperator()) {
                case IS_EMPTY -> true;
                case IS_NOT_EMPTY -> false;
                default -> false;
            };
        }

        itemValue = itemValue.toLowerCase();

        return switch (rule.getOperator()) {
            case EQUALS -> itemValue.equals(ruleValue);
            case NOT_EQUALS -> !itemValue.equals(ruleValue);
            case CONTAINS -> itemValue.contains(ruleValue);
            case NOT_CONTAINS -> !itemValue.contains(ruleValue);
            case STARTS_WITH -> itemValue.startsWith(ruleValue);
            case ENDS_WITH -> itemValue.endsWith(ruleValue);
            case IN -> {
                String[] values = ruleValue.split(",");
                for (String v : values) {
                    if (itemValue.equals(v.trim())) {
                        yield true;
                    }
                }
                yield false;
            }
            case NOT_IN -> {
                String[] values = ruleValue.split(",");
                for (String v : values) {
                    if (itemValue.equals(v.trim())) {
                        yield false;
                    }
                }
                yield true;
            }
            case IS_EMPTY -> itemValue.isEmpty();
            case IS_NOT_EMPTY -> !itemValue.isEmpty();
            case GREATER_THAN -> compareNumeric(itemValue, ruleValue) > 0;
            case LESS_THAN -> compareNumeric(itemValue, ruleValue) < 0;
        };
    }

    private String getFieldValue(PlanItem item, String fieldName) {
        return switch (fieldName) {
            case "issuetype", "type" -> item.getIssueType();
            case "status" -> item.getStatus();
            case "resolution" -> "";
            case "labels" -> "";
            case "project" -> "";
            case "epic" -> item.getParentId() != null ? item.getParentId().toString() : null;
            case "archived" -> "";
            default -> null;
        };
    }

    private int compareNumeric(String a, String b) {
        try {
            double numA = Double.parseDouble(a);
            double numB = Double.parseDouble(b);
            return Double.compare(numA, numB);
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    @Transactional(readOnly = true)
    public List<ExclusionRule> getAvailableFields() {
        return List.of(
            ExclusionRule.builder().fieldName("Issue Type").operator(ExclusionRule.Operator.IN).build(),
            ExclusionRule.builder().fieldName("Status").operator(ExclusionRule.Operator.IN).build(),
            ExclusionRule.builder().fieldName("Resolution").operator(ExclusionRule.Operator.IN).build(),
            ExclusionRule.builder().fieldName("Labels").operator(ExclusionRule.Operator.IN).build(),
            ExclusionRule.builder().fieldName("Project").operator(ExclusionRule.Operator.IN).build(),
            ExclusionRule.builder().fieldName("Epic").operator(ExclusionRule.Operator.IN).build(),
            ExclusionRule.builder().fieldName("Archived").operator(ExclusionRule.Operator.EQUALS).build()
        );
    }
}