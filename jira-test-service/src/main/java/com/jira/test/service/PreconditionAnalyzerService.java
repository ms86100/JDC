package com.jira.test.service;

import com.jira.test.entity.Precondition;
import com.jira.test.entity.PreconditionVersion;
import com.jira.test.entity.TestPreconditionLink;
import com.jira.test.entity.TestIssue;
import com.jira.test.repository.PreconditionRepository;
import com.jira.test.repository.PreconditionVersionRepository;
import com.jira.test.repository.TestPreconditionLinkRepository;
import com.jira.test.repository.TestIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreconditionAnalyzerService {

    private final PreconditionRepository preconditionRepository;
    private final PreconditionVersionRepository versionRepository;
    private final TestPreconditionLinkRepository linkRepository;
    private final TestIssueRepository testIssueRepository;

    // ========== Conflict Detection ==========

    public ConflictAnalysisResult analyzeConflicts(UUID preconditionId) {
        Precondition precondition = preconditionRepository.findById(preconditionId)
                .orElseThrow(() -> new RuntimeException("Precondition not found: " + preconditionId));

        List<ConflictInfo> conflicts = new ArrayList<>();
        List<Precondition> projectPreconditions = preconditionRepository.findByProjectId(precondition.getProjectId());

        // Check for exact script duplicates
        for (Precondition other : projectPreconditions) {
            if (other.getId().equals(preconditionId)) continue;

            if (areConditionsIdentical(precondition, other)) {
                conflicts.add(ConflictInfo.builder()
                        .type(ConflictType.DUPLICATE_CONDITION)
                        .severity(Severity.HIGH)
                        .conflictingPreconditionId(other.getId())
                        .conflictingPreconditionName(other.getName())
                        .description("Identical condition script with: " + other.getName())
                        .resolution("Consider merging these preconditions or using a shared version")
                        .build());
            }

            // Check for mutually exclusive conditions
            if (areConditionsMutuallyExclusive(precondition, other)) {
                conflicts.add(ConflictInfo.builder()
                        .type(ConflictType.MUTUALLY_EXCLUSIVE)
                        .severity(Severity.MEDIUM)
                        .conflictingPreconditionId(other.getId())
                        .conflictingPreconditionName(other.getName())
                        .description("These conditions cannot both be true")
                        .resolution("Review the condition logic - only one should be used at a time")
                        .build());
            }

            // Check for overlapping scopes
            if (hasOverlappingScope(precondition, other)) {
                conflicts.add(ConflictInfo.builder()
                        .type(ConflictType.OVERLAPPING_SCOPE)
                        .severity(Severity.LOW)
                        .conflictingPreconditionId(other.getId())
                        .conflictingPreconditionName(other.getName())
                        .description("These preconditions may have overlapping effect scope")
                        .resolution("Consider if one precondition covers the other's requirements")
                        .build());
            }
        }

        return ConflictAnalysisResult.builder()
                .preconditionId(preconditionId)
                .preconditionName(precondition.getName())
                .conflicts(conflicts)
                .hasConflicts(!conflicts.isEmpty())
                .criticalCount((int) conflicts.stream().filter(c -> c.getSeverity() == Severity.HIGH).count())
                .warningCount((int) conflicts.stream().filter(c -> c.getSeverity() == Severity.MEDIUM).count())
                .infoCount((int) conflicts.stream().filter(c -> c.getSeverity() == Severity.LOW).count())
                .build();
    }

    public List<ConflictInfo> findAllConflictsInProject(UUID projectId) {
        List<Precondition> preconditions = preconditionRepository.findByProjectId(projectId);
        List<ConflictInfo> allConflicts = new ArrayList<>();

        for (int i = 0; i < preconditions.size(); i++) {
            for (int j = i + 1; j < preconditions.size(); j++) {
                Precondition p1 = preconditions.get(i);
                Precondition p2 = preconditions.get(j);

                if (areConditionsIdentical(p1, p2)) {
                    allConflicts.add(ConflictInfo.builder()
                            .type(ConflictType.DUPLICATE_CONDITION)
                            .severity(Severity.HIGH)
                            .preconditionId1(p1.getId())
                            .preconditionName1(p1.getName())
                            .conflictingPreconditionId(p2.getId())
                            .conflictingPreconditionName(p2.getName())
                            .description("Duplicate preconditions: " + p1.getName() + " and " + p2.getName())
                            .resolution("Merge or delete one of the duplicates")
                            .build());
                }
            }
        }

        return allConflicts;
    }

    private boolean areConditionsIdentical(Precondition p1, Precondition p2) {
        if (p1.getConditionScript() == null && p2.getConditionScript() == null) return true;
        if (p1.getConditionScript() == null || p2.getConditionScript() == null) return false;
        return p1.getConditionScript().trim().equals(p2.getConditionScript().trim());
    }

    private boolean areConditionsMutuallyExclusive(Precondition p1, Precondition p2) {
        String s1 = p1.getConditionScript();
        String s2 = p2.getConditionScript();
        if (s1 == null || s2 == null) return false;

        // Check for obvious mutual exclusivity patterns
        // e.g., "env == 'prod'" vs "env == 'staging'"
        if (s1.contains("==") && s2.contains("==")) {
            String var1 = extractVariable(s1);
            String var2 = extractVariable(s2);
            if (var1 != null && var1.equals(var2)) {
                String val1 = extractValue(s1);
                String val2 = extractValue(s2);
                return val1 != null && val2 != null && !val1.equals(val2);
            }
        }
        return false;
    }

    private boolean hasOverlappingScope(Precondition p1, Precondition p2) {
        // Simple overlap detection based on category or common variable references
        if (p1.getCategory() != null && p1.getCategory().equals(p2.getCategory())) {
            return true;
        }
        return false;
    }

    private String extractVariable(String script) {
        if (script == null) return null;
        int eqIndex = script.indexOf("==");
        if (eqIndex > 0) {
            return script.substring(0, eqIndex).trim();
        }
        return null;
    }

    private String extractValue(String script) {
        if (script == null) return null;
        int eqIndex = script.indexOf("==");
        if (eqIndex > 0 && eqIndex < script.length() - 1) {
            return script.substring(eqIndex + 1).trim().replace("'", "").replace("\"", "");
        }
        return null;
    }

    // ========== Circular Dependency Detection ==========

    public CircularDependencyResult detectCircularDependencies(UUID testId) {
        List<TestPreconditionLink> links = linkRepository.findByTestId(testId);
        Map<UUID, List<UUID>> dependencyGraph = buildDependencyGraph(links);

        List<List<UUID>> cycles = findCycles(dependencyGraph);
        boolean hasCycles = !cycles.isEmpty();

        List<CircularDependencyInfo> cycleInfos = cycles.stream()
                .map(cycle -> {
                    List<String> cycleNames = cycle.stream()
                            .map(id -> {
                                try {
                                    return preconditionRepository.findById(id)
                                            .map(Precondition::getName)
                                            .orElse(id.toString());
                                } catch (Exception e) {
                                    return id.toString();
                                }
                            })
                            .collect(Collectors.toList());

                    return CircularDependencyInfo.builder()
                            .cycleIds(cycle)
                            .cycleNames(cycleNames)
                            .cycleLength(cycle.size())
                            .description("Circular dependency: " + String.join(" -> ", cycleNames))
                            .breakpointSuggestion("Remove dependency from " + cycleNames.get(cycle.size() - 1) + " to " + cycleNames.get(0))
                            .build();
                })
                .collect(Collectors.toList());

        return CircularDependencyResult.builder()
                .testId(testId)
                .hasCircularDependencies(hasCycles)
                .cycles(cycleInfos)
                .totalCycles(cycles.size())
                .build();
    }

    private Map<UUID, List<UUID>> buildDependencyGraph(List<TestPreconditionLink> links) {
        Map<UUID, List<UUID>> graph = new HashMap<>();
        for (TestPreconditionLink link : links) {
            graph.computeIfAbsent(link.getPreconditionId(), k -> new ArrayList<>());
            if (link.getDependsOnPreconditions() != null && !link.getDependsOnPreconditions().isBlank()) {
                List<String> depIds = Arrays.asList(link.getDependsOnPreconditions().split(","));
                for (String depId : depIds) {
                    try {
                        UUID depUuid = UUID.fromString(depId.trim());
                        graph.get(link.getPreconditionId()).add(depUuid);
                    } catch (Exception e) {
                        // Ignore invalid UUIDs
                    }
                }
            }
        }
        return graph;
    }

    private List<List<UUID>> findCycles(Map<UUID, List<UUID>> graph) {
        List<List<UUID>> cycles = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        Set<UUID> recursionStack = new HashSet<>();

        for (UUID node : graph.keySet()) {
            if (!visited.contains(node)) {
                findCyclesUtil(node, graph, visited, recursionStack, new ArrayList<>(), cycles);
            }
        }
        return cycles;
    }

    private void findCyclesUtil(UUID node, Map<UUID, List<UUID>> graph,
                                Set<UUID> visited, Set<UUID> recursionStack,
                                List<UUID> path, List<List<UUID>> cycles) {
        visited.add(node);
        recursionStack.add(node);
        path.add(node);

        List<UUID> neighbors = graph.getOrDefault(node, Collections.emptyList());
        for (UUID neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                findCyclesUtil(neighbor, graph, visited, recursionStack, path, cycles);
            } else if (recursionStack.contains(neighbor)) {
                // Found a cycle
                int cycleStart = path.indexOf(neighbor);
                List<UUID> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                cycle.add(neighbor); // Complete the cycle
                if (!containsCycle(cycles, cycle)) {
                    cycles.add(cycle);
                }
            }
        }

        path.remove(path.size() - 1);
        recursionStack.remove(node);
    }

    private boolean containsCycle(List<List<UUID>> cycles, List<UUID> newCycle) {
        Set<UUID> newCycleSet = new HashSet<>(newCycle);
        return cycles.stream().anyMatch(c -> new HashSet<>(c).equals(newCycleSet));
    }

    // ========== Optimization Suggestions ==========

    public OptimizationSuggestions getOptimizationSuggestions(UUID testId) {
        List<TestPreconditionLink> links = linkRepository.findByTestId(testId);
        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        // Check for duplicate evaluations
        List<String> evaluatedScripts = new ArrayList<>();
        Map<String, List<UUID>> scriptToPreconditions = new HashMap<>();

        for (TestPreconditionLink link : links) {
            try {
                Precondition p = preconditionRepository.findById(link.getPreconditionId()).orElse(null);
                if (p != null && p.getConditionScript() != null) {
                    String script = p.getConditionScript().trim().toLowerCase();
                    scriptToPreconditions.computeIfAbsent(script, k -> new ArrayList<>())
                            .add(p.getId());

                    if (evaluatedScripts.contains(script)) {
                        suggestions.add(OptimizationSuggestion.builder()
                                .type(SuggestionType.DUPLICATE_EVALUATION)
                                .severity(Severity.MEDIUM)
                                .preconditionIds(scriptToPreconditions.get(script))
                                .title("Duplicate Condition Evaluation")
                                .description("This condition is evaluated multiple times for the same test")
                                .potentialSavings("1 evaluation (50%+ reduction)")
                                .recommendation("Use shared preconditions or cache results")
                                .build());
                    }
                    evaluatedScripts.add(script);
                }
            } catch (Exception e) {
                log.warn("Could not analyze link: {}", e.getMessage());
            }
        }

        // Check for unnecessary manual preconditions
        long manualCount = links.stream()
                .filter(link -> {
                    try {
                        Precondition p = preconditionRepository.findById(link.getPreconditionId()).orElse(null);
                        return p != null && "MANUAL".equals(p.getPreconditionType());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        if (manualCount > 5) {
            suggestions.add(OptimizationSuggestion.builder()
                    .type(SuggestionType.EXCESSIVE_MANUAL_CHECKS)
                    .severity(Severity.LOW)
                    .title("High Manual Verification Count")
                    .description("This test has " + manualCount + " manual preconditions")
                    .potentialSavings(manualCount + " manual checks")
                    .recommendation("Consider automating some manual preconditions or grouping them")
                    .build());
        }

        // Check for missing dependencies
        List<UUID> allPreconditionIds = links.stream()
                .map(TestPreconditionLink::getPreconditionId)
                .collect(Collectors.toList());

        Set<UUID> referencedIds = new HashSet<>();
        for (TestPreconditionLink link : links) {
            if (link.getDependsOnPreconditions() != null) {
                Arrays.stream(link.getDependsOnPreconditions().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(s -> {
                            try { referencedIds.add(UUID.fromString(s)); } catch (Exception e) {}
                        });
            }
        }

        Set<UUID> missingDependencies = new HashSet<>(referencedIds);
        missingDependencies.removeAll(allPreconditionIds);

        if (!missingDependencies.isEmpty()) {
            suggestions.add(OptimizationSuggestion.builder()
                    .type(SuggestionType.MISSING_DEPENDENCIES)
                    .severity(Severity.HIGH)
                    .preconditionIds(new ArrayList<>(missingDependencies))
                    .title("Missing Precondition Dependencies")
                    .description("Some referenced preconditions are not linked to this test")
                    .potentialSavings("Avoid runtime failures")
                    .recommendation("Add missing preconditions or fix dependency references")
                    .build());
        }

        // Check for inefficient evaluation order
        if (links.size() > 3) {
            long automatedCount = links.stream()
                    .filter(link -> {
                        try {
                            Precondition p = preconditionRepository.findById(link.getPreconditionId()).orElse(null);
                            return p != null && !"MANUAL".equals(p.getPreconditionType());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            if (automatedCount > 0 && automatedCount < links.size()) {
                suggestions.add(OptimizationSuggestion.builder()
                        .type(SuggestionType.INEFFICIENT_ORDER)
                        .severity(Severity.LOW)
                        .title("Optimizable Evaluation Order")
                        .description("Consider grouping automated preconditions first, then manual")
                        .potentialSavings("Fail fast on automated checks")
                        .recommendation("Reorder preconditions to run automated checks before manual")
                        .build());
            }
        }

        return OptimizationSuggestions.builder()
                .testId(testId)
                .suggestions(suggestions)
                .totalSuggestions(suggestions.size())
                .highPriorityCount((int) suggestions.stream().filter(s -> s.getSeverity() == Severity.HIGH).count())
                .estimatedSavings(calculateEstimatedSavings(suggestions))
                .build();
    }

    private String calculateEstimatedSavings(List<OptimizationSuggestion> suggestions) {
        int totalDeductions = (int) suggestions.stream()
                .filter(s -> s.getType() == SuggestionType.DUPLICATE_EVALUATION)
                .count();
        if (totalDeductions > 0) {
            return "~" + (totalDeductions * 100) + "% reduction in duplicate evaluations";
        }
        return "Minimal savings";
    }

    // ========== Coverage Impact Analysis ==========

    public CoverageImpactAnalysis analyzeCoverageImpact(UUID preconditionId) {
        Precondition precondition = preconditionRepository.findById(preconditionId)
                .orElseThrow(() -> new RuntimeException("Precondition not found: " + preconditionId));

        List<TestPreconditionLink> links = linkRepository.findByPreconditionId(preconditionId);
        List<UUID> testIds = links.stream().map(TestPreconditionLink::getTestId).collect(Collectors.toList());

        List<TestIssue> affectedTests = testIds.isEmpty() ? List.of() :
                testIssueRepository.findAllById(testIds);

        Map<String, Long> coverageByCategory = affectedTests.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPriority() != null ? t.getPriority() : "Unassigned",
                        Collectors.counting()
                ));

        Map<String, Long> coverageByStatus = affectedTests.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus() != null ? t.getStatus() : "Unknown",
                        Collectors.counting()
                ));

        double totalCoverage = calculateTotalCoverage(precondition.getProjectId());
        double affectedCoverage = affectedTests.size() / Math.max(totalCoverage, 1.0) * 100;

        return CoverageImpactAnalysis.builder()
                .preconditionId(preconditionId)
                .preconditionName(precondition.getName())
                .totalAffectedTests(affectedTests.size())
                .coverageByPriority(coverageByCategory)
                .coverageByStatus(coverageByStatus)
                .percentageOfTotalCoverage(affectedCoverage)
                .category(precondition.getCategory())
                .riskLevel(determineRiskLevel(affectedCoverage, affectedTests.size()))
                .affectedTestIds(testIds)
                .build();
    }

    private double calculateTotalCoverage(UUID projectId) {
        // Get total tests in project
        return testIssueRepository.findByProjectId(projectId).size();
    }

    private String determineRiskLevel(double coveragePercent, int affectedCount) {
        if (coveragePercent > 50 || affectedCount > 100) {
            return "HIGH";
        } else if (coveragePercent > 20 || affectedCount > 50) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    // ========== Precondition Health Check ==========

    public PreconditionHealthReport generateHealthReport(UUID projectId) {
        List<Precondition> preconditions = preconditionRepository.findByProjectId(projectId);

        int totalCount = preconditions.size();
        int activeCount = (int) preconditions.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count();
        int inactiveCount = totalCount - activeCount;

        Map<String, Integer> byCategory = preconditions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategory() != null ? p.getCategory() : "Uncategorized",
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        Map<String, Integer> byType = preconditions.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPreconditionType() != null ? p.getPreconditionType() : "Unknown",
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        int orphanedCount = (int) preconditions.stream()
                .filter(p -> {
                    List<TestPreconditionLink> links = linkRepository.findByPreconditionId(p.getId());
                    return links.isEmpty();
                })
                .count();

        List<Precondition> unusedPreconditions = preconditions.stream()
                .filter(p -> linkRepository.findByPreconditionId(p.getId()).isEmpty())
                .collect(Collectors.toList());

        double healthScore = calculateHealthScore(totalCount, orphanedCount, inactiveCount);

        return PreconditionHealthReport.builder()
                .projectId(projectId)
                .totalPreconditions(totalCount)
                .activePreconditions(activeCount)
                .inactivePreconditions(inactiveCount)
                .orphanedPreconditions(orphanedCount)
                .unusedPreconditions(unusedPreconditions)
                .distributionByCategory(byCategory)
                .distributionByType(byType)
                .healthScore(healthScore)
                .recommendations(generateRecommendations(totalCount, orphanedCount, inactiveCount, healthScore))
                .build();
    }

    private double calculateHealthScore(int total, int orphaned, int inactive) {
        if (total == 0) return 100.0;

        double orphanedPenalty = (orphaned * 10.0) / Math.max(total, 1);
        double inactivePenalty = (inactive * 2.0) / Math.max(total, 1);
        double utilizationBonus = ((total - orphaned) * 5.0) / Math.max(total, 1);

        return Math.max(0, Math.min(100, 100 - orphanedPenalty - inactivePenalty + utilizationBonus));
    }

    private List<String> generateRecommendations(int total, int orphaned, int inactive, double healthScore) {
        List<String> recommendations = new ArrayList<>();

        if (orphaned > total * 0.2) {
            recommendations.add("High number of orphaned preconditions (" + orphaned + "). Consider deleting unused preconditions.");
        }
        if (inactive > total * 0.3) {
            recommendations.add("Many inactive preconditions (" + inactive + "). Review if they should be deleted or reactivated.");
        }
        if (healthScore < 70) {
            recommendations.add("Overall health score is below 70. Conduct a precondition review.");
        }
        if (total < 5) {
            recommendations.add("Low precondition usage. Consider adding preconditions for better test coverage.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Precondition library is well-maintained.");
        }

        return recommendations;
    }

    // ========== DTOs ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConflictAnalysisResult {
        private UUID preconditionId;
        private String preconditionName;
        private List<ConflictInfo> conflicts;
        private boolean hasConflicts;
        private int criticalCount;
        private int warningCount;
        private int infoCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConflictInfo {
        private ConflictType type;
        private Severity severity;
        private UUID preconditionId1;
        private String preconditionName1;
        private UUID conflictingPreconditionId;
        private String conflictingPreconditionName;
        private String description;
        private String resolution;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CircularDependencyResult {
        private UUID testId;
        private boolean hasCircularDependencies;
        private List<CircularDependencyInfo> cycles;
        private int totalCycles;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CircularDependencyInfo {
        private List<UUID> cycleIds;
        private List<String> cycleNames;
        private int cycleLength;
        private String description;
        private String breakpointSuggestion;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OptimizationSuggestions {
        private UUID testId;
        private List<OptimizationSuggestion> suggestions;
        private int totalSuggestions;
        private int highPriorityCount;
        private String estimatedSavings;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OptimizationSuggestion {
        private SuggestionType type;
        private Severity severity;
        private List<UUID> preconditionIds;
        private String title;
        private String description;
        private String potentialSavings;
        private String recommendation;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CoverageImpactAnalysis {
        private UUID preconditionId;
        private String preconditionName;
        private int totalAffectedTests;
        private Map<String, Long> coverageByPriority;
        private Map<String, Long> coverageByStatus;
        private double percentageOfTotalCoverage;
        private String category;
        private String riskLevel;
        private List<UUID> affectedTestIds;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PreconditionHealthReport {
        private UUID projectId;
        private int totalPreconditions;
        private int activePreconditions;
        private int inactivePreconditions;
        private int orphanedPreconditions;
        private List<Precondition> unusedPreconditions;
        private Map<String, Integer> distributionByCategory;
        private Map<String, Integer> distributionByType;
        private double healthScore;
        private List<String> recommendations;
    }

    public enum ConflictType {
        DUPLICATE_CONDITION,
        MUTUALLY_EXCLUSIVE,
        OVERLAPPING_SCOPE,
        MISSING_DEPENDENCY,
        CIRCULAR_DEPENDENCY
    }

    public enum Severity {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum SuggestionType {
        DUPLICATE_EVALUATION,
        EXCESSIVE_MANUAL_CHECKS,
        MISSING_DEPENDENCIES,
        INEFFICIENT_ORDER,
        UNUSED_PRECONDITION
    }
}