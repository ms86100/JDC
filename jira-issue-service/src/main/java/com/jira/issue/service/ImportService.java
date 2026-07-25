package com.jira.issue.service;

import com.jira.issue.dto.*;
import com.jira.issue.entity.*;
import com.jira.issue.exception.*;
import com.jira.issue.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * ImportService - Cucumber/Gherkin and JUnit XML import
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final IssueRepository issueRepository;
    private final IssueTypeRepository issueTypeRepository;
    private final IssueStatusRepository issueStatusRepository;
    private final ProjectRepository projectRepository;
    private final CucumberScenarioRepository cucumberScenarioRepository;
    private final CucumberFeatureRepository cucumberFeatureRepository;
    private final TestSetRepository testSetRepository;
    private final TestImportBatchRepository importBatchRepository;
    private final TestExecutionRepository executionRepository;
    private final RequirementLinkRepository requirementLinkRepository;

    @Value("${app.defaults.import-test-type-bdd:BDD}")
    private String importTestTypeBdd;

    @Value("${app.defaults.import-test-type-automated:AUTOMATED}")
    private String importTestTypeAutomated;

    @Value("${app.defaults.import-test-status:DRAFT}")
    private String importTestStatus;

    @Value("${app.defaults.import-ci-source-manual:MANUAL}")
    private String importCiSourceManual;

    @Value("${app.defaults.import-default-env:CI}")
    private String importDefaultEnv;

    @Value("${app.defaults.import-test-set-active-status:ACTIVE}")
    private String importTestSetActiveStatus;

    @Value("${app.defaults.execution-status:RUNNING}")
    private String defaultExecutionStatus;

    // ==================== Cucumber/Gherkin Import ====================

    @Transactional
    public CucumberImportResponse importCucumberFeature(UUID projectId, UUID testSetId, MultipartFile file, UUID userId) throws Exception {
        log.info("Importing Cucumber feature file: {}", file.getOriginalFilename());

        // Create import batch
        TestImportBatch batch = TestImportBatch.builder()
                .importType("CUCUMBER")
                .ciSource(importCiSourceManual)
                .status("PROCESSING")
                .build();
        batch = importBatchRepository.save(batch);

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String fileName = file.getOriginalFilename();

        try {
            // Parse the feature file
            CucumberFeature feature = parseFeatureFile(projectId, testSetId, fileName, content, batch.getId());

            // Parse scenarios
            List<CucumberScenario> scenarios = parseScenarios(projectId, testSetId, feature, content, batch.getId());

            // Create test issues for each scenario
            int created = 0;
            int updated = 0;
            for (CucumberScenario scenario : scenarios) {
                Issue testIssue = findOrCreateTestFromScenario(projectId, scenario, userId);
                scenario.setIssueId(testIssue.getId());
                cucumberScenarioRepository.save(scenario);
                created++;
            }

            // Update batch
            batch.setStatus("COMPLETED");
            batch.setTotalTests(scenarios.size());
            batch.setTestsCreated(created);
            batch.setTestsUpdated(updated);
            batch.setFinishedAt(java.time.LocalDateTime.now());
            importBatchRepository.save(batch);

            return CucumberImportResponse.builder()
                    .batchId(batch.getId())
                    .featureKey(feature.getFeatureKey())
                    .scenariosImported(scenarios.size())
                    .testsCreated(created)
                    .testsUpdated(updated)
                    .status("COMPLETED")
                    .build();

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
            batch.setFinishedAt(java.time.LocalDateTime.now());
            importBatchRepository.save(batch);
            throw e;
        }
    }

    private CucumberFeature parseFeatureFile(UUID projectId, UUID testSetId, String fileName, String content, UUID batchId) {
        String featureName = extractFeatureName(content);
        String featureKey = generateFeatureKey(fileName, featureName);

        // Check if feature already exists
        Optional<CucumberFeature> existing = cucumberFeatureRepository.findByFeatureKey(featureKey);
        if (existing.isPresent()) {
            CucumberFeature feature = existing.get();
            feature.setRawContent(content);
            feature.setScenarioCount(countScenarios(content));
            return cucumberFeatureRepository.save(feature);
        }

        String[] featureTags = extractTags(content, "Feature");
        String background = extractBackground(content);

        CucumberFeature feature = CucumberFeature.builder()
                .featureKey(featureKey)
                .featureFile(fileName)
                .featureName(featureName)
                .featureTags(featureTags)
                .background(background)
                .scenarioCount(countScenarios(content))
                .testSetId(testSetId)
                .rawContent(content)
                .importBatchId(batchId)
                .build();

        return cucumberFeatureRepository.save(feature);
    }

    private List<CucumberScenario> parseScenarios(UUID projectId, UUID testSetId, CucumberFeature feature, String content, UUID batchId) {
        List<CucumberScenario> scenarios = new ArrayList<>();

        Pattern scenarioPattern = Pattern.compile("(?:(@[^\\n]*\\n)*)\\s*Scenario:\\s*(.*?)\\n((?:[\\s\\S](?!\\nScenario:))*?)(?=\\nScenario:|\\Z)", Pattern.MULTILINE);
        Matcher scenarioMatcher = scenarioPattern.matcher(content);

        while (scenarioMatcher.find()) {
            String tagsLine = scenarioMatcher.group(1) != null ? scenarioMatcher.group(1) : "";
            String scenarioName = scenarioMatcher.group(2).trim();
            String scenarioBody = scenarioMatcher.group(3);

            String[] tags = tagsLine.trim().isEmpty() ? new String[]{} :
                    Arrays.stream(tagsLine.split("@"))
                            .filter(t -> !t.trim().isEmpty())
                            .map(t -> "@" + t.trim())
                            .toArray(String[]::new);

            CucumberScenario scenario = CucumberScenario.builder()
                    .featureKey(feature.getFeatureKey())
                    .featureFile(feature.getFeatureFile())
                    .featureName(feature.getFeatureName())
                    .scenarioName(scenarioName)
                    .scenarioKey(feature.getFeatureKey() + "::" + scenarioName)
                    .scenarioType("Scenario")
                    .background(feature.getBackground())
                    .tags(tags)
                    .testSetId(testSetId)
                    .importBatchId(batchId)
                    .build();

            // Parse steps from scenario body
            scenario = cucumberScenarioRepository.save(scenario);
            scenarios.add(scenario);
        }

        // Also parse Scenario Outline
        Pattern outlinePattern = Pattern.compile("(?:(@[^\\n]*\\n)*)\\s*Scenario Outline:\\s*(.*?)\\n((?:[\\s\\S](?!\\nScenario |\\Z))*?)(?=\\nScenario |\\Z)", Pattern.MULTILINE);
        Matcher outlineMatcher = outlinePattern.matcher(content);

        while (outlineMatcher.find()) {
            String tagsLine = outlineMatcher.group(1) != null ? outlineMatcher.group(1) : "";
            String scenarioName = outlineMatcher.group(2).trim();
            String scenarioBody = outlineMatcher.group(3);

            String[] tags = tagsLine.trim().isEmpty() ? new String[]{} :
                    Arrays.stream(tagsLine.split("@"))
                            .filter(t -> !t.trim().isEmpty())
                            .map(t -> "@" + t.trim())
                            .toArray(String[]::new);

            CucumberScenario scenario = CucumberScenario.builder()
                    .featureKey(feature.getFeatureKey())
                    .featureFile(feature.getFeatureFile())
                    .featureName(feature.getFeatureName())
                    .scenarioName(scenarioName)
                    .scenarioKey(feature.getFeatureKey() + "::" + scenarioName)
                    .scenarioType("Scenario Outline")
                    .background(feature.getBackground())
                    .tags(tags)
                    .testSetId(testSetId)
                    .importBatchId(batchId)
                    .build();

            scenario = cucumberScenarioRepository.save(scenario);
            scenarios.add(scenario);
        }

        return scenarios;
    }

    private Issue findOrCreateTestFromScenario(UUID projectId, CucumberScenario scenario, UUID userId) {
        // Try to find existing test by gherkin scenario ID
        List<Issue> existing = issueRepository.findByProjectIdAndIssueTypeName(projectId, "Test").stream()
                .filter(i -> scenario.getScenarioKey().equals(i.getGherkinScenarioId()))
                .collect(Collectors.toList());

        if (!existing.isEmpty()) {
            Issue issue = existing.get(0);
            issue.setTitle(scenario.getScenarioName());
            issue.setGherkinFeatureKey(scenario.getFeatureKey());
            return issueRepository.save(issue);
        }

        // Create new test issue
        IssueType testType = issueTypeRepository.findByName("Test")
                .orElseThrow(() -> new ValidationException("Test issue type not found"));

        IssueStatus status = issueStatusRepository.findByName("To Do").orElse(null);

        Issue issue = Issue.builder()
                .projectId(projectId)
                .issueKey(generateTestKey(projectId))
                .title(scenario.getScenarioName())
                .description(generateScenarioDescription(scenario))
                .issueType(testType)
                .status(status)
                .testType(importTestTypeBdd)
                .testStatus(importTestStatus)
                .testSteps(generateStepsFromScenario(scenario))
                .gherkinFeatureKey(scenario.getFeatureKey())
                .gherkinScenarioId(scenario.getScenarioKey())
                .labels(scenario.getTags())
                .reporterId(userId)
                .creatorId(userId)
                .build();

        return issueRepository.save(issue);
    }

    // ==================== JUnit XML Import ====================

    @Transactional
    public JunitImportResponse importJunitXml(UUID projectId, UUID testSetId, MultipartFile file,
                                              String ciSource, String ciBuildUrl, UUID userId) throws Exception {
        log.info("Importing JUnit XML from CI: {}", ciSource);

        // Create import batch
        TestImportBatch batch = TestImportBatch.builder()
                .importType("JUNIT")
                .ciSource(ciSource)
                .ciBuildUrl(ciBuildUrl)
                .status("PROCESSING")
                .build();
        batch = importBatchRepository.save(batch);

        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        try {
            JUnitImportResult result = parseJUnitXml(projectId, testSetId, content, batch.getId(), userId);

            batch.setStatus("COMPLETED");
            batch.setTotalTests(result.totalTests);
            batch.setTotalPassed(result.passed);
            batch.setTotalFailed(result.failed);
            batch.setTotalSkipped(result.skipped);
            batch.setExecutionsCreated(result.executionsCreated);
            batch.setTestsCreated(result.testsCreated);
            batch.setFinishedAt(java.time.LocalDateTime.now());
            importBatchRepository.save(batch);

            return JunitImportResponse.builder()
                    .batchId(batch.getId())
                    .totalTests(result.totalTests)
                    .passed(result.passed)
                    .failed(result.failed)
                    .skipped(result.skipped)
                    .executionsCreated(result.executionsCreated)
                    .status("COMPLETED")
                    .build();

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batch.setErrorMessage(e.getMessage());
            batch.setFinishedAt(java.time.LocalDateTime.now());
            importBatchRepository.save(batch);
            throw e;
        }
    }

    private JUnitImportResult parseJUnitXml(UUID projectId, UUID testSetId, String content, UUID batchId, UUID userId) {
        JUnitImportResult result = new JUnitImportResult();

        // Parse XML
        Map<String, Object> xml = parseXmlContent(content);
        String suiteName = (String) xml.getOrDefault("name", "Test Suite");
        List<Map<String, Object>> testCases = (List<Map<String, Object>>) xml.getOrDefault("testCases", new ArrayList<>());

        // Create or find test set
        TestSet testSet = testSetRepository.findByProjectIdAndName(projectId, suiteName)
                .orElseGet(() -> {
                    TestSet newSet = TestSet.builder()
                            .projectId(projectId)
                            .name(suiteName)
                            .testType(importTestTypeAutomated)
                            .status(importTestSetActiveStatus)
                            .createdBy(userId)
                            .build();
                    return testSetRepository.save(newSet);
                });

        // Create test execution
        TestExecution execution = TestExecution.builder()
                .projectId(projectId)
                .testSetId(testSetId)
                .name(suiteName + " - " + java.time.LocalDateTime.now())
                .status(defaultExecutionStatus)
                .testEnv(importDefaultEnv)
                .testerId(userId)
                .ciBuildUrl((String) xml.get("buildUrl"))
                .createdBy(userId)
                .build();
        execution = executionRepository.save(execution);
        result.executionsCreated = 1;

        // Process test cases
        for (Map<String, Object> tc : testCases) {
            String testName = (String) tc.get("name");
            String className = (String) tc.get("classname");
            String status = (String) tc.get("status");
            long duration = ((Number) tc.getOrDefault("duration", 0)).longValue();

            result.totalTests++;

            switch (status) {
                case "passed": result.passed++; break;
                case "failed": result.failed++; break;
                case "skipped": result.skipped++; break;
            }

            // Find or create test
            String gherkinKey = className + "::" + testName;
            Issue test = findOrCreateJunitTest(projectId, testSetId, testName, className, gherkinKey, userId);
            if (test.getId() == null) result.testsCreated++;
        }

        return result;
    }

    private Issue findOrCreateJunitTest(UUID projectId, UUID testSetId, String testName, String className,
                                        String gherkinKey, UUID userId) {
        IssueType testType = issueTypeRepository.findByName("Test")
                .orElseThrow(() -> new ValidationException("Test issue type not found"));

        IssueStatus status = issueStatusRepository.findByName("To Do").orElse(null);

        Issue issue = Issue.builder()
                .projectId(projectId)
                .issueKey(generateTestKey(projectId))
                .title(testName)
                .description("Automated test from " + className)
                .issueType(testType)
                .status(status)
                .testType(importTestTypeAutomated)
                .testSetId(testSetId)
                .gherkinScenarioId(gherkinKey)
                .reporterId(userId)
                .creatorId(userId)
                .build();

        return issueRepository.save(issue);
    }

    // ==================== Helper Methods ====================

    private String extractFeatureName(String content) {
        Pattern pattern = Pattern.compile("Feature:\\s*(.+?)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : "Unknown Feature";
    }

    private String[] extractTags(String content, String section) {
        Pattern pattern = Pattern.compile("^@" + section + "\\s*(:\\s*@\\w+)*", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String tagsLine = matcher.group(0);
            return Arrays.stream(tagsLine.split("@"))
                    .filter(t -> !t.trim().isEmpty())
                    .map(t -> "@" + t.trim())
                    .toArray(String[]::new);
        }
        return new String[]{};
    }

    private String extractBackground(String content) {
        Pattern pattern = Pattern.compile("Background:\\s*\\n((?:.*?\\n)*?)(?=\\n\\w|\\Z)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private int countScenarios(String content) {
        int count = 0;
        Pattern pattern = Pattern.compile("^Scenario:", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) count++;
        Pattern outlinePattern = Pattern.compile("^Scenario Outline:", Pattern.MULTILINE);
        Matcher outlineMatcher = outlinePattern.matcher(content);
        while (outlineMatcher.find()) count++;
        return count;
    }

    private String generateFeatureKey(String fileName, String featureName) {
        return fileName.replace(".feature", "") + "::" + featureName;
    }

    private String generateTestKey(UUID projectId) {
        String projectKey = projectRepository.findById(projectId)
                .map(p -> p.getProjectKey())
                .orElse("TEST");
        long count = issueRepository.count() + 1;
        return projectKey + "-T" + count;
    }

    private String generateScenarioDescription(CucumberScenario scenario) {
        return "BDD Scenario: " + scenario.getScenarioName() +
                "\n\nFeature: " + scenario.getFeatureName() +
                "\n\nTags: " + String.join(", ", scenario.getTags());
    }

    private String generateStepsFromScenario(CucumberScenario scenario) {
        // This would parse the actual steps from the scenario
        return "[]";
    }

    private Map<String, Object> parseXmlContent(String xml) {
        Map<String, Object> result = new HashMap<>();
        // Very simplified XML parsing - use proper XML parser in production
        Pattern namePattern = Pattern.compile("name=\"([^\"]+)\"");
        Pattern timePattern = Pattern.compile("time=\"([^\"]+)\"");

        Matcher nameMatcher = namePattern.matcher(xml);
        if (nameMatcher.find()) result.put("name", nameMatcher.group(1));

        Matcher timeMatcher = timePattern.matcher(xml);
        if (timeMatcher.find()) result.put("time", timeMatcher.group(1));

        // Extract test cases
        List<Map<String, Object>> testCases = new ArrayList<>();
        Pattern tcPattern = Pattern.compile("<testcase[^>]+>", Pattern.MULTILINE);
        Matcher tcMatcher = tcPattern.matcher(xml);
        while (tcMatcher.find()) {
            Map<String, Object> tc = new HashMap<>();
            String tcXml = tcMatcher.group(0);

            Pattern tcName = Pattern.compile("name=\"([^\"]+)\"");
            Matcher nm = tcName.matcher(tcXml);
            tc.put("name", nm.find() ? nm.group(1) : "Unknown");

            Pattern tcClass = Pattern.compile("classname=\"([^\"]+)\"");
            Matcher cm = tcClass.matcher(tcXml);
            tc.put("classname", cm.find() ? cm.group(1) : "Unknown");

            Pattern tcTime = Pattern.compile("time=\"([^\"]+)\"");
            Matcher tm = tcTime.matcher(tcXml);
            tc.put("duration", tm.find() ? Double.parseDouble(tm.group(1)) * 1000 : 0);

            // Check for failure
            if (tcXml.contains("failure")) {
                tc.put("status", "failed");
            } else if (tcXml.contains("skipped")) {
                tc.put("status", "skipped");
            } else {
                tc.put("status", "passed");
            }

            testCases.add(tc);
        }
        result.put("testCases", testCases);

        return result;
    }

    // Inner class for results
    private static class JUnitImportResult {
        int totalTests = 0;
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        int testsCreated = 0;
        int executionsCreated = 0;
    }

    // ==================== Import Status ====================

    @Transactional(readOnly = true)
    public ImportBatchResponse getImportBatchStatus(UUID batchId) {
        TestImportBatch batch = importBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("ImportBatch", "id", batchId));

        return ImportBatchResponse.builder()
                .id(batch.getId())
                .importType(batch.getImportType())
                .ciSource(batch.getCiSource())
                .ciBuildUrl(batch.getCiBuildUrl())
                .status(batch.getStatus())
                .totalTests(batch.getTotalTests())
                .totalPassed(batch.getTotalPassed())
                .totalFailed(batch.getTotalFailed())
                .totalSkipped(batch.getTotalSkipped())
                .testsCreated(batch.getTestsCreated())
                .testsUpdated(batch.getTestsUpdated())
                .executionsCreated(batch.getExecutionsCreated())
                .errorMessage(batch.getErrorMessage())
                .startedAt(batch.getStartedAt())
                .finishedAt(batch.getFinishedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ImportBatchResponse> getImportHistory(UUID projectId) {
        return importBatchRepository.findAll().stream()
                .filter(b -> b.getStatus().equals("COMPLETED") || b.getStatus().equals("FAILED"))
                .map(b -> ImportBatchResponse.builder()
                        .id(b.getId())
                        .importType(b.getImportType())
                        .ciSource(b.getCiSource())
                        .ciBuildUrl(b.getCiBuildUrl())
                        .status(b.getStatus())
                        .totalTests(b.getTotalTests())
                        .totalPassed(b.getTotalPassed())
                        .totalFailed(b.getTotalFailed())
                        .testsCreated(b.getTestsCreated())
                        .errorMessage(b.getErrorMessage())
                        .startedAt(b.getStartedAt())
                        .finishedAt(b.getFinishedAt())
                        .build())
                .collect(Collectors.toList());
    }
}