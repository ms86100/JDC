import apiClient from './axiosClient';

export interface VersionResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  released: boolean;
  archived: boolean;
  sequence: number;
  startDate?: string;
  releaseDate?: string;
  actualReleaseDate?: string;
  semanticVersion?: string;
  buildNumber?: string;
  branchName?: string;
  releaseTrain?: string;
  deploymentStatus?: string;
  releaseStatus?: string;
  releaseNotesUrl?: string;
  releaseNotesGenerated?: boolean;
  color?: string;
  createdAt?: string;
  updatedAt?: string;
  overdue?: boolean;
  issueCount?: number;
  unresolvedIssueCount?: number;
  completedIssueCount?: number;
  progressPercentage?: number;
}

export interface CreateVersionRequest {
  projectId: string;
  name: string;
  description?: string;
  startDate?: string;
  releaseDate?: string;
  semanticVersion?: string;
  buildNumber?: string;
  branchName?: string;
  releaseTrain?: string;
  color?: string;
}

export interface UpdateVersionRequest {
  name?: string;
  description?: string;
  startDate?: string;
  releaseDate?: string;
  semanticVersion?: string;
  buildNumber?: string;
  branchName?: string;
  releaseTrain?: string;
  color?: string;
  sequence?: number;
}

export interface ReleaseVersionRequest {
  releasedBy?: string;
  actualReleaseDate?: string;
  releaseNotesUrl?: string;
  generateReleaseNotes?: boolean;
}

export interface MergeVersionsRequest {
  sourceVersionId: string;
  targetVersionId: string;
  issueIdsToMove?: string[];
}

export interface VersionReleaseNoteResponse {
  versionId: string;
  content?: string;
  generatedAt?: string;
}

const BASE = '/versions';

export const versionApi = {
  getByProject: (projectId: string, includeArchived = false) =>
    apiClient
      .get<VersionResponse[]>(`${BASE}/project/${projectId}`, {
        params: { includeArchived },
      })
      .then((r) => r.data),

  getById: (versionId: string) =>
    apiClient.get<VersionResponse>(`${BASE}/${versionId}`).then((r) => r.data),

  create: (data: CreateVersionRequest) =>
    apiClient.post<VersionResponse>(BASE, data).then((r) => r.data),

  update: (versionId: string, data: UpdateVersionRequest) =>
    apiClient.put<VersionResponse>(`${BASE}/${versionId}`, data).then((r) => r.data),

  delete: (versionId: string) => apiClient.delete(`${BASE}/${versionId}`),

  release: (versionId: string, data: ReleaseVersionRequest = {}) =>
    apiClient.post<VersionResponse>(`${BASE}/${versionId}/release`, data).then((r) => r.data),

  archive: (versionId: string) =>
    apiClient.post<VersionResponse>(`${BASE}/${versionId}/archive`).then((r) => r.data),

  unarchive: (versionId: string) =>
    apiClient.post<VersionResponse>(`${BASE}/${versionId}/unarchive`).then((r) => r.data),

  restore: (versionId: string) =>
    apiClient.post<VersionResponse>(`${BASE}/${versionId}/restore`).then((r) => r.data),

  merge: (data: MergeVersionsRequest) =>
    apiClient.post<VersionResponse>(`${BASE}/merge`, data).then((r) => r.data),

  generateReleaseNotes: (versionId: string) =>
    apiClient
      .post<VersionReleaseNoteResponse>(`${BASE}/${versionId}/release-notes/generate`)
      .then((r) => r.data),

  recordMetricsSnapshot: (versionId: string) =>
    apiClient.post<unknown>(`${BASE}/${versionId}/metrics/snapshot`).then((r) => r.data),

  getMetrics: (versionId: string) =>
    apiClient.get<unknown[]>(`${BASE}/${versionId}/metrics`).then((r) => r.data),

  getAuditLogs: (versionId: string) =>
    apiClient.get<unknown[]>(`${BASE}/${versionId}/audit`).then((r) => r.data),

  assignFixVersion: (issueId: string, versionId: string) =>
    apiClient.post(`${BASE}/fix-version`, null, { params: { issueId, versionId } }),

  removeFixVersion: (issueId: string, versionId: string) =>
    apiClient.delete(`${BASE}/fix-version`, { params: { issueId, versionId } }),

  assignAffectsVersion: (issueId: string, versionId: string) =>
    apiClient.post(`${BASE}/affects-version`, null, { params: { issueId, versionId } }),

  removeAffectsVersion: (issueId: string, versionId: string) =>
    apiClient.delete(`${BASE}/affects-version`, { params: { issueId, versionId } }),

  bulkAssignFixVersion: (issueIds: string[], versionId: string) =>
    apiClient
      .post<number>(`${BASE}/bulk-assign`, { issueIds, versionId })
      .then((r) => r.data),
};
