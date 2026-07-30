package com.avionics_systems.migration.service;

import com.avionics_systems.migration.entity.MigrationValidationResult;
import com.avionics_systems.migration.repository.MigrationValidationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CSV export of persisted dry-run validation rows (P4-12).
 */
@Service
@RequiredArgsConstructor
public class ValidationReportService {

    private final MigrationValidationResultRepository validationResultRepository;

    @Transactional(readOnly = true)
    public String buildValidationReportCsv(UUID jobId, UUID sessionId) {
        List<MigrationValidationResult> rows = jobId != null
                ? validationResultRepository.findByJobIdOrderByRowNumberAsc(jobId)
                : validationResultRepository.findByWizardSessionIdOrderByRowNumberAsc(sessionId);

        StringBuilder csv = new StringBuilder();
        csv.append("row_number,entity_type,entity_key,severity,field_name,error_code,message\n");
        for (MigrationValidationResult r : rows) {
            csv.append(r.getRowNumber()).append(',')
                    .append(escape(r.getEntityType())).append(',')
                    .append(escape(r.getEntityKey())).append(',')
                    .append(escape(r.getSeverity())).append(',')
                    .append(escape(r.getFieldName())).append(',')
                    .append(escape(r.getErrorCode())).append(',')
                    .append(escape(r.getMessage()))
                    .append('\n');
        }
        return csv.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
