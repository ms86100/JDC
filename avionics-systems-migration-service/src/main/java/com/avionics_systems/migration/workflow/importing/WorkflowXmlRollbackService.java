package com.avionics_systems.migration.workflow.importing;

import com.avionics_systems.migration.entity.MigrationWorkflowImport;
import com.avionics_systems.migration.repository.MigrationWorkflowImportRepository;
import com.avionics_systems.migration.service.MigrationAuditPersistenceService;
import com.avionics_systems.migration.service.clients.WorkflowServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowXmlRollbackService {

    private final MigrationWorkflowImportRepository importRepository;
    private final WorkflowServiceClient workflowServiceClient;
    private final MigrationAuditPersistenceService auditService;

    @Transactional
    public Map<String, Object> rollback(UUID importId, UUID userId) {
        MigrationWorkflowImport record = importRepository.findById(importId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow import not found: " + importId));

        if (record.getRolledBackAt() != null) {
            return Map.of("status", "ALREADY_ROLLED_BACK", "importId", importId);
        }

        String targetId = record.getTargetWorkflowId();
        if (targetId != null && !targetId.startsWith("stub-")) {
            try {
                workflowServiceClient.deleteWorkflow(targetId);
            } catch (Exception e) {
                log.warn("Workflow delete during rollback failed: {}", e.getMessage());
            }
        }

        record.setRolledBackAt(LocalDateTime.now());
        record.setImportStatus("ROLLED_BACK");
        importRepository.save(record);

        auditService.log(record.getJobId(), "WORKFLOW_XML_ROLLBACK", "WORKFLOW",
                record.getWorkflowName(), userId, Map.of("importId", importId.toString(), "targetWorkflowId", targetId));

        return Map.of(
                "status", "ROLLED_BACK",
                "importId", importId,
                "workflowName", record.getWorkflowName(),
                "targetWorkflowId", targetId
        );
    }
}
