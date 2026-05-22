/**
 * Route metadata — document title + Jira DC-style breadcrumbs.
 */

export interface RouteMeta {
  title: string;
  breadcrumbs: { label: string; href?: string }[];
  area?: 'workspace' | 'admin';
}

function projectCrumbs(
  projectId: string,
  tail: { label: string; href?: string }[],
  projectLabel = 'Project',
) {
  return [
    { label: 'Projects', href: '/projects' },
    { label: projectLabel, href: `/projects/${projectId}` },
    ...tail,
  ];
}

export const ROUTE_META: { match: string | RegExp; meta: RouteMeta }[] = [
  { match: '/dashboard', meta: { area: 'workspace', title: 'Dashboard', breadcrumbs: [{ label: 'Dashboards' }] } },
  { match: /^\/projects\/create/, meta: { area: 'workspace', title: 'Create project', breadcrumbs: [{ label: 'Projects', href: '/projects' }, { label: 'Create' }] } },
  { match: /^\/projects\/([^/]+)\/settings\/([^/]+)/, meta: { area: 'workspace', title: 'Project settings', breadcrumbs: [] } },
  { match: /^\/projects\/([^/]+)\/backlog/, meta: { area: 'workspace', title: 'Backlog', breadcrumbs: [] } },
  { match: /^\/projects\/([^/]+)\/board/, meta: { area: 'workspace', title: 'Board', breadcrumbs: [] } },
  { match: /^\/projects\/([^/]+)\/releases/, meta: { area: 'workspace', title: 'Releases', breadcrumbs: [] } },
  { match: /^\/projects\/([^/]+)\/reports/, meta: { area: 'workspace', title: 'Reports', breadcrumbs: [] } },
  { match: /^\/projects\/([^/]+)\/components/, meta: { area: 'workspace', title: 'Components', breadcrumbs: [] } },
  { match: /^\/projects\/([^/]+)\/issues/, meta: { area: 'workspace', title: 'Issues', breadcrumbs: [] } },
  { match: /^\/projects\/([^/]+)/, meta: { area: 'workspace', title: 'Project', breadcrumbs: [] } },
  { match: '/projects', meta: { area: 'workspace', title: 'Projects', breadcrumbs: [{ label: 'Projects' }] } },
  { match: /^\/issues\/[^/]+/, meta: { area: 'workspace', title: 'Issue', breadcrumbs: [{ label: 'Issues', href: '/issues' }, { label: 'Issue' }] } },
  { match: '/issues/batch', meta: { area: 'workspace', title: 'Batch lookup', breadcrumbs: [{ label: 'Issues', href: '/issues' }, { label: 'Batch lookup' }] } },
  { match: '/issues', meta: { area: 'workspace', title: 'Issues', breadcrumbs: [{ label: 'Issues' }] } },
  { match: '/epics', meta: { area: 'workspace', title: 'Epics', breadcrumbs: [{ label: 'More' }, { label: 'Epics' }] } },
  { match: '/kanban', meta: { area: 'workspace', title: 'Kanban', breadcrumbs: [{ label: 'Boards', href: '/boards' }, { label: 'Kanban' }] } },
  { match: '/boards', meta: { area: 'workspace', title: 'Boards', breadcrumbs: [{ label: 'Boards' }] } },
  { match: '/board/classic', meta: { area: 'workspace', title: 'Classic board', breadcrumbs: [{ label: 'Boards', href: '/boards' }, { label: 'Classic' }] } },
  { match: '/sprints', meta: { area: 'workspace', title: 'Sprints', breadcrumbs: [{ label: 'Boards', href: '/boards' }, { label: 'Sprints' }] } },
  { match: '/workflows/admin', meta: { area: 'workspace', title: 'Workflow administration', breadcrumbs: [{ label: 'Administration', href: '/admin' }, { label: 'Workflows', href: '/workflows' }, { label: 'Admin API' }] } },
  { match: '/workflows', meta: { area: 'workspace', title: 'Workflows', breadcrumbs: [{ label: 'Workflows' }] } },
  { match: '/search', meta: { area: 'workspace', title: 'Search', breadcrumbs: [{ label: 'Search' }] } },
  { match: '/notifications', meta: { area: 'workspace', title: 'Notifications', breadcrumbs: [{ label: 'Notifications' }] } },
  { match: '/audit', meta: { area: 'workspace', title: 'Audit logs', breadcrumbs: [{ label: 'More' }, { label: 'Audit logs' }] } },
  { match: '/migration', meta: { area: 'workspace', title: 'Migration Center', breadcrumbs: [{ label: 'More' }, { label: 'Migration' }] } },
  { match: '/tests', meta: { area: 'workspace', title: 'Xray Test Management', breadcrumbs: [{ label: 'Xray plugin' }] } },
  { match: /^\/programs\/create/, meta: { area: 'workspace', title: 'Create program', breadcrumbs: [{ label: 'Plans', href: '/plans' }, { label: 'Create program' }] } },
  { match: /^\/programs\/[^/]+/, meta: { area: 'workspace', title: 'Program', breadcrumbs: [{ label: 'Plans', href: '/plans' }, { label: 'Program' }] } },
  { match: '/programs', meta: { area: 'workspace', title: 'Programs', breadcrumbs: [{ label: 'Plans', href: '/plans' }, { label: 'Programs' }] } },
  { match: /^\/plans\/create/, meta: { area: 'workspace', title: 'Create plan', breadcrumbs: [{ label: 'Plans', href: '/plans' }, { label: 'Create' }] } },
  { match: /^\/plans\/[^/]+/, meta: { area: 'workspace', title: 'Plan', breadcrumbs: [{ label: 'Plans', href: '/plans' }, { label: 'Plan' }] } },
  { match: '/plans', meta: { area: 'workspace', title: 'Plans', breadcrumbs: [{ label: 'Plans' }] } },
  { match: '/admin', meta: { area: 'admin', title: 'Administration', breadcrumbs: [{ label: 'Administration' }] } },
];

