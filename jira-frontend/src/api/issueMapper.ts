import type { IssueResponse } from './issueApi';

/** Maps backend IssueResponse field names to frontend-friendly aliases. */
export function normalizeIssue(raw: Record<string, unknown>): IssueResponse {
  const r = raw as IssueResponse & Record<string, unknown>;
  return {
    ...r,
    status: (r.status as string) || (raw.statusName as string) || '',
    priority: (r.priority as string) || (raw.priorityName as string) || '',
    issueType: (r.issueType as string) || (raw.issueTypeName as string) || '',
    statusId: r.statusId || (raw.statusId as string),
    priorityId: r.priorityId || (raw.priorityId as string),
    issueTypeId: r.issueTypeId || (raw.issueTypeId as string),
    securityLevel: r.securityLevel || (raw.securityLevelName as string),
    components: (r.components as string[]) || (raw.componentNames as string[]),
    labels: (r.labels as string[]) || [],
  };
}
