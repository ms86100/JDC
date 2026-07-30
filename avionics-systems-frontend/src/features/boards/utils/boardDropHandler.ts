import type { BoardColumn, BoardIssue } from '../../../api/boardApi';
import { issueApi } from '../../../api/issueApi';
import { transitionIssueToTargetStatus } from '../../issues/utils/boardWorkflowTransition';
import {
  executeTransitionChecked,
  loadStatusNameById,
  normalizeWorkflowStatus,
  resolveTransitionTargetName,
} from '../../issues/utils/workflowExecuteUtils';
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

function transitionMatchesColumn(
  t: AvailableTransition,
  normTarget: string,
  normColumn: string,
  statusById?: Map<string, string>,
): boolean {
  const toName = normalizeWorkflowStatus(t.toStatusName ?? '');
  const name = normalizeWorkflowStatus(t.name ?? '');

  const parsedTarget = normalizeWorkflowStatus(resolveTransitionTargetName(t, statusById));
  if (parsedTarget) {
    if (
      parsedTarget === normTarget ||
      normTarget.includes(parsedTarget) ||
      parsedTarget.includes(normTarget)
    ) {
      return true;
    }
    if (
      parsedTarget === normColumn ||
      normColumn.includes(parsedTarget) ||
      parsedTarget.includes(normColumn)
    ) {
      return true;
    }
  }

  if (toName) {
    if (toName === normTarget || normTarget.includes(toName) || toName.includes(normTarget)) {
      return true;
    }
    if (toName === normColumn || normColumn.includes(toName) || toName.includes(normColumn)) {
      return true;
    }
  }

  return (
    name.includes(normTarget) ||
    normTarget.includes(name) ||
    name.includes(normColumn) ||
    normColumn.includes(name)
  );
}

export async function findTransitionForColumn(
  issue: BoardIssue,
  projectId: string,
  column: BoardColumn,
): Promise<{ transition?: AvailableTransition; targetStatus: string }> {
  const targetStatus = targetStatusForColumn(column);
  const normTarget = normalizeWorkflowStatus(targetStatus);
  const normColumn = normalizeWorkflowStatus(column.name);

  try {
    const statusById = await loadStatusNameById();
    const { data } = await issueApi.getAvailableTransitions(issue.id, projectId);
    const transitions = (data as { transitions?: AvailableTransition[] }).transitions ?? [];

    const match = transitions.find((t) =>
      transitionMatchesColumn(t, normTarget, normColumn, statusById),
    );

    if (match) {
      const resolved = resolveTransitionTargetName(match, statusById).trim() || targetStatus;
      return { transition: match, targetStatus: resolved };
    }
  } catch (err) {
    console.log('[findTransition] Error fetching transitions:', err);
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

  const currentNormStatus = normalizeWorkflowStatus(issue.status ?? '');
  const targetNormStatus = normalizeWorkflowStatus(targetStatus);
  const statusChanged =
    currentNormStatus !== targetNormStatus &&
    !currentNormStatus.includes(targetNormStatus) &&
    !targetNormStatus.includes(currentNormStatus);

  if (statusChanged) {
    if (transition?.id) {
      try {
        await executeTransitionChecked(issue.id, projectId, transition.id, screenPayload);
        if (sorted.length > 0 || rank) {
          await boardApi.reorderIssue(boardId, issue.id, safeIndex, targetStatus);
        }
        return;
      } catch (err) {
        console.log('[executeBoardDrop] Single transition failed, trying multi-step:', err);
      }
    }

    await transitionIssueToTargetStatus(issue.id, projectId, targetStatus);
    if (sorted.length > 0 || rank) {
      await boardApi.reorderIssue(boardId, issue.id, safeIndex, targetStatus);
    }
    return;
  }

  if (sorted.length > 0) {
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
  const statusChanged =
    normalizeWorkflowStatus(issue.status ?? '') !== normalizeWorkflowStatus(targetStatus);

  if (!statusChanged) return;

  if (transition?.id) {
    try {
      await executeTransitionChecked(issue.id, projectId, transition.id, screenPayload);
      return;
    } catch {
      /* multi-step fallback */
    }
  }

  await transitionIssueToTargetStatus(issue.id, projectId, targetStatus);
}
