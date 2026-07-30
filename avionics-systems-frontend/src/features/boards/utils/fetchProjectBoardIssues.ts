import boardApi, { type BoardIssue } from '../../../api/boardApi';
import { issueApi, type IssueResponse } from '../../../api/issueApi';
import { normalizeIssue } from '../../../api/issueMapper';
import { asArray } from '../../../utils/apiList';
import { mapIssueToBoardIssue } from './mapIssueToBoardIssue';

/** Load issues for a project via issue-service (normalized status names). */
export async function fetchProjectBoardIssues(projectId: string): Promise<BoardIssue[]> {
  const response = await issueApi.getAll({ projectId, size: '500' });
  const list = asArray<IssueResponse>(response.data);
  return list.map((row) =>
    mapIssueToBoardIssue(normalizeIssue(row as unknown as Record<string, unknown>)),
  );
}

/**
 * Board API first; if empty or failing, fall back to issue-service list for the project.
 */
export async function fetchBoardIssuesWithFallback(
  boardId: string | null | undefined,
  projectId: string | undefined,
): Promise<BoardIssue[]> {
  if (boardId) {
    try {
      const fromBoard = await boardApi.getBoardIssues(boardId);
      if (fromBoard.length > 0) {
        return fromBoard.map((issue) => ({
          ...issue,
          status: issue.status?.trim() || 'To Do',
        }));
      }
    } catch {
      /* use issue API */
    }
  }
  if (projectId) {
    return fetchProjectBoardIssues(projectId);
  }
  return [];
}
