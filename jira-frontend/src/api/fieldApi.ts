import apiClient from './axiosClient';

export interface FieldDefinitionDto {
  id: string;
  fieldKey: string;
  displayName: string;
  description?: string;
  fieldType: string;
  renderer?: string;
  required?: boolean;
  readOnly?: boolean;
  hidden?: boolean;
  custom?: boolean;
  deprecated?: boolean;
}

export interface CustomFieldDefinitionDto {
  id: string;
  name: string;
  description?: string;
  type: string;
  fieldKey: string;
  enabled?: boolean;
}

export interface ServerFieldMappingDto {
  sourceKey: string;
  targetKey: string;
  confidence: string;
  strategy: string;
  pluginSource?: string;
}

export interface FieldMappingResultDto {
  mappings: ServerFieldMappingDto[];
  unmappedFields?: ServerFieldMappingDto[];
  mappedFields?: number;
  unmappedFieldsCount?: number;
  typeWarnings?: string[];
}

export interface CreateCustomFieldRequest {
  name: string;
  description?: string;
  type: string;
  searcherKey?: string;
  rendererKey?: string;
  config?: Record<string, unknown>;
  projectIds?: string[];
}

export interface UpdateCustomFieldRequest {
  name?: string;
  description?: string;
  type?: string;
  enabled?: boolean;
}

export const fieldApi = {
  getDefinitions: () =>
    apiClient.get<FieldDefinitionDto[]>('/api/fields/definitions'),

  getCustomFields: () =>
    apiClient.get<CustomFieldDefinitionDto[]>('/api/fields/custom'),

  getCustomField: (id: string) =>
    apiClient.get<CustomFieldDefinitionDto>(`/api/fields/custom/${id}`),

  createCustomField: (body: CreateCustomFieldRequest, userId?: string) =>
    apiClient.post<CustomFieldDefinitionDto>('/api/fields/custom', body, {
      headers: userId ? { 'X-User-Id': userId } : undefined,
    }),

  updateCustomField: (id: string, body: UpdateCustomFieldRequest) =>
    apiClient.put<CustomFieldDefinitionDto>(`/api/fields/custom/${id}`, body),

  deleteCustomField: (id: string) =>
    apiClient.delete(`/api/fields/custom/${id}`),

  ensureProjectFieldScheme: (projectId: string, fieldKeys?: string[]) =>
    apiClient.post<{ projectId: string; fieldsAligned: number; status: string }>(
      `/api/fields/schemes/projects/${projectId}/ensure-fields`,
      fieldKeys ?? null
    ),

  getScreenConfiguration: (screenType = 'issue') =>
    apiClient.get(`/api/fields/screens/configuration`, { params: { screenType } }),

  mapFields: (sourceFieldKeys: string[]) =>
    apiClient.post<FieldMappingResultDto>('/api/fields/map', sourceFieldKeys),

  getIssueFieldValues: (issueId: string) =>
    apiClient.get<{
      issueId: string;
      issueKey?: string;
      customFields?: Record<string, unknown>;
      standardFields?: Record<string, unknown>;
      allFieldValues?: Array<{ fieldKey: string; fieldDisplayName?: string; value: unknown }>;
    }>(`/api/fields/issues/${encodeURIComponent(issueId)}/values`),

  getVisibleIssueFields: (
    issueIdOrKey: string,
    params?: { screen?: string; projectId?: string; issueTypeId?: string },
  ) =>
    apiClient.get<IssueVisibleFieldsDto>(
      `/api/fields/issues/${encodeURIComponent(issueIdOrKey)}/visible`,
      { params },
    ),
};

export interface VisibleFieldDto {
  fieldKey: string;
  displayName: string;
  fieldType?: string;
  renderer?: string;
  value: unknown;
  required?: boolean;
  readOnly?: boolean;
  custom?: boolean;
  displayOrder?: number;
}

export interface IssueVisibleFieldsDto {
  issueId: string;
  issueKey?: string;
  projectId?: string;
  issueTypeId?: string;
  screenType?: string;
  fields: VisibleFieldDto[];
  totalCount: number;
}

