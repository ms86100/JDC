import apiClient from './axiosClient';

export interface WorklogResponse {
  id: string;
  issueId: string;
  authorId: string;
  authorName?: string;
  timeSpentSeconds: number;
  workDescription?: string;
  description?: string;
  startedAt?: string;
  createdAt: string;
  updatedAt?: string;
  timeWorkedFormatted?: string;
}

export interface CreateWorklogRequest {
  issueId?: string;
  timeSpentSeconds: number;
  workDescription?: string;
  startedAt?: string;
  authorId?: string;
}

export interface UpdateWorklogRequest {
  timeSpentSeconds: number;
  workDescription?: string;
  startedAt?: string;
}

export const secondsToMinutes = (seconds: number) => Math.round(seconds / 60);

export const minutesToSeconds = (minutes: number) => minutes * 60;

export const worklogApi = {
  create: (issueId: string, data: Omit<CreateWorklogRequest, 'issueId'>) =>
    apiClient.post<WorklogResponse>(`/api/issues/${issueId}/worklogs`, data),

  getAll: (issueId: string) =>
    apiClient.get<WorklogResponse[]>(`/api/issues/${issueId}/worklogs`),

  getById: (issueId: string, worklogId: string) =>
    apiClient.get<WorklogResponse>(`/api/issues/${issueId}/worklogs/${worklogId}`),

  update: (issueId: string, worklogId: string, data: UpdateWorklogRequest) =>
    apiClient.put<WorklogResponse>(`/api/issues/${issueId}/worklogs/${worklogId}`, data),

  delete: (issueId: string, worklogId: string) =>
    apiClient.delete(`/api/issues/${issueId}/worklogs/${worklogId}`),

  getTotalTime: (issueId: string) =>
    apiClient.get<number>(`/api/issues/${issueId}/worklogs/total`),
};
