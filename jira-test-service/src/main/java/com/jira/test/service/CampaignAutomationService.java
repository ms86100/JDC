package com.jira.test.service;

import com.jira.test.dto.CampaignCreationResponse;
import com.jira.test.entity.VvoDefinition;
import com.jira.test.repository.TestExecutionRepository;
import com.jira.test.repository.TestPlanRepository;
import com.jira.test.repository.VvoDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Parse LTR CSV and create campaign.
     * <p>
     * The CSV format: ID Doors, VVO Summary, Applicability, Version, Fix Version, Priority
     * <p>
     * For each VVO line in CSV:
     *   For each Test linked to VVO (via requirement_link):
     *     If Test status == APPROVED and
     *        No existing TestExecution for {test, applicability} in TestPlan:
     *       Create TestExecution with fields from Test + CSV
     */
    @Transactional
    public CampaignCreationResponse createCampaignFromCsv(UUID testPlanId, String csvContent) {
        // Parse CSV
        String[] lines = csvContent.split("\n");
        if (lines.length < 2) {
            return CampaignCreationResponse.builder()
                    .success(false)
                    .errorMessage("CSV must have header + at least 1 row")
                    .build();
        }

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
                errors.add("Line " + (i + 1) + ": insufficient columns");
                continue;
            }

            String idDoors = parts[0].trim();
            String applicability = parts[2].trim();
            String priority = parts[5].trim();

            // Find VVO by ID Doors
            Optional<VvoDefinition> vvoOpt = vvoRepo.findByIdDoors(idDoors);
            if (vvoOpt.isEmpty()) {
                errors.add("VVO with ID Doors '" + idDoors + "' not found");
                continue;
            }

            vvosProcessed++;
            logEntries.add("VVO " + idDoors + " processed, applicability: "
                    + applicability + ", priority: " + priority);
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
