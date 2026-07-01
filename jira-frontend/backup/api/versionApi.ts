import axiosClient from './axiosClient';

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
  deploymentStatus: string;
  releaseStatus: string;
  releaseNotesUrl?: string;
  releaseNotesGenerated: boolean;
  color?: string;
  createdBy?: string;
  updatedBy?: string;
  releasedBy?: string;
  archivedBy?: string;
  createdAt: string;
  updatedAt: string;
  overdue: boolean;
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

export const versionApi = {
  getByProject: async (projectId: string, includeArchived = false): Promise<VersionResponse[]> => {
    const response = await axiosClient.get(`/versions/project/${projectId}?includeArchived=${includeArchived}`);
    return response.data;
  },

  getById: async (versionId: string): Promise<VersionResponse> => {
    const response = await axiosClient.get(`/versions/${versionId}`);
    return response.data;
  },

  create: async (data: CreateVersionRequest): Promise<VersionResponse> => {
    const response = await axiosClient.post('/versions', data);
    return response.data;
  },

  update: async (versionId: string, data: UpdateVersionRequest): Promise<VersionResponse> => {
    const response = await axiosClient.put(`/versions/${versionId}`, data);
    return response.data;
  },

  delete: async (versionId: string): Promise<void> => {
    await axiosClient.delete(`/versions/${versionId}`);
  },

  release: async (versionId: string, data: ReleaseVersionRequest): Promise<VersionResponse> => {
    const response = await axiosClient.post(`/versions/${versionId}/release`, data);
    return response.data;
  },

  archive: async (versionId: string): Promise<VersionResponse> => {
    const response = await axiosClient.post(`/versions/${versionId}/archive`);
    return response.data;
  },

  unarchive: async (versionId: string): Promise<VersionResponse> => {
    const response = await axiosClient.post(`/versions/${versionId}/unarchive`);
    return response.data;
  },

  restore: async (versionId: string): Promise<VersionResponse> => {
    const response = await axiosClient.post(`/versions/${versionId}/restore`);
    return response.data;
  },

  merge: async (data: MergeVersionsRequest): Promise<VersionResponse> => {
    const response = await axiosClient.post('/versions/merge', data);
    return response.data;
  },

  bulkAssign: async (issueIds: string[], versionId: string): Promise<number> => {
    const response = await axiosClient.post('/versions/bulk-assign', { issueIds, versionId });
    return response.data;
  },

  bulkMove: async (issueIds: string[], sourceVersionId: string, targetVersionId: string): Promise<number> => {
    const response = await axiosClient.post('/versions/bulk-move', {
      issueIds,
      versionId: sourceVersionId,
      targetVersionId
    });
    return response.data;
  },

  generateReleaseNotes: async (versionId: string): Promise<any> => {
    const response = await axiosClient.post(`/versions/${versionId}/release-notes/generate`);
    return response.data;
  },

  getMetrics: async (versionId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/versions/${versionId}/metrics`);
    return response.data;
  },

  getDeployments: async (versionId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/versions/${versionId}/deployments`);
    return response.data;
  },

  getBuilds: async (versionId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/versions/${versionId}/builds`);
    return response.data;
  },

  getAuditLogs: async (versionId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/versions/${versionId}/audit`);
    return response.data;
  },

  getReleaseTrains: async (): Promise<any[]> => {
    const response = await axiosClient.get('/versions/trains');
    return response.data;
  },

  assignFixVersion: async (issueId: string, versionId: string): Promise<void> => {
    await axiosClient.post('/versions/fix-version', null, { params: { issueId, versionId } });
  },

  removeFixVersion: async (issueId: string, versionId: string): Promise<void> => {
    await axiosClient.delete('/versions/fix-version', { params: { issueId, versionId } });
  },

  assignAffectsVersion: async (issueId: string, versionId: string): Promise<void> => {
    await axiosClient.post('/versions/affects-version', null, { params: { issueId, versionId } });
  },

  removeAffectsVersion: async (issueId: string, versionId: string): Promise<void> => {
    await axiosClient.delete('/versions/affects-version', { params: { issueId, versionId } });
  },
};