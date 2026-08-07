/**

 * Workspace nav — categories + flyout items (paths must match App.tsx routes).

 */



import {

  migrationPath,

  migrationQueryMatches,

} from '../../features/migration/utils/migrationDeepLinks';



export interface WorkspaceNavItem {

  label: string;

  path: string;

  description?: string;

}



export interface WorkspaceNavCategory {

  key: string;

  label: string;

  icon: string;

  items: WorkspaceNavItem[];

}



/** Work area — hover flyout per category (intent-based grouping). */

export const WORKSPACE_NAV_CATEGORIES: WorkspaceNavCategory[] = [

  {

    key: 'planning',

    label: 'Planning',

    icon: '📊',

    items: [

      { label: 'Dashboard', path: '/dashboard', description: 'Home & activity overview' },

      { label: 'All projects', path: '/projects', description: 'Browse and open projects' },

      { label: 'Create project', path: '/projects/create', description: 'New project wizard' },

      { label: 'Plans (programs)', path: '/programs', description: 'Advanced Roadmaps programs' },

      { label: 'Create program', path: '/programs/create', description: 'New program' },

      { label: 'Manage plans', path: '/plans', description: 'Plans across programs' },

      { label: 'Create plan', path: '/plans/create', description: 'New delivery plan' },

      {
        label: 'Plan detail (context)',
        path: '/plans',
        description: 'Open a plan from Manage plans — URL: /plans/:planId',
      },

      {
        label: 'Program detail (context)',
        path: '/programs',
        description: 'Open a program from Programs — URL: /programs/:programId',
      },

      {
        label: 'Project settings (context)',
        path: '/projects',
        description: 'Open a project → Settings — URL: /projects/:projectId/settings',
      },

    ],

  },

  {

    key: 'delivery',

    label: 'Delivery',

    icon: '📋',

    items: [

      { label: 'Issue navigator', path: '/issues', description: 'Search and filter issues' },

      { label: 'Batch issue lookup', path: '/issues/batch', description: 'Bulk issue key lookup' },

      { label: 'Epics', path: '/epics', description: 'Epic hierarchy' },
      {
        label: 'Epic detail (context)',
        path: '/epics',
        description: 'Open an epic from Epics list — URL: /epics/:epicId',
      },

      { label: 'Boards hub', path: '/boards', description: 'Board picker & management' },

      { label: 'Classic board', path: '/board/classic', description: 'Legacy kanban view' },

      { label: 'Kanban board', path: '/kanban', description: 'Quick kanban board' },

      { label: 'Sprints', path: '/sprints', description: 'Sprint planning & backlog' },

    ],

  },

  {

    key: 'workflows',

    label: 'Workflows',

    icon: '🔀',

    items: [

      { label: 'Workflow hub (app)', path: '/workflows', description: 'Create, publish, schemes' },
      { label: 'Workflows (administration)', path: '/admin/workflows', description: 'Avionics Systems Issues → Workflows' },
      { label: 'Workflow schemes', path: '/admin/workflow-schemes', description: 'Issue type → workflow mapping' },
      { label: 'Screens', path: '/admin/screens', description: 'Field screens' },
      { label: 'Open workflow…', path: '/workflows/open?view=designer', description: 'Picker → designer' },
      { label: 'Workflow API tools', path: '/workflows/admin/tools', description: 'Export, validate, migrate' },

    ],

  },

  {

    key: 'quality',

    label: 'Xray (Test Management)',

    icon: '🧪',

    items: [

      { label: 'Xray — Test repository', path: '/tests', description: 'Xray plugin home & project picker' },

      { label: 'Create test', path: '/tests/create', description: 'New manual or automated test' },

      { label: 'Screen configuration', path: '/tests/screen-config', description: 'Schemes, screens, fields' },

      { label: 'Test plugins', path: '/tests/plugins', description: 'Plugin registry & config' },

      { label: 'Defects', path: '/tests/defects', description: 'Defect tracking' },

      { label: 'Evidence gallery', path: '/tests/evidence', description: 'Run evidence' },

      { label: 'Shared steps', path: '/tests/shared-steps', description: 'Reusable steps' },

      { label: 'Datasets', path: '/tests/datasets', description: 'Parameterized data' },

      { label: 'Flaky tests', path: '/tests/flaky', description: 'Stability signals' },

      { label: 'Flaky dashboard', path: '/tests/flaky-dashboard', description: 'Advanced flaky analytics' },

      { label: 'Quarantine', path: '/tests/quarantine', description: 'Isolated tests' },

      { label: 'Coverage', path: '/tests/coverage', description: 'Requirement coverage' },

      { label: 'Traceability matrix', path: '/tests/traceability', description: 'Req ↔ test links' },

      { label: 'Preconditions', path: '/tests/preconditions', description: 'Precondition library' },

      { label: 'Timeline', path: '/tests/timeline', description: 'Schedule view' },

      { label: 'Requirement versions', path: '/tests/requirement-versions', description: 'Version matrix' },

      { label: 'Environment matrix', path: '/tests/environment-matrix', description: 'Env × browser' },

      { label: 'Test workflows', path: '/tests/workflows', description: 'Test workflow builder' },
      { label: 'Workflow builder (open)', path: '/tests/workflows/builder', description: 'Visual test workflow editor' },

      { label: 'Reporting', path: '/tests/reporting', description: 'Dashboards & metrics' },

      { label: 'Time tracking reports', path: '/reports/time-tracking', description: 'Logged time' },

      { label: 'Import tests', path: '/tests/import', description: 'Bulk test import' },

      { label: 'CI/CD webhooks', path: '/tests/webhooks', description: 'Pipeline hooks' },

      { label: 'AI assistant', path: '/tests/ai', description: 'AI test helpers' },

      { label: 'Impact analysis', path: '/tests/impact', description: 'Change impact' },

      { label: 'Exploratory testing', path: '/tests/exploratory', description: 'Charter-based sessions' },

      { label: 'Test settings', path: '/tests/settings', description: 'Project test config' },

      { label: 'Test configuration', path: '/tests/admin-config', description: 'Statuses, types, execution statuses' },

    ],

  },

  {

    key: 'sysdops',

    label: 'SYSDOPS (V&V)',

    icon: '✈',

    items: [

      { label: 'V&V Dashboard', path: '/aircraft-design/dashboard', description: 'Project V&V metrics overview' },

      { label: 'VVO Management', path: '/aircraft-design/vvos', description: 'Verification & Validation Objectives' },

      { label: 'HLVVO Packages', path: '/aircraft-design/hlvvos', description: 'High-Level V&V Objective grouping' },

      { label: 'Tech Events (M1668)', path: '/aircraft-design/tech-events', description: 'System anomaly management' },

      { label: 'Change Cards', path: '/aircraft-design/change-cards', description: 'Design change tracking (6-tab)' },

      { label: 'Baseline Management', path: '/aircraft-design/baselines', description: 'VVO baselines, DOORS, transfer' },

      { label: 'Campaign Creation', path: '/aircraft-design/campaigns', description: 'CSV-driven test campaigns' },

      { label: 'Master Data Admin', path: '/aircraft-design/master-data', description: 'Programs, systems, test means' },

      { label: 'Architecture', path: '/aircraft-design/architecture', description: 'System architecture documentation' },

      { label: 'VVM Cards (IFCS)', path: '/aircraft-design/vvm-cards', description: 'V&V Management strategy cards' },

      { label: 'IVV Cards (IFCS)', path: '/aircraft-design/ivv-cards', description: 'Formal validation/verification items' },

      { label: 'Groups (IFCS)', path: '/aircraft-design/groups', description: 'Activity packaging and deliverables' },

    ],

  },

  {

    key: 'tools',

    label: 'Tools',

    icon: '🔧',

    items: [

      { label: 'Global search', path: '/search', description: 'Issue & entity search' },

      { label: 'GraphQL explorer', path: '/developer/graphql', description: 'API query playground' },

      { label: 'Notifications', path: '/notifications', description: 'Inbox & alerts' },

    ],

  },

];



