import apiClient from './axiosClient';

// ============ Basic Types ============
export interface ProjectResponse {
  id: string;
  projectKey: string;
  name: string;
  description?: string;
  leadUserId?: string;
  leadName?: string;
  projectType: 'SOFTWARE' | 'BUSINESS';
  template?: string;
  category?: string;
  defaultAssigneeType?: string;
  allowIssueCreation?: boolean;
  archived?: boolean;
  classification?: 'PUBLIC' | 'RESTRICTED' | 'CONFIDENTIAL' | 'EXPORT_CONTROLLED';
  issueCounter: number;
  url?: string;
  avatarUrl?: string;
  workflowSchemeId?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
  leadUserId?: string;
  projectType?: string;
  template?: string;
}

export interface ProjectMemberResponse {
  id: string;
  projectId: string;
  userId: string;
  userName?: string;
  role: string;
  joinedAt: string;
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

// ============ Project Type & Template Types ============
export interface ProjectType {
  id: string;
  name: string;
  description: string;
  category: 'COMPANY_MANAGED' | 'TEAM_MANAGED';
  icon: string;
  sortOrder: number;
  isActive: boolean;
}

export interface ProjectTemplate {
  id: string;
  typeId: string;
  typeName: string;
  name: string;
  description: string;
  icon: string;
  color: string;
  defaultAssigneeType: string;
  allowIssueCreation: boolean;
  sortOrder: number;
  isActive: boolean;
}

export interface TemplateDetails {
  templateId: string;
  templateName: string;
  icon: string;
  color: string;
  defaultAssigneeType: string;
  allowIssueCreation: boolean;
  issueTypeSchemeId: string;
  issueTypeSchemeName: string;
  workflowSchemeId: string;
  workflowSchemeName: string;
  permissionSchemeId: string;
  permissionSchemeName: string;
  notificationSchemeId: string;
  notificationSchemeName: string;
  screenSchemeId: string;
  screenSchemeName: string;
  defaultRoles: string[];
}

export interface ProjectScheme {
  id: string;
  projectId: string;
  issueTypeScheme: {
    id: string;
    name: string;
    issueTypeIds: string[];
    defaultIssueTypeId: string;
  };
  workflowScheme: {
    id: string;
    name: string;
    defaultWorkflowId: string;
  };
  permissionScheme: {
    id: string;
    name: string;
    permissions: string;
  };
  notificationScheme: {
    id: string;
    name: string;
    notifications: string;
  };
  screenScheme: {
    id: string;
    name: string;
    screens: { screenType: string; screenId: string }[];
  };
}

// ============ Wizard Request Type ============
export interface CreateProjectWizardRequest {
  projectType: 'COMPANY_MANAGED' | 'TEAM_MANAGED';
  templateId?: string;
  name: string;
  projectKey: string;
  leadUserId?: string;
  defaultAssigneeType?: string;
  description?: string;
  avatarUrl?: string;
  allowIssueCreation?: boolean;
}

// ============ API Functions ============
export const projectApi = {
  // Existing endpoints
  create: (data: { name: string; description?: string; leadUserId?: string }) =>
    apiClient.post<ProjectResponse>('/projects', data),
  getAll: (params?: { search?: string; archived?: boolean; page?: number; size?: number }) =>
    apiClient.get<ProjectResponse[] | { content: ProjectResponse[] }>('/projects', { params })
      .then(response => {
        const data = response.data;
        if (Array.isArray(data)) return data;
        return data?.content ?? [];
      }),
  getById: (id: string) => apiClient.get<ProjectResponse>(`/api/projects/${id}`),
  update: (id: string, data: Partial<{ name: string; description?: string; leadUserId?: string }>) =>
    apiClient.put<ProjectResponse>(`/api/projects/${id}`, data),
  delete: (id: string) => apiClient.delete(`/api/projects/${id}`),
  archive: (id: string) => apiClient.post(`/api/projects/${id}/archive`),
  unarchive: (id: string) => apiClient.post(`/api/projects/${id}/unarchive`),
  addMember: (id: string, userId: string, projectRoleName: string) =>
    apiClient.post(`/api/projects/${id}/members`, { userId, projectRoleName }),
  getMembers: (id: string) => apiClient.get<ProjectMemberResponse[]>(`/api/projects/${id}/members`),
  getVersions: async (projectId: string) => {
    const { versionApi } = await import('./versionApi');
    const data = await versionApi.getByProject(projectId);
    return { data: data as Version[] };
  },
  getComponents: async (projectId: string) => {
    const { componentApi } = await import('./componentApi');
    const data = await componentApi.getByProject(projectId);
    return {
      data: data.map((c) => ({
        id: c.id,
        name: c.name,
        description: c.description,
        leadId: c.leadUserId,
        leadName: c.leadUserId,
        projectId: c.projectId,
      })) as Component[],
    };
  },
  getSprints: (projectId: string) => apiClient.get(`/api/sprints?projectId=${projectId}`),

  // Wizard endpoints
  getProjectTypes: () => apiClient.get<ProjectType[]>('/projects/types'),
  getTemplatesForType: (typeId: string) =>
    apiClient.get<ProjectTemplate[]>(`/api/projects/types/${typeId}/templates`),
  getTemplateDetails: (templateId: string) =>
    apiClient.get<TemplateDetails>(`/api/projects/templates/${templateId}`),
  getProjectScheme: (projectId: string) =>
    apiClient.get<ProjectScheme>(`/api/projects/${projectId}/scheme`),
  createViaWizard: (data: CreateProjectWizardRequest) =>
    apiClient.post<ProjectResponse>('/projects/wizard', data),
  checkProjectKey: (key: string) =>
    apiClient.get<{ projectKey: string; valid: boolean; available: boolean; message: string }>(
      `/api/projects/key/check/${key}`
    ),

  /** Re-index all project issues in the search service (DC project maintenance). */
  reindexSearch: async (projectId: string): Promise<{ indexed: number; total: number }> => {
    const { issueApi } = await import('./issueApi');
    const { searchApi } = await import('./serviceApi');
    const res = await issueApi.getAll({ projectId });
    const data = res.data;
    const list = Array.isArray(data) ? data : (data?.content ?? []);
    let indexed = 0;
    for (const issue of list) {
      try {
        await searchApi.indexEntity({
          entityType: 'ISSUE',
          entityId: issue.id,
          title: issue.title ?? issue.issueKey ?? issue.id,
          content: issue.description ?? '',
        });
        indexed += 1;
      } catch {
        /* continue with remaining issues */
      }
    }
    return { indexed, total: list.length };
  },
};