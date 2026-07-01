/** Per-conflict resolution sent to migration API as options.conflictResolutions */
export type DcConflictAction = 'PROCEED' | 'SKIP_ENTITY' | 'USE_DEFAULT' | 'OVERRIDE_VALUE';

export interface DcConflictResolution {
  conflictId: string;
  entityKey: string;
  field: string;
  code: string;
  action: DcConflictAction;
  overrideValue?: string;
}

export function conflictRowId(
  c: { code: string; entityKey: string; field: string },
  index: number
): string {
  return `${c.code}|${c.entityKey}|${c.field}|${index}`;
}

export function buildConflictResolutionsPayload(
  resolutions: Record<string, DcConflictResolution>
): Array<Record<string, string>> {
  return Object.values(resolutions)
    .filter((r) => r.action !== 'PROCEED')
    .map((r) => {
      const row: Record<string, string> = {
        entityKey: r.entityKey,
        field: r.field,
        code: r.code,
        action: r.action,
      };
      if (r.overrideValue) {
        row.overrideValue = r.overrideValue;
      }
      return row;
    });
}
