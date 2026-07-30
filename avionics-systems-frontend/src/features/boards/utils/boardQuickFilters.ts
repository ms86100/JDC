import type { BoardIssue } from '../../../api/boardApi';

const ONE_DAY_MS = 24 * 60 * 60 * 1000;

/** Client-side quick filters when board JQL search is unavailable. */
export function applyBoardQuickFilter(
  issues: BoardIssue[],
  filterId: string,
  currentUserId?: string,
): BoardIssue[] {
  switch (filterId) {
    case 'qf-assigned-me':
      return currentUserId
        ? issues.filter((i) => i.assigneeId === currentUserId)
        : issues;
    case 'qf-reporter-me':
      return currentUserId
        ? issues.filter((i) => i.reporterId === currentUserId)
        : issues;
    case 'qf-recently-updated': {
      const cutoff = Date.now() - ONE_DAY_MS;
      return issues.filter((i) => {
        if (!i.updated) return false;
        const t = new Date(i.updated).getTime();
        return !Number.isNaN(t) && t >= cutoff;
      });
    }
    case 'qf-no-assignee':
      return issues.filter((i) => !i.assigneeId);
    case 'qf-has-due-date':
      return issues.filter((i) => !!i.dueDate);
    default:
      return issues;
  }
}
