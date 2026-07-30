package com.avionics_systems.migration.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.entity.ProjectMapping;
import com.avionics_systems.migration.entity.UserMapping;
import com.avionics_systems.migration.exception.EntityNotFoundException;
import com.avionics_systems.migration.exception.MigrationException;
import com.avionics_systems.migration.repository.EntityStatusRepository;
import com.avionics_systems.migration.repository.ProjectMappingRepository;
import com.avionics_systems.migration.repository.UserMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class LegacyDcXmlParser {

    private final ValidationEngine validationEngine;
    private final ProjectMappingRepository projectMappingRepository;
    private final UserMappingRepository userMappingRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> LEGACY_DC_TO_PLATFORM_TABLE_MAP = Map.ofEntries(
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

    public ParseResult parseXmlBackup(String xmlContent, UUID jobId) {
        return parseXmlBackup(xmlContent, jobId, null);
    }

    public ParseResult parseXmlBackup(String xmlContent, UUID jobId, java.nio.file.Path xmlPath) {
        log.info("Parsing Legacy DC XML backup for job: {}", jobId);

        LegacyDcXmlFormat format = LegacyDcXmlFormatDetector.detect(xmlContent);
        if (xmlPath != null) {
            if (format == LegacyDcXmlFormat.ENTITIES_XML) {
                List<ParsedEntity> entities = LegacyDcEntitiesXmlParser.parse(xmlPath);
                ParseResult result = new ParseResult();
                result.setXmlFormat(format);
                result.setEntities(entities);
                result.setTotalEntities(entities.size());
                log.info("Parsed {} entities from {} XML (streamed path)", result.getTotalEntities(), format);
                return result;
            }
            if (format == LegacyDcXmlFormat.RSS_092) {
                List<ParsedEntity> entities = LegacyDcRss092Parser.parsePath(xmlPath);
                ParseResult result = new ParseResult();
                result.setXmlFormat(format);
                result.setEntities(entities);
                result.setTotalEntities(entities.size());
                log.info("Parsed {} entities from RSS (streamed path)", result.getTotalEntities());
                return result;
            }
            if (format == LegacyDcXmlFormat.ENTITY_BACKUP) {
                List<ParsedEntity> entities = LegacyDcEntityBackupSaxParser.parse(xmlPath);
                ParseResult result = new ParseResult();
                result.setXmlFormat(format);
                result.setEntities(entities);
                result.setTotalEntities(entities.size());
                log.info("Parsed {} entities from Entity backup (streamed path)", result.getTotalEntities());
                return result;
            }
        }
        log.info("Detected Legacy DC XML format: {}", format);

        if (format == LegacyDcXmlFormat.ENTITY_BACKUP && xmlPath != null) {
            List<ParsedEntity> entities = LegacyDcEntityBackupSaxParser.parse(xmlPath);
            ParseResult result = new ParseResult();
            result.setXmlFormat(format);
            result.setEntities(entities);
            result.setTotalEntities(entities.size());
            log.info("Parsed {} entities from Entity backup (streamed in-memory path)", result.getTotalEntities());
            return result;
        }

        List<ParsedEntity> entities = switch (format) {
            case RSS_092 -> LegacyDcRss092Parser.parse(xmlContent);
            case ENTITY_BACKUP -> LegacyDcEntityBackupParser.parse(xmlContent);
            case ENTITIES_XML -> LegacyDcEntitiesXmlParser.parse(xmlContent);
            case UNKNOWN -> throw new MigrationException(
                    "Unsupported Legacy DC XML format. Expected RSS 0.92 (<rss>), "
                            + "Entity backup (<LegacyDcBackup><Entity>), or native entities.xml.");
        };

        ParseResult result = new ParseResult();
        result.setXmlFormat(format);
        result.setEntities(entities);
        result.setTotalEntities(entities.size());

        log.info("Parsed {} entities from {} XML", result.getTotalEntities(), format);
        return result;
    }

    public String mapToPlatformEntity(ParsedEntity entity, UUID jobId) {
        String entityType = entity.getEntityType();

        return switch (entityType) {
            case "Project" -> mapProject(entity, jobId);
            case "Issue" -> mapIssue(entity, jobId);
            case "User" -> mapUser(entity, jobId);
            case "IssueType" -> mapIssueType(entity);
            case "Status" -> mapStatus(entity);
            case "Priority" -> mapPriority(entity);
            default -> {
                log.warn("Unknown entity type for mapping: {}", entityType);
                yield null;
            }
        };
    }

    private String mapProject(ParsedEntity entity, UUID jobId) {
        Map<String, String> fields = entity.getFields();
        String sourceKey = fields.get("key");

        Optional<ProjectMapping> existing = projectMappingRepository.findByJobIdAndSourceKey(jobId, sourceKey);
        if (existing.isPresent()) {
            return existing.get().getTargetKey();
        }

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

        String sourceProjectKey = fields.get("project");
        ProjectMapping projectMapping = projectMappingRepository.findByJobIdAndSourceKey(jobId, sourceProjectKey)
                .orElseThrow(() -> new EntityNotFoundException("Project", sourceProjectKey));

        projectMapping.setIssueKeySequence(projectMapping.getIssueKeySequence() + 1);
        projectMappingRepository.save(projectMapping);

        return projectMapping.getTargetKey() + "-" + projectMapping.getIssueKeySequence();
    }

    private String mapUser(ParsedEntity entity, UUID jobId) {
        Map<String, String> fields = entity.getFields();

        String username = fields.get("lowerUserName");

        Optional<UserMapping> existing = userMappingRepository.findByJobIdAndSourceIdentifier(jobId, username);
        if (existing.isPresent()) {
            return existing.get().getTargetUsername();
        }

        return username;
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
        private LegacyDcXmlFormat xmlFormat;

        public int getTotalEntities() { return totalEntities; }
        public void setTotalEntities(int totalEntities) { this.totalEntities = totalEntities; }
        public List<ParsedEntity> getEntities() { return entities; }
        public void setEntities(List<ParsedEntity> entities) { this.entities = entities; }
        public LegacyDcXmlFormat getXmlFormat() { return xmlFormat; }
        public void setXmlFormat(LegacyDcXmlFormat xmlFormat) { this.xmlFormat = xmlFormat; }
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
