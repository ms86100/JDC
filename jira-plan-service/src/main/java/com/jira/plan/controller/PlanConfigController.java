package com.jira.plan.controller;

import com.jira.plan.entity.ExclusionRule;
import com.jira.plan.entity.PlanIssueSource;
import com.jira.plan.service.ExclusionRuleService;
import com.jira.plan.service.IssueSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans/{planId}")
@RequiredArgsConstructor
public class PlanConfigController {

    private final IssueSourceService issueSourceService;
    private final ExclusionRuleService exclusionRuleService;

    @GetMapping("/issue-sources")
    public ResponseEntity<List<PlanIssueSource>> getIssueSources(@PathVariable UUID planId) {
        return ResponseEntity.ok(issueSourceService.getActiveSources(planId));
    }

    @PostMapping("/issue-sources")
    public ResponseEntity<PlanIssueSource> addIssueSource(
            @PathVariable UUID planId,
            @RequestBody Map<String, String> body) {
        PlanIssueSource.SourceType type = PlanIssueSource.SourceType.valueOf(body.get("sourceType"));
        UUID sourceId = UUID.fromString(body.get("sourceId"));
        String name = body.getOrDefault("sourceName", "Source");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(issueSourceService.addIssueSource(planId, type, sourceId, name));
    }

    @DeleteMapping("/issue-sources/{sourceId}")
    public ResponseEntity<Void> removeIssueSource(
            @PathVariable UUID planId,
            @PathVariable UUID sourceId,
            @RequestParam String sourceType) {
        issueSourceService.removeIssueSource(planId, sourceId, PlanIssueSource.SourceType.valueOf(sourceType));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exclusion-rules")
    public ResponseEntity<List<ExclusionRule>> getExclusionRules(@PathVariable UUID planId) {
        return ResponseEntity.ok(exclusionRuleService.getActiveRules(planId));
    }

    @PostMapping("/exclusion-rules")
    public ResponseEntity<ExclusionRule> createExclusionRule(
            @PathVariable UUID planId,
            @RequestBody Map<String, String> body) {
        ExclusionRule.Operator op = ExclusionRule.Operator.valueOf(body.get("operator"));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                exclusionRuleService.createExclusionRule(
                        planId,
                        body.get("fieldName"),
                        op,
                        body.get("fieldValue")));
    }

    @DeleteMapping("/exclusion-rules/{ruleId}")
    public ResponseEntity<Void> deleteExclusionRule(@PathVariable UUID ruleId) {
        exclusionRuleService.deleteExclusionRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}
