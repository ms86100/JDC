import { useState, useCallback } from 'react';
import { useMutation } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';
import type { ValidationResult, ValidationError, FieldMapping } from '../types/migration';
import { matchHeaderToTargetField } from '../utils/fieldMappingMatch';

const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
const MAX_PREVIEW_ROWS = 10;
const MAX_COLUMNS = 100;

interface UseValidationOptions {
  onValidationComplete?: (result: ValidationResult) => void;
  onValidationError?: (error: Error) => void;
}

export function useValidation(options: UseValidationOptions = {}) {
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  const [parseError, setParseError] = useState<string | null>(null);

  // Parse CSV file client-side
  const parseCsvFile = useCallback(async (file: File): Promise<{
    headers: string[];
    rows: string[][];
    totalRows: number;
  }> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onload = (event) => {
        try {
          let content = event.target?.result as string;
          if (content.charCodeAt(0) === 0xfeff) {
            content = content.slice(1);
          }
          const lines = content.split(/\r?\n/).filter((line) => line.trim());

          if (lines.length === 0) {
            reject(new Error('File is empty'));
            return;
          }

          // Parse header row
          const headers = parseCsvRow(lines[0]);

          if (headers.length > MAX_COLUMNS) {
            reject(new Error(`File has too many columns (max ${MAX_COLUMNS})`));
            return;
          }

          // Parse data rows (limit preview)
          const dataRows = lines.slice(1);
          const previewRows = dataRows.slice(0, MAX_PREVIEW_ROWS).map(parseCsvRow);

          resolve({
            headers,
            rows: previewRows,
            totalRows: dataRows.length,
          });
        } catch (error) {
          reject(new Error('Failed to parse CSV file'));
        }
      };

      reader.onerror = () => {
        reject(new Error('Failed to read file'));
      };

      reader.readAsText(file);
    });
  }, []);

  // Parse a single CSV row handling quoted values
  const parseCsvRow = (row: string): string[] => {
    const result: string[] = [];
    let current = '';
    let inQuotes = false;

    for (let i = 0; i < row.length; i++) {
      const char = row[i];
      const nextChar = row[i + 1];

      if (char === '"') {
        if (inQuotes && nextChar === '"') {
          // Escaped quote
          current += '"';
          i++;
        } else {
          // Toggle quote mode
          inQuotes = !inQuotes;
        }
      } else if (char === ',' && !inQuotes) {
        result.push(current.trim());
        current = '';
      } else {
        current += char;
      }
    }

    result.push(current.trim());
    return result;
  };

  // Validate CSV file client-side
  const validateCsvClientSide = useCallback(
    async (file: File): Promise<ValidationResult> => {
      setIsValidating(true);
      setParseError(null);

      try {
        // Check file size
        if (file.size > MAX_FILE_SIZE) {
          throw new Error(`File size exceeds maximum of ${MAX_FILE_SIZE / (1024 * 1024)}MB`);
        }

        // Check file extension
        if (!file.name.toLowerCase().endsWith('.csv')) {
          throw new Error('File must be a CSV file');
        }

        // Parse file
        const { headers, rows, totalRows } = await parseCsvFile(file);

        // Validate headers
        const headerErrors: ValidationError[] = [];
        const headerWarnings: ValidationError[] = [];

        // Check for empty headers
        headers.forEach((header, index) => {
          if (!header.trim()) {
            headerErrors.push({
              row: 0,
              column: `Column ${index + 1}`,
              value: '',
              severity: 'ERROR',
              message: 'Empty header name',
              code: 'EMPTY_HEADER',
            });
          }
        });

        // Check for duplicate headers
        const headerSet = new Set<string>();
        headers.forEach((header) => {
          const normalized = header.toLowerCase().trim();
          if (headerSet.has(normalized)) {
            headerWarnings.push({
              row: 0,
              column: header,
              value: header,
              severity: 'WARNING',
              message: 'Duplicate column header',
              code: 'DUPLICATE_HEADER',
            });
          }
          headerSet.add(normalized);
        });

        // Validate data rows
        const dataErrors: ValidationError[] = [];
        const dataWarnings: ValidationError[] = [];
        let validRowCount = totalRows;

        rows.forEach((row, rowIndex) => {
          // Check column count mismatch
          if (row.length !== headers.length) {
            dataWarnings.push({
              row: rowIndex + 2, // +2 for 1-indexed and header row
              column: 'All',
              value: row.join(', '),
              severity: 'WARNING',
              message: `Row has ${row.length} columns, expected ${headers.length}`,
              code: 'COLUMN_MISMATCH',
            });
            validRowCount--;
          }

          // Check for empty rows
          if (row.every((cell) => !cell.trim())) {
            dataWarnings.push({
              row: rowIndex + 2,
              column: 'All',
              value: '',
              severity: 'WARNING',
              message: 'Row is empty',
              code: 'EMPTY_ROW',
            });
            validRowCount--;
          }

          // Check for required field patterns (common Systems and Avionics fields)
          const requiredFields = ['summary', 'description', 'issuetype', 'priority', 'project'];
          requiredFields.forEach((field) => {
            const fieldIndex = headers.findIndex(
              (h) => h.toLowerCase().replace(/[\s_-]/g, '') === field
            );
            if (fieldIndex !== -1) {
              const value = row[fieldIndex];
              if (!value.trim()) {
                dataWarnings.push({
                  row: rowIndex + 2,
                  column: headers[fieldIndex],
                  value,
                  severity: 'WARNING',
                  message: `Required field "${field}" is empty`,
                  code: 'REQUIRED_FIELD_EMPTY',
                });
              }
            }
          });

          // Check for invalid characters
          row.forEach((cell, colIndex) => {
            // Check for unclosed quotes
            const quoteCount = (cell.match(/"/g) || []).length;
            if (quoteCount % 2 !== 0) {
              dataErrors.push({
                row: rowIndex + 2,
                column: headers[colIndex],
                value: cell.substring(0, 50) + (cell.length > 50 ? '...' : ''),
                severity: 'ERROR',
                message: 'Unclosed quote in cell',
                code: 'UNCLOSED_QUOTE',
              });
            }

            // Check for invalid characters in specific fields
            if (headers[colIndex]?.toLowerCase().includes('key')) {
              if (!/^[A-Z][A-Z0-9]*-[0-9]+$/i.test(cell) && cell.trim()) {
                dataWarnings.push({
                  row: rowIndex + 2,
                  column: headers[colIndex],
                  value: cell,
                  severity: 'WARNING',
                  message: 'Issue key format may be invalid (expected format: PROJECT-123)',
                  code: 'INVALID_KEY_FORMAT',
                });
              }
            }
          });
        });

        const result: ValidationResult = {
          fileName: file.name,
          totalRows,
          validRows: validRowCount,
          errors: [...headerErrors, ...dataErrors],
          warnings: [...headerWarnings, ...dataWarnings],
          headers,
          previewRows: rows,
        };

        setValidationResult(result);
        options.onValidationComplete?.(result);
        return result;
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Validation failed';
        setParseError(errorMessage);
        options.onValidationError?.(error instanceof Error ? error : new Error(errorMessage));
        throw error;
      } finally {
        setIsValidating(false);
      }
    },
    [parseCsvFile, options]
  );

  // Validate with server
  const validateCsvServerSide = useMutation({
    mutationFn: async ({ file, entityType }: { file: File; entityType: string }) => {
      const response = await migrationApi.validateCsv(file, entityType);
      return response.data;
    },
    onSuccess: (data) => {
      setValidationResult(data as unknown as ValidationResult);
      options.onValidationComplete?.(data as unknown as ValidationResult);
    },
    onError: (error: Error) => {
      options.onValidationError?.(error);
    },
  });

  // Validate single row
  const validateRow = useMutation({
    mutationFn: async ({ row, entityType }: { row: Record<string, string>; entityType: string }) => {
      const response = await migrationApi.validateRow(row, entityType);
      return response.data;
    },
    onError: (error: Error) => {
      console.error('Failed to validate row:', error.message);
    },
  });

  // Generate field mappings based on headers
  const generateFieldMappings = useCallback(
    (headers: string[], targetFields: Array<{ field: string; dataType: string; required: boolean }>): FieldMapping[] => {
      const mappings: FieldMapping[] = [];

      const targetFieldKeys = targetFields.map((tf) => tf.field);

      headers.forEach((header) => {
        const matchedField = matchHeaderToTargetField(header, targetFieldKeys) ?? '';
        const matched = matchedField.length > 0;

        const targetFieldInfo = targetFields.find((tf) => tf.field === matchedField);

        mappings.push({
          sourceColumn: header,
          targetField: matchedField || '',
          dataType: targetFieldInfo?.dataType || 'STRING',
          required: targetFieldInfo?.required || false,
          mapped: matched,
        });
      });

      return mappings;
    },
    []
  );

  // Export validation errors to CSV
  const exportErrorsToCsv = useCallback((errors: ValidationError[]): void => {
    if (errors.length === 0) return;

    const headers = ['Row', 'Column', 'Severity', 'Code', 'Value', 'Message'];
    const rows = errors.map((error) => [
      error.row.toString(),
      error.column,
      error.severity,
      error.code,
      `"${error.value.replace(/"/g, '""')}"`,
      `"${error.message.replace(/"/g, '""')}"`,
    ]);

    const csv = [headers.join(','), ...rows.map((row) => row.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `validation_errors_${Date.now()}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }, []);

  // Reset validation state
  const resetValidation = useCallback(() => {
    setValidationResult(null);
    setParseError(null);
    setIsValidating(false);
  }, []);

  return {
    // State
    validationResult,
    setValidationResult,
    isValidating,
    parseError,

    // Client-side validation
    validateCsvClientSide,
    parseCsvFile,

    // Server-side validation
    validateCsvServerSide,
    validateRow,

    // Utilities
    generateFieldMappings,
    exportErrorsToCsv,
    resetValidation,

    // Constants
    maxFileSize: MAX_FILE_SIZE,
    maxPreviewRows: MAX_PREVIEW_ROWS,
  };
}

export type UseValidationReturn = ReturnType<typeof useValidation>;
