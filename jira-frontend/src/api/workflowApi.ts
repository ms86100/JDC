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
};
