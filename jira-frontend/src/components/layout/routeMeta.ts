/**
 * Systems and Avionics — route metadata.
 * Single source of truth for document title and breadcrumb trail.
 * Matches against location.pathname using longest-prefix semantics.
 */

import { ADMIN_CATEGORIES } from './adminCategories';

export interface RouteMeta {
  title: string;
  breadcrumbs: { label: string; href?: string }[];
  area?: 'workspace' | 'admin';
}

// Ordered longest-first; first match wins.
export const ROUTE_META: { match: string | RegExp; meta: RouteMeta }[] = [
  // Workspace
  { match: '/dashboard',         meta: { area: 'workspace', title: 'Dashboard',     breadcrumbs: [{ label: 'Dashboard' }] } },
  { match: /^\/projects\/create/,   meta: { area: 'workspace', title: 'Create project',breadcrumbs: [{ label: 'Projects', href: '/projects' }, { label: 'Create' }] } },
  { match: /^\/projects\/[^/]+\/settings/, meta: { area: 'workspace', title: 'Project settings', breadcrumbs: [{ label: 'Projects', href: '/projects' }, { label: 'Settings' }] } },
  { match: /^\/projects\/[^/]+/,    meta: { area: 'workspace', title: 'Project',     breadcrumbs: [{ label: 'Projects', href: '/projects' }, { label: 'Detail' }] } },
  { match: '/projects',          meta: { area: 'workspace', title: 'Projects',      breadcrumbs: [{ label: 'Projects' }] } },
  { match: /^\/issues\/[^/]+/,      meta: { area: 'workspace', title: 'Issue',        breadcrumbs: [{ label: 'Issues', href: '/issues' }, { label: 'Detail' }] } },
  { match: '/issues',            meta: { area: 'workspace', title: 'Issues',        breadcrumbs: [{ label: 'Issues' }] } },
  { match: '/kanban',            meta: { area: 'workspace', title: 'Kanban',        breadcrumbs: [{ label: 'Boards', href: '/boards' }, { label: 'Kanban' }] } },
  { match: '/boards',            meta: { area: 'workspace', title: 'Boards',        breadcrumbs: [{ label: 'Boards' }] } },
  { match: '/board/classic',     meta: { area: 'workspace', title: 'Classic board', breadcrumbs: [{ label: 'Boards', href: '/boards' }, { label: 'Classic' }] } },
  { match: '/sprints',           meta: { area: 'workspace', title: 'Sprints',       breadcrumbs: [{ label: 'Sprints' }] } },
  { match: '/workflows',         meta: { area: 'workspace', title: 'Workflows',     breadcrumbs: [{ label: 'Workflows' }] } },
  { match: '/search',            meta: { area: 'workspace', title: 'Search',        breadcrumbs: [{ label: 'Search' }] } },
  { match: '/notifications',     meta: { area: 'workspace', title: 'Notifications', breadcrumbs: [{ label: 'Notifications' }] } },
  { match: '/audit',             meta: { area: 'workspace', title: 'Audit logs',    breadcrumbs: [{ label: 'Operations' }, { label: 'Audit logs' }] } },
  { match: '/migration',         meta: { area: 'workspace', title: 'Migration',     breadcrumbs: [{ label: 'Operations' }, { label: 'Migration' }] } },
  { match: /^\/programs\/create/,   meta: { area: 'workspace', title: 'Create program',breadcrumbs: [{ label: 'Plans', href: '/programs' }, { label: 'Create program' }] } },
  { match: /^\/programs\/[^/]+/,    meta: { area: 'workspace', title: 'Program',      breadcrumbs: [{ label: 'Plans', href: '/programs' }, { label: 'Program' }] } },
  { match: '/programs',          meta: { area: 'workspace', title: 'Programs',      breadcrumbs: [{ label: 'Plans' }, { label: 'Programs' }] } },
  { match: /^\/plans\/create/,      meta: { area: 'workspace', title: 'Create plan',   breadcrumbs: [{ label: 'Plans', href: '/plans' }, { label: 'Create' }] } },
  { match: /^\/plans\/[^/]+/,       meta: { area: 'workspace', title: 'Plan',         breadcrumbs: [{ label: 'Plans', href: '/plans' }, { label: 'Detail' }] } },
  { match: '/plans',             meta: { area: 'workspace', title: 'Plans',         breadcrumbs: [{ label: 'Plans' }] } },
];

function adminMetaFor(pathname: string): RouteMeta | null {
  if (!pathname.startsWith('/admin')) return null;

  if (pathname === '/admin' || pathname === '/admin/') {
    return {
      area: 'admin',
      title: 'Administration',
      breadcrumbs: [{ label: 'Administration' }],
    };
  }

  let pageLabel = 'Settings';
  let categoryLabel = 'Administration';

  for (const cat of ADMIN_CATEGORIES) {
    for (const it of cat.items) {
      if (pathname === it.path || pathname.startsWith(`${it.path}/`)) {
        pageLabel = it.label;
        categoryLabel = cat.label;
        break;
      }
    }
  }

  return {
    area: 'admin',
    title: pageLabel,
    breadcrumbs: [
      { label: 'Administration', href: '/admin' },
      { label: categoryLabel },
      { label: pageLabel },
    ],
  };
}

export function metaFor(pathname: string): RouteMeta {
  const adminMeta = adminMetaFor(pathname);
  if (adminMeta) return adminMeta;

  for (const { match, meta } of ROUTE_META) {
    if (typeof match === 'string') {
      if (pathname === match || pathname.startsWith(match + '/')) return meta;
    } else if (match.test(pathname)) {
      return meta;
    }
  }
  return { title: 'Systems and Avionics', breadcrumbs: [] };
}
