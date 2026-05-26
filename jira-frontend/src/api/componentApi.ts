import apiClient from './axiosClient';

export interface ComponentResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  leadUserId?: string;
  assigneeType: string;
  defaultAssignee?: string;
  archived: boolean;
  color?: string;
  icon?: string;
  sequence: number;
  createdAt?: string;
  updatedAt?: string;
  issueCount?: number;
  openIssueCount?: number;
  closedIssueCount?: number;
}

export interface CreateComponentRequest {
  projectId: string;
  name: string;
  description?: string;
  leadUserId?: string;
  assigneeType?: string;
  defaultAssignee?: string;
  color?: string;
  icon?: string;
}

export interface UpdateComponentRequest {
  name?: string;
  description?: string;
  leadUserId?: string;
  assigneeType?: string;
  defaultAssignee?: string;
  color?: string;
  icon?: string;
  sequence?: number;
}

export interface TransferOwnershipRequest {
  newLeadId: string;
  reason?: string;
  transferredBy?: string;
}

export const ASSIGNEE_TYPES = [
  { value: 'PROJECT_DEFAULT', label: 'Project default assignee' },
  { value: 'COMPONENT_LEAD', label: 'Component lead' },
  { value: 'UNASSIGNED', label: 'Unassigned' },
  { value: 'SPECIFIC_USER', label: 'Specific user' },
] as const;

const BASE = '/components';

export const componentApi = {
  getByProject: (projectId: string, includeArchived = false) =>
    apiClient
      .get<ComponentResponse[]>(`${BASE}/project/${projectId}`, {
        params: { includeArchived },
      })
      .then((r) => r.data),

  getById: (componentId: string) =>
    apiClient.get<ComponentResponse>(`${BASE}/${componentId}`).then((r) => r.data),

  create: (data: CreateComponentRequest) =>
    apiClient.post<ComponentResponse>(BASE, data).then((r) => r.data),

  update: (componentId: string, data: UpdateComponentRequest) =>
    apiClient.put<ComponentResponse>(`${BASE}/${componentId}`, data).then((r) => r.data),

  delete: (componentId: string) => apiClient.delete(`${BASE}/${componentId}`),

  archive: (componentId: string) =>
    apiClient.post<ComponentResponse>(`${BASE}/${componentId}/archive`).then((r) => r.data),

  unarchive: (componentId: string) =>
    apiClient.post<ComponentResponse>(`${BASE}/${componentId}/unarchive`).then((r) => r.data),

  restore: (componentId: string) =>
    apiClient.post<ComponentResponse>(`${BASE}/${componentId}/restore`).then((r) => r.data),

  transferOwnership: (componentId: string, data: TransferOwnershipRequest) =>
    apiClient
      .post<ComponentResponse>(`${BASE}/${componentId}/transfer-ownership`, data)
      .then((r) => r.data),

  assignToIssue: (issueId: string, componentId: string) =>
    apiClient.post(`${BASE}/issue`, null, { params: { issueId, componentId } }),

  removeFromIssue: (issueId: string, componentId: string) =>
    apiClient.delete(`${BASE}/issue`, { params: { issueId, componentId } }),

  getIssueComponents: (issueId: string) =>
    apiClient.get<string[]>(`${BASE}/issue/${issueId}`).then((r) => r.data),

  getAuditLogs: (componentId: string) =>
    apiClient.get<unknown[]>(`${BASE}/${componentId}/audit`).then((r) => r.data),

  bulkAssign: (issueIds: string[], componentId: string) =>
    apiClient
      .post<number>(`${BASE}/bulk-assign`, { issueIds, componentId })
      .then((r) => r.data),
};
