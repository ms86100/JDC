import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationMappingApi, type OptionMappingDto } from '../../../api/fieldApi';

interface Props {
  sessionId: string | null;
  value: OptionMappingDto[];
  onChange: (rows: OptionMappingDto[]) => void;
}

const EMPTY_ROW = (): OptionMappingDto => ({
  sourceFieldKey: '',
  sourceOptionValue: '',
  targetFieldKey: '',
  targetOptionValue: '',
});

export default function OptionMappingMatrixPanel({ sessionId, value, onChange }: Props) {
  const { data: serverRows } = useQuery({
    queryKey: ['option-mappings', sessionId],
    queryFn: () =>
      sessionId
        ? migrationMappingApi.getSessionOptionMappings(sessionId).then((r) => r.data)
        : Promise.resolve([]),
    enabled: !!sessionId,
  });

  useEffect(() => {
    if (serverRows?.length && value.length === 0) onChange(serverRows);
  }, [serverRows]); // eslint-disable-line react-hooks/exhaustive-deps

  const updateRow = (index: number, patch: Partial<OptionMappingDto>) => {
    onChange(value.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  };

  return (
    <div className="bg-white rounded-lg border p-6 space-y-4" data-testid="option-mapping-matrix">
      <div>
        <h3 className="text-lg font-semibold">Option mapping (select / multi-select)</h3>
        <p className="text-sm text-gray-600">
          Map source option values to target catalog options for custom fields.
        </p>
      </div>
      <table className="min-w-full text-sm">
        <thead>
          <tr className="border-b text-left text-gray-600">
            <th className="py-2 pr-2">Source field</th>
            <th className="py-2 pr-2">Source value</th>
            <th className="py-2 pr-2">Target field</th>
            <th className="py-2 pr-2">Target value</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {value.map((row, i) => (
            <tr key={i} className="border-b border-gray-100">
              <td className="py-1 pr-2">
                <input
                  className="border rounded px-2 py-1 w-full"
                  value={row.sourceFieldKey}
                  onChange={(e) => updateRow(i, { sourceFieldKey: e.target.value })}
                />
              </td>
              <td className="py-1 pr-2">
                <input
                  className="border rounded px-2 py-1 w-full"
                  value={row.sourceOptionValue}
                  onChange={(e) => updateRow(i, { sourceOptionValue: e.target.value })}
                />
              </td>
              <td className="py-1 pr-2">
                <input
                  className="border rounded px-2 py-1 w-full"
                  value={row.targetFieldKey}
                  onChange={(e) => updateRow(i, { targetFieldKey: e.target.value })}
                />
              </td>
              <td className="py-1 pr-2">
                <input
                  className="border rounded px-2 py-1 w-full"
                  value={row.targetOptionValue}
                  onChange={(e) => updateRow(i, { targetOptionValue: e.target.value })}
                />
              </td>
              <td className="py-1">
                <button
                  type="button"
                  className="text-red-600 text-xs"
                  onClick={() => onChange(value.filter((_, j) => j !== i))}
                >
                  Remove
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <button type="button" className="px-4 py-2 border rounded text-sm" onClick={() => onChange([...value, EMPTY_ROW()])}>
        Add row
      </button>
    </div>
  );
}
