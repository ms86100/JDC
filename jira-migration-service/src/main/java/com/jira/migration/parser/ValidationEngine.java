package com.jira.migration.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.migration.dto.ValidationResult;
import com.jira.migration.entity.CsvTemplate;
import com.jira.migration.exception.ValidationException;
import com.jira.migration.repository.CsvTemplateRepository;
import com.jira.migration.service.DbValidationRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationEngine {

    private final CsvTemplateRepository csvTemplateRepository;
    private final DbValidationRuleEngine dbValidationRuleEngine;
    private final ObjectMapper objectMapper;

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
        // Project key validation
        String projectKey = row.get("project_key");
        if (projectKey == null || projectKey.isBlank()) {
            errors.add(createError("project_key", "PROJECT_KEY_REQUIRED", "Project key is required", rowNum, null));
        }

        // Issue type validation
        String issueType = row.get("issue_type");
        if (issueType == null || issueType.isBlank()) {
            errors.add(createError("issue_type", "ISSUE_TYPE_REQUIRED", "Issue type is required", rowNum, null));
        } else {
            Set<String> validTypes = Set.of("Epic", "Story", "Task", "Bug", "Subtask", "Improvement");
            if (!validTypes.contains(issueType)) {
                warnings.add(createWarning("issue_type", "UNKNOWN_ISSUE_TYPE", "Issue type '" + issueType + "' may not exist in target system", rowNum));
            }
        }

        // Summary validation
        String summary = row.get("summary");
        if (summary == null || summary.isBlank()) {
            errors.add(createError("summary", "SUMMARY_REQUIRED", "Summary is required", rowNum, null));
        } else if (summary.length() > 500) {
            errors.add(createError("summary", "SUMMARY_TOO_LONG", "Summary cannot exceed 500 characters", rowNum, summary));
        }

        // Status validation
        String status = row.get("status");
        if (status == null || status.isBlank()) {
            errors.add(createError("status", "STATUS_REQUIRED", "Status is required", rowNum, null));
        }

        // Priority validation
        String priority = row.get("priority");
        if (priority != null && !priority.isBlank()) {
            Set<String> validPriorities = Set.of("Highest", "High", "Medium", "Low", "Lowest");
            if (!validPriorities.contains(priority)) {
                warnings.add(createWarning("priority", "UNKNOWN_PRIORITY", "Priority '" + priority + "' may not exist in target system", rowNum));
            }
        }

        // Due date validation
        String dueDate = row.get("due_date");
        if (dueDate != null && !dueDate.isBlank()) {
            if (!isValidDate(dueDate)) {
                errors.add(createError("due_date", "INVALID_DATE_FORMAT", "Due date must be in YYYY-MM-DD format", rowNum, dueDate));
            }
        }

        // Story points validation
        String storyPoints = row.get("story_points");
        if (storyPoints != null && !storyPoints.isBlank()) {
            try {
                int points = Integer.parseInt(storyPoints);
                if (points < 0 || points > 100) {
                    errors.add(createError("story_points", "INVALID_STORY_POINTS", "Story points must be between 0 and 100", rowNum, storyPoints));
                }
            } catch (NumberFormatException e) {
                errors.add(createError("story_points", "INVALID_STORY_POINTS", "Story points must be a number", rowNum, storyPoints));
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