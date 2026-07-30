import React from 'react';
import type { LegacyDcValidateResponse } from '../../../api/serviceApi';
import {
  conflictRowId,
  type DcConflictAction,
  type DcConflictResolution,
} from '../types/dcConflictResolution';

interface Props {
  conflicts: LegacyDcValidateResponse['conflicts'];
  acknowledged?: boolean;
  onAcknowledgeChange?: (ack: boolean) => void;
  resolutions?: Record<string, DcConflictResolution>;
  onResolutionChange?: (conflictId: string, resolution: DcConflictResolution) => void;
}

const ACTION_OPTIONS: { value: DcConflictAction; label: string; forBlocker?: boolean }[] = [
  { value: 'PROCEED', label: 'Proceed as-is' },
  { value: 'SKIP_ENTITY', label: 'Skip entity on import' },
  { value: 'USE_DEFAULT', label: 'Use platform default' },
  { value: 'OVERRIDE_VALUE', label: 'Override field value' },
];

export default function DcImportConflictPanel({
  conflicts,
  acknowledged = false,
  onAcknowledgeChange,
  resolutions = {},
  onResolutionChange,
}: Props) {
  if (!conflicts?.length) {
    return null;
  }

  const blockers = conflicts.filter((c) => c.severity === 'BLOCKER');
  const warnings = conflicts.filter((c) => c.severity !== 'BLOCKER');
  const interactive = !!onResolutionChange;

  const renderRow = (c: NonNullable<LegacyDcValidateResponse['conflicts']>[number], index: number) => {
    const id = conflictRowId(c, index);
    const current = resolutions[id];
    const action = current?.action ?? (c.severity === 'BLOCKER' ? 'SKIP_ENTITY' : 'PROCEED');

    return (
      <li
        key={id}
        className="border rounded p-2 space-y-2 bg-white/60"
        data-testid={`dc-conflict-row-${index}`}
      >
        <div className="text-xs">
          <span className="font-medium">[{c.code}]</span> {c.message}
          {c.entityKey && (
            <span className="text-gray-600 block mt-0.5">
              Entity: <code>{c.entityKey}</code>
              {c.field ? (
                <>
                  {' '}
                  · Field: <code>{c.field}</code>
                </>
              ) : null}
            </span>
          )}
          {c.resolution && <span className="text-gray-500 block">Suggested: {c.resolution}</span>}
        </div>
        {interactive && (
          <div className="flex flex-wrap gap-2 items-center">
            <select
              className="text-xs border rounded px-2 py-1"
              data-testid={`dc-conflict-action-${index}`}
              value={action}
              onChange={(e) => {
                const nextAction = e.target.value as DcConflictAction;
                onResolutionChange!(id, {
                  conflictId: id,
                  entityKey: c.entityKey ?? '',
                  field: c.field ?? '',
                  code: c.code,
                  action: nextAction,
                  overrideValue: current?.overrideValue,
                });
              }}
            >
              {ACTION_OPTIONS.filter((o) =>
                c.severity === 'BLOCKER' ? o.value !== 'PROCEED' : true
              ).map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
            {(action === 'OVERRIDE_VALUE' || action === 'USE_DEFAULT') && (
              <input
                type="text"
                className="text-xs border rounded px-2 py-1 flex-1 min-w-[120px]"
                placeholder={action === 'USE_DEFAULT' ? 'Default value (optional)' : 'Override value'}
                data-testid={`dc-conflict-override-${index}`}
                value={current?.overrideValue ?? ''}
                onChange={(e) =>
                  onResolutionChange!(id, {
                    conflictId: id,
                    entityKey: c.entityKey ?? '',
                    field: c.field ?? '',
                    code: c.code,
                    action,
                    overrideValue: e.target.value,
                  })
                }
              />
            )}
          </div>
        )}
      </li>
    );
  };

  return (
    <div
      className="rounded-lg border border-amber-200 bg-amber-50 p-4 space-y-3"
      data-testid="dc-import-conflict-panel"
    >
      <h4 className="text-sm font-semibold text-amber-900">Import conflicts</h4>
      <p className="text-xs text-amber-800">
        {interactive
          ? 'Choose per-conflict actions before import. Skipped entities are omitted from the job.'
          : 'Review blockers before import. Warnings may proceed with explicit acknowledgment.'}
      </p>

      {blockers.length > 0 && (
        <div>
          <p className="text-xs font-semibold text-red-800 mb-1">Blockers ({blockers.length})</p>
          <ul className="text-xs text-red-700 space-y-2">
            {blockers.map((c, i) => renderRow(c, i))}
          </ul>
        </div>
      )}

      {warnings.length > 0 && onAcknowledgeChange && !interactive && (
        <label className="flex items-center gap-2 text-xs text-amber-900">
          <input
            type="checkbox"
            checked={acknowledged}
            onChange={(e) => onAcknowledgeChange(e.target.checked)}
            data-testid="dc-conflicts-ack-checkbox"
          />
          I reviewed warnings and want to proceed with import
        </label>
      )}

      {warnings.length > 0 && (
        <div>
          <p className="text-xs font-semibold text-amber-800 mb-1">Warnings ({warnings.length})</p>
          <ul className="text-xs text-amber-900 space-y-2 max-h-48 overflow-y-auto">
            {warnings.map((c, i) => renderRow(c, blockers.length + i))}
          </ul>
          {interactive && onAcknowledgeChange && (
            <label className="flex items-center gap-2 text-xs text-amber-900 mt-2">
              <input
                type="checkbox"
                checked={acknowledged}
                onChange={(e) => onAcknowledgeChange(e.target.checked)}
                data-testid="dc-conflicts-ack-checkbox"
              />
              I reviewed remaining warnings and want to proceed
            </label>
          )}
        </div>
      )}
    </div>
  );
}
