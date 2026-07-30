package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.CampaignCreationResponse;
import com.avionics_systems.test.entity.RequirementLink;
import com.avionics_systems.test.entity.TestExecution;
import com.avionics_systems.test.entity.TestIssue;
import com.avionics_systems.test.entity.VvoDefinition;
import com.avionics_systems.test.repository.RequirementLinkRepository;
import com.avionics_systems.test.repository.TestExecutionRepository;
import com.avionics_systems.test.repository.TestIssueRepository;
import com.avionics_systems.test.repository.TestPlanRepository;
import com.avionics_systems.test.repository.VvoDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignAutomationService {

    private final VvoDefinitionRepository vvoRepo;
    private final TestPlanRepository testPlanRepo;
    private final TestExecutionRepository testExecutionRepo;
    private final RequirementLinkRepository requirementLinkRepo;
    private final TestIssueRepository testIssueRepo;

    @Value("${app.defaults.campaign-execution-status:TODO}")
    private String defaultCampaignExecutionStatus;

    @Value("${app.defaults.campaign-approved-status:APPROVED}")
    private String approvedStatus;

    /**
     * Parse LTR CSV and create campaign.
     * <p>
     * The CSV format: ID Doors, VVO Summary, Applicability, Version, Fix Version, Priority
     * <p>
     * For each VVO line in CSV:
     *   For each Test linked to VVO (via requirement_link):
     *     If Test status == APPROVED (or first campaign) and
     *        No existing TestExecution for {test, applicability} in TestPlan:
     *       Create TestExecution with fields from Test + CSV
     */
    @Transactional
    public CampaignCreationResponse createCampaignFromCsv(UUID testPlanId, String csvContent) {
        // Validate TestPlan exists
        testPlanRepo.findById(testPlanId)
                .orElseThrow(() -> new RuntimeException("TestPlan not found: " + testPlanId));

        // Parse CSV
        String[] lines = csvContent.split("\n");
        if (lines.length < 2) {
            return CampaignCreationResponse.builder()
                    .success(false)
                    .errorMessage("CSV must have header + at least 1 row")
                    .build();
        }

        // Determine if this is the first campaign for this test plan
        // (if no executions exist yet, we relax the APPROVED status requirement)
        boolean isFirstCampaign = testExecutionRepo.findByTestPlanId(testPlanId).isEmpty();

        int executionsCreated = 0;
        int vvosProcessed = 0;
        List<String> logEntries = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 6) {
                errors.add("Line " + (i + 1) + ": insufficient columns (expected 6, got " + parts.length + ")");
                continue;
            }

            String idDoors = parts[0].trim();
            String vvoSummary = parts[1].trim();
            String applicability = parts[2].trim();
            String version = parts[3].trim();
            String fixVersion = parts[4].trim();
            String priority = parts[5].trim();

            // Find VVO by ID Doors
            Optional<VvoDefinition> vvoOpt = vvoRepo.findByIdDoors(idDoors);
            if (vvoOpt.isEmpty()) {
                errors.add("VVO with ID Doors '" + idDoors + "' not found");
                continue;
            }

            VvoDefinition vvo = vvoOpt.get();
            vvosProcessed++;

            // Find all tests linked to this VVO via RequirementLink
            // The VVO's issueKey is the requirementKey in RequirementLink
            List<RequirementLink> links = requirementLinkRepo.findByRequirementKey(vvo.getIssueKey());

            if (links.isEmpty()) {
                logEntries.add("VVO " + idDoors + " (" + vvo.getIssueKey() + "): no linked tests found");
                continue;
            }

            int testsLinked = 0;
            int testsSkippedStatus = 0;
            int testsSkippedDuplicate = 0;

            for (RequirementLink link : links) {
                UUID testId = link.getTestId();

                // Look up the TestIssue to get name and check status
                Optional<TestIssue> testOpt = testIssueRepo.findById(testId);
                if (testOpt.isEmpty()) {
                    errors.add("VVO " + idDoors + ": linked test " + testId + " not found in test_issue table");
                    continue;
                }

                TestIssue test = testOpt.get();

                // Check test status: must be APPROVED unless it is the first campaign
                if (!isFirstCampaign && !approvedStatus.equalsIgnoreCase(test.getStatus())) {
                    testsSkippedStatus++;
                    continue;
                }

                // Check no existing TestExecution for {testId, testEnv=applicability} in this TestPlan
                if (testExecutionRepo.existsByTestPlanIdAndTestIdAndTestEnv(testPlanId, testId, applicability)) {
                    testsSkippedDuplicate++;
                    continue;
                }

                // Create TestExecution
                TestExecution execution = TestExecution.builder()
                        .testPlanId(testPlanId)
                        .testId(testId)
                        .name(test.getName())
                        .description(test.getDescription())
                        .status(defaultCampaignExecutionStatus)
                        .testEnv(applicability)
                        .totalTests(1)
                        .passedTests(0)
                        .failedTests(0)
                        .blockedTests(0)
                        .notRunTests(1)
                        .build();

                testExecutionRepo.save(execution);
                executionsCreated++;
                testsLinked++;
            }

            logEntries.add("VVO " + idDoors + " (" + vvo.getIssueKey() + "): "
                    + testsLinked + " executions created, "
                    + testsSkippedStatus + " skipped (status not " + approvedStatus + "), "
                    + testsSkippedDuplicate + " skipped (duplicate), "
                    + "applicability=" + applicability + ", priority=" + priority);
        }

        log.info("Campaign creation from CSV: {} VVOs processed, {} executions created, {} errors",
                vvosProcessed, executionsCreated, errors.size());

        return CampaignCreationResponse.builder()
                .success(errors.isEmpty())
                .vvosProcessed(vvosProcessed)
                .executionsCreated(executionsCreated)
                .logEntries(logEntries)
                .errors(errors)
                .build();
    }
}
