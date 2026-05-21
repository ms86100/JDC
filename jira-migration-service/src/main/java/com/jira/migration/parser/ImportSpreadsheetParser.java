package com.jira.migration.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Unified parser for CSV and Excel migration uploads.
 */
@Component
public class ImportSpreadsheetParser {

    private final CsvParser csvParser;
    private final ExcelParser excelParser;

    public ImportSpreadsheetParser(CsvParser csvParser, ExcelParser excelParser) {
        this.csvParser = csvParser;
        this.excelParser = excelParser;
    }

    public boolean isExcelFile(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".xlsx");
    }

    public CsvParser.CsvParseResult parse(byte[] content, String fileName) throws IOException {
        if (isExcelFile(fileName)) {
            return excelParser.parseBytes(content, 1);
        }
        return csvParser.parseContent(new String(content, StandardCharsets.UTF_8), 1);
    }
}
