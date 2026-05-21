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

export const fieldApi = {
  getDefinitions: () =>
    apiClient.get<FieldDefinitionDto[]>('/api/fields/definitions'),

  getCustomFields: () =>
    apiClient.get<CustomFieldDefinitionDto[]>('/api/fields/custom'),

  mapFields: (sourceFieldKeys: string[]) =>
    apiClient.post<FieldMappingResultDto>('/api/fields/map', sourceFieldKeys),
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
};
