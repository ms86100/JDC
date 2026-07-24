package com.jira.test.service;

import com.jira.test.dto.*;
import com.jira.test.entity.*;
import com.jira.test.event.*;
import com.jira.test.exception.*;
import com.jira.test.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CiCdImportService {

    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;
    private final TestService testService;
    private final TestSetRepository testSetRepository;
    private final TestSetItemRepository testSetItemRepository;
    private final EventPublisherService eventPublisher;

    @Transactional
    public JunitImportResponse importJUnitXml(JunitImportRequest request) {
        log.info("Importing JUnit XML results for project: {}", request.getProjectId());

        List<String> errors = new ArrayList<>();
        List<TestResponse> createdTests = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        int successCount = 0;
        int failureCount = 0;
        UUID batchId = UUID.randomUUID();

        try {
            JUnitParseResult parseResult = parseJUnitResults(request.getXmlContent());

            TestSet testSet = null;
            if (request.getTestSetId() != null) {
                testSet = testSetRepository.findById(request.getTestSetId())
                        .orElse(null);
            }

            TestExecution execution = TestExecution.builder()
                    .testId(null)
                    .name("CI Run: " + (request.getCiJobName() != null ? request.getCiJobName() : "Unknown Job"))
                    .description("Auto-imported from CI/CD pipeline")
                    .status(parseResult.allPassed ? "PASSED" : "FAILED")
                    .testEnv("CI")
                    .ciBuildUrl(request.getCiBuildUrl())
                    .ciJobId(request.getCiBuildNumber())
                    .testCycle(request.getBranch())
                    .totalTests(parseResult.total)
                    .passedTests(parseResult.passed)
                    .failedTests(parseResult.failed)
                    .blockedTests(0)
                    .notRunTests(parseResult.skipped)
                    .startedAt(LocalDateTime.now().minusMinutes(5))
                    .finishedAt(LocalDateTime.now())
                    .build();

            execution = executionRepository.save(execution);

            for (JUnitTestCase testCase : parseResult.testCases) {
                try {
                    TestIssue test = testIssueRepository.findAll().stream()
                            .filter(t -> t.getName().equals(testCase.name) && t.getProjectId().equals(request.getProjectId()))
                            .findFirst()
                            .orElse(null);

                    if (test == null) {
                        CreateTestRequest createRequest = CreateTestRequest.builder()
                                .projectId(request.getProjectId())
                                .name(testCase.name)
                                .description("Auto-created from JUnit import: " + testCase.className)
                                .testType("AUTOMATED")
                                .labels(List.of("automated", "ci-import"))
                                .build();

                        test = TestIssue.builder()
                                .projectId(request.getProjectId())
                                .name(testCase.name)
                                .description("Auto-created from JUnit import: " + testCase.className)
                                .testType("AUTOMATED")
                                .labels(List.of("automated", "ci-import"))
                                .status(testCase.status)
                                .build();
                        test = testIssueRepository.save(test);
                        createdTests.add(mapToTestResponse(test));
                        successCount++;
                    }

                    StepResult stepResult = StepResult.builder()
                            .executionId(execution.getId())
                            .stepId(test.getId())
                            .status(testCase.status)
                            .actualResult(testCase.message)
                            .executedAt(LocalDateTime.now())
                            .build();
                    stepResultRepository.save(stepResult);

                    if ("PASSED".equals(testCase.status)) passed++;
                    else if ("FAILED".equals(testCase.status)) failed++;
                    else skipped++;

                } catch (Exception e) {
                    log.warn("Failed to process test case '{}': {}", testCase.name, e.getMessage());
                    errors.add("Test case '" + testCase.name + "': " + e.getMessage());
                    failureCount++;
                }
            }

            if (testSet != null) {
                execution.setTestSetId(testSet.getId());
                executionRepository.save(execution);
            }

            log.info("JUnit import completed: {} passed, {} failed, {} skipped", passed, failed, skipped);

            // Publish TestImportedEvent
            String ciSource = detectCiSource(request.getCiBuildUrl());
            publishTestImportedEvent(request.getProjectId(), batchId, ciSource, "JUNIT_XML",
                    parseResult.total, successCount, failureCount, errors, testSet != null ? testSet.getId() : null);

            return JunitImportResponse.builder()
                    .batchId(batchId)
                    .status(errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS")
                    .totalTests(parseResult.total)
                    .passed(passed)
                    .failed(failed)
                    .skipped(skipped)
                    .message("Import completed successfully")
                    .createdTests(createdTests)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse JUnit XML: {}", e.getMessage(), e);

            // Publish failure event
            publishTestImportedEvent(request.getProjectId(), batchId, "JUNIT", "JUNIT_XML",
                    0, 0, 1, List.of(e.getMessage()), null);

            return JunitImportResponse.builder()
                    .batchId(batchId)
                    .status("FAILED")
                    .totalTests(0)
                    .passed(0)
                    .failed(0)
                    .skipped(0)
                    .message("Failed to parse JUnit XML: " + e.getMessage())
                    .createdTests(List.of())
                    .build();
        }
    }

    private void publishTestImportedEvent(UUID projectId, UUID batchId, String importSource,
                                          String importType, int totalImported, int successCount,
                                          int failureCount, List<String> errors, UUID testPlanId) {
        try {
            TestImportedEvent event = TestImportedEvent.builder()
                    .source(this)
                    .projectId(projectId)
                    .batchId(batchId)
                    .importSource(importSource)
                    .importType(importType)
                    .totalImported(totalImported)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .errors(errors)
                    .testPlanId(testPlanId)
                    .build();
            eventPublisher.publish(event);
            log.info("Published TestImportedEvent for JUnit batch: {}", batchId);
        } catch (Exception e) {
            log.error("Failed to publish TestImportedEvent: {}", e.getMessage(), e);
        }
    }

    public String detectCiSource(String buildUrl) {
        if (buildUrl == null) return "UNKNOWN";

        if (buildUrl.contains("github.com") || buildUrl.contains("gitlab.com")) {
            return "GITHUB_ACTIONS";
        } else if (buildUrl.contains("jenkins")) {
            return "JENKINS";
        } else if (buildUrl.contains("azure")) {
            return "AZURE_DEVOPS";
        } else if (buildUrl.contains("circleci")) {
            return "CIRCLECI";
        } else if (buildUrl.contains("travis")) {
            return "TRAVIS";
        } else if (buildUrl.contains("gitlab")) {
            return "GITLAB_CI";
        }
        return "UNKNOWN";
    }

    private JUnitParseResult parseJUnitResults(String xmlContent) throws Exception {
        List<JUnitTestCase> testCases = new ArrayList<>();
        int total = 0;
        int passed = 0;
        int failed = 0;
        int skipped = 0;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

        NodeList testSuites = document.getElementsByTagName("testsuite");
        for (int i = 0; i < testSuites.getLength(); i++) {
            Element suite = (Element) testSuites.item(i);
            total += Integer.parseInt(getAttribute(suite, "tests", "0"));
            skipped += Integer.parseInt(getAttribute(suite, "skipped", "0"));

            NodeList testCasesNodes = suite.getElementsByTagName("testcase");
            for (int j = 0; j < testCasesNodes.getLength(); j++) {
                Element testCase = (Element) testCasesNodes.item(j);
                String className = getAttribute(testCase, "classname", "Unknown");
                String name = getAttribute(testCase, "name", "Unknown");
                double time = Double.parseDouble(getAttribute(testCase, "time", "0"));

                String status = "PASSED";
                String message = null;

                NodeList failures = testCase.getElementsByTagName("failure");
                if (failures.getLength() > 0) {
                    status = "FAILED";
                    Element failure = (Element) failures.item(0);
                    message = getTextContent(failure);
                    failed++;
                } else {
                    passed++;
                }

                testCases.add(new JUnitTestCase(name, className, status, message, time));
            }
        }

        if (testSuites.getLength() == 0) {
            NodeList testCasesNodes = document.getElementsByTagName("testcase");
            for (int j = 0; j < testCasesNodes.getLength(); j++) {
                Element testCase = (Element) testCasesNodes.item(j);
                String className = getAttribute(testCase, "classname", "Unknown");
                String name = getAttribute(testCase, "name", "Unknown");

                String status = "PASSED";
                String message = null;

                NodeList failures = testCase.getElementsByTagName("failure");
                if (failures.getLength() > 0) {
                    status = "FAILED";
                    Element failure = (Element) failures.item(0);
                    message = getTextContent(failure);
                    failed++;
                } else {
                    passed++;
                }

                testCases.add(new JUnitTestCase(name, className, status, message, 0));
                total++;
            }
        }

        boolean allPassed = failed == 0;
        return new JUnitParseResult(total, passed, failed, skipped, allPassed, testCases);
    }

    private String getAttribute(Element element, String attr, String defaultValue) {
        return element.hasAttribute(attr) ? element.getAttribute(attr) : defaultValue;
    }

    private String getTextContent(Element element) {
        return element.getTextContent() != null ? element.getTextContent().trim() : null;
    }

    private TestResponse mapToTestResponse(TestIssue test) {
        return TestResponse.builder()
                .id(test.getId())
                .projectId(test.getProjectId())
                .name(test.getName())
                .description(test.getDescription())
                .testType(test.getTestType())
                .status(test.getStatus())
                .labels(test.getLabels())
                .priority(test.getPriority())
                .ownerId(test.getOwnerId())
                .archived(test.getArchived())
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }

    private record JUnitTestCase(String name, String className, String status, String message, double time) {}

    private record JUnitParseResult(int total, int passed, int failed, int skipped,
            boolean allPassed, List<JUnitTestCase> testCases) {}
}