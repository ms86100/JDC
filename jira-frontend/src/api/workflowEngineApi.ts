import axiosClient from './axiosClient';

// ========== Workflow Engine Types ==========

export interface WorkflowState {
  id: string;
  name: string;
  description?: string;
  type: 'INITIAL' | 'INTERMEDIATE' | 'FINAL';
  properties?: Record<string, unknown>;
}

export interface WorkflowTransition {
  id: string;
  fromState: string;
  toState: string;
  name: string;
  description?: string;
  conditions?: TransitionCondition[];
  validators?: TransitionValidator[];
  postFunctions?: TransitionPostFunction[];
}

export interface TransitionCondition {
  id: string;
  type: string;
  name?: string;
  configuration?: Record<string, unknown>;
}

export interface TransitionValidator {
  id: string;
  type: string;
  name?: string;
  configuration?: Record<string, unknown>;
}

export interface TransitionPostFunction {
  id: string;
  type: string;
  name?: string;
  configuration?: Record<string, unknown>;
}

export interface TransitionRule {
  id: string;
  name: string;
  priority: number;
  conditions: TransitionCondition[];
  actions?: TransitionPostFunction[];
}

export interface CreateWorkflowDefinitionRequest {
  name: string;
  description?: string;
  projectId: string;
  workflowType: string;
  workflowStepsJson: string;
  transitionRulesJson: string;
  isDefault?: boolean;
}

