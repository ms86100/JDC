import { issueApi } from '../../../api/issueApi';
import {
  executeTransitionChecked,
  loadStatusNameById,
  normalizeWorkflowStatus,
  pickTransitionTowardTarget,
  resolveTransitionTargetName,
  statusMatchesTarget,
} from './workflowExecuteUtils';

async function fetchIssueStatusName(issueId: string): Promise<string> {
  try {
    const { data } = await issueApi.getById(issueId);
    const row = data as { status?: string; statusName?: string };
    const status =
      typeof row.status === 'string' ? row.status : (row.statusName ?? '');
    return status.trim();
  } catch {
    return '';
  }
}

/**
 * Move an issue to a target status via workflow transitions (multi-step when needed).
 */
export async function transitionIssueToTargetStatus(
  issueId: string,
  projectId: string,
  targetStatusLabel: string,
): Promise<void> {
  let currentStatus = await fetchIssueStatusName(issueId);

  if (statusMatchesTarget(currentStatus, targetStatusLabel)) {
    return;
  }

  const statusById = await loadStatusNameById();

  for (let step = 0; step < 8; step++) {
    if (statusMatchesTarget(currentStatus, targetStatusLabel)) {
      return;
    }

    const { data } = await issueApi.getAvailableTransitions(issueId, projectId);
    const transitions = data.transitions ?? [];
    if (!transitions.length) {
      break;
    }

    const match = pickTransitionTowardTarget(transitions, targetStatusLabel, statusById);
    if (!match?.id) {
      break;
    }

    const destLabel = resolveTransitionTargetName(match, statusById);
    if (
      statusMatchesTarget(currentStatus, destLabel) &&
      !statusMatchesTarget(destLabel, targetStatusLabel)
    ) {
      break;
    }

    try {
      await executeTransitionChecked(issueId, projectId, match.id);
      currentStatus = destLabel || (await fetchIssueStatusName(issueId));
    } catch {
      break;
    }
  }

  currentStatus = await fetchIssueStatusName(issueId);
  if (statusMatchesTarget(currentStatus, targetStatusLabel)) {
    return;
  }

  throw new Error(
    `Cannot move issue to "${targetStatusLabel}". No workflow transition from "${currentStatus || 'current status'}".`,
  );
}
