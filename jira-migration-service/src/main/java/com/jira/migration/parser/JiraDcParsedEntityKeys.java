package com.jira.migration.parser;

import java.util.Map;
import java.util.UUID;

/**
 * Stable entity keys for parsed DC entities (RSS and Entity backup formats).
 */
public final class JiraDcParsedEntityKeys {

    private JiraDcParsedEntityKeys() {
    }

    public static String generate(JiraDcXmlParser.ParsedEntity entity) {
        Map<String, String> fields = entity.getFields() != null ? entity.getFields() : Map.of();
        return switch (entity.getEntityType()) {
            case "Project" -> fields.getOrDefault("key", UUID.randomUUID().toString());
            case "Issue", "SubTask" -> firstNonBlank(fields, "issueKey", "issue_key")
                    != null ? firstNonBlank(fields, "issueKey", "issue_key")
                    : projectDashId(fields);
            case "Comment" -> {
                String issue = firstNonBlank(fields, "issue", "issueKey", "issueId");
                yield (issue != null ? issue : "unknown") + ":comment:"
                        + fields.getOrDefault("id", fields.getOrDefault("sourceCommentId", "1"));
            }
            case "Attachment" -> {
                String issue = firstNonBlank(fields, "issue", "issueKey", "issueId");
                String file = firstNonBlank(fields, "filename", "fileName", "name");
                String attId = fields.getOrDefault("sourceAttachmentId", "");
                yield (issue != null ? issue : "unknown") + ":att:"
                        + (!attId.isBlank() ? attId + ":" : "")
                        + (file != null ? file : "file");
            }
            case "User" -> fields.getOrDefault("userKey",
                    fields.getOrDefault("lowerUserName", UUID.randomUUID().toString()));
            default -> entity.getEntityType() + "-" + UUID.randomUUID().toString().substring(0, 8);
        };
    }

    private static String projectDashId(Map<String, String> fields) {
        String pkey = fields.getOrDefault("project", "");
        String issueNum = fields.getOrDefault("id", UUID.randomUUID().toString().substring(0, 8));
        return pkey + "-" + issueNum;
    }

    private static String firstNonBlank(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            String v = fields.get(key);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /** Extracts project key prefix from issue key e.g. ENT-1024 → ENT. */
    public static String projectKeyFromIssueKey(String issueKey) {
        if (issueKey == null || issueKey.isBlank()) {
            return null;
        }
        int dash = issueKey.indexOf('-');
        if (dash <= 0) {
            return issueKey;
        }
        return issueKey.substring(0, dash);
    }
}
