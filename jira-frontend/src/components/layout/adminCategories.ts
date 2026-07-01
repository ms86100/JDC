/**
 * Administration navigation — Jira Data Center hierarchy (Issues-first).
 * Paths match AdminRoutes.tsx exactly.
 */

import { JIRA_DC_ADMIN_CATEGORY_ORDER } from './jiraDcNavRegistry';

export interface AdminItem { label: string; path: string; }
export interface AdminCategory { key: string; label: string; icon: string; items: AdminItem[]; }

const CATEGORIES_BY_KEY: Record<string, AdminCategory> = {
  issues: {
    key: 'issues', label: 'Issues', icon: '📋',
    items: [
      { label: 'Issue types', path: '/admin/issue-types' },
      { label: 'Issue type schemes', path: '/admin/issue-type-schemes' },
      { label: 'Priorities', path: '/admin/priorities' },
      { label: 'Statuses', path: '/admin/statuses' },
      { label: 'Resolutions', path: '/admin/resolutions' },
      { label: 'Field configuration', path: '/admin/field-config' },
    ],
  },
  workflows: {
    key: 'workflows', label: 'Workflows', icon: '🔀',
    items: [
      { label: 'Workflows', path: '/admin/workflows' },
      { label: 'Workflow schemes', path: '/admin/workflow-schemes' },
      { label: 'Screens', path: '/admin/screens' },
      { label: 'Screen schemes', path: '/admin/screen-schemes' },
      { label: 'Workflow hub (app)', path: '/workflows' },
    ],
  },
  fields: {
    key: 'fields', label: 'Fields', icon: '🏷',
    items: [
      { label: 'Custom fields', path: '/admin/custom-fields' },
      { label: 'Field types', path: '/admin/field-types' },
      { label: 'Field contexts', path: '/admin/field-contexts' },
    ],
  },
  projects: {
    key: 'projects', label: 'Projects', icon: '📁',
    items: [
      { label: 'Project types', path: '/admin/project-types' },
      { label: 'Project categories', path: '/admin/project-categories' },
      { label: 'Archetypes', path: '/admin/archetypes' },
      { label: 'All projects (app)', path: '/projects' },
    ],
  },
  users: {
    key: 'users', label: 'User management', icon: '👤',
    items: [
      { label: 'Users', path: '/admin/users' },
      { label: 'Create user', path: '/admin/users/create' },
      { label: 'Groups', path: '/admin/groups' },
      { label: 'Group browser', path: '/admin/groups/view' },
      { label: 'Directories', path: '/admin/directories' },
      { label: 'Roles', path: '/admin/roles' },
      { label: 'User management (legacy)', path: '/admin/user-management' },
      { label: 'Global permissions', path: '/admin/permissions' },
      { label: 'Permission schemes', path: '/admin/permission-schemes' },
      { label: 'Notification schemes', path: '/admin/notification-schemes' },
      { label: 'Security schemes', path: '/admin/security-schemes' },
      { label: 'Password policy', path: '/admin/password-policy' },
      { label: 'Sessions', path: '/admin/sessions' },
    ],
  },
  system: {
    key: 'system', label: 'System', icon: '⚙',
    items: [
      { label: 'General configuration', path: '/admin/system/general' },
      { label: 'Appearance', path: '/admin/system/appearance' },
      { label: 'Attachments', path: '/admin/system/attachments' },
      { label: 'Time tracking', path: '/admin/system/time-tracking' },
      { label: 'Sub-tasks', path: '/admin/system/subtasks' },
      { label: 'Import & export', path: '/admin/system/import' },
      { label: 'Licensing', path: '/admin/system/licensing' },
      { label: 'System info', path: '/admin/system/info' },
      { label: 'Reports', path: '/admin/reports' },
      { label: 'Insights', path: '/admin/insights' },
    ],
  },
  addons: {
    key: 'addons', label: 'Applications', icon: '⚡',
    items: [
      { label: 'Application links', path: '/admin/application-links' },
      { label: 'Automation', path: '/admin/automation' },
      { label: 'Webhooks', path: '/admin/webhooks' },
      { label: 'Outgoing mail', path: '/admin/mail' },
      { label: 'REST API', path: '/admin/api' },
      { label: 'GraphQL explorer', path: '/admin/api/graphql' },
      { label: 'OAuth consumers', path: '/admin/oauth' },
    ],
  },
  dc: {
    key: 'dc', label: 'Data Center', icon: '🖥',
    items: [
      { label: 'Cluster', path: '/admin/cluster' },
      { label: 'Cache', path: '/admin/cache' },
      { label: 'Indexing', path: '/admin/indexing' },
      { label: 'Background jobs', path: '/admin/jobs' },
      { label: 'Services', path: '/admin/services' },
    ],
  },
  audit: {
    key: 'audit', label: 'Audit & bulk', icon: '📊',
    items: [
      { label: 'Auditing', path: '/admin/auditing' },
      { label: 'Platform audit (app)', path: '/audit' },
      { label: 'Bulk operations', path: '/admin/bulk-create' },
      { label: 'Dark features', path: '/admin/dark-features' },
    ],
  },
  testing: {
    key: 'testing', label: 'Xray Test Management', icon: '🧪',
    items: [
      { label: 'Test cases home', path: '/tests' },
      { label: 'Test sets', path: '/admin/tests/sets' },
      { label: 'Test plans', path: '/admin/tests/plans' },
      { label: 'Test environments', path: '/admin/tests/environments' },
      { label: 'Import tests', path: '/admin/tests/import' },
      { label: 'Test reports', path: '/admin/tests/reports' },
      { label: 'CI/CD webhooks', path: '/admin/tests/webhooks' },
      { label: 'AI suggestions', path: '/admin/tests/ai' },
    ],
  },
};

export const ADMIN_CATEGORIES: AdminCategory[] = JIRA_DC_ADMIN_CATEGORY_ORDER.map(
  (key) => CATEGORIES_BY_KEY[key],
).filter(Boolean);
