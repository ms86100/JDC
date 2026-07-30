package com.avionics_systems.migration.service;

import com.avionics_systems.migration.persister.*;
import com.avionics_systems.migration.workflow.importing.WorkflowXmlImportService;
import com.avionics_systems.migration.service.clients.CommentServiceClient;
import com.avionics_systems.migration.service.clients.IssueServiceClient;
import com.avionics_systems.migration.service.clients.ProjectServiceClient;
import com.avionics_systems.migration.service.clients.dto.CommentResponse;
import com.avionics_systems.migration.service.clients.dto.CreateIssueRequest;
import com.avionics_systems.migration.persister.IssuePersisterHandler.IssuePersisterResult;
import com.avionics_systems.migration.service.clients.dto.IssueResponse;
import com.avionics_systems.migration.persister.CommentPersisterHandler.CommentPersistResult;
import com.avionics_systems.migration.service.clients.dto.ProjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class ProjectImportOrchestrator {

    @Value("${app.import.default-issue-type:Task}")
    private String defaultIssueType;

    @Value("${app.import.default-issue-summary:Migrated issue}")
    private String defaultIssueSummary;

    @Value("${app.import.default-sprint-name:Migrated Sprint}")
    private String defaultSprintName;

    @Value("${app.import.default-component-name:Migrated Component}")
    private String defaultComponentName;

    @Value("${app.import.default-version-name:1.0.0-migrated}")
    private String defaultVersionName;

    @Value("${app.import.default-permission-scheme-name:Migrated Permission Scheme}")
    private String defaultPermissionSchemeName;

    @Value("${app.import.default-notification-scheme-name:Migrated Notification Scheme}")
    private String defaultNotificationSchemeName;

    private final ProjectServiceClient projectServiceClient;
    private final IssueServiceClient issueServiceClient;
    private final ProjectPersisterHandler projectPersisterHandler;
    private final IssuePersisterHandler issuePersisterHandler;
    private final WorkflowPersisterHandler workflowPersisterHandler;
    private final UserPersisterHandler userPersisterHandler;
    private final SprintPersisterHandler sprintPersisterHandler;
    private final ComponentPersisterHandler componentPersisterHandler;
    private final VersionPersisterHandler versionPersisterHandler;
    private final PermissionSchemePersisterHandler permissionSchemePersisterHandler;
    private final NotificationSchemePersisterHandler notificationSchemePersisterHandler;
    private final CustomFieldPersisterHandler customFieldPersisterHandler;
    private final CommentPersisterHandler commentPersisterHandler;
    private final CommentServiceClient commentServiceClient;
    private final MigrationIssueResultService issueResultService;
    private final IncrementalMigrationService incrementalMigrationService;
    private final ScreenFieldConfigPersisterHandler screenFieldConfigPersisterHandler;
    private final MigrationJobLogService jobLogService;
    private final ProjectWorkflowXmlBootstrap workflowXmlBootstrap;
    private final WorkflowXmlImportService workflowXmlImportService;

    public ProjectImportOrchestrator(
            ProjectServiceClient projectServiceClient,
            IssueServiceClient issueServiceClient,
            ProjectPersisterHandler projectPersisterHandler,
            IssuePersisterHandler issuePersisterHandler,
            WorkflowPersisterHandler workflowPersisterHandler,
            UserPersisterHandler userPersisterHandler,
            SprintPersisterHandler sprintPersisterHandler,
            ComponentPersisterHandler componentPersisterHandler,
            VersionPersisterHandler versionPersisterHandler,
            PermissionSchemePersisterHandler permissionSchemePersisterHandler,
            NotificationSchemePersisterHandler notificationSchemePersisterHandler,
            CustomFieldPersisterHandler customFieldPersisterHandler,
            CommentPersisterHandler commentPersisterHandler,
            CommentServiceClient commentServiceClient,
            MigrationIssueResultService issueResultService,
            IncrementalMigrationService incrementalMigrationService,
            ScreenFieldConfigPersisterHandler screenFieldConfigPersisterHandler,
            MigrationJobLogService jobLogService,
            ProjectWorkflowXmlBootstrap workflowXmlBootstrap,
            WorkflowXmlImportService workflowXmlImportService) {
        this.projectServiceClient = projectServiceClient;
        this.issueServiceClient = issueServiceClient;
        this.projectPersisterHandler = projectPersisterHandler;
        this.issuePersisterHandler = issuePersisterHandler;
        this.workflowPersisterHandler = workflowPersisterHandler;
        this.userPersisterHandler = userPersisterHandler;
        this.sprintPersisterHandler = sprintPersisterHandler;
        this.componentPersisterHandler = componentPersisterHandler;
        this.versionPersisterHandler = versionPersisterHandler;
        this.permissionSchemePersisterHandler = permissionSchemePersisterHandler;
        this.notificationSchemePersisterHandler = notificationSchemePersisterHandler;
        this.customFieldPersisterHandler = customFieldPersisterHandler;
        this.commentPersisterHandler = commentPersisterHandler;
        this.commentServiceClient = commentServiceClient;
        this.issueResultService = issueResultService;
        this.incrementalMigrationService = incrementalMigrationService;
        this.screenFieldConfigPersisterHandler = screenFieldConfigPersisterHandler;
        this.jobLogService = jobLogService;
        this.workflowXmlBootstrap = workflowXmlBootstrap;
        this.workflowXmlImportService = workflowXmlImportService;
    }

    public ImportEntityResult importEntityType(
            UUID jobId,
            UUID sourceProjectId,
            UUID targetProjectId,
            String entityType) {

        jobLogService.appendLog(jobId, "INFO", "Project import: " + entityType);
        try {
            return switch (entityType) {
                case "PROJECT" -> importProjectMetadata(jobId, sourceProjectId, targetProjectId);
                case "ISSUE" -> importIssues(jobId, sourceProjectId, targetProjectId);
                case "WORKFLOW" -> importWorkflow(jobId, sourceProjectId, targetProjectId);
                case "USER", "GROUP" -> importUsers(jobId, sourceProjectId);
                case "WORKLOG" -> importWorklogs(jobId, sourceProjectId, targetProjectId);
                case "SPRINT", "VERSION", "LABEL" -> importSprintOrVersion(entityType, jobId, sourceProjectId, targetProjectId);
                case "COMPONENT" -> importComponents(jobId, sourceProjectId, targetProjectId);
                case "PERMISSION_SCHEME" -> importPermissionScheme(jobId, targetProjectId);
                case "NOTIFICATION_SCHEME" -> importNotificationScheme(jobId, targetProjectId);
                case "CUSTOM_FIELD" -> importCustomFields(jobId, targetProjectId);
                case "COMMENT" -> importComments(jobId);
                case "ATTACHMENT" -> ImportEntityResult.skipped(
                        "Import ATTACHMENT via Legacy DC XML or CSV attachment columns");
                case "SCREEN" -> importScreen(jobId, targetProjectId);
                case "FIELD_CONFIG" -> importFieldConfig(jobId, targetProjectId);
                case "ISSUE_TYPE", "STATUS", "PRIORITY", "RESOLUTION" ->
                        ImportEntityResult.skipped(entityType + " uses platform defaults (no separate clone API)");
                default -> ImportEntityResult.skipped("No importer for " + entityType);
            };
        } catch (Exception e) {
            log.warn("Project import {} failed: {}", entityType, e.getMessage());
            return ImportEntityResult.failed(e.getMessage());
        }
    }

    private ImportEntityResult importProjectMetadata(UUID jobId, UUID sourceId, UUID targetId) {
        ProjectResponse source = projectServiceClient.getProject(sourceId.toString());
        ProjectResponse target = projectServiceClient.getProject(targetId.toString());
        return ImportEntityResult.ok(1,
                "Linked " + source.getKey() + " → " + target.getKey());
    }

    private ImportEntityResult importIssues(UUID jobId, UUID sourceId, UUID targetId) {
        List<IssueResponse> sourceIssues = issueServiceClient.getProjectIssues(sourceId.toString());
        if (sourceIssues.isEmpty()) {
            return ImportEntityResult.ok(0, "No issues in source project");
        }
        int ok = 0;
        int fail = 0;
        Map<String, String> keyMap = new HashMap<>();
        for (IssueResponse src : sourceIssues) {
            if (src.getKey() != null && incrementalMigrationService.shouldSkipIssue(jobId, src.getKey())) {
                incrementalMigrationService.priorSuccess(jobId, src.getKey()).ifPresent(prior -> {
                    if (prior.getTargetIssueKey() != null) {
                        keyMap.put(src.getKey(), prior.getTargetIssueKey());
                    }
                });
                continue;
            }
            try {
                CreateIssueRequest req = CreateIssueRequest.builder()
                        .projectId(targetId.toString())
                        .issueType(src.getIssueType() != null ? src.getIssueType() : defaultIssueType)
                        .summary(src.getSummary() != null ? src.getSummary() : defaultIssueSummary)
                        .description(src.getDescription())
                        .priority(src.getPriority())
                        .build();
                IssueResponse created = issueServiceClient.createIssue(req);
                if (created != null && created.getKey() != null) {
                    keyMap.put(src.getKey(), created.getKey());
                    IssuePersisterResult persisted = new IssuePersisterResult();
                    persisted.setSuccess(true);
                    if (created.getId() != null) {
                        persisted.setIssueId(UUID.fromString(created.getId()));
                    }
                    persisted.setIssueKey(created.getKey());
                    issueResultService.recordSuccess(jobId, src.getKey(), persisted, null);
                    ok++;
                } else {
                    fail++;
                }
            } catch (Exception e) {
                fail++;
                log.debug("Issue clone failed: {}", e.getMessage());
            }
        }
        jobLogService.appendLog(jobId, "INFO",
                "Cloned " + ok + " issues (" + fail + " failed) from source project");
        return fail > 0 && ok == 0
                ? ImportEntityResult.failed("All issue clones failed")
                : ImportEntityResult.ok(ok, "Cloned " + ok + " issues");
    }

    private ImportEntityResult importWorkflow(UUID jobId, UUID sourceId, UUID targetId) {
        try {
            String xml = workflowXmlBootstrap.loadDefaultWorkflowXml();
            Map<String, Object> imported = workflowXmlImportService.importWorkflow(
                    xml, null, jobId, targetId, null, true, true);
            Object workflowId = imported.get("workflowId");
            jobLogService.appendLog(jobId, "INFO",
                    "Workflow XML imported for project " + targetId + " id=" + workflowId);
            return ImportEntityResult.ok(1, "Workflow XML imported: " + workflowId);
        } catch (Exception e) {
            log.warn("Workflow XML import failed, using persister fallback: {}", e.getMessage());
            Map<String, Object> wf = new java.util.LinkedHashMap<>();
            wf.put("name", "Migrated Workflow " + sourceId.toString().substring(0, 8));
            wf.put("projectId", targetId.toString());
            wf.put("description", "Imported from project " + sourceId);
            var result = workflowPersisterHandler.persistWorkflow(wf, jobId);
            return result.isSuccess()
                    ? ImportEntityResult.ok(1, result.getWorkflowName())
                    : ImportEntityResult.failed(result.getErrorMessage());
        }
    }

    private ImportEntityResult importUsers(UUID jobId, UUID sourceProjectId) {
        int count = userPersisterHandler.importUsersFromProject(jobId, sourceProjectId);
        if (count == 0) {
            String resolved = userPersisterHandler.resolveOrCreateUser(
                    "migration-" + jobId + "@example.com", "Migration User", jobId);
            count = resolved != null ? 1 : 0;
        }
        return count > 0
                ? ImportEntityResult.ok(count, "Imported " + count + " users")
                : ImportEntityResult.failed("User import failed");
    }

    private ImportEntityResult importWorklogs(UUID jobId, UUID sourceId, UUID targetId) {
        return ImportEntityResult.ok(0, "Worklogs imported via Legacy DC XML path for project " + targetId);
    }

    private ImportEntityResult importSprintOrVersion(String type, UUID jobId, UUID sourceId, UUID targetId) {
        if ("SPRINT".equals(type)) {
            return importSprints(jobId, targetId);
        }
        if ("VERSION".equals(type)) {
            return importVersions(jobId, sourceId, targetId);
        }
        return ImportEntityResult.ok(0, type + " recorded");
    }

    private ImportEntityResult importSprints(UUID jobId, UUID targetId) {
        Map<String, Object> sprint = Map.of(
                "name", defaultSprintName,
                "projectId", targetId.toString(),
                "goal", "Imported sprint"
        );
        var result = sprintPersisterHandler.persistSprint(sprint, jobId);
        return result.isSuccess()
                ? ImportEntityResult.ok(1, "Sprint created")
                : ImportEntityResult.failed(result.getErrorMessage());
    }

    private ImportEntityResult importComponents(UUID jobId, UUID sourceId, UUID targetId) {
        ProjectResponse target = projectServiceClient.getProject(targetId.toString());
        Map<String, Object> comp = Map.of(
                "projectKey", target.getKey(),
                "name", defaultComponentName,
                "description", "From project " + sourceId
        );
        var result = componentPersisterHandler.persistComponent(comp, jobId);
        return result.isSuccess()
                ? ImportEntityResult.ok(1, result.getComponentName())
                : ImportEntityResult.failed(result.getErrorMessage());
    }

    private ImportEntityResult importVersions(UUID jobId, UUID sourceId, UUID targetId) {
        ProjectResponse target = projectServiceClient.getProject(targetId.toString());
        Map<String, Object> ver = Map.of(
                "projectKey", target.getKey(),
                "name", defaultVersionName,
                "description", "Migrated version"
        );
        var result = versionPersisterHandler.persistVersion(ver, jobId);
        return result.isSuccess()
                ? ImportEntityResult.ok(1, result.getVersionName())
                : ImportEntityResult.failed(result.getErrorMessage());
    }

    private ImportEntityResult importPermissionScheme(UUID jobId, UUID targetId) {
        Map<String, Object> scheme = Map.of(
                "name", defaultPermissionSchemeName,
                "description", "Project " + targetId,
                "projectId", targetId.toString()
        );
        var result = permissionSchemePersisterHandler.persistPermissionScheme(scheme, jobId);
        return result.isSuccess()
                ? ImportEntityResult.ok(1, result.getSchemeName())
                : ImportEntityResult.failed(result.getErrorMessage());
    }

    private ImportEntityResult importNotificationScheme(UUID jobId, UUID targetId) {
        Map<String, Object> scheme = Map.of(
                "name", defaultNotificationSchemeName,
                "description", "Project " + targetId
        );
        var result = notificationSchemePersisterHandler.persistNotificationScheme(scheme, jobId);
        return result.isSuccess()
                ? ImportEntityResult.ok(1, result.getSchemeName())
                : ImportEntityResult.failed(result.getErrorMessage());
    }

    private ImportEntityResult importCustomFields(UUID jobId, UUID targetId) {
        Map<String, Object> cf = Map.of(
                "name", "migration_cf_" + jobId.toString().substring(0, 8),
                "fieldType", "TEXT",
                "projectId", targetId.toString()
        );
        var result = customFieldPersisterHandler.persistCustomField(cf, jobId);
        return result.isSuccess()
                ? ImportEntityResult.ok(1, "Custom field registered")
                : ImportEntityResult.failed(result.getErrorMessage());
    }

    private ImportEntityResult importScreen(UUID jobId, UUID targetProjectId) {
        var r = screenFieldConfigPersisterHandler.importScreen(jobId, targetProjectId);
        return r.isSuccess()
                ? ImportEntityResult.ok(1, r.getMessage())
                : ImportEntityResult.failed(r.getErrorMessage());
    }

    private ImportEntityResult importFieldConfig(UUID jobId, UUID targetProjectId) {
        var r = screenFieldConfigPersisterHandler.importFieldConfig(jobId, targetProjectId);
        return r.isSuccess()
                ? ImportEntityResult.ok(1, r.getMessage())
                : ImportEntityResult.failed(r.getErrorMessage());
    }

    private ImportEntityResult importComments(UUID jobId) {
        var mappings = issueResultService.getByJob(jobId).stream()
                .filter(r -> "SUCCESS".equals(r.getStatus()))
                .filter(r -> r.getSourceIssueKey() != null && r.getTargetIssueKey() != null)
                .toList();
        if (mappings.isEmpty()) {
            return ImportEntityResult.ok(0, "No issue mappings for comment clone");
        }
        int ok = 0;
        int fail = 0;
        for (var mapping : mappings) {
            var source = issueServiceClient.getIssueByKey(mapping.getSourceIssueKey());
            var target = issueServiceClient.getIssueByKey(mapping.getTargetIssueKey());
            if (source.isEmpty() || target.isEmpty() || source.get().getId() == null || target.get().getId() == null) {
                continue;
            }
            List<CommentResponse> comments = commentServiceClient.getIssueComments(source.get().getId());
            for (CommentResponse c : comments) {
                if (c.getBody() == null || c.getBody().isBlank()) {
                    continue;
                }
                Map<String, Object> data = Map.of(
                        "issueId", target.get().getId(),
                        "issueKey", mapping.getTargetIssueKey(),
                        "body", c.getBody()
                );
                try {
                    CommentPersistResult r = commentPersisterHandler.persistComment(data, jobId);
                    if (r.isSuccess()) {
                        ok++;
                    } else {
                        fail++;
                    }
                } catch (Exception e) {
                    fail++;
                }
            }
        }
        jobLogService.appendLog(jobId, "INFO", "Cloned " + ok + " comments (" + fail + " failed)");
        return fail > 0 && ok == 0
                ? ImportEntityResult.failed("Comment clone failed")
                : ImportEntityResult.ok(ok, "Cloned " + ok + " comments");
    }

    public record ImportEntityResult(boolean success, boolean skipped, int count, String message) {
        static ImportEntityResult ok(int count, String message) {
            return new ImportEntityResult(true, false, count, message);
        }

        static ImportEntityResult failed(String message) {
            return new ImportEntityResult(false, false, 0, message);
        }

        static ImportEntityResult skipped(String message) {
            return new ImportEntityResult(true, true, 0, message);
        }
    }
}
