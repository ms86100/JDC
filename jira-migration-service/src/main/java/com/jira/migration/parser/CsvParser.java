package com.jira.migration.parser;

import com.jira.migration.dto.ValidationResult;
import com.jira.migration.exception.ValidationException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class CsvParser {

    public CsvParseResult parseFile(String filePath, String[] headers, int dataStartRow) throws IOException {
        List<String[]> allRows = new ArrayList<>();
        List<String[]> dataRows = new ArrayList<>();
        String[] headerRow = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            int lineNum = 0;

            while ((line = csvReader.readNext()) != null) {
                // Skip empty lines without counting them in lineNum
                if (line.length == 1 && line[0].isEmpty()) {
                    continue;
                }

                lineNum++;
                allRows.add(line);

                if (lineNum == 1) {
                    headerRow = line;
                    log.info("CSV Headers: {}", String.join(", ", headerRow));
                } else if (lineNum >= dataStartRow) {
                    dataRows.add(line);
                }
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSV validation error: " + e.getMessage(), e);
        }

        return CsvParseResult.builder()
                .headers(headerRow)
                .allRows(allRows)
                .dataRows(dataRows)
                .totalRows(dataRows.size())
                .filePath(filePath)
                .build();
    }

    public CsvParseResult parseContent(String csvContent, int dataStartRow) throws IOException {
        List<String[]> allRows = new ArrayList<>();
        List<String[]> dataRows = new ArrayList<>();
        String[] headerRow = null;

        try (BufferedReader reader = new BufferedReader(
                new StringReader(csvContent));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            int lineNum = 0;

            while ((line = csvReader.readNext()) != null) {
                // Skip empty lines without counting them in lineNum
                if (line.length == 1 && line[0].isEmpty()) {
                    continue;
                }

                lineNum++;
                allRows.add(line);

                if (lineNum == dataStartRow) {
                    // Header row is at dataStartRow
                    headerRow = line;
                } else if (lineNum > dataStartRow) {
                    dataRows.add(line);
                }
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSV validation error: " + e.getMessage(), e);
        }

        return CsvParseResult.builder()
                .headers(headerRow)
                .allRows(allRows)
                .dataRows(dataRows)
                .totalRows(dataRows.size())
                .build();
    }

    public Map<String, Integer> buildColumnIndexMap(String[] headers) {
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            indexMap.put(headers[i].trim().toLowerCase(), i);
        }
        return indexMap;
    }

    public String getValue(String[] row, Map<String, Integer> indexMap, String columnName) {
        Integer index = indexMap.get(columnName.toLowerCase());
        if (index == null || index >= row.length) {
            return null;
        }
        return row[index] != null ? row[index].trim() : null;
    }

    public void validateRequiredColumns(String[] headers, List<String> requiredColumns) {
        Set<String> headerSet = new HashSet<>();
        for (String h : headers) {
            headerSet.add(h.trim().toLowerCase());
        }

        List<String> missingColumns = new ArrayList<>();
        for (String required : requiredColumns) {
            if (!headerSet.contains(required.toLowerCase())) {
                missingColumns.add(required);
            }
        }

        if (!missingColumns.isEmpty()) {
            throw new ValidationException(
                    "Missing required columns: " + String.join(", ", missingColumns),
                    "MISSING_REQUIRED_COLUMNS",
                    "headers"
            );
        }
    }

    public List<String> detectColumnMapping(String[] csvHeaders, Map<String, String> targetFieldMapping) {
        List<String> mapping = new ArrayList<>();
        for (String csvHeader : csvHeaders) {
            String normalizedHeader = csvHeader.trim().toLowerCase();
            String mappedField = targetFieldMapping.get(normalizedHeader);
            if (mappedField != null) {
                mapping.add(mappedField);
            } else {
                mapping.add(normalizedHeader);
            }
        }
        return mapping;
    }

    public static class CsvParseResult {
        private String[] headers;
        private List<String[]> allRows;
        private List<String[]> dataRows;
        private int totalRows;
        private String filePath;

        public static CsvParseResultBuilder builder() {
            return new CsvParseResultBuilder();
        }

        public String[] getHeaders() { return headers; }
        public List<String[]> getAllRows() { return allRows; }
        public List<String[]> getDataRows() { return dataRows; }
        public int getTotalRows() { return totalRows; }
        public String getFilePath() { return filePath; }

        public static class CsvParseResultBuilder {
            private String[] headers;
            private List<String[]> allRows = new ArrayList<>();
            private List<String[]> dataRows = new ArrayList<>();
            private int totalRows;
            private String filePath;

            public CsvParseResultBuilder headers(String[] headers) { this.headers = headers; return this; }
            public CsvParseResultBuilder allRows(List<String[]> allRows) { this.allRows = allRows; return this; }
            public CsvParseResultBuilder dataRows(List<String[]> dataRows) { this.dataRows = dataRows; return this; }
            public CsvParseResultBuilder totalRows(int totalRows) { this.totalRows = totalRows; return this; }
            public CsvParseResultBuilder filePath(String filePath) { this.filePath = filePath; return this; }

            public CsvParseResult build() {
                CsvParseResult result = new CsvParseResult();
                result.headers = this.headers;
                result.allRows = this.allRows;
                result.dataRows = this.dataRows;
                result.totalRows = this.totalRows;
                result.filePath = this.filePath;
                return result;
            }
        }
    }
}