import apiClient from './axiosClient';

export interface SprintResponse {
  id: string;
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  status: 'PLANNING' | 'ACTIVE' | 'COMPLETED';
  projectId: string;
  createdAt: string;
  updatedAt: string;
  issueCount?: number;
  completedIssueCount?: number;
}

export interface CreateSprintRequest {
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  projectId: string;
}

export interface UpdateSprintRequest {
  name?: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
  status?: 'PLANNING' | 'ACTIVE' | 'COMPLETED';
}

export interface BurndownDataPoint {
  date: string;
  remainingPoints: number;
  idealPoints: number;
  totalIssues: number;
  completedIssues: number;
  addedIssues: number;
  removedIssues: number;
}

export interface BurndownResponse {
  sprintId: string;
  sprintName: string;
  startDate: string;
  endDate: string;
  totalPoints: number;
  completedPoints: number;
  remainingPoints: number;
  totalIssues: number;
  completedIssues: number;
  completionRate: number;
  dailyData: BurndownDataPoint[];
}

export interface SprintVelocity {
  sprintId: string;
  sprintName: string;
  startDate: string;
  endDate: string;
  committedPoints: number;
  completedPoints: number;
  reliability: number;
  isCompleted: boolean;
}

export interface VelocityResponse {
  projectId: string;
  currentVelocity: number;
  averageVelocity: number;
  highestVelocity: number;
  lowestVelocity: number;
  totalSprints: number;
  completedSprints: number;
  velocityTrend: number;
  sprintVelocities: SprintVelocity[];
}

export interface SprintReportResponse {
  sprintId: string;
  sprintName: string;
  sprintGoal?: string;
  startDate: string;
  endDate: string;
  completeDate?: string;
  status: string;
  totalIssues: number;
  completedIssues: number;
  inProgressIssues: number;
  todoIssues: number;
  blockedIssues: number;
  totalPoints: number;
  completedPoints: number;
  remainingPoints: number;
  completionRate: number;
  pointsCompletionRate: number;
  daysRemaining: number;
  dailyBurnRate: number;
  projectedCompletion: number;
  issuesByStatus: Record<string, number>;
  issuesByPriority: Record<string, number>;
  issuesByType: Record<string, number>;
  issuesByAssignee: Record<string, number>;
  burndown: BurndownResponse;
  velocity: VelocityResponse;
}

export const sprintApi = {
  create: (data: CreateSprintRequest) =>
    apiClient.post<SprintResponse>('/api/sprints', data),

  getAll: (projectId?: string) =>
    apiClient.get<{ content: SprintResponse[] } | SprintResponse[]>('/api/sprints', {
      params: projectId ? { projectId } : {},
    }).then(response => {
      const data = response.data;
      // Handle both array and paginated response
      if (Array.isArray(data)) return data;
      return data?.content ?? [];
    }),

  getById: (sprintId: string) =>
    apiClient.get<SprintResponse>(`/api/sprints/${sprintId}`),

  update: (sprintId: string, data: UpdateSprintRequest) =>
    apiClient.put<SprintResponse>(`/api/sprints/${sprintId}`, data),

  delete: (sprintId: string) =>
    apiClient.delete(`/api/sprints/${sprintId}`),

  start: (sprintId: string) =>
    apiClient.post<SprintResponse>(`/api/sprints/${sprintId}/start`),

  complete: (sprintId: string) =>
    apiClient.post<SprintResponse>(`/api/sprints/${sprintId}/complete`),

  getIssues: (sprintId: string) =>
    apiClient.get('/api/issues', { params: { sprintId } }),

  // Reports
  getReport: (sprintId: string) =>
    apiClient.get<SprintReportResponse>(`/api/sprints/reports/${sprintId}`),

  getBurndown: (sprintId: string) =>
    apiClient.get<BurndownResponse>(`/api/sprints/reports/${sprintId}/burndown`),

  getVelocity: (projectId: string) =>
    apiClient.get<VelocityResponse>('/api/sprints/reports/velocity', {
      params: { projectId },
    }),
};
