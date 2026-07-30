package com.avionics_systems.test.service;

import com.avionics_systems.test.dto.DoorsImportResponse;
import com.avionics_systems.test.entity.VvoDefinition;
import com.avionics_systems.test.repository.VvoDefinitionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoorsIntegrationService {

    private final VvoDefinitionRepository vvoRepo;

    /**
     * Export VVOs to CSV format for DOORS import.
     * Only includes VVOs in RELEASED or CANCELLED status within the given baseline.
     */
    @Transactional(readOnly = true)
    public String exportVvosForDoors(UUID projectId, UUID fixVersionId) {
        List<VvoDefinition> vvos = vvoRepo.findByFixVersionId(fixVersionId).stream()
                .filter(v -> v.getProjectId().equals(projectId))
                .filter(v -> List.of("RELEASED", "CANCELLED").contains(v.getStatus()))
                .sorted(Comparator.comparing(VvoDefinition::getIssueKey))
                .toList();

        StringBuilder csv = new StringBuilder();
        csv.append("Issue key,Summary,Status,VVO Version,Applicability,Supplier Applicability,")
                .append("Operational Conditions,Expected Results\n");

        for (VvoDefinition vvo : vvos) {
            csv.append(escapeCsv(vvo.getIssueKey())).append(",");
            csv.append(escapeCsv(vvo.getSummary())).append(",");
            csv.append(escapeCsv(vvo.getStatus())).append(",");
            csv.append(vvo.getVvoVersion()).append(",");
            csv.append(escapeCsv(joinList(vvo.getApplicability()))).append(",");
            csv.append(escapeCsv(joinList(vvo.getSupplierApplicability()))).append(",");
            csv.append(escapeCsv(vvo.getOperationalConditions() != null
                    ? vvo.getOperationalConditions() : "")).append(",");
            csv.append(escapeCsv(vvo.getExpectedResults() != null
                    ? vvo.getExpectedResults() : ""));
            csv.append("\n");
        }

        log.info("Exported {} VVOs to CSV for DOORS (project: {}, baseline: {})",
                vvos.size(), projectId, fixVersionId);
        return csv.toString();
    }

    /**
     * Import DOORS IDs from CSV into VVOs.
     * Expected CSV format: "Issue key,Summary,Custom field (ID Doors)"
     *
     * Validations:
     * 1. CSV header matches expected format
     * 2. ID Doors is not empty for any row
     * 3. ID Doors is not duplicated within the CSV
     * 4. Issue key exists in the system
     * 5. Issue key belongs to the specified project
     * 6. If an existing ID Doors differs from the CSV value, reject the row
     */
    @Transactional
    public DoorsImportResponse importDoorsIds(UUID projectId, String csvContent) {
        String[] lines = csvContent.split("\n");
        if (lines.length == 0) {
            return DoorsImportResponse.builder()
                    .success(false)
                    .errorMessage("Empty CSV file")
                    .build();
        }

        // Validate header
        String header = lines[0].trim();
        if (!"Issue key,Summary,Custom field (ID Doors)".equals(header)) {
            return DoorsImportResponse.builder()
                    .success(false)
                    .errorMessage("Invalid CSV header. Expected: 'Issue key,Summary,Custom field (ID Doors)'")
                    .build();
        }

        // Parse rows
        List<DoorsImportRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", 3);
            if (parts.length < 3) {
                return DoorsImportResponse.builder()
                        .success(false)
                        .errorMessage("Line " + (i + 1) + ": insufficient columns")
                        .build();
            }
            rows.add(new DoorsImportRow(parts[0].trim(), parts[1].trim(), parts[2].trim()));
        }

        // Validation pass 1: Check for empty or duplicate DOORS IDs
        Set<String> seenIds = new HashSet<>();
        for (DoorsImportRow row : rows) {
            if (row.getIdDoors().isEmpty()) {
                return DoorsImportResponse.builder()
                        .success(false)
                        .errorMessage("ID Doors is empty for issue " + row.getIssueKey())
                        .build();
            }
            if (!seenIds.add(row.getIdDoors())) {
                return DoorsImportResponse.builder()
                        .success(false)
                        .errorMessage("Duplicate ID Doors: " + row.getIdDoors())
                        .build();
            }
        }

        // Validation pass 2: Check each row against the database
        List<String> errors = new ArrayList<>();
        int updated = 0;

        for (DoorsImportRow row : rows) {
            Optional<VvoDefinition> vvoOpt = vvoRepo.findByIssueKey(row.getIssueKey());
            if (vvoOpt.isEmpty()) {
                errors.add("Issue " + row.getIssueKey() + " not found");
                continue;
            }

            VvoDefinition vvo = vvoOpt.get();
            if (!vvo.getProjectId().equals(projectId)) {
                errors.add("Issue " + row.getIssueKey() + " belongs to different project");
                continue;
            }

            if (vvo.getIdDoors() != null && !vvo.getIdDoors().isEmpty()
                    && !vvo.getIdDoors().equals(row.getIdDoors())) {
                errors.add("Issue " + row.getIssueKey() + " already has ID Doors '"
                        + vvo.getIdDoors() + "' which differs from CSV value '"
                        + row.getIdDoors() + "'");
                continue;
            }

            vvo.setIdDoors(row.getIdDoors());
            vvoRepo.save(vvo);
            updated++;
        }

        if (!errors.isEmpty()) {
            return DoorsImportResponse.builder()
                    .success(false)
                    .errorMessage("Import failed with errors")
                    .errors(errors)
                    .updatedCount(0)
                    .build();
        }

        log.info("DOORS import: {} VVOs updated with ID Doors in project {}", updated, projectId);
        return DoorsImportResponse.builder()
                .success(true)
                .updatedCount(updated)
                .build();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String joinList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return String.join(";", items);
    }

    @Data
    @AllArgsConstructor
    private static class DoorsImportRow {
        private String issueKey;
        private String summary;
        private String idDoors;
    }
}