export interface WorkflowDefinitionResponse {
  id: string;
  name: string;
  description?: string;
  projectId: string;
  workflowType: string;
  workflowStepsJson: string;
  transitionRulesJson: string;
  isDefault: boolean;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowInstanceResponse {
  id: string;
  definitionId: string;
  entityType: string;
  entityId: string;
  currentState: string;
  stateHistoryJson: string;
  initiatedBy: string;
  isCompleted: boolean;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface StateTransitionResponse {
  fromState: string;
  toState: string;
  transitionedBy: string;
  transitionedAt: string;
  comment?: string;
}

export interface WorkflowProgressResponse {
  totalStates: number;
  visitedStates: number;
  percentComplete: number;
  currentState: string;
  isCompleted: boolean;
  elapsedTime: number;
}

export interface ValidationIssue {
  type: 'ERROR' | 'WARNING';
  message: string;
}

export interface ValidationResult {
  errors: string[];
  warnings: string[];
  isValid: boolean;
  hasWarnings: boolean;
}

// ========== Workflow Builder Types ==========

export interface WorkflowBuilderState {
  id: string;
  name: string;
  description?: string;
  stateType: 'INITIAL' | 'INTERMEDIATE' | 'FINAL';
  position: { x: number; y: number };
  color?: string;
  icon?: string;
}

export interface WorkflowBuilderTransition {
  id: string;
  fromStateId: string;
  toStateId: string;
  name: string;
  description?: string;
  conditions: TransitionCondition[];
  isValid: boolean;
  validationMessage?: string;
}

export interface WorkflowBuilderData {
  id?: string;
  name: string;
  description: string;
  projectId: string;
  workflowType: string;
  states: WorkflowBuilderState[];
  transitions: WorkflowBuilderTransition[];
  initialStateId: string;
  finalStateIds: string[];
}

export interface WorkflowVisualizerNode {
  id: string;
  stateId: string;
  label: string;
  stateType: 'INITIAL' | 'INTERMEDIATE' | 'FINAL';
  isActive: boolean;
  isCurrent: boolean;
  position: { x: number; y: number };
}

export interface WorkflowVisualizerEdge {
  id: string;
  fromNodeId: string;
  toNodeId: string;
  label: string;
  isTraversed: boolean;
}

// ========== API Methods ==========

const workflowEngineApi = {
  // Definition Management
  createDefinition: (data: CreateWorkflowDefinitionRequest): Promise<WorkflowDefinitionResponse> =>
    axiosClient.post('/test-workflows/definitions', data).then(r => r.data),

  getDefinitionsByProject: (projectId: string): Promise<WorkflowDefinitionResponse[]> =>
    axiosClient.get('/test-workflows/definitions', { params: { projectId } }).then(r => r.data),

  getDefinitionsByType: (projectId: string, workflowType: string): Promise<WorkflowDefinitionResponse[]> =>
    axiosClient.get('/test-workflows/definitions', { params: { projectId, workflowType } }).then(r => r.data),

  getDefinition: (id: string): Promise<WorkflowDefinitionResponse> =>
    axiosClient.get(`/test-workflows/definitions/${id}`).then(r => r.data),

  getDefaultDefinition: (projectId: string, workflowType: string): Promise<WorkflowDefinitionResponse> =>
    axiosClient.get(`/test-workflows/definitions/default`, { params: { projectId, workflowType } }).then(r => r.data),

  updateDefinition: (id: string, data: Partial<CreateWorkflowDefinitionRequest>): Promise<WorkflowDefinitionResponse> =>
    axiosClient.put(`/test-workflows/definitions/${id}`, data).then(r => r.data),

  deleteDefinition: (id: string): Promise<void> =>
    axiosClient.delete(`/test-workflows/definitions/${id}`).then(r => r.data),

  activateDefinition: (id: string): Promise<WorkflowDefinitionResponse> =>
    axiosClient.post(`/test-workflows/definitions/${id}/activate`).then(r => r.data),

  deactivateDefinition: (id: string): Promise<WorkflowDefinitionResponse> =>
    axiosClient.post(`/test-workflows/definitions/${id}/deactivate`).then(r => r.data),

  // Instance Management
  startWorkflow: (definitionId: string, entityType: string, entityId: string): Promise<WorkflowInstanceResponse> =>
    axiosClient.post('/test-workflows/instances', { definitionId, entityType, entityId }).then(r => r.data),

  transition: (instanceId: string, targetState: string, comment?: string): Promise<WorkflowInstanceResponse> =>
    axiosClient.post(`/test-workflows/instances/${instanceId}/transition`, { targetState, comment }).then(r => r.data),

  getInstance: (id: string): Promise<WorkflowInstanceResponse> =>
    axiosClient.get(`/test-workflows/instances/${id}`).then(r => r.data),

  getActiveInstances: (): Promise<WorkflowInstanceResponse[]> =>
    axiosClient.get('/test-workflows/instances/active').then(r => r.data),

  getInstancesByEntity: (entityType: string, entityId: string): Promise<WorkflowInstanceResponse[]> =>
    axiosClient.get('/test-workflows/instances', { params: { entityType, entityId } }).then(r => r.data),

  getInstancesByDefinition: (definitionId: string): Promise<WorkflowInstanceResponse[]> =>
    axiosClient.get('/test-workflows/instances', { params: { definitionId } }).then(r => r.data),

  cancelWorkflow: (instanceId: string, reason?: string): Promise<WorkflowInstanceResponse> =>
    axiosClient.post(`/test-workflows/instances/${instanceId}/cancel`, { reason }).then(r => r.data),

  reassignWorkflow: (instanceId: string, newAssigneeId: string): Promise<WorkflowInstanceResponse> =>
    axiosClient.post(`/test-workflows/instances/${instanceId}/reassign`, { newAssigneeId }).then(r => r.data),

  // Workflow Engine Queries
  getAvailableTransitions: (instanceId: string): Promise<string[]> =>
    axiosClient.get(`/test-workflows/instances/${instanceId}/available-transitions`).then(r => r.data),

  getAllStates: (definitionId: string): Promise<string[]> =>
    axiosClient.get(`/test-workflows/definitions/${definitionId}/states`).then(r => r.data),

  getWorkflowProgress: (instanceId: string): Promise<WorkflowProgressResponse> =>
    axiosClient.get(`/test-workflows/instances/${instanceId}/progress`).then(r => r.data),

  getStateHistory: (instanceId: string): Promise<StateTransitionResponse[]> =>
    axiosClient.get(`/test-workflows/instances/${instanceId}/history`).then(r => r.data),

  // Validation
  validateDefinition: (definition: Partial<WorkflowDefinitionResponse>): Promise<ValidationResult> =>
    axiosClient.post('/test-workflows/definitions/validate', definition).then(r => r.data),
};

export default workflowEngineApi;