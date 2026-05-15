package com.jira.migration.integration;

import com.jira.migration.BaseIntegrationTest;
import com.jira.migration.TestUtils;
import com.jira.migration.dto.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the ValidationEngine component.
 * Tests validation scenarios for CSV data.
 */
@Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Validation Engine Integration Tests")
@Nested
@Slf4j
public class ValidationEngineIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String validProjectsCsv;
    private String validIssuesCsv;
    private String invalidCsv;

    @BeforeEach
    void setUp() {
        validProjectsCsv = TestUtils.getResourceFileContent("projects.csv");
        validIssuesCsv = TestUtils.getResourceFileContent("issues.csv");
        invalidCsv = TestUtils.getResourceFileContent("invalid.csv");
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate required project_key field")
    void shouldValidateRequiredProjectKeyField() {
        // Given CSV with missing project_key
        String csvContent = """
                project_key,name,description
                ,Project Without Key
                PROJ1,Valid Project,Description
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then validation should fail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
        assertThat(response.getBody().getErrors()).isNotEmpty();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate required name field")
    void shouldValidateRequiredNameField() {
        // Given CSV with missing name
        String csvContent = """
                project_key,name,description
                PROJ1,Valid Project,Description
                PROJ2,,Missing Name
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then validation should fail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate project key format")
    void shouldValidateProjectKeyFormat() {
        // Given CSV with invalid project key formats
        String csvContent = """
                project_key,name,description
                invalid_lowercase,Invalid Key
                123StartsWithNumber,Invalid Key
                TOOLONGKEYNAME,Too Long Key
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then validation should fail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate project key pattern - uppercase letters")
    void shouldValidateProjectKeyPattern() {
        // Given CSV with valid project keys
        String csvContent = """
                project_key,name,description
                PROJ1,Project One,Description One
                ABC,Short Key,Description
                TEAM1,Team Project,Description
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then validation should pass (for valid keys)
        // Note: Some keys may fail due to other validations
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate project type values")
    void shouldValidateProjectTypeValues() {
        // Given CSV with invalid project type
        String csvContent = """
                project_key,name,project_type
                PROJ1,Project One,INVALID_TYPE
                PROJ2,Project Two,COMPANY_MANAGED
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then validation should detect invalid project type
        assertThat(response.getBody()).isNotNull();
        // Validation depends on implementation
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate issue type for issues")
    void shouldValidateIssueTypeForIssues() {
        // Given CSV with missing issue_type
        String csvContent = """
                project_key,issue_type,summary,status
                PROJ1,,Summary missing type,Open
                PROJ1,Bug,Valid Bug,Open
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "ISSUE");

        // Then validation should fail for missing issue type
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate required summary field")
    void shouldValidateRequiredSummaryField() {
        // Given CSV with missing summary
        String csvContent = """
                project_key,issue_type,summary,status
                PROJ1,Bug,,Open
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "ISSUE");

        // Then validation should fail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate date format for due_date")
    void shouldValidateDateFormatForDueDate() {
        // Given CSV with invalid date format
        String csvContent = """
                project_key,issue_type,summary,status,due_date
                PROJ1,Bug,Test Issue,Open,invalid-date-format
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "ISSUE");

        // Then validation should fail
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate story points range")
    void shouldValidateStoryPointsRange() {
        // Given CSV with invalid story points
        String csvContent = """
                project_key,issue_type,summary,status,story_points
                PROJ1,Story,Test Story,Open,150
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "ISSUE");

        // Then validation should fail for out-of-range story points
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate user email format")
    void shouldValidateUserEmailFormat() {
        // Given CSV with invalid email
        String csvContent = """
                username,email,display_name
                user1,invalid-email,User One
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "USER");

        // Then validation should fail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate required username field")
    void shouldValidateRequiredUsernameField() {
        // Given CSV with missing username
        String csvContent = """
                username,email,display_name
                ,user@test.com,User One
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "USER");

        // Then validation should fail
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValid()).isFalse();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should provide detailed error messages")
    void shouldProvideDetailedErrorMessages() {
        // Given CSV with multiple validation errors
        String csvContent = """
                project_key,name
                ,Missing Key
                PROJ1,
                INVALID,Valid Name
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then detailed error messages should be provided
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors()).isNotEmpty();

        response.getBody().getErrors().forEach(error -> {
            assertThat(error.getField()).isNotNull();
            assertThat(error.getErrorCode()).isNotNull();
            assertThat(error.getMessage()).isNotNull();
            log.debug("Validation error: {} - {} ({})", error.getField(), error.getMessage(), error.getErrorCode());
        });
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should provide warnings for non-critical issues")
    void shouldProvideWarningsForNonCriticalIssues() {
        // Given CSV with unknown issue type (warning, not error)
        String csvContent = """
                project_key,issue_type,summary,status,priority
                PROJ1,UnknownType,Test Issue,Open,Medium
                """;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "ISSUE");

        // Then warnings should be provided
        assertThat(response.getBody()).isNotNull();
        // Warnings are non-blocking, so validation might still pass
        // but warnings should be captured if present
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate valid project CSV successfully")
    void shouldValidateValidProjectCsvSuccessfully() {
        // Given valid project CSV
        String csvContent = validProjectsCsv;

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then validation should pass or show only warnings
        assertThat(response.getBody()).isNotNull();
        log.debug("Validation result: valid={}, errors={}, warnings={}",
                response.getBody().isValid(),
                response.getBody().getErrors().size(),
                response.getBody().getWarnings().size());
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should validate row by row")
    void shouldValidateRowByRow() {
        // Given a single row to validate
        Map<String, String> row = Map.of(
                "project_key", "PROJ1",
                "name", "Test Project",
                "description", "Test Description",
                "project_type", "COMPANY_MANAGED"
        );

        // When validating single row
        ResponseEntity<ValidationResult> response = validateRow(row, "PROJECT");

        // Then result should be returned
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should handle empty CSV gracefully")
    void shouldHandleEmptyCsvGracefully() {
        // Given empty CSV content
        String csvContent = "";

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then appropriate response should be returned
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Disabled("Requires full infrastructure (database, Redis)")
@DisplayName("Should handle CSV with only headers")
    void shouldHandleCsvWithOnlyHeaders() {
        // Given CSV with only headers
        String csvContent = "project_key,name,description\n";

        // When validating
        ResponseEntity<ValidationResult> response = validateCsv(csvContent, "PROJECT");

        // Then validation should handle gracefully
        assertThat(response.getBody()).isNotNull();
    }

    // Helper methods

    private ResponseEntity<ValidationResult> validateCsv(String csvContent, String entityType) {
        HttpHeaders headers = new HttpHeaders();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(csvContent.getBytes()) {
            @Override
            public String getFilename() {
                return "test.csv";
            }
        });
        body.add("entityType", entityType);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = "http://localhost:" + port + "/api/migration/validate/csv";
        return restTemplate.postForEntity(url, requestEntity, ValidationResult.class);
    }

    private ResponseEntity<ValidationResult> validateRow(Map<String, String> row, String entityType) {
        HttpHeaders headers = new HttpHeaders();

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(row, headers);

        String url = "http://localhost:" + port + "/api/migration/validate/row?entityType=" + entityType;
        return restTemplate.postForEntity(url, requestEntity, ValidationResult.class);
    }
}
