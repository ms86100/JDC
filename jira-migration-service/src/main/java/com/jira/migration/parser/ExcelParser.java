package com.jira.migration.parser;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Excel (.xlsx) migration files into the same shape as {@link CsvParser.CsvParseResult}.
 */
@Component
@Slf4j
public class ExcelParser {

    public CsvParser.CsvParseResult parseBytes(byte[] content, int dataStartRow) throws IOException {
        List<String[]> allRows = new ArrayList<>();
        List<String[]> dataRows = new ArrayList<>();
        String[] headerRow = null;

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IOException("Excel file has no sheets");
            }

            int rowIndex = 0;
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                String[] cells = readRow(row);
                if (isEmptyRow(cells)) {
                    continue;
                }
                rowIndex++;
                allRows.add(cells);

                if (rowIndex == dataStartRow) {
                    headerRow = cells;
                    log.info("Excel headers: {}", String.join(", ", headerRow));
                } else if (rowIndex > dataStartRow) {
                    dataRows.add(cells);
                }
            }
        }

        if (headerRow == null) {
            throw new IOException("Excel file has no header row");
        }

        return CsvParser.CsvParseResult.builder()
                .headers(headerRow)
                .allRows(allRows)
                .dataRows(dataRows)
                .totalRows(dataRows.size())
                .build();
    }

    private String[] readRow(Row row) {
        int lastCell = Math.max(row.getLastCellNum(), 0);
        String[] cells = new String[lastCell];
        for (int i = 0; i < lastCell; i++) {
            cells[i] = cellToString(row.getCell(i));
        }
        return cells;
    }

    private String cellToString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : trimNumeric(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield trimNumeric(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    private String trimNumeric(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private boolean isEmptyRow(String[] cells) {
        for (String c : cells) {
            if (c != null && !c.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
