import type { ImportType } from '../components/ImportTypeSelector';

/** Maps migration job importSource / jobType to wizard import type for job console panels. */
export function mapMigrationImportSource(
  importSource?: string | null,
  jobType?: string | null
): ImportType | null {
  if (jobType === 'EXPORT') {
    return 'project-export';
  }
  const src = (importSource ?? '').toUpperCase();
  switch (src) {
    case 'JIRA_DC':
      return 'jira-dc';
    case 'CSV':
      return 'csv';
    case 'WORKFLOW_XML':
      return 'workflow-xml';
    case 'PROJECT_IMPORT':
      return 'project-import';
    case 'PROJECT_EXPORT':
      return 'project-export';
    default:
      return null;
  }
}
