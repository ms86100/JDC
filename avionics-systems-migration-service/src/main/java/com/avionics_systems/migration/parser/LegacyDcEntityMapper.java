package com.avionics_systems.migration.parser;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Legacy DC XML entity fields to platform persister payloads.
 */
public final class LegacyDcEntityMapper {

    private LegacyDcEntityMapper() {
    }

    public static Map<String, Object> toIssueData(Map<String, String> fields, String entityKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("issueKey", entityKey);
        putIfPresent(data, "summary", first(fields, "summary", "title"));
        putIfPresent(data, "description", first(fields, "description", "body"));
        putIfPresent(data, "issueType", first(fields, "type", "issueType", "issuetype"));
        putIfPresent(data, "status", first(fields, "status"));
        putIfPresent(data, "priority", first(fields, "priority"));
        putIfPresent(data, "assignee", first(fields, "assignee", "assigneeKey"));
        putIfPresent(data, "reporter", first(fields, "reporter", "creator", "reporterKey"));
        putIfPresent(data, "parentIssueKey", first(fields, "parent", "parentKey", "parentIssue"));
        putIfPresent(data, "epicLink", first(fields, "epicLink", "epic"));
        putIfPresent(data, "createdAt", first(fields, "created", "createdDate"));
        putIfPresent(data, "updatedAt", first(fields, "updated", "updatedDate"));
        putIfPresent(data, "dueDate", first(fields, "duedate", "dueDate"));
        putIfPresent(data, "environment", first(fields, "environment"));
        putIfPresent(data, "securityLevelId", first(fields, "security", "securityLevel", "securityLevelId"));
        putIfPresent(data, "resolution", first(fields, "resolution"));
        if (fields.containsKey("project")) {
            data.put("projectKey", fields.get("project"));
        }
        if (fields.containsKey("labels")) {
            data.put("labels", List.of(fields.get("labels").split(",")));
        }
        Map<String, Object> customFields = extractCustomFields(fields);
        if (!customFields.isEmpty()) {
            data.put("customFields", customFields);
        }
        return data;
    }

    public static Map<String, Object> toIssueLinkData(Map<String, String> fields, String entityKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("linkType", first(fields, "linkType", "type", "linktype"));
        data.put("sourceIssueKey", first(fields, "sourceIssueKey", "outwardKey", "outward"));
        data.put("targetIssueKey", first(fields, "targetIssueKey", "inwardKey", "inward"));
        if (data.get("sourceIssueKey") == null && entityKey != null && entityKey.contains("->")) {
            String[] parts = entityKey.split("->", 2);
            data.put("sourceIssueKey", parts[0].trim());
            data.put("targetIssueKey", parts.length > 1 ? parts[1].trim() : null);
        }
        return data;
    }

    public static Map<String, Object> toHistoryData(Map<String, String> fields, String entityKey) {
        Map<String, Object> data = new HashMap<>();
        putIfPresent(data, "issueKey", first(fields, "issue", "issueKey"));
        putIfPresent(data, "field", first(fields, "field"));
        putIfPresent(data, "old", first(fields, "old", "oldValue", "oldString"));
        putIfPresent(data, "new", first(fields, "new", "newValue", "newString"));
        putIfPresent(data, "author", first(fields, "author", "authorKey"));
        putIfPresent(data, "created", first(fields, "created", "createdDate"));
        data.put("entityKey", entityKey);
        return data;
    }