/** Operations — platform administration & data movement. */

export const WORKSPACE_NAV_OPERATIONS: WorkspaceNavCategory[] = [

  {

    key: 'administration',

    label: 'Administration',

    icon: '⚙',

    items: [

      { label: 'Admin home', path: '/admin', description: 'Configuration overview' },

      { label: 'Users', path: '/admin/users', description: 'User directory' },

      { label: 'Groups', path: '/admin/groups', description: 'Group membership' },

      { label: 'Roles', path: '/admin/roles', description: 'Role assignments' },

      { label: 'Sessions', path: '/admin/sessions', description: 'Active sessions' },

      { label: 'Issue types', path: '/admin/issue-types', description: 'Issue type catalog' },

      { label: 'Issue type schemes', path: '/admin/issue-type-schemes', description: 'Scheme mappings' },

      { label: 'Priorities', path: '/admin/priorities', description: 'Priority levels' },

      { label: 'Statuses', path: '/admin/statuses', description: 'Workflow statuses' },

      { label: 'Resolutions', path: '/admin/resolutions', description: 'Resolution values' },

      { label: 'Workflows (admin)', path: '/admin/workflows', description: 'System workflows' },

      { label: 'Screens (admin)', path: '/admin/screens', description: 'Field screens' },

      { label: 'Custom fields', path: '/admin/custom-fields', description: 'Field catalog' },

      { label: 'Application links', path: '/admin/application-links', description: 'External integrations' },

      { label: 'Automation', path: '/admin/automation', description: 'Rule automation' },

      { label: 'Directories', path: '/admin/directories', description: 'User directories' },
      { label: 'Import (system)', path: '/admin/system/import', description: 'System import' },
      { label: 'Cluster', path: '/admin/cluster', description: 'Data Center cluster' },
      { label: 'Cache', path: '/admin/cache', description: 'Cache management' },
      { label: 'Indexing', path: '/admin/indexing', description: 'Search indexing' },
      { label: 'Background jobs', path: '/admin/jobs', description: 'Scheduled jobs' },
      { label: 'Services', path: '/admin/services', description: 'Service status' },

      { label: 'System settings', path: '/admin/system/general', description: 'General settings' },

      { label: 'Reports & insights', path: '/admin/reports', description: 'Admin reports' },

      { label: 'Bulk operations', path: '/admin/bulk-create', description: 'Bulk user/issue create' },

    ],

  },

  {

    key: 'data',

    label: 'Migration & audit',

    icon: '📦',

    items: [

      { label: 'Migration Center', path: migrationPath(), description: 'Import wizard home' },

      { label: 'CSV import', path: migrationPath('wizard', 'csv'), description: 'Spreadsheet import' },

      { label: 'Avionics Systems backup import', path: migrationPath('wizard', 'legacy-dc'), description: 'Systems backup XML' },

      { label: 'Workflow XML import', path: migrationPath('wizard', 'workflow-xml'), description: 'Workflow descriptor XML' },

      { label: 'Project copy', path: migrationPath('wizard', 'project-import'), description: 'Project-to-project' },

      { label: 'Project export', path: migrationPath('wizard', 'project-export'), description: 'Export to file' },

      { label: 'Job history', path: migrationPath('history'), description: 'Jobs, rollback, reports' },

      { label: 'Platform health', path: migrationPath('health'), description: 'Services & cluster' },

      { label: 'Global DLQ', path: migrationPath('dlq'), description: 'Dead-letter queue' },

      { label: 'Mapping templates', path: migrationPath('templates'), description: 'Saved field mappings' },

      { label: 'Capability map', path: migrationPath('catalog'), description: 'Feature index' },

      { label: 'Import settings', path: migrationPath('settings'), description: 'Attachment limits, FILE: dir' },

      { label: 'Custom fields (admin)', path: '/admin/custom-fields', description: 'Field catalog CRUD' },

      { label: 'Platform audit logs', path: '/audit', description: 'Workspace audit trail' },

      { label: 'Admin audit', path: '/admin/auditing', description: 'Admin change log' },

    ],

  },

];