const PROJECT_SECTION_LABELS: Record<string, string> = {
  backlog: 'Backlog',
  board: 'Board',
  releases: 'Releases',
  reports: 'Reports',
  components: 'Components',
  issues: 'Issues',
  settings: 'Project settings',
};

export interface MetaContext {
  entityLabel?: string;
  entitySubtitle?: string;
}

export function metaFor(pathname: string, ctx?: MetaContext): RouteMeta {
  for (const { match, meta } of ROUTE_META) {
    if (typeof match === 'string') {
      if (pathname === match || pathname.startsWith(match + '/')) {
        return enrichProjectMeta(pathname, meta, undefined, ctx);
      }
    } else {
      const m = pathname.match(match);
      if (m) {
        return enrichProjectMeta(pathname, meta, m, ctx);
      }
    }
  }
  if (pathname.startsWith('/admin')) {
    return { area: 'admin', title: 'Administration', breadcrumbs: [{ label: 'Administration', href: '/admin' }, { label: 'Configuration' }] };
  }
  if (pathname.startsWith('/tests')) {
    const tail = pathname === '/tests' ? 'Hub' : 'Module';
    return {
      area: 'workspace',
      title: 'Xray Test Management',
      breadcrumbs: [
        { label: 'Xray plugin', href: '/tests' },
        { label: tail },
      ],
    };
  }
  const issueMatch = pathname.match(/^\/issues\/([^/]+)/);
  if (issueMatch) {
    return {
      area: 'workspace',
      title: ctx?.entitySubtitle ?? 'Issue',
      breadcrumbs: [
        { label: 'Issues', href: '/issues' },
        { label: ctx?.entitySubtitle ?? issueMatch[1] },
      ],
    };
  }
  return { title: 'Systems and Avionics', breadcrumbs: [] };
}

function enrichProjectMeta(
  pathname: string,
  meta: RouteMeta,
  match?: RegExpMatchArray,
  ctx?: MetaContext,
): RouteMeta {
  const pm = pathname.match(/^\/projects\/([^/]+)(?:\/(.+))?/);
  if (!pm) return meta;
  const projectId = pm[1];
  const projectLabel = ctx?.entityLabel ?? 'Project';
  const rest = pm[2] ?? '';
  if (!rest || rest === '') {
    return {
      ...meta,
      title: ctx?.entityLabel ?? meta.title,
      breadcrumbs: projectCrumbs(projectId, [{ label: projectLabel }], projectLabel),
    };
  }
  if (rest.startsWith('settings/')) {
    const section = rest.split('/')[1] ?? 'summary';
    return {
      ...meta,
      title: 'Project settings',
      breadcrumbs: projectCrumbs(projectId, [
        { label: 'Project settings', href: `/projects/${projectId}/settings/summary` },
        { label: section.replace(/-/g, ' ') },
      ], projectLabel),
    };
  }
  const segment = rest.split('/')[0];
  const label = PROJECT_SECTION_LABELS[segment] ?? segment;
  return {
    ...meta,
    title: `${projectLabel} — ${label}`,
    breadcrumbs: projectCrumbs(projectId, [{ label }], projectLabel),
  };
}
