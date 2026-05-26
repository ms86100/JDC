import apiClient from './axiosClient';
import { normalizeIssue } from './issueMapper';
import { toBackendCreatePayload, toBackendUpdatePayload } from './issuePayload';

// ==================== Full Issue Types ====================

export interface IssueResponse {
  id: string;
  projectId?: string;
  issueKey?: string;
  issueNumber?: number;
  title?: string;
  description?: string;
  status?: string;
  statusId?: string;
  priority?: string;
  priorityId?: string;
  issueType?: string;
  issueTypeId?: string;
  reporterId?: string;
  assigneeId?: string;
  parentIssueId?: string;
  parentId?: string;
  epicId?: string;
  sprintId?: string;
  rank?: string;
  createdAt: string;
  updatedAt: string;
  dueDate?: string;
  labels?: string[];
  storyPoints?: number;
  originalEstimate?: number;
  remainingEstimate?: number;
  timeSpent?: number;
  resolutionId?: string;
  resolvedAt?: string;
  classification?: 'PUBLIC' | 'RESTRICTED' | 'CONFIDENTIAL' | 'EXPORT_CONTROLLED';
  components?: string[];
  versions?: string[];
  watchers?: string[];
  votes?: number;
  securityLevel?: string;
  securityLevelId?: string;
  environment?: string;
  componentIds?: string[];
  affectsVersionIds?: string[];
  fixVersionIds?: string[];
  // Calculated fields
  assigneeName?: string;
  reporterName?: string;
  sprintName?: string;
  epicName?: string;
  resolution?: string;
  linkedIssues?: Array<{ type: string; key: string; title: string }>;
  children?: IssueResponse[];
}

export interface CreateIssueRequest {
  projectId: string;
  title: string;
  description?: string;
  issueTypeId: string;
  priorityId?: string;
  reporterId?: string;
  assigneeId?: string;
  parentIssueId?: string;
  parentId?: string;
  epicId?: string;
  sprintId?: string;
  dueDate?: string;
  storyPoints?: number;
  originalEstimateSeconds?: number;
  remainingEstimateSeconds?: number;
  labels?: string[];
  classification?: string;
  securityLevelId?: string;
  environment?: string;
  teamId?: string;
  fixVersionIds?: string[];
  affectsVersionIds?: string[];
  componentIds?: string[];
  linkedIssues?: Array<{ targetIssueKey: string; linkType: string }>;
}

export interface UpdateIssueRequest extends Partial<CreateIssueRequest> {
  statusId?: string;
  remainingEstimate?: number;
  timeSpent?: number;
}

export interface IssueStatus {
  id: string;
  name: string;
  sequence: number;
  category: string;
  color: string;
}

export interface IssueType {
  id: string;
  name: string;
  icon: string;
  description: string;
  color: string;
  category: 'STANDARD' | 'SUBTASK' | 'EPIC';
  isSubtask: boolean;
}

export interface IssuePriority {
  id: string;
  name: string;
  icon: string;
  color: string;
  sequence: number;
}

export interface Project {
  id: string;
  name: string;
  key: string;
  description?: string;
  leadId?: string;
  leadName?: string;
  projectType: string;
  template: string;
  avatarUrl?: string;
  isArchived: boolean;
  classification?: string;
  issueCounter: number;
  url?: string;
  workflowSchemeId?: string;
}

export interface Sprint {
  id: string;
  name: string;
  state: 'FUTURE' | 'ACTIVE' | 'CLOSED';
  goal?: string;
  startDate?: string;
  endDate?: string;
  completedDate?: string;
  boardId?: string;
}

export interface Version {
  id: string;
  name: string;
  description?: string;
  releaseDate?: string;
  released: boolean;
  archived: boolean;
  projectId: string;
}

export interface Component {
  id: string;
  name: string;
  description?: string;
  leadId?: string;
  leadName?: string;
  projectId: string;
}

// ==================== API Functions ====================

