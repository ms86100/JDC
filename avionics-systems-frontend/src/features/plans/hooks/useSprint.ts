import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import { appNotify } from '../../../lib/appNotify';

// Types
export interface SprintResponse {
  id: string;
  boardId: string;
  name: string;
  goal: string | null;
  startDate: string | null;
  endDate: string | null;
  completeDate: string | null;
  state: 'FUTURE' | 'ACTIVE' | 'CLOSED' | 'ABANDONED';
  sequence: number;
  velocity: number;
  committedPoints: number;
  completedPoints: number;
  totalIssues: number;
  completedIssues: number;
}

export interface SprintIssueResponse {
  id: string;
  sprintId: string;
  planItemId: string;
  issueId: string;
  rankValue: string | null;
  addedAt: string;
  addedBy: string | null;
  removedAt: string | null;
  completionStatus: 'UNCOMPLETED' | 'COMPLETED' | 'DROPPED';
  completedAt: string | null;
  assigneeId?: string;
  updatedAt?: string;
  flagged?: boolean;
  flagReason?: string | null;
}

// New types for gap implementations
export interface PaginatedResponse<T> {
  maxResults: number;
  startAt: number;
  total: number;
  isLast: boolean;
  values: T[];
}

export interface SprintReportResponse {
  sprintId: string;
  sprintName: string;
  sprintGoal: string | null;
  startDate: string | null;
  endDate: string | null;
  completeDate: string | null;
  state: string;
  completedIssues: SprintIssueResponse[];
  issuesNotCompletedInCurrentSprint: SprintIssueResponse[];
  puntedIssues: SprintIssueResponse[];
  issueKeysAddedDuringSprint: string[];
  committedPoints: number;
  completedPoints: number;
  scopeChangePoints: number;
  totalIssues: number;
  completedIssueCount: number;
  inProgressIssueCount: number;
  todoIssueCount: number;
  completionRate: number;
}

export interface VelocityChartResponse {
  boardId: string;
  averageVelocity: number;
  sprints: {
    sprintId: string;
    sprintName: string;
    startDate: string | null;
    endDate: string | null;
    committedPoints: number;
    completedPoints: number;
  }[];
}

export interface EventBurndownResponse {
  sprintId: string;
  startTime: string | null;
  endTime: string | null;
  events: {
    timestamp: string;
    eventType: string;
    planItemId: string | null;
    pointsDelta: number | null;
    oldValue: number | null;
    newValue: number | null;
  }[];
  dailySnapshots: BurndownPoint[];
}

export interface CumulativeFlowResponse {
  boardId: string;
  columns: string[];
  dataPoints: {
    date: string;
    columnCounts: Record<string, number>;
  }[];
}

export interface ControlChartResponse {
  boardId: string;
  averageCycleTime: number;
  averageLeadTime: number;
  standardDeviation: number;
  issues: {
    issueId: string;
    planItemId: string;
    cycleTimeDays: number;
    leadTimeDays: number;
    completedAt: string;
  }[];
}

export interface EpicBurndownResponse {
  epicId: string;
  epicName: string;
  sprintEntries: {
    sprintId: string;
    sprintName: string;
    totalPoints: number;
    completedPoints: number;
    remainingPoints: number;
  }[];
}

export interface BoardFeaturesResponse {
  boardId: string;
  sprints: boolean;
  backlog: boolean;
  estimation: boolean;
  parallelSprints: boolean;
}

export interface BacklogPlanningResponse {
  boardId: string;
  sprintSections: {
    sprintId: string;
    sprintName: string;
    sprintState: string;
    totalIssues: number;
    totalPoints: number;
    issues: SprintIssueResponse[];
  }[];
  backlog: {
    totalIssues: number;
    totalPoints: number;
    planItemIds: string[];
  };
}

export interface SprintPropertyResponse {
  key: string;
  value: string;
}

export interface IssueEstimationResponse {
  planItemId: string;
  boardId: string;
  storyPoints: number | null;
}

export interface BulkMoveIssuesResponse {
  addedCount: number;
  removedFromPreviousCount: number;
  movedIssues: SprintIssueResponse[];
}

