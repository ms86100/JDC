package com.avionics_systems.workflow.engine.script;

import com.avionics_systems.workflow.engine.WorkflowIntegrationClient;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import java.util.HashMap;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j

public class JdcApi {

    private final WorkflowIntegrationClient client;
    private final Map<String, Object> context;
    private String lastError = null;
    private ScriptTracer tracer;
    private MutationBuffer mutationBuffer;

    @HostAccess.Export public final IssueApi issue;
    @HostAccess.Export public final ProjectApi project;
    @HostAccess.Export public final UserApi user;
    @HostAccess.Export public final WorkflowApi workflow;
    @HostAccess.Export public final SearchApi search;
    @HostAccess.Export public final LogApi log;

    public JdcApi(WorkflowIntegrationClient client, Map<String, Object> context) {
        this.client = client;
        this.context = context;
        this.issue = new IssueApi();
        this.project = new ProjectApi();
        this.user = new UserApi();
        this.workflow = new WorkflowApi();
        this.search = new SearchApi();
        this.log = new LogApi();
    }

    @HostAccess.Export
    public String getLastError() {
        return lastError;
    }

    @HostAccess.Export
    public void clearLastError() {
        lastError = null;
    }

    // === Backward-compatible top-level methods ===

    @HostAccess.Export
    public Map<String, Object> getIssue() {
        return issue.getCurrentIssue();
    }

    @HostAccess.Export
    public Object getIssueField(String fieldName) {
        return issue.getFieldValue(fieldName);
    }

    @HostAccess.Export
    public void setIssueField(String fieldName, Object value) {
        issue.setFieldValue(fieldName, value);
    }

    @HostAccess.Export
    public Map<String, Object> getCurrentUser() {
        return user.getCurrentUser();
    }

    @HostAccess.Export
    public Map<String, Object> getUser(String userId) {
        return user.getUser(userId);
    }

    @HostAccess.Export
    public Map<String, Object> getProject() {
        return project.getCurrentProject();
    }

    @HostAccess.Export
    public void addComment(String content) {
        issue.addComment(content);
    }

    @HostAccess.Export
    public boolean hasPermission(String permission) {
        return user.hasPermission(permission);
    }

    @HostAccess.Export
    public List<Map<String, Object>> getLinkedIssues() {
        try {
            Object id = context.get("issueId");
            if (id == null) return List.of();
            return client.fetchLinkedIssuesForWorkflow(UUID.fromString(id.toString()));
        } catch (Exception e) { return List.of(); }
    }

    @HostAccess.Export
    public Map<String, Object> getScreenInput() {
        return castMap(context.getOrDefault("screenInput", Map.of()));
    }

    @HostAccess.Export
    public int getAttachmentCount() {
        try {
            Object id = context.get("issueId");
            if (id == null) return 0;
            return client.countAttachments(UUID.fromString(id.toString()));
        } catch (Exception e) { return 0; }
    }

    @HostAccess.Export
    public String getTransitionName() {
        return str(context.get("transitionName"));
    }

    @HostAccess.Export
    public String getFromStatusId() {
        return str(context.get("fromStatusId"));
    }

    @HostAccess.Export
    public String getToStatusId() {
        return str(context.get("toStatusId"));
    }

    // === Sub-API: jdc.issue ===

    public class IssueApi {

