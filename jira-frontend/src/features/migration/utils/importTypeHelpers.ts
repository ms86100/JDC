import type { ImportType } from '../types/migration';

/** Jira DC issue engine: standalone issue XML or full backup ZIP. */
export function isJiraDcIssueImport(type: ImportType | null | string | undefined): boolean {
  return type === 'jira-dc' || type === 'issue-xml';
}

export function isIssueXmlImport(type: ImportType | null | string | undefined): boolean {
  return type === 'issue-xml';
}

/** Stored on migration job options for history labeling. */
export function dcImportProfile(type: ImportType | null): string | undefined {
  if (type === 'issue-xml') return 'issue-xml';
  if (type === 'jira-dc') return 'full-backup';
  return undefined;
}
