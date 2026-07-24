package com.jira.test.service;

import com.jira.test.dto.ConsistencyCheckResult;
import com.jira.test.dto.ConsistencyCheckResult.ConsistencyItem;
import com.jira.test.entity.Component;
import com.jira.test.entity.RequirementLink;
import com.jira.test.entity.TestComponentMapping;
import com.jira.test.entity.TestExecution;
import com.jira.test.entity.TestIssue;
import com.jira.test.entity.VvoDefinition;
import com.jira.test.repository.ComponentRepository;
import com.jira.test.repository.RequirementLinkRepository;
import com.jira.test.repository.TestComponentMappingRepository;
import com.jira.test.repository.TestExecutionRepository;
import com.jira.test.repository.TestIssueRepository;
import com.jira.test.repository.VvoDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service that compares VVO and linked Test field values for consistency.
 * Checks Component, Applicability, and Supplier Applicability alignment.
 * Used by the consistency gadget endpoint.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VvoTestConsistencyService {

    private final VvoDefinitionRepository vvoRepo;
    private final RequirementLinkRepository requirementLinkRepo;
    private final TestIssueRepository testIssueRepo;
    private final TestComponentMappingRepository testComponentMappingRepo;
    private final TestExecutionRepository testExecutionRepo;
    private final ComponentRepository componentRepo;

    /**
     * Run a full consistency check for all VVOs in a project.
     * Compares VVO field values against linked test field values.
     */
    @Transactional(readOnly = true)
    public ConsistencyCheckResult checkConsistency(UUID projectId) {
        log.info("Running VVO-Test consistency check for project {}", projectId);

        List<VvoDefinition> vvos = vvoRepo.findByProjectIdAndArchivedFalse(projectId);
        List<ConsistencyItem> items = new ArrayList<>();
        Set<UUID> checkedTestIds = new HashSet<>();

        for (VvoDefinition vvo : vvos) {
            // Find all tests linked to this VVO
            List<RequirementLink> links = requirementLinkRepo.findByRequirementKey(vvo.getIssueKey());

            for (RequirementLink link : links) {
                UUID testId = link.getTestId();
                checkedTestIds.add(testId);

                Optional<TestIssue> testOpt = testIssueRepo.findById(testId);
                if (testOpt.isEmpty()) {
                    items.add(ConsistencyItem.builder()
                            .vvoId(vvo.getId())
                            .vvoIssueKey(vvo.getIssueKey())
                            .vvoSummary(vvo.getSummary())
                            .testId(testId)
                            .testName("(not found)")
                            .fieldName("existence")
                            .vvoValue(vvo.getIssueKey())
                            .testValue("MISSING")
                            .severity("ERROR")
                            .message("Linked test " + testId + " does not exist")
                            .build());
                    continue;
                }

                TestIssue test = testOpt.get();

                // 1. Component consistency check
                checkComponentConsistency(vvo, test, items);

                // 2. Applicability consistency check (compare VVO applicability with test execution environments)
                checkApplicabilityConsistency(vvo, test, items);

                // 3. Supplier Applicability consistency check
                checkSupplierApplicabilityConsistency(vvo, test, items);
            }
        }

        ConsistencyCheckResult result = ConsistencyCheckResult.builder()
                .projectId(projectId)
                .totalVvosChecked(vvos.size())
                .totalTestsChecked(checkedTestIds.size())
                .totalInconsistencies(items.size())
                .items(items)
                .generatedAt(LocalDateTime.now())
                .build();

        log.info("Consistency check completed for project {}: {} VVOs, {} tests, {} inconsistencies",
                projectId, vvos.size(), checkedTestIds.size(), items.size());

        return result;
    }

    /**
     * Run consistency check for a single VVO and all its linked tests.
     */
    @Transactional(readOnly = true)
    public ConsistencyCheckResult checkConsistencyForVvo(UUID vvoId) {
        VvoDefinition vvo = vvoRepo.findById(vvoId)
                .orElseThrow(() -> new RuntimeException("VVO not found: " + vvoId));

        List<ConsistencyItem> items = new ArrayList<>();
        List<RequirementLink> links = requirementLinkRepo.findByRequirementKey(vvo.getIssueKey());
        Set<UUID> checkedTestIds = new HashSet<>();

        for (RequirementLink link : links) {
            UUID testId = link.getTestId();
            checkedTestIds.add(testId);

            Optional<TestIssue> testOpt = testIssueRepo.findById(testId);
            if (testOpt.isEmpty()) {
                continue;
            }

            TestIssue test = testOpt.get();
            checkComponentConsistency(vvo, test, items);
            checkApplicabilityConsistency(vvo, test, items);
            checkSupplierApplicabilityConsistency(vvo, test, items);
        }

        return ConsistencyCheckResult.builder()
                .projectId(vvo.getProjectId())
                .totalVvosChecked(1)
                .totalTestsChecked(checkedTestIds.size())
                .totalInconsistencies(items.size())
                .items(items)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Check that the test's components match the VVO's componentIds.
     */
    private void checkComponentConsistency(VvoDefinition vvo, TestIssue test, List<ConsistencyItem> items) {
        List<UUID> vvoComponentIds = vvo.getComponentIds() != null ? vvo.getComponentIds() : List.of();
        List<UUID> testComponentIds = testComponentMappingRepo.findComponentIdsByTestId(test.getId());

        if (vvoComponentIds.isEmpty() && testComponentIds.isEmpty()) {
            // Both have no components - no inconsistency
            return;
        }

        Set<UUID> vvoSet = new HashSet<>(vvoComponentIds);
        Set<UUID> testSet = new HashSet<>(testComponentIds);

        // Find components in VVO but not in test
        Set<UUID> missingInTest = new HashSet<>(vvoSet);
        missingInTest.removeAll(testSet);

        // Find components in test but not in VVO
        Set<UUID> extraInTest = new HashSet<>(testSet);
        extraInTest.removeAll(vvoSet);

        if (!missingInTest.isEmpty()) {
            String vvoCompNames = resolveComponentNames(vvoComponentIds);
            String testCompNames = resolveComponentNames(testComponentIds);

            items.add(ConsistencyItem.builder()
                    .vvoId(vvo.getId())
                    .vvoIssueKey(vvo.getIssueKey())
                    .vvoSummary(vvo.getSummary())
                    .testId(test.getId())
                    .testName(test.getName())
                    .fieldName("Component")
                    .vvoValue(vvoCompNames)
                    .testValue(testCompNames)
                    .severity("WARNING")
                    .message("VVO has components not present in linked test: "
                            + resolveComponentNames(new ArrayList<>(missingInTest)))
                    .build());
        }

        if (!extraInTest.isEmpty()) {
            String vvoCompNames = resolveComponentNames(vvoComponentIds);
            String testCompNames = resolveComponentNames(testComponentIds);

            items.add(ConsistencyItem.builder()
                    .vvoId(vvo.getId())
                    .vvoIssueKey(vvo.getIssueKey())
                    .vvoSummary(vvo.getSummary())
                    .testId(test.getId())
                    .testName(test.getName())
                    .fieldName("Component")
                    .vvoValue(vvoCompNames)
                    .testValue(testCompNames)
                    .severity("WARNING")
                    .message("Test has components not present in linked VVO: "
                            + resolveComponentNames(new ArrayList<>(extraInTest)))
                    .build());
        }
    }

    /**
     * Check that test executions cover all VVO applicability values.
     * VVO.applicability is a list of environment targets.
     * TestExecution.testEnv should match these.
     */
    private void checkApplicabilityConsistency(VvoDefinition vvo, TestIssue test, List<ConsistencyItem> items) {
        List<String> vvoApplicability = vvo.getApplicability() != null ? vvo.getApplicability() : List.of();
        if (vvoApplicability.isEmpty()) {
            return;
        }

        // Find all executions for this test and get their testEnv values
        List<TestExecution> executions = testExecutionRepo.findByTestId(test.getId());
        Set<String> coveredEnvs = executions.stream()
                .map(TestExecution::getTestEnv)
                .filter(env -> env != null && !env.isEmpty())
                .collect(Collectors.toSet());

        // Check which VVO applicabilities are not covered by any execution
        List<String> uncoveredApplicabilities = vvoApplicability.stream()
                .filter(app -> !coveredEnvs.contains(app))
                .toList();

        if (!uncoveredApplicabilities.isEmpty()) {
            items.add(ConsistencyItem.builder()
                    .vvoId(vvo.getId())
                    .vvoIssueKey(vvo.getIssueKey())
                    .vvoSummary(vvo.getSummary())
                    .testId(test.getId())
                    .testName(test.getName())
                    .fieldName("Applicability")
                    .vvoValue(String.join(", ", vvoApplicability))
                    .testValue(coveredEnvs.isEmpty() ? "(no executions)" : String.join(", ", coveredEnvs))
                    .severity("WARNING")
                    .message("VVO applicability values not covered by test executions: "
                            + String.join(", ", uncoveredApplicabilities))
                    .build());
        }
    }

    /**
     * Check supplier applicability consistency.
     * VVO.supplierApplicability should be considered by linked tests.
     * Since tests don't directly track supplier applicability, we flag when
     * VVO has supplier applicability values that differ from its own applicability
     * (a data quality check).
     */
    private void checkSupplierApplicabilityConsistency(VvoDefinition vvo, TestIssue test, List<ConsistencyItem> items) {
        List<String> supplierApplicability = vvo.getSupplierApplicability() != null
                ? vvo.getSupplierApplicability() : List.of();
        List<String> applicability = vvo.getApplicability() != null
                ? vvo.getApplicability() : List.of();

        if (supplierApplicability.isEmpty()) {
            return;
        }

        // Flag supplier applicability values that are not in the VVO's own applicability
        // This indicates a potential data entry error
        List<String> supplierOnlyValues = supplierApplicability.stream()
                .filter(sa -> !applicability.contains(sa))
                .toList();

        if (!supplierOnlyValues.isEmpty()) {
            items.add(ConsistencyItem.builder()
                    .vvoId(vvo.getId())
                    .vvoIssueKey(vvo.getIssueKey())
                    .vvoSummary(vvo.getSummary())
                    .testId(test.getId())
                    .testName(test.getName())
                    .fieldName("Supplier Applicability")
                    .vvoValue(String.join(", ", supplierApplicability))
                    .testValue(String.join(", ", applicability))
                    .severity("WARNING")
                    .message("Supplier applicability values not in VVO applicability: "
                            + String.join(", ", supplierOnlyValues))
                    .build());
        }
    }

    /**
     * Resolve component UUIDs to human-readable names.
     */
    private String resolveComponentNames(List<UUID> componentIds) {
        if (componentIds == null || componentIds.isEmpty()) {
            return "(none)";
        }
        return componentIds.stream()
                .map(id -> componentRepo.findById(id)
                        .map(Component::getComponentName)
                        .orElse(id.toString()))
                .collect(Collectors.joining(", "));
    }
}
