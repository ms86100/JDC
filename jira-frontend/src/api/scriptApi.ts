import apiClient from './axiosClient';

export interface ScriptDefinition {
  id: string;
  name: string;
  description?: string;
  scriptType: 'CONDITION' | 'VALIDATOR' | 'POST_FUNCTION';
  scriptKey: string;
  scriptBody: string;
  version: number;
  isEnabled: boolean;
  createdBy?: string;
  updatedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateScriptRequest {
  name: string;
  description?: string;
  scriptType: string;
  scriptKey: string;
  scriptBody: string;
}

export interface UpdateScriptRequest {
  name?: string;
  description?: string;
  scriptBody?: string;
  isEnabled?: boolean;
  changeSummary?: string;
}

export interface ScriptConsoleRequest {
  scriptBody: string;
  scriptType: string;
  context: Record<string, unknown>;
}

export interface ScriptConsoleResponse {
  success: boolean;
  result: unknown;
  errorMessage?: string;
  executionMs: number;
  consoleOutput?: string;
}

export interface ScriptVersion {
  id: string;
  scriptId: string;
  version: number;
  scriptBody: string;
  changeSummary?: string;
  createdBy?: string;
  createdAt: string;
}

export interface ScriptExecutionLog {
  id: string;
  scriptId: string;
  scriptKey: string;
  scriptType: string;
  executionMode: string;
  issueId?: string;
  projectId?: string;
  userId?: string;
  success: boolean;
  resultValue?: string;
  errorMessage?: string;
  executionMs: number;
  createdAt: string;
}

export interface ScriptSchedule {
  id: string;
  scriptId: string;
  cronExpression: string;
  isEnabled: boolean;
  lastRunAt?: string;
  nextRunAt?: string;
  lastResult?: string;
  lastSuccess?: boolean;
  runCount: number;
  createdAt: string;
}

export interface ScriptListener {
  id: string;
  scriptId: string;
  eventType: string;
  projectFilter?: string;
  issueTypeFilter?: string;
  isEnabled: boolean;
  executionOrder: number;
  createdAt: string;
}

export interface ScriptFieldBehavior {
  id: string;
  scriptId: string;
  screenContext: string;
  projectId?: string;
  issueTypeId?: string;
  isEnabled: boolean;
  executionOrder: number;
  createdAt: string;
}

const BASE = '/api/workflow/scripts';

export const scriptApi = {
  // CRUD
  list: (type?: string) =>
    apiClient.get<ScriptDefinition[]>(BASE, { params: type ? { type } : {} }),

  get: (id: string) =>
    apiClient.get<ScriptDefinition>(`${BASE}/${id}`),

  create: (data: CreateScriptRequest) =>
    apiClient.post<ScriptDefinition>(BASE, data),

  update: (id: string, data: UpdateScriptRequest) =>
    apiClient.put<ScriptDefinition>(`${BASE}/${id}`, data),

  delete: (id: string) =>
    apiClient.delete(`${BASE}/${id}`),

  toggle: (id: string, enabled: boolean) =>
    apiClient.patch<ScriptDefinition>(`${BASE}/${id}/toggle`, null, { params: { enabled } }),

  // Versions
  getVersions: (id: string) =>
    apiClient.get<ScriptVersion[]>(`${BASE}/${id}/versions`),

  revertToVersion: (id: string, version: number) =>
    apiClient.post<ScriptDefinition>(`${BASE}/${id}/revert/${version}`),

  // Execution
  getExecutionLogs: (id: string, page = 0, size = 20) =>
    apiClient.get<{ content: ScriptExecutionLog[] }>(`${BASE}/${id}/executions`, { params: { page, size } }),

  getAllExecutionLogs: (page = 0, size = 20) =>
    apiClient.get<{ content: ScriptExecutionLog[] }>(`${BASE}/executions`, { params: { page, size } }),

  executeConsole: (data: ScriptConsoleRequest) =>
    apiClient.post<ScriptConsoleResponse>(`${BASE}/console`, data),

  // Available for dropdowns
  getAvailable: (type: string) =>
    apiClient.get<ScriptDefinition[]>(`${BASE}/available`, { params: { type } }),

  // Validation
  validate: (scriptBody: string) =>
    apiClient.post<{ valid: boolean; error?: string }>(`${BASE}/validate`, { scriptBody }),

  // Import/Export
  exportScript: (id: string) =>
    apiClient.get<Record<string, unknown>>(`${BASE}/${id}/export`),

  importScript: (data: CreateScriptRequest) =>
    apiClient.post<ScriptDefinition>(`${BASE}/import`, data),

  // Schedules
  getSchedule: (scriptId: string) =>
    apiClient.get<ScriptSchedule>(`${BASE}/${scriptId}/schedule`),

  createSchedule: (scriptId: string, cronExpression: string) =>
    apiClient.post<ScriptSchedule>(`${BASE}/${scriptId}/schedule`, { cronExpression }),

  deleteSchedule: (scriptId: string) =>
    apiClient.delete(`${BASE}/${scriptId}/schedule`),

  toggleSchedule: (scriptId: string) =>
    apiClient.patch<ScriptSchedule>(`${BASE}/${scriptId}/schedule/toggle`),

  // Listeners
  getListeners: (scriptId: string) =>
    apiClient.get<ScriptListener[]>(`${BASE}/${scriptId}/listeners`),

  createListener: (scriptId: string, data: { eventType: string; projectFilter?: string; issueTypeFilter?: string }) =>
    apiClient.post<ScriptListener>(`${BASE}/${scriptId}/listeners`, data),

  deleteListener: (listenerId: string) =>
    apiClient.delete(`${BASE}/listeners/${listenerId}`),

  // Field Behaviors
  getFieldBehaviors: (scriptId: string) =>
    apiClient.get<ScriptFieldBehavior[]>(`${BASE}/${scriptId}/field-behaviors`),

  createFieldBehavior: (scriptId: string, data: { screenContext: string; projectId?: string; issueTypeId?: string }) =>
    apiClient.post<ScriptFieldBehavior>(`${BASE}/${scriptId}/field-behaviors`, data),

  deleteFieldBehavior: (behaviorId: string) =>
    apiClient.delete(`${BASE}/field-behaviors/${behaviorId}`),

  evaluateFieldBehaviors: (data: { screenContext: string; projectId?: string; issueTypeId?: string; issueData?: Record<string, unknown>; userId?: string }) =>
    apiClient.post<{ fields: Array<{ fieldName: string; visible?: boolean; required?: boolean; readOnly?: boolean; defaultValue?: unknown }> }>(`${BASE}/field-behaviors/evaluate`, data),

  // Execute by key (for external integrations)
  executeByKey: (scriptKey: string, context?: Record<string, unknown>) =>
    apiClient.post<ScriptConsoleResponse>(`${BASE}/execute-by-key/${scriptKey}`, context || {}),
};
