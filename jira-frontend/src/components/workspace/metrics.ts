import type { IssueResponse } from '../../api/issueApi';
import type { PlanResponse } from '../../api/planApi';
import type { SprintResponse } from '../../api/sprintApi';

export type HealthLevel = 'healthy' | 'at-risk' | 'critical' | 'unknown';

export interface WorkMetrics {
  total: number;
  done: number;
  inProgress: number;
  todo: number;
  blocked: number;
  completionPct: number;
  health: HealthLevel;
  overdue: number;
}

const DONE_STATUSES = new Set(['done', 'closed', 'resolved', 'complete', 'completed']);
const IN_PROGRESS_STATUSES = new Set(['in progress', 'in review', 'in development', 'active']);
const BLOCKED_PRIORITIES = new Set(['highest', 'critical', 'blocker', 'high']);

function normalizeStatus(status: string): string {
  return status.trim().toLowerCase();
}

function isDone(status: string): boolean {
  const s = normalizeStatus(status);
  return DONE_STATUSES.has(s) || s.includes('done') || s.includes('closed');
}

function isInProgress(status: string): boolean {
  const s = normalizeStatus(status);
  return IN_PROGRESS_STATUSES.has(s) || s.includes('progress') || s.includes('review');
}

function isBlocked(issue: IssueResponse): boolean {
  const p = (issue.priority || '').toLowerCase();
  return BLOCKED_PRIORITIES.has(p) && !isDone(issue.status);
}

function isOverdue(issue: IssueResponse): boolean {
  if (!issue.dueDate || isDone(issue.status)) return false;
  return new Date(issue.dueDate) < new Date();
}

export function computeWorkMetrics(issues: IssueResponse[]): WorkMetrics {
  if (!issues.length) {
    return {
      total: 0,
      done: 0,
      inProgress: 0,
      todo: 0,
      blocked: 0,
      completionPct: 0,
      health: 'unknown',
      overdue: 0,
    };
  }

  let done = 0;
  let inProgress = 0;
  let blocked = 0;
  let overdue = 0;

  for (const issue of issues) {
    if (isDone(issue.status)) done += 1;
    else if (isInProgress(issue.status)) inProgress += 1;
    if (isBlocked(issue)) blocked += 1;
    if (isOverdue(issue)) overdue += 1;
  }

  const total = issues.length;
  const todo = total - done - inProgress;
  const completionPct = Math.round((done / total) * 100);

  let health: HealthLevel = 'healthy';
  if (blocked >= 3 || overdue >= 5) health = 'critical';
  else if (blocked >= 1 || overdue >= 2 || completionPct < 40) health = 'at-risk';

  return { total, done, inProgress, todo, blocked, completionPct, health, overdue };
}

export function aggregatePlanMetrics(plans: PlanResponse[]): {
  totalItems: number;
  totalTeams: number;
  totalReleases: number;
  activePlans: number;
  nearestEndDate?: string;
} {
  let totalItems = 0;
  let totalTeams = 0;
  let totalReleases = 0;
  let activePlans = 0;
  let nearestEnd: Date | undefined;

  for (const plan of plans) {
    totalItems += plan.itemCount ?? 0;
    totalTeams += plan.teamCount ?? 0;
    totalReleases += plan.releaseCount ?? 0;
    if (plan.isActive) activePlans += 1;
    if (plan.endDate) {
      const d = new Date(plan.endDate);
      if (!nearestEnd || d < nearestEnd) nearestEnd = d;
    }
  }

  return {
    totalItems,
    totalTeams,
    totalReleases,
    activePlans,
    nearestEndDate: nearestEnd?.toISOString(),
  };
}

export function getActiveSprint(sprints: SprintResponse[]): SprintResponse | undefined {
  return sprints.find((s) => s.status === 'ACTIVE') ?? sprints.find((s) => s.status === 'PLANNING');
}

export function formatRelativeDate(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${Math.max(1, mins)}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

export function formatShortDate(dateStr?: string): string {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

export const HEALTH_LABELS: Record<HealthLevel, string> = {
  healthy: 'On track',
  'at-risk': 'At risk',
  critical: 'Needs attention',
  unknown: 'No data',
};
