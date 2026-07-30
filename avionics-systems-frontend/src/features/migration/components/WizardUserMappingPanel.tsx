import React, { useCallback, useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  migrationWizardApi,
  type WizardUserMappingRow,
} from '../../../api/serviceApi';
import { migrationMappingApi } from '../../../api/fieldApi';

export type UserMappingDraft = {
  sourceIdentifier: string;
  sourceType: string;
  targetUsername?: string;
  targetEmail?: string;
  targetUserId?: string;
  mappingType: string;
};

function rowToDraft(row: WizardUserMappingRow): UserMappingDraft {
  return {
    sourceIdentifier: row.sourceIdentifier,
    sourceType: row.sourceType ?? 'LEGACY_DC',
    targetUsername: row.targetUsername,
    targetEmail: row.targetEmail,
    targetUserId: row.targetUserId,
    mappingType: row.mappingType ?? 'MANUAL',
  };
}

function draftToSessionPayload(drafts: UserMappingDraft[]): Array<Record<string, unknown>> {
  return drafts.map((d) => ({
    sourceIdentifier: d.sourceIdentifier,
    sourceType: d.sourceType,
    targetUsername: d.targetUsername,
    targetEmail: d.targetEmail,
    targetUserId: d.targetUserId,
    mappingType: d.mappingType,
  }));
}

interface WizardUserMappingPanelProps {
  sessionId: string | null;
  migrationJobId?: string;
  userCountHint?: number;
  value: UserMappingDraft[];
  onChange: (mappings: UserMappingDraft[]) => void;
}

export default function WizardUserMappingPanel({
  sessionId,
  migrationJobId,
  userCountHint,
  value,
  onChange,
}: WizardUserMappingPanelProps) {
  const queryClient = useQueryClient();
  const [newSource, setNewSource] = useState('');

  const { data: serverRows, isLoading } = useQuery({
    queryKey: ['wizard-user-mappings', sessionId],
    queryFn: async () => {
      if (!sessionId) return [];
      const res = await migrationWizardApi.getUserMappings(sessionId);
      return res.data;
    },
    enabled: !!sessionId,
  });

  useEffect(() => {
    if (!serverRows?.length) return;
    const drafts = serverRows.map(rowToDraft);
    const same =
      value.length === drafts.length
      && value.every((v, i) => v.sourceIdentifier === drafts[i]?.sourceIdentifier);
    if (!same) onChange(drafts);
  }, [serverRows]); // eslint-disable-line react-hooks/exhaustive-deps

  const resolveMutation = useMutation({
    mutationFn: async () => {
      if (!migrationJobId) throw new Error('No migration job linked to this session yet');
      const ids = value.map((v) => v.sourceIdentifier).filter(Boolean);
      if (ids.length === 0) throw new Error('Add at least one source user identifier');
      const res = await migrationMappingApi.resolveUsers(migrationJobId, ids);
      return res.data as WizardUserMappingRow[];
    },
    onSuccess: (rows) => {
      onChange(rows.map(rowToDraft));
      queryClient.invalidateQueries({ queryKey: ['wizard-user-mappings', sessionId] });
    },
  });

  const updateRow = useCallback(
    (index: number, patch: Partial<UserMappingDraft>) => {
      const next = value.map((row, i) => (i === index ? { ...row, ...patch, mappingType: 'MANUAL' } : row));
      onChange(next);
    },
    [value, onChange],
  );

  const addRow = () => {
    const id = newSource.trim();
    if (!id) return;
    if (value.some((v) => v.sourceIdentifier === id)) {
      setNewSource('');
      return;
    }
    onChange([
      ...value,
      { sourceIdentifier: id, sourceType: 'LEGACY_DC', mappingType: 'MANUAL' },
    ]);
    setNewSource('');
  };

  const removeRow = (index: number) => {
    onChange(value.filter((_, i) => i !== index));
  };

  return (
    <div className="bg-white rounded-lg border p-6 space-y-4" data-testid="wizard-user-mapping-panel">
      <div>
        <h3 className="text-lg font-semibold text-gray-900">User mapping</h3>
        <p className="text-sm text-gray-600 mt-1">
          Map Systems DC users (username or email) to target platform accounts before import.
          {userCountHint != null && userCountHint > 0 && (
            <span> Export contains approximately {userCountHint} user record(s).</span>
          )}
        </p>
      </div>

      {isLoading && <p className="text-sm text-gray-500">Loading user mappings…</p>}

      {!migrationJobId && (
        <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded px-3 py-2">
          Mappings are saved on the wizard session. After upload creates a migration job, use Auto-resolve
          to match against the user directory.
        </p>
      )}

      <div className="overflow-x-auto">
        <table className="min-w-full text-sm border-collapse">
          <thead>
            <tr className="border-b text-left text-gray-600">
              <th className="py-2 pr-4">Source (DC)</th>
              <th className="py-2 pr-4">Target username</th>
              <th className="py-2 pr-4">Target email</th>
              <th className="py-2 pr-4">Type</th>
              <th className="py-2" />
            </tr>
          </thead>
          <tbody>
            {value.length === 0 && (
              <tr>
                <td colSpan={5} className="py-4 text-gray-500">
                  No mappings yet. Add source identifiers from your DC export below.
                </td>
              </tr>
            )}
            {value.map((row, index) => (
              <tr key={`${row.sourceIdentifier}-${index}`} className="border-b border-gray-100">
                <td className="py-2 pr-4 font-mono text-xs">{row.sourceIdentifier}</td>
                <td className="py-2 pr-4">
                  <input
                    className="border rounded px-2 py-1 w-full max-w-[180px]"
                    value={row.targetUsername ?? ''}
                    onChange={(e) => updateRow(index, { targetUsername: e.target.value })}
                    placeholder="jane.doe"
                  />
                </td>
                <td className="py-2 pr-4">
                  <input
                    className="border rounded px-2 py-1 w-full max-w-[220px]"
                    type="email"
                    value={row.targetEmail ?? ''}
                    onChange={(e) => updateRow(index, { targetEmail: e.target.value })}
                    placeholder="jane@example.com"
                  />
                </td>
                <td className="py-2 pr-4 text-gray-500">{row.mappingType}</td>
                <td className="py-2">
                  <button
                    type="button"
                    className="text-red-600 hover:underline text-xs"
                    onClick={() => removeRow(index)}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex flex-wrap gap-2 items-end">
        <div className="flex-1 min-w-[200px]">
          <label className="block text-xs text-gray-600 mb-1">Add source user</label>
          <input
            className="border rounded px-3 py-2 w-full"
            value={newSource}
            onChange={(e) => setNewSource(e.target.value)}
            placeholder="dc-username or email@company.com"
            onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addRow())}
          />
        </div>
        <button type="button" className="px-4 py-2 border rounded hover:bg-gray-50" onClick={addRow}>
          Add
        </button>
        {migrationJobId && (
          <button
            type="button"
            className="px-4 py-2 bg-avisys-blue text-white rounded hover:opacity-90 disabled:opacity-50"
            disabled={resolveMutation.isPending || value.length === 0}
            onClick={() => resolveMutation.mutate()}
          >
            {resolveMutation.isPending ? 'Resolving…' : 'Auto-resolve via directory'}
          </button>
        )}
      </div>

      {resolveMutation.isError && (
        <p className="text-sm text-red-600" role="alert">
          {resolveMutation.error instanceof Error
            ? resolveMutation.error.message
            : 'User resolution failed'}
        </p>
      )}
    </div>
  );
}

export { draftToSessionPayload };
