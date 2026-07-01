import apiClient from './axiosClient';

// ============================================
// AUDIT API
// ============================================
export interface AuditLogResponse {
  id: string;
  userId: string;
  username?: string;
  serviceName: string;
  entityType: string;
  entityId: string;
  action: string;
  changes?: Record<string, any>;
  ipAddress?: string;
  createdAt: string;
}

export interface AuditSearchParams {
  entityType?: string;
  entityId?: string;
  userId?: string;
  action?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export const auditApi = {
  getLogs: (params?: AuditSearchParams) =>
    apiClient.get<{ content: AuditLogResponse[]; totalElements: number; totalPages: number }>(
      '/audit/logs',
      { params }
    ),

  getLogsForEntity: (entityType: string, entityId: string) =>
    apiClient.get<AuditLogResponse[]>(`/audit/logs/${entityType}/${entityId}`),

  getLogsForUser: (userId: string, page = 0, size = 20) =>
    apiClient.get<{ content: AuditLogResponse[]; totalElements: number }>(
      `/audit/logs/user/${userId}`,
      { params: { page, size } }
    ),

  createLog: (log: {
    entityType: string;
    entityId: string;
    action: string;
    changes?: Record<string, any>;
  }) => apiClient.post('/audit/logs', log),
};

// ============================================
// SEARCH API
// ============================================
export interface SearchParams {
  query?: string;
  entityType?: string;
  projectId?: string;
  page?: number;
  size?: number;
}

export interface SearchResult {
  id: string;
  entityType: string;
  entityId: string;
  title: string;
  content?: string;
  highlights?: string[];
  score: number;
}

export interface JQLSearchParams {
  jql: string;
  page?: number;
  size?: number;
}

export const searchApi = {
  search: (params: SearchParams) =>
    apiClient.get<{ results: SearchResult[]; totalCount: number }>('/search', { params }),

  indexEntity: (entity: { entityType: string; entityId: string; title: string; content?: string }) =>
    apiClient.post('/search/index', entity),

  removeFromIndex: (entityType: string, entityId: string) =>
    apiClient.delete(`/search/index/${entityType}/${entityId}`),

  jqlSearch: (params: JQLSearchParams) =>
    apiClient.post<{ results: SearchResult[]; totalCount: number }>('/jql/search', params),
};

// ============================================
// MIGRATION API
// ============================================
export interface MigrationJobResponse {
  id: string;
  jobType: string;
  jobStatus: string;
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
  resultMetadata?: Record<string, unknown>;
}

export interface JiraDcValidateResponse {
  valid: boolean;
  format?: string;
  totalEntities?: number;
  entitiesByType?: Record<string, number>;
  riskScore?: number;
  blockerCount?: number;
  warningCount?: number;
  message?: string;
  attachmentsRootResolved?: boolean;
  relationshipEdges?: Array<{ from: string; to: string; type: string }>;
  errors?: Array<{ field: string; code: string; message: string }>;
  warnings?: Array<{ field: string; code: string; message: string }>;
  conflicts?: Array<{
    severity: string;
    code: string;
    field: string;
    entityKey: string;
    message: string;
    resolution: string;
  }>;
  unknownCustomFields?: Array<{ fieldId: string; message: string }>;
  acSignoffPreview?: Record<string, unknown>;
  backupZipDetected?: boolean;
}

export interface ImportResultResponse {
  jobId: string;
  jobStatus: string;
  totalEntities: number;
  processedEntities: number;
  failedEntities: number;
  successCount: number;
  warningCount: number;
  errors: Array<{
    entityType: string;
    entityKey: string;
    row?: number;
    field?: string;
    errorCode: string;
    errorMessage: string;
  }>;
  warnings: Array<{
    entityType: string;
    entityKey: string;
    row?: number;
    field?: string;
    warningMessage: string;
  }>;
  resultMetadata?: Record<string, unknown>;
}

export interface CsvTemplateResponse {
  id: string;
  templateName: string;
  entityType: string;
  version: string;
  columns: Array<{
    columnName: string;
    displayName: string;
    dataType: string;
    required: boolean;
  }>;
}

const migrationUserHeaders = (opts?: { targetProjectId?: string }) => {
  const headers: Record<string, string> = {
    'X-User-Id': localStorage.getItem('userId') || '00000000-0000-0000-0000-000000000001',
    'X-Migration-Role': localStorage.getItem('migrationRole') || 'MIGRATION_OPERATOR',
  };
  const token = localStorage.getItem('accessToken');
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (opts?.targetProjectId) {
    headers['X-Target-Project-Id'] = opts.targetProjectId;
  }
  return headers;
};

export const migrationApi = {
  // CSV Import
  startCsvImport: (
    file: File,
    targetProjectId?: string,
    fieldMappings?: unknown[],
    options?: Record<string, unknown>
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    if (targetProjectId) formData.append('targetProjectId', targetProjectId);
    if (fieldMappings?.length) {
      formData.append('fieldMappings', JSON.stringify(fieldMappings));
    }
    if (options && Object.keys(options).length > 0) {
      formData.append('options', JSON.stringify(options));
    }
    return apiClient.post<MigrationJobResponse>('/api/migration/import/csv', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        ...migrationUserHeaders({ targetProjectId }),
      },
    });
  },

  validateJiraDcImport: (params: {
    file: File;
    attachmentBundle?: File | null;
    backupZip?: boolean;
    options?: Record<string, unknown>;
  }) => {
    const formData = new FormData();
    formData.append('file', params.file);
    if (params.attachmentBundle) {
      formData.append('attachmentBundle', params.attachmentBundle);
    }
    if (params.backupZip) {
      formData.append('backupZip', 'true');
    }
    if (params.options && Object.keys(params.options).length > 0) {
      formData.append('options', JSON.stringify(params.options));
    }
    return apiClient.post<JiraDcValidateResponse>('/api/migration/import/jira-dc/validate', formData, {
      headers: { 'Content-Type': 'multipart/form-data', ...migrationUserHeaders() },
    });
  },

  startJiraDcImport: (params: {
    file: File;
    targetProjectId?: string;
    attachmentBundle?: File | null;
    backupZip?: boolean;
    options?: Record<string, unknown>;
  }) => {
    const formData = new FormData();
    formData.append('file', params.file);
    if (params.targetProjectId) {
      formData.append('targetProjectId', params.targetProjectId);
    }
    if (params.attachmentBundle) {
      formData.append('attachmentBundle', params.attachmentBundle);
    }
    if (params.backupZip) {
      formData.append('backupZip', 'true');
    }
    if (params.options && Object.keys(params.options).length > 0) {
      formData.append('options', JSON.stringify(params.options));
    }
    return apiClient.post<MigrationJobResponse>('/api/migration/import/jira-dc', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        ...migrationUserHeaders({ targetProjectId: params.targetProjectId }),
      },
    });
  },

  validateWorkflowXml: (workflowFile: File, schemeFile?: File) => {
    const formData = new FormData();
    formData.append('file', workflowFile);
    if (schemeFile) formData.append('schemeFile', schemeFile);
    return apiClient.post<Record<string, unknown>>('/api/migration/import/workflow-xml/validate', formData, {
      headers: { 'Content-Type': 'multipart/form-data', ...migrationUserHeaders() },
    });
  },

  simulateWorkflowXml: (workflowFile: File, startStepId = '1', transitionPath?: string) => {
    const formData = new FormData();
    formData.append('file', workflowFile);
    const params = new URLSearchParams({ startStepId });
    if (transitionPath) params.set('path', transitionPath);
    return apiClient.post<Record<string, unknown>>(
      `/api/migration/import/workflow-xml/simulate?${params}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data', ...migrationUserHeaders() } }
    );
  },

  importWorkflowXml: (
    workflowFile: File,
    schemeFile?: File,
    stubDownstream = true,
    makeDefault = false,
    projectId?: string
  ) => {
    const formData = new FormData();
    formData.append('file', workflowFile);
    if (schemeFile) formData.append('schemeFile', schemeFile);
    const params = new URLSearchParams({
      stubDownstream: String(stubDownstream),
      makeDefault: String(makeDefault),
    });
    if (projectId) params.set('projectId', projectId);
    return apiClient.post<MigrationJobResponse>(
      `/api/migration/import/workflow-xml?${params}`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data', ...migrationUserHeaders() } }
    );
  },

  downloadWorkflowValidationReport: (workflowFile: File, schemeFile?: File) => {
    const formData = new FormData();
    formData.append('file', workflowFile);
    if (schemeFile) formData.append('schemeFile', schemeFile);
    return apiClient.post<string>('/api/migration/import/workflow-xml/validation-report', formData, {
      headers: { 'Content-Type': 'multipart/form-data', ...migrationUserHeaders() },
      responseType: 'text',
    });
  },

  rollbackWorkflowXmlImport: (importId: string) =>
    apiClient.post<Record<string, unknown>>(
      `/api/migration/import/workflow-xml/rollback/${importId}`,
      null,
      { headers: migrationUserHeaders() }
    ),

  getServicesHealth: () =>
    apiClient.get<Record<string, unknown>>('/api/migration/health/services', {
      headers: migrationUserHeaders(),
    }),

  getClusterHealth: () =>
    apiClient.get<Record<string, unknown>>('/api/migration/health/cluster', {
      headers: migrationUserHeaders(),
    }),

  getJobAttachmentResults: (jobId: string) =>
    apiClient.get<Array<Record<string, unknown>>>(`/api/migration/jobs/${jobId}/attachment-results`, {
      headers: migrationUserHeaders(),
    }),

  getJobVerification: (jobId: string) =>
    apiClient.get<Record<string, unknown>>(`/api/migration/jobs/${jobId}/verification`, {
      headers: migrationUserHeaders(),
    }),

  triggerJobReindex: (jobId: string, entityTypes?: string[]) =>
    apiClient.post<Record<string, unknown>>(
      `/api/migration/jobs/${jobId}/reindex`,
      null,
      {
        params: entityTypes?.length ? { entityTypes } : undefined,
        headers: migrationUserHeaders(),
      }
    ),

  getJobReindexStatus: (jobId: string) =>
    apiClient.get<Record<string, unknown>>(`/api/migration/jobs/${jobId}/reindex`, {
      headers: migrationUserHeaders(),
    }),

  getObservability: () =>
    apiClient.get<Record<string, unknown>>('/api/migration/health/observability', {
      headers: migrationUserHeaders(),
    }),

  pauseJob: (jobId: string) =>
    apiClient.post<Record<string, unknown>>(`/api/migration/jobs/${jobId}/pause`, null, {
      headers: migrationUserHeaders(),
    }),

  resumePausedJob: (jobId: string) =>
    apiClient.post<Record<string, unknown>>(`/api/migration/jobs/${jobId}/resume-control`, null, {
      headers: migrationUserHeaders(),
    }),

  scanUpload: (uploadId: string) =>
    apiClient.post<Record<string, string>>(`/api/migration/uploads/${uploadId}/virus-scan`, null, {
      headers: migrationUserHeaders(),
    }),

  // Project Import
  startProjectImport: (sourceProjectId: string, targetProjectId: string) =>
    apiClient.post<MigrationJobResponse>('/api/migration/import/project', null, {
      params: { sourceProjectId, targetProjectId },
    }),

  // Project Export
  startProjectExport: (projectId: string, format = 'xml') =>
    apiClient.post<MigrationJobResponse>('/api/migration/export/project', null, {
      params: { projectId, format },
    }),

  listJobs: (params?: {
    status?: string;
    type?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) =>
    apiClient.get<{
      content: MigrationJobResponse[];
      totalElements: number;
      totalPages: number;
      number: number;
    }>('/api/migration/jobs', { params, headers: migrationUserHeaders() }),

  retryJob: (jobId: string) =>
    apiClient.post<{ jobId: string; retried: number; succeeded: number }>(
      `/api/migration/jobs/${jobId}/retry`,
      null,
      { headers: migrationUserHeaders() }
    ),

  downloadJobReport: (jobId: string) =>
    apiClient.get(`/api/migration/jobs/${jobId}/report`, {
      responseType: 'blob',
      headers: migrationUserHeaders(),
    }),

  downloadJobLogs: (jobId: string) =>
    apiClient.get(`/api/migration/jobs/${jobId}/logs/download`, {
      responseType: 'blob',
      headers: migrationUserHeaders(),
    }),

  getRollbackInfo: (jobId: string) =>
    apiClient.get<{
      jobId: string;
      canRollback: boolean;
      canRollbackReason: string;
      entitiesToRollback: number;
      backupSnapshotAvailable: boolean;
    }>(`/api/migration/jobs/${jobId}/rollback-info`, { headers: migrationUserHeaders() }),

  rollbackJob: (jobId: string) =>
    apiClient.post<{
      jobId: string;
      success: boolean;
      rolledBackCount: number;
      failedCount: number;
    }>(`/api/migration/jobs/${jobId}/rollback`, null, { headers: migrationUserHeaders() }),

  downloadValidationReport: (jobId: string) =>
    apiClient.get(`/api/migration/jobs/${jobId}/validation-report`, {
      responseType: 'blob',
      headers: migrationUserHeaders(),
    }),

  getJobAuditTrail: (jobId: string) =>
    apiClient.get<Array<Record<string, unknown>>>(`/api/migration/jobs/${jobId}/audit-trail`, {
      headers: migrationUserHeaders(),
    }),

  getJobIssueResults: (jobId: string) =>
    apiClient.get<Array<Record<string, unknown>>>(`/api/migration/jobs/${jobId}/issue-results`, {
      headers: migrationUserHeaders(),
    }),

  getJobStagingSummary: (jobId: string) =>
    apiClient.get<Record<string, unknown>>(`/api/migration/jobs/${jobId}/staging-summary`, {
      headers: migrationUserHeaders(),
    }),

  getJobLogs: (jobId: string) =>
    apiClient.get<Array<{ timestamp?: string; level?: string; message?: string }>>(
      `/api/migration/jobs/${jobId}/logs`,
      { headers: migrationUserHeaders() }
    ),

  getJobDlq: (jobId: string) =>
    apiClient.get<Array<Record<string, unknown>>>(`/api/migration/jobs/${jobId}/dlq`, {
      headers: migrationUserHeaders(),
    }),

  retryJobDlqEntry: (jobId: string, dlqId: string) =>
    apiClient.post<Record<string, unknown>>(`/api/migration/jobs/${jobId}/dlq/${dlqId}/retry`, null, {
      headers: migrationUserHeaders(),
    }),

  // Job Status
  getJobStatus: (jobId: string) =>
    apiClient.get<MigrationJobResponse>(`/api/migration/jobs/${jobId}`, {
      headers: migrationUserHeaders(),
    }),

  kickStalledJob: (jobId: string) =>
    apiClient.post<Record<string, unknown>>(
      `/api/migration/jobs/${jobId}/kick`,
      null,
      { headers: migrationUserHeaders() }
    ),

  getJobProgress: (jobId: string) =>
    apiClient.get<{
      jobId: string;
      jobStatus: string;
      progressPercentage: number;
      totalEntities: number;
      processedEntities: number;
      failedEntities: number;
      entityProgress: Array<{ entityType: string; total: number; completed: number; failed: number }>;
    }>(`/api/migration/jobs/${jobId}/progress`, { headers: migrationUserHeaders() }),

  getImportResult: (jobId: string) =>
    apiClient.get<ImportResultResponse>(`/api/migration/jobs/${jobId}/result`, {
      headers: migrationUserHeaders(),
    }),

  getDcSlaProof: (jobId: string) =>
    apiClient.get<Record<string, unknown>>(`/api/migration/jobs/${jobId}/dc-sla-proof`, {
      headers: migrationUserHeaders(),
    }),

  getDcAcSignoff: (jobId: string) =>
    apiClient.get<Record<string, unknown>>(`/api/migration/jobs/${jobId}/dc-ac-signoff`, {
      headers: migrationUserHeaders(),
    }),

  cancelJob: (jobId: string) =>
    apiClient.post(`/api/migration/jobs/${jobId}/cancel`, null, { headers: migrationUserHeaders() }),

  // Templates
  getTemplates: (entityType?: string) =>
    apiClient.get<CsvTemplateResponse[]>('/api/migration/templates', { params: { entityType } }),

  downloadTemplate: (templateId: string) =>
    apiClient.get(`/api/migration/templates/${templateId}/download`, { responseType: 'blob' }),

  // Field Mappings (saved templates)
  getMappings: (mappingType?: string) =>
    apiClient.get<Array<Record<string, unknown>>>('/api/migration/mappings', {
      params: mappingType ? { mappingType } : undefined,
      headers: migrationUserHeaders(),
    }),
  getMapping: (mappingId: string) =>
    apiClient.get<Record<string, unknown>>(`/api/migration/mappings/${mappingId}`, {
      headers: migrationUserHeaders(),
    }),
  createMapping: (mapping: Record<string, unknown>) =>
    apiClient.post<Record<string, unknown>>('/api/migration/mappings', mapping, {
      headers: migrationUserHeaders(),
    }),
  deleteMapping: (mappingId: string) =>
    apiClient.delete(`/api/migration/mappings/${mappingId}`, { headers: migrationUserHeaders() }),

  getConfigImportSummary: (jobId: string) =>
    apiClient.get<Record<string, unknown>>(`/api/migration/jobs/${jobId}/config-import-summary`, {
      headers: migrationUserHeaders(),
    }),

  // Global DLQ
  listGlobalDlq: (page = 0, size = 20) =>
    apiClient.get<{ content: Array<Record<string, unknown>>; totalElements: number }>('/api/migration/dlq', {
      params: { page, size },
      headers: migrationUserHeaders(),
    }),
  getGlobalDlqStats: () =>
    apiClient.get<Record<string, unknown>>('/api/migration/dlq/statistics', {
      headers: migrationUserHeaders(),
    }),
  retryGlobalDlq: (id: string) =>
    apiClient.post<Record<string, unknown>>(`/api/migration/dlq/retry/${id}`, null, {
      headers: migrationUserHeaders(),
    }),
  retryAllGlobalDlq: () =>
    apiClient.post<Record<string, unknown>>('/api/migration/dlq/retry/all', null, {
      headers: migrationUserHeaders(),
    }),
  purgeGlobalDlq: () =>
    apiClient.delete<Record<string, unknown>>('/api/migration/dlq/purge', { headers: migrationUserHeaders() }),

  // Validation
  validateCsv: (file: File, entityType: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('entityType', entityType);
    return apiClient.post('/api/migration/validate/csv', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  validateRow: (row: Record<string, string>, entityType: string) =>
    apiClient.post('/api/migration/validate/row', row, { params: { entityType } }),
};

// ============================================
// MIGRATION WIZARD API (persisted sessions)
// ============================================
export interface WizardSessionResponse {
  sessionId: string;
  step: string;
  importType: string;
  status: string;
  targetProjectId?: string;
  migrationJobId?: string;
  fileName?: string;
  fileSize?: number;
  detectedHeaders?: string[];
  detectedEntityType?: string;
  attachmentColumn?: string;
  parentColumn?: string;
  epicColumn?: string;
  totalRows?: number;
  validationResult?: Record<string, unknown>;
  fieldMappings?: Record<string, unknown>[];
  userMappings?: Array<Record<string, unknown>>;
  previewRows?: string[][];
  sessionData?: Record<string, unknown>;
}

export interface WizardUserMappingRow {
  id?: string;
  sourceIdentifier: string;
  sourceType?: string;
  targetUserId?: string;
  targetUsername?: string;
  targetEmail?: string;
  mappingType?: string;
  confidenceScore?: number;
}

export interface WizardUploadResponse {
  sessionId: string;
  uploadId?: string;
  virusScanStatus?: string;
  fileName: string;
  detectedHeaders?: string[];
  detectedEntityType?: string;
  totalRows?: number;
  previewRows?: string[][];
  success: boolean;
  errorMessage?: string;
}

export interface WizardDiscoveredFieldInfo {
  sourceKey: string;
  normalizedKey: string;
  category: string;
  suggestedType: string;
  suggestedRegion: string;
  isKnown: boolean;
  requiresProvisioning: boolean;
}

export interface WizardFieldDiscoveryResponse {
  discoveredFields: WizardDiscoveredFieldInfo[];
  standardFieldCount: number;
  agileFieldCount: number;
  pluginFieldCount: number;
  unknownFieldCount: number;
  missingFieldKeys?: string[];
}

export interface WizardFieldProvisioningResponse {
  provisionedFields?: Array<{ fieldKey: string; displayName?: string }>;
  existingFields?: Array<{ fieldKey: string; displayName?: string }>;
  failedFields?: string[];
  fieldKeyMapping: Record<string, string>;
  totalProvisioned: number;
  totalExisting: number;
  totalFailed: number;
}

export const migrationSettingsApi = {
  getSettings: () =>
    apiClient.get<Record<string, unknown>>('/api/migration/settings', {
      headers: migrationUserHeaders(),
    }),
};

export const migrationWizardApi = {
  createSession: (body: { importType: string; targetProjectId?: string; options?: Record<string, unknown> }) =>
    apiClient.post<WizardSessionResponse>('/api/migration/wizard/sessions', body, {
      headers: migrationUserHeaders(),
    }),

  getSession: (sessionId: string) =>
    apiClient.get<WizardSessionResponse>(`/api/migration/wizard/sessions/${sessionId}`, {
      headers: migrationUserHeaders(),
    }),

  updateSession: (
    sessionId: string,
    body: {
      step?: string;
      targetProjectId?: string;
      importOptions?: Record<string, unknown>;
      userMappings?: Array<Record<string, unknown>>;
    }
  ) =>
    apiClient.patch<WizardSessionResponse>(`/api/migration/wizard/sessions/${sessionId}`, body, {
      headers: migrationUserHeaders(),
    }),

  getUserMappings: (sessionId: string) =>
    apiClient.get<WizardUserMappingRow[]>(`/api/migration/wizard/sessions/${sessionId}/user-mappings`, {
      headers: migrationUserHeaders(),
    }),

  uploadFile: (sessionId: string, file: File, importType?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (importType) formData.append('importType', importType);
    return apiClient.post<WizardUploadResponse>(`/api/migration/wizard/sessions/${sessionId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data', ...migrationUserHeaders() },
    });
  },

  uploadFileWithProgress: (
    sessionId: string,
    file: File,
    importType?: string,
    onProgress?: (percent: number) => void,
    signal?: AbortSignal
  ) => {
    const formData = new FormData();
    formData.append('file', file);
    if (importType) formData.append('importType', importType);
    return apiClient.post<WizardUploadResponse>(`/api/migration/wizard/sessions/${sessionId}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data', ...migrationUserHeaders() },
      signal,
      onUploadProgress: (event) => {
        if (event.total && onProgress) {
          onProgress(Math.round((event.loaded * 100) / event.total));
        }
      },
    });
  },

  getPreview: (sessionId: string, page = 0, size = 10) =>
    apiClient.get<WizardSessionResponse>(`/api/migration/wizard/sessions/${sessionId}/preview`, {
      params: { page, size },
      headers: migrationUserHeaders(),
    }),

  downloadValidationReport: (sessionId: string) =>
    apiClient.get(`/api/migration/wizard/sessions/${sessionId}/validation-report`, {
      responseType: 'blob',
      headers: migrationUserHeaders(),
    }),

  validateSession: (sessionId: string, entityType?: string) =>
    apiClient.post(
      `/api/migration/wizard/sessions/${sessionId}/validate`,
      null,
      { params: { entityType }, headers: migrationUserHeaders() }
    ),

  saveFieldMappings: (sessionId: string, mappings: Record<string, unknown>[]) =>
    apiClient.patch<WizardSessionResponse>(
      `/api/migration/wizard/sessions/${sessionId}/field-mappings`,
      mappings,
      { headers: migrationUserHeaders() }
    ),

  discoverSessionFields: (sessionId: string) =>
    apiClient.post<WizardFieldDiscoveryResponse>(
      `/api/migration/wizard/sessions/${sessionId}/fields/discover`,
      null,
      { headers: migrationUserHeaders() }
    ),

  provisionMissingSessionFields: (sessionId: string) =>
    apiClient.post<WizardFieldProvisioningResponse>(
      `/api/migration/wizard/sessions/${sessionId}/fields/provision-missing`,
      null,
      { headers: migrationUserHeaders() }
    ),

  executeImport: (sessionId: string, body?: { targetProjectId?: string; options?: Record<string, unknown> }) =>
    apiClient.post<MigrationJobResponse>(
      `/api/migration/wizard/sessions/${sessionId}/execute`,
      body ?? {},
      {
        headers: migrationUserHeaders({
          targetProjectId: body?.targetProjectId,
        }),
      }
    ),
};

// ============================================
// SPRINT API
// ============================================
export interface SprintResponse {
  id: string;
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  status: 'PLANNING' | 'ACTIVE' | 'COMPLETED';
  projectId: string;
  createdBy?: string;
}

export interface BoardResponse {
  id: string;
  name: string;
  projectId: string;
  boardType: 'SCRUM' | 'KANBAN';
  columns?: BoardColumnResponse[];
}

export interface BoardColumnResponse {
  id: string;
  name: string;
  statusId?: string;
  statusCategory?: string;
  orderIndex: number;
  isDone: boolean;
  maxIssues?: number;
}

export interface VelocityResponse {
  velocityPoints: Array<{
    sprintId: string;
    sprintName: string;
    completedIssues: number;
    plannedIssues: number;
  }>;
  averageVelocity: number;
}

export const sprintApi = {
  // Sprints
  getSprints: (projectId?: string) =>
    apiClient.get<SprintResponse[]>('/sprints', { params: { projectId } }),

  getSprint: (sprintId: string) => apiClient.get<SprintResponse>(`/sprints/${sprintId}`),

  getActiveSprint: (projectId: string) => apiClient.get<SprintResponse>('/sprints/active', {
    params: { projectId },
  }),

  createSprint: (sprint: {
    name: string;
    goal?: string;
    startDate?: string;
    endDate?: string;
    projectId: string;
  }) => apiClient.post<SprintResponse>('/sprints', sprint),

  updateSprint: (sprintId: string, sprint: Partial<SprintResponse>) =>
    apiClient.put<SprintResponse>(`/sprints/${sprintId}`, sprint),

  deleteSprint: (sprintId: string) => apiClient.delete(`/sprints/${sprintId}`),

  startSprint: (sprintId: string) => apiClient.post<SprintResponse>(`/sprints/${sprintId}/start`),

  completeSprint: (sprintId: string) => apiClient.post<SprintResponse>(`/sprints/${sprintId}/complete`),

  addIssueToSprint: (sprintId: string, issueId: string) =>
    apiClient.post(`/sprints/${sprintId}/issues`, { issueId }),

  removeIssueFromSprint: (sprintId: string, issueId: string) =>
    apiClient.delete(`/sprints/${sprintId}/issues/${issueId}`),

  // Boards
  getBoards: (projectId: string) => apiClient.get<BoardResponse[]>('/boards', { params: { projectId } }),

  getBoard: (boardId: string) => apiClient.get<BoardResponse>(`/boards/${boardId}`),

  getBoardData: (boardId: string) =>
    apiClient.get<{
      board: BoardResponse;
      columns: BoardColumnResponse[];
      sprints: SprintResponse[];
    }>(`/boards/${boardId}/data`),

  createBoard: (board: {
    name: string;
    projectId: string;
    boardType: 'SCRUM' | 'KANBAN';
  }) => apiClient.post<BoardResponse>('/boards', board),

  updateBoard: (boardId: string, board: Partial<BoardResponse>) =>
    apiClient.put<BoardResponse>(`/boards/${boardId}`, board),

  deleteBoard: (boardId: string) => apiClient.delete(`/boards/${boardId}`),

  // Board Columns
  addColumn: (boardId: string, column: { name: string; statusId?: string; orderIndex: number }) =>
    apiClient.post<BoardColumnResponse>(`/boards/${boardId}/columns`, column),

  updateColumn: (boardId: string, columnId: string, column: Partial<BoardColumnResponse>) =>
    apiClient.put<BoardColumnResponse>(`/boards/${boardId}/columns/${columnId}`, column),

  deleteColumn: (boardId: string, columnId: string) =>
    apiClient.delete(`/boards/${boardId}/columns/${columnId}`),

  // Velocity
  getVelocity: (boardId: string) => apiClient.get<VelocityResponse>(`/boards/${boardId}/velocity`),
};

// ============================================
// ATTACHMENT API
// ============================================
export interface AttachmentResponse {
  id: string;
  issueId: string;
  filename: string;
  mimeType: string;
  size: number;
  uploaderId: string;
  uploaderName: string;
  storagePath: string;
  createdAt: string;
}

export const attachmentApi = {
  upload: (issueId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<AttachmentResponse>(`/attachments/issue/${issueId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getForIssue: (issueId: string) => apiClient.get<AttachmentResponse[]>(`/attachments/issue/${issueId}`),

  getMetadata: (attachmentId: string) => apiClient.get<AttachmentResponse>(`/attachments/${attachmentId}`),

  download: (attachmentId: string) =>
    apiClient.get(`/attachments/${attachmentId}/download`, { responseType: 'blob' }),

  delete: (attachmentId: string) => apiClient.delete(`/attachments/${attachmentId}`),

  deleteAllForIssue: (issueId: string) => apiClient.delete(`/attachments/issue/${issueId}`),
};

// ============================================
// USER MANAGEMENT API
// ============================================
export interface UserProfileResponse {
  id: string;
  userId: string;
  displayName: string;
  avatarUrl?: string;
  bio?: string;
  timezone?: string;
}

export interface OrganizationResponse {
  id: string;
  name: string;
  description?: string;
  memberCount: number;
  createdAt: string;
}

export interface TeamResponse {
  id: string;
  name: string;
  organizationId: string;
  memberCount: number;
  createdAt: string;
}

export const userManagementApi = {
  // Profiles
  getProfile: (userId: string) => apiClient.get<UserProfileResponse>(`/users/profiles/${userId}`),

  updateProfile: (userId: string, profile: Partial<UserProfileResponse>) =>
    apiClient.put<UserProfileResponse>(`/users/profiles/${userId}`, profile),

  // Organizations
  getOrganizations: () => apiClient.get<OrganizationResponse[]>('/users/organizations'),

  createOrganization: (org: { name: string; description?: string }) =>
    apiClient.post<OrganizationResponse>('/users/organizations', org),

  getOrganization: (orgId: string) => apiClient.get<OrganizationResponse>(`/users/organizations/${orgId}`),

  getOrganizationMembers: (orgId: string) =>
    apiClient.get<Array<{ userId: string; displayName: string; role: string }>>(
      `/users/organizations/${orgId}/members`
    ),

  addOrganizationMember: (orgId: string, userId: string, role: string) =>
    apiClient.post(`/users/organizations/${orgId}/members`, { userId, role }),

  removeOrganizationMember: (orgId: string, userId: string) =>
    apiClient.delete(`/users/organizations/${orgId}/members/${userId}`),

  // Teams
  getTeams: (orgId?: string) =>
    apiClient.get<TeamResponse[]>('/users/teams', { params: { organizationId: orgId } }),

  createTeam: (team: { name: string; organizationId: string }) =>
    apiClient.post<TeamResponse>('/users/teams', team),

  getTeam: (teamId: string) => apiClient.get<TeamResponse>(`/users/teams/${teamId}`),

  updateTeam: (teamId: string, team: Partial<TeamResponse>) =>
    apiClient.put<TeamResponse>(`/users/teams/${teamId}`, team),

  deleteTeam: (teamId: string) => apiClient.delete(`/users/teams/${teamId}`),
};