export const issueApi = {
  // Issue CRUD
  create: (data: CreateIssueRequest) =>
    apiClient.post<IssueResponse>('/issues', toBackendCreatePayload(data)),
  getAll: (params?: Record<string, string>) => apiClient.get<{ content: IssueResponse[]; totalElements: number }>('/issues', { params }),
  getById: async (id: string) => {
    const response = await apiClient.get<IssueResponse>(`/issues/${id}`);
    response.data = normalizeIssue(response.data as unknown as Record<string, unknown>);
    return response;
  },

  getByKey: (issueKey: string) =>
    apiClient.get<IssueResponse>(`/issues/by-key/${encodeURIComponent(issueKey)}`),

  getBatch: (ids: string[]) =>
    apiClient.get<IssueResponse[]>('/issues/batch', {
      params: { ids: ids.join(',') },
    }),
  update: (id: string, data: UpdateIssueRequest) =>
    apiClient.put<IssueResponse>(`/issues/${id}`, toBackendUpdatePayload(data)),
  delete: (id: string) => apiClient.delete(`/issues/${id}`),

  // Status transitions (workflow engine)
  transitionStatus: (id: string, projectId: string, data: {
    statusId?: string;
    transitionId?: string;
    comment?: string;
    resolutionId?: string;
    screenInput?: Record<string, unknown>;
  }) => apiClient.patch<IssueResponse>(`/issues/${id}/status`, data, { params: { projectId } }),

  executeTransition: (data: {
    issueId: string;
    projectId: string;
    transitionId: string;
    comment?: string;
    resolutionId?: string;
    screenInput?: Record<string, unknown>;
  }) => apiClient.post('/workflows/transitions/execute', data),

  getAvailableTransitions: (id: string, projectId: string) =>
    apiClient.get<{
      issueId: string;
      workflowId: string;
      currentStatusId: string;
      transitions: Array<{
        id: string;
        name: string;
        description?: string;
        toStatusId: string;
        hasScreen?: boolean;
        screenFields?: Array<{ fieldId: string; fieldName: string; required: boolean }>;
      }>;
    }>(`/issues/${id}/transitions`, { params: { projectId } }),

  // Watch & Vote
  watch: (id: string) => apiClient.post(`/issues/${id}/watch`),
  unwatch: (id: string) => apiClient.delete(`/issues/${id}/watch`),
  vote: (id: string) => apiClient.post(`/issues/${id}/vote`),
  unvote: (id: string) => apiClient.delete(`/issues/${id}/vote`),

  // Clone & Move (backend uses @RequestParam, not body)
  clone: (id: string, data?: { projectId?: string }) =>
    apiClient.post<IssueResponse>(`/issues/${id}/clone`, null, {
      params: { projectId: data?.projectId },
    }),
  cloneToProject: (id: string, targetProjectId: string) =>
    apiClient.post<IssueResponse>(`/issues/${id}/clone-to-project`, null, {
      params: { targetProjectId },
    }),
  move: (id: string, data: { projectId: string }) =>
    apiClient.post<IssueResponse>(`/issues/${id}/move`, null, {
      params: { targetProjectId: data.projectId },
    }),

  // Link issues (key-based or ID-based)
  linkIssue: async (id: string, data: { targetIssueId?: string; targetIssueKey?: string; linkType: string }) => {
    let resolvedId = data.targetIssueId;
    if (data.targetIssueKey && !resolvedId) {
      const found = await apiClient.get<IssueResponse>(`/issues/by-key/${encodeURIComponent(data.targetIssueKey)}`);
      resolvedId = found.data.id;
    }
    return apiClient.post('/issues/links', {
      sourceIssueId: id,
      targetIssueId: resolvedId,
      linkTypeName: data.linkType,
    });
  },
  unlinkIssue: (id: string, linkId: string) => apiClient.delete(`/issues/links/${linkId}`),
  getLinks: (id: string) => apiClient.get<Array<{ id: string; type: string; targetIssue: IssueResponse }>>(`/issues/${id}/links`),

  // Subtasks
  getSubtasks: (id: string) => apiClient.get<IssueResponse[]>(`/issues/${id}/subtasks`),

  // Labels
  addLabel: (id: string, label: string) => apiClient.post(`/issues/${id}/labels`, { label }),
  removeLabel: (id: string, label: string) => apiClient.delete(`/issues/${id}/labels/${encodeURIComponent(label)}`),

  // Versions
  addFixVersion: (id: string, versionId: string) => apiClient.post(`/issues/${id}/fix-versions`, { versionId }),
  removeFixVersion: (id: string, versionId: string) => apiClient.delete(`/issues/${id}/fix-versions/${versionId}`),

  // Components
  addComponent: (id: string, componentId: string) => apiClient.post(`/issues/${id}/components`, { componentId }),
  removeComponent: (id: string, componentId: string) => apiClient.delete(`/issues/${id}/components/${componentId}`),

  // Enums lookups
  getTypes: () => apiClient.get<IssueType[]>('/issues/types'),
  getPriorities: () => apiClient.get<IssuePriority[]>('/issues/priorities'),
  getStatuses: () => apiClient.get<IssueStatus[]>('/issues/statuses'),
};

