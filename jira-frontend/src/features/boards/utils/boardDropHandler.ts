import type { BoardColumn, BoardIssue } from '../../../api/boardApi';
import { issueApi } from '../../../api/issueApi';
import { transitionIssueToTargetStatus } from '../../issues/utils/boardWorkflowTransition';
import { targetStatusForColumn } from './boardColumnUtils';
import { rankForIndex, sortIssuesByRank } from './boardRankUtils';
import type { AvailableTransition } from '../../issues/components/TransitionScreenForm';

export interface PendingBoardTransition {
  issue: BoardIssue;
  column: BoardColumn;
  targetStatus: string;
  transition: AvailableTransition;
  dropIndex: number;
  swimlaneKey?: string;
}

function normalizeStatus(s: string): string {
  return s.toLowerCase().replace(/[\s_-]+/g, '');
}

export async function findTransitionForColumn(
  issue: BoardIssue,
  projectId: string,
  column: BoardColumn,
): Promise<{ transition?: AvailableTransition; targetStatus: string }> {
  const targetStatus = targetStatusForColumn(column);
  try {
    const { data } = await issueApi.getAvailableTransitions(issue.id, projectId);
    const transitions = (data as { transitions?: AvailableTransition[] }).transitions ?? [];
    const normTarget = normalizeStatus(targetStatus);
    const match = transitions.find((t) => {
      const name = normalizeStatus(t.name ?? '');
      return name.includes(normTarget) || normTarget.includes(name);
    });
    if (match) {
      return { transition: match, targetStatus };
    }
  } catch {
    /* fallback below */
  }
  return { targetStatus };
}

export async function executeBoardDrop(
  boardId: string,
  projectId: string,
  issue: BoardIssue,
  column: BoardColumn,
  columnIssues: BoardIssue[],
  dropIndex: number,
  boardApi: {
    moveIssue: (b: string, i: string, status: string, rank?: string) => Promise<unknown>;
    reorderIssue: (b: string, i: string, index: number, status: string) => Promise<void>;
  },
  transition?: AvailableTransition,
  screenPayload?: { comment?: string; screenInput?: Record<string, unknown> },
): Promise<void> {
  const targetStatus = targetStatusForColumn(column);
  const sorted = sortIssuesByRank(columnIssues.filter((i) => i.id !== issue.id));
  const safeIndex = Math.max(0, Math.min(dropIndex, sorted.length));
  const rank = rankForIndex(safeIndex);

  const statusChanged = normalizeStatus(issue.status ?? '') !== normalizeStatus(targetStatus);

  if (statusChanged) {
    if (transition?.id) {
      await issueApi.executeTransition({
        issueId: issue.id,
        projectId,
        transitionId: transition.id,
        comment: screenPayload?.comment,
        screenInput: screenPayload?.screenInput,
      });
    } else {
      await transitionIssueToTargetStatus(issue.id, projectId, targetStatus);
    }
    await boardApi.moveIssue(boardId, issue.id, targetStatus, rank);
  } else {
    await boardApi.reorderIssue(boardId, issue.id, safeIndex, targetStatus);
  }
}

/** Drag/drop when no persisted agile board exists (workflow + rank via issue service only). */
export async function executeStandaloneDrop(
  projectId: string,
  issue: BoardIssue,
  column: BoardColumn,
  transition?: AvailableTransition,
  screenPayload?: { comment?: string; screenInput?: Record<string, unknown> },
): Promise<void> {
  const targetStatus = targetStatusForColumn(column);
  const statusChanged = normalizeStatus(issue.status ?? '') !== normalizeStatus(targetStatus);

  if (!statusChanged) return;

  if (transition?.id) {
    await issueApi.executeTransition({
      issueId: issue.id,
      projectId,
      transitionId: transition.id,
      comment: screenPayload?.comment,
      screenInput: screenPayload?.screenInput,
    });
    return;
  }

  await transitionIssueToTargetStatus(issue.id, projectId, targetStatus);
}
