import apiClient from './axiosClient';

export interface IssueLinkResponse {
  id: string;
  sourceIssueId: string;
  sourceIssueKey?: string;
  destinationIssueId: string;
  destinationIssueKey?: string;
  linkType: string;
  linkTypeLabel: string;
  sequence: number;
  createdAt: string;
}

export interface CreateIssueLinkRequest {
  sourceIssueId?: string;
  destinationIssueId: string;
  targetIssueId?: string;
  linkType: string;
  linkTypeName?: string;
}

export const issueLinkApi = {
  create: (issueId: string, data: { destinationIssueId: string; linkType: string }) =>
    apiClient.post<IssueLinkResponse>(`/api/issues/${issueId}/links`, {
      targetIssueId: data.destinationIssueId,
      linkTypeName: data.linkType,
    }),

  getAll: (issueId: string) =>
    apiClient.get<IssueLinkResponse[]>(`/api/issues/${issueId}/links`),

  getOutward: (issueId: string) =>
    apiClient.get<IssueLinkResponse[]>(`/api/issues/${issueId}/links/outward`),

  getInward: (issueId: string) =>
    apiClient.get<IssueLinkResponse[]>(`/api/issues/${issueId}/links/inward`),

  delete: (issueId: string, linkId: string) =>
    apiClient.delete(`/api/issues/${issueId}/links/${linkId}`),

  getLinkTypes: (issueId?: string) =>
    issueId
      ? apiClient.get<string[]>(`/api/issues/${issueId}/links/types`)
      : apiClient.get<string[]>('/api/issues/links/types'),
};
