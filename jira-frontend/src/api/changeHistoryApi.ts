import apiClient from './axiosClient';

export interface ChangeItemResponse {
  id?: string;
  changeGroupId?: string;
  fieldType: string;
  field: string;
  oldValue?: string;
  oldString?: string;
  newValue?: string;
  newString?: string;
  createdAt?: string;
}

export interface ChangeHistoryResponse {
  id: string;
  issueId: string;
  authorId: string;
  authorName: string;
  createdAt: string;
  changes: ChangeItemResponse[];
}

export const changeHistoryApi = {
  getByIssue: (issueId: string) =>
    apiClient.get<ChangeHistoryResponse[]>(`/api/issues/${issueId}/history`),

  getById: (issueId: string, changeGroupId: string) =>
    apiClient.get<ChangeHistoryResponse>(`/api/issues/${issueId}/history/${changeGroupId}`),
};