export interface SprintBurndownResponse {
  sprintId: string;
  sprintName: string;
  startDate: string | null;
  endDate: string | null;
  totalIssues: number;
  completedIssues: number;
  totalPoints: number;
  completedPoints: number;
  burndownPoints: BurndownPoint[];
}

export interface BurndownPoint {
  date: string;
  remainingIssues: number;
  completedIssues: number;
  remainingPoints: number;
  idealRemaining: number;
}

export interface CreateSprintRequest {
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
}

// API functions
const sprintApi = {
  getByBoardId: (boardId: string) =>
    apiClient.get<SprintResponse[]>(`/api/plans/boards/${boardId}/sprints`),
  getById: (sprintId: string) =>
    apiClient.get<SprintResponse>(`/api/plans/sprints/${sprintId}`),
  create: (boardId: string, data: CreateSprintRequest) =>
    apiClient.post<SprintResponse>(`/api/plans/boards/${boardId}/sprints`, data),
  update: (sprintId: string, data: CreateSprintRequest) =>
    apiClient.put<SprintResponse>(`/api/plans/sprints/${sprintId}`, data),
  delete: (sprintId: string) =>
    apiClient.delete(`/api/plans/sprints/${sprintId}`),

  start: (sprintId: string, userId?: string) =>
    apiClient.post<SprintResponse>(`/api/plans/sprints/${sprintId}/start${userId ? `?userId=${userId}` : ''}`),
  close: (sprintId: string, userId?: string) =>
    apiClient.post<SprintResponse>(`/api/plans/sprints/${sprintId}/close${userId ? `?userId=${userId}` : ''}`),
  abandon: (sprintId: string, userId?: string) =>
    apiClient.post<SprintResponse>(`/api/plans/sprints/${sprintId}/abandon${userId ? `?userId=${userId}` : ''}`),

  getIssues: (sprintId: string) =>
    apiClient.get<SprintIssueResponse[]>(`/api/plans/sprints/${sprintId}/issues`),
  addIssue: (sprintId: string, planItemId: string, userId?: string) =>
    apiClient.post<SprintIssueResponse>(
      `/api/plans/sprints/${sprintId}/issues?planItemId=${planItemId}${userId ? `&userId=${userId}` : ''}`
    ),
  removeIssue: (sprintId: string, planItemId: string, userId?: string) =>
    apiClient.delete(
      `/api/plans/sprints/${sprintId}/issues/${planItemId}${userId ? `?userId=${userId}` : ''}`
    ),
  completeIssue: (sprintId: string, planItemId: string) =>
    apiClient.post<SprintIssueResponse>(`/api/plans/sprints/${sprintId}/issues/${planItemId}/complete`),

  getBurndown: (sprintId: string) =>
    apiClient.get<SprintBurndownResponse>(`/api/plans/sprints/${sprintId}/burndown`),
  takeSnapshot: (sprintId: string) =>
    apiClient.post(`/api/plans/sprints/${sprintId}/burndown/snapshot`),

  getVelocity: (boardId: string) =>
    apiClient.get<number>(`/api/plans/boards/${boardId}/velocity`),

  // Gap 1: Partial update
  partialUpdate: (sprintId: string, data: Partial<CreateSprintRequest> & { wipLimit?: number; state?: string }) =>
    apiClient.post<SprintResponse>(`/api/plans/sprints/${sprintId}`, data),

  // Gap 2: Sprint swap
  swapSprints: (sprintId: string, sprintToSwapWith: string, userId: string) =>
    apiClient.post(`/api/plans/sprints/${sprintId}/swap?userId=${userId}`, { sprintToSwapWith }),

  // Gap 3: Sprint properties
  getProperties: (sprintId: string) =>
    apiClient.get<SprintPropertyResponse[]>(`/api/plans/sprints/${sprintId}/properties`),
  getProperty: (sprintId: string, key: string) =>
    apiClient.get<SprintPropertyResponse>(`/api/plans/sprints/${sprintId}/properties/${key}`),
  setProperty: (sprintId: string, key: string, value: string, userId: string) =>
    apiClient.put<SprintPropertyResponse>(`/api/plans/sprints/${sprintId}/properties/${key}?userId=${userId}`, value),
  deleteProperty: (sprintId: string, key: string, userId: string) =>
    apiClient.delete(`/api/plans/sprints/${sprintId}/properties/${key}?userId=${userId}`),

  // Gap 4: Bulk move
  bulkMoveIssues: (sprintId: string, data: { issueIds: string[]; rankBeforeIssue?: string; rankAfterIssue?: string }, userId: string) =>
    apiClient.post<BulkMoveIssuesResponse>(`/api/plans/sprints/${sprintId}/issues/bulk-move?userId=${userId}`, data),

  // Gap 5: Move to backlog
  moveToBacklog: (planItemIds: string[], userId: string) =>
    apiClient.post(`/api/plans/backlog/issues?userId=${userId}`, { planItemIds }),

  // Gap 6: Rank issue
  rankIssue: (planItemId: string, data: { sprintId: string; rankBeforeIssue?: string; rankAfterIssue?: string }, userId: string) =>
    apiClient.put<SprintIssueResponse>(`/api/plans/sprints/issues/${planItemId}/rank?userId=${userId}`, data),

  // Gap 9: Search/filter issues
  searchIssues: (sprintId: string, params: { jql?: string; startAt?: number; maxResults?: number }) =>
    apiClient.get<PaginatedResponse<SprintIssueResponse>>(`/api/plans/sprints/${sprintId}/issues/search`, { params }),

  // Gap 10: Sprint report
  getReport: (sprintId: string) =>
    apiClient.get<SprintReportResponse>(`/api/plans/sprints/${sprintId}/report`),

  // Gap 11: Event burndown
  getEventBurndown: (sprintId: string) =>
    apiClient.get<EventBurndownResponse>(`/api/plans/sprints/${sprintId}/burndown/events`),

  // Gap 12: Velocity chart
  getVelocityChart: (boardId: string) =>
    apiClient.get<VelocityChartResponse>(`/api/plans/boards/${boardId}/velocity/chart`),

  // Gap 13: CFD
  getCfd: (boardId: string, from?: string, to?: string) =>
    apiClient.get<CumulativeFlowResponse>(`/api/plans/boards/${boardId}/cfd`, { params: { from, to } }),

  // Gap 14: Control chart
  getControlChart: (boardId: string) =>
    apiClient.get<ControlChartResponse>(`/api/plans/boards/${boardId}/control-chart`),

  // Gap 15: Epic burndown
  getEpicBurndown: (epicPlanItemId: string) =>
    apiClient.get<EpicBurndownResponse>(`/api/plans/epics/${epicPlanItemId}/burndown`),

  // Gap 16: Reopen sprint
  reopen: (sprintId: string, userId: string) =>
    apiClient.post<SprintResponse>(`/api/plans/sprints/${sprintId}/reopen?userId=${userId}`),

  // Gap 17: Estimation
  getEstimation: (boardId: string, planItemId: string) =>
    apiClient.get<IssueEstimationResponse>(`/api/plans/boards/${boardId}/issues/${planItemId}/estimation`),
  updateEstimation: (boardId: string, planItemId: string, value: number, userId: string) =>
    apiClient.put<IssueEstimationResponse>(`/api/plans/boards/${boardId}/issues/${planItemId}/estimation?userId=${userId}`, { value }),

  // Gap 18: Board features
  getBoardFeatures: (boardId: string) =>
    apiClient.get<BoardFeaturesResponse>(`/api/plans/boards/${boardId}/features`),
  updateBoardFeatures: (boardId: string, data: Partial<BoardFeaturesResponse>, userId: string) =>
    apiClient.put<BoardFeaturesResponse>(`/api/plans/boards/${boardId}/features?userId=${userId}`, data),

  // Gap 19: Flag + closed sprints
  toggleFlag: (sprintId: string, planItemId: string, flagged: boolean, reason: string | null, userId: string) =>
    apiClient.post<SprintIssueResponse>(`/api/plans/sprints/${sprintId}/issues/${planItemId}/flag?userId=${userId}`, { flagged, reason }),
  getClosedSprintsForIssue: (planItemId: string) =>
    apiClient.get<SprintResponse[]>(`/api/plans/issues/${planItemId}/closed-sprints`),

  // Gap 20: Close with move
  closeWithMove: (sprintId: string, userId: string, moveIncompleteToSprintId?: string) =>
    apiClient.post<SprintResponse>(
      `/api/plans/sprints/${sprintId}/close?userId=${userId}`,
      moveIncompleteToSprintId ? { moveIncompleteToSprintId } : undefined
    ),

  // Gap 22: Backlog planning
  getBacklogPlanning: (boardId: string) =>
    apiClient.get<BacklogPlanningResponse>(`/api/plans/boards/${boardId}/backlog-planning`),
};

