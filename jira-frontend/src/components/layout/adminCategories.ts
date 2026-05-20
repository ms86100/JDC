/**
 * Systems and Avionics — admin nav categories (Atlassian-style 2-pane).
 * Paths match AdminRoutes.tsx routes exactly. Do NOT alter paths here
 * without updating AdminRoutes.tsx in lockstep.
 */

export interface AdminItem { label: string; path: string; }
export interface AdminCategory { key: string; label: string; icon: string; items: AdminItem[]; }

export const ADMIN_CATEGORIES: AdminCategory[] = [
  {
    key: 'system', label: 'System', icon: '⚙',
    items: [
      { label: 'General',        path: '/admin/system/general' },
      { label: 'Appearance',     path: '/admin/system/appearance' },
      { label: 'Attachments',    path: '/admin/system/attachments' },
      { label: 'Time tracking',  path: '/admin/system/time-tracking' },
      { label: 'Sub-tasks',      path: '/admin/system/subtasks' },
      { label: 'Licensing',      path: '/admin/system/licensing' },
      { label: 'System info',    path: '/admin/system/info' },
      { label: 'Reports',        path: '/admin/reports' },
      { label: 'Insights',       path: '/admin/insights' },
    ],
  },
  {
    key: 'users', label: 'Users & Security', icon: '👤',
    items: [
      { label: 'Users',              path: '/admin/users' },
      { label: 'Create user',        path: '/admin/users/create' },
      { label: 'Groups',             path: '/admin/groups' },
      { label: 'User management',    path: '/admin/user-management' },
      { label: 'Roles',              path: '/admin/roles' },
      { label: 'Permissions',        path: '/admin/permissions' },
      { label: 'Permission schemes', path: '/admin/permission-schemes' },
      { label: 'Notification schemes', path: '/admin/notification-schemes' },
      { label: 'Security schemes',   path: '/admin/security-schemes' },
      { label: 'Password policy',    path: '/admin/password-policy' },
      { label: 'Sessions',           path: '/admin/sessions' },
    ],
  },
  {
    key: 'issues', label: 'Issues', icon: '📋',
    items: [
      { label: 'Issue types',         path: '/admin/issue-types' },
      { label: 'Issue type schemes',  path: '/admin/issue-type-schemes' },
      { label: 'Priorities',          path: '/admin/priorities' },
      { label: 'Statuses',            path: '/admin/statuses' },
      { label: 'Resolutions',         path: '/admin/resolutions' },
      { label: 'Field configuration', path: '/admin/field-config' },
    ],
  },
  {
    key: 'workflows', label: 'Workflows & Screens', icon: '🔀',
    items: [
      { label: 'Workflows',        path: '/admin/workflows' },
      { label: 'Workflow schemes', path: '/admin/workflow-schemes' },
      { label: 'Screens',          path: '/admin/screens' },
      { label: 'Screen schemes',   path: '/admin/screen-schemes' },
    ],
  },
  {
    key: 'projects', label: 'Projects', icon: '📁',
    items: [
      { label: 'Project types',      path: '/admin/project-types' },
      { label: 'Project categories', path: '/admin/project-categories' },
      { label: 'Archetypes',         path: '/admin/archetypes' },
    ],
  },
  {
    key: 'fields', label: 'Custom Fields', icon: '🏷',
    items: [
      { label: 'Custom fields',  path: '/admin/custom-fields' },
      { label: 'Field types',    path: '/admin/field-types' },
      { label: 'Field contexts', path: '/admin/field-contexts' },
    ],
  },
  {
    key: 'addons', label: 'Add-ons & Automation', icon: '⚡',
    items: [
      { label: 'Automation', path: '/admin/automation' },
      { label: 'Webhooks',   path: '/admin/webhooks' },
      { label: 'Mail',       path: '/admin/mail' },
      { label: 'API',        path: '/admin/api' },
      { label: 'OAuth',      path: '/admin/oauth' },
      { label: 'Links',      path: '/admin/links' },
    ],
  },
  {
    key: 'dc', label: 'Data Center', icon: '🖥',
    items: [
      { label: 'Cluster',  path: '/admin/cluster' },
      { label: 'Cache',    path: '/admin/cache' },
      { label: 'Indexing', path: '/admin/indexing' },
      { label: 'Jobs',     path: '/admin/jobs' },
      { label: 'Services', path: '/admin/services' },
    ],
  },
  {
    key: 'audit', label: 'Audit', icon: '📊',
    items: [
      { label: 'Auditing',      path: '/admin/auditing' },
      { label: 'Dark features', path: '/admin/dark-features' },
      { label: 'Bulk operations', path: '/admin/bulk-create' },
    ],
  },
];
