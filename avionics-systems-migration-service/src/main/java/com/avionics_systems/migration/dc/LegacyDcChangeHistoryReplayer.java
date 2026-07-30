package com.avionics_systems.migration.dc;

import com.avionics_systems.migration.parser.LegacyDcXmlParser;
import com.avionics_systems.migration.service.clients.IssueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Replays imported DC change history without workflow transitions.
 * Groups ChangeItems by change group / issue for batched replay.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegacyDcChangeHistoryReplayer {

    private final IssueServiceClient issueServiceClient;

    public int replay(UUID jobId, List<LegacyDcXmlParser.ParsedEntity> histories,
                      Map<String, String> issueKeyToTargetId, boolean stub) {
        return replay(jobId, histories, issueKeyToTargetId, null, stub);
    }

    public int replay(UUID jobId, List<LegacyDcXmlParser.ParsedEntity> histories,
                      Map<String, String> issueKeyToTargetId,
                      LegacyDcIssueIdRegistry idRegistry,
                      boolean stub) {
        if (stub || histories == null || histories.isEmpty()) {
            return histories != null ? histories.size() : 0;
        }

        Map<String, List<LegacyDcXmlParser.ParsedEntity>> byGroup = histories.stream()
                .filter(e -> "History".equals(e.getEntityType()))
                .collect(Collectors.groupingBy(e -> groupKey(e, idRegistry)));

        int count = 0;
        for (Map.Entry<String, List<LegacyDcXmlParser.ParsedEntity>> entry : byGroup.entrySet()) {
            String[] parts = entry.getKey().split("::", 2);
            if (parts.length < 2) {
                continue;
            }
            String issueKey = parts[0];
            String targetId = issueKeyToTargetId.get(issueKey);
            if (targetId == null) {
                continue;
            }
            try {
                Map<String, Object> request = buildBatchRequest(entry.getValue(), idRegistry);
                if (!request.isEmpty()) {
                    issueServiceClient.recordChangeHistory(targetId, request);
                    count += entry.getValue().size();
                }
            } catch (Exception e) {
                log.warn("History replay failed for {}: {}", issueKey, e.getMessage());
            }
        }
        return count;
    }

    private static String groupKey(LegacyDcXmlParser.ParsedEntity entity, LegacyDcIssueIdRegistry idRegistry) {
        Map<String, String> f = entity.getFields() != null ? entity.getFields() : Map.of();
        String issueKey = first(f, "issueKey", "issue", "issueId");
        if (issueKey != null && idRegistry != null && !issueKey.contains("-")) {
            issueKey = idRegistry.resolveIssueKey(issueKey);
        }
        String groupId = first(f, "sourceHistoryId", "id", "changeGroupId");
        if (groupId == null) {
            groupId = "default";
        }
        return (issueKey != null ? issueKey : "?") + "::" + groupId;
    }

    private static Map<String, Object> buildBatchRequest(List<LegacyDcXmlParser.ParsedEntity> items,
                                                         LegacyDcIssueIdRegistry idRegistry) {
        if (items.isEmpty()) {
            return Map.of();
        }
        Map<String, String> firstFields = items.get(0).getFields() != null
                ? items.get(0).getFields() : Map.of();

        Map<String, Object> request = new HashMap<>();
        request.put("authorName", first(firstFields, "author", "authorKey", "userKey"));
        if (request.get("authorName") == null) {
            request.put("authorName", "migration-import");
        }
        request.put("created", first(firstFields, "created", "createdDate"));
        request.put("historyOnly", true);

        List<Map<String, String>> changes = new ArrayList<>();
        for (LegacyDcXmlParser.ParsedEntity item : items) {
            Map<String, String> f = item.getFields() != null ? item.getFields() : Map.of();
            Map<String, String> change = new HashMap<>();
            change.put("fieldType", "avionics-systems");
            change.put("field", first(f, "field", "fieldname", "fieldName"));
            change.put("oldString", first(f, "old", "oldValue", "oldstring", "oldString"));
            change.put("newString", first(f, "new", "newValue", "newstring", "newString"));
            if (change.get("field") != null) {
                changes.add(change);
            }
        }
        if (changes.isEmpty()) {
            return Map.of();
        }
        request.put("changes", changes);
        return request;
    }

    private static String first(Map<String, String> f, String... keys) {
        for (String k : keys) {
            if (f.containsKey(k) && f.get(k) != null && !f.get(k).isBlank()) {
                return f.get(k);
            }
        }
        return null;
    }
}
