package com.avionics_systems.migration.parser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportSpreadsheetParserTest {

    private final ImportSpreadsheetParser parser = new ImportSpreadsheetParser(
            new CsvParser(),
            new ExcelParser()
    );

    @Test
    void parseLegacyExportCsv_detectsHeadersAndRows() throws IOException {
        Path csv = Paths.get("..", "docs", "Legacy 2026-05-16T12_52_47+0530.csv").normalize();
        if (!Files.exists(csv)) {
            csv = Paths.get("docs", "Legacy 2026-05-16T12_52_47+0530.csv");
        }
        byte[] content = Files.readAllBytes(csv);

        CsvParser.CsvParseResult result = parser.parse(content, csv.getFileName().toString());

        assertTrue(result.getHeaders().length > 40, "Legacy export should expose many columns");
        assertEquals("Summary", result.getHeaders()[0].trim());
        assertEquals(5, result.getTotalRows(), "Sample export has 5 data rows");
        assertTrue(
                result.getDataRows().stream().anyMatch(r ->
                        java.util.Arrays.stream(r).anyMatch(c -> c != null && c.contains("PROJ-"))),
                "Expected issue keys in data"
        );
    }

    @Test
    void parseContent_stripsUtf8Bom() throws IOException {
        String csv = "\uFEFFSummary,Issue key\nfoo,PROJ-1\n";
        CsvParser.CsvParseResult result = parser.parse(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), "test.csv");
        assertEquals("Summary", result.getHeaders()[0].trim());
        assertEquals(1, result.getTotalRows());
    }
}
