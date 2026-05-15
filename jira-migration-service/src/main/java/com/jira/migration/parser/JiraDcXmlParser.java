package com.jira.migration.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jira.migration.dto.ValidationResult;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.entity.UserMapping;
import com.jira.migration.exception.EntityNotFoundException;
import com.jira.migration.exception.MigrationException;
import com.jira.migration.exception.ValidationException;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.ProjectMappingRepository;
import com.jira.migration.repository.UserMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JiraDcXmlParser {

    private final ValidationEngine validationEngine;
    private final ProjectMappingRepository projectMappingRepository;
    private final UserMappingRepository userMappingRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> JIRA_DC_TO_PLATFORM_TABLE_MAP = Map.ofEntries(
            Map.entry("Project", "projects"),
            Map.entry("Issue", "issues"),
            Map.entry("User", "users"),
            Map.entry("Group", "groups"),
            Map.entry("IssueType", "issue_types"),
            Map.entry("Status", "statuses"),
            Map.entry("Priority", "priorities"),
            Map.entry("Resolution", "resolutions"),
            Map.entry("CustomField", "custom_fields"),
            Map.entry("Workflow", "workflows"),
            Map.entry("Scheme", "schemes"),
            Map.entry("Component", "components"),
            Map.entry("Version", "versions"),
            Map.entry("Label", "labels"),
            Map.entry("Attachment", "attachments"),
            Map.entry("Comment", "comments"),
            Map.entry("Worklog", "worklogs"),
            Map.entry("Vote", "votes"),
            Map.entry("Watcher", "watchers")
    );

    public static class JiraXmlBackup {
        public JiraXmlBackup.BackupInfo backupInfo;
        public List<JiraXmlBackup.EntityGroup> entityGroups;

        @lombok.Data
        public static class BackupInfo {
            public String jiraVersion;
            public String exportDate;
            public String exportUser;
        }

        @lombok.Data
        public static class EntityGroup {
            public String groupName;
            public List<EntityRecord> entities;
        }

        @lombok.Data
        public static class EntityRecord {
            public String entityName;
            public Map<String, String> fields;
        }
    }

    public ParseResult parseXmlBackup(String xmlContent, UUID jobId) {
        log.info("Parsing Jira DC XML backup for job: {}", jobId);

        try {
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.registerModule(new JavaTimeModule());
            xmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Parse XML structure
            // Note: In production, use proper XML parsing with namespace handling
            Map<String, Object> parsed = xmlMapper.readValue(xmlContent, Map.class);

            ParseResult result = new ParseResult();
            result.setTotalEntities(countEntities(parsed));

            // Map entities
            result.setEntities(mapEntities(parsed));

            log.info("Parsed {} entities from XML backup", result.getTotalEntities());
            return result;

        } catch (Exception e) {
            log.error("Failed to parse XML backup", e);
            throw new MigrationException("Failed to parse XML backup: " + e.getMessage(), e);
        }
    }

    private int countEntities(Map<String, Object> parsed) {
        int count = 0;
        if (parsed.containsKey("Entity")) {
            Object entity = parsed.get("Entity");
            if (entity instanceof List) {
                count = ((List<?>) entity).size();
            } else if (entity instanceof Map) {
                count = 1;
            }
        }
        return count;
    }

    private List<ParsedEntity> mapEntities(Map<String, Object> parsed) {
        List<ParsedEntity> entities = new ArrayList<>();

        if (parsed.containsKey("Entity")) {
            Object entity = parsed.get("Entity");
            if (entity instanceof List) {
                for (Object e : (List<?>) entity) {
                    if (e instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> entityMap = (Map<String, Object>) e;
                        entities.add(convertToParsedEntity(entityMap));
                    }
                }
            } else if (entity instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entityMap = (Map<String, Object>) entity;
                entities.add(convertToParsedEntity(entityMap));
            }
        }

        return entities;
    }

    private ParsedEntity convertToParsedEntity(Map<String, Object> entityMap) {
        ParsedEntity entity = new ParsedEntity();
        entity.setEntityType((String) entityMap.get("entityName"));
        entity.setEntityKey(generateEntityKey(entity));

        Map<String, String> fields = new HashMap<>();
        if (entityMap.containsKey("field")) {
            Object fieldObj = entityMap.get("field");
            if (fieldObj instanceof List) {
                for (Object f : (List<?>) fieldObj) {
                    if (f instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fieldMap = (Map<String, Object>) f;
                        String name = (String) fieldMap.get("name");
                        String value = (String) fieldMap.get("value");
                        if (name != null && value != null) {
                            fields.put(name, value);
                        }
                    }
                }
            } else if (fieldObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fieldMap = (Map<String, Object>) fieldObj;
                String name = (String) fieldMap.get("name");
                Object value = fieldMap.get("value");
                if (name != null && value != null) {
                    fields.put(name, value.toString());
                }
            }
        }

        entity.setFields(fields);
        return entity;
    }

    private String generateEntityKey(ParsedEntity entity) {
        Map<String, String> fields = entity.getFields();
        switch (entity.getEntityType()) {
            case "Project":
                return fields.getOrDefault("key", UUID.randomUUID().toString());
            case "Issue":
                String pkey = fields.getOrDefault("project", "");
                String issueNum = fields.getOrDefault("id", UUID.randomUUID().toString().substring(0, 8));
                return pkey + "-" + issueNum;
            case "User":
                return fields.getOrDefault("userKey", fields.getOrDefault("lowerUserName", UUID.randomUUID().toString()));
            default:
                return entity.getEntityType() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    public String mapToPlatformEntity(ParsedEntity entity, UUID jobId) {
        String entityType = entity.getEntityType();

        // Map based on entity type
        switch (entityType) {
            case "Project":
                return mapProject(entity, jobId);
            case "Issue":
                return mapIssue(entity, jobId);
            case "User":
                return mapUser(entity, jobId);
            case "IssueType":
                return mapIssueType(entity);
            case "Status":
                return mapStatus(entity);
            case "Priority":
                return mapPriority(entity);
            default:
                log.warn("Unknown entity type for mapping: {}", entityType);
                return null;
        }
    }

    private String mapProject(ParsedEntity entity, UUID jobId) {
        Map<String, String> fields = entity.getFields();
        String sourceKey = fields.get("key");

        // Check if project already mapped
        Optional<ProjectMapping> existing = projectMappingRepository.findByJobIdAndSourceKey(jobId, sourceKey);
        if (existing.isPresent()) {
            return existing.get().getTargetKey();
        }

        // Generate new key for target
        String targetKey = generateProjectKey(sourceKey, jobId);

        ProjectMapping mapping = ProjectMapping.builder()
                .jobId(jobId)
                .sourceKey(sourceKey)
                .targetKey(targetKey)
                .build();

        projectMappingRepository.save(mapping);

        return targetKey;
    }

    private String generateProjectKey(String sourceKey, UUID jobId) {
        String baseKey = sourceKey.toUpperCase().replaceAll("[^A-Z0-9]", "");

        // Ensure unique key
        int suffix = 0;
        String candidateKey = baseKey;
        while (projectMappingRepository.existsByJobIdAndTargetKey(jobId, candidateKey)) {
            suffix++;
            candidateKey = baseKey + suffix;
            if (suffix > 100) {
                throw new MigrationException("Could not generate unique project key for: " + sourceKey);
            }
        }

        return candidateKey;
    }

    private String mapIssue(ParsedEntity entity, UUID jobId) {
        Map<String, String> fields = entity.getFields();

        // Map project key
        String sourceProjectKey = fields.get("project");
        ProjectMapping projectMapping = projectMappingRepository.findByJobIdAndSourceKey(jobId, sourceProjectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project", sourceProjectKey));

        // Generate issue number
        projectMapping.setIssueKeySequence(projectMapping.getIssueKeySequence() + 1);
        projectMappingRepository.save(projectMapping);

        return projectMapping.getTargetKey() + "-" + projectMapping.getIssueKeySequence();
    }

    private String mapUser(ParsedEntity entity, UUID jobId) {
        Map<String, String> fields = entity.getFields();

        // Try to match by email or username
        String email = fields.get("email");
        String username = fields.get("lowerUserName");

        Optional<UserMapping> existing = userMappingRepository.findByJobIdAndSourceIdentifier(jobId, username);
        if (existing.isPresent()) {
            return existing.get().getTargetUsername();
        }

        return username; // Return source username, let service handle mapping
    }

    private String mapIssueType(ParsedEntity entity) {
        Map<String, String> fields = entity.getFields();
        return fields.getOrDefault("name", entity.getEntityKey());
    }

    private String mapStatus(ParsedEntity entity) {
        Map<String, String> fields = entity.getFields();
        return fields.getOrDefault("name", entity.getEntityKey());
    }

    private String mapPriority(ParsedEntity entity) {
        Map<String, String> fields = entity.getFields();
        return fields.getOrDefault("name", entity.getEntityKey());
    }

    public static class ParseResult {
        private int totalEntities;
        private List<ParsedEntity> entities;

        public int getTotalEntities() { return totalEntities; }
        public void setTotalEntities(int totalEntities) { this.totalEntities = totalEntities; }
        public List<ParsedEntity> getEntities() { return entities; }
        public void setEntities(List<ParsedEntity> entities) { this.entities = entities; }
    }

    public static class ParsedEntity {
        private String entityType;
        private String entityKey;
        private Map<String, String> fields;
        private Map<String, String> dependencies;

        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public String getEntityKey() { return entityKey; }
        public void setEntityKey(String entityKey) { this.entityKey = entityKey; }
        public Map<String, String> getFields() { return fields; }
        public void setFields(Map<String, String> fields) { this.fields = fields; }
        public Map<String, String> getDependencies() { return dependencies; }
        public void setDependencies(Map<String, String> dependencies) { this.dependencies = dependencies; }
    }
}