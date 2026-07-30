/**

 * Contextual navigation for project/program/plan routes.

 * Project items: avisysDcNavRegistry.ts (synced with ProjectDcSidebar).

 */



import { buildProjectNavItems } from './avisysDcNavRegistry';



export interface ContextNavItem {

  id?: string;

  name: string;

  path: string;

  /** NavLink `end` — match exact path only */

  end?: boolean;

}



export type ContextEntityType = 'project' | 'program' | 'plan';



export interface RouteContext {

  type: ContextEntityType;

  id: string;

}



const PROJECT_CTX = /^\/projects\/([^/]+)/;

const PROGRAM_CTX = /^\/programs\/([^/]+)/;

const PLAN_CTX = /^\/plans\/([^/]+)/;

const SKIP_IDS = new Set(['create']);



export function getRouteContext(pathname: string): RouteContext | null {

  const pm = pathname.match(PROJECT_CTX);

  if (pm && !SKIP_IDS.has(pm[1])) {

    return { type: 'project', id: pm[1] };

  }

  const gm = pathname.match(PROGRAM_CTX);

  if (gm && !SKIP_IDS.has(gm[1]) && gm[1] !== 'create') {

    return { type: 'program', id: gm[1] };

  }

  const plm = pathname.match(PLAN_CTX);

  if (plm && !SKIP_IDS.has(plm[1]) && plm[1] !== 'create') {

    return { type: 'plan', id: plm[1] };

  }

  return null;

}



export function isInProjectContext(pathname: string): boolean {

  return getRouteContext(pathname)?.type === 'project';

}



export function isInProgramContext(pathname: string): boolean {

  return getRouteContext(pathname)?.type === 'program';

}



/** Project sub-nav — same order as ProjectDcSidebar */

export function getProjectContextNav(

  projectId: string,

  template?: string,

  defaultBoardPath?: string,

): ContextNavItem[] {

  return buildProjectNavItems(projectId, template, defaultBoardPath).map((item) => ({

    id: item.id,

    name: item.label,

    path: item.path,

    end: item.end,

  }));

}



export function getProgramContextNav(programId: string): ContextNavItem[] {

  const base = `/programs/${programId}`;

  return [

    { name: 'Overview', path: `${base}?tab=overview`, end: true },

    { name: 'Schedule', path: `${base}?tab=schedule` },

    { name: 'Scope', path: `${base}?tab=scope` },

    { name: 'Portfolio', path: `${base}/portfolio` },

    { name: 'Settings', path: `${base}/settings` },

    { name: 'All plans', path: '/plans' },

  ];

}



export function getPlanContextNav(planId: string): ContextNavItem[] {

  const base = `/plans/${planId}`;

  return [

    { name: 'Roadmap', path: base, end: true },

    { name: 'Teams', path: `${base}?tab=teams` },

    { name: 'Releases', path: `${base}?tab=releases` },

    { name: 'Dependencies', path: `${base}?tab=dependencies` },

    { name: 'Settings', path: `${base}/settings` },

  ];

}



export function contextSectionLabel(type: ContextEntityType): string {

  if (type === 'project') return 'Current project';

  if (type === 'plan') return 'Current plan';

  return 'Current program';

}


