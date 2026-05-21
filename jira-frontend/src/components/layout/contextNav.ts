/**
 * Contextual navigation for project/program detail routes.
 */

export interface ContextNavItem {
  name: string;
  path: string;
  /** NavLink `end` — match exact path only */
  end?: boolean;
}

export type ContextEntityType = 'project' | 'program';

export interface RouteContext {
  type: ContextEntityType;
  id: string;
}

const PROJECT_CTX = /^\/projects\/([^/]+)/;
const PROGRAM_CTX = /^\/programs\/([^/]+)/;
const SKIP_IDS = new Set(['create']);

export function getRouteContext(pathname: string): RouteContext | null {
  const pm = pathname.match(PROJECT_CTX);
  if (pm && !SKIP_IDS.has(pm[1])) {
    return { type: 'project', id: pm[1] };
  }
  const gm = pathname.match(PROGRAM_CTX);
  if (gm && !SKIP_IDS.has(gm[1])) {
    return { type: 'program', id: gm[1] };
  }
  return null;
}

export function isInProjectContext(pathname: string): boolean {
  return getRouteContext(pathname)?.type === 'project';
}

export function isInProgramContext(pathname: string): boolean {
  return getRouteContext(pathname)?.type === 'program';
}

/** Project sub-nav — template adjusts emphasis (sprints for Scrum, etc.) */
export function getProjectContextNav(
  projectId: string,
  template?: string,
  defaultBoardPath?: string,
): ContextNavItem[] {
  const base = `/projects/${projectId}`;
  const items: ContextNavItem[] = [
    { name: 'Overview', path: base, end: true },
    { name: 'Board', path: defaultBoardPath ?? '/boards' },
    { name: 'Issues', path: `/issues?projectId=${projectId}` },
  ];

  const scrumLike = !template || template === 'SCRUM' || template === 'PROJECT_MANAGEMENT';
  if (scrumLike) {
    items.push({ name: 'Sprints', path: `/sprints?projectId=${projectId}` });
  }

  items.push(
    { name: 'Workflows', path: '/workflows' },
    { name: 'Settings', path: `${base}/settings` },
  );

  return items;
}

export function getProgramContextNav(programId: string): ContextNavItem[] {
  const base = `/programs/${programId}`;
  return [
    { name: 'Overview', path: base, end: true },
    { name: 'Plans', path: `${base}?tab=plans` },
    { name: 'Initiatives', path: `${base}?tab=initiatives` },
    { name: 'All plans', path: '/plans' },
    { name: 'Create plan', path: '/plans/create' },
  ];
}

export function contextSectionLabel(type: ContextEntityType): string {
  return type === 'project' ? 'Current project' : 'Current program';
}
