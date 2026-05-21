package com.jira.migration.service;

import com.jira.migration.dto.ValidationResult;
import com.jira.migration.entity.MigrationValidationResult;
import com.jira.migration.parser.ValidationEngine;
import com.jira.migration.repository.MigrationValidationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Dry-run validation with persisted per-row results (P4-02).
 */
@Service
@RequiredArgsConstructor
public class DryRunValidationService {

    private final ValidationEngine validationEngine;
    private final MigrationValidationResultRepository validationResultRepository;

    @Transactional
    public ValidationResult validateAndPersist(
            UUID jobId,
            UUID sessionId,
            String entityType,
            List<Map<String, String>> rows,
            boolean clearPrevious) {

        if (clearPrevious && jobId != null) {
            validationResultRepository.deleteByJobId(jobId);
        }

        List<ValidationResult.ValidationError> allErrors = new ArrayList<>();
        List<ValidationResult.ValidationWarning> allWarnings = new ArrayList<>();

        int rowNum = 2;
        for (Map<String, String> row : rows) {
            ValidationResult rowResult = validationEngine.validateRow(row, entityType, rowNum);
            allErrors.addAll(rowResult.getErrors());
            allWarnings.addAll(rowResult.getWarnings());

            persistRowResults(jobId, sessionId, entityType, row, rowNum, rowResult);
            rowNum++;
        }

        return ValidationResult.builder()
                .valid(allErrors.isEmpty())
                .errors(allErrors)
                .warnings(allWarnings)
                .build();
    }

    private void persistRowResults(
            UUID jobId,
            UUID sessionId,
            String entityType,
            Map<String, String> row,
            int rowNum,
            ValidationResult rowResult) {

        Map<String, Object> rowData = new HashMap<>(row);
        String entityKey = row.getOrDefault("issue_key", row.get("project_key"));

        for (ValidationResult.ValidationError e : rowResult.getErrors()) {
            validationResultRepository.save(MigrationValidationResult.builder()
                    .jobId(jobId)
                    .wizardSessionId(sessionId)
                    .rowNumber(rowNum)
                    .entityType(entityType)
                    .entityKey(entityKey)
                    .severity("ERROR")
                    .fieldName(e.getField())
                    .errorCode(e.getErrorCode())
                    .message(e.getMessage())
                    .rowData(rowData)
                    .build());
        }
        for (ValidationResult.ValidationWarning w : rowResult.getWarnings()) {
            validationResultRepository.save(MigrationValidationResult.builder()
                    .jobId(jobId)
                    .wizardSessionId(sessionId)
                    .rowNumber(rowNum)
                    .entityType(entityType)
                    .entityKey(entityKey)
                    .severity("WARNING")
                    .fieldName(w.getField())
                    .errorCode(w.getWarningCode())
                    .message(w.getMessage())
                    .rowData(rowData)
                    .build());
        }
    }
}
