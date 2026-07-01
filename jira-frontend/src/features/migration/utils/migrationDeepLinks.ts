import type { MigrationCenterView } from '../components/MigrationCenterNav';
import type { ImportType } from '../components/ImportTypeSelector';

export const MIGRATION_CENTER_VIEWS: MigrationCenterView[] = [
  'wizard',
  'history',
  'health',
  'catalog',
  'dlq',
  'templates',
  'settings',
];

export const MIGRATION_IMPORT_TYPES: ImportType[] = [
  'csv',
  'issue-xml',
  'jira-dc',
  'workflow-xml',
  'project-import',
  'project-export',
];

export function migrationPath(view?: MigrationCenterView, importType?: ImportType): string {
  const params = new URLSearchParams();
  if (view && view !== 'wizard') {
    params.set('view', view);
  }
  if (importType) {
    params.set('import', importType);
  }
  const q = params.toString();
  return q ? `/migration?${q}` : '/migration';
}

export function parseMigrationCenterView(search: string): MigrationCenterView | null {
  const raw = search.startsWith('?') ? search.slice(1) : search;
  const v = new URLSearchParams(raw).get('view');
  if (v && MIGRATION_CENTER_VIEWS.includes(v as MigrationCenterView)) {
    return v as MigrationCenterView;
  }
  return null;
}

export function parseMigrationImportType(search: string): ImportType | null {
  const raw = search.startsWith('?') ? search.slice(1) : search;
  const imp = new URLSearchParams(raw).get('import');
  if (imp && MIGRATION_IMPORT_TYPES.includes(imp as ImportType)) {
    return imp as ImportType;
  }
  return null;
}

export function migrationQueryMatches(
  pathname: string,
  search: string,
  itemPath: string,
): boolean {
  if (!itemPath.startsWith('/migration')) {
    return false;
  }
  if (pathname !== '/migration' && !pathname.startsWith('/migration/')) {
    return false;
  }
  const qIdx = itemPath.indexOf('?');
  if (qIdx < 0) {
    const raw = search.startsWith('?') ? search.slice(1) : search;
    const cur = new URLSearchParams(raw);
    return !cur.has('view') && !cur.has('import');
  }
  const itemQs = itemPath.slice(qIdx + 1);
  const itemParams = new URLSearchParams(itemQs);
  const cur = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search);
  for (const [key, val] of itemParams.entries()) {
    if (cur.get(key) !== val) {
      return false;
    }
  }
  return true;
}