// Hooks
export const useSprints = (boardId: string) => {
  return useQuery({
    queryKey: ['sprints', boardId],
    queryFn: () => sprintApi.getByBoardId(boardId),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};

export const useSprint = (sprintId: string) => {
  return useQuery({
    queryKey: ['sprint', sprintId],
    queryFn: () => sprintApi.getById(sprintId),
    select: (res) => res.data,
    enabled: !!sprintId,
  });
};

export const useCreateSprint = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ boardId, data }: { boardId: string; data: CreateSprintRequest }) =>
      sprintApi.create(boardId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprints', variables.boardId] });
    },
    onError: (error: Error) => {
      console.error('Failed to create sprint:', error);
      appNotify.error(error.message || 'Failed to create sprint');
    },
  });
};

export const useUpdateSprint = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sprintId, data }: { sprintId: string; data: CreateSprintRequest }) =>
      sprintApi.update(sprintId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprint', variables.sprintId] });
    },
    onError: (error: Error) => {
      console.error('Failed to update sprint:', error);
      appNotify.error(error.message || 'Failed to update sprint');
    },
  });
};

export const useStartSprint = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sprintId, userId }: { sprintId: string; userId?: string }) =>
      sprintApi.start(sprintId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      queryClient.invalidateQueries({ queryKey: ['sprint'] });
    },
    onError: (error: Error) => {
      console.error('Failed to start sprint:', error);
      appNotify.error(error.message || 'Failed to start sprint');
    },
  });
};

