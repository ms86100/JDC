import apiClient from './axiosClient';

export interface WorkflowStatusLink {
  id: string;
  workflowId: string;
  statusId: string;
  statusName?: string;
  statusCategory?: string;
  statusColor?: string;
  sequence: number;
}

export interface WorkflowTransitionDetail {
  id: string;
  workflowId: string;
  name: string;
  description?: string;
  fromStatusId: string;
  toStatusId: string;
  fromStatusName?: string;
  toStatusName?: string;
  fromStatusCategory?: string;
  toStatusCategory?: string;
  fromStatusColor?: string;
  toStatusColor?: string;
  displayOrder?: number;
  requiresApproval?: boolean;
  type?: string;
  conditions?: WorkflowComponent[];
  validators?: WorkflowComponent[];
  postFunctions?: WorkflowComponent[];
}

export interface WorkflowComponent {
  id: string;
  type: string;
  name?: string;
  description?: string;
  configuration?: Record<string, unknown>;
  sequence?: number;
}

export interface Workflow {
  id: string;
  name: string;
  description?: string;
  isDraft: boolean;
  isActive: boolean;
  isSystem?: boolean;
  isDefault?: boolean;
  projectId?: string;
  draftOfWorkflowId?: string;
  statusCount?: number;
  transitionCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowSchemeMapping {
  id: string;
  issueTypeId: string;
  issueTypeName?: string;
  workflowId: string;
  workflowName?: string;
}

export interface WorkflowScheme {
  id: string;
  name: string;
  description?: string;
  isDefault?: boolean;
  defaultWorkflowId?: string;
  isDraft?: boolean;
  isActive?: boolean;
  mappings?: WorkflowSchemeMapping[];
  issueTypeCount?: number;
  projectCount?: number;
}

export interface WorkflowVersion {
  id: string;
  versionNumber: number;
  changeDescription: string;
  changeType: string;
  createdAt: string;
  createdBy?: string;
}

export const workflowApi = {
  getAll: () => apiClient.get<Workflow[]>('/api/workflows'),
  getById: (id: string) => apiClient.get<Workflow>(`/api/workflows/${id}`),
  getWorkflowDetail: (id: string) =>
    apiClient.get<{
      workflow: Workflow;
      statuses: WorkflowStatusLink[];
      transitions: WorkflowTransitionDetail[];
      versions: WorkflowVersion[];
    }>(`/api/workflows/${id}/detail`),
  create: (data: { name: string; description?: string; projectId?: string }) =>
    apiClient.post<Workflow>('/api/workflows', data),
  update: (id: string, data: Partial<Workflow>) =>
    apiClient.put<Workflow>(`/api/workflows/${id}`, data),
  delete: (id: string) => apiClient.delete(`/api/workflows/${id}`),
  clone: (id: string, newName: string) =>
    apiClient.post<Workflow>(`/api/admin/workflows/${id}/clone`, { newName }),

  getWorkflowStatuses: (workflowId: string) =>
    apiClient.get<WorkflowStatusLink[]>(`/api/workflows/${workflowId}/statuses`),
  addStatusToWorkflow: (workflowId: string, statusId: string, sequence?: number) =>
    apiClient.post<WorkflowStatusLink>(`/api/workflows/${workflowId}/statuses`, { statusId, sequence }),
  removeStatusFromWorkflow: (workflowId: string, workflowStatusId: string) =>
    apiClient.delete(`/api/workflows/${workflowId}/statuses/${workflowStatusId}`),

  createTransition: (data: {
    workflowId: string;
    name: string;
    fromStatusId: string;
    toStatusId: string;
    description?: string;
    requiresApproval?: boolean;
  }) => apiClient.post('/api/workflows/transitions', data),
  updateTransition: (transitionId: string, data: Partial<WorkflowTransitionDetail>) =>
    apiClient.put(`/api/workflows/transitions/${transitionId}`, data),
  deleteTransition: (transitionId: string) =>
    apiClient.delete(`/api/workflows/transitions/${transitionId}`),
  getTransitionsWithDetails: (workflowId: string) =>
    apiClient.get<WorkflowTransitionDetail[]>(`/api/workflows/${workflowId}/transitions-with-details`),

  addCondition: (transitionId: string, data: { type: string; configuration?: Record<string, unknown> }) =>
    apiClient.post(`/api/workflows/transitions/${transitionId}/conditions`, data),
  addValidator: (transitionId: string, data: { type: string; configuration?: Record<string, unknown> }) =>
    apiClient.post(`/api/workflows/transitions/${transitionId}/validators`, data),
  addPostFunction: (transitionId: string, data: { type: string; configuration?: Record<string, unknown> }) =>
    apiClient.post(`/api/workflows/transitions/${transitionId}/post-functions`, data),
  deleteCondition: (transitionId: string, conditionId: string) =>
    apiClient.delete(`/api/workflows/transitions/${transitionId}/conditions/${conditionId}`),
  deleteValidator: (transitionId: string, validatorId: string) =>
    apiClient.delete(`/api/workflows/transitions/${transitionId}/validators/${validatorId}`),
  deletePostFunction: (transitionId: string, functionId: string) =>
    apiClient.delete(`/api/workflows/transitions/${transitionId}/post-functions/${functionId}`),

  getConditionDefinitions: () => apiClient.get('/api/admin/workflows/conditions/definitions'),
  getValidatorDefinitions: () => apiClient.get('/api/admin/workflows/validators/definitions'),
  getPostFunctionDefinitions: () => apiClient.get('/api/admin/workflows/post-functions/definitions'),

  getSchemes: () => apiClient.get<WorkflowScheme[]>('/api/workflow-schemes'),
  getScheme: (id: string) => apiClient.get<WorkflowScheme>(`/api/workflow-schemes/${id}`),
  createScheme: (data: { name: string; description?: string; defaultWorkflowId?: string }) =>
    apiClient.post<WorkflowScheme>('/api/workflow-schemes', data),
  updateScheme: (id: string, data: { name: string; description?: string; defaultWorkflowId?: string }) =>
    apiClient.put<WorkflowScheme>(`/api/workflow-schemes/${id}`, data),
  deleteScheme: (id: string) => apiClient.delete(`/api/workflow-schemes/${id}`),
  addSchemeMapping: (schemeId: string, data: { issueTypeId: string; workflowId: string }) =>
    apiClient.post<WorkflowScheme>(`/api/workflow-schemes/${schemeId}/mappings`, data),
  removeSchemeMapping: (schemeId: string, mappingId: string) =>
    apiClient.delete(`/api/workflow-schemes/${schemeId}/mappings/${mappingId}`),
  publishScheme: (schemeId: string) =>
    apiClient.post<WorkflowScheme>(`/api/workflow-schemes/${schemeId}/publish`),
  assignSchemeToProject: (projectId: string, schemeId: string) =>
    apiClient.put(`/api/workflow-schemes/projects/${projectId}/assign`, { schemeId }),
  getProjectScheme: (projectId: string) =>
    apiClient.get<{ projectId: string; schemeId: string }>(`/api/workflow-schemes/projects/${projectId}`),

  syncDesignerLayout: (workflowId: string, nodes: Array<{ nodeId: string; positionX: number; positionY: number }>) =>
    apiClient.put(`/api/workflow-schemes/workflows/${workflowId}/layout/positions`, { nodes }),
  getLayout: (workflowId: string) => apiClient.get(`/api/workflow-schemes/workflows/${workflowId}/layout`),
  autoLayout: (workflowId: string) => apiClient.post(`/api/workflow-schemes/workflows/${workflowId}/layout/auto`),
  publishWorkflow: (workflowId: string) => apiClient.post(`/api/admin/workflows/${workflowId}/publish`),
  createWorkflowDraft: (workflowId: string) =>
    apiClient.post(`/api/workflow-schemes/workflows/${workflowId}/draft`),
  getVersions: (workflowId: string) => apiClient.get<WorkflowVersion[]>(`/api/workflow-schemes/workflows/${workflowId}/versions`),
};
