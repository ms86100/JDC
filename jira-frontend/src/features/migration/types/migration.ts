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

export interface MigrationTargetField {
  field: string;
  displayName: string;
  dataType: string;
  required: boolean;
  description?: string;
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

export interface StageProgress {
  completed: number;
  total: number;
}

export interface JobProgress {
  jobId: string;
  jobStatus: MigrationJob['jobStatus'];
  errorMessage?: string;
  progressPercentage: number;
  totalEntities: number;
  processedEntities: number;
  failedEntities: number;
  entityProgress: EntityProgress[];
  stages?: Record<string, StageProgress>;
  currentPhase?: string;
  recentLogs?: Array<{ timestamp?: string; level?: string; message?: string }>;
  attachmentBytesWritten?: number;
  attachmentsCompleted?: number;
  incrementalSkipped?: number;
  attachmentChunkIndex?: number;
  attachmentChunkTotal?: number;
  attachmentCurrentFile?: string;
  attachmentChunked?: boolean;
  currentStep?: string;
  startedAt?: string;
  estimatedTimeRemaining?: number;
  elapsedTimeMs?: number;
}

export interface ImportResult {
  jobId: string;
  jobStatus: MigrationJob['jobStatus'];
  totalEntities: number;
  processedEntities: number;
  failedEntities: number;
  successCount: number;
  warningCount: number;
  durationMs?: number;
  resultMetadata?: Record<string, unknown>;
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
  /** Jira DC: LIGHTWEIGHT vs External System Import (G-03) */
  csvImportProfile?: 'LIGHTWEIGHT' | 'EXTERNAL';
  attachmentColumn?: string;
  attachmentsImportDir?: string;
  skipValidation?: boolean;
  fieldMappings?: FieldMappingRule[];
  onConflict?: 'SKIP' | 'OVERWRITE' | 'ERROR';
}

export type ImportType =
  | 'csv'
  | 'issue-xml'
  | 'jira-dc'
  | 'workflow-xml'
  | 'project-import'
  | 'project-export';

export interface MigrationState {
  step:
    | 'source'
    | 'targetProject'
    | 'map'
    | 'validate'
    | 'configure'
    | 'review'
    | 'importing'
    | 'complete';
  importType: ImportType | null;
  selectedFile: File | null;
  validationResult: ValidationResult | null;
  fieldMappings: FieldMapping[];
  importOptions: ImportOptions;
  currentJobId: string | null;
  jobProgress: JobProgress | null;
  importResult: ImportResult | null;
}
