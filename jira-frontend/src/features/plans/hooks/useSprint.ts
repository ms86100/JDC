import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';

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
      alert(error.message || 'Failed to create sprint');
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
      alert(error.message || 'Failed to update sprint');
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
      alert(error.message || 'Failed to start sprint');
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
      alert(error.message || 'Failed to close sprint');
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
      alert(error.message || 'Failed to abandon sprint');
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
      alert(error.message || 'Failed to add issue to sprint');
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
      alert(error.message || 'Failed to remove issue from sprint');
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
      alert(error.message || 'Failed to complete issue');
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
      alert(error.message || 'Failed to take burndown snapshot');
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
    queryFn: () => apiClient.get('/api/plans/working-days/default'),
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
      alert(error.message || 'Failed to update issue column');
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
      alert(error.message || 'Failed to rank issue to top');
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
      alert(error.message || 'Failed to rank issue to bottom');
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
      alert(error.message || 'Failed to assign issue');
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
      alert(error.message || 'Failed to clone issue');
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
      alert(error.message || 'Failed to create sub-task');
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
      alert(error.message || 'Failed to archive issue');
    },
  });
};