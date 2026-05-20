package com.jira.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuarantineService {

    private final TestQuarantineRepository quarantineRepository;
    private final QuarantineTransitionRepository transitionRepository;
    private final QuarantineMetricsRepository metricsRepository;
    private final QuarantineRuleRepository ruleRepository;
    private final TestIssueRepository testIssueRepository;
    private final ObjectMapper objectMapper;

    // ==================== Core Operations ====================

    @Transactional
    public QuarantineResponse quarantineTest(QuarantineRequest request) {
        log.info("Quarantining test: {}", request.getTestId());

        TestIssue test = testIssueRepository.findById(request.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", request.getTestId()));

        // Check if already quarantined
        Optional<TestQuarantine> existing = quarantineRepository.findByTestId(request.getTestId());
        if (existing.isPresent()) {
            // Update status if needed
            TestQuarantine quarantine = existing.get();
            String oldStatus = quarantine.getStatus();
            if (!oldStatus.equals(request.getStatus())) {
                quarantine.setStatus(request.getStatus());
                recordTransition(quarantine.getId(), oldStatus, request.getStatus(), request.getQuarantineReason(), request.getTriggerType());
            }
            quarantineRepository.save(quarantine);
            return mapToQuarantineResponse(quarantine, test);
        }

        // Create new quarantine entry
        TestQuarantine quarantine = TestQuarantine.builder()
                .testId(request.getTestId())
                .status(request.getStatus() != null ? request.getStatus() : "quarantined")
                .quarantineReason(request.getQuarantineReason())
                .triggerType(request.getTriggerType())
                .triggeredBy(null)
                .autoRestoreEnabled(request.getAutoRestoreEnabled() != null ? request.getAutoRestoreEnabled() : true)
                .autoRestoreConditions(request.getAutoRestoreConditions() != null ?
                        serializeMap(request.getAutoRestoreConditions()) : null)
                .build();

        quarantine = quarantineRepository.save(quarantine);

        // Record transition
        recordTransition(quarantine.getId(), null, quarantine.getStatus(), request.getQuarantineReason(), request.getTriggerType());

        log.info("Test {} quarantined with status: {}", request.getTestId(), quarantine.getStatus());
        return mapToQuarantineResponse(quarantine, test);
    }

    @Transactional
    public QuarantineResponse updateStatus(UUID quarantineId, String newStatus, String reason) {
        TestQuarantine quarantine = quarantineRepository.findById(quarantineId)
                .orElseThrow(() -> new ResourceNotFoundException("TestQuarantine", "id", quarantineId));

        String oldStatus = quarantine.getStatus();
        quarantine.setStatus(newStatus);

        if ("restored".equals(newStatus)) {
            quarantine.setRestoredAt(LocalDateTime.now());
            quarantine.setRestoreReason(reason);
        }

        quarantine = quarantineRepository.save(quarantine);

        // Record transition
        recordTransition(quarantineId, oldStatus, newStatus, reason, null);

        TestIssue test = testIssueRepository.findById(quarantine.getTestId()).orElse(null);
        return mapToQuarantineResponse(quarantine, test);
    }

    @Transactional
    public QuarantineResponse restoreTest(UUID quarantineId, String reason, UUID restoredBy) {
        TestQuarantine quarantine = quarantineRepository.findById(quarantineId)
                .orElseThrow(() -> new ResourceNotFoundException("TestQuarantine", "id", quarantineId));

        String oldStatus = quarantine.getStatus();
        quarantine.setStatus("restored");
        quarantine.setRestoredAt(LocalDateTime.now());
        quarantine.setRestoredBy(restoredBy);
        quarantine.setRestoreReason(reason);

        quarantine = quarantineRepository.save(quarantine);

        // Record transition
        recordTransition(quarantineId, oldStatus, "restored", reason, null);

        TestIssue test = testIssueRepository.findById(quarantine.getTestId()).orElse(null);
        log.info("Test {} restored from quarantine", quarantine.getTestId());
        return mapToQuarantineResponse(quarantine, test);
    }

    // ==================== Query ====================

    @Transactional(readOnly = true)
    public List<QuarantineResponse> getQuarantinedTests(UUID projectId) {
        List<TestQuarantine> quarantines = quarantineRepository.findAllByOrderByTriggeredAtDesc();
        Set<UUID> projectTestIds = getProjectTestIds(projectId);

        return quarantines.stream()
                .filter(q -> projectTestIds.contains(q.getTestId()))
                .map(q -> {
                    TestIssue test = testIssueRepository.findById(q.getTestId()).orElse(null);
                    return mapToQuarantineResponse(q, test);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuarantineResponse> getQuarantinedTestsByStatus(String status) {
        List<TestQuarantine> quarantines = quarantineRepository.findByStatus(status);
        return quarantines.stream().map(q -> {
            TestIssue test = testIssueRepository.findById(q.getTestId()).orElse(null);
            return mapToQuarantineResponse(q, test);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuarantineResponse getQuarantine(UUID testId) {
        TestQuarantine quarantine = quarantineRepository.findByTestId(testId)
                .orElseThrow(() -> new ResourceNotFoundException("TestQuarantine", "testId", testId));
        TestIssue test = testIssueRepository.findById(testId).orElse(null);
        return mapToQuarantineResponse(quarantine, test);
    }

    @Transactional(readOnly = true)
    public QuarantineDashboardResponse getDashboard(UUID projectId) {
        List<TestQuarantine> allQuarantines = quarantineRepository.findAllByOrderByTriggeredAtDesc();
        Set<UUID> projectTestIds = getProjectTestIds(projectId);

        List<TestQuarantine> projectQuarantines = allQuarantines.stream()
                .filter(q -> projectTestIds.contains(q.getTestId()))
                .collect(Collectors.toList());

        int quarantinedCount = (int) projectQuarantines.stream().filter(q -> "quarantined".equals(q.getStatus())).count();
        int investigationCount = (int) projectQuarantines.stream().filter(q -> "investigation".equals(q.getStatus())).count();
        int candidateCount = (int) projectQuarantines.stream().filter(q -> "candidate".equals(q.getStatus())).count();

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        int restoredThisWeek = (int) projectQuarantines.stream()
                .filter(q -> q.getRestoredAt() != null && q.getRestoredAt().isAfter(weekAgo))
                .count();

        // Calculate average duration
        List<Long> durations = projectQuarantines.stream()
                .filter(q -> q.getRestoredAt() != null)
                .map(q -> ChronoUnit.DAYS.between(q.getTriggeredAt(), q.getRestoredAt()))
                .collect(Collectors.toList());

        BigDecimal avgDuration = durations.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf(durations.stream().mapToLong(Long::longValue).average().orElse(0))
                        .setScale(1, RoundingMode.HALF_UP);

        // Count by trigger type
        Map<String, Integer> byTriggerType = new HashMap<>();
        projectQuarantines.forEach(q -> {
            String type = q.getTriggerType() != null ? q.getTriggerType() : "unknown";
            byTriggerType.put(type, byTriggerType.getOrDefault(type, 0) + 1);
        });

        List<QuarantineResponse> recentQuarantined = projectQuarantines.stream()
                .sorted(Comparator.comparing(TestQuarantine::getTriggeredAt).reversed())
                .limit(5)
                .map(q -> {
                    TestIssue test = testIssueRepository.findById(q.getTestId()).orElse(null);
                    return mapToQuarantineResponse(q, test);
                })
                .collect(Collectors.toList());

        // Find tests ready for restore (passing consistently)
        List<QuarantineResponse> readyForRestore = projectQuarantines.stream()
                .filter(q -> "quarantined".equals(q.getStatus()) && q.getCurrentPassCount() >= 5)
                .limit(10)
                .map(q -> {
                    TestIssue test = testIssueRepository.findById(q.getTestId()).orElse(null);
                    return mapToQuarantineResponse(q, test);
                })
                .collect(Collectors.toList());

        return QuarantineDashboardResponse.builder()
                .totalQuarantined(projectQuarantines.size())
                .quarantinedCount(quarantinedCount)
                .investigationCount(investigationCount)
                .candidateCount(candidateCount)
                .restoredThisWeek(restoredThisWeek)
                .averageQuarantineDurationDays(avgDuration)
                .byTriggerType(byTriggerType)
                .recentQuarantined(recentQuarantined)
                .readyForRestore(readyForRestore)
                .build();
    }

    // ==================== Auto-Management ====================

    @Transactional
    public void evaluateAutoRestore(UUID quarantineId) {
        TestQuarantine quarantine = quarantineRepository.findById(quarantineId).orElse(null);
        if (quarantine == null || !quarantine.getAutoRestoreEnabled()) return;

        Map<String, Object> conditions = parseMap(quarantine.getAutoRestoreConditions());
        int requiredPasses = (int) conditions.getOrDefault("passCount", 3);
        int requiredDays = (int) conditions.getOrDefault("daysElapsed", 7);

        boolean passCondition = quarantine.getCurrentPassCount() >= requiredPasses;
        boolean timeCondition = quarantine.getTriggeredAt() != null &&
                ChronoUnit.DAYS.between(quarantine.getTriggeredAt(), LocalDateTime.now()) >= requiredDays;

        if (passCondition && timeCondition) {
            restoreTest(quarantineId, "Auto-restore: passed " + requiredPasses + " times after " + requiredDays + " days", null);
            log.info("Auto-restored test {} from quarantine", quarantine.getTestId());
        }
    }

    @Transactional
    public void recordExecution(UUID quarantineId, boolean passed) {
        TestQuarantine quarantine = quarantineRepository.findById(quarantineId).orElse(null);
        if (quarantine == null) return;

        quarantine.setCurrentExecutionCount(quarantine.getCurrentExecutionCount() + 1);
        if (passed) {
            quarantine.setCurrentPassCount(quarantine.getCurrentPassCount() + 1);
        }
        quarantine.setLastExecutionAt(LocalDateTime.now());
        quarantine.setLastStatus(passed ? "PASSED" : "FAILED");

        quarantineRepository.save(quarantine);

        // Check auto-restore
        if (passed && quarantine.getAutoRestoreEnabled()) {
            evaluateAutoRestore(quarantineId);
        }
    }

    // ==================== Rules Management ====================

    @Transactional
    public QuarantineRuleResponse createRule(QuarantineRuleRequest request) {
        QuarantineRule rule = QuarantineRule.builder()
                .projectId(request.getProjectId())
                .ruleName(request.getRuleName())
                .ruleType(request.getRuleType())
                .conditions(serializeMap(request.getConditions()))
                .autoQuarantine(request.getAutoQuarantine() != null ? request.getAutoQuarantine() : true)
                .notifyOnTrigger(request.getNotifyOnTrigger() != null ? request.getNotifyOnTrigger() : true)
                .build();

        rule = ruleRepository.save(rule);
        log.info("Created quarantine rule: {}", rule.getId());
        return mapToRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<QuarantineRuleResponse> getRules(UUID projectId) {
        return ruleRepository.findByProjectId(projectId).stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        ruleRepository.deleteById(ruleId);
        log.info("Deleted quarantine rule: {}", ruleId);
    }

    // ==================== Transitions & Metrics ====================

    private void recordTransition(UUID quarantineId, String fromStatus, String toStatus, String reason, String triggerType) {
        QuarantineTransition transition = QuarantineTransition.builder()
                .quarantineId(quarantineId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .transitionReason(reason)
                .triggeredBy(null)
                .build();
        transitionRepository.save(transition);
    }

    @Transactional(readOnly = true)
    public List<QuarantineTransition> getTransitions(UUID quarantineId) {
        return transitionRepository.findByQuarantineIdOrderByTransitionedAtDesc(quarantineId);
    }

    // ==================== Helper Methods ====================

    private Set<UUID> getProjectTestIds(UUID projectId) {
        return testIssueRepository.findByProjectIdAndArchivedFalse(projectId).stream()
                .map(TestIssue::getId)
                .collect(Collectors.toSet());
    }

    private String serializeMap(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private QuarantineResponse mapToQuarantineResponse(TestQuarantine quarantine, TestIssue test) {
        return QuarantineResponse.builder()
                .id(quarantine.getId())
                .testId(quarantine.getTestId())
                .testIssueKey(test != null ? test.getName() : null)
                .testName(test != null ? test.getName() : null)
                .status(quarantine.getStatus())
                .quarantineReason(quarantine.getQuarantineReason())
                .triggerType(quarantine.getTriggerType())
                .triggeredBy(quarantine.getTriggeredBy())
                .triggeredAt(quarantine.getTriggeredAt())
                .autoRestoreEnabled(quarantine.getAutoRestoreEnabled())
                .autoRestoreConditions(parseMap(quarantine.getAutoRestoreConditions()))
                .currentExecutionCount(quarantine.getCurrentExecutionCount())
                .currentPassCount(quarantine.getCurrentPassCount())
                .lastExecutionAt(quarantine.getLastExecutionAt())
                .lastStatus(quarantine.getLastStatus())
                .restoredAt(quarantine.getRestoredAt())
                .restoredBy(quarantine.getRestoredBy())
                .restoreReason(quarantine.getRestoreReason())
                .createdAt(quarantine.getUpdatedAt())
                .build();
    }

    private QuarantineRuleResponse mapToRuleResponse(QuarantineRule rule) {
        return QuarantineRuleResponse.builder()
                .id(rule.getId())
                .projectId(rule.getProjectId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .conditions(parseMap(rule.getConditions()))
                .autoQuarantine(rule.getAutoQuarantine())
                .notifyOnTrigger(rule.getNotifyOnTrigger())
                .isActive(rule.getIsActive())
                .createdBy(rule.getCreatedBy())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}