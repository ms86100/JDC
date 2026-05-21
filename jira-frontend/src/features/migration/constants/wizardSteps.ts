export type MigrationStep =
  | 'source'
  | 'targetProject'
  | 'map'
  | 'validate'
  | 'configure'
  | 'review'
  | 'importing'
  | 'complete';

export const STEP_ORDER: MigrationStep[] = [
  'source',
  'targetProject',
  'map',
  'validate',
  'configure',
  'review',
  'importing',
  'complete',
];

/** Jira DC XML — no CSV field-mapping step. */
export const JIRA_DC_STEP_ORDER: MigrationStep[] = [
  'source',
  'targetProject',
  'validate',
  'configure',
  'review',
  'importing',
  'complete',
];

export const WORKFLOW_XML_STEP_ORDER: MigrationStep[] = [
  'source',
  'targetProject',
  'validate',
  'review',
  'importing',
  'complete',
];

export const PROJECT_IMPORT_STEP_ORDER: MigrationStep[] = [
  'source',
  'targetProject',
  'review',
  'importing',
  'complete',
];

export const PROJECT_EXPORT_STEP_ORDER: MigrationStep[] = [
  'source',
  'targetProject',
  'review',
  'importing',
  'complete',
];

export const STEP_LABELS: Record<MigrationStep, string> = {
  source: 'Source',
  targetProject: 'Target Project',
  map: 'Map Fields',
  validate: 'Validate',
  configure: 'Configure',
  review: 'Review',
  importing: 'Progress',
  complete: 'Complete',
};
