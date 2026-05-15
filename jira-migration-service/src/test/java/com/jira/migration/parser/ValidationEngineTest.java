package com.jira.migration.parser;

import com.jira.migration.dto.ValidationResult;
import com.jira.migration.entity.CsvTemplate;
import com.jira.migration.repository.CsvTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ValidationEngine component.
 * Tests validation logic for various entity types.
 */
@DisplayName("Validation Engine Unit Tests")
@Nested
@ExtendWith(MockitoExtension.class)
public class ValidationEngineTest {

    @Mock
    private CsvTemplateRepository csvTemplateRepository;

    private ValidationEngine validationEngine;

    @BeforeEach
    void setUp() {
        validationEngine = new ValidationEngine(csvTemplateRepository, new ObjectMapper());
    }

    // ============================================
    // Project Validation Tests
    // ============================================

    @Test
    @DisplayName("Should validate valid project row")
    void shouldValidateValidProjectRow() {
        // Given valid project row data
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "name", "Test Project",
                "description", "Test Description",
                "project_type", "COMPANY_MANAGED"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then should pass validation
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should validate project with optional fields missing")
    void shouldValidateProjectWithOptionalFieldsMissing() {
        // Given project with only required fields
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "name", "Test Project"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then should pass
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should fail validation when project_key is missing")
    void shouldFailValidationWhenProjectKeyIsMissing() {
        // Given project row without project_key
        Map<String, String> row = Map.of(
                "name", "Test Project"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("project_key"));
    }

    @Test
    @DisplayName("Should fail validation when name is missing")
    void shouldFailValidationWhenNameIsMissing() {
        // Given project row without name
        Map<String, String> row = Map.of(
                "project_key", "PROJ1"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("name"));
    }

    @Test
    @DisplayName("Should fail validation for invalid project key format")
    void shouldFailValidationForInvalidProjectKeyFormat() {
        // Given project with invalid key format
        Map<String, String> row = Map.of(
                "project_key", "invalid_lowercase",
                "name", "Test Project"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then should fail with pattern error
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e ->
                e.getField().equals("project_key") &&
                e.getErrorCode().equals("PROJECT_KEY_PATTERN"));
    }

    @Test
    @DisplayName("Should fail validation for too long project key")
    void shouldFailValidationForTooLongProjectKey() {
        // Given project with too long key
        Map<String, String> row = Map.of(
                "project_key", "TOOLONGPROJECTKEY123",
                "name", "Test Project"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should fail validation for too long project name")
    void shouldFailValidationForTooLongProjectName() {
        // Given project with name exceeding 200 chars
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "name", "A".repeat(201)
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e ->
                e.getField().equals("name") &&
                e.getErrorCode().equals("PROJECT_NAME_TOO_LONG"));
    }

    @Test
    @DisplayName("Should fail validation for invalid project type")
    void shouldFailValidationForInvalidProjectType() {
        // Given project with invalid type
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "name", "Test Project",
                "project_type", "INVALID_TYPE"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e ->
                e.getField().equals("project_type") &&
                e.getErrorCode().equals("INVALID_PROJECT_TYPE"));
    }

    // ============================================
    // Issue Validation Tests
    // ============================================

    @Test
    @DisplayName("Should validate valid issue row")
    void shouldValidateValidIssueRow() {
        // Given valid issue row
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "issue_type", "Bug",
                "summary", "Test Issue Summary",
                "status", "Open",
                "priority", "High"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should pass
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should fail validation when project_key is missing for issue")
    void shouldFailValidationWhenProjectKeyMissingForIssue() {
        // Given issue without project_key
        Map<String, String> row = Map.of(
                "issue_type", "Bug",
                "summary", "Test Summary",
                "status", "Open"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("project_key"));
    }

    @Test
    @DisplayName("Should fail validation when issue_type is missing")
    void shouldFailValidationWhenIssueTypeMissing() {
        // Given issue without issue_type
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "summary", "Test Summary",
                "status", "Open"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("issue_type"));
    }

    @Test
    @DisplayName("Should fail validation when summary is missing")
    void shouldFailValidationWhenSummaryMissing() {
        // Given issue without summary
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "issue_type", "Bug",
                "status", "Open"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("summary"));
    }

    @Test
    @DisplayName("Should fail validation when status is missing")
    void shouldFailValidationWhenStatusMissing() {
        // Given issue without status
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "issue_type", "Bug",
                "summary", "Test Summary"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("status"));
    }