export const useCloseSprint = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sprintId, userId }: { sprintId: string; userId?: string }) =>
      sprintApi.close(sprintId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      queryClient.invalidateQueries({ queryKey: ['sprint'] });
    },
    onError: (error: Error) => {
      console.error('Failed to close sprint:', error);
      appNotify.error(error.message || 'Failed to close sprint');
    },
  });
};

export const useAbandonSprint = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sprintId, userId }: { sprintId: string; userId?: string }) =>
      sprintApi.abandon(sprintId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      queryClient.invalidateQueries({ queryKey: ['sprint'] });
    },
    onError: (error: Error) => {
      console.error('Failed to abandon sprint:', error);
      appNotify.error(error.message || 'Failed to abandon sprint');
    },
  });
};

export const useSprintIssues = (sprintId: string) => {
  return useQuery({
    queryKey: ['sprintIssues', sprintId],
    queryFn: () => sprintApi.getIssues(sprintId),
    select: (res) => res.data,
    enabled: !!sprintId,
  });
};

export const useAddIssueToSprint = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sprintId, planItemId, userId }: { sprintId: string; planItemId: string; userId?: string }) =>
      sprintApi.addIssue(sprintId, planItemId, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues', variables.sprintId] });
      queryClient.invalidateQueries({ queryKey: ['sprint', variables.sprintId] });
    },
    onError: (error: Error) => {
      console.error('Failed to add issue to sprint:', error);
      appNotify.error(error.message || 'Failed to add issue to sprint');
    },
  });
};

export const useRemoveIssueFromSprint = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sprintId, planItemId, userId }: { sprintId: string; planItemId: string; userId?: string }) =>
      sprintApi.removeIssue(sprintId, planItemId, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues', variables.sprintId] });
      queryClient.invalidateQueries({ queryKey: ['sprint', variables.sprintId] });
    },
    onError: (error: Error) => {
      console.error('Failed to remove issue from sprint:', error);
      appNotify.error(error.message || 'Failed to remove issue from sprint');
    },
  });
};