export interface BoardCardLayoutDto {
  boardId: string;
  projectId?: string;
  eligibleFields: Array<{
    fieldKey: string;
    displayName: string;
    fieldType?: string;
    custom?: boolean;
  }>;
  selectedFields: Array<{
    fieldKey: string;
    displayName: string;
    displayOrder: number;
    position?: string;
    visible?: boolean;
  }>;
}

export interface DashboardGadgetDto {
  dashboardKey: string;
  gadgetKey: string;
  configuredFields: Array<{
    fieldKey: string;
    displayName: string;
    chartType?: string;
    displayOrder: number;
    enabled?: boolean;
  }>;
  eligibleFields: Array<{
    fieldKey: string;
    displayName: string;
    fieldType?: string;
    supportsChart?: boolean;
    supportsFilter?: boolean;
  }>;
  statistics?: Record<string, unknown>;
}

export const boardFieldApi = {
  getCardLayout: (boardId: string, projectId?: string) =>
    apiClient.get<BoardCardLayoutDto>(`/api/fields/boards/${boardId}/card-layout`, {
      params: projectId ? { projectId } : undefined,
    }),

  saveCardLayout: (
    boardId: string,
    body: { projectId?: string; fields: Array<{ fieldKey: string; displayOrder?: number }> },
  ) => apiClient.put<BoardCardLayoutDto>(`/api/fields/boards/${boardId}/card-layout`, body),

  batchIssueFieldValues: (body: {
    issueIds: string[];
    fieldKeys: string[];
    projectId?: string;
  }) =>
    apiClient.post<{ valuesByIssue: Record<string, VisibleFieldDto[]> }>(
      '/api/fields/boards/issues/visible-batch',
      body,
    ),
};

export const dashboardFieldApi = {
  listGadgets: () => apiClient.get<string[]>('/api/fields/dashboard/gadgets'),

  getGadget: (gadgetKey: string, params?: { dashboardKey?: string; projectId?: string }) =>
    apiClient.get<DashboardGadgetDto>(`/api/fields/dashboard/gadgets/${gadgetKey}`, { params }),

  saveGadget: (
    gadgetKey: string,
    body: {
      dashboardKey?: string;
      gadgetKey?: string;
      fields: Array<{ fieldKey: string; chartType?: string; displayOrder?: number }>;
    },
    projectId?: string,
  ) =>
    apiClient.put<DashboardGadgetDto>(`/api/fields/dashboard/gadgets/${gadgetKey}`, body, {
      params: projectId ? { projectId } : undefined,
    }),
};

export interface OptionMappingDto {
  sourceFieldKey: string;
  sourceOptionValue: string;
  targetFieldKey: string;
  targetOptionValue: string;
}

export const migrationMappingApi = {
  saveSessionFieldDefaults: (sessionId: string, defaults: Record<string, unknown>) =>
    apiClient.patch(`/api/migration/mapping-engine/sessions/${sessionId}/field-defaults`, defaults),

  saveSessionWorkflowMappings: (sessionId: string, mappings: Record<string, unknown>) =>
    apiClient.put(`/api/migration/mapping-engine/sessions/${sessionId}/workflow-status-mappings`, mappings),

  resolveUsers: (jobId: string, sourceIdentifiers: string[]) =>
    apiClient.post(`/api/migration/mapping-engine/jobs/${jobId}/resolve-users`, sourceIdentifiers),

  getSessionOptionMappings: (sessionId: string) =>
    apiClient.get<OptionMappingDto[]>(`/api/migration/mapping-engine/sessions/${sessionId}/option-mappings`),

  saveSessionOptionMappings: (sessionId: string, mappings: OptionMappingDto[]) =>
    apiClient.put<OptionMappingDto[]>(
      `/api/migration/mapping-engine/sessions/${sessionId}/option-mappings`,
      mappings,
    ),

  getJobOptionMappings: (jobId: string) =>
    apiClient.get<OptionMappingDto[]>(`/api/migration/mapping-engine/jobs/${jobId}/option-mappings`),

  saveJobOptionMappings: (jobId: string, mappings: OptionMappingDto[]) =>
    apiClient.put<OptionMappingDto[]>(`/api/migration/mapping-engine/jobs/${jobId}/option-mappings`, mappings),
};