    /**
     * Collects {@code customfield_*} keys from RSS or Entity exports into a platform customFields map.
     */
    public static Map<String, Object> extractCustomFields(Map<String, String> fields) {
        Map<String, Object> customFields = new HashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            if (key == null || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (key.startsWith("customfield_") && !key.endsWith("_name")) {
                customFields.put(key, entry.getValue());
                String nameKey = key + "_name";
                if (fields.containsKey(nameKey)) {
                    customFields.put(key + ".name", fields.get(nameKey));
                }
            }
        }
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.endsWith("_name") && "Story Points".equalsIgnoreCase(entry.getValue())) {
                String cfId = key.substring(0, key.length() - 5);
                String value = fields.get(cfId);
                if (value != null && !value.isBlank()) {
                    customFields.putIfAbsent("story_points", value);
                }
            }
        }
        return customFields;
    }

    public static Map<String, Object> toCommentData(Map<String, String> fields, String entityKey,
                                                    Map<String, String> issueKeyToTargetId) {
        return toCommentData(fields, entityKey, issueKeyToTargetId, null);
    }

    public static Map<String, Object> toCommentData(Map<String, String> fields, String entityKey,
                                                    Map<String, String> issueKeyToTargetId,
                                                    com.avionics_systems.migration.dc.LegacyDcIssueIdRegistry idRegistry) {
        Map<String, Object> data = new HashMap<>();
        String issueKey = resolveIssueKey(fields, entityKey, idRegistry);
        data.put("issueKey", issueKey);
        if (issueKey != null && issueKeyToTargetId.containsKey(issueKey)) {
            data.put("issueId", issueKeyToTargetId.get(issueKey));
        }
        data.put("body", first(fields, "body", "comment", "commentbody", "text"));
        putIfPresent(data, "authorId", first(fields, "author", "authorKey", "username"));
        putIfPresent(data, "createdAt", first(fields, "created", "createdDate", "createdAt"));
        return data;
    }

    public static AttachmentPayload toAttachmentPayload(Map<String, String> fields, String entityKey,
                                                          Map<String, String> issueKeyToTargetId) {
        return toAttachmentPayload(fields, entityKey, issueKeyToTargetId, null);
    }

    public static AttachmentPayload toAttachmentPayload(Map<String, String> fields, String entityKey,
                                                          Map<String, String> issueKeyToTargetId,
                                                          com.avionics_systems.migration.dc.LegacyDcIssueIdRegistry idRegistry) {
        Map<String, Object> data = new HashMap<>();
        String issueKey = resolveIssueKey(fields, entityKey, idRegistry);
        data.put("issueKey", issueKey);
        if (issueKey != null && issueKeyToTargetId.containsKey(issueKey)) {
            data.put("issueId", issueKeyToTargetId.get(issueKey));
        }
        String fileName = first(fields, "filename", "fileName", "name", "attachmentFilename");
        if (fileName == null) {
            fileName = "attachment-" + entityKey;
        }
        data.put("fileName", fileName);
        data.put("mimeType", first(fields, "mimetype", "mimeType", "contentType"));
        putIfPresent(data, "authorId", first(fields, "author", "authorKey"));
        putIfPresent(data, "expectedChecksum", first(fields, "checksum", "md5", "sha256"));

        byte[] content = decodeContent(fields);
        return new AttachmentPayload(data, content);
    }

    public static Map<String, Object> toComponentData(Map<String, String> fields, String entityKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", first(fields, "name", "componentName"));
        data.put("description", first(fields, "description"));
        data.put("projectKey", first(fields, "project", "projectKey"));
        if (data.get("name") == null) {
            data.put("name", entityKey);
        }
        return data;
    }

    public static Map<String, Object> toVersionData(Map<String, String> fields, String entityKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", first(fields, "name", "versionName"));
        data.put("description", first(fields, "description"));
        data.put("projectKey", first(fields, "project", "projectKey"));
        data.put("released", parseBoolean(first(fields, "released", "isReleased")));
        data.put("archived", parseBoolean(first(fields, "archived", "isArchived")));
        putIfPresent(data, "releaseDate", first(fields, "releaseDate", "releasedate"));
        if (data.get("name") == null) {
            data.put("name", entityKey);
        }
        return data;
    }

    private static Boolean parseBoolean(String v) {
        if (v == null) {
            return null;
        }
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
    }

    public static Map<String, Object> toWorklogData(Map<String, String> fields, String entityKey,
                                                    Map<String, String> issueKeyToTargetId) {
        return toWorklogData(fields, entityKey, issueKeyToTargetId, null);
    }

    public static Map<String, Object> toWorklogData(Map<String, String> fields, String entityKey,
                                                    Map<String, String> issueKeyToTargetId,
                                                    com.avionics_systems.migration.dc.LegacyDcIssueIdRegistry idRegistry) {
        Map<String, Object> data = new HashMap<>();
        String issueKey = resolveIssueKey(fields, entityKey, idRegistry);
        data.put("issueKey", issueKey);
        if (issueKey != null && issueKeyToTargetId.containsKey(issueKey)) {
            data.put("issueId", issueKeyToTargetId.get(issueKey));
        }
        putIfPresent(data, "timeSpentFormatted", first(fields, "timeSpent", "timeSpentSeconds", "timespent"));
        putIfPresent(data, "timeSpentSeconds", parseInt(first(fields, "timeSpentSeconds", "seconds")));
        putIfPresent(data, "startedAt", first(fields, "started", "startDate", "created"));
        putIfPresent(data, "comment", first(fields, "comment", "body"));
        putIfPresent(data, "authorId", first(fields, "author", "authorKey"));
        return data;
    }

    private static String resolveIssueKey(Map<String, String> fields, String entityKey) {
        return resolveIssueKey(fields, entityKey, null);
    }

    private static String resolveIssueKey(Map<String, String> fields, String entityKey,
                                          com.avionics_systems.migration.dc.LegacyDcIssueIdRegistry idRegistry) {
        String issue = first(fields, "issue", "issueKey", "issueId", "issuekey");
        if (issue != null) {
            if (idRegistry != null && !issue.contains("-")) {
                String resolved = idRegistry.resolveIssueKey(issue);
                if (resolved != null) {
                    return resolved;
                }
            }
            return issue.contains("-") ? issue : (idRegistry != null ? idRegistry.resolveIssueKey(issue) : issue);
        }
        if (fields.containsKey("issueKey") && fields.get("issueKey") != null) {
            return fields.get("issueKey");
        }
        if (entityKey != null && entityKey.contains(":")) {
            return entityKey.split(":")[0];
        }
        return null;
    }

    private static byte[] decodeContent(Map<String, String> fields) {
        String encoded = first(fields, "file", "content", "data", "base64");
        if (encoded == null || encoded.isBlank()) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException e) {
            return encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private static String first(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            if (fields.containsKey(key) && fields.get(key) != null && !fields.get(key).isBlank()) {
                return fields.get(key);
            }
        }
        return null;
    }

    private static Integer parseInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    public record AttachmentPayload(Map<String, Object> metadata, byte[] content) {
    }
}
