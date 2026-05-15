// Migration Types

export interface MigrationJob {
  id: string;
  jobType: 'CSV' | 'JIRA_DC' | 'PROJECT_IMPORT' | 'PROJECT_EXPORT';
  jobStatus: 'PENDING' | 'VALIDATING' | 'MAPPING' | 'IMPORTING' | 'INDEXING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  importSource?: string;
  totalEntities?: number;
  processedEntities?: number;
  failedEntities?: number;
  progressPercentage?: number;
  initiatedBy?: string;
  initiatedAt?: string;
  startedAt?: string;
  completedAt?: string;
  errorMessage?: string;
  warnings?: string[];
}

export interface ValidationError {
  row: number;
  column: string;
  value: string;
  severity: 'ERROR' | 'WARNING';
  message: string;
  code: string;
}

export interface ValidationResult {
  fileName: string;
  totalRows: number;
  validRows: number;
  errors: ValidationError[];
  warnings: ValidationError[];
  headers: string[];
  previewRows: string[][];
}

export interface FieldMapping {
  sourceColumn: string;
  targetField: string;
  dataType: string;
  required: boolean;
  mapped: boolean;
  transformer?: string;
}

export interface ImportHistoryItem {
  id: string;
  jobType: string;
  status: string;
  totalEntities: number;
  successCount: number;
  failedCount: number;
  initiatedAt: string;
  completedAt?: string;
  initiatedBy: string;
}

export interface EntityProgress {
  entityType: string;
  total: number;
  completed: number;
  failed: number;
}

export interface JobProgress {
  jobId: string;
  jobStatus: MigrationJob['jobStatus'];
  progressPercentage: number;
  totalEntities: number;
  processedEntities: number;
  failedEntities: number;
  entityProgress: EntityProgress[];
  currentStep?: string;
  startedAt?: string;
  estimatedTimeRemaining?: number;
}

export interface ImportResult {
  jobId: string;
  jobStatus: MigrationJob['jobStatus'];
  totalEntities: number;
  processedEntities: number;
  failedEntities: number;
  successCount: number;
  warningCount: number;
  errors: ImportError[];
  warnings: ImportWarning[];
}

export interface ImportError {
  entityType: string;
  entityKey: string;
  row?: number;
  field?: string;
  errorCode: string;
  errorMessage: string;
}

export interface ImportWarning {
  entityType: string;
  entityKey: string;
  row?: number;
  field?: string;
  warningMessage: string;
}

export interface CsvTemplate {
  id: string;
  templateName: string;
  entityType: string;
  version: string;
  columns: TemplateColumn[];
}

export interface TemplateColumn {
  columnName: string;
  displayName: string;
  dataType: string;
  required: boolean;
  description?: string;
  sampleValue?: string;
}

export interface FieldMappingRule {
  id: string;
  sourceColumn: string;
  targetField: string;
  dataType: string;
  transformer?: string;
  defaultValue?: string;
  skipEmpty?: boolean;
}

export interface ImportOptions {
  targetProjectId?: string;
  importMode: 'CREATE_ONLY' | 'UPDATE_ONLY' | 'CREATE_UPDATE';
  skipValidation?: boolean;
  fieldMappings?: FieldMappingRule[];
  onConflict?: 'SKIP' | 'OVERWRITE' | 'ERROR';
}

export type ImportType = 'csv' | 'jira-dc' | 'project-import' | 'project-export';

export interface MigrationState {
  step: 'select' | 'upload' | 'validate' | 'map' | 'preview' | 'importing' | 'complete';
  importType: ImportType | null;
  selectedFile: File | null;
  validationResult: ValidationResult | null;
  fieldMappings: FieldMapping[];
  importOptions: ImportOptions;
  currentJobId: string | null;
  jobProgress: JobProgress | null;
  importResult: ImportResult | null;
}