export const projectApi = {
  getAll: (params?: { search?: string; archived?: boolean; page?: number; size?: number }) =>
    apiClient.get<{ content: Project[]; totalElements: number }>('/projects', { params }),
  getById: (id: string) => apiClient.get<Project>(`/projects/${id}`),
  create: (data: Partial<Project>) => apiClient.post<Project>('/projects', data),
  update: (id: string, data: Partial<Project>) => apiClient.put<Project>(`/projects/${id}`, data),
  delete: (id: string) => apiClient.delete(`/projects/${id}`),
  archive: (id: string) => apiClient.post(`/projects/${id}/archive`),
  unarchive: (id: string) => apiClient.post(`/projects/${id}/unarchive`),
  getVersions: async (projectId: string) => {
    const { versionApi } = await import('./versionApi');
    return { data: await versionApi.getByProject(projectId) };
  },
  getComponents: async (projectId: string) => {
    const { componentApi } = await import('./componentApi');
    return { data: await componentApi.getByProject(projectId) };
  },
  getSprints: (projectId: string) => apiClient.get<Sprint[]>(`/sprints?projectId=${projectId}`),
};

export const sprintApi = {
  getAll: (params?: { boardId?: string; state?: string; projectId?: string }) =>
    apiClient.get<Sprint[]>('/sprints', { params }),
  getById: (id: string) => apiClient.get<Sprint>(`/sprints/${id}`),
  create: (data: Partial<Sprint>) => apiClient.post<Sprint>('/sprints', data),
  update: (id: string, data: Partial<Sprint>) => apiClient.put<Sprint>(`/sprints/${id}`, data),
  start: (id: string) => apiClient.post<Sprint>(`/sprints/${id}/start`),
  complete: (id: string) => apiClient.post<Sprint>(`/sprints/${id}/complete`),
};

/** @deprecated Import from `api/versionApi` — thin wrappers for legacy callers */
export const versionApi = {
  getByProject: async (projectId: string) => {
    const { versionApi: vApi } = await import('./versionApi');
    const data = await vApi.getByProject(projectId);
    return { data: data as Version[] };
  },
  create: async (projectId: string, data: Partial<Version>) => {
    const { versionApi: vApi } = await import('./versionApi');
    const created = await vApi.create({
      projectId,
      name: data.name ?? 'Version',
      description: data.description,
      releaseDate: data.releaseDate,
    });
    return { data: created as unknown as Version };
  },
  update: async (versionId: string, data: Partial<Version>) => {
    const { versionApi: vApi } = await import('./versionApi');
    const updated = await vApi.update(versionId, data);
    return { data: updated as unknown as Version };
  },
  release: async (versionId: string) => {
    const { versionApi: vApi } = await import('./versionApi');
    const released = await vApi.release(versionId);
    return { data: released as unknown as Version };
  },
  archive: async (versionId: string) => {
    const { versionApi: vApi } = await import('./versionApi');
    const archived = await vApi.archive(versionId);
    return { data: archived as unknown as Version };
  },
};

