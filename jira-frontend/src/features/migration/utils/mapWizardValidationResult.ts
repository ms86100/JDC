import type { ValidationError, ValidationResult } from '../types/migration';

/** Raw validation payload from migration-service wizard validate endpoint. */
export interface ServerValidationPayload {
  valid?: boolean;
  errors?: Array<{
    field?: string;
    row?: number;
    errorCode?: string;
    message?: string;
    invalidValue?: unknown;
  }>;
  warnings?: Array<{
    field?: string;
    row?: number;
    warningCode?: string;
    message?: string;
  }>;
}

function mapServerIssue(
  item: NonNullable<ServerValidationPayload['errors']>[number],
  severity: 'ERROR' | 'WARNING'
): ValidationError {
  const code =
    severity === 'ERROR'
      ? (item as { errorCode?: string }).errorCode
      : (item as { warningCode?: string }).warningCode;
  return {
    row: item?.row ?? 0,
    column: item?.field ?? '',
    value:
      item && 'invalidValue' in item && item.invalidValue != null
        ? String(item.invalidValue)
        : '',
    severity,
    message: item?.message ?? '',
    code: code ?? (severity === 'ERROR' ? 'VALIDATION_ERROR' : 'VALIDATION_WARNING'),
  };
}

export function mapWizardValidationResult(
  server: ServerValidationPayload,
  previous?: ValidationResult | null
): ValidationResult {
  const errors = (server.errors ?? []).map((e) => mapServerIssue(e, 'ERROR'));
  const warnings = (server.warnings ?? []).map((w) => mapServerIssue(w, 'WARNING'));
  const totalRows = previous?.totalRows ?? 0;
  const errorRows = new Set(errors.map((e) => e.row).filter((r) => r > 0));
  const validRows =
    errors.length === 0
      ? totalRows
      : Math.max(0, totalRows - errorRows.size);

  return {
    fileName: previous?.fileName ?? '',
    totalRows,
    validRows,
    errors,
    warnings,
    headers: previous?.headers ?? [],
    previewRows: previous?.previewRows ?? [],
  };
}

export function hasBlockingValidationErrors(result: ValidationResult | null | undefined): boolean {
  return (result?.errors?.length ?? 0) > 0;
}
