package com.avionics_systems.issue.service;

import com.avionics_systems.issue.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI Test Service
 * Phase 16 - AI Features
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiTestService {

    private final TestManagementService testService;

    public List<TestResponse> findDuplicateTests(TestResponse test, List<TestResponse> allTests) {
        List<TestResponse> duplicates = new ArrayList<>();
        Set<String> testTokens = tokenize(test);

        for (TestResponse candidate : allTests) {
            if (candidate.getId() != null && candidate.getId().equals(test.getId())) continue;
            Set<String> candidateTokens = tokenize(candidate);
            double similarity = jaccardSimilarity(testTokens, candidateTokens);
            if (similarity > 0.7) {
                duplicates.add(candidate);
            }
        }
        return duplicates;
    }

    public List<TestResponse> getCoverageRecommendations(String requirementKey, UUID projectId) {
        List<TestResponse> tests = testService.getTestsByProject(projectId, null, null, null);
        List<TestResponse> recommended = new ArrayList<>();

        for (TestResponse test : tests) {
            List<String> reqKeys = test.getRequirementKeys();
            if (reqKeys == null || !reqKeys.contains(requirementKey)) {
                recommended.add(test);
                if (recommended.size() >= 5) break;
            }
        }
        return recommended;
    }

    public Map<String, Object> clusterFailures(List<TestExecutionResponse> failures) {
        Map<String, Object> clusters = new HashMap<>();
        int total = failures.size();

        for (TestExecutionResponse failure : failures) {
            String key = "group_" + (failures.indexOf(failure) % 3);
            clusters.putIfAbsent(key, new ArrayList<TestExecutionResponse>());
            @SuppressWarnings("unchecked")
            List<TestExecutionResponse> list = (List<TestExecutionResponse>) clusters.get(key);
            list.add(failure);
        }
        clusters.put("totalFailures", total);
        return clusters;
    }

    public List<TestResponse> suggestTests(String keywords, UUID projectId) {
        List<TestResponse> allTests = testService.getTestsByProject(projectId, null, null, null);
        List<TestResponse> suggestions = new ArrayList<>();
        String[] keywordsArr = keywords.toLowerCase().split("\\s+");

        for (TestResponse test : allTests) {
            String searchable = (test.getTitle() + " " +
                (test.getDescription() != null ? test.getDescription() : "")).toLowerCase();
            for (String kw : keywordsArr) {
                if (searchable.contains(kw)) {
                    suggestions.add(test);
                    break;
                }
            }
        }
        return suggestions;
    }

    public Map<String, Object> assessRisk(UUID testId) {
        Map<String, Object> risk = new HashMap<>();
        risk.put("score", 0.5);
        risk.put("level", "MEDIUM");
        risk.put("factors", List.of("No execution history", "Manual test"));
        return risk;
    }

    private Set<String> tokenize(TestResponse test) {
        Set<String> tokens = new HashSet<>();
        String text = (test.getTitle() + " " +
            (test.getDescription() != null ? test.getDescription() : "") + " " +
            (test.getLabels() != null ? String.join(" ", test.getLabels()) : "")).toLowerCase();
        for (String word : text.split("\\s+")) {
            if (word.length() > 3) tokens.add(word);
        }
        return tokens;
    }

    private double jaccardSimilarity(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
}