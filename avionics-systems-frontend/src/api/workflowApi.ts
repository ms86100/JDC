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
  value?: string;
  validatorData?: string;
  functionData?: string;
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

  addCondition: (transitionId: string, data: Record<string, unknown>) =>
    apiClient.post(`/api/workflows/transitions/${transitionId}/conditions`, data),
  addValidator: (transitionId: string, data: Record<string, unknown>) =>
    apiClient.post(`/api/workflows/transitions/${transitionId}/validators`, data),
  addPostFunction: (transitionId: string, data: Record<string, unknown>) =>
    apiClient.post(`/api/workflows/transitions/${transitionId}/post-functions`, data),
  deleteCondition: (transitionId: string, conditionId: string) =>
    apiClient.delete(`/api/workflows/transitions/${transitionId}/conditions/${conditionId}`),
  deleteValidator: (transitionId: string, validatorId: string) =>
    apiClient.delete(`/api/workflows/transitions/${transitionId}/validators/${validatorId}`),
  deletePostFunction: (transitionId: string, functionId: string) =>
    apiClient.delete(`/api/workflows/transitions/${transitionId}/post-functions/${functionId}`),
  updateCondition: (transitionId: string, conditionId: string, data: Record<string, unknown>) =>
    apiClient.put(`/api/workflows/transitions/${transitionId}/conditions/${conditionId}`, data),
  updateValidator: (transitionId: string, validatorId: string, data: Record<string, unknown>) =>
    apiClient.put(`/api/workflows/transitions/${transitionId}/validators/${validatorId}`, data),
  updatePostFunction: (transitionId: string, functionId: string, data: Record<string, unknown>) =>
    apiClient.put(`/api/workflows/transitions/${transitionId}/post-functions/${functionId}`, data),

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
  rollbackToVersion: (workflowId: string, versionNumber: number) =>
    apiClient.post<Workflow>(`/api/workflow-schemes/workflows/${workflowId}/rollback/${versionNumber}`),

  createSchemeDraft: (schemeId: string) =>
    apiClient.post<WorkflowScheme>(`/api/workflow-schemes/${schemeId}/draft`),
  createWorkflowDraftByScheme: (workflowId: string) =>
    apiClient.post(`/api/workflow-schemes/workflows/${workflowId}/draft`),
  getWorkflowDraft: (workflowId: string) => apiClient.get(`/api/workflow-schemes/workflows/${workflowId}/draft`),
  publishDraft: (draftId: string, changeDescription?: string) =>
    apiClient.post<Workflow>(`/api/workflow-schemes/drafts/${draftId}/publish`, null, {
      params: changeDescription ? { changeDescription } : undefined,
    }),
  discardDraft: (draftId: string) => apiClient.post(`/api/workflow-schemes/drafts/${draftId}/discard`),

  lockLayout: (workflowId: string) => apiClient.post(`/api/workflow-schemes/workflows/${workflowId}/layout/lock`),
  unlockLayout: (workflowId: string) => apiClient.post(`/api/workflow-schemes/workflows/${workflowId}/layout/unlock`),

  assignSchemeBulk: (schemeId: string, projectIds: string[]) =>
    apiClient.post<{ schemeId: string; updatedProjects: number }>('/api/workflow-schemes/projects/assign-bulk', {
      schemeId,
      projectIds,
    }),

  createStatusMigration: (data: {
    workflowId: string;
    oldStatusId: string;
    newStatusId: string;
    migrationType?: string;
    userId?: string;
  }) => apiClient.post<WorkflowStatusMigration>('/api/workflow-schemes/migrations', data),
  previewStatusMigration: (migrationId: string, oldStatusId: string, newStatusId: string) =>
    apiClient.post<MigrationPreview>(`/api/workflow-schemes/migrations/${migrationId}/preview`, null, {
      params: { oldStatusId, newStatusId },
    }),
  executeStatusMigration: (migrationId: string) =>
    apiClient.post<WorkflowStatusMigration>(`/api/workflow-schemes/migrations/${migrationId}/execute`),
  getStatusMigration: (migrationId: string) =>
    apiClient.get<WorkflowStatusMigration>(`/api/workflow-schemes/migrations/${migrationId}`),
  cancelStatusMigration: (migrationId: string) =>
    apiClient.post<WorkflowStatusMigration>(`/api/workflow-schemes/migrations/${migrationId}/cancel`),
  retryStatusMigration: (migrationId: string) =>
    apiClient.post<WorkflowStatusMigration>(`/api/workflow-schemes/migrations/${migrationId}/retry`),

  listTransitionScreens: () => apiClient.get<WorkflowTransitionScreen[]>('/api/admin/workflows/screens'),
  getTransitionScreen: (screenId: string) =>
    apiClient.get<WorkflowTransitionScreen>(`/api/admin/workflows/screens/${screenId}`),
  createTransitionScreen: (data: { name: string; description?: string }) =>
    apiClient.post<WorkflowTransitionScreen>('/api/admin/workflows/screens', data),
  updateTransitionScreen: (screenId: string, data: Record<string, unknown>) =>
    apiClient.put<WorkflowTransitionScreen>(`/api/admin/workflows/screens/${screenId}`, data),
  deleteTransitionScreen: (screenId: string) => apiClient.delete(`/api/admin/workflows/screens/${screenId}`),
  assignScreenToTransition: (transitionId: string, screenId: string) =>
    apiClient.post(`/api/admin/workflows/transitions/${transitionId}/screen`, { screenId }),
  removeScreenFromTransition: (transitionId: string) =>
    apiClient.delete(`/api/admin/workflows/transitions/${transitionId}/screen`),

  getAvailableTransitions: (issueId: string, projectId: string) =>
    apiClient.get<{ transitions: AvailableTransition[] }>(
      `/api/workflows/issues/${issueId}/available-transitions`,
      { params: { projectId } },
    ),

  executeBulkTransitions: (data: {
    projectId: string;
    items: Array<{ issueId: string; transitionId: string; comment?: string }>;
  }) =>
    apiClient.post<{
      total: number;
      succeeded: number;
      failed: number;
      results: Array<{ issueId: string; success: boolean; error?: string }>;
    }>('/api/workflows/transitions/execute-bulk', data),
};

export interface WorkflowDefinition {
  type: string;
  name?: string;
  description?: string;
  category?: string;
}

export interface WorkflowStatusMigration {
  id: string;
  workflowId: string;
  oldStatusId: string;
  newStatusId: string;
  status: string;
  totalIssues?: number;
  migratedIssues?: number;
  failedIssues?: number;
}

export interface MigrationPreview {
  migrationId: string;
  affectedIssueCount: number;
  sampleIssues?: Array<{ issueId: string; issueKey: string; currentStatus: string }>;
}

export interface WorkflowTransitionScreen {
  id: string;
  name: string;
  description?: string;
}

export interface AvailableTransition {
  id: string;
  name: string;
  description?: string;
  toStatusId?: string;
  toStatusName?: string;
  hasScreen?: boolean;
  screenFields?: Array<{ fieldId: string; fieldName: string; required: boolean }>;
}
