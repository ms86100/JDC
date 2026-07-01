import apiClient from './axiosClient';

export interface IssueTransitionHistoryEntry {
  id: string;
  issueId: string;
  projectId?: string;
  workflowId?: string;
  transitionId?: string;
  transitionName?: string;
  fromStatusId?: string;
  toStatusId?: string;
  userId?: string;
  comment?: string;
  success?: boolean;
  errorMessage?: string;
  executedAt: string;
}

export const transitionHistoryApi = {
  listByIssue: (issueId: string) =>
    apiClient.get<IssueTransitionHistoryEntry[]>(`/issues/${issueId}/transitions/history`),
};
