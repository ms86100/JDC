/** Normalize CSV/Excel headers the same way as migration-service (snake_case). */
export function normalizeMigrationHeader(header: string): string {
  if (!header) return '';
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
  issueId: ['issue_id', 'issueid'],
  priority: ['priority', 'prio'],
  project: ['project', 'proj', 'project_key', 'projectkey'],
  project_key: ['project_key', 'projectkey'],
  status: ['status', 'state', 'workflow_status'],
  assignee: ['assignee', 'assigned_to', 'assignedto'],
  reporter: ['reporter', 'reported_by', 'reportedby'],
  creator: ['creator'],
  labels: ['labels', 'tags', 'label'],
  resolution: ['resolution'],
  dueDate: ['due_date', 'duedate'],
  created: ['created', 'created_at', 'creation_date'],
  updated: ['updated', 'updated_at'],
  resolved: ['resolved', 'resolved_at', 'resolution_date'],
  components: ['components', 'component', 'component_s'],
  fixVersions: ['fix_versions', 'fix_version_s', 'fixversion'],
  affectsVersions: ['affects_versions', 'affects_version_s', 'affectsversion'],
  environment: ['environment', 'env'],
  storyPoints: ['story_points', 'storypoints'],
  sprint: ['sprint', 'sprint_name'],
  epicLink: ['epic_link', 'epiclink'],
  epicName: ['epic_name', 'epicname'],
  parent: ['parent', 'parent_link', 'parent_issue'],
  attachment: ['attachment', 'attachments'],
  securityLevel: ['security_level', 'security'],
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
