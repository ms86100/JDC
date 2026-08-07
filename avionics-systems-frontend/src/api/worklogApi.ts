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
  visibility?: string;
  visibilityGroupId?: string;
}

export type RemainingEstimateStrategy = 'AUTO' | 'LEAVE' | 'SET' | 'REDUCE' | 'INCREASE';

export interface CreateWorklogRequest {
  issueId?: string;
  timeSpentSeconds: number;
  workDescription?: string;
  startedAt?: string;
  authorId?: string;
  adjustEstimate?: RemainingEstimateStrategy;
  adjustmentSeconds?: number;
  visibility?: string;
  visibilityGroupId?: string;
}

export interface UpdateWorklogRequest {
  timeSpentSeconds: number;
  workDescription?: string;
  startedAt?: string;
  adjustEstimate?: RemainingEstimateStrategy;
  adjustmentSeconds?: number;
  visibility?: string;
  visibilityGroupId?: string;
}

export interface AggregateTimeResponse {
  aggregateEstimate: number;
  aggregateTimeSpent: number;
  aggregateRemaining: number;
}

export const parseTimeInput = (input: string): number | null => {
  if (!input || !input.trim()) return null;
  const trimmed = input.trim().toLowerCase();

  const num = Number(trimmed);
  if (!isNaN(num) && num > 0) return num * 60;

  let totalSeconds = 0;
  let matched = false;

  const weekMatch = trimmed.match(/(\d+(?:\.\d+)?)\s*w/);
  if (weekMatch) { totalSeconds += parseFloat(weekMatch[1]) * 5 * 8 * 3600; matched = true; }

  const dayMatch = trimmed.match(/(\d+(?:\.\d+)?)\s*d/);
  if (dayMatch) { totalSeconds += parseFloat(dayMatch[1]) * 8 * 3600; matched = true; }

  const hourMatch = trimmed.match(/(\d+(?:\.\d+)?)\s*h/);
  if (hourMatch) { totalSeconds += parseFloat(hourMatch[1]) * 3600; matched = true; }

  const minuteMatch = trimmed.match(/(\d+(?:\.\d+)?)\s*m/);
  if (minuteMatch) { totalSeconds += parseFloat(minuteMatch[1]) * 60; matched = true; }

  return matched && totalSeconds > 0 ? Math.round(totalSeconds) : null;
};

export const formatTimeDisplay = (seconds: number | null | undefined): string => {
  if (!seconds || seconds <= 0) return 'None';
  const weeks = Math.floor(seconds / (5 * 8 * 3600));
  let remainder = seconds % (5 * 8 * 3600);
  const days = Math.floor(remainder / (8 * 3600));
  remainder = remainder % (8 * 3600);
  const hours = Math.floor(remainder / 3600);
  remainder = remainder % 3600;
  const minutes = Math.floor(remainder / 60);

  const parts: string[] = [];
  if (weeks > 0) parts.push(`${weeks}w`);
  if (days > 0) parts.push(`${days}d`);
  if (hours > 0) parts.push(`${hours}h`);
  if (minutes > 0) parts.push(`${minutes}m`);
  return parts.length > 0 ? parts.join(' ') : '0m';
};

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

  delete: (issueId: string, worklogId: string, adjustEstimate: RemainingEstimateStrategy = 'AUTO', adjustmentSeconds?: number) => {
    const params = new URLSearchParams({ adjustEstimate });
    if (adjustmentSeconds !== undefined) params.set('adjustmentSeconds', String(adjustmentSeconds));
    return apiClient.delete(`/api/issues/${issueId}/worklogs/${worklogId}?${params.toString()}`);
  },

  getTotalTime: (issueId: string) =>
    apiClient.get<number>(`/api/issues/${issueId}/worklogs/total`),

  getAggregateTime: (issueId: string) =>
    apiClient.get<AggregateTimeResponse>(`/api/issues/${issueId}/worklogs/aggregate`),
};