export const useCompleteIssue = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sprintId, planItemId }: { sprintId: string; planItemId: string }) =>
      sprintApi.completeIssue(sprintId, planItemId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues', variables.sprintId] });
      queryClient.invalidateQueries({ queryKey: ['sprint', variables.sprintId] });
    },
    onError: (error: Error) => {
      console.error('Failed to complete issue:', error);
      appNotify.error(error.message || 'Failed to complete issue');
    },
  });
};

export const useSprintBurndown = (sprintId: string) => {
  return useQuery({
    queryKey: ['sprintBurndown', sprintId],
    queryFn: () => sprintApi.getBurndown(sprintId),
    select: (res) => res.data,
    enabled: !!sprintId,
  });
};

export const useTakeBurndownSnapshot = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: sprintApi.takeSnapshot,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprintBurndown'] });
    },
    onError: (error: Error) => {
      console.error('Failed to take burndown snapshot:', error);
      appNotify.error(error.message || 'Failed to take burndown snapshot');
    },
  });
};

export const useBoardVelocity = (boardId: string) => {
  return useQuery({
    queryKey: ['boardVelocity', boardId],
    queryFn: () => sprintApi.getVelocity(boardId),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};

// Working days query for sprint creation
export const useWorkingDays = () => {
  return useQuery({
    queryKey: ['workingDays', 'default'],
    queryFn: () => apiClient.get('/plans/working-days/default'),
    select: (res) => res.data,
  });
};

// Update issue column/status (for drag and drop)
export const useUpdateIssueColumn = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ sprintId, planItemId, columnName }: { sprintId: string; planItemId: string; columnName: string }) =>
      apiClient.put(`/api/plans/sprints/${sprintId}/issues/${planItemId}/column`, { columnName }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues', variables.sprintId] });
      queryClient.invalidateQueries({ queryKey: ['sprint', variables.sprintId] });
    },
    onError: (error: Error) => {
      console.error('Failed to update issue column:', error);
      appNotify.error(error.message || 'Failed to update issue column');
    },
  });
};

// Issue ranking actions (Rank to Top/Bottom)
export const useRankIssueToTop = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ planId, planItemId }: { planId: string; planItemId: string }) =>
      apiClient.post(`/api/plans/${planId}/backlog/rank/top`, { planItemId }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['backlog', variables.planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to rank issue to top:', error);
      appNotify.error(error.message || 'Failed to rank issue to top');
    },
  });
};

export const useRankIssueToBottom = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ planId, planItemId }: { planId: string; planItemId: string }) =>
      apiClient.post(`/api/plans/${planId}/backlog/rank/bottom`, { planItemId }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['backlog', variables.planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to rank issue to bottom:', error);
      appNotify.error(error.message || 'Failed to rank issue to bottom');
    },
  });
};

// Issue assignment
export const useAssignIssueToMe = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ issueId }: { issueId: string }) =>
      apiClient.put(`/api/issues/${issueId}/assign`, { assignee: 'current_user' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
    },
    onError: (error: Error) => {
      console.error('Failed to assign issue:', error);
      appNotify.error(error.message || 'Failed to assign issue');
    },
  });
};

// Clone issue
export const useCloneIssue = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ issueId }: { issueId: string }) =>
      apiClient.post(`/api/issues/${issueId}/clone`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
    },
    onError: (error: Error) => {
      console.error('Failed to clone issue:', error);
      appNotify.error(error.message || 'Failed to clone issue');
    },
  });
};

// Create sub-task
export const useCreateSubTask = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ parentIssueId, subTaskSummary }: { parentIssueId: string; subTaskSummary: string }) =>
      apiClient.post(`/api/issues/${parentIssueId}/subtasks`, { summary: subTaskSummary }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create sub-task:', error);
      appNotify.error(error.message || 'Failed to create sub-task');
    },
  });
};

