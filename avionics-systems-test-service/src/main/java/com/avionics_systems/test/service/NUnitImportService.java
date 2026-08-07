package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.NUnitImportResponse;
import com.avionics_systems.test.dto.TestResponse;
import com.avionics_systems.test.entity.*;
import com.avionics_systems.test.repository.*;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NUnitImportService {

    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;
    private final TestSetRepository testSetRepository;

    @Transactional
    public NUnitImportResponse importNUnitXml(UUID projectId, String xmlContent, UUID testSetId) {
        log.info("Importing NUnit XML results for project: {}", projectId);

        List<String> errors = new ArrayList<>();
        List<TestResponse> createdTests = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        int total = 0;
        UUID batchId = UUID.randomUUID();

        try {
            Document document = parseXmlSecurely(xmlContent);
            Element root = document.getDocumentElement();
            String rootName = root.getTagName();

            boolean isV3 = "test-run".equals(rootName);
            boolean isV2 = "test-results".equals(rootName);

            if (!isV3 && !isV2) {
                throw new IllegalArgumentException(
                        "Unsupported NUnit XML format. Expected root element 'test-run' (v3) or 'test-results' (v2), got: " + rootName);
            }

            log.info("Detected NUnit {} format", isV3 ? "v3" : "v2");

            TestSet testSet = null;
            if (testSetId != null) {
                testSet = testSetRepository.findById(testSetId).orElse(null);
            }

            String suiteName = getAttribute(root, "name", "NUnit Run");

            TestExecution execution = TestExecution.builder()
                    .projectId(projectId)
                    .name("NUnit: " + suiteName)
                    .description("Auto-imported from NUnit " + (isV3 ? "v3" : "v2") + " XML results")
                    .status("RUNNING")
                    .testEnv("CI")
                    .startedAt(LocalDateTime.now())
                    .build();
            execution = executionRepository.save(execution);

            NodeList testCases = document.getElementsByTagName("test-case");
            for (int i = 0; i < testCases.getLength(); i++) {
                Element testCase = (Element) testCases.item(i);
                String name = getAttribute(testCase, "name", "Unknown");
                String resultAttr = getAttribute(testCase, "result", "Inconclusive");
                String duration = getAttribute(testCase, "duration", "0");

                String mappedStatus;
                if (isV3) {
                    switch (resultAttr) {
                        case "Passed" -> { mappedStatus = "PASSED"; passed++; }
                        case "Failed" -> { mappedStatus = "FAILED"; failed++; }
                        case "Skipped" -> { mappedStatus = "SKIPPED"; skipped++; }
                        default -> { mappedStatus = "SKIPPED"; skipped++; }
                    }
                } else {
                    switch (resultAttr) {
                        case "Success" -> { mappedStatus = "PASSED"; passed++; }
                        case "Failure", "Error" -> { mappedStatus = "FAILED"; failed++; }
                        case "Skipped" -> { mappedStatus = "SKIPPED"; skipped++; }
                        default -> { mappedStatus = "SKIPPED"; skipped++; }
                    }
                }
                total++;

                String failureMessage = null;
                NodeList failureElements = testCase.getElementsByTagName("failure");
                if (failureElements.getLength() > 0) {
                    Element failureEl = (Element) failureElements.item(0);
                    NodeList messageElements = failureEl.getElementsByTagName("message");
                    if (messageElements.getLength() > 0) {
                        failureMessage = getTextContent((Element) messageElements.item(0));
                    }
                }

                try {
                    TestIssue testIssue = findOrCreateTest(name, projectId);
                    createdTests.add(mapToTestResponse(testIssue));

                    StepResult stepResult = StepResult.builder()
                            .executionId(execution.getId())
                            .stepId(testIssue.getId())
                            .status(mappedStatus)
                            .actualResult(failureMessage)
                            .executedAt(LocalDateTime.now())
                            .build();
                    stepResultRepository.save(stepResult);
                } catch (Exception e) {
                    log.warn("Failed to process NUnit test case '{}': {}", name, e.getMessage());
                    errors.add("Test case '" + name + "': " + e.getMessage());
                }
            }

            execution.setTotalTests(total);
            execution.setPassedTests(passed);
            execution.setFailedTests(failed);
            execution.setNotRunTests(skipped);
            execution.setBlockedTests(0);
            execution.setStatus(failed > 0 ? "FAILED" : "PASSED");
            execution.setFinishedAt(LocalDateTime.now());

            if (testSet != null) {
                execution.setTestSetId(testSet.getId());
            }
            executionRepository.save(execution);

            log.info("NUnit import completed: {} total, {} passed, {} failed, {} skipped",
                    total, passed, failed, skipped);

            return NUnitImportResponse.builder()
                    .batchId(batchId)
                    .status(errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS")
                    .totalTests(total)
                    .passed(passed)
                    .failed(failed)
                    .skipped(skipped)
                    .message("NUnit import completed successfully")
                    .createdTests(createdTests)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse NUnit XML: {}", e.getMessage(), e);
            return NUnitImportResponse.builder()
                    .batchId(batchId)
                    .status("FAILED")
                    .totalTests(0)
                    .passed(0)
                    .failed(0)
                    .skipped(0)
                    .message("Failed to parse NUnit XML: " + e.getMessage())
                    .createdTests(List.of())
                    .build();
        }
    }

    private Document parseXmlSecurely(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    private TestIssue findOrCreateTest(String testName, UUID projectId) {
        return testIssueRepository.findByProjectIdAndName(projectId, testName)
                .orElseGet(() -> {
                    TestIssue newTest = TestIssue.builder()
                            .projectId(projectId)
                            .name(testName)
                            .description("Auto-created from NUnit import: " + testName)
                            .testType("AUTOMATED")
                            .labels(List.of("automated", "nunit-import"))
                            .build();
                    return testIssueRepository.save(newTest);
                });
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
}
