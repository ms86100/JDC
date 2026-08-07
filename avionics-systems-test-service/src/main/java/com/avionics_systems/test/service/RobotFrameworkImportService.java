package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.RobotImportResponse;
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
public class RobotFrameworkImportService {

    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;
    private final TestSetRepository testSetRepository;

    @Transactional
    public RobotImportResponse importRobotXml(UUID projectId, String xmlContent, UUID testSetId) {
        log.info("Importing Robot Framework XML results for project: {}", projectId);

        List<String> errors = new ArrayList<>();
        List<TestResponse> createdTests = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        int total = 0;
        UUID batchId = UUID.randomUUID();

        try {
            Document document = parseXmlSecurely(xmlContent);

            TestSet testSet = null;
            if (testSetId != null) {
                testSet = testSetRepository.findById(testSetId).orElse(null);
            }

            Element robotElement = document.getDocumentElement();
            String generatedTime = getAttribute(robotElement, "generated", "");

            TestExecution execution = TestExecution.builder()
                    .projectId(projectId)
                    .name("Robot Framework Run")
                    .description("Auto-imported from Robot Framework XML results")
                    .status("RUNNING")
                    .testEnv("CI")
                    .startedAt(LocalDateTime.now())
                    .build();
            execution = executionRepository.save(execution);

            NodeList suiteElements = document.getElementsByTagName("suite");
            for (int i = 0; i < suiteElements.getLength(); i++) {
                Element suiteElement = (Element) suiteElements.item(i);
                NodeList testElements = suiteElement.getElementsByTagName("test");

                for (int j = 0; j < testElements.getLength(); j++) {
                    Element testElement = (Element) testElements.item(j);

                    // Only process direct children to avoid double-counting nested suites
                    if (!testElement.getParentNode().equals(suiteElement)) continue;

                    String testName = getAttribute(testElement, "name", "Unknown");

                    String statusValue = "FAIL";
                    NodeList statusElements = testElement.getElementsByTagName("status");
                    if (statusElements.getLength() > 0) {
                        Element statusEl = (Element) statusElements.item(0);
                        statusValue = getAttribute(statusEl, "status", "FAIL").toUpperCase();
                    }

                    String mappedStatus;
                    switch (statusValue) {
                        case "PASS" -> { mappedStatus = "PASSED"; passed++; }
                        case "FAIL" -> { mappedStatus = "FAILED"; failed++; }
                        default -> { mappedStatus = "SKIPPED"; skipped++; }
                    }
                    total++;

                    try {
                        TestIssue testIssue = findOrCreateTest(testName, projectId);
                        createdTests.add(mapToTestResponse(testIssue));

                        StepResult stepResult = StepResult.builder()
                                .executionId(execution.getId())
                                .stepId(testIssue.getId())
                                .status(mappedStatus)
                                .executedAt(LocalDateTime.now())
                                .build();
                        stepResultRepository.save(stepResult);
                    } catch (Exception e) {
                        log.warn("Failed to process Robot test '{}': {}", testName, e.getMessage());
                        errors.add("Test '" + testName + "': " + e.getMessage());
                    }
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

            log.info("Robot Framework import completed: {} total, {} passed, {} failed, {} skipped",
                    total, passed, failed, skipped);

            return RobotImportResponse.builder()
                    .batchId(batchId)
                    .status(errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS")
                    .totalTests(total)
                    .passed(passed)
                    .failed(failed)
                    .skipped(skipped)
                    .message("Robot Framework import completed successfully")
                    .createdTests(createdTests)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Robot Framework XML: {}", e.getMessage(), e);
            return RobotImportResponse.builder()
                    .batchId(batchId)
                    .status("FAILED")
                    .totalTests(0)
                    .passed(0)
                    .failed(0)
                    .skipped(0)
                    .message("Failed to parse Robot Framework XML: " + e.getMessage())
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
                            .description("Auto-created from Robot Framework import: " + testName)
                            .testType("AUTOMATED")
                            .labels(List.of("automated", "robot-import"))
                            .build();
                    return testIssueRepository.save(newTest);
                });
    }

    private String getAttribute(Element element, String attr, String defaultValue) {
        return element.hasAttribute(attr) ? element.getAttribute(attr) : defaultValue;
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