// Archive issue
export const useArchiveIssue = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ issueId }: { issueId: string }) =>
      apiClient.put(`/api/issues/${issueId}/archive`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
    },
    onError: (error: Error) => {
      console.error('Failed to archive issue:', error);
      appNotify.error(error.message || 'Failed to archive issue');
    },
  });
};

// ==================== GAP IMPLEMENTATION HOOKS ====================

// Gap 1: Partial update sprint
export const usePartialUpdateSprint = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sprintId, data }: { sprintId: string; data: Partial<CreateSprintRequest> & { wipLimit?: number } }) =>
      sprintApi.partialUpdate(sprintId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprint', variables.sprintId] });
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to update sprint'),
  });
};

// Gap 2: Swap sprint order
export const useSwapSprint = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sprintId, swapWith, userId }: { sprintId: string; swapWith: string; userId: string }) =>
      sprintApi.swapSprints(sprintId, swapWith, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to swap sprints'),
  });
};

// Gap 3: Sprint properties
export const useSprintProperties = (sprintId: string) => {
  return useQuery({
    queryKey: ['sprintProperties', sprintId],
    queryFn: () => sprintApi.getProperties(sprintId),
    select: (res) => res.data,
    enabled: !!sprintId,
  });
};

export const useSetSprintProperty = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sprintId, key, value, userId }: { sprintId: string; key: string; value: string; userId: string }) =>
      sprintApi.setProperty(sprintId, key, value, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprintProperties', variables.sprintId] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to set sprint property'),
  });
};

export const useDeleteSprintProperty = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sprintId, key, userId }: { sprintId: string; key: string; userId: string }) =>
      sprintApi.deleteProperty(sprintId, key, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprintProperties', variables.sprintId] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to delete sprint property'),
  });
};

// Gap 4: Bulk move issues
export const useBulkMoveIssues = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sprintId, issueIds, userId, rankBeforeIssue, rankAfterIssue }: {
      sprintId: string; issueIds: string[]; userId: string; rankBeforeIssue?: string; rankAfterIssue?: string;
    }) => sprintApi.bulkMoveIssues(sprintId, { issueIds, rankBeforeIssue, rankAfterIssue }, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to move issues'),
  });
};

// Gap 5: Move to backlog
export const useMoveToBacklog = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planItemIds, userId }: { planItemIds: string[]; userId: string }) =>
      sprintApi.moveToBacklog(planItemIds, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      queryClient.invalidateQueries({ queryKey: ['backlog'] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to move issues to backlog'),
  });
};

// Gap 6: Rank issue
export const useRankIssue = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planItemId, sprintId, rankBeforeIssue, rankAfterIssue, userId }: {
      planItemId: string; sprintId: string; rankBeforeIssue?: string; rankAfterIssue?: string; userId: string;
    }) => sprintApi.rankIssue(planItemId, { sprintId, rankBeforeIssue, rankAfterIssue }, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to rank issue'),
  });
};

// Gap 8: Paginated sprints
export const useSprintsPaginated = (boardId: string, params?: { state?: string; startAt?: number; maxResults?: number }) => {
  return useQuery({
    queryKey: ['sprintsPaginated', boardId, params],
    queryFn: () => apiClient.get<PaginatedResponse<SprintResponse>>(`/api/plans/boards/${boardId}/sprints`, { params }),
    select: (res) => res.data,
    enabled: !!boardId && !!params,
  });
};

// Gap 9: Search sprint issues
export const useSearchSprintIssues = (sprintId: string, params?: { jql?: string; startAt?: number; maxResults?: number }) => {
  return useQuery({
    queryKey: ['sprintIssuesSearch', sprintId, params],
    queryFn: () => sprintApi.searchIssues(sprintId, params || {}),
    select: (res) => res.data,
    enabled: !!sprintId,
  });
};

// Gap 10: Sprint report
export const useSprintReport = (sprintId: string) => {
  return useQuery({
    queryKey: ['sprintReport', sprintId],
    queryFn: () => sprintApi.getReport(sprintId),
    select: (res) => res.data,
    enabled: !!sprintId,
  });
};

// Gap 11: Event burndown
export const useEventBurndown = (sprintId: string) => {
  return useQuery({
    queryKey: ['eventBurndown', sprintId],
    queryFn: () => sprintApi.getEventBurndown(sprintId),
    select: (res) => res.data,
    enabled: !!sprintId,
  });
};

