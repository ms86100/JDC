package com.jira.test.service;

import com.jira.test.entity.Precondition;
import com.jira.test.entity.PreconditionTemplate;
import com.jira.test.entity.PreconditionVersion;
import com.jira.test.entity.TestPreconditionLink;
import com.jira.test.entity.TestIssue;
import com.jira.test.repository.PreconditionRepository;
import com.jira.test.repository.PreconditionTemplateRepository;
import com.jira.test.repository.PreconditionVersionRepository;
import com.jira.test.repository.TestPreconditionLinkRepository;
import com.jira.test.repository.TestIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreconditionService {

    private final PreconditionRepository preconditionRepository;
    private final PreconditionTemplateRepository templateRepository;
    private final PreconditionVersionRepository versionRepository;
    private final TestPreconditionLinkRepository linkRepository;
    private final TestIssueRepository testIssueRepository;

    // Cache for optimization - stores evaluation results to skip re-evaluation
    private final Map<String, CachedEvaluationResult> evaluationCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    // ========== CRUD Operations ==========

    public Precondition createPrecondition(UUID projectId, CreatePreconditionRequest request) {
        Precondition precondition = Precondition.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(projectId)
                .preconditionType(request.getPreconditionType())
                .conditionScript(request.getConditionScript())
                .expectedResult(request.getExpectedResult())
                .status("ACTIVE")
                .category(request.getCategory())
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .version(1)
                .build();

        Precondition saved = preconditionRepository.save(precondition);
        createVersionSnapshot(saved, "CREATE", null);
        return saved;
    }

    public Precondition getPreconditionById(UUID id) {
        return preconditionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Precondition not found: " + id));
    }

    public List<Precondition> getPreconditionsByProject(UUID projectId) {
        return preconditionRepository.findByProjectId(projectId);
    }

    public List<Precondition> getPreconditionsByType(UUID projectId, String type) {
        return preconditionRepository.findByProjectIdAndPreconditionType(projectId, type);
    }

    public List<Precondition> getPreconditionsByCategory(UUID projectId, String category) {
        return preconditionRepository.findByProjectIdAndCategory(projectId, category);
    }

    @Transactional
    public Precondition updatePrecondition(UUID id, UpdatePreconditionRequest request) {
        Precondition precondition = getPreconditionById(id);
        String oldContent = buildVersionContent(precondition);

        if (request.getName() != null) precondition.setName(request.getName());
        if (request.getDescription() != null) precondition.setDescription(request.getDescription());
        if (request.getConditionScript() != null) precondition.setConditionScript(request.getConditionScript());
        if (request.getExpectedResult() != null) precondition.setExpectedResult(request.getExpectedResult());
        if (request.getStatus() != null) precondition.setStatus(request.getStatus());
        if (request.getCategory() != null) precondition.setCategory(request.getCategory());
        if (request.getTags() != null) precondition.setTags(String.join(",", request.getTags()));

        // Increment version if content changed
        boolean contentChanged = hasContentChanged(precondition, request);
        if (contentChanged) {
            precondition.setVersion(precondition.getVersion() + 1);
            precondition.setLastModifiedAt(LocalDateTime.now());
        }

        Precondition saved = preconditionRepository.save(precondition);
        if (contentChanged) {
            createVersionSnapshot(saved, "UPDATE", oldContent);
            invalidateCacheForPrecondition(id);
        }
        return saved;
    }

    @Transactional
    public void deletePrecondition(UUID id) {
        versionRepository.deleteByPreconditionId(id);
        List<TestPreconditionLink> links = linkRepository.findByPreconditionId(id);
        linkRepository.deleteAll(links);
        preconditionRepository.deleteById(id);
        invalidateCacheForPrecondition(id);
    }

    // ========== Precondition Templates ==========

    public PreconditionTemplate createTemplate(UUID projectId, CreateTemplateRequest request) {
        PreconditionTemplate template = PreconditionTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .preconditionType(request.getPreconditionType())
                .conditionScript(request.getConditionScript())
                .expectedResult(request.getExpectedResult())
                .tags(request.getTags() != null ? String.join(",", request.getTags()) : null)
                .isSystemTemplate(false)
                .usageCount(0)
                .build();
        return templateRepository.save(template);
    }

    public List<PreconditionTemplate> getTemplatesByCategory(String category) {
        return templateRepository.findByCategory(category);
    }

    public List<PreconditionTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    public List<PreconditionTemplate> getSystemTemplates() {
        return templateRepository.findByIsSystemTemplate(true);
    }

    public Precondition createFromTemplate(UUID projectId, UUID templateId, String name) {
        PreconditionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        Precondition precondition = Precondition.builder()
                .name(name)
                .description(template.getDescription())
                .projectId(projectId)
                .preconditionType(template.getPreconditionType())
                .conditionScript(template.getConditionScript())
                .expectedResult(template.getExpectedResult())
                .status("ACTIVE")
                .category(template.getCategory())
                .tags(template.getTags())
                .version(1)
                .build();

        template.setUsageCount(template.getUsageCount() + 1);
        templateRepository.save(template);

        return preconditionRepository.save(precondition);
    }

    public void incrementTemplateUsage(UUID templateId) {
        templateRepository.findById(templateId).ifPresent(template -> {
            template.setUsageCount(template.getUsageCount() + 1);
            templateRepository.save(template);
        });
    }

    // ========== Versioning ==========

    @Transactional
    public PreconditionVersion createVersionSnapshot(Precondition precondition, String changeType, String oldContent) {
        PreconditionVersion version = PreconditionVersion.builder()
                .preconditionId(precondition.getId())
                .versionNumber(precondition.getVersion())
                .name(precondition.getName())
                .description(precondition.getDescription())
                .conditionScript(precondition.getConditionScript())
                .expectedResult(precondition.getExpectedResult())
                .category(precondition.getCategory())
                .changeType(changeType)
                .oldContent(oldContent)
                .createdBy(precondition.getCreatedBy())
                .build();
        return versionRepository.save(version);
    }

    public List<PreconditionVersion> getVersionHistory(UUID preconditionId) {
        return versionRepository.findByPreconditionIdOrderByVersionNumberDesc(preconditionId);
    }

    public PreconditionVersion getVersion(UUID preconditionId, int versionNumber) {
        return versionRepository.findByPreconditionIdAndVersionNumber(preconditionId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));
    }

    @Transactional
    public Precondition rollbackToVersion(UUID preconditionId, int versionNumber) {
        PreconditionVersion version = getVersion(preconditionId, versionNumber);
        Precondition precondition = getPreconditionById(preconditionId);

        String oldContent = buildVersionContent(precondition);

        precondition.setName(version.getName());
        precondition.setDescription(version.getDescription());
        precondition.setConditionScript(version.getConditionScript());
        precondition.setExpectedResult(version.getExpectedResult());
        precondition.setCategory(version.getCategory());
        precondition.setVersion(precondition.getVersion() + 1);

        Precondition saved = preconditionRepository.save(precondition);
        createVersionSnapshot(saved, "ROLLBACK", oldContent);
        invalidateCacheForPrecondition(preconditionId);
        return saved;
    }

    private String buildVersionContent(Precondition p) {
        return String.format("%s|%s|%s|%s|%s",
                p.getName(), p.getDescription(), p.getConditionScript(),
                p.getExpectedResult(), p.getCategory());
    }

    private boolean hasContentChanged(Precondition p, UpdatePreconditionRequest req) {
        if (req.getConditionScript() != null && !req.getConditionScript().equals(p.getConditionScript())) return true;
        if (req.getName() != null && !req.getName().equals(p.getName())) return true;
        if (req.getCategory() != null && !req.getCategory().equals(p.getCategory())) return true;
        return false;
    }

    // ========== Link Operations ==========

    @Transactional
    public TestPreconditionLink linkPreconditionToTest(UUID testId, UUID preconditionId, Integer stepOrder,
                                                        List<UUID> dependsOnPreconditions) {
        testIssueRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));
        getPreconditionById(preconditionId);

        if (linkRepository.existsByTestIdAndPreconditionId(testId, preconditionId)) {
            throw new RuntimeException("Precondition already linked to this test");
        }

        TestPreconditionLink link = TestPreconditionLink.builder()
                .testId(testId)
                .preconditionId(preconditionId)
                .stepOrder(stepOrder != null ? stepOrder : getNextOrder(testId))
                .dependsOnPreconditions(dependsOnPreconditions != null ?
                        String.join(",", dependsOnPreconditions.stream().map(UUID::toString).toList()) : null)
                .build();

        return linkRepository.save(link);
    }

    @Transactional
    public TestPreconditionLink linkPreconditionToTest(UUID testId, UUID preconditionId, Integer stepOrder) {
        return linkPreconditionToTest(testId, preconditionId, stepOrder, null);
    }

    @Transactional
    public void unlinkPreconditionFromTest(UUID testId, UUID preconditionId) {
        linkRepository.deleteByTestIdAndPreconditionId(testId, preconditionId);
        invalidateCacheForTest(testId);
    }

    public List<Precondition> getPreconditionsForTest(UUID testId) {
        List<TestPreconditionLink> links = linkRepository.findByTestIdOrderByStepOrderAsc(testId);
        List<UUID> preconditionIds = links.stream()
                .map(TestPreconditionLink::getPreconditionId)
                .collect(Collectors.toList());

        if (preconditionIds.isEmpty()) {
            return List.of();
        }

        return preconditionRepository.findByIdIn(preconditionIds);
    }

    public List<TestPreconditionLink> getPreconditionLinksForTest(UUID testId) {
        return linkRepository.findByTestIdOrderByStepOrderAsc(testId);
    }

    public List<TestIssue> getTestsUsingPrecondition(UUID preconditionId) {
        List<TestPreconditionLink> links = linkRepository.findByPreconditionId(preconditionId);
        List<UUID> testIds = links.stream()
                .map(TestPreconditionLink::getTestId)
                .collect(Collectors.toList());

        if (testIds.isEmpty()) {
            return List.of();
        }

        return testIssueRepository.findAllById(testIds);
    }

    public TestPreconditionLink updateLinkNotes(UUID testId, UUID preconditionId, String notes) {
        TestPreconditionLink link = linkRepository.findByTestIdAndPreconditionId(testId, preconditionId)
                .orElseThrow(() -> new RuntimeException("Link not found"));
        link.setNotes(notes);
        return linkRepository.save(link);
    }

    // ========== Categories ==========

    public List<PreconditionCategory> getCategories() {
        return List.of(
                PreconditionCategory.builder().id("ENVIRONMENTAL").name("Environmental")
                        .description("Preconditions related to environment setup")
                        .icon("environment").color("#4CAF50").build(),
                PreconditionCategory.builder().id("DATA").name("Data")
                        .description("Preconditions related to data state")
                        .icon("database").color("#2196F3").build(),
                PreconditionCategory.builder().id("SYSTEM").name("System")
                        .description("Preconditions related to system state")
                        .icon("system").color("#9C27B0").build(),
                PreconditionCategory.builder().id("CONFIGURATION").name("Configuration")
                        .description("Preconditions related to configuration")
                        .icon("settings").color("#FF9800").build(),
                PreconditionCategory.builder().id("NETWORK").name("Network")
                        .description("Preconditions related to network state")
                        .icon("network").color("#00BCD4").build(),
                PreconditionCategory.builder().id("AUTHENTICATION").name("Authentication")
                        .description("Preconditions related to authentication")
                        .icon("security").color("#F44336").build()
        );
    }

    // ========== Evaluation with Optimization ==========

    public PreconditionEvaluationResult evaluatePreconditions(UUID testId, EvaluationContext context) {
        return evaluatePreconditions(testId, context, false);
    }

    public PreconditionEvaluationResult evaluatePreconditions(UUID testId, EvaluationContext context, boolean skipCached) {
        // Check cache first
        String cacheKey = buildCacheKey(testId, context);
        if (!skipCached && evaluationCache.containsKey(cacheKey)) {
            CachedEvaluationResult cached = evaluationCache.get(cacheKey);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
                log.debug("Returning cached evaluation for test: {}", testId);
                return cached.result;
            }
        }

        List<TestPreconditionLink> links = linkRepository.findByTestIdOrderByStepOrderAsc(testId);
        List<Precondition> preconditions = links.stream()
                .map(link -> {
                    try {
                        return getPreconditionById(link.getPreconditionId());
                    } catch (Exception e) {
                        log.warn("Precondition not found: {}", link.getPreconditionId());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        PreconditionEvaluationResult result = PreconditionEvaluationResult.builder()
                .testId(testId)
                .totalPreconditions(preconditions.size())
                .evaluatedPreconditions(0)
                .passedPreconditions(0)
                .failedPreconditions(0)
                .skippedPreconditions(0)
                .results(new ArrayList<>())
                .overallPassed(true)
                .evaluationTimeMs(System.currentTimeMillis())
                .build();

        // Track already evaluated conditions to skip duplicate evaluations
        Set<String> evaluatedConditions = new HashSet<>();

        for (int i = 0; i < preconditions.size(); i++) {
            Precondition precondition = preconditions.get(i);
            TestPreconditionLink link = links.get(i);

            // Check dependencies before evaluating
            boolean dependenciesMet = checkDependencies(link, evaluatedConditions, result);
            if (!dependenciesMet) {
                PreconditionResult skippedResult = PreconditionResult.builder()
                        .preconditionId(precondition.getId())
                        .preconditionName(precondition.getName())
                        .passed(false)
                        .skipped(true)
                        .message("Dependencies not met")
                        .build();
                result.getResults().add(skippedResult);
                result.setSkippedPreconditions(result.getSkippedPreconditions() + 1);
                continue;
            }

            // Create unique condition key for caching
            String conditionKey = buildConditionKey(precondition, context);
            PreconditionResult pr;

            if (evaluatedConditions.contains(conditionKey)) {
                // Reuse result from earlier evaluation
                pr = findEarlierResult(result, conditionKey);
                if (pr != null) {
                    pr = PreconditionResult.builder()
                            .preconditionId(precondition.getId())
                            .preconditionName(precondition.getName())
                            .passed(pr.isPassed())
                            .message("Reused from earlier evaluation")
                            .requiresManualCheck(pr.isRequiresManualCheck())
                            .build();
                }
            } else {
                pr = evaluateSinglePrecondition(precondition, context);
                evaluatedConditions.add(conditionKey);
            }

            result.getResults().add(pr);
            result.setEvaluatedPreconditions(result.getEvaluatedPreconditions() + 1);
            if (pr.isPassed()) {
                result.setPassedPreconditions(result.getPassedPreconditions() + 1);
            } else if (!pr.isSkipped()) {
                result.setFailedPreconditions(result.getFailedPreconditions() + 1);
                result.setOverallPassed(false);
            }
        }

        result.setEvaluationTimeMs(System.currentTimeMillis() - result.getEvaluationTimeMs());
        result.setOptimized(!evaluatedConditions.isEmpty());

        // Cache the result
        evaluationCache.put(cacheKey, new CachedEvaluationResult(result, System.currentTimeMillis()));

        return result;
    }

    private boolean checkDependencies(TestPreconditionLink link, Set<String> evaluatedConditions,
                                       PreconditionEvaluationResult result) {
        if (link.getDependsOnPreconditions() == null || link.getDependsOnPreconditions().isBlank()) {
            return true;
        }

        List<String> depIds = Arrays.asList(link.getDependsOnPreconditions().split(","));
        for (String depId : depIds) {
            UUID depUuid;
            try {
                depUuid = UUID.fromString(depId.trim());
            } catch (Exception e) {
                continue;
            }
            boolean found = result.getResults().stream()
                    .filter(r -> r.getPreconditionId().equals(depUuid))
                    .anyMatch(PreconditionResult::isPassed);
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private String buildConditionKey(Precondition p, EvaluationContext ctx) {
        return String.format("%s:%s:%s", p.getId(), ctx.getEnvironment(),
                ctx.getVariables() != null ? ctx.getVariables().hashCode() : 0);
    }

    private PreconditionResult findEarlierResult(PreconditionEvaluationResult result, String conditionKey) {
        // This is a simplified lookup - in production you'd store condition keys in results
        return null;
    }

    private String buildCacheKey(UUID testId, EvaluationContext context) {
        return String.format("%s:%s:%s", testId, context.getEnvironment(),
                context.getVariables() != null ? context.getVariables().hashCode() : "empty");
    }

    private PreconditionResult evaluateSinglePrecondition(Precondition precondition, EvaluationContext context) {
        PreconditionResult result = PreconditionResult.builder()
                .preconditionId(precondition.getId())
                .preconditionName(precondition.getName())
                .category(precondition.getCategory())
                .passed(false)
                .message("")
                .build();

        long startTime = System.currentTimeMillis();
        try {
            if ("MANUAL".equals(precondition.getPreconditionType())) {
                result.setPassed(true);
                result.setMessage("Manual verification required");
                result.setRequiresManualCheck(true);
            } else {
                boolean evaluationResult = evaluateConditionScript(precondition.getConditionScript(), context);
                result.setPassed(evaluationResult);
                result.setMessage(evaluationResult ? "Condition met" : "Condition not met");
            }
        } catch (Exception e) {
            result.setPassed(false);
            result.setMessage("Evaluation error: " + e.getMessage());
            result.setError(true);
            log.error("Error evaluating precondition {}: {}", precondition.getId(), e.getMessage());
        }

        result.setEvaluationTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private boolean evaluateConditionScript(String script, EvaluationContext context) {
        if (script == null || script.isBlank()) {
            return true;
        }

        String trimmed = script.trim();

        // Check for simple boolean expressions
        if (trimmed.equalsIgnoreCase("true") || trimmed.equals("1")) return true;
        if (trimmed.equalsIgnoreCase("false") || trimmed.equals("0")) return false;

        // Check environment variable
        if (context.getEnvironment() != null) {
            if (trimmed.equalsIgnoreCase(context.getEnvironment())) return true;
            if (trimmed.contains("${ENV}") && context.getEnvironment().matches(trimmed.replace("${ENV}", ".*"))) return true;
        }

        // Check context variables
        if (context.getVariables() != null) {
            if (context.getVariables().containsKey(trimmed)) {
                Object value = context.getVariables().get(trimmed);
                return value != null && !value.toString().equals("null") && !value.toString().isEmpty();
            }

            // Support expression evaluation
            if (trimmed.contains("==") || trimmed.contains("!=") || trimmed.contains(">") || trimmed.contains("<")) {
                return evaluateExpression(trimmed, context.getVariables());
            }
        }

        // Check feature flags
        if (context.getFeatureFlags() != null) {
            if (context.getFeatureFlags().containsKey(trimmed)) {
                return Boolean.parseBoolean(context.getFeatureFlags().get(trimmed));
            }
        }

        // Default: condition met if script can't be evaluated
        return true;
    }

    private boolean evaluateExpression(String expression, Map<String, Object> variables) {
        try {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String varName = entry.getKey();
                Object varValue = entry.getValue();
                String replacement = varValue != null ? varValue.toString() : "";
                expression = expression.replace(varName, replacement);
            }
            // Simple math evaluation for numbers
            if (expression.matches(".*\\d+\\s*[<>=!]+\\s*\\d+.*")) {
                return evaluateNumericExpression(expression);
            }
        } catch (Exception e) {
            log.warn("Expression evaluation failed: {}", e.getMessage());
        }
        return true;
    }

    private boolean evaluateNumericExpression(String expr) {
        try {
            // Simple comparisons
            if (expr.contains(">=")) {
                String[] parts = expr.split(">=");
                return Double.parseDouble(parts[0].trim()) >= Double.parseDouble(parts[1].trim());
            }
            if (expr.contains("<=")) {
                String[] parts = expr.split("<=");
                return Double.parseDouble(parts[0].trim()) <= Double.parseDouble(parts[1].trim());
            }
            if (expr.contains("==")) {
                String[] parts = expr.split("==");
                return parts[0].trim().equals(parts[1].trim());
            }
            if (expr.contains("!=")) {
                String[] parts = expr.split("!=");
                return !parts[0].trim().equals(parts[1].trim());
            }
        } catch (NumberFormatException e) {
            // Not a numeric comparison
        }
        return true;
    }

    private int getNextOrder(UUID testId) {
        List<TestPreconditionLink> links = linkRepository.findByTestId(testId);
        return links.stream()
                .mapToInt(TestPreconditionLink::getStepOrder)
                .max()
                .orElse(0) + 1;
    }

    // ========== Fuzzy Search ==========

    public List<PreconditionSearchResult> fuzzySearchPreconditions(UUID projectId, String query, double threshold) {
        List<Precondition> allPreconditions = preconditionRepository.findByProjectId(projectId);
        List<PreconditionSearchResult> results = new ArrayList<>();

        String normalizedQuery = query.toLowerCase().trim();

        for (Precondition p : allPreconditions) {
            double score = calculateSimilarity(normalizedQuery, p.getName().toLowerCase());
            double descScore = calculateSimilarity(normalizedQuery,
                    p.getDescription() != null ? p.getDescription().toLowerCase() : "");

            double maxScore = Math.max(score, descScore * 0.7); // Description weighted lower

            if (maxScore >= threshold) {
                results.add(PreconditionSearchResult.builder()
                        .precondition(p)
                        .score(maxScore)
                        .matchedField(score >= descScore ? "name" : "description")
                        .build());
            }
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(PreconditionSearchResult::getScore).reversed())
                .collect(Collectors.toList());
    }

    public List<PreconditionSearchResult> searchPreconditions(UUID projectId, String query) {
        return fuzzySearchPreconditions(projectId, query, 0.3);
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0;
        if (s1.equals(s2)) return 1.0;

        // Levenshtein distance based similarity
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return 1.0 - (double) distance / maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    public List<Precondition> searchPreconditionsSimple(UUID projectId, String query) {
        return preconditionRepository.searchByName(projectId, query);
    }

    // ========== Impact Analysis ==========

    public PreconditionImpactAnalysis analyzeImpact(UUID preconditionId) {
        Precondition precondition = getPreconditionById(preconditionId);
        List<TestIssue> affectedTests = getTestsUsingPrecondition(preconditionId);
        List<TestPreconditionLink> links = linkRepository.findByPreconditionId(preconditionId);

        Set<String> affectedEnvironments = affectedTests.stream()
                .map(TestIssue::getEnvironment)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> affectedPriorities = affectedTests.stream()
                .map(TestIssue::getPriority)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        long criticalTests = affectedTests.stream()
                .filter(t -> "Critical".equals(t.getPriority()) || "Blocker".equals(t.getPriority()))
                .count();

        return PreconditionImpactAnalysis.builder()
                .preconditionId(preconditionId)
                .preconditionName(precondition.getName())
                .totalAffectedTests(affectedTests.size())
                .criticalTests(criticalTests)
                .affectedEnvironments(new ArrayList<>(affectedEnvironments))
                .affectedPriorities(new ArrayList<>(affectedPriorities))
                .links(links)
                .canSafelyDelete(affectedTests.isEmpty())
                .requiresReview(!affectedTests.isEmpty())
                .build();
    }

    // ========== Bulk Operations ==========

    @Transactional
    public BulkOperationResult bulkUpdateStatus(List<UUID> preconditionIds, String newStatus) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (UUID id : preconditionIds) {
            try {
                Precondition p = getPreconditionById(id);
                p.setStatus(newStatus);
                p.setVersion(p.getVersion() + 1);
                preconditionRepository.save(p);
                createVersionSnapshot(p, "BULK_UPDATE", null);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add(String.format("Failed to update %s: %s", id, e.getMessage()));
            }
        }

        return BulkOperationResult.builder()
                .total(preconditionIds.size())
                .success(success)
                .failed(failed)
                .errors(errors)
                .build();
    }

    @Transactional
    public List<Precondition> bulkCreate(UUID projectId, List<CreatePreconditionRequest> requests) {
        List<Precondition> created = new ArrayList<>();
        for (CreatePreconditionRequest request : requests) {
            created.add(createPrecondition(projectId, request));
        }
        return created;
    }

    @Transactional
    public void bulkDelete(List<UUID> preconditionIds) {
        for (UUID id : preconditionIds) {
            deletePrecondition(id);
        }
    }

    // ========== Cache Management ==========

    public void invalidateCacheForPrecondition(UUID preconditionId) {
        evaluationCache.entrySet().removeIf(entry ->
                entry.getKey().contains(preconditionId.toString()));
    }

    public void invalidateCacheForTest(UUID testId) {
        evaluationCache.entrySet().removeIf(entry ->
                entry.getKey().startsWith(testId.toString()));
    }

    public void clearEvaluationCache() {
        evaluationCache.clear();
    }

    // ========== Duplicate ==========

    public Precondition duplicatePrecondition(UUID preconditionId, String newName) {
        Precondition original = getPreconditionById(preconditionId);

        Precondition duplicate = Precondition.builder()
                .name(newName)
                .description(original.getDescription())
                .projectId(original.getProjectId())
                .preconditionType(original.getPreconditionType())
                .conditionScript(original.getConditionScript())
                .expectedResult(original.getExpectedResult())
                .status("ACTIVE")
                .category(original.getCategory())
                .tags(original.getTags())
                .version(1)
                .build();

        return preconditionRepository.save(duplicate);
    }

    // ========== DTOs ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreatePreconditionRequest {
        private String name;
        private String description;
        private String preconditionType;
        private String conditionScript;
        private String expectedResult;
        private String category;
        private List<String> tags;
        private List<UUID> dependsOn;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UpdatePreconditionRequest {
        private String name;
        private String description;
        private String conditionScript;
        private String expectedResult;
        private String status;
        private String category;
        private List<String> tags;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EvaluationContext {
        private UUID userId;
        private UUID projectId;
        private String environment;
        private List<String> userRoles;
        private Map<String, String> featureFlags;
        private Map<String, Object> variables;
        private Map<String, String> headers;
        private String executionId;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreconditionEvaluationResult {
        private UUID testId;
        private int totalPreconditions;
        private int evaluatedPreconditions;
        private int passedPreconditions;
        private int failedPreconditions;
        private int skippedPreconditions;
        private List<PreconditionResult> results;
        @lombok.Builder.Default
        private boolean overallPassed = true;
        @lombok.Builder.Default
        private long evaluationTimeMs = 0;
        @lombok.Builder.Default
        private boolean optimized = false;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreconditionResult {
        private UUID preconditionId;
        private String preconditionName;
        private String category;
        @lombok.Builder.Default
        private boolean passed = false;
        private String message;
        @lombok.Builder.Default
        private boolean requiresManualCheck = false;
        @lombok.Builder.Default
        private boolean error = false;
        @lombok.Builder.Default
        private boolean skipped = false;
        @lombok.Builder.Default
        private long evaluationTimeMs = 0;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreconditionSearchResult {
        private Precondition precondition;
        private double score;
        private String matchedField;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreconditionImpactAnalysis {
        private UUID preconditionId;
        private String preconditionName;
        private int totalAffectedTests;
        private int criticalTests;
        private List<String> affectedEnvironments;
        private List<String> affectedPriorities;
        private List<TestPreconditionLink> links;
        private boolean canSafelyDelete;
        private boolean requiresReview;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BulkOperationResult {
        private int total;
        private int success;
        private int failed;
        private List<String> errors;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreconditionCategory {
        private String id;
        private String name;
        private String description;
        private String icon;
        private String color;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateTemplateRequest {
        private String name;
        private String description;
        private String category;
        private String preconditionType;
        private String conditionScript;
        private String expectedResult;
        private List<String> tags;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CachedEvaluationResult {
        private PreconditionEvaluationResult result;
        private long timestamp;
    }
}