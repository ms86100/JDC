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
  // Backend: /api/issues/links (root-level, not nested under issueId)
  create: (issueId: string, data: { destinationIssueId: string; linkType: string }) =>
    apiClient.post<IssueLinkResponse>('/api/issues/links', {
      sourceIssueId: issueId,
      targetIssueId: data.destinationIssueId,
      linkTypeName: data.linkType,
    }),

  getAll: (issueId: string) =>
    apiClient.get<IssueLinkResponse[]>(`/api/issues/links/issue/${issueId}`),

  getOutward: (issueId: string) =>
    apiClient.get<IssueLinkResponse[]>(`/api/issues/links/issue/${issueId}/outward`),

  getInward: (issueId: string) =>
    apiClient.get<IssueLinkResponse[]>(`/api/issues/links/issue/${issueId}/inward`),

  delete: (issueId: string, linkId: string) =>
    apiClient.delete(`/api/issues/links/${linkId}`),

  getLinkTypes: (issueId?: string) =>
    apiClient.get<string[]>('/api/issues/links/types/names'),
};