export function categoryForWorkspacePath(pathname: string, search: string): string | null {

  for (const cat of [...WORKSPACE_NAV_CATEGORIES, ...WORKSPACE_NAV_OPERATIONS]) {

    if (cat.items.some((it) => isWorkspaceItemActive(pathname, search, it.path))) {

      return cat.key;

    }

  }

  if (pathname.startsWith('/migration')) return 'data';

  if (pathname.startsWith('/admin')) return 'administration';

  if (pathname.startsWith('/tests')) return 'quality';

  return null;

}



export function isWorkspaceItemActive(pathname: string, search: string, itemPath: string): boolean {

  if (itemPath.startsWith('/migration')) {

    return migrationQueryMatches(pathname, search, itemPath);

  }



  const qIdx = itemPath.indexOf('?');

  const pathOnly = qIdx >= 0 ? itemPath.slice(0, qIdx) : itemPath;

  const itemQuery = qIdx >= 0 ? itemPath.slice(qIdx + 1) : '';



  if (itemQuery) {

    const normSearch = search.startsWith('?') ? search.slice(1) : search;

    return pathname === pathOnly && normSearch === itemQuery;

  }



  if (pathOnly === '/dashboard') {

    return pathname === '/' || pathname === '/dashboard' || pathname.startsWith('/dashboard/');

  }

  if (pathOnly === '/tests') {

    return pathname === '/tests';

  }

  if (pathOnly === '/workflows') {

    return pathname === '/workflows';

  }

  if (pathOnly === '/workflows/open') {

    return pathname === '/workflows/open';

  }

  if (pathOnly === '/workflows/admin') {

    return pathname.startsWith('/workflows/admin');

  }

  if (itemPath.includes('/workflows/open?')) {

    const normSearch = search.startsWith('?') ? search.slice(1) : search;

    const itemQs = itemPath.slice(itemPath.indexOf('?') + 1);

    return pathname === '/workflows/open' && normSearch === itemQs;

  }

  if (pathOnly === '/admin') {

    return pathname === '/admin' || pathname === '/admin/';

  }

  if (pathOnly === '/programs') {

    return pathname.startsWith('/programs');

  }

  if (pathOnly === '/tests/screen-config') {

    return pathname === '/tests/screen-config' || pathname.startsWith('/tests/screen-config/');

  }

  if (pathOnly === '/tests/flaky-dashboard') {

    return pathname.startsWith('/tests/flaky-dashboard');

  }

  if (pathOnly === '/tests/plugins') {

    return pathname.startsWith('/tests/plugins');

  }



  return pathname === pathOnly || pathname.startsWith(`${pathOnly}/`);

}


