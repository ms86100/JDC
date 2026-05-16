import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import axios from '../../api/axiosClient';

export interface FieldDefinition {
  id: string;
  fieldKey: string;
  displayName: string;
  description?: string;
  fieldType: FieldType;
  renderer: RendererType;
  screenRegion: ScreenRegion;
  pluginSource?: string;
  searchable: boolean;
  sortable: boolean;
  filterable: boolean;
  required: boolean;
  readOnly: boolean;
  hidden: boolean;
  custom: boolean;
  builtIn: boolean;
  deprecated: boolean;
  schemaDefinition?: Record<string, any>;
  visibilityRules?: Record<string, any>;
  validationRules?: Record<string, any>;
  options?: FieldOption[];
  defaultValue?: string;
}

export interface FieldOption {
  value: string;
  label: string;
  order: number;
  color?: string;
  disabled: boolean;
}

export type FieldType =
  | 'TEXT' | 'TEXTAREA' | 'RICHTEXT' | 'NUMBER' | 'DATE' | 'DATETIME' | 'TIME'
  | 'SINGLE_SELECT' | 'MULTI_SELECT' | 'CHECKBOX' | 'RADIO'
  | 'USER' | 'GROUP' | 'PROJECT' | 'ISSUE_TYPE' | 'STATUS' | 'PRIORITY' | 'RESOLUTION'
  | 'COMPONENT' | 'VERSION' | 'LABEL' | 'SECURITY_LEVEL'
  | 'URL' | 'EMAIL' | 'CURRENCY' | 'DURATION'
  | 'SPRINT' | 'EPIC' | 'PARENT_ISSUE' | 'SUBTASK'
  | 'VOTES' | 'WATCHERS' | 'ATTACHMENT' | 'COMMENT' | 'WORKLOG'
  | 'CUSTOM' | 'UNKNOWN';

export type RendererType =
  | 'TEXT' | 'TEXTAREA' | 'RICHTEXT' | 'SELECT' | 'MULTI_SELECT'
  | 'USER_PICKER' | 'GROUP_PICKER' | 'PROJECT_PICKER' | 'DATETIME_PICKER'
  | 'NUMBER' | 'SLIDER' | 'RADIO' | 'CHECKBOX' | 'LABEL_EDITOR'
  | 'CURRENCY' | 'DURATION' | 'URL_LINK' | 'EMAIL_LINK' | 'SECURITY_LEVEL'
  | 'SPRINT_SELECTOR' | 'EPIC_LINK' | 'VOTES' | 'WATCHERS'
  | 'ATTACHMENT_UPLOAD' | 'READ_ONLY' | 'CUSTOM';

export type ScreenRegion =
  | 'HEADER' | 'LEFT_PRIMARY' | 'LEFT_DESCRIPTION' | 'LEFT_ACTIVITY'
  | 'SIDEBAR' | 'SIDEBAR_PEOPLE' | 'SIDEBAR_DETAILS' | 'SIDEBAR_TIME'
  | 'SIDEBAR_AGILE' | 'SIDEBAR_DATES' | 'SIDEBAR_VERSIONS'
  | 'MODAL' | 'POPOVER';

interface FieldDefinitionContextType {
  fieldDefinitions: FieldDefinition[];
  getFieldByKey: (key: string) => FieldDefinition | undefined;
  getFieldsByRegion: (region: ScreenRegion) => FieldDefinition[];
  getFieldsByType: (type: FieldType) => FieldDefinition[];
  getCustomFields: () => FieldDefinition[];
  getBuiltInFields: () => FieldDefinition[];
  loading: boolean;
  error: string | null;
  refreshFields: () => Promise<void>;
  createCustomField: (field: Partial<FieldDefinition>) => Promise<FieldDefinition>;
  updateField: (id: string, updates: Partial<FieldDefinition>) => Promise<void>;
}

const FieldDefinitionContext = createContext<FieldDefinitionContextType | undefined>(undefined);

