package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CucumberImportService {

    private final CucumberScenarioRepository scenarioRepository;
    private final TestIssueRepository testIssueRepository;
    private final TestStepRepository testStepRepository;
    private final TestService testService;

    private static final Pattern FEATURE_PATTERN = Pattern.compile("Feature:\\s*(.+)", Pattern.MULTILINE);
    private static final Pattern SCENARIO_PATTERN = Pattern.compile("(?:(@\\w+)\\s*)*Scenario(?: Outline)?:\\s*(.+)", Pattern.MULTILINE);
    private static final Pattern STEP_PATTERN = Pattern.compile("(Given|When|Then|And|But)\\s+(.+)", Pattern.MULTILINE);
    private static final Pattern TAG_PATTERN = Pattern.compile("@(\\w+)", Pattern.MULTILINE);

    @Transactional
    public CucumberImportResponse importFeatureFile(UUID projectId, String featureContent, String fileName, List<String> filterTags, UUID testSetId) {
        log.info("Importing Cucumber feature file: {} for project: {}", fileName, projectId);

        List<String> errors = new ArrayList<>();
        List<TestResponse> createdTests = new ArrayList<>();
        int skipped = 0;

        String featureName = extractFeatureName(featureContent);
        String featureKey = fileName + "::" + featureName;

        List<ScenarioParseResult> scenarios = parseScenarios(featureContent, featureKey, featureName);

        for (ScenarioParseResult scenario : scenarios) {
            try {
                if (filterTags != null && !filterTags.isEmpty()) {
                    boolean hasTag = scenario.tags.stream().anyMatch(filterTags::contains);
                    if (!hasTag) {
                        skipped++;
                        continue;
                    }
                }

                CreateTestRequest request = CreateTestRequest.builder()
                        .projectId(projectId)
                        .name(scenario.name)
                        .description("BDD Scenario from " + featureName)
                        .testType("CUKE")
                        .labels(List.of("automated", "bdd"))
                        .requirementKeys(null)
                        .steps(scenario.steps)
                        .build();

                TestResponse test = testService.createTest(request);
                createdTests.add(test);

                CucumberScenario cucumberScenario = CucumberScenario.builder()
                        .featureKey(featureKey)
                        .featureFile(fileName)
                        .featureName(featureName)
                        .scenarioName(scenario.name)
                        .scenarioKey(scenario.key)
                        .scenarioType(scenario.type)
                        .background(scenario.background)
                        .tags(scenario.tags)
                        .lineNumber(scenario.lineNumber)
                        .testId(test.getId())
                        .build();
                scenarioRepository.save(cucumberScenario);

            } catch (Exception e) {
                log.warn("Failed to import scenario '{}': {}", scenario.name, e.getMessage());
                errors.add("Scenario '" + scenario.name + "': " + e.getMessage());
            }
        }

        log.info("Imported {} scenarios, skipped {}, errors: {}", createdTests.size(), skipped, errors.size());
        return CucumberImportResponse.builder()
                .batchId(UUID.randomUUID())
                .status(errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS")
                .totalScenarios(scenarios.size())
                .importedTests(createdTests.size())
                .skippedScenarios(skipped)
                .errors(errors)
                .createdTests(createdTests)
                .build();
    }

    private String extractFeatureName(String content) {
        Matcher m = FEATURE_PATTERN.matcher(content);
        if (m.find()) return m.group(1).trim();
        return "Unknown Feature";
    }

    private List<ScenarioParseResult> parseScenarios(String content, String featureKey, String featureName) {
        List<ScenarioParseResult> results = new ArrayList<>();
        String[] lines = content.split("\n");
        String currentScenarioTags = "";
        String currentBackground = "";
        String currentScenarioType = "Scenario";
        String currentScenarioName = "";
        List<CreateTestRequest.TestStepDto> currentSteps = new ArrayList<>();
        boolean inScenario = false;
        boolean inBackground = false;
        int scenarioLine = 0;
        int scenarioIndex = 0;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("Background:")) {
                inBackground = true;
                inScenario = false;
                currentBackground = "";
                continue;
            }

            if (trimmed.startsWith("Scenario") && !trimmed.startsWith("Scenarios")) {
                if (inScenario && !currentSteps.isEmpty()) {
                    results.add(buildScenario(currentScenarioName, currentScenarioType, currentSteps, featureKey,
                            featureName, currentBackground, extractTags(currentScenarioTags), scenarioIndex++, scenarioLine));
                }
                inScenario = true;
                inBackground = false;
                currentScenarioTags = "";
                currentScenarioName = trimmed.replaceAll("Scenario(?: Outline)?:\\s*", "");
                currentScenarioType = trimmed.startsWith("Scenario Outline") ? "Scenario Outline" : "Scenario";
                currentSteps = new ArrayList<>();
                currentBackground = "";
                scenarioLine = 0;
                continue;
            }

            if (inBackground && !trimmed.startsWith("#") && !trimmed.isEmpty()) {
                Matcher stepM = STEP_PATTERN.matcher(trimmed);
                if (stepM.find()) {
                    currentBackground += trimmed + "\n";
                }
            }

            if (trimmed.startsWith("@")) {
                currentScenarioTags = trimmed;
            }

            if (inScenario) {
                Matcher stepM = STEP_PATTERN.matcher(trimmed);
                if (stepM.find()) {
                    currentSteps.add(CreateTestRequest.TestStepDto.builder()
                            .stepType(stepM.group(1))
                            .description(stepM.group(2))
                            .build());
                }
            }
        }

        if (inScenario && !currentSteps.isEmpty()) {
            results.add(buildScenario(currentScenarioName, currentScenarioType, currentSteps, featureKey,
                    featureName, currentBackground, extractTags(currentScenarioTags), scenarioIndex, scenarioLine));
        }

        return results;
    }

    private ScenarioParseResult buildScenario(String name, String type, List<CreateTestRequest.TestStepDto> steps,
            String featureKey, String featureName, String background, List<String> tags, int index, int line) {
        return new ScenarioParseResult(
                name.trim(), type, steps, featureKey + "::" + name.trim() + "[" + index + "]",
                featureKey, featureName, background, tags, line
        );
    }

    private List<String> extractTags(String tagLine) {
        if (tagLine == null || tagLine.isBlank()) return List.of();
        List<String> tags = new ArrayList<>();
        Matcher m = TAG_PATTERN.matcher(tagLine);
        while (m.find()) tags.add(m.group(1));
        return tags;
    }

    private record ScenarioParseResult(String name, String type, List<CreateTestRequest.TestStepDto> steps,
            String key, String featureKey, String featureName, String background,
            List<String> tags, int lineNumber) {}
}