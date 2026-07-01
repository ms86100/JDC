import type { BoardIssue } from '../../../api/boardApi';
import type { IssueResponse } from '../../../api/issueApi';
import { normalizeIssue } from '../../../api/issueMapper';

/** Map issue-service rows to board card shape for standalone Kanban mode. */
export function mapIssueToBoardIssue(issue: IssueResponse): BoardIssue {
  const n = normalizeIssue(issue as unknown as Record<string, unknown>);
  return {
    id: issue.id,
    issueKey: issue.issueKey ?? issue.id,
    title: issue.title ?? '',
    status: n.status || 'To Do',
    priority: n.priority || 'Medium',
    projectId: issue.projectId,
    assigneeId: issue.assigneeId,
    assigneeName: issue.assigneeName,
    reporterId: issue.reporterId,
    epicId: issue.epicId,
    epicName: issue.epicName,
    storyPoints: issue.storyPoints,
    labels: issue.labels ?? [],
    issueType: n.issueType || 'Task',
    created: issue.createdAt ?? '',
    updated: issue.updatedAt ?? '',
    sprintId: issue.sprintId,
    dueDate: issue.dueDate,
    rank: issue.rank,
  };
}
