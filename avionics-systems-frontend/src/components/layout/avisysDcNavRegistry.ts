/**
 * Avionics Systems navigation registry (SSOT).
 * UX hierarchy matches Avionics Systems; styling remains Airbus via AppShell tokens.
 *
 * Used by: contextNav, projectDcNav, routeMeta, top nav dropdowns.
 */

export interface DcNavLink {
  id: string;
  label: string;
  path: string;
  end?: boolean;
  scrumOnly?: boolean;
}

export const AVISYS_DC_PRIMARY_TOP_NAV = [
  { label: 'Dashboards', path: '/dashboard' },
  { label: 'Projects', path: '/projects' },
  { label: 'Issues', path: '/issues' },
  { label: 'Boards', path: '/boards' },
] as const;

/** Secondary top nav — Avionics Systems has no Tests/Migration in primary bar; kept discoverable here. */
export const AVISYS_DC_MORE_TOP_NAV = [
  { label: 'Plans & programs', path: '/plans', description: 'Advanced Roadmaps' },
  { label: 'Xray Test Management', path: '/tests', description: 'Xray plugin — tests & traceability' },
  { label: 'SYSDOPS V&V', path: '/aircraft-design/dashboard', description: 'Aircraft design — VVO, defects, baselines' },
  { label: 'Architecture', path: '/aircraft-design/architecture', description: 'System architecture documentation' },
  { label: 'VVM Cards (IFCS)', path: '/aircraft-design/vvm-cards', description: 'V&V Management strategy cards' },
  { label: 'IVV Cards (IFCS)', path: '/aircraft-design/ivv-cards', description: 'Formal validation/verification items' },
  { label: 'Groups (IFCS)', path: '/aircraft-design/groups', description: 'IFCS activity packaging' },
  { label: 'Migration Center', path: '/migration', description: 'Import, export, jobs' },
  { label: 'Epics', path: '/epics', description: 'Epic hierarchy' },
  { label: 'Workflow hub', path: '/workflows', description: 'Workflows & schemes' },
  { label: 'Global search', path: '/search', description: 'Issue & entity search' },
  { label: 'Platform audit', path: '/audit', description: 'Workspace audit trail' },
  { label: 'Administration', path: '/admin', description: 'System configuration' },
] as const;

export function isScrumLikeTemplate(template?: string, category?: string): boolean {
  const cat = (category ?? '').toLowerCase();
  if (cat === 'scrum') return true;
  const t = (template ?? '').toUpperCase();
  return !t || t === 'SCRUM' || t === 'PROJECT_MANAGEMENT';
}

/**
 * Project sidebar + workspace context rail (must stay in sync).
 * Order mirrors Avionics Systems project nav from DC walkthrough frames.
 */
export function buildProjectNavItems(
  projectId: string,
  template?: string,
  activeBoardPath?: string,
  category?: string,
): DcNavLink[] {
  const base = `/projects/${projectId}`;
  const scrumLike = isScrumLikeTemplate(template, category);
  const boardPath = activeBoardPath ?? `${base}/board/active`;
  const items: DcNavLink[] = [
    { id: 'overview', label: 'Overview', path: base, end: true },
  ];

  if (scrumLike) {
    items.push(
      { id: 'backlog', label: 'Backlog', path: `${base}/backlog`, scrumOnly: true },
      { id: 'active-sprint', label: 'Active sprints', path: boardPath, scrumOnly: true },
    );
  } else {
    items.push({ id: 'board', label: 'Kanban board', path: boardPath });
  }

  items.push(
    { id: 'releases', label: 'Releases', path: `${base}/releases` },
    { id: 'reports', label: 'Reports', path: `${base}/reports` },
    { id: 'issues', label: 'Issues', path: `${base}/issues` },
    { id: 'components', label: 'Components', path: `${base}/components` },
  );

  return items;
}

export function projectSettingsPath(projectId: string, section = 'summary'): string {
  return `/projects/${projectId}/settings/${section}`;
}

export const PROJECT_SETTINGS_NAV: { id: string; label: string; group?: string }[] = [
  { id: 'summary', label: 'Summary', group: 'Project settings' },
  { id: 'details', label: 'Details' },
  { id: 'issue-types', label: 'Issue types' },
  { id: 'workflows', label: 'Workflows' },
  { id: 'screens', label: 'Screens' },
  { id: 'fields', label: 'Fields' },
  { id: 'priorities', label: 'Priorities' },
  { id: 'users', label: 'Users and roles' },
  { id: 'components', label: 'Components' },
  { id: 'versions', label: 'Versions' },
  { id: 'permissions', label: 'Permissions' },
  { id: 'project-links', label: 'Project links', group: 'Advanced' },
  { id: 'audit', label: 'Audit log' },
  { id: 'reindex', label: 'Re-index project' },
];

/** Avionics Systems Administration left-nav order (Issues-first). */
export const AVISYS_DC_ADMIN_CATEGORY_ORDER = [
  'issues',
  'workflows',
  'fields',
  'projects',
  'users',
  'system',
  'addons',
  'dc',
  'audit',
  'testing',
] as const;
