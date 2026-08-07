package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.TestNgImportResponse;
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
public class TestNgImportService {

    private final TestIssueRepository testIssueRepository;
    private final TestExecutionRepository executionRepository;
    private final StepResultRepository stepResultRepository;
    private final TestSetRepository testSetRepository;

    @Transactional
    public TestNgImportResponse importTestNgXml(UUID projectId, String xmlContent, UUID testSetId) {
        log.info("Importing TestNG XML results for project: {}", projectId);

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

            Element suiteElement = document.getDocumentElement();
            String suiteName = getAttribute(suiteElement, "name", "TestNG Suite");

            TestExecution execution = TestExecution.builder()
                    .projectId(projectId)
                    .name("TestNG: " + suiteName)
                    .description("Auto-imported from TestNG XML results")
                    .status("RUNNING")
                    .testEnv("CI")
                    .startedAt(LocalDateTime.now())
                    .build();
            execution = executionRepository.save(execution);

            NodeList testElements = document.getElementsByTagName("test");
            for (int i = 0; i < testElements.getLength(); i++) {
                Element testElement = (Element) testElements.item(i);
                NodeList classElements = testElement.getElementsByTagName("class");

                for (int j = 0; j < classElements.getLength(); j++) {
                    Element classElement = (Element) classElements.item(j);
                    String className = getAttribute(classElement, "name", "Unknown");

                    NodeList methodElements = classElement.getElementsByTagName("test-method");
                    for (int k = 0; k < methodElements.getLength(); k++) {
                        Element methodElement = (Element) methodElements.item(k);

                        // Skip config methods
                        String isConfig = getAttribute(methodElement, "is-config", "false");
                        if ("true".equals(isConfig)) continue;

                        String methodName = getAttribute(methodElement, "name", "Unknown");
                        String statusAttr = getAttribute(methodElement, "status", "SKIP").toUpperCase();
                        String durationMs = getAttribute(methodElement, "duration-ms", "0");

                        String mappedStatus;
                        switch (statusAttr) {
                            case "PASS" -> { mappedStatus = "PASSED"; passed++; }
                            case "FAIL" -> { mappedStatus = "FAILED"; failed++; }
                            default -> { mappedStatus = "SKIPPED"; skipped++; }
                        }
                        total++;

                        String failureMessage = null;
                        NodeList exceptionElements = methodElement.getElementsByTagName("exception");
                        if (exceptionElements.getLength() > 0) {
                            Element exceptionEl = (Element) exceptionElements.item(0);
                            NodeList messageElements = exceptionEl.getElementsByTagName("message");
                            if (messageElements.getLength() > 0) {
                                failureMessage = getTextContent((Element) messageElements.item(0));
                            }
                        }

                        try {
                            String testName = className + "." + methodName;
                            TestIssue testIssue = findOrCreateTest(testName, className, projectId);
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
                            log.warn("Failed to process TestNG method '{}': {}", methodName, e.getMessage());
                            errors.add("Method '" + methodName + "': " + e.getMessage());
                        }
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

            log.info("TestNG import completed: {} total, {} passed, {} failed, {} skipped",
                    total, passed, failed, skipped);

            return TestNgImportResponse.builder()
                    .batchId(batchId)
                    .status(errors.isEmpty() ? "COMPLETED" : "COMPLETED_WITH_ERRORS")
                    .totalTests(total)
                    .passed(passed)
                    .failed(failed)
                    .skipped(skipped)
                    .message("TestNG import completed successfully")
                    .createdTests(createdTests)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse TestNG XML: {}", e.getMessage(), e);
            return TestNgImportResponse.builder()
                    .batchId(batchId)
                    .status("FAILED")
                    .totalTests(0)
                    .passed(0)
                    .failed(0)
                    .skipped(0)
                    .message("Failed to parse TestNG XML: " + e.getMessage())
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

    private TestIssue findOrCreateTest(String testName, String className, UUID projectId) {
        return testIssueRepository.findByProjectIdAndName(projectId, testName)
                .orElseGet(() -> {
                    TestIssue newTest = TestIssue.builder()
                            .projectId(projectId)
                            .name(testName)
                            .description("Auto-created from TestNG import: " + className)
                            .testType("AUTOMATED")
                            .labels(List.of("automated", "testng-import"))
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
