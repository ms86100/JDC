package com.jira.migration.dc;

import com.jira.migration.dto.ValidationResult;
import com.jira.migration.entity.DcStagingEntry;
import com.jira.migration.entity.DcUnknownCustomField;
import com.jira.migration.entity.MigrationValidationResult;
import com.jira.migration.parser.JiraDcXmlParser;
import com.jira.migration.repository.DcUnknownCustomFieldRepository;
import com.jira.migration.repository.MigrationValidationResultRepository;
import com.jira.migration.service.clients.UserServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JiraDcImportValidationService {

    private static final Pattern ISSUE_KEY = Pattern.compile("^[A-Z][A-Z0-9]+-\\d+$");
    private static final java.util.regex.Pattern CUSTOM_FIELD_ID =
            java.util.regex.Pattern.compile("^customfield_\\d+$", java.util.regex.Pattern.CASE_INSENSITIVE);

    private final MigrationValidationResultRepository validationResultRepository;
    private final DcUnknownCustomFieldRepository unknownCustomFieldRepository;
    private final UserServiceClient userServiceClient;

    @Transactional
    public JiraDcValidationReport validate(
            UUID jobId,
            UUID sessionId,
            List<JiraDcXmlParser.ParsedEntity> entities,
            PathAttachmentContext attachmentContext,
            boolean clearPrevious) {

        if (clearPrevious && jobId != null) {
            validationResultRepository.deleteByJobId(jobId);
        }

        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        List<ValidationResult.ValidationWarning> warnings = new ArrayList<>();
        Map<String, String> entityKeyToState = new HashMap<>();

        Set<String> issueKeys = new HashSet<>();
        Set<String> duplicateKeys = new HashSet<>();
        Map<String, String> parentOf = new HashMap<>();

        for (JiraDcXmlParser.ParsedEntity entity : entities) {
            String key = entity.getEntityKey();
            if ("Issue".equals(entity.getEntityType()) || "SubTask".equals(entity.getEntityType())) {
                if (key != null && !issueKeys.add(key)) {
                    duplicateKeys.add(key);
                    errors.add(error("issueKey", "DUPLICATE_ISSUE_KEY", "Duplicate issue key: " + key, key));
                }
                if (key != null && !ISSUE_KEY.matcher(key).matches()) {
                    warnings.add(warn("issueKey", "ISSUE_KEY_FORMAT", "Issue key format unusual: " + key, key));
                }
                Map<String, String> f = entity.getFields();
                if (f != null) {
                    String parent = f.get("parent");
                    if (parent != null) {
                        parentOf.put(key, parent);
                    }
                    validateCustomFields(jobId, f, warnings);
                    validateStoryPoints(f, errors, key);
                }
            }
        }

        for (Map.Entry<String, String> e : parentOf.entrySet()) {
            if (!issueKeys.contains(e.getValue())) {
                warnings.add(warn("parent", "ORPHAN_SUBTASK",
                        "Parent issue not in export: " + e.getValue() + " for " + e.getKey(), e.getKey()));
            }
        }

        if (hasParentCycle(parentOf)) {
            errors.add(error("parent", "CIRCULAR_PARENT", "Circular parent reference detected", null));
        }

        validateDirectoryUsers(entities, warnings);
        validateWorkflowReferences(entities, warnings);

        for (JiraDcXmlParser.ParsedEntity entity : entities) {
            if ("Attachment".equals(entity.getEntityType())) {
                Map<String, String> f = entity.getFields();
                String attId = f != null ? f.get("sourceAttachmentId") : null;
                String fileName = f != null ? first(f, "filename", "name") : null;
                boolean hasInline = f != null && f.containsKey("file") && !f.get("file").isBlank();
                boolean hasBundle = attachmentContext != null && attachmentContext.hasFile(attId, fileName);
                if (!hasInline && !hasBundle) {
                    warnings.add(warn("attachment", "MISSING_ATTACHMENT_BINARY",
                            "Attachment metadata without binary: " + entity.getEntityKey(), entity.getEntityKey()));
                }
            }
        }

        for (JiraDcXmlParser.ParsedEntity entity : entities) {
            if (entity.getEntityKey() == null) {
                continue;
            }
            String state = duplicateKeys.contains(entity.getEntityKey()) ? "BLOCKED" : "VALID";
            if (!duplicateKeys.contains(entity.getEntityKey()) && !warnings.isEmpty()) {
                state = "WARN";
            }
            entityKeyToState.put(entity.getEntityKey(), state);
        }

        persistValidationRows(jobId, sessionId, errors, warnings);

        int blockers = errors.size();
        int warnCount = warnings.size();
        int riskScore = Math.max(0, 100 - (blockers * 15) - (warnCount * 3));

        return new JiraDcValidationReport(
                errors.isEmpty(),
                errors,
                warnings,
                riskScore,
                entityKeyToState,
                blockers,
                warnCount
        );
    }

    private void validateDirectoryUsers(
            List<JiraDcXmlParser.ParsedEntity> entities,
            List<ValidationResult.ValidationWarning> warnings) {
        Set<String> emails = new HashSet<>();
        for (JiraDcXmlParser.ParsedEntity entity : entities) {
            if (!"User".equals(entity.getEntityType()) && !"Group".equals(entity.getEntityType())) {
                continue;
            }
            Map<String, String> f = entity.getFields();
            if (f == null) {
                continue;
            }
            String email = f.get("email");
            if (email != null && !emails.add(email)) {
                continue;
            }
            if (email != null && userServiceClient.findUserByEmail(email).isEmpty()) {
                warnings.add(warn("user", "USER_WILL_BE_CREATED",
                        "User will be created on import: " + email, entity.getEntityKey()));
            }
        }
    }

    private void validateWorkflowReferences(
            List<JiraDcXmlParser.ParsedEntity> entities,
            List<ValidationResult.ValidationWarning> warnings) {
        long workflowEntities = entities.stream()
                .filter(e -> "Workflow".equals(e.getEntityType()) || "Status".equals(e.getEntityType()))
                .count();
        if (workflowEntities > 0) {
            warnings.add(warn("workflow", "WORKFLOW_ENTITIES_PRESENT",
                    "Export contains " + workflowEntities + " workflow/status entities — verify target compatibility",
                    null));
        }
    }

    private void validateCustomFields(UUID jobId, Map<String, String> fields, List<ValidationResult.ValidationWarning> warnings) {
        for (Map.Entry<String, String> e : fields.entrySet()) {
            String key = e.getKey();
            if (key == null || !key.startsWith("customfield_") || key.endsWith("_name")) {
                continue;
            }
            if (e.getValue() != null && e.getValue().length() > 4000) {
                warnings.add(warn(key, "CUSTOM_FIELD_VALUE_TRUNCATED",
                        "Custom field value may be truncated on import: " + key, key));
            }
            if (CUSTOM_FIELD_ID.matcher(key).matches()) {
                continue;
            }
            if (key.startsWith("customfield_") || key.contains("greenhopper") || key.contains("tempo")) {
                warnings.add(warn(key, "UNKNOWN_CUSTOM_FIELD",
                        "Plugin/unknown custom field: " + key, key));
                if (jobId != null && unknownCustomFieldRepository.findByJobId(jobId).stream()
                        .noneMatch(u -> key.equals(u.getFieldId()))) {
                    unknownCustomFieldRepository.save(DcUnknownCustomField.builder()
                            .jobId(jobId)
                            .fieldId(key)
                            .fieldName(fields.get(key + "_name"))
                            .sampleValue(e.getValue())
                            .detectedType(guessType(e.getValue()))
                            .build());
                }
            }
        }
    }

    private void validateStoryPoints(Map<String, String> fields, List<ValidationResult.ValidationError> errors, String issueKey) {
        String sp = fields.get("customfield_10010");
        if (sp == null) {
            return;
        }
        try {
            double v = Double.parseDouble(sp.trim());
            if (v < 0 || v > 1000) {
                errors.add(error("customfield_10010", "INVALID_STORY_POINTS",
                        "Story points out of range", issueKey));
            }
        } catch (NumberFormatException ex) {
            errors.add(error("customfield_10010", "INVALID_STORY_POINTS",
                    "Story points must be numeric", issueKey));
        }
    }

    private static String guessType(String value) {
        if (value == null) {
            return "unknown";
        }
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            return "number";
        }
        if (value.contains(",")) {
            return "multi-select";
        }
        return "text";
    }

    private static boolean hasParentCycle(Map<String, String> parentOf) {
        for (String start : parentOf.keySet()) {
            Set<String> visited = new HashSet<>();
            String current = start;
            while (current != null && parentOf.containsKey(current)) {
                if (!visited.add(current)) {
                    return true;
                }
                current = parentOf.get(current);
            }
        }
        return false;
    }

    private void persistValidationRows(
            UUID jobId,
            UUID sessionId,
            List<ValidationResult.ValidationError> errors,
            List<ValidationResult.ValidationWarning> warnings) {
        if (jobId == null) {
            return;
        }
        int row = 0;
        for (ValidationResult.ValidationError e : errors) {
            validationResultRepository.save(MigrationValidationResult.builder()
                    .jobId(jobId)
                    .wizardSessionId(sessionId)
                    .rowNumber(++row)
                    .entityType("ISSUE")
                    .entityKey(e.getInvalidValue() != null ? e.getInvalidValue().toString() : null)
                    .severity("ERROR")
                    .fieldName(e.getField())
                    .errorCode(e.getErrorCode())
                    .message(e.getMessage())
                    .build());
        }
        for (ValidationResult.ValidationWarning w : warnings) {
            validationResultRepository.save(MigrationValidationResult.builder()
                    .jobId(jobId)
                    .wizardSessionId(sessionId)
                    .rowNumber(++row)
                    .entityType("ISSUE")
                    .entityKey(null)
                    .severity("WARN")
                    .fieldName(w.getField())
                    .errorCode(w.getWarningCode())
                    .message(w.getMessage())
                    .build());
        }
    }

    private static ValidationResult.ValidationError error(String field, String code, String msg, Object key) {
        return ValidationResult.ValidationError.builder()
                .field(field).errorCode(code).message(msg).invalidValue(key).build();
    }

    private static ValidationResult.ValidationWarning warn(String field, String code, String msg, Object key) {
        return ValidationResult.ValidationWarning.builder()
                .field(field).warningCode(code).message(msg).row(null).build();
    }

    private static String first(Map<String, String> f, String... keys) {
        for (String k : keys) {
            if (f.containsKey(k) && f.get(k) != null && !f.get(k).isBlank()) {
                return f.get(k);
            }
        }
        return null;
    }

    public record PathAttachmentContext(java.nio.file.Path bundleRoot) {
        public boolean hasFile(String attId, String fileName) {
            if (bundleRoot == null) {
                return false;
            }
            java.nio.file.Path root = bundleRoot;
            if (attId != null) {
                if (java.nio.file.Files.exists(root.resolve(attId))) {
                    return true;
                }
                if (java.nio.file.Files.exists(root.resolve("data/attachments").resolve(attId))) {
                    return true;
                }
            }
            if (fileName != null) {
                if (java.nio.file.Files.exists(root.resolve(fileName))) {
                    return true;
                }
                if (java.nio.file.Files.exists(root.resolve("data/attachments").resolve(fileName))) {
                    return true;
                }
            }
            return false;
        }
    }

    public record JiraDcValidationReport(
            boolean valid,
            List<ValidationResult.ValidationError> errors,
            List<ValidationResult.ValidationWarning> warnings,
            int riskScore,
            Map<String, String> entityKeyToState,
            int blockerCount,
            int warningCount
    ) {
    }
}
