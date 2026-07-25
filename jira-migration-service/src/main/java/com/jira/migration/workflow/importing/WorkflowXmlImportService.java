package com.jira.migration.workflow.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationWorkflowImport;
import com.jira.migration.exception.MigrationException;
import com.jira.migration.service.clients.ServiceClientException;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.MigrationWorkflowImportRepository;
import com.jira.migration.service.MigrationAuditPersistenceService;
import com.jira.migration.service.clients.dto.WorkflowResponse;
import com.jira.migration.workflow.graph.WorkflowGraph;
import com.jira.migration.workflow.graph.WorkflowGraphBuilder;
import com.jira.migration.workflow.model.WorkflowDescriptorModel;
import com.jira.migration.workflow.model.WorkflowSchemeModel;
import com.jira.migration.workflow.parser.JiraDcWorkflowSchemeXmlParser;
import com.jira.migration.workflow.parser.JiraDcWorkflowXmlParser;
import com.jira.migration.workflow.simulation.WorkflowExecutionSimulator;
import com.jira.migration.workflow.validation.WorkflowXmlValidationReport;
import com.jira.migration.workflow.validation.WorkflowXmlValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowXmlImportService {

    @Value("${app.workflow.default-simulation-transitions:Submit for Review,Start Progress,Approve,Deploy,Complete}")
    private String defaultSimulationTransitionsStr;

    private final JiraDcWorkflowXmlParser workflowXmlParser;
    private final JiraDcWorkflowSchemeXmlParser schemeXmlParser;
    private final WorkflowXmlValidationService validationService;
    private final WorkflowGraphBuilder graphBuilder;
    private final WorkflowExecutionSimulator simulator;
    private final WorkflowImportBridge importBridge;
    private final WorkflowSchemeImportBridge schemeImportBridge;
    private final MigrationWorkflowImportRepository importRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final MigrationAuditPersistenceService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> simulateFromFile(String workflowXml, String startStepId, String transitionPath) {
        WorkflowDescriptorModel descriptor = workflowXmlParser.parse(workflowXml);
        WorkflowXmlValidationReport report = validationService.validate(descriptor);
        WorkflowGraph graph = graphBuilder.build(descriptor);
        List<String> path = transitionPath != null && !transitionPath.isBlank()
                ? Arrays.asList(transitionPath.split(","))
                : List.of();
        Map<String, Object> body = toValidateResponse(descriptor, report, null);
        body.put("simulation", simulator.simulate(descriptor, graph, startStepId, path));
        return body;
    }

    public Map<String, Object> validateOnly(String workflowXml, String schemeXml) {
        WorkflowDescriptorModel descriptor = workflowXmlParser.parse(workflowXml);
        WorkflowXmlValidationReport report = validationService.validate(descriptor);
        WorkflowSchemeModel scheme = schemeXml != null && !schemeXml.isBlank()
                ? schemeXmlParser.parse(schemeXml) : null;
        return toValidateResponse(descriptor, report, scheme);
    }

    @Transactional
    public Map<String, Object> importWorkflow(String workflowXml, String schemeXml, UUID jobId, UUID projectId,
                                              UUID userId, boolean stubDownstream, boolean makeDefault) {
        WorkflowDescriptorModel descriptor = workflowXmlParser.parse(workflowXml);
        WorkflowXmlValidationReport report = validationService.validate(descriptor);
        if (!report.isValid()) {
            throw new MigrationException("Workflow XML validation failed: " + report.getErrors());
        }

        WorkflowGraph graph = graphBuilder.build(descriptor);
        Map<String, Object> simulation = simulator.simulate(descriptor, graph, "1",
                Arrays.asList(defaultSimulationTransitionsStr.split(",")));

        WorkflowSchemeModel scheme = schemeXml != null && !schemeXml.isBlank()
                ? schemeXmlParser.parse(schemeXml) : null;

        MigrationWorkflowImport record = MigrationWorkflowImport.builder()
                .jobId(jobId)
                .workflowName(descriptor.getName())
                .schemeName(scheme != null ? scheme.getName() : null)
                .sourceFormat("WORKFLOW_DESCRIPTOR")
                .importStatus("IN_PROGRESS")
                .descriptorJson(toMap(descriptor))
                .schemeJson(scheme != null ? toMap(scheme) : null)
                .graphJson(report.getGraphJson())
                .validationReport(toMap(report))
                .simulationTrace(simulation)
                .unsupportedFeatures(Map.of("items", report.getUnsupportedFeatures()))
                .snapshotBefore(Map.of("capturedAt", LocalDateTime.now().toString()))
                .build();
        record = importRepository.save(record);

        String targetWorkflowId = null;
        try {
            if (!stubDownstream) {
                WorkflowResponse created = importBridge.pushToWorkflowService(descriptor, graph, projectId, makeDefault);
                targetWorkflowId = created.getId() != null ? created.getId().toString() : null;
            } else {
                targetWorkflowId = "stub-" + record.getId();
            }

            Map<String, Object> schemeResult = null;
            if (scheme != null) {
                UUID wfUuid = null;
                try {
                    if (targetWorkflowId != null && !targetWorkflowId.startsWith("stub-")) {
                        wfUuid = UUID.fromString(targetWorkflowId);
                    }
                } catch (IllegalArgumentException ignored) {
                }
                schemeResult = schemeImportBridge.importScheme(scheme, wfUuid, stubDownstream);
                record.setTargetSchemeId(schemeResult.get("targetSchemeId") != null
                        ? String.valueOf(schemeResult.get("targetSchemeId")) : null);
            }
            record.setTargetWorkflowId(targetWorkflowId);
            record.setImportStatus("COMPLETED");
            record.setCompletedAt(LocalDateTime.now());
            importRepository.save(record);

            updateEntityStatus(jobId, descriptor.getName(), targetWorkflowId, true);
            auditService.log(jobId, "WORKFLOW_XML_IMPORT", "WORKFLOW", descriptor.getName(),
                    userId, Map.of("workflowId", targetWorkflowId, "stub", stubDownstream));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("importId", record.getId());
            result.put("workflowName", descriptor.getName());
            result.put("targetWorkflowId", targetWorkflowId);
            result.put("stubDownstream", stubDownstream);
            result.put("validation", report);
            result.put("graph", report.getGraphJson());
            result.put("simulation", simulation);
            result.put("unsupportedFeatures", report.getUnsupportedFeatures());
            if (scheme != null) {
                result.put("schemeImport", schemeResult);
            }
            return result;
        } catch (ServiceClientException e) {
            record.setImportStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            importRepository.save(record);
            updateEntityStatus(jobId, descriptor.getName(), null, false);
            throw new MigrationException("Workflow service import failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> toValidateResponse(WorkflowDescriptorModel descriptor,
                                                   WorkflowXmlValidationReport report,
                                                   WorkflowSchemeModel scheme) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("valid", report.isValid());
        body.put("workflowName", descriptor.getName());
        body.put("errors", report.getErrors());
        body.put("warnings", report.getWarnings());
        body.put("unsupportedFeatures", report.getUnsupportedFeatures());
        body.put("executionRisks", report.getExecutionRisks());
        body.put("compatibilityMatrix", report.getCompatibilityMatrix());
        body.put("graph", report.getGraphJson());
        body.put("stepCount", report.getStepCount());
        body.put("transitionCount", report.getTransitionCount());
        if (scheme != null) {
            body.put("scheme", scheme);
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) {
        return objectMapper.convertValue(value, Map.class);
    }

    private void updateEntityStatus(UUID jobId, String sourceKey, String targetId, boolean success) {
        if (jobId == null) {
            return;
        }
        EntityStatus status = entityStatusRepository
                .findByJobIdAndEntityTypeAndSourceIdentifier(jobId, "WORKFLOW", sourceKey)
                .orElse(EntityStatus.builder()
                        .jobId(jobId)
                        .entityType("WORKFLOW")
                        .sourceIdentifier(sourceKey)
                        .build());
        status.setTargetId(targetId);
        status.setEntityKey(sourceKey);
        status.setStatus(success ? "COMPLETED" : "FAILED");
        status.setProcessedAt(LocalDateTime.now());
        entityStatusRepository.save(status);
    }
}
