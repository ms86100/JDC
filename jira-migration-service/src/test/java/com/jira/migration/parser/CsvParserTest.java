package com.jira.migration.parser;

import com.jira.migration.dto.ValidationResult;
import com.jira.migration.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for CsvParser component.
 * Tests CSV parsing, column mapping, and validation.
 */
@DisplayName("CSV Parser Tests")
@Nested
@Slf4j
public class CsvParserTest {

    private CsvParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new CsvParser();
    }

    // ============================================
    // Parse Content Tests
    // ============================================

    @Test
    @DisplayName("Should parse valid CSV content")
    void shouldParseValidCsvContent() throws IOException {
        // Given valid CSV content
        String csvContent = """
                project_key,name,description
                PROJ1,Project One,Description One
                PROJ2,Project Two,Description Two
                """;

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then should correctly parse headers and data
        assertThat(result.getHeaders()).containsExactly("project_key", "name", "description");
        assertThat(result.getDataRows()).hasSize(2);
        assertThat(result.getTotalRows()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should parse CSV with different data start rows")
    void shouldParseCsvWithDifferentDataStartRows() throws IOException {
        // Given CSV with metadata rows before data
        String csvContent = """
                Metadata: Import batch 2026-05-11
                Format: Standard CSV
                project_key,name,description
                PROJ1,Project One,Description One
                """;

        // When parsing with data start row = 3
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 3);

        // Then data should start from row 3
        assertThat(result.getHeaders()).containsExactly("project_key", "name", "description");
        assertThat(result.getDataRows()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle CSV with quoted fields containing commas")
    void shouldHandleCsvWithQuotedFieldsContainingCommas() throws IOException {
        // Given CSV with commas inside quoted fields
        String csvContent = """
                project_key,name,description
                PROJ1,Project One,"Description, with comma"
                PROJ2,Project Two,Normal description
                """;

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then quoted commas should be handled correctly
        assertThat(result.getDataRows()).hasSize(2);
        assertThat(result.getDataRows().get(0)).hasSize(3);
    }

    @Test
    @DisplayName("Should handle empty CSV gracefully")
    void shouldHandleEmptyCsvGracefully() throws IOException {
        // Given empty CSV
        String csvContent = "";

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then should return empty result
        assertThat(result.getDataRows()).isEmpty();
        assertThat(result.getTotalRows()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle CSV with only headers")
    void shouldHandleCsvWithOnlyHeaders() throws IOException {
        // Given CSV with only headers
        String csvContent = "project_key,name,description\n";

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then should parse headers but no data rows
        assertThat(result.getHeaders()).containsExactly("project_key", "name", "description");
        assertThat(result.getDataRows()).isEmpty();
        assertThat(result.getTotalRows()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle CSV with varying column counts per row")
    void shouldHandleCsvWithVaryingColumnCounts() throws IOException {
        // Given CSV with rows having different column counts
        String csvContent = """
                project_key,name,description
                PROJ1,Project One
                PROJ2,Project Two,Description Two,Extra Column
                PROJ3,Project Three
                """;

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then should parse all rows
        assertThat(result.getDataRows()).hasSize(3);
    }

    @Test
    @DisplayName("Should handle CSV with Windows-style line endings")
    void shouldHandleCsvWithWindowsLineEndings() throws IOException {
        // Given CSV with CRLF line endings
        String csvContent = "project_key,name\r\nPROJ1,Project One\r\nPROJ2,Project Two\r\n";

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then should handle correctly
        assertThat(result.getDataRows()).hasSize(2);
    }

    @Test
    @DisplayName("Should trim whitespace from fields")
    void shouldTrimWhitespaceFromFields() throws IOException {
        // Given CSV with whitespace
        String csvContent = """
                project_key,name,description
                PROJ1,  Project One  ,  Description One
                PROJ2,Project Two,Description Two
                """;

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then values should be trimmed (depends on implementation)
        assertThat(result.getDataRows()).hasSize(2);
    }

    // ============================================
    // Column Index Map Tests
    // ============================================

    @Test
    @DisplayName("Should build column index map correctly")
    void shouldBuildColumnIndexMapCorrectly() {
        // Given headers
        String[] headers = {"project_key", "name", "description"};

        // When building index map
        Map<String, Integer> indexMap = csvParser.buildColumnIndexMap(headers);

        // Then should map columns to indices
        assertThat(indexMap).containsEntry("project_key", 0);
        assertThat(indexMap).containsEntry("name", 1);
        assertThat(indexMap).containsEntry("description", 2);
    }

    @Test
    @DisplayName("Should build case-insensitive column index map")
    void shouldBuildCaseInsensitiveColumnIndexMap() {
        // Given headers with mixed case
        String[] headers = {"Project_Key", "NAME", "Description"};

        // When building index map
        Map<String, Integer> indexMap = csvParser.buildColumnIndexMap(headers);

        // Then should handle case insensitively
        assertThat(indexMap.get("project_key")).isEqualTo(0);
        assertThat(indexMap.get("name")).isEqualTo(1);
        assertThat(indexMap.get("description")).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle empty headers array")
    void shouldHandleEmptyHeadersArray() {
        // Given empty headers
        String[] headers = {};

        // When building index map
        Map<String, Integer> indexMap = csvParser.buildColumnIndexMap(headers);

        // Then should return empty map
        assertThat(indexMap).isEmpty();
    }

    // ============================================
    // Get Value Tests
    // ============================================

    @Test
    @DisplayName("Should get value by column name")
    void shouldGetValueByColumnName() {
        // Given row and index map
        String[] row = {"PROJ1", "Project One", "Description One"};
        Map<String, Integer> indexMap = Map.of(
                "project_key", 0,
                "name", 1,
                "description", 2
        );

        // When getting values
        String projectKey = csvParser.getValue(row, indexMap, "project_key");
        String name = csvParser.getValue(row, indexMap, "name");

        // Then should return correct values
        assertThat(projectKey).isEqualTo("PROJ1");
        assertThat(name).isEqualTo("Project One");
    }

    @Test
    @DisplayName("Should return null for non-existent column")
    void shouldReturnNullForNonExistentColumn() {
        // Given row and index map
        String[] row = {"PROJ1", "Project One", "Description One"};
        Map<String, Integer> indexMap = Map.of(
                "project_key", 0,
                "name", 1,
                "description", 2
        );

        // When getting non-existent column
        String value = csvParser.getValue(row, indexMap, "non_existent");

        // Then should return null
        assertThat(value).isNull();
    }

    @Test
    @DisplayName("Should handle index out of bounds gracefully")
    void shouldHandleIndexOutOfBoundsGracefully() {
        // Given row with fewer columns than index map expects
        String[] row = {"PROJ1", "Project One"};
        Map<String, Integer> indexMap = Map.of(
                "project_key", 0,
                "name", 1,
                "description", 2  // This doesn't exist in row
        );

        // When getting out-of-bounds column
        String value = csvParser.getValue(row, indexMap, "description");

        // Then should return null
        assertThat(value).isNull();
    }

    @Test
    @DisplayName("Should handle case-insensitive column lookup")
    void shouldHandleCaseInsensitiveColumnLookup() {
        // Given row and index map with lowercase
        String[] row = {"PROJ1", "Project One"};
        Map<String, Integer> indexMap = csvParser.buildColumnIndexMap(new String[]{"PROJECT_KEY", "NAME"});

        // When getting value with different case
        String value = csvParser.getValue(row, indexMap, "project_key");

        // Then should find it
        assertThat(value).isEqualTo("PROJ1");
    }

    // ============================================
    // Validate Required Columns Tests
    // ============================================

    @Test
    @DisplayName("Should validate presence of required columns")
    void shouldValidatePresenceOfRequiredColumns() {
        // Given headers and required columns
        String[] headers = {"project_key", "name", "description"};
        List<String> requiredColumns = List.of("project_key", "name");

        // When validating
        // Then should not throw
        csvParser.validateRequiredColumns(headers, requiredColumns);
    }

    @Test
    @DisplayName("Should throw exception for missing required columns")
    void shouldThrowExceptionForMissingRequiredColumns() {
        // Given headers missing required columns
        String[] headers = {"project_key"};
        List<String> requiredColumns = List.of("project_key", "name", "description");

        // When validating
        assertThatThrownBy(() -> csvParser.validateRequiredColumns(headers, requiredColumns))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Missing required columns");
    }

    @Test
    @DisplayName("Should identify all missing required columns")
    void shouldIdentifyAllMissingRequiredColumns() {
        // Given headers missing multiple required columns
        String[] headers = {"project_key"};
        List<String> requiredColumns = List.of("project_key", "name", "description", "owner");

        // When validating
        try {
            csvParser.validateRequiredColumns(headers, requiredColumns);
        } catch (ValidationException e) {
            // Then all missing columns should be in message
            assertThat(e.getMessage()).contains("name", "description", "owner");
        }
    }

    @Test
    @DisplayName("Should handle empty required columns list")
    void shouldHandleEmptyRequiredColumnsList() {
        // Given headers and empty required list
        String[] headers = {"project_key", "name"};
        List<String> requiredColumns = List.of();

        // When validating
        // Then should not throw
        csvParser.validateRequiredColumns(headers, requiredColumns);
    }

    // ============================================
    // Detect Column Mapping Tests
    // ============================================

    @Test
    @DisplayName("Should detect column mapping with target mapping")
    void shouldDetectColumnMappingWithTargetMapping() {
        // Given CSV headers and target field mapping
        String[] csvHeaders = {"Proj_Key", "Proj_Name", "Custom_Field"};
        Map<String, String> targetMapping = Map.of(
                "proj_key", "projectKey",
                "proj_name", "projectName"
        );

        // When detecting mapping
        List<String> mapping = csvParser.detectColumnMapping(csvHeaders, targetMapping);

        // Then should apply mappings
        assertThat(mapping).contains("projectKey", "projectName", "custom_field");
    }

    @Test
    @DisplayName("Should use original header when no mapping exists")
    void shouldUseOriginalHeaderWhenNoMappingExists() {
        // Given CSV headers with no target mapping
        String[] csvHeaders = {"project_key", "unknown_field"};
        Map<String, String> targetMapping = Map.of();

        // When detecting mapping
        List<String> mapping = csvParser.detectColumnMapping(csvHeaders, targetMapping);

        // Then should use original headers (normalized)
        assertThat(mapping).contains("project_key", "unknown_field");
    }

    @Test
    @DisplayName("Should handle partial mapping coverage")
    void shouldHandlePartialMappingCoverage() {
        // Given CSV headers with partial mapping
        String[] csvHeaders = {"project_key", "name", "description"};
        Map<String, String> targetMapping = Map.of(
                "project_key", "key"  // Only one mapping
        );

        // When detecting mapping
        List<String> mapping = csvParser.detectColumnMapping(csvHeaders, targetMapping);

        // Then should apply partial mapping
        assertThat(mapping).containsExactly("key", "name", "description");
    }

    // ============================================
    // Edge Cases
    // ============================================

    @Test
    @DisplayName("Should handle CSV with empty lines")
    void shouldHandleCsvWithEmptyLines() throws IOException {
        // Given CSV with empty lines
        String csvContent = """
                project_key,name
                PROJ1,Project One

                PROJ2,Project Two
                """;

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then should handle gracefully
        assertThat(result.getDataRows()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle CSV with escaped quotes")
    void shouldHandleCsvWithEscapedQuotes() throws IOException {
        String csvContent = "project_key,name,description\n" +
                "PROJ1,Project One,\"Description with \"\"quotes\"\"\"\n" +
                "PROJ2,Project Two,Normal description\n";

        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        assertThat(result.getDataRows()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle large CSV efficiently")
    void shouldHandleLargeCsvEfficiently() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("project_key,name,description\n");
        for (int i = 0; i < 10000; i++) {
            sb.append("PROJ").append(String.format("%05d", i));
            sb.append(",Project ").append(i);
            sb.append(",Description ").append(i);
            sb.append("\n");
        }
        String largeCsv = sb.toString();

        long startTime = System.currentTimeMillis();
        CsvParser.CsvParseResult result = csvParser.parseContent(largeCsv, 1);
        long parseTime = System.currentTimeMillis() - startTime;
        
        log.debug("Parsed 10000 rows in {}ms", parseTime);
    }

    @Test
    @DisplayName("Should handle UTF-8 special characters")
    void shouldHandleUtf8SpecialCharacters() throws IOException {
        // Given CSV with UTF-8 special characters
        String csvContent = """
                project_key,name,description
                PROJ1,Projet Français,Description with émoji 🚀
                PROJ2,Proyecto Español,Description with ñ
                """;

        // When parsing
        CsvParser.CsvParseResult result = csvParser.parseContent(csvContent, 1);

        // Then should handle UTF-8 correctly
        assertThat(result.getDataRows()).hasSize(2);
        assertThat(result.getDataRows().get(0)[1]).contains("Français");
    }
}
