/**

 * Jira DC project sidebar navigation (Systems walkthrough parity).

 * SSOT: jiraDcNavRegistry.ts

 */



import {

  buildProjectNavItems,

  projectSettingsPath,

  PROJECT_SETTINGS_NAV,

  type DcNavLink,

} from '../../components/layout/jiraDcNavRegistry';



export type { DcNavLink as ProjectDcNavItem };



export function getProjectDcNav(

  projectId: string,

  template?: string,

  activeBoardPath?: string,

  category?: string,

): DcNavLink[] {

  return buildProjectNavItems(projectId, template, activeBoardPath, category);

}



export { projectSettingsPath, PROJECT_SETTINGS_NAV };



/** True when the in-project DC sidebar should render (overview + all sub-routes). */

export function isProjectDcSubRoute(pathname: string, projectId: string): boolean {
  const base = `/projects/${projectId}`;
  return pathname === base || pathname.startsWith(`${base}/`);
}



export type ProjectSettingsSection =

  | 'summary'

  | 'details'

  | 'issue-types'

  | 'workflows'

  | 'screens'

  | 'fields'

  | 'priorities'

  | 'users'

  | 'components'

  | 'versions'

  | 'permissions'

  | 'project-links'

  | 'reindex'

  | 'audit';



export const PROJECT_SETTINGS_SECTIONS = PROJECT_SETTINGS_NAV as {

  id: ProjectSettingsSection;

  label: string;

  group?: string;

}[];


