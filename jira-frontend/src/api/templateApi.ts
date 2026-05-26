import apiClient from './axiosClient';

export interface TemplateCapability {
  key: string;
  label: string;
  group: string;
}

export interface CatalogTemplate {
  id: string;
  typeId: string;
  typeName: string;
  name: string;
  description: string;
  shortDescription?: string;
  icon: string;
  iconEmoji?: string;
  color: string;
  previewAccent?: string;
  categoryKey?: string;
  categoryName?: string;
  templateType?: string;
  workflowType?: string;
  workflowTypeLabel?: string;
  useCases?: string;
  instructions?: string;
  recommended?: boolean;
  projectTypeCategory?: 'COMPANY_MANAGED' | 'TEAM_MANAGED';
  defaultAssigneeType: string;
  allowIssueCreation: boolean;
  sortOrder: number;
  capabilities: TemplateCapability[];
}

export interface TemplateCategoryCatalog {
  categoryKey: string;
  name: string;
  description: string;
  icon: string;
  iconEmoji: string;
  sortOrder: number;
  templates: CatalogTemplate[];
}

export interface TemplateCatalog {
  categories: TemplateCategoryCatalog[];
  recommended: CatalogTemplate[];
  recentlyUsed: CatalogTemplate[];
}

export interface TemplateWorkflowStatus {
  id: string;
  statusName: string;
  statusKey: string;
  statusColor: string;
  statusCategory: string;
  sequence: number;
  description?: string;
  icon?: string;
}

export interface TemplateIssueType {
  id: string;
  issueTypeName: string;
  issueTypeIcon?: string;
  isDefault: boolean;
  isSubtask: boolean;
  sequence: number;
}

export interface TemplateWithWorkflow {
  id: string;
  name: string;
  description: string;
  icon: string;
  iconEmoji?: string;
  color: string;
  category?: string;
  templateType?: string;
  workflowType?: string;
  instructions?: string;
  issueTypes: TemplateIssueType[];
  workflowStatuses: TemplateWorkflowStatus[];
  workflowTransitions: { transitionName: string; fromStatusKey: string; toStatusKey: string }[];
  issueTypeScheme?: { id: string; name: string };
  workflowScheme?: { id: string; name: string };
  permissionScheme?: { id: string; name: string };
  notificationScheme?: { id: string; name: string };
  screenScheme?: { id: string; name: string };
}

export const templateApi = {
  getCatalog: () => apiClient.get<TemplateCatalog>('/templates/catalog'),
  getTemplateWithWorkflow: (templateId: string) =>
    apiClient.get<TemplateWithWorkflow>(`/api/templates/${templateId}/workflow`),
  searchCatalog: (query: string, catalog: TemplateCatalog) => {
    const q = query.trim().toLowerCase();
    if (!q) return catalog;

    const filterTemplate = (t: CatalogTemplate) =>
      t.name.toLowerCase().includes(q) ||
      t.description?.toLowerCase().includes(q) ||
      t.shortDescription?.toLowerCase().includes(q) ||
      t.workflowTypeLabel?.toLowerCase().includes(q) ||
      t.capabilities?.some((c) => c.label.toLowerCase().includes(q));

    return {
      ...catalog,
      categories: catalog.categories
        .map((cat) => ({
          ...cat,
          templates: cat.templates.filter(filterTemplate),
        }))
        .filter((cat) => cat.templates.length > 0),
      recommended: catalog.recommended.filter(filterTemplate),
      recentlyUsed: catalog.recentlyUsed.filter(filterTemplate),
    };
  },
};
