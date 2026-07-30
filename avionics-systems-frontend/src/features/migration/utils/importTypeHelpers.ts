import type { ImportType } from '../types/migration';

/** Avionics Systems issue engine: standalone issue XML or full backup ZIP. */
export function isLegacyDcIssueImport(type: ImportType | null | string | undefined): boolean {
  return type === 'legacy-dc' || type === 'issue-xml';
}

export function isIssueXmlImport(type: ImportType | null | string | undefined): boolean {
  return type === 'issue-xml';
}

/** Stored on migration job options for history labeling. */
export function dcImportProfile(type: ImportType | null): string | undefined {
  if (type === 'issue-xml') return 'issue-xml';
  if (type === 'legacy-dc') return 'full-backup';
  return undefined;
}
