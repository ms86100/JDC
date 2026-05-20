import apiClient from './axiosClient';

// Extended interface for workflow status linking
export interface WorkflowStatusLink {
  id: string;
  workflowId: string;
  statusId: string;
  statusName?: string;
  statusCategory?: string;
  statusColor?: string;
  sequence: number;
  createdAt?: string;
}

// Workflow transition with full details
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
  icon?: string;
  type?: string;
  triggerType?: string;
  requiresApproval?: boolean;
  approvalGroupId?: string;
  allowAssigneeOverride?: boolean;
  allowUnassign?: boolean;
  fieldsRequired?: string[];
  fieldsHidden?: string[];
  permissionCheck?: string;
  userGroupIds?: string[];
  allowLoop?: boolean;
  maxLoopCount?: number;
  createdAt?: string;
  updatedAt?: string;
  conditions?: any[];
  validators?: any[];
  postFunctions?: any[];
}

// Base interface (backwards compatible)
export interface WorkflowStatus {
  id: string;
  name: string;
  description?: string;
  category?: string;
  color?: string;
  sequence?: number;
}

export interface WorkflowTransition {
  id: string;
  name: string;
  description?: string;
  fromStatusId: string;
  toStatusId: string;
  displayOrder?: number;
  type?: string;
}

export interface Workflow {
  id: string;
  name: string;
  description: string;
  isDraft: boolean;
  isActive: boolean;
  isSystem: boolean;
  isDefault?: boolean;
  projectId?: string;
  draftOfWorkflowId?: string;
  createdAt?: string;
  updatedAt?: string;
  statusCount?: number;
  transitionCount?: number;
  statuses?: WorkflowStatus[];
  transitions?: WorkflowTransition[];
}

export const workflowApi = {
  // Workflow CRUD
  getAll: () => apiClient.get<Workflow[]>('/api/workflows'),
  getById: (id: string) => apiClient.get<Workflow>(`/api/workflows/${id}`),

  getWorkflowDetail: (id: string) =>
    apiClient.get<{
      workflow: Workflow;
      statuses: WorkflowStatusLink[];
      transitions: WorkflowTransitionDetail[];
      versions: Array<{
        id: string;
        versionNumber: number;
        changeDescription: string;
        changeType: string;
        createdAt: string;
        createdBy?: string;
      }>;
    }>(`/api/workflows/${id}/detail`),
  create: (data: { name: string; description?: string; projectId?: string }) =>
    apiClient.post<Workflow>('/api/workflows', data),
  update: (id: string, data: Partial<Workflow>) =>
    apiClient.put<Workflow>(`/api/workflows/${id}`, data),
  delete: (id: string) => apiClient.delete(`/api/workflows/${id}`),

  // Workflow Status Management
  getWorkflowStatuses: (workflowId: string) =>
    apiClient.get<WorkflowStatusLink[]>(`/api/workflows/${workflowId}/statuses`),
  addStatusToWorkflow: (workflowId: string, statusId: string, sequence?: number) =>
    apiClient.post<WorkflowStatusLink>(`/api/workflows/${workflowId}/statuses`, { statusId, sequence }),
  removeStatusFromWorkflow: (workflowId: string, workflowStatusId: string) =>
    apiClient.delete(`/api/workflows/${workflowId}/statuses/${workflowStatusId}`),
  reorderWorkflowStatuses: (workflowId: string, statusIds: string[]) =>
    apiClient.put<WorkflowStatusLink[]>(`/api/workflows/${workflowId}/statuses/reorder`, statusIds),

  // Transition CRUD
  getAllTransitions: () => apiClient.get<WorkflowTransition[]>('/api/workflows/transitions'),
  getTransitionById: (transitionId: string) =>
    apiClient.get<WorkflowTransition>(`/api/workflows/transitions/${transitionId}`),
  createTransition: (data: {
    workflowId: string;
    name: string;
    fromStatusId: string;
    toStatusId: string;
    requiresApproval?: boolean;
  }) => apiClient.post<WorkflowTransition>('/api/workflows/transitions', data),
  updateTransition: (transitionId: string, data: Partial<WorkflowTransition>) =>
    apiClient.put<WorkflowTransition>(`/api/workflows/transitions/${transitionId}`, data),
  deleteTransition: (transitionId: string) =>
    apiClient.delete(`/api/workflows/transitions/${transitionId}`),
  getTransitionsWithDetails: (workflowId: string) =>
    apiClient.get<WorkflowTransitionDetail[]>(`/api/workflows/${workflowId}/transitions-with-details`),

  // Schemes (correct path)
  getSchemes: () => apiClient.get('/api/workflow-schemes'),
  getScheme: (id: string) => apiClient.get(`/api/workflow-schemes/${id}`),

  executeTransition: (data: {
    issueId: string;
    projectId: string;
    transitionId?: string;
    statusId?: string;
    comment?: string;
    screenInput?: Record<string, unknown>;
  }) => apiClient.post('/api/workflows/transitions/execute', data),

  getAvailableTransitionsForIssue: (issueId: string, projectId: string) =>
    apiClient.get(`/api/workflows/issues/${issueId}/available-transitions`, { params: { projectId } }),

  executeBulkTransitions: (data: {
    projectId: string;
    userId?: string;
    items: Array<{
      issueId: string;
      transitionId?: string;
      statusId?: string;
      comment?: string;
      resolutionId?: string;
      screenInput?: Record<string, unknown>;
    }>;
  }) => apiClient.post('/api/workflows/transitions/execute-bulk', data),

  syncDesignerLayout: (workflowId: string, nodes: Array<{ nodeId: string; positionX: number; positionY: number }>) =>
    apiClient.put(`/api/workflow-schemes/workflows/${workflowId}/layout/positions`, { nodes }),

  getVersions: (workflowId: string) =>
    apiClient.get<
      Array<{
        id: string;
        versionNumber: number;
        changeDescription: string;
        changeType: string;
        createdAt: string;
        createdBy?: string;
      }>
    >(`/api/workflow-schemes/workflows/${workflowId}/versions`),

  getLayout: (workflowId: string) =>
    apiClient.get(`/api/workflow-schemes/workflows/${workflowId}/layout`),

  autoLayout: (workflowId: string) =>
    apiClient.post(`/api/workflow-schemes/workflows/${workflowId}/layout/auto`),

  publishWorkflow: (workflowId: string) =>
    apiClient.post(`/api/admin/workflows/${workflowId}/publish`),
};
