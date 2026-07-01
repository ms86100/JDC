import { useQuery } from '@tanstack/react-query';
import { issueApi, IssueResponse } from '../../api/issueApi';
import { normalizeIssue } from '../../api/issueMapper';
import { sprintApi, SprintResponse } from '../../api/sprintApi';
import boardApi from '../../api/boardApi';
import { asArray } from '../../utils/apiList';
import { computeWorkMetrics, getActiveSprint } from './metrics';

function normalizeIssueList(rows: IssueResponse[]): IssueResponse[] {
  return rows.map((row) =>
    normalizeIssue(row as unknown as Record<string, unknown>),
  );
}

async function fetchIssuesForProject(projectId: string): Promise<IssueResponse[]> {
  const response = await issueApi.getAll({ projectId });
  return normalizeIssueList(asArray<IssueResponse>(response.data));
}

async function fetchAllIssues(): Promise<IssueResponse[]> {
  const response = await issueApi.getAll();
  return normalizeIssueList(asArray<IssueResponse>(response.data));
}

export function useProjectWorkspaceData(projectId: string | undefined) {
  const issuesQuery = useQuery({
    queryKey: ['ws-project-issues', projectId],
    queryFn: () => fetchIssuesForProject(projectId!),
    enabled: !!projectId,
    staleTime: 30000,
    retry: 1,
  });

  const sprintsQuery = useQuery({
    queryKey: ['ws-project-sprints', projectId],
    queryFn: () => sprintApi.getAll(projectId).catch(() => [] as SprintResponse[]),
    enabled: !!projectId,
    staleTime: 30000,
    retry: 1,
  });

  const boardsQuery = useQuery({
    queryKey: ['ws-project-boards', projectId],
    queryFn: () => boardApi.getBoardsByProject(projectId!).catch(() => []),
    enabled: !!projectId,
    staleTime: 60000,
  });

  const issues = issuesQuery.data ?? [];
  const metrics = computeWorkMetrics(issues);
  const sprints = sprintsQuery.data ?? [];
  const activeSprint = getActiveSprint(sprints);
  const boards = boardsQuery.data ?? [];

  return {
    issues,
    metrics,
    sprints,
    activeSprint,
    boards,
    isLoading: (issuesQuery.isPending && !issuesQuery.data) || (sprintsQuery.isPending && !sprintsQuery.data),
    isError: issuesQuery.isError || sprintsQuery.isError,
  };
}

export function useProjectsPortfolioData(projectIds: string[]) {
  const allIssuesQuery = useQuery({
    queryKey: ['ws-portfolio-issues'],
    queryFn: fetchAllIssues,
    staleTime: 30000,
  });

  const issuesByProject = new Map<string, IssueResponse[]>();
  for (const issue of allIssuesQuery.data ?? []) {
    const list = issuesByProject.get(issue.projectId) ?? [];
    list.push(issue);
    issuesByProject.set(issue.projectId, list);
  }

  const metricsByProject = new Map(
    projectIds.map((id) => [id, computeWorkMetrics(issuesByProject.get(id) ?? [])])
  );

  return {
    issuesByProject,
    metricsByProject,
    isLoading: allIssuesQuery.isLoading,
  };
}

export function useProgramPortfolioIssues() {
  return useQuery({
    queryKey: ['ws-program-portfolio-issues'],
    queryFn: fetchAllIssues,
    staleTime: 30000,
  });
}

export function useAllSprints() {
  return useQuery<SprintResponse[]>({
    queryKey: ['ws-all-sprints'],
    queryFn: () => sprintApi.getAll(),
    staleTime: 30000,
  });
}
