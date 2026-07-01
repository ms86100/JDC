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
      '/api/audit/logs',
      { params }
    ),

  getLogsForEntity: (entityType: string, entityId: string) =>
    apiClient.get<AuditLogResponse[]>(`/api/audit/logs/${entityType}/${entityId}`),

  getLogsForUser: (userId: string, page = 0, size = 20) =>
    apiClient.get<{ content: AuditLogResponse[]; totalElements: number }>(
      `/api/audit/logs/user/${userId}`,
      { params: { page, size } }
    ),

  createLog: (log: {
    entityType: string;
    entityId: string;
    action: string;
    changes?: Record<string, any>;
  }) => apiClient.post('/api/audit/logs', log),
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
    apiClient.post<{ results: SearchResult[]; totalCount: number }>('/api/jql/search', params),
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

export const migrationApi = {
  // CSV Import
  startCsvImport: (file: File, targetProjectId?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (targetProjectId) formData.append('targetProjectId', targetProjectId);
    return apiClient.post<MigrationJobResponse>('/api/migration/import/csv', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  // Jira DC Import
  startJiraDcImport: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<MigrationJobResponse>('/api/migration/import/jira-dc', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

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

  // Job Status
  getJobStatus: (jobId: string) => apiClient.get<MigrationJobResponse>(`/api/migration/jobs/${jobId}`),

  getJobProgress: (jobId: string) =>
    apiClient.get<{
      jobId: string;
      jobStatus: string;
      progressPercentage: number;
      totalEntities: number;
      processedEntities: number;
      failedEntities: number;
      entityProgress: Array<{ entityType: string; total: number; completed: number; failed: number }>;
    }>(`/api/migration/jobs/${jobId}/progress`),

  getImportResult: (jobId: string) => apiClient.get<ImportResultResponse>(`/api/migration/jobs/${jobId}/result`),

  cancelJob: (jobId: string) => apiClient.post(`/api/migration/jobs/${jobId}/cancel`),

  // Templates
  getTemplates: (entityType?: string) =>
    apiClient.get<CsvTemplateResponse[]>('/api/migration/templates', { params: { entityType } }),

  downloadTemplate: (templateId: string) =>
    apiClient.get(`/api/migration/templates/${templateId}/download`, { responseType: 'blob' }),

  // Field Mappings
  getMappings: () => apiClient.get('/api/migration/mappings'),
  createMapping: (mapping: any) => apiClient.post('/api/migration/mappings', mapping),

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
    apiClient.get<SprintResponse[]>('/api/sprints', { params: { projectId } }),

  getSprint: (sprintId: string) => apiClient.get<SprintResponse>(`/api/sprints/${sprintId}`),

  getActiveSprint: (projectId: string) => apiClient.get<SprintResponse>('/api/sprints/active', {
    params: { projectId },
  }),

  createSprint: (sprint: {
    name: string;
    goal?: string;
    startDate?: string;
    endDate?: string;
    projectId: string;
  }) => apiClient.post<SprintResponse>('/api/sprints', sprint),

  updateSprint: (sprintId: string, sprint: Partial<SprintResponse>) =>
    apiClient.put<SprintResponse>(`/api/sprints/${sprintId}`, sprint),

  deleteSprint: (sprintId: string) => apiClient.delete(`/api/sprints/${sprintId}`),

  startSprint: (sprintId: string) => apiClient.post<SprintResponse>(`/api/sprints/${sprintId}/start`),

  completeSprint: (sprintId: string) => apiClient.post<SprintResponse>(`/api/sprints/${sprintId}/complete`),

  addIssueToSprint: (sprintId: string, issueId: string) =>
    apiClient.post(`/api/sprints/${sprintId}/issues`, { issueId }),

  removeIssueFromSprint: (sprintId: string, issueId: string) =>
    apiClient.delete(`/api/sprints/${sprintId}/issues/${issueId}`),

  // Boards
  getBoards: (projectId: string) => apiClient.get<BoardResponse[]>('/api/boards', { params: { projectId } }),

  getBoard: (boardId: string) => apiClient.get<BoardResponse>(`/api/boards/${boardId}`),

  getBoardData: (boardId: string) =>
    apiClient.get<{
      board: BoardResponse;
      columns: BoardColumnResponse[];
      sprints: SprintResponse[];
    }>(`/api/boards/${boardId}/data`),

  createBoard: (board: {
    name: string;
    projectId: string;
    boardType: 'SCRUM' | 'KANBAN';
  }) => apiClient.post<BoardResponse>('/api/boards', board),

  updateBoard: (boardId: string, board: Partial<BoardResponse>) =>
    apiClient.put<BoardResponse>(`/api/boards/${boardId}`, board),

  deleteBoard: (boardId: string) => apiClient.delete(`/api/boards/${boardId}`),

  // Board Columns
  addColumn: (boardId: string, column: { name: string; statusId?: string; orderIndex: number }) =>
    apiClient.post<BoardColumnResponse>(`/api/boards/${boardId}/columns`, column),

  updateColumn: (boardId: string, columnId: string, column: Partial<BoardColumnResponse>) =>
    apiClient.put<BoardColumnResponse>(`/api/boards/${boardId}/columns/${columnId}`, column),

  deleteColumn: (boardId: string, columnId: string) =>
    apiClient.delete(`/api/boards/${boardId}/columns/${columnId}`),

  // Velocity
  getVelocity: (boardId: string) => apiClient.get<VelocityResponse>(`/api/boards/${boardId}/velocity`),
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
    return apiClient.post<AttachmentResponse>(`/api/attachments/issue/${issueId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getForIssue: (issueId: string) => apiClient.get<AttachmentResponse[]>(`/api/attachments/issue/${issueId}`),

  getMetadata: (attachmentId: string) => apiClient.get<AttachmentResponse>(`/api/attachments/${attachmentId}`),

  download: (attachmentId: string) =>
    apiClient.get(`/api/attachments/${attachmentId}/download`, { responseType: 'blob' }),

  delete: (attachmentId: string) => apiClient.delete(`/api/attachments/${attachmentId}`),

  deleteAllForIssue: (issueId: string) => apiClient.delete(`/api/attachments/issue/${issueId}`),
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
  getProfile: (userId: string) => apiClient.get<UserProfileResponse>(`/api/users/profiles/${userId}`),

  updateProfile: (userId: string, profile: Partial<UserProfileResponse>) =>
    apiClient.put<UserProfileResponse>(`/api/users/profiles/${userId}`, profile),

  // Organizations
  getOrganizations: () => apiClient.get<OrganizationResponse[]>('/api/users/organizations'),

  createOrganization: (org: { name: string; description?: string }) =>
    apiClient.post<OrganizationResponse>('/api/users/organizations', org),

  getOrganization: (orgId: string) => apiClient.get<OrganizationResponse>(`/api/users/organizations/${orgId}`),

  getOrganizationMembers: (orgId: string) =>
    apiClient.get<Array<{ userId: string; displayName: string; role: string }>>(
      `/api/users/organizations/${orgId}/members`
    ),

  addOrganizationMember: (orgId: string, userId: string, role: string) =>
    apiClient.post(`/api/users/organizations/${orgId}/members`, { userId, role }),

  removeOrganizationMember: (orgId: string, userId: string) =>
    apiClient.delete(`/api/users/organizations/${orgId}/members/${userId}`),

  // Teams
  getTeams: (orgId?: string) =>
    apiClient.get<TeamResponse[]>('/api/users/teams', { params: { organizationId: orgId } }),

  createTeam: (team: { name: string; organizationId: string }) =>
    apiClient.post<TeamResponse>('/api/users/teams', team),

  getTeam: (teamId: string) => apiClient.get<TeamResponse>(`/api/users/teams/${teamId}`),

  updateTeam: (teamId: string, team: Partial<TeamResponse>) =>
    apiClient.put<TeamResponse>(`/api/users/teams/${teamId}`, team),

  deleteTeam: (teamId: string) => apiClient.delete(`/api/users/teams/${teamId}`),
};