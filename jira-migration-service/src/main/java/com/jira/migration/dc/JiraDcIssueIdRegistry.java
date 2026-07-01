package com.jira.migration.dc;

import com.jira.migration.parser.JiraDcParsedEntityKeys;
import com.jira.migration.parser.JiraDcXmlParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps Jira DC numeric issue ids to issue keys for comment/attachment/history resolution.
 */
public final class JiraDcIssueIdRegistry {

    private final Map<String, String> idToKey = new HashMap<>();
    private final Map<String, String> keyToId = new HashMap<>();

    public static JiraDcIssueIdRegistry fromEntities(List<JiraDcXmlParser.ParsedEntity> entities) {
        JiraDcIssueIdRegistry registry = new JiraDcIssueIdRegistry();
        for (JiraDcXmlParser.ParsedEntity e : entities) {
            if (!"Issue".equals(e.getEntityType()) && !"SubTask".equals(e.getEntityType())) {
                continue;
            }
            Map<String, String> f = e.getFields();
            if (f == null) {
                continue;
            }
            String key = f.getOrDefault("issueKey", e.getEntityKey());
            String id = f.get("id");
            if (key != null && id != null) {
                registry.idToKey.put(id, key);
                registry.keyToId.put(key, id);
            }
        }
        return registry;
    }

    public String resolveIssueKey(String issueRef) {
        if (issueRef == null || issueRef.isBlank()) {
            return null;
        }
        if (issueRef.contains("-")) {
            return issueRef;
        }
        return idToKey.getOrDefault(issueRef, issueRef);
    }

    public String issueIdForKey(String issueKey) {
        return keyToId.get(issueKey);
    }

    public Map<String, String> idToKeyView() {
        return Map.copyOf(idToKey);
    }
}
