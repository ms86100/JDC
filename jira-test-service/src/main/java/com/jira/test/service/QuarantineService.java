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
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private final QuarantineDurationPolicyRepository policyRepository;
    private final QuarantineReviewRepository reviewRepository;
    private final QuarantineReviewHistoryRepository reviewHistoryRepository;
    private final FlakyTestAnalysisRepository flakyAnalysisRepository;
    private final ObjectMapper objectMapper;

    // ==================== Core Quarantine Operations ====================

    @Transactional
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    public QuarantineResponse quarantineTest(QuarantineRequest request) {
        return quarantineTestInternal(request, null);
    }

    @Transactional
    public QuarantineResponse quarantineTestInternal(QuarantineRequest request, UUID triggeredBy) {
        log.info("Quarantining test: {} with reason: {}", request.getTestId(), request.getQuarantineReason());

        TestIssue test = testIssueRepository.findById(request.getTestId())
                .orElseThrow(() -> new ResourceNotFoundException("Test", "id", request.getTestId()));

        // Check if already quarantined
        Optional<TestQuarantine> existing = quarantineRepository.findByTestId(request.getTestId());
        if (existing.isPresent()) {
            TestQuarantine quarantine = existing.get();
            String oldStatus = quarantine.getStatus();
            if (!oldStatus.equals(request.getStatus())) {
                quarantine.setStatus(request.getStatus());
                recordTransition(quarantine.getId(), oldStatus, request.getStatus(),
                        request.getQuarantineReason(), request.getTriggerType(), triggeredBy);
            }
            quarantineRepository.save(quarantine);
            return mapToQuarantineResponse(quarantine, test);
        }

        // Get duration policy
        Map<String, Object> autoRestoreConditions = request.getAutoRestoreConditions();
        if (autoRestoreConditions == null) {
            autoRestoreConditions = getDefaultAutoRestoreConditions(request.getProjectId());
        }

        // Create new quarantine entry
        TestQuarantine quarantine = TestQuarantine.builder()
                .testId(request.getTestId())
                .status(request.getStatus() != null ? request.getStatus() : "quarantined")
                .quarantineReason(request.getQuarantineReason())
                .triggerType(request.getTriggerType())
                .triggeredBy(triggeredBy)
                .autoRestoreEnabled(request.getAutoRestoreEnabled() != null ? request.getAutoRestoreEnabled() : true)
                .autoRestoreConditions(serializeMap(autoRestoreConditions))
                .build();

        quarantine = quarantineRepository.save(quarantine);

        // Record transition
        recordTransition(quarantine.getId(), null, quarantine.getStatus(),
                request.getQuarantineReason(), request.getTriggerType(), triggeredBy);

        boolean reviewRequired = shouldRequireReview(request);
        if (reviewRequired) {
            createReviewFromQuarantine(quarantine);
        }

        log.info("Test {} quarantined with status: {}, review required: {}",
                request.getTestId(), quarantine.getStatus(), reviewRequired);
        return mapToQuarantineResponse(quarantine, test);
    }

    @Transactional
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    public QuarantineResponse updateStatus(UUID quarantineId, String newStatus, String reason, UUID projectId) {
        TestQuarantine quarantine = quarantineRepository.findById(quarantineId)
                .orElseThrow(() -> new ResourceNotFoundException("TestQuarantine", "id", quarantineId));

        String oldStatus = quarantine.getStatus();
        quarantine.setStatus(newStatus);

        if ("restored".equals(newStatus)) {
            quarantine.setRestoredAt(LocalDateTime.now());
            quarantine.setRestoreReason(reason);
        }

        UUID currentUserId = getCurrentUserId();
        quarantine = quarantineRepository.save(quarantine);

        // Record transition
        recordTransition(quarantineId, oldStatus, newStatus, reason, null, currentUserId);

        TestIssue test = testIssueRepository.findById(quarantine.getTestId()).orElse(null);
        return mapToQuarantineResponse(quarantine, test);
    }

    @Transactional
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #projectId)")
    public QuarantineResponse restoreTest(UUID quarantineId, String reason, UUID projectId) {
        TestQuarantine quarantine = quarantineRepository.findById(quarantineId)
                .orElseThrow(() -> new ResourceNotFoundException("TestQuarantine", "id", quarantineId));

        // Check if review is required and completed
        if (hasPendingReview(quarantineId)) {
            Optional<QuarantineReview> review = reviewRepository.findByQuarantineId(quarantineId);
            if (review.isPresent() && review.get().getStatus() != QuarantineReview.ReviewStatus.APPROVED_FOR_RESTORE
                    && review.get().getStatus() != QuarantineReview.ReviewStatus.COMPLETED) {
                throw new InvalidOperationException("Review must be approved before restoring");
            }
        }

        String oldStatus = quarantine.getStatus();
        UUID currentUserId = getCurrentUserId();
        quarantine.setStatus("restored");
        quarantine.setRestoredAt(LocalDateTime.now());
        quarantine.setRestoredBy(currentUserId);
        quarantine.setRestoreReason(reason);

        quarantine = quarantineRepository.save(quarantine);

        // Record transition
        recordTransition(quarantineId, oldStatus, "restored", reason, null, currentUserId);

        // Update review if exists
        reviewRepository.findByQuarantineId(quarantineId).ifPresent(r -> {
            r.setStatus(QuarantineReview.ReviewStatus.COMPLETED);
            r.setReviewCompletedAt(LocalDateTime.now());
            r.setDecisionReason(reason);
            reviewRepository.save(r);
        });

        TestIssue test = testIssueRepository.findById(quarantine.getTestId()).orElse(null);
        log.info("Test {} restored from quarantine by user {}", quarantine.getTestId(), currentUserId);
        return mapToQuarantineResponse(quarantine, test);
    }

    // ==================== Batch Operations ====================

    @Transactional
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #request.projectId)")
    public QuarantineBatchResponse batchOperation(QuarantineBatchRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Starting batch {} operation for {} tests", request.getAction(), request.getTestIds().size());

        List<UUID> successfulIds = new ArrayList<>();
        List<QuarantineBatchResponse.BatchFailure> failures = new ArrayList<>();

        for (UUID testId : request.getTestIds()) {
            try {
                switch (request.getAction()) {
                    case quarantine -> {
                        QuarantineRequest qReq = QuarantineRequest.builder()
                                .testId(testId)
                                .projectId(request.getProjectId())
                                .status(request.getTargetStatus() != null ? request.getTargetStatus() : "quarantined")
                                .quarantineReason(request.getReason())
                                .triggerType("batch_operation")
                                .autoRestoreEnabled(request.getOptions() != null ?
                                        request.getOptions().getAutoRestoreEnabled() : true)
                                .autoRestoreConditions(request.getOptions() != null ?
                                        request.getOptions().getAutoRestoreConditions() : null)
                                .build();
                        quarantineTestInternal(qReq, null);
                        successfulIds.add(testId);
                    }
                    case restore -> {
                        Optional<TestQuarantine> quarantine = quarantineRepository.findByTestId(testId);
                        if (quarantine.isPresent()) {
                            restoreTest(quarantine.get().getId(), request.getReason(), request.getProjectId());
                            successfulIds.add(testId);
                        } else {
                            failures.add(QuarantineBatchResponse.BatchFailure.builder()
                                    .testId(testId)
                                    .error("Test not in quarantine")
                                    .errorCode("NOT_QUARANTINED")
                                    .build());
                        }
                    }
                    case update_status -> {
                        Optional<TestQuarantine> quarantine = quarantineRepository.findByTestId(testId);
                        if (quarantine.isPresent()) {
                            updateStatus(quarantine.get().getId(), request.getTargetStatus(), request.getReason(), request.getProjectId());
                            successfulIds.add(testId);
                        } else {
                            failures.add(QuarantineBatchResponse.BatchFailure.builder()
                                    .testId(testId)
                                    .error("Test not in quarantine")
                                    .errorCode("NOT_QUARANTINED")
                                    .build());
                        }
                    }
                    case submit_for_review -> {
                        Optional<TestQuarantine> quarantine = quarantineRepository.findByTestId(testId);
                        if (quarantine.isPresent()) {
                            submitForReview(quarantine.get().getId(), request.getOptions() != null ?
                                    request.getOptions().getReviewNotes() : null);
                            successfulIds.add(testId);
                        }
                    }
                    case extend_duration -> {
                        if (request.getOptions() != null && request.getOptions().getExtendDays() != null) {
                            Optional<TestQuarantine> quarantine = quarantineRepository.findByTestId(testId);
                            if (quarantine.isPresent()) {
                                extendQuarantineDuration(quarantine.get().getId(), request.getOptions().getExtendDays());
                                successfulIds.add(testId);
                            }
                        }
                    }
                    case delete -> {
                        quarantineRepository.findByTestId(testId).ifPresent(q -> {
                            quarantineRepository.delete(q);
                            successfulIds.add(testId);
                        });
                    }
                }
            } catch (Exception e) {
                log.error("Batch operation failed for test {}", testId, e);
                failures.add(QuarantineBatchResponse.BatchFailure.builder()
                        .testId(testId)
                        .error(e.getMessage())
                        .errorCode("OPERATION_FAILED")
                        .build());
            }
        }

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Batch operation completed: {} successful, {} failed in {}ms",
                successfulIds.size(), failures.size(), processingTime);

        return QuarantineBatchResponse.builder()
                .totalRequested(request.getTestIds().size())
                .successCount(successfulIds.size())
                .failureCount(failures.size())
                .successfulIds(successfulIds)
                .failures(failures)
                .processingTimeMs(processingTime)
                .build();
    }

    // ==================== Query Operations ====================

    @Transactional(readOnly = true)
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
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
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    public List<QuarantineResponse> getQuarantinedTestsByStatus(String status, UUID projectId) {
        List<TestQuarantine> quarantines = quarantineRepository.findByStatus(status);
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
    public QuarantineResponse getQuarantine(UUID testId) {
        TestQuarantine quarantine = quarantineRepository.findByTestId(testId)
                .orElseThrow(() -> new ResourceNotFoundException("TestQuarantine", "testId", testId));
        TestIssue test = testIssueRepository.findById(testId).orElse(null);
        return mapToQuarantineResponse(quarantine, test);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    public QuarantineDashboardResponse getDashboard(UUID projectId) {
        List<TestQuarantine> allQuarantines = quarantineRepository.findAllByOrderByTriggeredAtDesc();
        Set<UUID> projectTestIds = getProjectTestIds(projectId);

        List<TestQuarantine> projectQuarantines = allQuarantines.stream()
                .filter(q -> projectTestIds.contains(q.getTestId()))
                .collect(Collectors.toList());

        int quarantinedCount = (int) projectQuarantines.stream()
                .filter(q -> "quarantined".equals(q.getStatus())).count();
        int investigationCount = (int) projectQuarantines.stream()
                .filter(q -> "investigation".equals(q.getStatus())).count();
        int candidateCount = (int) projectQuarantines.stream()
                .filter(q -> "candidate".equals(q.getStatus())).count();

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

    // ==================== Auto-Quarantine Rules ====================

    @Transactional
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    public boolean evaluateAndAutoQuarantine(UUID testId, UUID projectId, String triggerType,
            Map<String, Object> failureContext) {
        List<QuarantineRule> activeRules = ruleRepository.findByProjectIdAndIsActiveTrue(projectId);

        for (QuarantineRule rule : activeRules) {
            if (shouldApplyRule(rule, testId, triggerType, failureContext)) {
                log.info("Auto-quarantining test {} due to rule: {}", testId, rule.getRuleName());

                QuarantineRequest request = QuarantineRequest.builder()
                        .testId(testId)
                        .projectId(projectId)
                        .triggerType(triggerType)
                        .quarantineReason("Auto-quarantine: " + rule.getRuleName())
                        .autoRestoreEnabled(rule.getAutoQuarantine())
                        .autoRestoreConditions(parseMap(rule.getConditions()))
                        .build();

                quarantineTestInternal(request, null);

                if (rule.getNotifyOnTrigger()) {
                    // Could integrate with notification service
                    log.info("Rule {} triggered notification for test {}", rule.getRuleName(), testId);
                }
                return true;
            }
        }
        return false;
    }

    private boolean shouldApplyRule(QuarantineRule rule, UUID testId, String triggerType,
            Map<String, Object> failureContext) {
        if (!rule.getRuleType().equals(triggerType) && !"*".equals(rule.getRuleType())) {
            return false;
        }

        Map<String, Object> conditions = parseMap(rule.getConditions());

        // Check flakiness threshold
        if (conditions.containsKey("flakyScoreThreshold")) {
            double threshold = ((Number) conditions.get("flakyScoreThreshold")).doubleValue();
            Optional<FlakyTestAnalysis> analysis = flakyAnalysisRepository.findByTestId(testId);
            if (analysis.isPresent() && analysis.get().getFlakyScore() != null) {
                if (analysis.get().getFlakyScore().doubleValue() < threshold) {
                    return false;
                }
            }
        }

        // Check consecutive failures
        if (conditions.containsKey("consecutiveFailures")) {
            int requiredFails = ((Number) conditions.get("consecutiveFailures")).intValue();
            Optional<FlakyTestAnalysis> analysis = flakyAnalysisRepository.findByTestId(testId);
            if (analysis.isEmpty() || analysis.get().getTotalFailures() == null ||
                    analysis.get().getTotalFailures() < requiredFails) {
                return false;
            }
        }

        // Check environment match
        if (conditions.containsKey("environment")) {
            String requiredEnv = (String) conditions.get("environment");
            String currentEnv = (String) failureContext.get("environment");
            if (!requiredEnv.equals(currentEnv)) {
                return false;
            }
        }

        return true;
    }

    // ==================== Auto-Management & Restore ====================

    @Transactional
    public void evaluateAutoRestore(UUID quarantineId) {
        TestQuarantine quarantine = quarantineRepository.findById(quarantineId).orElse(null);
        if (quarantine == null || !quarantine.getAutoRestoreEnabled()) return;

        Map<String, Object> conditions = parseMap(quarantine.getAutoRestoreConditions());
        int requiredPasses = (int) conditions.getOrDefault("passCount", 3);
        int requiredDays = (int) conditions.getOrDefault("daysElapsed", 7);
        double requiredPassRate = conditions.containsKey("passRateThreshold") ?
                ((Number) conditions.get("passRateThreshold")).doubleValue() : 1.0;

        boolean passCondition = quarantine.getCurrentPassCount() >= requiredPasses;
        boolean rateCondition = quarantine.getCurrentExecutionCount() > 0 &&
                (double) quarantine.getCurrentPassCount() / quarantine.getCurrentExecutionCount() >= requiredPassRate;
        boolean timeCondition = quarantine.getTriggeredAt() != null &&
                ChronoUnit.DAYS.between(quarantine.getTriggeredAt(), LocalDateTime.now()) >= requiredDays;

        if (passCondition && rateCondition && timeCondition) {
            // Check if review is required
            if (hasPendingReview(quarantineId)) {
                Optional<QuarantineReview> review = reviewRepository.findByQuarantineId(quarantineId);
                if (review.isEmpty() || (review.get().getStatus() != QuarantineReview.ReviewStatus.APPROVED_FOR_RESTORE
                        && review.get().getStatus() != QuarantineReview.ReviewStatus.COMPLETED)) {
                    log.info("Test {} eligible for auto-restore but requires review approval", quarantine.getTestId());
                    return;
                }
            }

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

        // Record metrics
        recordMetrics(quarantine);

        // Check auto-restore
        if (passed && quarantine.getAutoRestoreEnabled()) {
            evaluateAutoRestore(quarantineId);
        }

        // Check if test was updated (potential auto-unquarantine)
        if (passed && hasTestBeenUpdated(quarantine.getTestId())) {
            log.info("Test {} passed after being updated, may be eligible for auto-unquarantine", quarantine.getTestId());
        }
    }

    // ==================== Duration Policies ====================

    @Transactional
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    public QuarantineDurationPolicyResponse createDurationPolicy(QuarantineDurationPolicyRequest request) {
        QuarantineDurationPolicy policy = QuarantineDurationPolicy.builder()
                .projectId(request.getProjectId())
                .policyName(request.getPolicyName())
                .description(request.getDescription())
                .policyType(mapRequestPolicyType(request.getPolicyType()))
                .durationRule(serializeJson(request.getDurationRule()))
                .autoRestoreConfig(serializeJson(request.getAutoRestoreConfig()))
                .reviewConfig(serializeJson(request.getReviewConfig()))
                .escalationConfig(serializeJson(request.getEscalationConfig()))
                .conditions(serializeMap(request.getConditions()))
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .createdBy(getCurrentUserId())
                .build();

        final QuarantineDurationPolicy savedPolicy = policyRepository.save(policy);
        policy = savedPolicy;

        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            policyRepository.findByProjectId(request.getProjectId()).stream()
                    .filter(p -> !p.getId().equals(savedPolicy.getId()) && Boolean.TRUE.equals(p.getIsDefault()))
                    .forEach(p -> {
                        p.setIsDefault(false);
                        policyRepository.save(p);
                    });
        }

        log.info("Created duration policy: {}", savedPolicy.getId());
        return mapToPolicyResponse(savedPolicy);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    public List<QuarantineDurationPolicyResponse> getDurationPolicies(UUID projectId) {
        return policyRepository.findByProjectId(projectId).stream()
                .map(this::mapToPolicyResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    public void deleteDurationPolicy(UUID policyId, UUID projectId) {
        QuarantineDurationPolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("QuarantineDurationPolicy", "id", policyId));
        if (!policy.getProjectId().equals(projectId)) {
            throw new InvalidOperationException("Policy does not belong to this project");
        }
        policyRepository.delete(policy);
    }

    private LocalDateTime calculateExpectedRestoreDate(UUID projectId, String triggerType) {
        Optional<QuarantineDurationPolicy> defaultPolicy = policyRepository.findByProjectIdAndIsDefaultTrue(projectId);
        if (defaultPolicy.isPresent()) {
            Map<String, Object> durationRule = parseMap(defaultPolicy.get().getDurationRule());
            Integer days = (Integer) durationRule.getOrDefault("durationDays", 30);
            return LocalDateTime.now().plusDays(days);
        }

        // Default based on trigger type
        return switch (triggerType) {
            case "auto_flaky" -> LocalDateTime.now().plusDays(14);
            case "environment" -> LocalDateTime.now().plusDays(30);
            case "third_party" -> LocalDateTime.now().plusDays(60);
            default -> LocalDateTime.now().plusDays(30);
        };
    }

    private Map<String, Object> getDefaultAutoRestoreConditions(UUID projectId) {
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("passCount", 5);
        conditions.put("daysElapsed", 7);
        conditions.put("passRateThreshold", 1.0);
        return conditions;
    }

    private boolean shouldRequireReview(QuarantineRequest request) {
        // Require review for permanent-looking issues
        if (request.getTriggerType() != null &&
                (request.getTriggerType().contains("third_party") ||
                        request.getTriggerType().contains("infrastructure"))) {
            return true;
        }
        return false;
    }

    private boolean hasTestBeenUpdated(UUID testId) {
        // Check if test has recent modifications that might indicate a fix
        Optional<TestIssue> test = testIssueRepository.findById(testId);
        return test.isPresent() && test.get().getUpdatedAt() != null &&
                test.get().getUpdatedAt().isAfter(LocalDateTime.now().minusDays(7));
    }

    // ==================== Review Workflow ====================

    @Transactional
    public QuarantineReviewResponse submitForReview(UUID quarantineId, String reviewerNotes) {
        TestQuarantine quarantine = quarantineRepository.findById(quarantineId)
                .orElseThrow(() -> new ResourceNotFoundException("TestQuarantine", "id", quarantineId));

        QuarantineReview review = reviewRepository.findByQuarantineId(quarantineId)
                .orElseGet(() -> createReviewFromQuarantine(quarantine));

        UUID currentUserId = getCurrentUserId();
        recordReviewHistory(review.getId(), quarantineId, "SUBMITTED", currentUserId, null,
                review.getStatus().name(), QuarantineReview.ReviewStatus.PENDING_REVIEW.name(),
                reviewerNotes, "Submitted for review");

        review.setStatus(QuarantineReview.ReviewStatus.PENDING_REVIEW);
        review.setReviewSubmittedAt(LocalDateTime.now());
        review.setReviewerNotes(reviewerNotes);
        reviewRepository.save(review);

        log.info("Quarantine {} submitted for review", quarantineId);
        return buildReviewResponse(review, quarantine);
    }

    @Transactional
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    public QuarantineReviewResponse processReview(QuarantineReviewRequest request, UUID projectId) {
        TestQuarantine quarantine = quarantineRepository.findById(request.getQuarantineId())
                .orElseThrow(() -> new ResourceNotFoundException("TestQuarantine", "id", request.getQuarantineId()));

        QuarantineReview review = reviewRepository.findByQuarantineId(request.getQuarantineId())
                .orElseThrow(() -> new ResourceNotFoundException("QuarantineReview", "quarantineId", request.getQuarantineId()));

        UUID currentUserId = getCurrentUserId();
        String previousStatus = review.getStatus().name();

        switch (request.getAction()) {
            case approve_restore -> {
                review.setStatus(QuarantineReview.ReviewStatus.APPROVED_FOR_RESTORE);
                quarantine.setStatus("candidate");
                quarantineRepository.save(quarantine);
                recordReviewHistory(review.getId(), quarantine.getId(), "APPROVED", currentUserId,
                        previousStatus, review.getStatus().name(), null, request.getReviewerNotes(),
                        "Approved for restore: " + request.getRecommendedAction());
            }
            case reject_restore -> {
                review.setStatus(QuarantineReview.ReviewStatus.REJECTED);
                recordReviewHistory(review.getId(), quarantine.getId(), "REJECTED", currentUserId,
                        previousStatus, review.getStatus().name(), null, request.getReviewerNotes(),
                        "Review rejected");
            }
            case extend_quarantine -> {
                review.setStatus(QuarantineReview.ReviewStatus.EXTENDED);
                if (request.getExtendDurationDays() != null) {
                    extendQuarantineDuration(quarantine.getId(), request.getExtendDurationDays());
                }
                recordReviewHistory(review.getId(), quarantine.getId(), "EXTENDED", currentUserId,
                        previousStatus, review.getStatus().name(), null, request.getReviewerNotes(),
                        "Extended by " + request.getExtendDurationDays() + " days");
            }
            case escalate -> {
                review.setStatus(QuarantineReview.ReviewStatus.ESCALATED);
                quarantine.setStatus("investigation");
                quarantineRepository.save(quarantine);
                recordReviewHistory(review.getId(), quarantine.getId(), "ESCALATED", currentUserId,
                        previousStatus, review.getStatus().name(), null, request.getReviewerNotes(),
                        "Escalated for further review");
            }
            case mark_permanent -> {
                review.setStatus(QuarantineReview.ReviewStatus.COMPLETED);
                quarantine.setStatus("permanent_quarantine");
                quarantineRepository.save(quarantine);
                recordReviewHistory(review.getId(), quarantine.getId(), "MARKED_PERMANENT", currentUserId,
                        previousStatus, review.getStatus().name(), null, request.getReviewerNotes(),
                        "Marked for permanent quarantine");
            }
            default -> {
            }
        }

        review.setReviewCompletedAt(LocalDateTime.now());
        review.setReviewerNotes(request.getReviewerNotes());
        review.setRecommendedAction(request.getRecommendedAction());
        review.setAutoRestoreOnFix(request.getAutoRestoreOnFix());
        reviewRepository.save(review);

        log.info("Review {} processed with action: {}", review.getId(), request.getAction());
        return buildReviewResponse(review, quarantine);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    public List<QuarantineReviewResponse> getPendingReviews(UUID projectId) {
        Set<UUID> projectTestIds = getProjectTestIds(projectId);

        return reviewRepository.findByStatus(QuarantineReview.ReviewStatus.PENDING_REVIEW).stream()
                .filter(r -> {
                    TestQuarantine q = quarantineRepository.findById(r.getQuarantineId()).orElse(null);
                    return q != null && projectTestIds.contains(q.getTestId());
                })
                .map(r -> {
                    TestQuarantine q = quarantineRepository.findById(r.getQuarantineId()).orElse(null);
                    return buildReviewResponse(r, q);
                })
                .collect(Collectors.toList());
    }

    private QuarantineReview createReviewFromQuarantine(TestQuarantine quarantine) {
        QuarantineReview review = QuarantineReview.builder()
                .quarantineId(quarantine.getId())
                .status(QuarantineReview.ReviewStatus.PENDING_REVIEW)
                .reviewSubmittedAt(LocalDateTime.now())
                .autoRestoreOnFix(true)
                .build();
        return reviewRepository.save(review);
    }

    private void extendQuarantineDuration(UUID quarantineId, int days) {
        log.debug("Extended quarantine {} review window by {} days", quarantineId, days);
    }

    private boolean hasPendingReview(UUID quarantineId) {
        return reviewRepository.findByQuarantineId(quarantineId).isPresent();
    }

    private QuarantineReviewResponse buildReviewResponse(QuarantineReview review, TestQuarantine quarantine) {
        List<QuarantineReviewHistory> history = reviewHistoryRepository
                .findByReviewIdOrderByCreatedAtDesc(review.getId());

        List<QuarantineReviewResponse.ReviewHistoryEntry> historyEntries = history.stream()
                .map(h -> QuarantineReviewResponse.ReviewHistoryEntry.builder()
                        .timestamp(h.getCreatedAt())
                        .reviewerId(h.getActorId())
                        .reviewerName(h.getActorName())
                        .action(h.getAction())
                        .notes(h.getNotes())
                        .previousStatus(h.getPreviousStatus())
                        .newStatus(h.getNewStatus())
                        .build())
                .collect(Collectors.toList());

        TestIssue test = quarantine != null ?
                testIssueRepository.findById(quarantine.getTestId()).orElse(null) : null;

        return QuarantineReviewResponse.builder()
                .quarantineId(review.getQuarantineId())
                .testId(quarantine != null ? quarantine.getTestId() : null)
                .testName(test != null ? test.getName() : null)
                .status(mapReviewStatus(review.getStatus()))
                .currentReviewer(review.getCurrentReviewer() != null ? review.getCurrentReviewer().toString() : null)
                .reviewSubmittedAt(review.getReviewSubmittedAt())
                .reviewCompletedAt(review.getReviewCompletedAt())
                .reviewHistory(historyEntries)
                .autoRestoreOnFix(review.getAutoRestoreOnFix())
                .build();
    }

    private QuarantineReviewResponse.ReviewStatus mapReviewStatus(QuarantineReview.ReviewStatus status) {
        return switch (status) {
            case PENDING_REVIEW -> QuarantineReviewResponse.ReviewStatus.pending_review;
            case UNDER_REVIEW -> QuarantineReviewResponse.ReviewStatus.under_review;
            case APPROVED_FOR_RESTORE -> QuarantineReviewResponse.ReviewStatus.approved_for_restore;
            case REJECTED -> QuarantineReviewResponse.ReviewStatus.rejected;
            case EXTENDED -> QuarantineReviewResponse.ReviewStatus.extended;
            case ESCALATED -> QuarantineReviewResponse.ReviewStatus.escalated;
            case COMPLETED -> QuarantineReviewResponse.ReviewStatus.completed;
            case CANCELLED -> QuarantineReviewResponse.ReviewStatus.completed;
        };
    }

    private void recordReviewHistory(UUID reviewId, UUID quarantineId, String action, UUID actorId,
            String previousStatus, String newStatus, String newStatusOverride, String notes, String metadata) {
        QuarantineReviewHistory history = QuarantineReviewHistory.builder()
                .reviewId(reviewId)
                .quarantineId(quarantineId)
                .action(action)
                .actorId(actorId)
                .actorType("USER")
                .previousStatus(previousStatus)
                .newStatus(newStatusOverride != null ? newStatusOverride : newStatus)
                .notes(notes)
                .metadata("{\"reason\": \"" + metadata + "\"}")
                .build();
        reviewHistoryRepository.save(history);
    }

    // ==================== Rules Management ====================

    @Transactional
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    public QuarantineRuleResponse createRule(QuarantineRuleRequest request) {
        QuarantineRule rule = QuarantineRule.builder()
                .projectId(request.getProjectId())
                .ruleName(request.getRuleName())
                .ruleType(request.getRuleType())
                .conditions(serializeMap(request.getConditions()))
                .autoQuarantine(request.getAutoQuarantine() != null ? request.getAutoQuarantine() : true)
                .notifyOnTrigger(request.getNotifyOnTrigger() != null ? request.getNotifyOnTrigger() : true)
                .createdBy(getCurrentUserId())
                .build();

        rule = ruleRepository.save(rule);
        log.info("Created quarantine rule: {}", rule.getId());
        return mapToRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    public List<QuarantineRuleResponse> getRules(UUID projectId) {
        return ruleRepository.findByProjectId(projectId).stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    public QuarantineRuleResponse updateRule(UUID ruleId, QuarantineRuleRequest request, UUID projectId) {
        QuarantineRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("QuarantineRule", "id", ruleId));

        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setConditions(serializeMap(request.getConditions()));
        rule.setAutoQuarantine(request.getAutoQuarantine());
        rule.setNotifyOnTrigger(request.getNotifyOnTrigger());
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }

        rule = ruleRepository.save(rule);
        return mapToRuleResponse(rule);
    }

    @Transactional
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #projectId)")
    public void deleteRule(UUID ruleId, UUID projectId) {
        QuarantineRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("QuarantineRule", "id", ruleId));
        if (!rule.getProjectId().equals(projectId)) {
            throw new InvalidOperationException("Rule does not belong to this project");
        }
        ruleRepository.delete(rule);
        log.info("Deleted quarantine rule: {}", ruleId);
    }

    // ==================== Transitions & Metrics ====================

    private void recordTransition(UUID quarantineId, String fromStatus, String toStatus,
            String reason, String triggerType, UUID transitionedBy) {
        QuarantineTransition transition = QuarantineTransition.builder()
                .quarantineId(quarantineId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .transitionReason(reason)
                .triggerType(triggerType)
                .transitionedBy(transitionedBy)
                .build();
        transitionRepository.save(transition);
    }

    @Transactional(readOnly = true)
    public List<QuarantineTransition> getTransitions(UUID quarantineId) {
        return transitionRepository.findByQuarantineIdOrderByTransitionedAtDesc(quarantineId);
    }

    private void recordMetrics(TestQuarantine quarantine) {
        QuarantineMetrics metrics = QuarantineMetrics.builder()
                .quarantineId(quarantine.getId())
                .metricDate(LocalDateTime.now())
                .quarantineAgeDays((int) ChronoUnit.DAYS.between(quarantine.getTriggeredAt(), LocalDateTime.now()))
                .executionCount(quarantine.getCurrentExecutionCount())
                .passCount(quarantine.getCurrentPassCount())
                .failCount(quarantine.getCurrentExecutionCount() - quarantine.getCurrentPassCount())
                .build();

        // Calculate flaky score
        if (quarantine.getCurrentExecutionCount() > 0) {
            double passRate = (double) quarantine.getCurrentPassCount() / quarantine.getCurrentExecutionCount();
            double flakyScore = 1.0 - passRate;
            metrics.setFlakyScore(BigDecimal.valueOf(flakyScore).setScale(2, RoundingMode.HALF_UP));
        }

        metricsRepository.save(metrics);
    }

    // ==================== History Tracking ====================

    @Transactional(readOnly = true)
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    public List<QuarantineResponse> getQuarantineHistory(UUID testId, UUID projectId) {
        // Get all quarantines for this test (including restored ones)
        Optional<TestQuarantine> quarantine = quarantineRepository.findByTestId(testId);
        if (quarantine.isEmpty()) {
            return Collections.emptyList();
        }

        TestQuarantine q = quarantine.get();
        TestIssue test = testIssueRepository.findById(testId).orElse(null);

        List<QuarantineResponse> history = new ArrayList<>();
        history.add(mapToQuarantineResponse(q, test));

        // Get all transitions for context
        List<QuarantineTransition> transitions = getTransitions(q.getId());

        return history;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@projectSecurity.canViewReports(authentication, #projectId)")
    public Map<String, Object> getQuarantineSummary(UUID projectId) {
        List<TestQuarantine> quarantines = quarantineRepository.findAll();
        Set<UUID> projectTestIds = getProjectTestIds(projectId);

        List<TestQuarantine> projectQuarantines = quarantines.stream()
                .filter(q -> projectTestIds.contains(q.getTestId()))
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalQuarantines", projectQuarantines.size());
        summary.put("activeQuarantines", projectQuarantines.stream()
                .filter(q -> !"restored".equals(q.getStatus())).count());
        summary.put("restoredQuarantines", projectQuarantines.stream()
                .filter(q -> "restored".equals(q.getStatus())).count());

        // Calculate average metrics
        List<TestQuarantine> restored = projectQuarantines.stream()
                .filter(q -> q.getRestoredAt() != null).toList();

        if (!restored.isEmpty()) {
            double avgDuration = restored.stream()
                    .mapToLong(q -> ChronoUnit.DAYS.between(q.getTriggeredAt(), q.getRestoredAt()))
                    .average().orElse(0);
            summary.put("averageDurationDays", BigDecimal.valueOf(avgDuration).setScale(1, RoundingMode.HALF_UP));
        }

        summary.put("resolutionRate", projectQuarantines.isEmpty() ? BigDecimal.ZERO :
                BigDecimal.valueOf((double) restored.size() / projectQuarantines.size() * 100)
                        .setScale(1, RoundingMode.HALF_UP));

        return summary;
    }

    // ==================== Helper Methods ====================

    private Set<UUID> getProjectTestIds(UUID projectId) {
        return testIssueRepository.findByProjectIdAndArchivedFalse(projectId).stream()
                .map(TestIssue::getId)
                .collect(Collectors.toSet());
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID) {
            return (UUID) auth.getPrincipal();
        }
        return null;
    }

    private String serializeMap(Map<String, Object> map) {
        if (map == null) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error serializing map", e);
            return "{}";
        }
    }

    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Error serializing value", e);
            return "{}";
        }
    }

    private <T> T parseJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("Error parsing json into {}", type.getSimpleName(), e);
            return null;
        }
    }

    private QuarantineDurationPolicy.PolicyType mapRequestPolicyType(QuarantineDurationPolicyRequest.PolicyType type) {
        if (type == null) {
            return QuarantineDurationPolicy.PolicyType.CUSTOM;
        }
        return QuarantineDurationPolicy.PolicyType.valueOf(type.name());
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing map", e);
            return new HashMap<>();
        }
    }

    private QuarantineResponse mapToQuarantineResponse(TestQuarantine quarantine, TestIssue test) {
        QuarantineResponse.QuarantineResponseBuilder builder = QuarantineResponse.builder()
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
                .createdAt(quarantine.getUpdatedAt());

        return builder.build();
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

    private QuarantineDurationPolicyResponse mapToPolicyResponse(QuarantineDurationPolicy policy) {
        return QuarantineDurationPolicyResponse.builder()
                .id(policy.getId())
                .projectId(policy.getProjectId())
                .policyName(policy.getPolicyName())
                .description(policy.getDescription())
                .policyType(mapPolicyType(policy.getPolicyType()))
                .durationRule(parseJson(policy.getDurationRule(), QuarantineDurationPolicyResponse.DurationRule.class))
                .autoRestoreConfig(parseJson(policy.getAutoRestoreConfig(), QuarantineDurationPolicyResponse.AutoRestoreConfig.class))
                .reviewConfig(parseJson(policy.getReviewConfig(), QuarantineDurationPolicyResponse.ReviewConfig.class))
                .escalationConfig(parseJson(policy.getEscalationConfig(), QuarantineDurationPolicyResponse.EscalationConfig.class))
                .conditions(parseMap(policy.getConditions()))
                .isDefault(policy.getIsDefault())
                .isActive(policy.getIsActive())
                .currentUsageCount(policy.getCurrentUsageCount())
                .historicalUsageCount(policy.getHistoricalUsageCount())
                .createdAt(policy.getCreatedAt())
                .createdBy(policy.getCreatedBy())
                .build();
    }

    private QuarantineDurationPolicyResponse.PolicyType mapPolicyType(QuarantineDurationPolicy.PolicyType type) {
        if (type == null) return QuarantineDurationPolicyResponse.PolicyType.CUSTOM;
        return switch (type) {
            case FLAKY_TEST -> QuarantineDurationPolicyResponse.PolicyType.FLAKY_TEST;
            case ENVIRONMENTAL -> QuarantineDurationPolicyResponse.PolicyType.ENVIRONMENTAL;
            case DATA_DEPENDENCY -> QuarantineDurationPolicyResponse.PolicyType.DATA_DEPENDENCY;
            case INFRASTRUCTURE -> QuarantineDurationPolicyResponse.PolicyType.INFRASTRUCTURE;
            case THIRD_PARTY -> QuarantineDurationPolicyResponse.PolicyType.THIRD_PARTY;
            case MANUAL_OVERRIDE -> QuarantineDurationPolicyResponse.PolicyType.MANUAL_OVERRIDE;
            case CUSTOM -> QuarantineDurationPolicyResponse.PolicyType.CUSTOM;
        };
    }
}
