import { issueApi } from '../../../api/issueApi';

export function normalizeWorkflowStatus(s: string | undefined | null): string {
  if (!s) return '';
  return s
    .toLowerCase()
    .replace(/\(legacy\)/gi, '')
    .replace(/\(new\)/gi, '')
    .replace(/→|->/g, ' ')
    .replace(/[\s_-]+/g, '');
}

type TransitionItem = {
  id: string;
  name?: string;
  toStatusName?: string;
  toStatusId?: string;
  hasScreen?: boolean;
};

/** Map workflow status id -> display name (cached per call site). */
export async function loadStatusNameById(): Promise<Map<string, string>> {
  const map = new Map<string, string>();
  try {
    const { data: statuses } = await issueApi.getStatuses();
    for (const s of statuses) {
      if (s.id && s.name) map.set(s.id, s.name);
    }
  } catch {
    /* optional */
  }
  return map;
}

export function resolveTransitionTargetName(
  t: { name?: string; toStatusName?: string; toStatusId?: string },
  statusById?: Map<string, string>,
): string {
  const direct = t.toStatusName?.trim();
  if (direct) return direct;
  if (t.toStatusId && statusById?.has(t.toStatusId)) {
    return statusById.get(t.toStatusId)!;
  }
  const name = t.name ?? '';
  const parts = name.split(/→|->|→|\u2192|\u2013|\u2014| to /i);
  if (parts.length > 1) {
    return parts[parts.length - 1].trim();
  }
  return name.trim();
}

export function statusMatchesTarget(
  currentStatus: string | undefined | null,
  targetStatusLabel: string,
): boolean {
  const current = normalizeWorkflowStatus(currentStatus);
  const target = normalizeWorkflowStatus(targetStatusLabel);
  if (!current || !target) return false;
  if (current === target) return true;
  if (current.includes(target) || target.includes(current)) return true;

  const selectedAliases = ['todo', 'selectedfordevelopment', 'selected'];
  if (
    selectedAliases.some((a) => target.includes(a) || a === target) &&
    selectedAliases.some((a) => current.includes(a) || a === current)
  ) {
    return true;
  }

  const backlogAliases = ['backlog', 'open', 'new', 'defined'];
  if (
    backlogAliases.some((a) => target.includes(a)) &&
    backlogAliases.some((a) => current.includes(a))
  ) {
    return true;
  }

  return false;
}

/** Workflow column order for greedy multi-step moves (Kanban drag). */
function statusRank(statusName: string): number {
  const n = normalizeWorkflowStatus(statusName);
  if (n.includes('backlog') || n === 'open' || n === 'new' || n.includes('defined')) return 0;
  if (n.includes('todo') || n.includes('selected')) return 1;
  if (n.includes('progress') && !n.includes('review')) return 2;
  if (n.includes('review')) return 3;
  if (n.includes('done') || n.includes('closed') || n.includes('resolved') || n.includes('complete')) {
    return 4;
  }
  return 1;
}

export function pickTransitionTowardTarget(
  transitions: TransitionItem[],
  targetStatusLabel: string,
  statusById?: Map<string, string>,
): TransitionItem | undefined {
  const targetRank = statusRank(targetStatusLabel);
  const targetNorm = normalizeWorkflowStatus(targetStatusLabel);

  let best: TransitionItem | undefined;
  let bestScore = -Infinity;

  for (const t of transitions) {
    const dest = resolveTransitionTargetName(t, statusById);
    const destNorm = normalizeWorkflowStatus(dest);
    const destRank = statusRank(dest);

    let score = 0;
    if (destNorm === targetNorm || destNorm.includes(targetNorm) || targetNorm.includes(destNorm)) {
      score = 1000;
    } else if (statusMatchesTarget(dest, targetStatusLabel)) {
      score = 900;
    } else if (destRank <= targetRank) {
      score = 50 + destRank;
    } else {
      score = -1;
    }

    if (score > bestScore) {
      bestScore = score;
      best = t;
    }
  }

  return bestScore >= 0 ? best : undefined;
}

export type TransitionExecuteResult = {
  success: boolean;
  newStatusId?: string | null;
  error?: string;
};

/** Execute transition and throw when the engine reports success: false (HTTP 200 failures). */
export async function executeTransitionChecked(
  issueId: string,
  projectId: string,
  transitionId: string,
  screenPayload?: { comment?: string; screenInput?: Record<string, unknown> },
): Promise<TransitionExecuteResult> {
  const { data } = await issueApi.executeTransition({
    issueId,
    projectId,
    transitionId,
    comment: screenPayload?.comment,
    screenInput: screenPayload?.screenInput,
  });

  const result = data as TransitionExecuteResult & { error?: string };
  if (!result?.success) {
    throw new Error(result?.error || 'Workflow transition failed');
  }
  return result;
}
