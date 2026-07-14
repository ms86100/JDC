import apiClient from './axiosClient';

export interface LabelResponse {
  id: string;
  issueId: string;
  name: string;
  createdBy?: string;
  createdAt: string;
}

export interface CreateLabelRequest {
  issueId: string;
  name: string;
}

export const labelApi = {
  add: (issueId: string, name: string) =>
    apiClient.post<LabelResponse>(`/api/issues/${issueId}/labels`, { name }),

  getAll: (issueId: string) =>
    apiClient.get<LabelResponse[]>(`/api/issues/${issueId}/labels`),

  search: (_issueId: string, query: string) =>
    apiClient.get<LabelResponse[]>(`/api/issues/${_issueId}/labels/search`, { params: { query } }),

  remove: (issueId: string, labelName: string) =>
    apiClient.delete(`/api/issues/${issueId}/labels/${encodeURIComponent(labelName)}`),
};
