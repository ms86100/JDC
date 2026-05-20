import apiClient from './axiosClient';

export interface IssueTypeScheme {
  id: string;
  name: string;
  description?: string;
  defaultIssueType?: string;
  issueTypeIdList?: string[];
  projectCount?: number;
  isDefault?: boolean;
}

export function schemesForIssueType(
  schemes: IssueTypeScheme[] | undefined,
  issueTypeId: string
): IssueTypeScheme[] {
  if (!schemes) return [];
  return schemes.filter((s) => (s.issueTypeIdList ?? []).includes(issueTypeId));
}

export interface SchemeProjectAssignment {
  id: string;
  projectKey: string;
  name: string;
  status: string;
  assigned: boolean;
  currentSchemeId?: string;
  currentSchemeName?: string;
}

export const issueTypeSchemeApi = {
  list: () => apiClient.get<IssueTypeScheme[]>('/api/admin/issues/issue-type-schemes'),
  get: (id: string) => apiClient.get<IssueTypeScheme>(`/api/admin/issues/issue-type-schemes/${id}`),
  create: (data: {
    name: string;
    description?: string;
    issueTypeIds: string[];
    defaultIssueType?: string;
  }) => apiClient.post<IssueTypeScheme>('/api/admin/issues/issue-type-schemes', data),
  update: (
    id: string,
    data: {
      name?: string;
      description?: string;
      issueTypeIds?: string[];
      defaultIssueType?: string;
    }
  ) => apiClient.put<IssueTypeScheme>(`/api/admin/issues/issue-type-schemes/${id}`, data),
  delete: (id: string) => apiClient.delete(`/api/admin/issues/issue-type-schemes/${id}`),
  listProjects: (schemeId: string) =>
    apiClient.get<SchemeProjectAssignment[]>(`/api/admin/issues/issue-type-schemes/${schemeId}/projects`),
  assignProjects: (schemeId: string, projectIds: string[]) =>
    apiClient.put<SchemeProjectAssignment[]>(`/api/admin/issues/issue-type-schemes/${schemeId}/projects`, {
      projectIds,
    }),
};
