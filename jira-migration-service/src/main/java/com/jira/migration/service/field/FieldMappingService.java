package com.jira.migration.service.field;

import com.jira.migration.entity.field.FieldDefinition;
import com.jira.migration.entity.field.PluginFieldRegistry;
import com.jira.migration.repository.field.FieldDefinitionRepository;
import com.jira.migration.repository.field.PluginFieldRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for mapping import fields to existing field definitions.
 * Supports exact match, semantic match, alias match, and plugin namespace matching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FieldMappingService {

    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final PluginFieldRegistryRepository pluginFieldRegistryRepository;

    private static final Map<String, String> EXACT_MAPPINGS = Map.ofEntries(
            Map.entry("summary", "summary"),
            Map.entry("title", "summary"),
            Map.entry("subject", "summary"),
            Map.entry("description", "description"),
            Map.entry("desc", "description"),
            Map.entry("environment", "environment"),
            Map.entry("env", "environment"),
            Map.entry("issuetype", "issue_type"),
            Map.entry("issue_type", "issue_type"),
            Map.entry("type", "issue_type"),
            Map.entry("type_name", "issue_type"),
            Map.entry("status", "status"),
            Map.entry("status_name", "status"),
            Map.entry("priority", "priority"),
            Map.entry("priority_name", "priority"),
            Map.entry("resolution", "resolution"),
            Map.entry("resolution_name", "resolution"),
            Map.entry("assignee", "assignee"),
            Map.entry("assignee_name", "assignee"),
            Map.entry("reporter", "reporter"),
            Map.entry("reporter_name", "reporter"),
            Map.entry("creator", "creator"),
            Map.entry("created", "created"),
            Map.entry("created_at", "created"),
            Map.entry("creation_date", "created"),
            Map.entry("updated", "updated"),
            Map.entry("updated_at", "updated"),
            Map.entry("modified_date", "updated"),
            Map.entry("duedate", "due_date"),
            Map.entry("due_date", "due_date"),
            Map.entry("resolutiondate", "resolution_date"),
            Map.entry("resolution_date", "resolution_date"),
            Map.entry("resolved_at", "resolution_date"),
            Map.entry("labels", "labels"),
            Map.entry("label", "labels"),
            Map.entry("components", "components"),
            Map.entry("component", "components"),
            Map.entry("versions", "versions"),
            Map.entry("affects_versions", "affects_versions"),
            Map.entry("affectsversion", "affects_versions"),
            Map.entry("fix_versions", "fix_versions"),
            Map.entry("fixversion", "fix_versions"),
            Map.entry("security", "security_level"),
            Map.entry("security_level", "security_level"),
            Map.entry("project", "project"),
            Map.entry("project_key", "project_key"),
            Map.entry("parent", "parent"),
            Map.entry("parent_issue", "parent"),
            Map.entry("epic", "epic_link"),
            Map.entry("epic_link", "epic_link"),
            Map.entry("epiclink", "epic_link"),
            Map.entry("epic_name", "epic_name"),
            Map.entry("epicname", "epic_name"),
            Map.entry("epic_color", "epic_color"),
            Map.entry("epiccolor", "epic_color"),
            Map.entry("story_points", "story_points"),
            Map.entry("storypoints", "story_points"),
            Map.entry("storypoint", "story_points"),
            Map.entry("rank", "rank"),
            Map.entry("sprint", "sprint"),
            Map.entry("sprint_name", "sprint"),
            Map.entry("original_estimate", "original_estimate"),
            Map.entry("originalestimate", "original_estimate"),
            Map.entry("timeestimate", "original_estimate"),
            Map.entry("remaining_estimate", "remaining_estimate"),
            Map.entry("remainingestimate", "remaining_estimate"),
            Map.entry("timespent", "time_spent"),
            Map.entry("time_spent", "time_spent"),
            Map.entry("votes", "votes"),
            Map.entry("vote_count", "votes"),
            Map.entry("watchers", "watchers"),
            Map.entry("watcher_count", "watchers"),
            Map.entry("attachments", "attachments"),
            Map.entry("attachment", "attachments"),
            Map.entry("linked_issues", "linked_issues"),
            Map.entry("linkedissues", "linked_issues"),
            Map.entry("subtasks", "subtasks"),
            Map.entry("comments", "comments"),
            Map.entry("comment", "comments"),
            Map.entry("worklog", "worklog"),
            Map.entry("work_logs", "worklog"),
            Map.entry("issue_key", "issue_key"),
            Map.entry("issuekey", "issue_key"),
            Map.entry("issue_id", "issue_id"),
            Map.entry("issueid", "issue_id"),
            Map.entry("last_viewed", "last_viewed"),
            Map.entry("lastviewed", "last_viewed"),
            Map.entry("project_name", "project_name"),
            Map.entry("projectname", "project_name"),
            Map.entry("project_type", "project_type"),
            Map.entry("project_lead", "project_lead"),
            Map.entry("project_description", "project_description"),
            Map.entry("project_url", "project_url"),
            Map.entry("affects_version_s", "affects_versions"),
            Map.entry("fix_version_s", "fix_versions"),
            Map.entry("component_s", "components"),
            Map.entry("log_work", "worklog"),
            Map.entry("original_story_points", "story_points"),
            Map.entry("target_end", "target_end"),
            Map.entry("target_start", "target_start"),
            Map.entry("parent_link", "parent")
    );

    private static final Map<String, String> SEMANTIC_MAPPINGS = Map.ofEntries(
            Map.entry("name", "summary"),
            Map.entry("headline", "summary"),
            Map.entry("topic", "summary"),
            Map.entry("body", "description"),
            Map.entry("content", "description"),
            Map.entry("details", "description"),
            Map.entry("notes", "description"),
            Map.entry("steps_to_reproduce", "description"),
            Map.entry("bug_description", "description"),
            Map.entry("feature_description", "description"),
            Map.entry("acceptance_criteria", "description"),
            Map.entry("issue_status", "status"),
            Map.entry("current_status", "status"),
            Map.entry("state", "status"),
            Map.entry("workflow_status", "status"),
            Map.entry("severity", "priority"),
            Map.entry("urgency", "priority"),
            Map.entry("impact", "priority"),
            Map.entry("fix_status", "resolution"),
            Map.entry("close_reason", "resolution"),
            Map.entry("handler", "assignee"),
            Map.entry("assigned_to", "assignee"),
            Map.entry("owner", "assignee"),
            Map.entry("submitter", "reporter"),
            Map.entry("submitted_by", "reporter"),
            Map.entry("author", "creator"),
            Map.entry("opened_by", "creator"),
            Map.entry("creation_time", "created"),
            Map.entry("created_on", "created"),
            Map.entry("last_modified", "updated"),
            Map.entry("modified", "updated"),
            Map.entry("modified_on", "updated"),
            Map.entry("last_change", "updated"),
            Map.entry("target_date", "due_date"),
            Map.entry("deadline", "due_date"),
            Map.entry("finish_date", "due_date"),
            Map.entry("end_date", "due_date"),
            Map.entry("closed_date", "resolution_date"),
            Map.entry("tags", "labels"),
            Map.entry("tag", "labels"),
            Map.entry("categories", "components"),
            Map.entry("category", "components"),
            Map.entry("module", "components"),
            Map.entry("affected_versions", "affects_versions"),
            Map.entry("affected_releases", "affects_versions"),
            Map.entry("target_versions", "fix_versions"),
            Map.entry("target_release", "fix_versions"),
            Map.entry("fixed_in", "fix_versions"),
            Map.entry("release", "fix_versions"),
            Map.entry("access_level", "security_level"),
            Map.entry("permission", "security_level"),
            Map.entry("privacy", "security_level"),
            Map.entry("base_project", "project"),
            Map.entry("project_name", "project"),
            Map.entry("epic_link", "epic_link"),
            Map.entry("parent_issue", "parent"),
            Map.entry("parent_story", "parent"),
            Map.entry("blocking_issue", "parent"),
            Map.entry("effort", "story_points"),
            Map.entry("story_point", "story_points"),
            Map.entry("points", "story_points"),
            Map.entry("estimation", "story_points"),
            Map.entry("size", "story_points"),
            Map.entry("agile_sprint", "sprint"),
            Map.entry("iteration", "sprint"),
            Map.entry("planned_time", "original_estimate"),
            Map.entry("budgeted_hours", "original_estimate"),
            Map.entry("initially_estimates", "original_estimate"),
            Map.entry("remaining_time", "remaining_estimate"),
            Map.entry("left_to_do", "remaining_estimate"),
            Map.entry("still_to_do", "remaining_estimate"),
            Map.entry("actual_hours", "time_spent"),
            Map.entry("logged_hours", "time_spent"),
            Map.entry("actual_time", "time_spent"),
            Map.entry("work_done", "time_spent"),
            Map.entry("thumbs_up", "votes"),
            Map.entry("supporters", "votes"),
            Map.entry("interested", "watchers"),
            Map.entry("followers", "watchers"),
            Map.entry("files", "attachments"),
            Map.entry("uploads", "attachments"),
            Map.entry("related_issues", "linked_issues"),
            Map.entry("links", "linked_issues"),
            Map.entry("children", "subtasks"),
            Map.entry("child_issues", "subtasks"),
            Map.entry("feedback", "comments"),
            Map.entry("remarks", "comments"),
            Map.entry("log_entries", "worklog"),
            Map.entry("work_items", "worklog")
    );

    private static final Map<String, List<String>> FIELD_ALIASES = Map.ofEntries(
            Map.entry("summary", List.of("title", "subject", "name", "headline", "topic")),
            Map.entry("description", List.of("body", "content", "details", "notes", "desc")),
            Map.entry("environment", List.of("env", "platform", "system", "os", "browser")),
            Map.entry("issue_type", List.of("type", "issueType", "type_name", "issuetype")),
            Map.entry("status", List.of("state", "issue_status", "current_status", "workflow_status")),
            Map.entry("priority", List.of("severity", "urgency", "impact", "importance")),
            Map.entry("resolution", List.of("fix_status", "close_reason", "outcome")),
            Map.entry("assignee", List.of("handler", "assigned_to", "owner", "responsible")),
            Map.entry("reporter", List.of("submitter", "submitted_by", "opened_by")),
            Map.entry("labels", List.of("tag", "tags", "categories", "keywords")),
            Map.entry("components", List.of("component", "module", "category")),
            Map.entry("affects_versions", List.of("affected_versions", "affected_releases")),
            Map.entry("fix_versions", List.of("target_versions", "target_release", "fixed_in")),
            Map.entry("story_points", List.of("effort", "estimation", "points", "size")),
            Map.entry("original_estimate", List.of("planned_time", "budgeted_hours", "initially_estimates")),
            Map.entry("remaining_estimate", List.of("remaining_time", "left_to_do", "still_to_do")),
            Map.entry("time_spent", List.of("actual_hours", "logged_hours", "actual_time", "work_done"))
    );

    private static final Map<String, String> PLUGIN_TYPE_MAPPINGS = Map.ofEntries(
            Map.entry("tempo:worklog", "worklog"),
            Map.entry("tempo:timesheet", "time_tracking"),
            Map.entry("xray:test_coverage", "labels"),
            Map.entry("xray:test_set", "labels"),
            Map.entry("zephyr:cycle", "sprint"),
            Map.entry("zephyr:version", "fix_versions"),
            Map.entry("structure:outline", "parent"),
            Map.entry("structure:hierarchy", "parent"),
            Map.entry("bigpicture:epic", "epic_link"),
            Map.entry("bigpicture:bucket", "labels")
    );

    public record FieldMapping(
            String sourceKey,
            String targetKey,
            MappingConfidence confidence,
            MappingStrategy strategy,
            String pluginSource,
            Map<String, Object> mappingConfig
    ) {}

    public enum MappingConfidence {
        EXACT(1.0),
        HIGH(0.9),
        MEDIUM(0.7),
        LOW(0.5),
        NONE(0.0);

        private final double score;

        MappingConfidence(double score) {
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }

    public enum MappingStrategy {
        EXACT_MATCH,
        SEMANTIC_MATCH,
        ALIAS_MATCH,
        PLUGIN_NAMESPACE_MATCH,
        INFERRED,
        UNMAPPED
    }

    public record MappingResult(
            List<FieldMapping> mappings,
            List<FieldMapping> unmappedFields,
            List<FieldMapping> highConfidenceMappings,
            List<FieldMapping> lowConfidenceMappings,
            Map<String, String> pluginFieldMappings,
            double averageConfidence
    ) {}

    public MappingResult mapFields(List<String> sourceFieldKeys) {
        log.info("Mapping {} source fields to target field definitions", sourceFieldKeys.size());

        List<FieldMapping> allMappings = new ArrayList<>();
        Map<String, String> pluginMappings = new HashMap<>();

        for (String sourceKey : sourceFieldKeys) {
            FieldMapping mapping = mapField(sourceKey);
            allMappings.add(mapping);

            if (mapping.pluginSource() != null) {
                pluginMappings.put(sourceKey, mapping.pluginSource());
            }
        }

        List<FieldMapping> unmapped = allMappings.stream()
                .filter(m -> m.strategy() == MappingStrategy.UNMAPPED)
                .toList();

        List<FieldMapping> highConfidence = allMappings.stream()
                .filter(m -> m.confidence().getScore() >= 0.7)
                .toList();

        List<FieldMapping> lowConfidence = allMappings.stream()
                .filter(m -> m.confidence().getScore() < 0.7 && m.strategy() != MappingStrategy.UNMAPPED)
                .toList();

        double avgConfidence = allMappings.stream()
                .mapToDouble(m -> m.confidence().getScore())
                .average()
                .orElse(0.0);

        log.info("Mapping complete: {} high confidence, {} low confidence, {} unmapped",
                highConfidence.size(), lowConfidence.size(), unmapped.size());

        return new MappingResult(
                allMappings, unmapped, highConfidence, lowConfidence, pluginMappings, avgConfidence
        );
    }

    public FieldMapping mapField(String sourceKey) {
        String normalizedSource = normalizeKey(sourceKey);

        if (EXACT_MAPPINGS.containsKey(normalizedSource)) {
            String targetKey = EXACT_MAPPINGS.get(normalizedSource);
            return new FieldMapping(
                    sourceKey, targetKey, MappingConfidence.EXACT,
                    MappingStrategy.EXACT_MATCH, null, Map.of()
            );
        }

        Optional<FieldDefinition> existingDef = fieldDefinitionRepository.findByFieldKey(normalizedSource);
        if (existingDef.isPresent()) {
            return new FieldMapping(
                    sourceKey, normalizedSource, MappingConfidence.EXACT,
                    MappingStrategy.EXACT_MATCH, existingDef.get().getPluginSource(), Map.of()
            );
        }

        boolean isCustomField = normalizedSource.startsWith("custom_field_");

        if (!isCustomField) {
            for (Map.Entry<String, String> entry : SEMANTIC_MAPPINGS.entrySet()) {
                if (normalizedSource.contains(entry.getKey()) || entry.getKey().contains(normalizedSource)) {
                    return new FieldMapping(
                            sourceKey, entry.getValue(), MappingConfidence.MEDIUM,
                            MappingStrategy.SEMANTIC_MATCH, null, Map.of("matchedPattern", entry.getKey())
                    );
                }
            }

            for (Map.Entry<String, List<String>> entry : FIELD_ALIASES.entrySet()) {
                for (String alias : entry.getValue()) {
                    if (normalizedSource.equalsIgnoreCase(alias) ||
                            normalizedSource.contains(alias) ||
                            alias.contains(normalizedSource)) {
                        return new FieldMapping(
                                sourceKey, entry.getKey(), MappingConfidence.HIGH,
                                MappingStrategy.ALIAS_MATCH, null, Map.of("matchedAlias", alias)
                        );
                    }
                }
            }
        }

        Optional<PluginFieldRegistry> pluginMapping = findPluginNamespaceMapping(normalizedSource);
        if (pluginMapping.isPresent()) {
            String jiraKey = pluginMapping.get().getJiraFieldKey();
            if (jiraKey != null) {
                return new FieldMapping(
                        sourceKey, jiraKey, MappingConfidence.HIGH,
                        MappingStrategy.PLUGIN_NAMESPACE_MATCH,
                        pluginMapping.get().getPluginKey(),
                        Map.of("pluginMapping", pluginMapping.get().getSchemaMapping())
                );
            }
        }

        String inferredKey = inferFieldKey(normalizedSource);
        if (inferredKey != null) {
            return new FieldMapping(
                    sourceKey, inferredKey, MappingConfidence.LOW,
                    MappingStrategy.INFERRED, null, Map.of()
            );
        }

        return new FieldMapping(
                sourceKey, normalizedSource, MappingConfidence.NONE,
                MappingStrategy.UNMAPPED, null, Map.of()
        );
    }

    private Optional<PluginFieldRegistry> findPluginNamespaceMapping(String normalizedKey) {
        for (Map.Entry<String, String> entry : PLUGIN_TYPE_MAPPINGS.entrySet()) {
            if (normalizedKey.contains(entry.getKey())) {
                String pluginKey = entry.getKey().split(":")[0];
                String fieldKey = entry.getKey().split(":")[1];

                return pluginFieldRegistryRepository.findByPluginKeyAndFieldKey(pluginKey, fieldKey);
            }
        }

        if (normalizedKey.startsWith("customfield_")) {
            String cfId = normalizedKey.replace("customfield_", "");
            return pluginFieldRegistryRepository.findByPluginKeyAndFieldKey("custom", cfId);
        }

        if (normalizedKey.startsWith("cf_")) {
            String cfId = normalizedKey.replace("cf_", "");
            return pluginFieldRegistryRepository.findByPluginKeyAndFieldKey("custom", cfId);
        }

        return Optional.empty();
    }

    private String inferFieldKey(String normalizedKey) {
        if (normalizedKey.startsWith("custom_field_")) {
            return null;
        }
        if (normalizedKey.contains("date") || normalizedKey.contains("time")) {
            if (normalizedKey.contains("due")) return "due_date";
            if (normalizedKey.contains("created")) return "created";
            if (normalizedKey.contains("updated")) return "updated";
            if (normalizedKey.contains("resolved")) return "resolution_date";
            return "due_date";
        }

        if (normalizedKey.contains("user") || normalizedKey.contains("person")) {
            if (normalizedKey.contains("assign")) return "assignee";
            if (normalizedKey.contains("report")) return "reporter";
            if (normalizedKey.contains("create")) return "creator";
        }

        if (normalizedKey.contains("name") || normalizedKey.contains("title")) {
            return "summary";
        }

        if (normalizedKey.contains("text") || normalizedKey.contains("content") || normalizedKey.contains("note")) {
            return "description";
        }

        if (normalizedKey.contains("number") || normalizedKey.contains("count")) {
            if (normalizedKey.contains("story")) return "story_points";
        }

        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) return "";

        return key.trim().toLowerCase()
                .replace(" ", "_")
                .replace("-", "_")
                .replace(".", "_")
                .replace("[", "_")
                .replace("]", "")
                .replace("__", "_");
    }

    public Map<String, String> createBulkMappingConfig(List<FieldMapping> mappings) {
        Map<String, String> config = new HashMap<>();

        for (FieldMapping mapping : mappings) {
            if (mapping.strategy() != MappingStrategy.UNMAPPED) {
                config.put(mapping.sourceKey(), mapping.targetKey());
            }
        }

        return config;
    }

    public List<String> suggestFieldMappings(String partialKey) {
        List<String> suggestions = new ArrayList<>();
        String normalized = normalizeKey(partialKey);

        fieldDefinitionRepository.findAll().forEach(def -> {
            if (def.getFieldKey().contains(normalized) ||
                    def.getDisplayName().toLowerCase().contains(normalized)) {
                suggestions.add(def.getFieldKey());
            }
        });

        return suggestions;
    }

    public MappingConfidence calculateConfidence(FieldMapping mapping, Object sampleValue) {
        if (mapping.confidence() == MappingConfidence.EXACT) {
            return MappingConfidence.EXACT;
        }

        if (sampleValue == null) {
            return mapping.confidence();
        }

        if (mapping.targetKey().equals("status") && sampleValue instanceof String statusValue) {
            if (List.of("open", "closed", "resolved", "in progress", "to do", "done").stream()
                    .anyMatch(s -> statusValue.equalsIgnoreCase(s))) {
                return MappingConfidence.HIGH;
            }
        }

        if (mapping.targetKey().equals("priority") && sampleValue instanceof String priorityValue) {
            if (List.of("highest", "high", "medium", "low", "lowest").stream()
                    .anyMatch(p -> priorityValue.equalsIgnoreCase(p))) {
                return MappingConfidence.HIGH;
            }
        }

        return mapping.confidence();
    }
}