        @HostAccess.Export
        public Map<String, Object> getCurrentIssue() {
            try {
                Object id = context.get("issueId");
                if (id == null) return Map.of();
                return client.fetchIssue(UUID.fromString(id.toString()));
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public Object getFieldValue(String fieldName) {
            try {
                long _t = System.currentTimeMillis();
                Map<String, Object> issueData = castMap(context.getOrDefault("issueData", Map.of()));
                Object result = issueData.get(fieldName);
                trace("issue", "getFieldValue", _t);
                return result;
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public boolean setFieldValue(String fieldName, Object value) {
            try {
                long _t = System.currentTimeMillis();
                Object id = context.get("issueId");
                if (id == null || fieldName == null) return false;
                if (mutationBuffer != null && !mutationBuffer.isCommitted()) {
                    mutationBuffer.addMutation(MutationBuffer.Mutation.SET_FIELD, id.toString(),
                            Map.of(fieldName, value != null ? value : ""));
                    trace("issue", "setFieldValue[buffered]", _t);
                    return true;
                }
                client.patchIssueFields(UUID.fromString(id.toString()), Map.of(fieldName, value));
                trace("issue", "setFieldValue", _t);
                return true;
            } catch (Exception e) { lastError = e.getMessage(); return false; }
        }

        @HostAccess.Export
        public Map<String, Object> getIssue(String issueIdOrKey) {
            try {
                if (issueIdOrKey == null) return Map.of();
                if (issueIdOrKey.contains("-")) {
                    return client.fetchIssueByKey(issueIdOrKey);
                }
                return client.fetchIssue(UUID.fromString(issueIdOrKey));
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public boolean addComment(String text) {
            try {
                long _t = System.currentTimeMillis();
                Object issueId = context.get("issueId");
                Object userId = context.get("userId");
                if (issueId == null || text == null) return false;
                client.addComment(
                        UUID.fromString(issueId.toString()),
                        text,
                        userId != null ? UUID.fromString(userId.toString()) : null);
                trace("issue", "addComment", _t);
                return true;
            } catch (Exception e) { lastError = e.getMessage(); return false; }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getComments() {
            try {
                long _t = System.currentTimeMillis();
                Object id = context.get("issueId");
                if (id == null) return List.of();
                List<Map<String, Object>> result = client.fetchComments(UUID.fromString(id.toString()));
                trace("issue", "getComments", _t);
                return result;
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getHistory() {
            try {
                Object id = context.get("issueId");
                if (id == null) return List.of();
                return client.fetchIssueHistory(UUID.fromString(id.toString()));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getWatchers() {
            try {
                long _t = System.currentTimeMillis();
                Object id = context.get("issueId");
                if (id == null) return List.of();
                List<Map<String, Object>> result = client.fetchWatchers(UUID.fromString(id.toString()));
                trace("issue", "getWatchers", _t);
                return result;
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public boolean addWatcher(String userId) {
            try {
                Object issueId = context.get("issueId");
                if (issueId == null || userId == null) return false;
                client.addWatcher(UUID.fromString(issueId.toString()), UUID.fromString(userId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean link(String targetIssueKey, String linkType) {
            try {
                Object issueId = context.get("issueId");
                if (issueId == null || targetIssueKey == null) return false;
                Map<String, Object> target = client.fetchIssueByKey(targetIssueKey);
                Object targetId = target.get("id");
                if (targetId == null) return false;
                client.createIssueLink(
                        UUID.fromString(issueId.toString()),
                        UUID.fromString(targetId.toString()),
                        linkType != null ? UUID.fromString(linkType) : null);
                return true;
            } catch (Exception e) { lastError = e.getMessage(); return false; }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getLinkedIssues() {
            try {
                long _t = System.currentTimeMillis();
                Object id = context.get("issueId");
                if (id == null) return List.of();
                List<Map<String, Object>> result = client.fetchLinkedIssuesForWorkflow(UUID.fromString(id.toString()));
                trace("issue", "getLinkedIssues", _t);
                return result;
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public int getAttachmentCount() {
            try {
                Object id = context.get("issueId");
                if (id == null) return 0;
                return client.countAttachments(UUID.fromString(id.toString()));
            } catch (Exception e) { return 0; }
        }

        @HostAccess.Export
        public Map<String, Object> createIssue(String projectId, String issueTypeId, String summary, Map<String, Object> fields) {
            try {
                long _t = System.currentTimeMillis();
                Map<String, Object> data = new java.util.HashMap<>(fields != null ? fields : Map.of());
                data.put("projectId", projectId);
                data.put("issueTypeId", issueTypeId);
                data.put("summary", summary);
                Object userId = context.get("userId");
                Map<String, Object> result = client.createIssue(data, userId != null ? UUID.fromString(userId.toString()) : null);
                trace("issue", "createIssue", _t);
                return result;
            } catch (Exception e) { lastError = e.getMessage(); return Map.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> cloneIssue(String issueIdOrKey) {
            try {
                long _t = System.currentTimeMillis();
                if (issueIdOrKey == null) return Map.of();
                UUID id;
                if (issueIdOrKey.contains("-")) {
                    Map<String, Object> issue = client.fetchIssueByKey(issueIdOrKey);
                    Object issueId = issue.get("id");
                    if (issueId == null) return Map.of();
                    id = UUID.fromString(issueId.toString());
                } else {
                    id = UUID.fromString(issueIdOrKey);
                }
                Map<String, Object> result = client.cloneIssue(id);
                trace("issue", "cloneIssue", _t);
                return result;
            } catch (Exception e) { lastError = e.getMessage(); return Map.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> moveIssue(String issueId, String targetProjectId) {
            try {
                long _t = System.currentTimeMillis();
                if (issueId == null || targetProjectId == null) return Map.of();
                Map<String, Object> result = client.moveIssue(UUID.fromString(issueId), UUID.fromString(targetProjectId));
                trace("issue", "moveIssue", _t);
                return result;
            } catch (Exception e) { lastError = e.getMessage(); return Map.of(); }
        }

        @HostAccess.Export
        public boolean deleteIssue(String issueId) {
            try {
                long _t = System.currentTimeMillis();
                if (issueId == null) return false;
                client.deleteIssue(UUID.fromString(issueId));
                trace("issue", "deleteIssue", _t);
                return true;
            } catch (Exception e) { lastError = e.getMessage(); return false; }
        }

        @HostAccess.Export
        public boolean transitionIssue(String issueId, String transitionId) {
            try {
                long _t = System.currentTimeMillis();
                if (issueId == null || transitionId == null) return false;
                Object projectId = context.get("projectId");
                client.transitionIssue(UUID.fromString(issueId),
                        projectId != null ? UUID.fromString(projectId.toString()) : null,
                        transitionId);
                trace("issue", "transitionIssue", _t);
                return true;
            } catch (Exception e) { lastError = e.getMessage(); return false; }
        }

        @HostAccess.Export
        public boolean addLabel(String label) {
            try {
                Object id = context.get("issueId");
                if (id == null || label == null) return false;
                client.addLabel(UUID.fromString(id.toString()), label);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean removeLabel(String label) {
            try {
                Object id = context.get("issueId");
                if (id == null || label == null) return false;
                client.removeLabel(UUID.fromString(id.toString()), label);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getLabels() {
            try {
                Object id = context.get("issueId");
                if (id == null) return List.of();
                return client.fetchLabels(UUID.fromString(id.toString()));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getWorklogs() {
            try {
                Object id = context.get("issueId");
                if (id == null) return List.of();
                return client.fetchWorklogs(UUID.fromString(id.toString()));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> addWorklog(String timeSpent, String comment) {
            try {
                Object id = context.get("issueId");
                Object userId = context.get("userId");
                if (id == null || timeSpent == null) return Map.of();
                return client.addWorklog(UUID.fromString(id.toString()), timeSpent, comment,
                        userId != null ? UUID.fromString(userId.toString()) : null);
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getSubtasks() {
            try {
                long _t = System.currentTimeMillis();
                Object id = context.get("issueId");
                if (id == null) return List.of();
                List<Map<String, Object>> result = client.fetchSubtasks(UUID.fromString(id.toString()));
                trace("issue", "getSubtasks", _t);
                return result;
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public boolean removeWatcher(String userId) {
            try {
                Object issueId = context.get("issueId");
                if (issueId == null || userId == null) return false;
                client.removeWatcher(UUID.fromString(issueId.toString()), UUID.fromString(userId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean addVote() {
            try {
                Object issueId = context.get("issueId");
                Object userId = context.get("userId");
                if (issueId == null) return false;
                client.addVote(UUID.fromString(issueId.toString()),
                        userId != null ? UUID.fromString(userId.toString()) : null);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean removeVote() {
            try {
                Object issueId = context.get("issueId");
                Object userId = context.get("userId");
                if (issueId == null) return false;
                client.removeVote(UUID.fromString(issueId.toString()),
                        userId != null ? UUID.fromString(userId.toString()) : null);
                return true;
            } catch (Exception e) { return false; }
        }

        // --- Attachment Methods ---

        @HostAccess.Export
        public List<Map<String, Object>> getAttachments() {
            try {
                Object id = context.get("issueId");
                if (id == null) return List.of();
                return client.fetchAttachments(UUID.fromString(id.toString()));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public boolean deleteAttachment(String attachmentId) {
            try {
                if (attachmentId == null) return false;
                client.deleteAttachment(UUID.fromString(attachmentId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public String getAttachmentContent(String attachmentId) {
            try {
                if (attachmentId == null) return "";
                byte[] content = client.fetchAttachmentContent(UUID.fromString(attachmentId));
                if (content == null || content.length == 0) return "";
                return Base64.getEncoder().encodeToString(content);
            } catch (Exception e) { return ""; }
        }

        @HostAccess.Export
        public String getAttachmentUrl(String attachmentId) {
            try {
                if (attachmentId == null) return "";
                return client.getAttachmentUrl(UUID.fromString(attachmentId));
            } catch (Exception e) { return ""; }
        }

        @HostAccess.Export
        public boolean copyAttachments(String targetIssueId) {
            try {
                Object id = context.get("issueId");
                if (id == null || targetIssueId == null) return false;
                client.copyAttachments(UUID.fromString(id.toString()), UUID.fromString(targetIssueId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public Map<String, Object> addAttachment(String issueId, String filename, String base64Content) {
            try {
                if (issueId == null || filename == null || base64Content == null) return Map.of();
                return client.uploadAttachment(UUID.fromString(issueId), filename, base64Content);
            } catch (Exception e) { lastError = e.getMessage(); return Map.of(); }
        }

        // --- Comment Mutation Methods ---

        @HostAccess.Export
        public boolean deleteComment(String commentId) {
            try {
                if (commentId == null) return false;
                client.deleteComment(UUID.fromString(commentId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean updateComment(String commentId, String newText) {
            try {
                if (commentId == null || newText == null) return false;
                Object userId = context.get("userId");
                client.updateComment(UUID.fromString(commentId), newText,
                        userId != null ? UUID.fromString(userId.toString()) : null);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public Map<String, Object> getLastComment() {
            try {
                Object id = context.get("issueId");
                if (id == null) return Map.of();
                List<Map<String, Object>> comments = client.fetchComments(UUID.fromString(id.toString()));
                if (comments == null || comments.isEmpty()) return Map.of();
                return comments.get(comments.size() - 1);
            } catch (Exception e) { return Map.of(); }
        }

        // --- Field Metadata (Task 2.5) ---

        @HostAccess.Export
        public boolean hasField(String fieldName) {
            try {
                Object id = context.get("issueId");
                if (id == null || fieldName == null) return false;
                Map<String, Object> issue = client.fetchIssue(UUID.fromString(id.toString()));
                return issue.containsKey(fieldName);
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getAvailableFieldValues(String fieldName, String projectId, String issueTypeId) {
            try {
                if (fieldName == null) return List.of();
                return client.fetchAvailableFieldValues(fieldName,
                    projectId != null ? UUID.fromString(projectId) : null,
                    issueTypeId != null ? UUID.fromString(issueTypeId) : null);
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public String getFieldType(String fieldName) {
            try {
                if (fieldName == null) return null;
                Map<String, Object> meta = client.fetchFieldMetadata(fieldName);
                return meta.getOrDefault("fieldType", "unknown").toString();
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public boolean clearField(String fieldName) {
            try {
                return setFieldValue(fieldName, null);
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getCustomFieldOptions(String fieldName) {
            try {
                if (fieldName == null) return List.of();
                return client.fetchCustomFieldOptions(fieldName);
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public String getFieldId(String fieldName) {
            try {
                if (fieldName == null) return null;
                Map<String, Object> meta = client.fetchFieldMetadata(fieldName);
                Object id = meta.get("id");
                return id != null ? id.toString() : null;
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public String getFieldName(String fieldId) {
            try {
                if (fieldId == null) return null;
                Map<String, Object> meta = client.fetchFieldMetadata(fieldId);
                Object name = meta.get("name");
                return name != null ? name.toString() : null;
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public boolean isFieldRequired(String fieldName, String screenId) {
            try {
                if (fieldName == null) return false;
                Map<String, Object> meta = client.fetchFieldMetadata(fieldName);
                Object required = meta.get("required");
                return Boolean.TRUE.equals(required);
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public Map<String, Object> getFieldConfig(String fieldName) {
            try {
                if (fieldName == null) return Map.of();
                return client.fetchFieldMetadata(fieldName);
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public String getFieldScreenTab(String fieldName) {
            try {
                if (fieldName == null) return null;
                Map<String, Object> meta = client.fetchFieldMetadata(fieldName);
                Object tab = meta.get("screenTab");
                return tab != null ? tab.toString() : null;
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public boolean addCustomFieldOption(String fieldName, String optionValue) {
            try {
                if (fieldName == null || optionValue == null) return false;
                client.addFieldOption(fieldName, optionValue);
                return true;
            } catch (Exception e) { return false; }
        }

        // --- Comment Visibility (SIL parity) ---

        @HostAccess.Export
        public boolean setCommentVisibility(String commentId, String restrictionType, String restrictionValue) {
            try {
                if (commentId == null) return false;
                Map<String, Object> body = new HashMap<>();
                body.put("restrictionType", restrictionType);
                body.put("restrictionValue", restrictionValue);
                client.updateCommentVisibility(UUID.fromString(commentId), body);
                return true;
            } catch (Exception e) { return false; }
        }

        // --- Issue Function Completion (Task 2.8) ---

        @HostAccess.Export
        public boolean unlinkIssue(String sourceIssueId, String targetIssueId) {
            try {
                if (sourceIssueId == null || targetIssueId == null) return false;
                client.unlinkIssues(UUID.fromString(sourceIssueId), UUID.fromString(targetIssueId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean setRank(String issueId, int rank) {
            try {
                if (issueId == null) return false;
                client.setIssueRank(UUID.fromString(issueId), rank);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public String getSecurityLevel() {
            try {
                Object id = context.get("issueId");
                if (id == null) return null;
                Map<String, Object> issue = client.fetchIssue(UUID.fromString(id.toString()));
                Object level = issue.get("securityLevel");
                return level != null ? level.toString() : null;
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public boolean setSecurityLevel(String issueId, String levelId) {
            try {
                if (issueId == null) return false;
                Map<String, Object> fields = new HashMap<>();
                fields.put("securityLevelId", levelId);
                client.patchIssueFields(UUID.fromString(issueId), fields);
                return true;
            } catch (Exception e) { return false; }
        }

        // --- Worklog Completion (Task 2.9) ---

        @HostAccess.Export
        public boolean deleteWorklog(String worklogId) {
            try {
                if (worklogId == null) return false;
                client.deleteWorklog(UUID.fromString(worklogId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean updateWorklog(String worklogId, String timeSpent, String comment) {
            try {
                if (worklogId == null) return false;
                client.updateWorklog(UUID.fromString(worklogId), timeSpent, comment);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public String getRemainingEstimate() {
            try {
                Object id = context.get("issueId");
                if (id == null) return null;
                Map<String, Object> issue = client.fetchIssue(UUID.fromString(id.toString()));
                Object estimate = issue.get("remainingEstimate");
                return estimate != null ? estimate.toString() : null;
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public String getOriginalEstimate() {
            try {
                Object id = context.get("issueId");
                if (id == null) return null;
                Map<String, Object> issue = client.fetchIssue(UUID.fromString(id.toString()));
                Object estimate = issue.get("originalEstimate");
                return estimate != null ? estimate.toString() : null;
            } catch (Exception e) { return null; }
        }
    }

    // === Sub-API: jdc.project ===

    public class ProjectApi {

        @HostAccess.Export
        public Map<String, Object> getCurrentProject() {
            try {
                Object id = context.get("projectId");
                if (id == null) return Map.of();
                return client.fetchProject(UUID.fromString(id.toString()));
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> getProject(String projectId) {
            try {
                if (projectId == null) return Map.of();
                return client.fetchProject(UUID.fromString(projectId));
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> getProjectByKey(String key) {
            try {
                if (key == null) return Map.of();
                return client.fetchProjectByKey(key);
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getVersions(String projectId) {
            try {
                if (projectId == null) return List.of();
                return client.fetchProjectVersions(UUID.fromString(projectId));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getComponents(String projectId) {
            try {
                if (projectId == null) return List.of();
                return client.fetchProjectComponents(UUID.fromString(projectId));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getIssueTypes() {
            try {
                return client.fetchIssueTypes();
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getMembers(String projectId) {
            try {
                if (projectId == null) return List.of();
                return client.fetchProjectMembers(UUID.fromString(projectId));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> createVersion(String projectId, String name, String releaseDate) {
            try {
                if (projectId == null || name == null) return Map.of();
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("projectId", projectId);
                data.put("name", name);
                if (releaseDate != null) data.put("releaseDate", releaseDate);
                return client.createVersion(data);
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public boolean releaseVersion(String versionId) {
            try {
                if (versionId == null) return false;
                client.releaseVersion(UUID.fromString(versionId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public Map<String, Object> createComponent(String projectId, String name, String leadId) {
            try {
                if (projectId == null || name == null) return Map.of();
                Map<String, Object> data = new java.util.HashMap<>();
                data.put("projectId", projectId);
                data.put("name", name);
                if (leadId != null) data.put("leadId", leadId);
                return client.createComponent(data);
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public boolean archiveVersion(String versionId) {
            try {
                if (versionId == null) return false;
                client.archiveVersion(UUID.fromString(versionId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean deleteVersion(String versionId) {
            try {
                if (versionId == null) return false;
                client.deleteVersion(UUID.fromString(versionId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean unreleaseVersion(String versionId) {
            try {
                if (versionId == null) return false;
                client.unreleaseVersion(UUID.fromString(versionId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean deleteComponent(String componentId) {
            try {
                if (componentId == null) return false;
                client.deleteComponent(UUID.fromString(componentId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getProjectRoles(String projectId) {
            try {
                if (projectId == null) return List.of();
                return client.fetchProjectRoles(UUID.fromString(projectId));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getAllProjects(String query) {
            try {
                return client.searchProjects(query);
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public Object getProjectProperty(String projectId, String key) {
            try {
                if (projectId == null || key == null) return null;
                Map<String, Object> props = client.fetchProjectProperties(UUID.fromString(projectId));
                return props.get(key);
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public boolean setProjectProperty(String projectId, String key, Object value) {
            try {
                if (projectId == null || key == null) return false;
                client.setProjectProperty(UUID.fromString(projectId), key, value);
                return true;
            } catch (Exception e) { return false; }
        }
    }

    // === Sub-API: jdc.user ===

    public class UserApi {

        @HostAccess.Export
        public Map<String, Object> getCurrentUser() {
            try {
                Object id = context.get("userId");
                if (id == null) return Map.of();
                return client.fetchUser(UUID.fromString(id.toString()));
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> getUser(String userId) {
            try {
                if (userId == null) return Map.of();
                return client.fetchUser(UUID.fromString(userId));
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        @SuppressWarnings("unchecked")
        public boolean isInGroup(String groupName) {
            try {
                Map<String, Object> userData = castMap(context.getOrDefault("userData", Map.of()));
                Object groups = userData.get("groups");
                if (groups instanceof List<?> list) {
                    return list.stream().anyMatch(g -> groupName.equalsIgnoreCase(String.valueOf(g)));
                }
                return false;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean hasPermission(String permission) {
            try {
                Object userId = context.get("userId");
                Object projectId = context.get("projectId");
                if (userId == null || projectId == null || permission == null) return false;
                return client.checkUserPermission(
                        UUID.fromString(userId.toString()),
                        UUID.fromString(projectId.toString()),
                        permission);
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        @SuppressWarnings("unchecked")
        public List<String> getUserGroups() {
            try {
                Map<String, Object> userData = castMap(context.getOrDefault("userData", Map.of()));
                Object groups = userData.get("groups");
                if (groups instanceof List<?> list) {
                    return list.stream().map(String::valueOf).toList();
                }
                return List.of();
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public boolean addUserToGroup(String userId, String groupName) {
            try {
                if (userId == null || groupName == null) return false;
                client.addUserToGroup(UUID.fromString(userId), groupName);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean removeUserFromGroup(String userId, String groupName) {
            try {
                if (userId == null || groupName == null) return false;
                client.removeUserFromGroup(UUID.fromString(userId), groupName);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean isAdmin(String userId) {
            try {
                if (userId == null) return false;
                return client.checkUserIsAdmin(UUID.fromString(userId));
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public Map<String, Object> getUserByEmail(String email) {
            try {
                if (email == null) return Map.of();
                return client.fetchUserByEmail(email);
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getAllUsers(String query, int limit) {
            try {
                return client.searchUsers(query, Math.min(limit, 200));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> createUser(String username, String email, String displayName) {
            try {
                if (username == null || email == null) return Map.of();
                return client.createUser(username, email, displayName);
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public boolean deactivateUser(String userId) {
            try {
                if (userId == null) return false;
                client.deactivateUser(UUID.fromString(userId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public boolean deleteUser(String userId) {
            try {
                if (userId == null) return false;
                client.deleteUser(UUID.fromString(userId));
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public Map<String, Object> createGroup(String groupName) {
            try {
                if (groupName == null) return Map.of();
                return client.createGroup(groupName);
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public boolean deleteGroup(String groupName) {
            try {
                if (groupName == null) return false;
                client.deleteGroup(groupName);
                return true;
            } catch (Exception e) { return false; }
        }
    }

    // === Sub-API: jdc.workflow ===

    public class WorkflowApi {

        @HostAccess.Export
        public Map<String, Object> getCurrentTransition() {
            try {
                return Map.of(
                        "transitionId", str(context.get("transitionId")),
                        "transitionName", str(context.get("transitionName")),
                        "fromStatusId", str(context.get("fromStatusId")),
                        "toStatusId", str(context.get("toStatusId"))
                );
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getAllStatuses() {
            try {
                return client.fetchIssueStatuses();
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getAvailableActions(String issueId) {
            try {
                if (issueId == null) return List.of();
                return client.fetchAvailableTransitions(UUID.fromString(issueId));
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public String getWorkflowName(String issueId) {
            try {
                if (issueId == null) return null;
                Map<String, Object> wf = client.fetchWorkflowForIssue(UUID.fromString(issueId));
                return wf.getOrDefault("name", "").toString();
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public Map<String, Object> getWorkflowScheme(String projectId) {
            try {
                if (projectId == null) return Map.of();
                return client.fetchWorkflowScheme(UUID.fromString(projectId));
            } catch (Exception e) { return Map.of(); }
        }

        @HostAccess.Export
        public Map<String, Object> getTransitionProperties(String transitionId) {
            try {
                if (transitionId == null) return Map.of();
                return client.fetchTransitionProperties(UUID.fromString(transitionId));
            } catch (Exception e) { return Map.of(); }
        }
    }

    // === Sub-API: jdc.search ===

    public class SearchApi {

        private static final int MAX_RESULTS_CAP = 500;

        @HostAccess.Export
        public List<Map<String, Object>> jql(String query, int maxResults) {
            try {
                if (query == null) return List.of();
                int capped = Math.min(Math.max(maxResults, 1), MAX_RESULTS_CAP);
                return client.searchIssuesJql(query, capped);
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> findIssues(String projectKey, String statusName) {
            try {
                if (projectKey == null || !projectKey.matches("^[A-Za-z][A-Za-z0-9_-]{0,30}$")) {
                    return List.of();
                }
                String q = "project = \"" + projectKey + "\"";
                if (statusName != null && !statusName.isBlank()) {
                    String sanitized = statusName.replaceAll("[\"\\\\]", "");
                    q += " AND status = \"" + sanitized + "\"";
                }
                return jql(q, 100);
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public int batch(String jqlQuery, int batchSize) {
            try {
                if (jqlQuery == null) return 0;
                int size = Math.min(Math.max(batchSize, 10), MAX_RESULTS_CAP);
                List<Map<String, Object>> results = jql(jqlQuery, size);
                return results.size();
            } catch (Exception e) { return 0; }
        }
    }

    // === Sub-API: jdc.log ===

    public class LogApi {
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("jdc.script");

        @HostAccess.Export
        public void info(Object... args) { logger.info("[script] {}", sanitize(join(args))); }

        @HostAccess.Export
        public void warn(Object... args) { logger.warn("[script] {}", sanitize(join(args))); }

        @HostAccess.Export
        public void error(Object... args) { logger.error("[script] {}", sanitize(join(args))); }

        @HostAccess.Export
        public void debug(Object... args) { logger.debug("[script] {}", sanitize(join(args))); }

        private String join(Object[] args) {
            if (args == null) return "null";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(' ');
                String val = args[i] == null ? "null" : String.valueOf(args[i]);
                if (sb.length() + val.length() > 10000) {
                    sb.append("[truncated]");
                    break;
                }
                sb.append(val);
            }
            return sb.toString();
        }

        private String sanitize(String msg) {
            return msg.replace("\r\n", " ").replace("\n", " ").replace("\r", " ");
        }
    }

    // === Security — runAs (SIL parity) ===

    @HostAccess.Export
    public Object runAs(String userId, Value callback) {
        try {
            if (userId == null || callback == null || !callback.canExecute()) return null;
            Object originalUserId = context.get("userId");
            context.put("userId", userId);
            try {
                return callback.execute();
            } finally {
                context.put("userId", originalUserId);
            }
        } catch (Exception e) {
            log.warn("runAs failed: {}", e.getMessage());
            return null;
        }
    }

    @HostAccess.Export
    public List<String> getUserPermissions(String userId) {
        try {
            if (userId == null) return List.of();
            return client.fetchUserPermissions(UUID.fromString(userId));
        } catch (Exception e) { return List.of(); }
    }

    // === Tracer / MutationBuffer support ===

    public void setTracer(ScriptTracer tracer) {
        this.tracer = tracer;
    }

    private void trace(String api, String method, long startMs) {
        if (tracer != null) tracer.traceApiCall(api, method, startMs);
    }

    public void setMutationBuffer(MutationBuffer buffer) {
        this.mutationBuffer = buffer;
    }

    @HostAccess.Export
    public boolean flush() {
        try {
            if (mutationBuffer == null || mutationBuffer.size() == 0) return true;
            for (MutationBuffer.Mutation m : mutationBuffer.getMutations()) {
                switch (m.type()) {
                    case MutationBuffer.Mutation.SET_FIELD -> {
                        UUID issueId = UUID.fromString(m.target());
                        client.patchIssueFields(issueId, m.data());
                    }
                    case MutationBuffer.Mutation.ADD_COMMENT -> {
                        UUID issueId = UUID.fromString(m.target());
                        String text = (String) m.data().get("text");
                        UUID userId = m.data().get("userId") != null ? UUID.fromString(m.data().get("userId").toString()) : null;
                        client.addComment(issueId, text, userId);
                    }
                    case MutationBuffer.Mutation.ADD_LABEL -> {
                        UUID issueId = UUID.fromString(m.target());
                        String label = (String) m.data().get("label");
                        client.addLabel(issueId, label);
                    }
                    case MutationBuffer.Mutation.REMOVE_LABEL -> {
                        UUID issueId = UUID.fromString(m.target());
                        String label = (String) m.data().get("label");
                        client.removeLabel(issueId, label);
                    }
                }
            }
            mutationBuffer.markCommitted();
            mutationBuffer.clear();
            return true;
        } catch (Exception e) {
            log.warn("Flush failed: {}", e.getMessage());
            return false;
        }
    }

    // === Helpers ===

    private String str(Object val) {
        return val != null ? val.toString() : "";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }
}
