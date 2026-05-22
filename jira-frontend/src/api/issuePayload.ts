import type { CreateIssueRequest, UpdateIssueRequest } from './issueApi';

const UUID_FIELDS = [
  'projectId',
  'issueTypeId',
  'priorityId',
  'assigneeId',
  'reporterId',
  'parentIssueId',
  'parentId',
  'epicId',
  'sprintId',
  'securityLevelId',
  'statusId',
  'resolutionId',
] as const;

function stripEmptyUuids<T extends Record<string, unknown>>(obj: T): T {
  const out = { ...obj };
  for (const key of UUID_FIELDS) {
    const v = out[key];
    if (v === '' || v === null || v === undefined) {
      delete out[key];
    }
  }
  return out;
}

/** Maps frontend create form fields to backend CreateIssueRequest JSON. */
export function toBackendCreatePayload(
  data: CreateIssueRequest & { linkedIssues?: unknown; parentId?: string }
): Record<string, unknown> {
  const {
    linkedIssues: _linked,
    parentId,
    originalEstimateSeconds,
    remainingEstimateSeconds,
    timeSpent,
    fixVersionIds,
    affectsVersionIds,
    componentIds,
    ...rest
  } = data;

  const payload: Record<string, unknown> = {
    ...rest,
    parentIssueId: rest.parentIssueId || parentId,
    originalEstimate: originalEstimateSeconds ?? (rest as { originalEstimate?: number }).originalEstimate,
    remainingEstimate: remainingEstimateSeconds ?? (rest as { remainingEstimate?: number }).remainingEstimate,
    timeSpent: (rest as { timeSpentSeconds?: number }).timeSpentSeconds ?? timeSpent,
    fixVersions: fixVersionIds?.length ? fixVersionIds : undefined,
    affectsVersions: affectsVersionIds?.length ? affectsVersionIds : undefined,
    componentIds: componentIds?.length ? componentIds : undefined,
  };

  delete payload.originalEstimateSeconds;
  delete payload.remainingEstimateSeconds;
  delete payload.timeSpentSeconds;
  delete payload.fixVersionIds;
  delete payload.affectsVersionIds;
  delete payload.sprintId;
  delete payload.classification;
  delete payload.environment;
  delete payload.teamId;
  delete payload.reporterId;

  return stripEmptyUuids(payload);
}

/** Maps frontend update payload to backend UpdateIssueRequest JSON. */
export function toBackendUpdatePayload(data: UpdateIssueRequest & Record<string, unknown>): Record<string, unknown> {
  const {
    originalEstimateSeconds,
    remainingEstimateSeconds,
    timeSpentSeconds,
    fixVersionIds,
    affectsVersionIds,
    priority,
    status,
    issueType,
    ...rest
  } = data;

  const payload: Record<string, unknown> = {
    ...rest,
    originalEstimate: originalEstimateSeconds ?? data.originalEstimate,
    remainingEstimate: remainingEstimateSeconds ?? data.remainingEstimate,
    timeSpent: timeSpentSeconds ?? data.timeSpent,
    fixVersions: fixVersionIds?.length ? fixVersionIds : data.fixVersions,
    affectsVersions: affectsVersionIds?.length ? affectsVersionIds : data.affectsVersions,
    componentIds: data.componentIds?.length ? data.componentIds : undefined,
    environment: data.environment,
    priorityId: data.priorityId ?? undefined,
    statusId: data.statusId ?? undefined,
    issueTypeId: data.issueTypeId ?? undefined,
    securityLevelId: data.securityLevelId ?? undefined,
  };

  delete payload.originalEstimateSeconds;
  delete payload.remainingEstimateSeconds;
  delete payload.timeSpentSeconds;
  delete payload.fixVersionIds;
  delete payload.affectsVersionIds;
  delete payload.priority;
  delete payload.status;
  delete payload.issueType;
  delete payload.projectId;
  delete payload.linkedIssues;
  delete payload.comment;

  return stripEmptyUuids(payload);
}