export function FieldDefinitionProvider({ children }: { children: ReactNode }) {
  const [fieldDefinitions, setFieldDefinitions] = useState<FieldDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchFieldDefinitions = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await axios.get('/api/fields/definitions');
      setFieldDefinitions(response.data);
    } catch (err: any) {
      setError(err.message || 'Failed to load field definitions');
      console.error('Error loading field definitions:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFieldDefinitions();
  }, []);

  const getFieldByKey = (key: string): FieldDefinition | undefined => {
    return fieldDefinitions.find(f => f.fieldKey === key);
  };

  const getFieldsByRegion = (region: ScreenRegion): FieldDefinition[] => {
    return fieldDefinitions.filter(f => f.screenRegion === region && !f.hidden && !f.deprecated);
  };

  const getFieldsByType = (type: FieldType): FieldDefinition[] => {
    return fieldDefinitions.filter(f => f.fieldType === type);
  };

  const getCustomFields = (): FieldDefinition[] => {
    return fieldDefinitions.filter(f => f.custom && !f.hidden && !f.deprecated);
  };

  const getBuiltInFields = (): FieldDefinition[] => {
    return fieldDefinitions.filter(f => f.builtIn && !f.hidden && !f.deprecated);
  };

  const createCustomField = async (field: Partial<FieldDefinition>): Promise<FieldDefinition> => {
    const response = await axios.post('/api/fields/custom', {
      name: field.displayName,
      description: field.description,
      type: mapFieldTypeToCustomFieldType(field.fieldType || 'TEXT'),
      config: field.schemaDefinition,
      options: field.options?.map(o => ({
        value: o.value,
        label: o.label,
        color: o.color
      }))
    });
    await fetchFieldDefinitions();
    return response.data;
  };

  const updateField = async (id: string, updates: Partial<FieldDefinition>) => {
    await axios.put(`/api/fields/definitions/${id}`, {
      displayName: updates.displayName,
      description: updates.description,
      renderer: updates.renderer,
      screenRegion: updates.screenRegion,
      searchable: updates.searchable,
      sortable: updates.sortable,
      filterable: updates.filterable,
      required: updates.required,
      readOnly: updates.readOnly,
      hidden: updates.hidden,
      deprecated: updates.deprecated,
      schemaDefinition: updates.schemaDefinition,
      validationRules: updates.validationRules
    });
    await fetchFieldDefinitions();
  };

  return (
    <FieldDefinitionContext.Provider
      value={{
        fieldDefinitions,
        getFieldByKey,
        getFieldsByRegion,
        getFieldsByType,
        getCustomFields,
        getBuiltInFields,
        loading,
        error,
        refreshFields: fetchFieldDefinitions,
        createCustomField,
        updateField
      }}
    >
      {children}
    </FieldDefinitionContext.Provider>
  );
}

export function useFieldDefinitions() {
  const context = useContext(FieldDefinitionContext);
  if (!context) {
    throw new Error('useFieldDefinitions must be used within a FieldDefinitionProvider');
  }
  return context;
}

function mapFieldTypeToCustomFieldType(type: FieldType): string {
  const mapping: Record<FieldType, string> = {
    TEXT: 'com.atlassian.jira.plugin.system.customfieldtypes:textfield',
    TEXTAREA: 'com.atlassian.jira.plugin.system.customfieldtypes:textarea',
    RICHTEXT: 'com.atlassian.jira.plugin.system.customfieldtypes:textarea',
    DATE: 'com.atlassian.jira.plugin.system.customfieldtypes:datepicker',
    DATETIME: 'com.atlassian.jira.plugin.system.customfieldtypes:datetime',
    NUMBER: 'com.atlassian.jira.plugin.system.customfieldtypes:number',
    SINGLE_SELECT: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    MULTI_SELECT: 'com.atlassian.jira.plugin.system.customfieldtypes:multiselect',
    RADIO: 'com.atlassian.jira.plugin.system.customfieldtypes:radiobuttons',
    CHECKBOX: 'com.atlassian.jira.plugin.system.customfieldtypes:checkbox',
    USER: 'com.atlassian.jira.plugin.system.customfieldtypes:userpicker',
    PROJECT: 'com.atlassian.jira.plugin.system.customfieldtypes:projectpicker',
    VERSION: 'com.atlassian.jira.plugin.system.customfieldtypes:versionpicker',
    LABEL: 'com.atlassian.jira.plugin.system.customfieldtypes:labels',
    URL: 'com.atlassian.jira.plugin.system.customfieldtypes:url',
    EMAIL: 'com.atlassian.jira.plugin.system.customfieldtypes:email',
    CUSTOM: 'com.atlassian.jira.plugin.system.customfieldtypes:textfield',
    UNKNOWN: 'com.atlassian.jira.plugin.system.customfieldtypes:textfield',
    TIME: 'com.atlassian.jira.plugin.system.customfieldtypes:textfield',
    GROUP: 'com.atlassian.jira.plugin.system.customfieldtypes:userpicker',
    ISSUE_TYPE: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    STATUS: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    PRIORITY: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    RESOLUTION: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    COMPONENT: 'com.atlassian.jira.plugin.system.customfieldtypes:multiselect',
    SECURITY_LEVEL: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    CURRENCY: 'com.atlassian.jira.plugin.system.customfieldtypes:number',
    DURATION: 'com.atlassian.jira.plugin.system.customfieldtypes:number',
    SPRINT: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    EPIC: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    PARENT_ISSUE: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    SUBTASK: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    VOTES: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    WATCHERS: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    ATTACHMENT: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    COMMENT: 'com.atlassian.jira.plugin.system.customfieldtypes:select',
    WORKLOG: 'com.atlassian.jira.plugin.system.customfieldtypes:select'
  };
  return mapping[type] || mapping.UNKNOWN;
}

export default FieldDefinitionContext;