package com.avionics_systems.migration.jiradc;

import java.util.*;

/**
 * Maps Jira Data Center REST API JSON responses to the internal Map<String, Object>
 * format consumed by the platform's persister handlers.
 */
public final class JiraDcEntityMapper {

    private JiraDcEntityMapper() {}

    private static final Map<String, String> FIELD_NAME_BY_ID = new HashMap<>();
    private static final Map<String, String> ACTIVE_FIELD_MAPPINGS = new HashMap<>();

    public static void registerFieldMappings(Map<String, String> mappings) {
        ACTIVE_FIELD_MAPPINGS.clear();
        if (mappings != null) {
            ACTIVE_FIELD_MAPPINGS.putAll(mappings);
        }
    }

    public static void registerFieldNames(List<Map<String, Object>> jiraFields) {
        FIELD_NAME_BY_ID.clear();
        for (Map<String, Object> field : jiraFields) {
            String id = str(field, "id");
            String name = str(field, "name");
            if (id != null && name != null) {
                FIELD_NAME_BY_ID.put(id, name);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toIssueData(Map<String, Object> jiraIssue) {
        Map<String, Object> data = new HashMap<>();
        String issueKey = str(jiraIssue, "key");
        data.put("issueKey", issueKey);
        data.put("originalIssueKey", issueKey);

        Map<String, Object> fields = (Map<String, Object>) jiraIssue.getOrDefault("fields", Map.of());

        data.put("summary", str(fields, "summary"));
        data.put("description", str(fields, "description"));

        putNestedName(data, "issueType", fields, "issuetype");
        putNestedName(data, "status", fields, "status");
        putNestedName(data, "priority", fields, "priority");
        putNestedName(data, "resolution", fields, "resolution");

        putUserName(data, "assignee", fields, "assignee");
        putUserName(data, "reporter", fields, "reporter");

        Map<String, Object> project = nested(fields, "project");
        if (project != null) {
            data.put("projectKey", str(project, "key"));
        }

        putIfPresent(data, "createdAt", str(fields, "created"));
        putIfPresent(data, "updatedAt", str(fields, "updated"));
        putIfPresent(data, "dueDate", str(fields, "duedate"));
        putIfPresent(data, "environment", str(fields, "environment"));

        Object resolutionDate = fields.get("resolutiondate");
        if (resolutionDate != null) {
            data.put("resolutionDate", resolutionDate.toString());
        }

        // Parent issue (subtask)
        Map<String, Object> parent = nested(fields, "parent");
        if (parent != null) {
            data.put("parentIssueKey", str(parent, "key"));
        }

        // Labels
        Object labelsObj = fields.get("labels");
        if (labelsObj instanceof List<?> labelsList && !labelsList.isEmpty()) {
            data.put("labels", labelsList.stream().map(Object::toString).toList());
        }

        // Components
        Object componentsObj = fields.get("components");
        if (componentsObj instanceof List<?> compList && !compList.isEmpty()) {
            List<String> componentNames = compList.stream()
                    .filter(c -> c instanceof Map)
                    .map(c -> str((Map<String, Object>) c, "name"))
                    .filter(Objects::nonNull)
                    .toList();
            if (!componentNames.isEmpty()) {
                data.put("components", componentNames);
            }
        }

        // Fix versions
        Object fixVersionsObj = fields.get("fixVersions");
        if (fixVersionsObj instanceof List<?> fvList && !fvList.isEmpty()) {
            List<String> versionNames = fvList.stream()
                    .filter(v -> v instanceof Map)
                    .map(v -> str((Map<String, Object>) v, "name"))
                    .filter(Objects::nonNull)
                    .toList();
            if (!versionNames.isEmpty()) {
                data.put("fixVersions", versionNames);
            }
        }

        // Affects versions
        Object versionsObj = fields.get("versions");
        if (versionsObj instanceof List<?> vList && !vList.isEmpty()) {
            List<String> versionNames = vList.stream()
                    .filter(v -> v instanceof Map)
                    .map(v -> str((Map<String, Object>) v, "name"))
                    .filter(Objects::nonNull)
                    .toList();
            if (!versionNames.isEmpty()) {
                data.put("affectsVersions", versionNames);
            }
        }

        // Time tracking
        Object timeOriginal = fields.get("timeoriginalestimate");
        if (timeOriginal instanceof Number n && n.longValue() > 0) {
            data.put("original_estimate", n.longValue());
        }
        Object timeRemaining = fields.get("timeestimate");
        if (timeRemaining instanceof Number n && n.longValue() > 0) {
            data.put("remaining_estimate", n.longValue());
        }
        Object timeSpent = fields.get("timespent");
        if (timeSpent instanceof Number n && n.longValue() > 0) {
            data.put("time_spent", n.longValue());
        }

        // Subtasks tracking
        Object subtasksObj = fields.get("subtasks");
        if (subtasksObj instanceof List<?> subtaskList && !subtaskList.isEmpty()) {
            List<String> subtaskKeys = subtaskList.stream()
                    .filter(s -> s instanceof Map)
                    .map(s -> str((Map<String, Object>) s, "key"))
                    .filter(Objects::nonNull)
                    .toList();
            if (!subtaskKeys.isEmpty()) {
                data.put("subtaskKeys", subtaskKeys);
            }
        }

        // Issue links
        Object issueLinksObj = fields.get("issuelinks");
        if (issueLinksObj instanceof List<?> linkList && !linkList.isEmpty()) {
            List<Map<String, String>> links = new ArrayList<>();
            for (Object linkObj : linkList) {
                if (linkObj instanceof Map<?, ?> link) {
                    Map<String, String> linkData = new HashMap<>();
                    Map<String, Object> linkType = nested((Map<String, Object>) link, "type");
                    if (linkType != null) {
                        linkData.put("linkType", str(linkType, "name"));
                    }
                    Map<String, Object> inward = nested((Map<String, Object>) link, "inwardIssue");
                    Map<String, Object> outward = nested((Map<String, Object>) link, "outwardIssue");
                    if (outward != null) {
                        linkData.put("targetIssueKey", str(outward, "key"));
                        linkData.put("direction", "outward");
                    } else if (inward != null) {
                        linkData.put("targetIssueKey", str(inward, "key"));
                        linkData.put("direction", "inward");
                    }
                    if (linkData.containsKey("targetIssueKey")) {
                        links.add(linkData);
                    }
                }
            }
            if (!links.isEmpty()) {
                data.put("issueLinks", links);
            }
        }

        // Custom fields
        Map<String, Object> customFields = extractCustomFields(fields);
        if (!customFields.isEmpty()) {
            data.put("customFields", customFields);
        }

        // Story points (common custom field)
        String storyPoints = findStoryPoints(fields);
        if (storyPoints != null) {
            data.put("storyPoints", storyPoints);
        }

        // Epic link (common custom field)
        String epicLink = findEpicLink(fields);
        if (epicLink != null) {
            data.put("epicLink", epicLink);
        }

        return data;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toCommentData(Map<String, Object> jiraComment,
                                                     String issueKey, String issueId) {
        Map<String, Object> data = new HashMap<>();
        data.put("issueKey", issueKey);
        if (issueId != null) {
            data.put("issueId", issueId);
        }
        data.put("body", str(jiraComment, "body"));

        Map<String, Object> author = nested(jiraComment, "author");
        if (author == null) {
            author = nested(jiraComment, "updateAuthor");
        }
        if (author != null) {
            putIfPresent(data, "authorId", str(author, "name"));
        }
        putIfPresent(data, "createdAt", str(jiraComment, "created"));
        return data;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toAttachmentMetadata(Map<String, Object> jiraAttachment,
                                                            String issueKey, String issueId) {
        Map<String, Object> data = new HashMap<>();
        data.put("issueKey", issueKey);
        if (issueId != null) {
            data.put("issueId", issueId);
        }
        data.put("fileName", str(jiraAttachment, "filename"));
        data.put("mimeType", str(jiraAttachment, "mimeType"));
        data.put("contentUrl", str(jiraAttachment, "content"));

        Map<String, Object> author = nested(jiraAttachment, "author");
        if (author != null) {
            putIfPresent(data, "authorId", str(author, "name"));
        }
        putIfPresent(data, "createdAt", str(jiraAttachment, "created"));

        Object size = jiraAttachment.get("size");
        if (size instanceof Number n) {
            data.put("fileSize", n.longValue());
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toWorklogData(Map<String, Object> jiraWorklog,
                                                     String issueKey, String issueId) {
        Map<String, Object> data = new HashMap<>();
        data.put("issueKey", issueKey);
        if (issueId != null) {
            data.put("issueId", issueId);
        }
        putIfPresent(data, "timeSpentFormatted", str(jiraWorklog, "timeSpent"));

        Object seconds = jiraWorklog.get("timeSpentSeconds");
        if (seconds instanceof Number n) {
            data.put("timeSpentSeconds", n.intValue());
        }
        putIfPresent(data, "startedAt", str(jiraWorklog, "started"));
        putIfPresent(data, "comment", str(jiraWorklog, "comment"));

        Map<String, Object> author = nested(jiraWorklog, "author");
        if (author != null) {
            putIfPresent(data, "authorId", str(author, "name"));
        }
        return data;
    }

    public static Map<String, Object> toComponentData(Map<String, Object> jiraComponent,
                                                       String projectKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", str(jiraComponent, "name"));
        putIfPresent(data, "description", str(jiraComponent, "description"));
        data.put("projectKey", projectKey);
        return data;
    }

    public static Map<String, Object> toVersionData(Map<String, Object> jiraVersion,
                                                     String projectKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", str(jiraVersion, "name"));
        putIfPresent(data, "description", str(jiraVersion, "description"));
        data.put("projectKey", projectKey);
        data.put("released", Boolean.TRUE.equals(jiraVersion.get("released")));
        data.put("archived", Boolean.TRUE.equals(jiraVersion.get("archived")));
        putIfPresent(data, "releaseDate", str(jiraVersion, "releaseDate"));
        return data;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> toIssueLinkData(Map<String, Object> linkInfo,
                                                       String sourceIssueKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("sourceIssueKey", sourceIssueKey);
        putIfPresent(data, "targetIssueKey", str(linkInfo, "targetIssueKey"));
        putIfPresent(data, "linkType", str(linkInfo, "linkType"));
        return data;
    }

    // ========== Custom Field Extraction ==========

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractCustomFields(Map<String, Object> fields) {
        Map<String, Object> customFields = new HashMap<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || !key.startsWith("customfield_") || value == null) {
                continue;
            }
            String fieldName = FIELD_NAME_BY_ID.getOrDefault(key, key);
            if (value instanceof Map<?, ?> mapVal) {
                Object displayValue = ((Map<String, Object>) mapVal).get("value");
                if (displayValue != null) {
                    customFields.put(key, displayValue.toString());
                    customFields.put(key + ".name", fieldName);
                }
            } else if (value instanceof List<?> listVal && !listVal.isEmpty()) {
                List<String> values = listVal.stream()
                        .map(item -> {
                            if (item instanceof Map<?, ?> m) {
                                Object v = ((Map<String, Object>) m).get("value");
                                return v != null ? v.toString() : null;
                            }
                            return item != null ? item.toString() : null;
                        })
                        .filter(Objects::nonNull)
                        .toList();
                if (!values.isEmpty()) {
                    customFields.put(key, String.join(",", values));
                    customFields.put(key + ".name", fieldName);
                }
            } else if (value instanceof String s && !s.isBlank()) {
                customFields.put(key, s);
                customFields.put(key + ".name", fieldName);
            } else if (value instanceof Number) {
                customFields.put(key, value.toString());
                customFields.put(key + ".name", fieldName);
            }
        }
        return customFields;
    }

    private static String findStoryPoints(Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("customfield_") && entry.getValue() instanceof Number n) {
                String name = FIELD_NAME_BY_ID.get(key);
                if ("Story Points".equalsIgnoreCase(name) || "Story point estimate".equalsIgnoreCase(name)) {
                    return String.valueOf(n.intValue());
                }
            }
        }
        return null;
    }

    private static String findEpicLink(Map<String, Object> fields) {
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("customfield_") && entry.getValue() instanceof String s && !s.isBlank()) {
                String name = FIELD_NAME_BY_ID.get(key);
                if ("Epic Link".equalsIgnoreCase(name)) {
                    return s;
                }
            }
        }
        return null;
    }

    // ========== Helpers ==========

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nested(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Map ? (Map<String, Object>) val : null;
    }

    private static void putNestedName(Map<String, Object> target, String targetKey,
                                       Map<String, Object> source, String sourceKey) {
        Map<String, Object> nested = nested(source, sourceKey);
        if (nested != null) {
            String name = str(nested, "name");
            if (name != null) {
                target.put(targetKey, name);
            }
        }
    }

    private static void putUserName(Map<String, Object> target, String targetKey,
                                     Map<String, Object> source, String sourceKey) {
        Map<String, Object> user = nested(source, sourceKey);
        if (user != null) {
            String name = str(user, "name");
            if (name == null) {
                name = str(user, "key");
            }
            if (name != null) {
                target.put(targetKey, name);
            }
        }
    }

    private static String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
