import { useQuery } from '@tanstack/react-query';
import { sprintApi, BurndownResponse, VelocityResponse } from '../../api/sprintApi';
import boardApi, { BoardDataResponse, AgileBoard } from '../../api/boardApi';
import type { IssueResponse } from '../../api/issueApi';
import type { SprintResponse } from '../../api/sprintApi';

export function useScrumProjectData(
  projectId: string | undefined,
  activeSprint: SprintResponse | undefined,
  boards: AgileBoard[]
) {
  const burndownQuery = useQuery({
    queryKey: ['ws-burndown', activeSprint?.id],
    queryFn: async () => {
      const res = await sprintApi.getBurndown(activeSprint!.id);
      return res.data as BurndownResponse;
    },
    enabled: !!activeSprint?.id,
    staleTime: 60000,
  });

  const velocityQuery = useQuery({
    queryKey: ['ws-velocity', projectId],
    queryFn: async () => {
      const res = await sprintApi.getVelocity(projectId!);
      return res.data as VelocityResponse;
    },
    enabled: !!projectId,
    staleTime: 60000,
  });

  const scrumBoard = boards.find((b) => b.boardType === 'SCRUM') ?? boards[0];

  return {
    burndown: burndownQuery.data,
    velocity: velocityQuery.data,
    scrumBoard,
    isLoading: burndownQuery.isLoading || velocityQuery.isLoading,
  };
}

export function useKanbanProjectData(projectId: string | undefined, boards: AgileBoard[]) {
  const kanbanBoard = boards.find((b) => b.boardType === 'KANBAN') ?? boards[0];

  const boardDataQuery = useQuery({
    queryKey: ['ws-kanban-board', kanbanBoard?.id],
    queryFn: () => boardApi.getBoardData(kanbanBoard!.id),
    enabled: !!kanbanBoard?.id,
    staleTime: 30000,
  });

  return {
    kanbanBoard,
    boardData: boardDataQuery.data as BoardDataResponse | undefined,
    isLoading: boardDataQuery.isLoading,
  };
}

/** Group issues by status for WIP / lifecycle views */
export function groupIssuesByStatus(issues: IssueResponse[]): { status: string; count: number; issues: IssueResponse[] }[] {
  const map = new Map<string, IssueResponse[]>();
  for (const issue of issues) {
    const list = map.get(issue.status) ?? [];
    list.push(issue);
    map.set(issue.status, list);
  }
  return Array.from(map.entries())
    .map(([status, list]) => ({ status, count: list.length, issues: list }))
    .sort((a, b) => b.count - a.count);
}

/** Assignee workload for task-oriented projects */
export function groupIssuesByAssignee(issues: IssueResponse[]): { assignee: string; count: number; overdue: number }[] {
  const map = new Map<string, { count: number; overdue: number }>();
  const now = Date.now();
  for (const issue of issues) {
    const key = issue.assigneeName || 'Unassigned';
    const entry = map.get(key) ?? { count: 0, overdue: 0 };
    entry.count += 1;
    if (issue.dueDate && new Date(issue.dueDate).getTime() < now && !isDoneStatus(issue.status)) {
      entry.overdue += 1;
    }
    map.set(key, entry);
  }
  return Array.from(map.entries())
    .map(([assignee, v]) => ({ assignee, ...v }))
    .sort((a, b) => b.count - a.count);
}

function isDoneStatus(status: string): boolean {
  const s = status.toLowerCase();
  return s.includes('done') || s.includes('closed') || s.includes('resolved');
}

export function getUpcomingDeadlines(issues: IssueResponse[], limit = 8) {
  const now = Date.now();
  return issues
    .filter((i) => i.dueDate && !isDoneStatus(i.status))
    .sort((a, b) => new Date(a.dueDate!).getTime() - new Date(b.dueDate!).getTime())
    .slice(0, limit);
}
