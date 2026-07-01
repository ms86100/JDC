/**
 * Xray Test Management plugin — navigation registry (Atlassian Xray parity).
 * All routes exist in App.tsx; this is the discoverability SSOT for the Xray hub.
 */

export interface XrayCapability {
  id: string;
  label: string;
  path: string;
  description: string;
  group: 'core' | 'quality' | 'planning' | 'integration' | 'admin';
}

export const XRAY_PLUGIN_LABEL = 'Xray Test Management';
export const XRAY_PLUGIN_TAGLINE =
  'Atlassian Xray-style test management plugin for Systems and Avionics — requirements, execution, and traceability.';

/** Build route matching App.tsx (`/tests/create/:projectId`, `/tests/traceability/:projectId`, etc.) */
export function xrayPath(projectId: string | undefined, segment: string): string {
  if (!segment) return projectId ? `/tests/${projectId}` : '/tests';
  if (segment.startsWith('/')) return segment;
  if (segment === 'create') {
    return projectId ? `/tests/create/${projectId}` : '/tests/create';
  }
  if (segment === 'workflows/builder') {
    return '/tests/workflows/builder';
  }
  return projectId ? `/tests/${segment}/${projectId}` : `/tests/${segment}`;
}

export const XRAY_CAPABILITIES: XrayCapability[] = [
  { id: 'home', label: 'Test repository', path: '', description: 'Tests, sets, and plans', group: 'core' },
  { id: 'create', label: 'Create test', path: 'create', description: 'Manual, automated, BDD', group: 'core' },
  { id: 'traceability', label: 'Traceability matrix', path: 'traceability', description: 'Requirement ↔ test ↔ defect', group: 'core' },
  { id: 'coverage', label: 'Coverage', path: 'coverage', description: 'Requirement coverage', group: 'core' },
  { id: 'defects', label: 'Defects', path: 'defects', description: 'Defect tracking', group: 'quality' },
  { id: 'evidence', label: 'Evidence gallery', path: 'evidence', description: 'Run attachments', group: 'quality' },
  { id: 'reporting', label: 'Test reports', path: 'reporting', description: 'Metrics & dashboards', group: 'quality' },
  { id: 'flaky', label: 'Flaky tests', path: 'flaky', description: 'Stability signals', group: 'quality' },
  { id: 'flaky-dash', label: 'Flaky dashboard', path: 'flaky-dashboard', description: 'Advanced analytics', group: 'quality' },
  { id: 'quarantine', label: 'Quarantine', path: 'quarantine', description: 'Isolated tests', group: 'quality' },
  { id: 'impact', label: 'Impact analysis', path: 'impact', description: 'Change impact', group: 'quality' },
  { id: 'shared-steps', label: 'Shared steps', path: 'shared-steps', description: 'Reusable steps', group: 'planning' },
  { id: 'preconditions', label: 'Preconditions', path: 'preconditions', description: 'Precondition library', group: 'planning' },
  { id: 'datasets', label: 'Datasets', path: 'datasets', description: 'Parameterized data', group: 'planning' },
  { id: 'timeline', label: 'Timeline', path: 'timeline', description: 'Schedule view', group: 'planning' },
  { id: 'req-versions', label: 'Requirement versions', path: 'requirement-versions', description: 'Version matrix', group: 'planning' },
  { id: 'env-matrix', label: 'Environment matrix', path: 'environment-matrix', description: 'Env × browser', group: 'planning' },
  { id: 'workflows', label: 'Test workflows', path: 'workflows', description: 'Test workflow builder', group: 'planning' },
  { id: 'import', label: 'Import tests', path: 'import', description: 'Cucumber, JUnit XML', group: 'integration' },
  { id: 'webhooks', label: 'CI/CD webhooks', path: 'webhooks', description: 'Pipeline integration', group: 'integration' },
  { id: 'ai', label: 'AI assistant', path: 'ai', description: 'AI test helpers', group: 'integration' },
  { id: 'screen-config', label: 'Screen configuration', path: 'screen-config', description: 'Schemes & fields', group: 'admin' },
  { id: 'plugins', label: 'Plugin registry', path: 'plugins', description: 'Xray plugin config', group: 'admin' },
  { id: 'settings', label: 'Test settings', path: 'settings', description: 'Project test config', group: 'admin' },
];

export const XRAY_GROUP_LABELS: Record<XrayCapability['group'], string> = {
  core: 'Test design',
  quality: 'Execution & quality',
  planning: 'Planning & structure',
  integration: 'Integrations',
  admin: 'Configuration',
};