// Gap 12: Velocity chart
export const useVelocityChart = (boardId: string) => {
  return useQuery({
    queryKey: ['velocityChart', boardId],
    queryFn: () => sprintApi.getVelocityChart(boardId),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};

// Gap 13: Cumulative flow diagram
export const useCumulativeFlow = (boardId: string, from?: string, to?: string) => {
  return useQuery({
    queryKey: ['cfd', boardId, from, to],
    queryFn: () => sprintApi.getCfd(boardId, from, to),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};

// Gap 14: Control chart
export const useControlChart = (boardId: string) => {
  return useQuery({
    queryKey: ['controlChart', boardId],
    queryFn: () => sprintApi.getControlChart(boardId),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};

// Gap 15: Epic burndown
export const useEpicBurndown = (epicPlanItemId: string) => {
  return useQuery({
    queryKey: ['epicBurndown', epicPlanItemId],
    queryFn: () => sprintApi.getEpicBurndown(epicPlanItemId),
    select: (res) => res.data,
    enabled: !!epicPlanItemId,
  });
};

// Gap 16: Reopen sprint
export const useReopenSprint = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sprintId, userId }: { sprintId: string; userId: string }) =>
      sprintApi.reopen(sprintId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      queryClient.invalidateQueries({ queryKey: ['sprint'] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to reopen sprint'),
  });
};

// Gap 17: Issue estimation
export const useIssueEstimation = (boardId: string, planItemId: string) => {
  return useQuery({
    queryKey: ['estimation', boardId, planItemId],
    queryFn: () => sprintApi.getEstimation(boardId, planItemId),
    select: (res) => res.data,
    enabled: !!boardId && !!planItemId,
  });
};

export const useUpdateEstimation = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ boardId, planItemId, value, userId }: { boardId: string; planItemId: string; value: number; userId: string }) =>
      sprintApi.updateEstimation(boardId, planItemId, value, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['estimation', variables.boardId, variables.planItemId] });
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to update estimation'),
  });
};

// Gap 18: Board features
export const useBoardFeatures = (boardId: string) => {
  return useQuery({
    queryKey: ['boardFeatures', boardId],
    queryFn: () => sprintApi.getBoardFeatures(boardId),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};

export const useUpdateBoardFeatures = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ boardId, data, userId }: { boardId: string; data: Partial<BoardFeaturesResponse>; userId: string }) =>
      sprintApi.updateBoardFeatures(boardId, data, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['boardFeatures', variables.boardId] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to update board features'),
  });
};

// Gap 19: Toggle flag
export const useToggleFlag = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sprintId, planItemId, flagged, reason, userId }: {
      sprintId: string; planItemId: string; flagged: boolean; reason: string | null; userId: string;
    }) => sprintApi.toggleFlag(sprintId, planItemId, flagged, reason, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['sprintIssues', variables.sprintId] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to toggle flag'),
  });
};

export const useClosedSprintsForIssue = (planItemId: string) => {
  return useQuery({
    queryKey: ['closedSprints', planItemId],
    queryFn: () => sprintApi.getClosedSprintsForIssue(planItemId),
    select: (res) => res.data,
    enabled: !!planItemId,
  });
};

// Gap 20: Close sprint with move
export const useCloseSprintWithMove = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sprintId, userId, moveIncompleteToSprintId }: {
      sprintId: string; userId: string; moveIncompleteToSprintId?: string;
    }) => sprintApi.closeWithMove(sprintId, userId, moveIncompleteToSprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      queryClient.invalidateQueries({ queryKey: ['sprint'] });
      queryClient.invalidateQueries({ queryKey: ['sprintIssues'] });
    },
    onError: (error: Error) => appNotify.error(error.message || 'Failed to close sprint'),
  });
};

// Gap 22: Backlog planning view
export const useBacklogPlanning = (boardId: string) => {
  return useQuery({
    queryKey: ['backlogPlanning', boardId],
    queryFn: () => sprintApi.getBacklogPlanning(boardId),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};