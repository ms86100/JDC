import apiClient from './axiosClient';

export interface EpicResponse {
  id: string;
  name: string;
  summary?: string;
  description?: string;
  color?: string;
  leadId?: string;
  leadName?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  linkedIssueId?: string;
  totalStoryPoints?: number;
  completedStoryPoints?: number;
  totalIssueCount?: number;
  completedIssueCount?: number;
  progressPercentage?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateEpicRequest {
  name: string;
  summary?: string;
  description?: string;
  color?: string;
  leadId?: string;
  leadName?: string;
  status?: string;
  linkedIssueId?: string;
  startDate?: string;
  endDate?: string;
}

export interface UpdateEpicRequest extends Partial<CreateEpicRequest> {}

export interface EpicProgressResponse {
  epicId: string;
  totalStoryPoints?: number;
  completedStoryPoints?: number;
  totalIssueCount?: number;
  completedIssueCount?: number;
  progressPercentage?: number;
  recordedAt?: string;
}

export const epicApi = {
  getAll: (params?: { leadId?: string; status?: string }) =>
    apiClient.get<EpicResponse[]>('/epics', { params }),

  getById: (epicId: string) =>
    apiClient.get<EpicResponse>(`/api/epics/${epicId}`),

  create: (data: CreateEpicRequest) =>
    apiClient.post<EpicResponse>('/epics', data),

  update: (epicId: string, data: UpdateEpicRequest) =>
    apiClient.put<EpicResponse>(`/api/epics/${epicId}`, data),

  delete: (epicId: string) =>
    apiClient.delete(`/api/epics/${epicId}`),

  getIssues: (epicId: string) =>
    apiClient.get<string[]>(`/api/epics/${epicId}/issues`),

  addIssue: (epicId: string, issueId: string) =>
    apiClient.post(`/api/epics/${epicId}/issues/${issueId}`),

  removeIssue: (epicId: string, issueId: string) =>
    apiClient.delete(`/api/epics/${epicId}/issues/${issueId}`),

  getProgress: (epicId: string) =>
    apiClient.get<EpicProgressResponse>(`/api/epics/${epicId}/progress`),

  recalculateProgress: (epicId: string) =>
    apiClient.post<EpicResponse>(`/api/epics/${epicId}/progress/recalculate`),

  updateStatus: (epicId: string, status: string) =>
    apiClient.put<EpicResponse>(`/api/epics/${epicId}/status`, { status }),

  getProgressHistory: (epicId: string) =>
    apiClient.get<EpicProgressResponse[]>(`/api/epics/${epicId}/progress/history`),
};