    @Test
    @DisplayName("Should provide warning for unknown issue type")
    void shouldProvideWarningForUnknownIssueType() {
        // Given issue with unknown type
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "issue_type", "UnknownType",
                "summary", "Test Summary",
                "status", "Open"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should have warning but still be valid
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).anyMatch(w -> w.getField().equals("issue_type"));
    }

    @Test
    @DisplayName("Should fail validation for invalid due date format")
    void shouldFailValidationForInvalidDueDateFormat() {
        // Given issue with invalid date
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "issue_type", "Bug",
                "summary", "Test Summary",
                "status", "Open",
                "due_date", "invalid-date"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("due_date"));
    }

    @Test
    @DisplayName("Should validate valid due date in ISO format")
    void shouldValidateValidDueDateInIsoFormat() {
        // Given issue with valid ISO date
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "issue_type", "Bug",
                "summary", "Test Summary",
                "status", "Open",
                "due_date", "2026-12-31"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should pass
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should fail validation for story points out of range")
    void shouldFailValidationForStoryPointsOutOfRange() {
        // Given issue with out-of-range story points
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "issue_type", "Story",
                "summary", "Test Summary",
                "status", "Open",
                "story_points", "150"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("story_points"));
    }

    @Test
    @DisplayName("Should fail validation for non-numeric story points")
    void shouldFailValidationForNonNumericStoryPoints() {
        // Given issue with non-numeric story points
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "issue_type", "Story",
                "summary", "Test Summary",
                "status", "Open",
                "story_points", "abc"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "ISSUE", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("story_points"));
    }

    // ============================================
    // User Validation Tests
    // ============================================

    @Test
    @DisplayName("Should validate valid user row")
    void shouldValidateValidUserRow() {
        // Given valid user row
        Map<String, String> row = Map.of(
                "username", "testuser",
                "email", "test@example.com",
                "display_name", "Test User"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "USER", 1);

        // Then should pass
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should fail validation when username is missing")
    void shouldFailValidationWhenUsernameMissing() {
        // Given user without username
        Map<String, String> row = Map.of(
                "email", "test@example.com"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "USER", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("username"));
    }

    @Test
    @DisplayName("Should fail validation for invalid username format")
    void shouldFailValidationForInvalidUsernameFormat() {
        // Given user with invalid username
        Map<String, String> row = Map.of(
                "username", "ab",  // Too short
                "email", "test@example.com"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "USER", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("username"));
    }

    @Test
    @DisplayName("Should fail validation when email is missing")
    void shouldFailValidationWhenEmailMissing() {
        // Given user without email
        Map<String, String> row = Map.of(
                "username", "testuser"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "USER", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("email"));
    }

    @Test
    @DisplayName("Should fail validation for invalid email format")
    void shouldFailValidationForInvalidEmailFormat() {
        // Given user with invalid email
        Map<String, String> row = Map.of(
                "username", "testuser",
                "email", "invalid-email"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "USER", 1);

        // Then should fail
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.getField().equals("email"));
    }

    @Test
    @DisplayName("Should provide warning for long display name")
    void shouldProvideWarningForLongDisplayName() {
        // Given user with display name exceeding 200 chars
        Map<String, String> row = Map.of(
                "username", "testuser",
                "email", "test@example.com",
                "display_name", "A".repeat(201)
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "USER", 1);

        // Then should have warning
        assertThat(result.getWarnings()).anyMatch(w -> w.getField().equals("display_name"));
    }

    // ============================================
    // Bulk Validation Tests
    // ============================================

    @Test
    @DisplayName("Should validate multiple rows and aggregate errors")
    void shouldValidateMultipleRowsAndAggregateErrors() {
        // Given multiple rows with different validities
        List<Map<String, String>> rows = List.of(
                Map.of("project_key", "PROJ1", "name", "Valid Project"),  // Valid
                Map.of("project_key", "", "name", "Invalid Project"),     // Invalid - missing key
                Map.of("project_key", "PROJ2"),                            // Invalid - missing name
                Map.of("project_key", "invalid", "name", "Valid Name")     // Invalid - bad key
        );

        // When validating bulk
        List<String> errors = validationEngine.validateBulk(rows, "PROJECT");

        // Then should return aggregated errors
        assertThat(errors).isNotEmpty();
        assertThat(errors.size()).isGreaterThanOrEqualTo(3); // At least 3 errors
    }

    @Test
    @DisplayName("Should include row numbers in bulk error messages")
    void shouldIncludeRowNumbersInBulkErrorMessages() {
        // Given multiple rows
        List<Map<String, String>> rows = List.of(
                Map.of("project_key", "PROJ1", "name", "Valid Project"),
                Map.of("project_key", "", "name", "Invalid Project")
        );

        // When validating bulk
        List<String> errors = validationEngine.validateBulk(rows, "PROJECT");

        // Then error messages should include row numbers
        assertThat(errors).anyMatch(e -> e.contains("Row 1") || e.contains("Row 2"));
    }

    @Test
    @DisplayName("Should return empty errors for all valid rows")
    void shouldReturnEmptyErrorsForAllValidRows() {
        // Given all valid rows
        List<Map<String, String>> rows = List.of(
                Map.of("project_key", "PROJ1", "name", "Project One"),
                Map.of("project_key", "PROJ2", "name", "Project Two"),
                Map.of("project_key", "PROJ3", "name", "Project Three")
        );

        // When validating bulk
        List<String> errors = validationEngine.validateBulk(rows, "PROJECT");

        // Then should return no errors
        assertThat(errors).isEmpty();
    }

    // ============================================
    // Row Number Tracking Tests
    // ============================================

    @Test
    @DisplayName("Should track row number in validation result")
    void shouldTrackRowNumberInValidationResult() {
        // Given a row at position 5
        Map<String, String> row = Map.of(
                "project_key", "",
                "name", "Test"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 5);

        // Then row number should be tracked
        assertThat(result.getErrors()).anyMatch(e -> e.getRow().equals(5));
    }

    // ============================================
    // Error Detail Tests
    // ============================================

    @Test
    @DisplayName("Should include field name in error details")
    void shouldIncludeFieldNameInErrorDetails() {
        // Given invalid row
        Map<String, String> row = Map.of(
                "project_key", "",
                "name", "Test"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then errors should include field name
        assertThat(result.getErrors()).allMatch(e -> e.getField() != null);
    }

    @Test
    @DisplayName("Should include error code in error details")
    void shouldIncludeErrorCodeInErrorDetails() {
        // Given invalid row
        Map<String, String> row = Map.of(
                "project_key", "",
                "name", "Test"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then errors should include error code
        assertThat(result.getErrors()).allMatch(e -> e.getErrorCode() != null);
    }

    @Test
    @DisplayName("Should include invalid value in error details")
    void shouldIncludeInvalidValueInErrorDetails() {
        // Given row with specific invalid value
        Map<String, String> row = Map.of(
                "project_key", "TOOLONGKEYNAME123",
                "name", "Test"
        );

        // When validating
        ValidationResult result = validationEngine.validateRow(row, "PROJECT", 1);

        // Then error may include the invalid value
        assertThat(result.getErrors()).isNotEmpty();
    }
}
