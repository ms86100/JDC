package com.jira.migration.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.dto.ValidationResult;
import com.jira.migration.entity.CsvTemplate;
import com.jira.migration.exception.ValidationException;
import com.jira.migration.repository.CsvTemplateRepository;
import com.jira.migration.service.DbValidationRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ValidationEngine {

    private final CsvTemplateRepository csvTemplateRepository;
    private final DbValidationRuleEngine dbValidationRuleEngine;
    private final ObjectMapper objectMapper;

    private final Set<String> validIssueTypes;
    private final Set<String> validPriorities;

    public ValidationEngine(
            CsvTemplateRepository csvTemplateRepository,
            DbValidationRuleEngine dbValidationRuleEngine,
            ObjectMapper objectMapper,
            @Value("${app.validation.valid-issue-types:Epic,Story,Task,Bug,Subtask,Improvement}") String validIssueTypesStr,
            @Value("${app.validation.valid-priorities:Highest,High,Medium,Low,Lowest}") String validPrioritiesStr) {
        this.csvTemplateRepository = csvTemplateRepository;
        this.dbValidationRuleEngine = dbValidationRuleEngine;
        this.objectMapper = objectMapper;
        this.validIssueTypes = Arrays.stream(validIssueTypesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.validPriorities = Arrays.stream(validPrioritiesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public ValidationResult validateRow(Map<String, String> rowData, String entityType, int rowNumber) {
        List<ValidationResult.ValidationError> errors = new ArrayList<>();
        List<ValidationResult.ValidationWarning> warnings = new ArrayList<>();

        // Validate based on entity type
        switch (entityType.toUpperCase()) {
            case "PROJECT" -> validateProjectRow(rowData, rowNumber, errors, warnings);
            case "ISSUE" -> validateIssueRow(rowData, rowNumber, errors, warnings);
            case "USER" -> validateUserRow(rowData, rowNumber, errors, warnings);
            default -> log.warn("No validation rules for entity type: {}", entityType);
        }

        dbValidationRuleEngine.applyRules(entityType, rowData, rowNumber, errors, warnings);

        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private void validateProjectRow(Map<String, String> row, int rowNum,
                                   List<ValidationResult.ValidationError> errors,
                                   List<ValidationResult.ValidationWarning> warnings) {
        // Project key validation
        String projectKey = row.get("project_key");
        if (projectKey == null || projectKey.isBlank()) {
            errors.add(createError("project_key", "PROJECT_KEY_REQUIRED", "Project key is required", rowNum, null));
        } else if (!Pattern.matches("^[A-Z][A-Z0-9]{0,9}$", projectKey)) {
            errors.add(createError("project_key", "PROJECT_KEY_PATTERN", "Project key must be uppercase letters and numbers, starting with a letter (max 10 chars)", rowNum, projectKey));
        }

        // Project name validation
        String name = row.get("name");
        if (name == null || name.isBlank()) {
            errors.add(createError("name", "PROJECT_NAME_REQUIRED", "Project name is required", rowNum, null));
        } else if (name.length() > 200) {
            errors.add(createError("name", "PROJECT_NAME_TOO_LONG", "Project name cannot exceed 200 characters", rowNum, name));
        }

        // Project type validation
        String projectType = row.get("project_type");
        if (projectType != null && !projectType.isBlank()) {
            if (!projectType.equals("COMPANY_MANAGED") && !projectType.equals("TEAM_MANAGED")) {
                errors.add(createError("project_type", "INVALID_PROJECT_TYPE", "Project type must be COMPANY_MANAGED or TEAM_MANAGED", rowNum, projectType));
            }
        }
    }

    private void validateIssueRow(Map<String, String> row, int rowNum,
                                 List<ValidationResult.ValidationError> errors,
                                 List<ValidationResult.ValidationWarning> warnings) {
        // Required field checks (project_key, issue_type, summary, status) are handled
        // by DbValidationRuleEngine via the validation_rules table — not duplicated here.

        // Issue type — warn if not a standard type (useful hint, not blocking)
        String issueType = firstNonBlank(row, "issue_type", "issuetype", "type");
        if (issueType != null && !issueType.isBlank()) {
            if (!validIssueTypes.contains(issueType)) {
                warnings.add(createWarning("issue_type", "UNKNOWN_ISSUE_TYPE", "Issue type '" + issueType + "' may not exist in target system", rowNum));
            }
        }

        // Priority — warn if not a standard value
        String priority = row.get("priority");
        if (priority != null && !priority.isBlank()) {
            if (!validPriorities.contains(priority)) {
                warnings.add(createWarning("priority", "UNKNOWN_PRIORITY", "Priority '" + priority + "' may not exist in target system", rowNum));
            }
        }

        // Due date — warn on invalid format; will be skipped during persist
        String dueDate = row.get("due_date");
        if (dueDate != null && !dueDate.isBlank()) {
            if (!isValidDate(dueDate)) {
                warnings.add(createWarning("due_date", "INVALID_DATE_FORMAT", "Due date '" + dueDate + "' is not YYYY-MM-DD; will be skipped", rowNum));
            }
        }

        // Story points — warn on non-numeric; will be skipped during persist
        String storyPoints = row.get("story_points");
        if (storyPoints != null && !storyPoints.isBlank()) {
            try {
                int points = Integer.parseInt(storyPoints);
                if (points < 0 || points > 100) {
                    warnings.add(createWarning("story_points", "INVALID_STORY_POINTS", "Story points " + storyPoints + " outside 0-100 range", rowNum));
                }
            } catch (NumberFormatException e) {
                warnings.add(createWarning("story_points", "INVALID_STORY_POINTS", "Story points '" + storyPoints + "' is not a number; will be skipped", rowNum));
            }
        }
    }

    private void validateUserRow(Map<String, String> row, int rowNum,
                                List<ValidationResult.ValidationError> errors,
                                List<ValidationResult.ValidationWarning> warnings) {
        // Username validation
        String username = row.get("username");
        if (username == null || username.isBlank()) {
            errors.add(createError("username", "USERNAME_REQUIRED", "Username is required", rowNum, null));
        } else {
            if (!Pattern.matches("^[a-zA-Z0-9._-]{3,150}$", username)) {
                errors.add(createError("username", "INVALID_USERNAME", "Username must be 3-150 characters, alphanumeric with ._- allowed", rowNum, username));
            }
        }

        // Email validation
        String email = row.get("email");
        if (email == null || email.isBlank()) {
            errors.add(createError("email", "EMAIL_REQUIRED", "Email is required", rowNum, null));
        } else if (!Pattern.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", email)) {
            errors.add(createError("email", "INVALID_EMAIL", "Invalid email format", rowNum, email));
        }

        // Display name length
        String displayName = row.get("display_name");
        if (displayName != null && displayName.length() > 200) {
            warnings.add(createWarning("display_name", "DISPLAY_NAME_TOO_LONG", "Display name will be truncated to 200 characters", rowNum));
        }
    }

    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException e) {
            try {
                LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return true;
            } catch (DateTimeParseException e2) {
                return false;
            }
        }
    }

    private static String firstNonBlank(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private ValidationResult.ValidationError createError(String field, String code, String message, Integer row, Object value) {
        return ValidationResult.ValidationError.builder()
                .field(field)
                .errorCode(code)
                .message(message)
                .row(row)
                .invalidValue(value)
                .build();
    }

    private ValidationResult.ValidationWarning createWarning(String field, String code, String message, Integer row) {
        return ValidationResult.ValidationWarning.builder()
                .field(field)
                .warningCode(code)
                .message(message)
                .row(row)
                .build();
    }

    public List<String> validateBulk(List<Map<String, String>> rows, String entityType) {
        List<String> allErrors = new ArrayList<>();
        int rowNum = 1;

        for (Map<String, String> row : rows) {
            ValidationResult result = validateRow(row, entityType, rowNum);
            for (ValidationResult.ValidationError error : result.getErrors()) {
                allErrors.add(String.format("Row %d, %s: %s", error.getRow(), error.getField(), error.getMessage()));
            }
            rowNum++;
        }

        return allErrors;
    }
}