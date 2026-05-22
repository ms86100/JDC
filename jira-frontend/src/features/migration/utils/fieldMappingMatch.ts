/** Normalize CSV/Excel headers the same way as migration-service (snake_case). */
export function normalizeMigrationHeader(header: string): string {
  return header
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '_')
    .replace(/[^a-z0-9_]/g, '');
}

const JIRA_CSV_ALIASES: Record<string, string[]> = {
  summary: ['summary', 'title', 'subject'],
  description: ['description', 'desc', 'body', 'details', 'content'],
  issuetype: ['issuetype', 'issue_type', 'type'],
  issueKey: ['issuekey', 'issue_key', 'key'],
  priority: ['priority', 'prio', 'importance', 'severity'],
  project: ['project', 'proj', 'project_key', 'projectkey'],
  project_key: ['project_key', 'projectkey'],
  status: ['status', 'state', 'workflow_status'],
  assignee: ['assignee', 'assigned_to', 'assignedto'],
  reporter: ['reporter', 'reported_by', 'reportedby'],
  labels: ['labels', 'tags'],
};

/** Strict header→target matching (avoids "Project name" → summary via substring "name"). */
export function matchHeaderToTargetField(
  header: string,
  targetFieldKeys: string[]
): string | null {
  const normalized = normalizeMigrationHeader(header);
  if (!normalized) return null;

  for (const [targetField, aliases] of Object.entries(JIRA_CSV_ALIASES)) {
    if (aliases.some((alias) => alias === normalized)) {
      return targetField;
    }
  }

  for (const field of targetFieldKeys) {
    const normField = normalizeMigrationHeader(field);
    if (normField && normField === normalized) {
      return field;
    }
  }

  return null;
}
