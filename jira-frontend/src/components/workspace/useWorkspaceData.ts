import { useQuery } from '@tanstack/react-query';
import { issueApi, IssueResponse } from '../../api/issueApi';
import { sprintApi, SprintResponse } from '../../api/sprintApi';
import boardApi from '../../api/boardApi';
import { computeWorkMetrics, getActiveSprint } from './metrics';

async function fetchIssuesForProject(projectId: string): Promise<IssueResponse[]> {
  const response = await issueApi.getAll({ projectId });
  const data = response.data;
  if (data && typeof data === 'object' && 'content' in data) {
    return (data as { content: IssueResponse[] }).content || [];
  }
  return Array.isArray(data) ? data : [];
}

async function fetchAllIssues(): Promise<IssueResponse[]> {
  const response = await issueApi.getAll();
  const data = response.data;
  if (data && typeof data === 'object' && 'content' in data) {
    return (data as { content: IssueResponse[] }).content || [];
  }
  return Array.isArray(data) ? data : [];
}

export function useProjectWorkspaceData(projectId: string | undefined) {
  const issuesQuery = useQuery({
    queryKey: ['ws-project-issues', projectId],
    queryFn: () => fetchIssuesForProject(projectId!),
    enabled: !!projectId,
    staleTime: 30000,
  });

  const sprintsQuery = useQuery({
    queryKey: ['ws-project-sprints', projectId],
    queryFn: () => sprintApi.getAll(projectId),
    enabled: !!projectId,
    staleTime: 30000,
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
    isLoading: issuesQuery.isLoading || sprintsQuery.isLoading,
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