/** @deprecated Import from `api/componentApi` */
export const componentApi = {
  getByProject: async (projectId: string) => {
    const { componentApi: cApi } = await import('./componentApi');
    const data = await cApi.getByProject(projectId);
    return { data: data as unknown as Component[] };
  },
  create: async (projectId: string, data: Partial<Component>) => {
    const { componentApi: cApi } = await import('./componentApi');
    const created = await cApi.create({
      projectId,
      name: data.name ?? 'Component',
      description: data.description,
      leadUserId: data.leadId,
    });
    return { data: created as unknown as Component };
  },
  update: async (componentId: string, data: Partial<Component>) => {
    const { componentApi: cApi } = await import('./componentApi');
    const updated = await cApi.update(componentId, {
      name: data.name,
      description: data.description,
      leadUserId: data.leadId,
    });
    return { data: updated as unknown as Component };
  },
  delete: async (componentId: string) => {
    const { componentApi: cApi } = await import('./componentApi');
    await cApi.delete(componentId);
  },
};

// ==================== Custom Fields ====================

export interface CustomField {
  id: string;
  name: string;
  description?: string;
  type: string;
  projectId?: string;
  options?: Array<{ value: string; label: string }>;
  isRequired: boolean;
  createdAt: string;
}

export interface CustomFieldValue {
  id: string;
  customFieldId: string;
  issueId: string;
  value: any;
}

export const customFieldApi = {
  getAll: () => apiClient.get<CustomField[]>('/custom-fields'),
  getById: (id: string) => apiClient.get<CustomField>(`/custom-fields/${id}`),
  create: (data: Partial<CustomField>) => apiClient.post<CustomField>('/custom-fields', data),
  update: (id: string, data: Partial<CustomField>) => apiClient.put<CustomField>(`/custom-fields/${id}`, data),
  delete: (id: string) => apiClient.delete(`/custom-fields/${id}`),
  getByIssue: (issueId: string) => apiClient.get<CustomFieldValue[]>(`/issues/${issueId}/custom-fields`),
  setValue: (issueId: string, customFieldId: string, value: any) =>
    apiClient.post<CustomFieldValue>(`/issues/${issueId}/custom-fields/${customFieldId}`, { value }),
};

// ==================== Resolution ====================

export interface Resolution {
  id: string;
  name: string;
  description?: string;
  sequence: number;
}

// ==================== Issue Link Types ====================

export interface IssueLinkType {
  id: string;
  name: string;
  inward: string;
  outward: string;
  style?: string;
  sequence: number;
}

// ==================== Security Levels ====================

export interface SecurityLevel {
  id: string;
  name: string;
  description?: string;
  sortOrder: number;
}

export const resolutionApi = {
  getAll: () => apiClient.get<Resolution[]>('/admin/issues/resolutions'),
  create: (data: Partial<Resolution>) => apiClient.post<Resolution>('/admin/issues/resolutions', data),
  update: (id: string, data: Partial<Resolution>) => apiClient.put<Resolution>(`/admin/issues/resolutions/${id}`, data),
  delete: (id: string) => apiClient.delete(`/admin/issues/resolutions/${id}`),
};

// ==================== Issue Link Types ====================

export const issueLinkTypeApi = {
  getAll: () => apiClient.get<IssueLinkType[]>('/issues/links/types'),
};

// ==================== Security Levels ====================

export const securityLevelApi = {
  getAll: () => apiClient.get<SecurityLevel[]>('/security-levels'),
};