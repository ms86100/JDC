package com.avionics_systems.migration.jiradc;

import com.avionics_systems.migration.entity.EntityStatus;
import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.persister.*;
import com.avionics_systems.migration.repository.EntityStatusRepository;
import com.avionics_systems.migration.repository.MigrationJobRepository;
import com.avionics_systems.migration.service.MigrationProgressNotifier;
import com.avionics_systems.migration.service.MigrationService;
import com.avionics_systems.migration.service.clients.IssueServiceClient;
import com.avionics_systems.migration.websocket.MigrationWebSocketHandler;
import com.avionics_systems.migration.websocket.dto.JobProgressUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class JiraDcApiImportOrchestrator {

    private final ObjectMapper objectMapper;
    private final EntityStatusRepository entityStatusRepository;
    private final MigrationJobRepository migrationJobRepository;
    private final MigrationService migrationService;
    private final MigrationWebSocketHandler webSocketHandler;

    private final IssuePersisterHandler issuePersisterHandler;
    private final CommentPersisterHandler commentPersisterHandler;
    private final AttachmentPersisterHandler attachmentPersisterHandler;
    private final WorklogPersisterHandler worklogPersisterHandler;
    private final ComponentPersisterHandler componentPersisterHandler;
    private final VersionPersisterHandler versionPersisterHandler;
    private final IssueLinkPersisterHandler issueLinkPersisterHandler;
    private final LabelPersisterHandler labelPersisterHandler;
    private final com.avionics_systems.migration.service.field.FieldProvisioningService fieldProvisioningService;

    public ImportResult executeImport(UUID jobId, JiraDcConnectionConfig config, UUID userId, String targetProjectId) {
        String userIdStr = userId != null ? userId.toString() : "system";
        JiraDcRestClient client = new JiraDcRestClient(config, objectMapper);

        try {
            // Phase 1: Validate connection
            log.info("Job {}: Connecting to Jira DC at {}", jobId, config.getBaseUrl());
            sendProgress(jobId, userIdStr, 0, 0, 0, "CONNECTING");
            Map<String, Object> serverInfo = client.getServerInfo();
            log.info("Job {}: Connected to Jira DC v{}", jobId, serverInfo.get("version"));

            Map<String, Object> myself = client.getMyself();
            log.info("Job {}: Authenticated as {}", jobId, myself.get("name"));

            // Phase 2: Fetch metadata (field definitions for custom field name resolution)
            sendProgress(jobId, userIdStr, 0, 0, 0, "FETCHING_METADATA");
            List<Map<String, Object>> jiraFields = client.getFields();
            JiraDcEntityMapper.registerFieldNames(jiraFields);
            log.info("Job {}: Loaded {} field definitions", jobId, jiraFields.size());

            // Phase 2b: Pre-provision all custom field definitions
            sendProgress(jobId, userIdStr, 0, 0, 0, "PROVISIONING_FIELDS");
            int provisionedCount = 0;
            UUID systemUser = UUID.fromString("00000000-0000-0000-0000-000000000001");
            for (Map<String, Object> f : jiraFields) {
                if (!Boolean.TRUE.equals(f.get("custom"))) continue;
                String fieldId = f.get("id") != null ? f.get("id").toString() : null;
                String fieldName = f.get("name") != null ? f.get("name").toString() : fieldId;
                if (fieldId == null) continue;
                try {
                    fieldProvisioningService.provisionCustomField(fieldName, "TEXT", systemUser);
                    provisionedCount++;
                } catch (Exception e) {
                    log.debug("Field {} already provisioned or failed: {}", fieldId, e.getMessage());
                }
            }
            log.info("Job {}: Pre-provisioned {} custom field definitions", jobId, provisionedCount);

            // Phase 3: Build JQL and count total issues
            String jql = buildJql(config);
            log.info("Job {}: Searching with JQL: {}", jobId, jql);
            JiraDcRestClient.SearchResult initialSearch = client.searchIssues(jql, 0, 1, null);
            int totalIssues = initialSearch.total();
            log.info("Job {}: Found {} issues to import", jobId, totalIssues);

            if (totalIssues == 0) {
                return new ImportResult(0, 0, 0, 0, 0, Map.of());
            }

            migrationService.setTotalEntities(jobId, totalIssues);

            // Phase 4: Fetch and import components/versions per project
            int componentCount = 0;
            int versionCount = 0;
            Map<String, String> componentNameToId = new HashMap<>();
            Map<String, String> versionNameToId = new HashMap<>();
            if (config.getProjectKeys() != null && !config.getProjectKeys().isEmpty()) {
                for (String projectKey : config.getProjectKeys()) {
                    componentCount += importProjectComponents(client, projectKey, jobId, componentNameToId);
                    versionCount += importProjectVersions(client, projectKey, jobId, versionNameToId);
                }
            }

            // Phase 5: Paginate through all issues
            AtomicInteger processedCount = new AtomicInteger(0);
            AtomicInteger failedCount = new AtomicInteger(0);
            AtomicInteger commentCount = new AtomicInteger(0);
            AtomicInteger attachmentCount = new AtomicInteger(0);
            Map<String, String> issueKeyToTargetId = new ConcurrentHashMap<>();
            List<Map<String, String>> pendingIssueLinks = Collections.synchronizedList(new ArrayList<>());

            // First pass: Epics, then Stories/Tasks/Bugs, then Subtasks
            sendProgress(jobId, userIdStr, 0, totalIssues, 0, "IMPORTING_ISSUES");

            int startAt = 0;
            int maxResults = config.getMaxResults();
            List<Map<String, Object>> allIssues = new ArrayList<>();

            while (startAt < totalIssues) {
                JiraDcRestClient.SearchResult page = client.searchIssues(
                        jql, startAt, maxResults, "renderedFields");
                if (page.issues().isEmpty()) {
                    break;
                }
                allIssues.addAll(page.issues());
                startAt += page.issues().size();
                log.info("Job {}: Fetched {}/{} issues from Jira DC", jobId, allIssues.size(), totalIssues);
            }

            // Sort by hierarchy: Epics first, then non-subtasks, then subtasks
            List<Map<String, Object>> sortedIssues = sortByHierarchy(allIssues);

            // Process each issue
            for (Map<String, Object> jiraIssue : sortedIssues) {
                String issueKey = (String) jiraIssue.get("key");
                try {
                    Map<String, Object> issueData = JiraDcEntityMapper.toIssueData(jiraIssue);

                    if (targetProjectId != null && !targetProjectId.isBlank()) {
                        issueData.put("projectId", targetProjectId);
                    }

                    // Resolve component names to UUIDs
                    @SuppressWarnings("unchecked")
                    List<String> compNames = issueData.get("components") instanceof List<?> cl
                            ? ((List<Object>) cl).stream().map(Object::toString).toList() : null;
                    if (compNames != null && !compNames.isEmpty()) {
                        List<String> compIds = compNames.stream()
                                .map(n -> componentNameToId.getOrDefault(n, n))
                                .toList();
                        issueData.put("components", compIds);
                    }

                    // Resolve version names to UUIDs
                    @SuppressWarnings("unchecked")
                    List<String> fvNames = issueData.get("fixVersions") instanceof List<?> fl
                            ? ((List<Object>) fl).stream().map(Object::toString).toList() : null;
                    if (fvNames != null && !fvNames.isEmpty()) {
                        issueData.put("fixVersions", fvNames.stream()
                                .map(n -> versionNameToId.getOrDefault(n, n)).toList());
                    }
                    @SuppressWarnings("unchecked")
                    List<String> avNames = issueData.get("affectsVersions") instanceof List<?> al
                            ? ((List<Object>) al).stream().map(Object::toString).toList() : null;
                    if (avNames != null && !avNames.isEmpty()) {
                        issueData.put("affectsVersions", avNames.stream()
                                .map(n -> versionNameToId.getOrDefault(n, n)).toList());
                    }

                    EntityStatus status = EntityStatus.builder()
                            .jobId(jobId)
                            .entityType("Issue")
                            .entityKey(issueKey)
                            .sourceIdentifier(issueKey)
                            .status("PROCESSING")
                            .processingOrder(processedCount.get())
                            .build();
                    entityStatusRepository.save(status);

                    IssuePersisterHandler.IssuePersisterResult result =
                            issuePersisterHandler.persistIssue(issueData, jobId);

                    if (result.isSuccess()) {
                        String targetId = result.getIssueId() != null ? result.getIssueId().toString() : null;
                        issueKeyToTargetId.put(issueKey, targetId);
                        status.markCompleted(result.getIssueId());
                        processedCount.incrementAndGet();

                        // Collect issue links for later processing
                        Object links = issueData.get("issueLinks");
                        if (links instanceof List<?> linkList) {
                            for (Object linkObj : linkList) {
                                if (linkObj instanceof Map<?, ?> linkMap) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, String> link = (Map<String, String>) linkMap;
                                    link.put("sourceIssueKey", issueKey);
                                    pendingIssueLinks.add(link);
                                }
                            }
                        }

                        // Import comments for this issue
                        if (config.isIncludeComments()) {
                            commentCount.addAndGet(
                                    importIssueComments(client, issueKey, targetId, jobId));
                        }

                        // Import attachments for this issue
                        if (config.isIncludeAttachments()) {
                            attachmentCount.addAndGet(
                                    importIssueAttachments(client, jiraIssue, issueKey, targetId, jobId));
                        }

                        // Import worklogs for this issue
                        if (config.isIncludeWorklogs()) {
                            importIssueWorklogs(client, issueKey, targetId, jobId);
                        }
                    } else {
                        status.markFailed(result.getErrorCode(), result.getErrorMessage(), null);
                        failedCount.incrementAndGet();
                    }
                    entityStatusRepository.save(status);

                } catch (Exception e) {
                    log.error("Job {}: Failed to import issue {}: {}", jobId, issueKey, e.getMessage(), e);
                    failedCount.incrementAndGet();
                    recordEntityFailure(jobId, "Issue", issueKey, e);
                }

                migrationService.updateJobProgress(jobId, processedCount.get(), failedCount.get());

                int total = processedCount.get() + failedCount.get();
                sendProgress(jobId, userIdStr, total, totalIssues, failedCount.get(), "IMPORTING_ISSUES");
            }

            // Phase 6: Process pending issue links
            sendProgress(jobId, userIdStr, processedCount.get(), totalIssues, failedCount.get(), "LINKING_ISSUES");
            int linksProcessed = 0;
            for (Map<String, String> linkInfo : pendingIssueLinks) {
                try {
                    String sourceKey = linkInfo.get("sourceIssueKey");
                    String targetKey = linkInfo.get("targetIssueKey");
                    if (sourceKey != null && targetKey != null
                            && issueKeyToTargetId.containsKey(sourceKey)
                            && issueKeyToTargetId.containsKey(targetKey)) {
                        Map<String, Object> linkData = JiraDcEntityMapper.toIssueLinkData(new HashMap<>(linkInfo), sourceKey);
                        issueLinkPersisterHandler.persistIssueLink(linkData, jobId);
                        linksProcessed++;
                    }
                } catch (Exception e) {
                    log.warn("Job {}: Failed to create issue link: {}", jobId, e.getMessage());
                }
            }
            log.info("Job {}: Processed {} issue links", jobId, linksProcessed);

            Map<String, Object> resultMetadata = new HashMap<>();
            resultMetadata.put("source", "JIRA_DC_API");
            resultMetadata.put("jiraVersion", serverInfo.get("version"));
            resultMetadata.put("jiraBaseUrl", config.getBaseUrl());
            resultMetadata.put("totalIssuesInJira", totalIssues);
            resultMetadata.put("issuesImported", processedCount.get());
            resultMetadata.put("issuesFailed", failedCount.get());
            resultMetadata.put("commentsImported", commentCount.get());
            resultMetadata.put("attachmentsImported", attachmentCount.get());
            resultMetadata.put("componentsImported", componentCount);
            resultMetadata.put("versionsImported", versionCount);
            resultMetadata.put("issueLinksProcessed", linksProcessed);

            return new ImportResult(
                    processedCount.get(), failedCount.get(),
                    commentCount.get(), attachmentCount.get(),
                    totalIssues, resultMetadata);

        } catch (JiraDcRestClient.JiraDcApiException e) {
            log.error("Job {}: Jira DC API error: {}", jobId, e.getMessage(), e);
            throw e;
        }
    }

    // ========== Component / Version Import ==========

    private int importProjectComponents(JiraDcRestClient client, String projectKey, UUID jobId,
                                         Map<String, String> componentNameToId) {
        try {
            List<Map<String, Object>> components = client.getProjectComponents(projectKey);
            int count = 0;
            for (Map<String, Object> comp : components) {
                try {
                    Map<String, Object> data = JiraDcEntityMapper.toComponentData(comp, projectKey);
                    ComponentPersisterHandler.ComponentPersistResult result =
                            componentPersisterHandler.persistComponent(data, jobId);
                    if (result.isSuccess() && result.getComponentId() != null) {
                        String name = data.get("name") != null ? data.get("name").toString() : "";
                        componentNameToId.put(name, result.getComponentId().toString());
                    }
                    count++;
                } catch (Exception e) {
                    log.warn("Job {}: Failed to import component '{}': {}",
                            jobId, comp.get("name"), e.getMessage());
                }
            }
            log.info("Job {}: Imported {}/{} components for project {}", jobId, count, components.size(), projectKey);
            return count;
        } catch (Exception e) {
            log.warn("Job {}: Failed to fetch components for project {}: {}", jobId, projectKey, e.getMessage());
            return 0;
        }
    }

    private int importProjectVersions(JiraDcRestClient client, String projectKey, UUID jobId,
                                       Map<String, String> versionNameToId) {
        try {
            List<Map<String, Object>> versions = client.getProjectVersions(projectKey);
            int count = 0;
            for (Map<String, Object> ver : versions) {
                try {
                    Map<String, Object> data = JiraDcEntityMapper.toVersionData(ver, projectKey);
                    VersionPersisterHandler.VersionPersistResult result =
                            versionPersisterHandler.persistVersion(data, jobId);
                    if (result.isSuccess() && result.getVersionId() != null) {
                        String name = data.get("name") != null ? data.get("name").toString() : "";
                        versionNameToId.put(name, result.getVersionId().toString());
                    }
                    count++;
                } catch (Exception e) {
                    log.warn("Job {}: Failed to import version '{}': {}",
                            jobId, ver.get("name"), e.getMessage());
                }
            }
            log.info("Job {}: Imported {}/{} versions for project {}", jobId, count, versions.size(), projectKey);
            return count;
        } catch (Exception e) {
            log.warn("Job {}: Failed to fetch versions for project {}: {}", jobId, projectKey, e.getMessage());
            return 0;
        }
    }

    // ========== Comment / Attachment / Worklog Import ==========

    private int importIssueComments(JiraDcRestClient client, String issueKey,
                                     String targetIssueId, UUID jobId) {
        try {
            List<Map<String, Object>> comments = client.getIssueComments(issueKey);
            int count = 0;
            for (Map<String, Object> comment : comments) {
                try {
                    Map<String, Object> data = JiraDcEntityMapper.toCommentData(comment, issueKey, targetIssueId);
                    commentPersisterHandler.persistComment(data, jobId);
                    count++;
                } catch (Exception e) {
                    log.warn("Job {}: Failed to import comment on {}: {}", jobId, issueKey, e.getMessage());
                }
            }
            return count;
        } catch (Exception e) {
            log.warn("Job {}: Failed to fetch comments for {}: {}", jobId, issueKey, e.getMessage());
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private int importIssueAttachments(JiraDcRestClient client, Map<String, Object> jiraIssue,
                                        String issueKey, String targetIssueId, UUID jobId) {
        try {
            Map<String, Object> fields = (Map<String, Object>) jiraIssue.getOrDefault("fields", Map.of());
            Object attachmentObj = fields.get("attachment");
            if (!(attachmentObj instanceof List<?> attachments) || attachments.isEmpty()) {
                return 0;
            }

            int count = 0;
            for (Object att : attachments) {
                if (!(att instanceof Map<?, ?> attMap)) continue;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> attData = (Map<String, Object>) attMap;
                    Map<String, Object> metadata = JiraDcEntityMapper.toAttachmentMetadata(
                            attData, issueKey, targetIssueId);

                    String contentUrl = (String) metadata.remove("contentUrl");
                    byte[] content = new byte[0];
                    if (contentUrl != null && !contentUrl.isBlank()) {
                        try {
                            content = client.downloadBinary(contentUrl);
                        } catch (Exception e) {
                            log.warn("Job {}: Failed to download attachment '{}' for {}: {}",
                                    jobId, metadata.get("fileName"), issueKey, e.getMessage());
                        }
                    }

                    attachmentPersisterHandler.persistAttachment(metadata, content, jobId);
                    count++;
                } catch (Exception e) {
                    log.warn("Job {}: Failed to import attachment on {}: {}", jobId, issueKey, e.getMessage());
                }
            }
            return count;
        } catch (Exception e) {
            log.warn("Job {}: Failed to process attachments for {}: {}", jobId, issueKey, e.getMessage());
            return 0;
        }
    }

    private void importIssueWorklogs(JiraDcRestClient client, String issueKey,
                                      String targetIssueId, UUID jobId) {
        try {
            List<Map<String, Object>> worklogs = client.getIssueWorklogs(issueKey);
            for (Map<String, Object> worklog : worklogs) {
                try {
                    Map<String, Object> data = JiraDcEntityMapper.toWorklogData(worklog, issueKey, targetIssueId);
                    worklogPersisterHandler.persistWorklog(data, jobId);
                } catch (Exception e) {
                    log.warn("Job {}: Failed to import worklog on {}: {}", jobId, issueKey, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Job {}: Failed to fetch worklogs for {}: {}", jobId, issueKey, e.getMessage());
        }
    }

    // ========== JQL Builder ==========

    private String buildJql(JiraDcConnectionConfig config) {
        List<String> clauses = new ArrayList<>();

        if (config.getProjectKeys() != null && !config.getProjectKeys().isEmpty()) {
            if (config.getProjectKeys().size() == 1) {
                clauses.add("project = " + config.getProjectKeys().get(0));
            } else {
                clauses.add("project IN (" + String.join(",", config.getProjectKeys()) + ")");
            }
        }

        if (config.getJqlFilter() != null && !config.getJqlFilter().isBlank()) {
            clauses.add("(" + config.getJqlFilter() + ")");
        }

        String jql = clauses.isEmpty() ? "" : String.join(" AND ", clauses);
        return jql + " ORDER BY key ASC";
    }

    // ========== Issue Hierarchy Sorting ==========

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> sortByHierarchy(List<Map<String, Object>> issues) {
        List<Map<String, Object>> epics = new ArrayList<>();
        List<Map<String, Object>> regular = new ArrayList<>();
        List<Map<String, Object>> subtasks = new ArrayList<>();

        for (Map<String, Object> issue : issues) {
            Map<String, Object> fields = (Map<String, Object>) issue.getOrDefault("fields", Map.of());
            Map<String, Object> issueType = fields.get("issuetype") instanceof Map<?, ?> m
                    ? (Map<String, Object>) m : Map.of();

            String typeName = issueType.get("name") != null ? issueType.get("name").toString() : "";
            boolean isSubtask = Boolean.TRUE.equals(issueType.get("subtask"));

            if ("Epic".equalsIgnoreCase(typeName)) {
                epics.add(issue);
            } else if (isSubtask) {
                subtasks.add(issue);
            } else {
                regular.add(issue);
            }
        }

        List<Map<String, Object>> sorted = new ArrayList<>(epics.size() + regular.size() + subtasks.size());
        sorted.addAll(epics);
        sorted.addAll(regular);
        sorted.addAll(subtasks);
        return sorted;
    }

    // ========== Progress / Error Helpers ==========

    private void sendProgress(UUID jobId, String userId, int processed, int total, int failed, String stage) {
        try {
            JobProgressUpdate update = new JobProgressUpdate();
            update.setJobId(jobId.toString());
            update.setProgressPercentage(total > 0 ? (int) ((double) processed / total * 100) : 0);
            update.setProcessedEntities(processed);
            update.setTotalEntities(total);
            update.setFailedEntities(failed);
            update.setCurrentStage(stage);
            update.setCurrentEntityType("Issue");
            webSocketHandler.broadcastProgress(jobId.toString(), update);
        } catch (Exception e) {
            log.debug("Failed to send progress update: {}", e.getMessage());
        }
    }

    private void recordEntityFailure(UUID jobId, String entityType, String entityKey, Exception e) {
        try {
            EntityStatus status = EntityStatus.builder()
                    .jobId(jobId)
                    .entityType(entityType)
                    .entityKey(entityKey)
                    .sourceIdentifier(entityKey)
                    .status("FAILED")
                    .errorCode("JIRA_DC_IMPORT_ERROR")
                    .errorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error")
                    .build();
            entityStatusRepository.save(status);
        } catch (Exception ex) {
            log.warn("Failed to record entity failure for {}: {}", entityKey, ex.getMessage());
        }
    }

    // ========== Result Record ==========

    public record ImportResult(
            int processedCount,
            int failedCount,
            int commentCount,
            int attachmentCount,
            int totalIssues,
            Map<String, Object> metadata
    ) {}
}
