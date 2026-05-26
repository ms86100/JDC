import apiClient from './axiosClient';



/** /api/admin/workflows/* — workflow administration API. */

export const workflowAdminApi = {

  list: (params?: { status?: string; name?: string }) =>

    apiClient.get<unknown[]>('/admin/workflows', { params }),



  get: (workflowId: string) => apiClient.get<Record<string, unknown>>(`/api/admin/workflows/${workflowId}`),



  create: (payload: Record<string, unknown>) =>

    apiClient.post<Record<string, unknown>>('/admin/workflows', payload),



  update: (workflowId: string, payload: Record<string, unknown>) =>

    apiClient.put<Record<string, unknown>>(`/api/admin/workflows/${workflowId}`, payload),



  delete: (workflowId: string) => apiClient.delete(`/api/admin/workflows/${workflowId}`),



  publish: (workflowId: string) =>

    apiClient.post<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/publish`),



  createDraft: (workflowId: string) =>

    apiClient.post<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/draft`),



  clone: (workflowId: string, newName: string) =>

    apiClient.post<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/clone`, { newName }),



  exportWorkflow: (workflowId: string) =>

    apiClient.post<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/export`),



  importWorkflow: (payload: Record<string, unknown>) =>

    apiClient.post<Record<string, unknown>>('/admin/workflows/import', payload),



  validate: (workflowId: string) =>

    apiClient.post<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/validate`),



  migrate: (workflowId: string, body: Record<string, unknown>) =>

    apiClient.post<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/migrate`, body),



  migrationPreview: (workflowId: string) =>

    apiClient.get<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/migration-preview`),



  usage: (workflowId: string) =>

    apiClient.get<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/usage`),



  transitionStats: (workflowId: string, params?: { startDate?: string; endDate?: string }) =>

    apiClient.get<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/transition-stats`, {

      params,

    }),



  getVersions: (workflowId: string) =>

    apiClient.get<unknown[]>(`/api/admin/workflows/${workflowId}/versions`),



  getVersion: (workflowId: string, versionNumber: number) =>

    apiClient.get<Record<string, unknown>>(

      `/api/admin/workflows/${workflowId}/versions/${versionNumber}`,

    ),



  compareVersions: (workflowId: string, v1: number, v2: number) =>

    apiClient.get<Record<string, unknown>>(`/api/admin/workflows/${workflowId}/compare`, {

      params: { v1, v2 },

    }),



  revertVersion: (workflowId: string, versionNumber: number) =>

    apiClient.post<Record<string, unknown>>(

      `/api/admin/workflows/${workflowId}/versions/${versionNumber}/revert`,

    ),



  auditLog: (workflowId: string, params?: { limit?: number; action?: string; page?: number; size?: number }) =>

    apiClient.get<unknown[]>(`/api/admin/workflows/${workflowId}/audit-log`, { params }),



  globalAuditLog: (params?: {

    limit?: number;

    action?: string;

    userId?: string;

    page?: number;

    size?: number;

  }) => apiClient.get<unknown[]>('/admin/workflows/audit-log', { params }),



  listSchemes: () => apiClient.get<unknown[]>('/admin/workflows/schemes'),



  getScheme: (schemeId: string) =>

    apiClient.get<Record<string, unknown>>(`/api/admin/workflows/schemes/${schemeId}`),



  createScheme: (data: Record<string, unknown>) =>

    apiClient.post<Record<string, unknown>>('/admin/workflows/schemes', data),



  updateScheme: (schemeId: string, data: Record<string, unknown>) =>

    apiClient.put<Record<string, unknown>>(`/api/admin/workflows/schemes/${schemeId}`, data),



  deleteScheme: (schemeId: string) => apiClient.delete(`/api/admin/workflows/schemes/${schemeId}`),



  assignSchemeMapping: (schemeId: string, mapping: Record<string, unknown>) =>

    apiClient.post<Record<string, unknown>>(

      `/api/admin/workflows/schemes/${schemeId}/mappings`,

      mapping,

    ),



  listScreens: (screenType?: string) =>

    apiClient.get<unknown[]>('/admin/workflows/screens', {

      params: screenType ? { screenType } : undefined,

    }),



  getScreen: (screenId: string) =>

    apiClient.get<Record<string, unknown>>(`/api/admin/workflows/screens/${screenId}`),



  createScreen: (data: Record<string, unknown>) =>

    apiClient.post<Record<string, unknown>>('/admin/workflows/screens', data),



  updateScreen: (screenId: string, data: Record<string, unknown>) =>

    apiClient.put<Record<string, unknown>>(`/api/admin/workflows/screens/${screenId}`, data),



  deleteScreen: (screenId: string) => apiClient.delete(`/api/admin/workflows/screens/${screenId}`),



  conditionDefinitions: () =>

    apiClient.get<unknown[]>('/admin/workflows/conditions/definitions'),



  validatorDefinitions: () =>

    apiClient.get<unknown[]>('/admin/workflows/validators/definitions'),



  postFunctionDefinitions: () =>

    apiClient.get<unknown[]>('/admin/workflows/post-functions/definitions'),

};


