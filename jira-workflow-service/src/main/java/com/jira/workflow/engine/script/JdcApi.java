package com.jira.workflow.engine.script;

import com.jira.workflow.engine.WorkflowIntegrationClient;
import org.graalvm.polyglot.HostAccess;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JdcApi {

    private final WorkflowIntegrationClient client;
    private final Map<String, Object> context;

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
                Map<String, Object> issueData = castMap(context.getOrDefault("issueData", Map.of()));
                return issueData.get(fieldName);
            } catch (Exception e) { return null; }
        }

        @HostAccess.Export
        public boolean setFieldValue(String fieldName, Object value) {
            try {
                Object id = context.get("issueId");
                if (id == null || fieldName == null) return false;
                client.patchIssueFields(UUID.fromString(id.toString()), Map.of(fieldName, value));
                return true;
            } catch (Exception e) { return false; }
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
                Object issueId = context.get("issueId");
                Object userId = context.get("userId");
                if (issueId == null || text == null) return false;
                client.addComment(
                        UUID.fromString(issueId.toString()),
                        text,
                        userId != null ? UUID.fromString(userId.toString()) : null);
                return true;
            } catch (Exception e) { return false; }
        }

        @HostAccess.Export
        public List<Map<String, Object>> getComments() {
            try {
                Object id = context.get("issueId");
                if (id == null) return List.of();
                return client.fetchComments(UUID.fromString(id.toString()));
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
                Object id = context.get("issueId");
                if (id == null) return List.of();
                return client.fetchWatchers(UUID.fromString(id.toString()));
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
            } catch (Exception e) { return false; }
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
        public int getAttachmentCount() {
            try {
                Object id = context.get("issueId");
                if (id == null) return 0;
                return client.countAttachments(UUID.fromString(id.toString()));
            } catch (Exception e) { return 0; }
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
    }

    // === Sub-API: jdc.search ===

    public class SearchApi {

        @HostAccess.Export
        public List<Map<String, Object>> jql(String query, int maxResults) {
            try {
                if (query == null) return List.of();
                return client.searchIssuesJql(query, maxResults > 0 ? maxResults : 50);
            } catch (Exception e) { return List.of(); }
        }

        @HostAccess.Export
        public List<Map<String, Object>> findIssues(String projectKey, String statusName) {
            try {
                String q = "project = \"" + projectKey + "\"";
                if (statusName != null && !statusName.isBlank()) {
                    q += " AND status = \"" + statusName + "\"";
                }
                return jql(q, 100);
            } catch (Exception e) { return List.of(); }
        }
    }

    // === Sub-API: jdc.log ===

    public class LogApi {
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("jdc.script");

        @HostAccess.Export
        public void info(Object... args) { logger.info("[script] {}", join(args)); }

        @HostAccess.Export
        public void warn(Object... args) { logger.warn("[script] {}", join(args)); }

        @HostAccess.Export
        public void error(Object... args) { logger.error("[script] {}", join(args)); }

        @HostAccess.Export
        public void debug(Object... args) { logger.debug("[script] {}", join(args)); }

        private String join(Object[] args) {
            if (args == null) return "null";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(' ');
                sb.append(args[i] == null ? "null" : args[i]);
            }
            return sb.toString();
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
