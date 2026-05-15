package com.jira.migration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Utility class for test data generation and file handling.
 */
public final class TestUtils {

    private TestUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Reads a resource file from the test resources directory.
     *
     * @param filename the filename relative to sample-data folder
     * @return the file contents as a String
     * @throws RuntimeException if file cannot be read
     */
    public static String getResourceFileContent(String filename) {
        String resourcePath = "sample-data/" + filename;
        try (InputStream is = TestUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                return content.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + resourcePath, e);
        }
    }

    /**
     * Generates a large CSV content for batch processing tests.
     *
     * @param rowCount number of data rows to generate
     * @param entityType the type of entity (PROJECT, ISSUE, USER)
     * @return CSV formatted String
     */
    public static String generateLargeCsv(int rowCount, String entityType) {
        return switch (entityType.toUpperCase()) {
            case "PROJECT" -> generateLargeProjectCsv(rowCount);
            case "ISSUE" -> generateLargeIssueCsv(rowCount);
            case "USER" -> generateLargeUserCsv(rowCount);
            default -> throw new IllegalArgumentException("Unknown entity type: " + entityType);
        };
    }

    private static String generateLargeProjectCsv(int rowCount) {
        StringBuilder csv = new StringBuilder();
        csv.append("project_key,name,description,project_type,lead_username\n");

        IntStream.rangeClosed(1, rowCount).forEach(i -> {
            csv.append("PROJ").append(String.format("%04d", i));
            csv.append(",Project ").append(i);
            csv.append(",Description for project ").append(i);
            csv.append(",COMPANY_MANAGED");
            csv.append(",admin\n");
        });

        return csv.toString();
    }

    private static String generateLargeIssueCsv(int rowCount) {
        StringBuilder csv = new StringBuilder();
        csv.append("project_key,issue_type,summary,description,status,priority,due_date,story_points\n");

        String[] statuses = {"Open", "In Progress", "Done", "Closed"};
        String[] priorities = {"Highest", "High", "Medium", "Low", "Lowest"};
        String[] types = {"Bug", "Story", "Task", "Epic", "Improvement"};

        IntStream.rangeClosed(1, rowCount).forEach(i -> {
            csv.append("PROJ").append(String.format("%04d", (i % 10) + 1));
            csv.append(",").append(types[i % types.length]);
            csv.append(",Test Issue ").append(i);
            csv.append(",Description for issue ").append(i);
            csv.append(",").append(statuses[i % statuses.length]);
            csv.append(",").append(priorities[i % priorities.length]);
            csv.append(",2026-12-31");
            csv.append(",").append((i % 10) + 1);
            csv.append("\n");
        });

        return csv.toString();
    }

    private static String generateLargeUserCsv(int rowCount) {
        StringBuilder csv = new StringBuilder();
        csv.append("username,email,display_name,department,active\n");

        IntStream.rangeClosed(1, rowCount).forEach(i -> {
            csv.append("user").append(String.format("%04d", i));
            csv.append(",user").append(String.format("%04d", i)).append("@test.com");
            csv.append(",Test User ").append(i);
            csv.append(",Engineering");
            csv.append(",true\n");
        });

        return csv.toString();
    }

    /**
     * Creates a test UUID for consistent testing.
     *
     * @return a fixed test UUID
     */
    public static UUID createTestUserId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    /**
     * Creates a random test UUID.
     *
     * @return a random UUID
     */
    public static UUID createRandomUuid() {
        return UUID.randomUUID();
    }

    /**
     * Generates invalid CSV content with various validation errors.
     *
     * @param errorType the type of error to generate
     * @return CSV formatted String with invalid data
     */
    public static String generateInvalidCsv(String errorType) {
        return switch (errorType) {
            case "MISSING_REQUIRED" -> """
                    project_key,name
                    ,Missing Key
                    PROJ1,
                    """;
            case "INVALID_PROJECT_KEY" -> """
                    project_key,name
                    invalid_lowercase,Valid Name
                    keywithwaytoolongname,Valid Name
                    123StartsWithNumber,Valid Name
                    """;
            case "INVALID_EMAIL" -> """
                    username,email,display_name
                    user1,invalid-email,User One
                    user2,@missing-domain.com,User Two
                    user3,user@,User Three
                    """;
            case "MALFORMED_CSV" -> """
                    project_key,name,description
                    PROJ1,Project One
                    "Unclosed quote, more text
                    PROJ2,Project Two
                    """;
            case "EMPTY_FILE" -> "";
            case "HEADERS_ONLY" -> "project_key,name,description\n";
            default -> throw new IllegalArgumentException("Unknown error type: " + errorType);
        };
    }
}
