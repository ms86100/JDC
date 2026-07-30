import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fieldApi, migrationMappingApi, type OptionMappingDto } from '../../../api/fieldApi';

interface CustomFieldInfo {
  id: string;
  name: string;
  fieldKey: string;
  type: string;
}

interface FieldOption {
  id: string;
  value: string;
  label: string;
}

const SELECT_TYPES = new Set([
  'com.avisys.platform.plugin.system.customfieldtypes:select',
  'com.avisys.platform.plugin.system.customfieldtypes:multiselect',
  'com.avisys.platform.plugin.system.customfieldtypes:radiobuttons',
  'com.avisys.platform.plugin.system.customfieldtypes:checkbox',
  'com.avisys.platform.plugin.system.customfieldtypes:cascadingselect',
  'SINGLE_SELECT',
  'MULTI_SELECT',
  'RADIO',
  'CHECKBOX',
]);

interface Props {
  sessionId: string | null;
  value: OptionMappingDto[];
  onChange: (rows: OptionMappingDto[]) => void;
  sourceHeaders: string[];
  previewRows?: string[][];
}

const EMPTY_ROW = (): OptionMappingDto => ({
  sourceFieldKey: '',
  sourceOptionValue: '',
  targetFieldKey: '',
  targetOptionValue: '',
});

export default function OptionMappingMatrixPanel({ sessionId, value, onChange, sourceHeaders, previewRows }: Props) {
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

  const { data: customFields } = useQuery({
    queryKey: ['custom-fields-for-options'],
    queryFn: () => fieldApi.getCustomFields().then((r) => r.data),
    staleTime: 5 * 60 * 1000,
  });

  const selectTypeFields = useMemo<CustomFieldInfo[]>(() => {
    if (!customFields) return [];
    return customFields
      .filter((cf) => SELECT_TYPES.has(cf.type))
      .map((cf) => ({ id: cf.id, name: cf.name, fieldKey: cf.fieldKey, type: cf.type }));
  }, [customFields]);

  const [targetOptionsCache, setTargetOptionsCache] = useState<Record<string, FieldOption[]>>({});

  const fetchTargetOptions = async (fieldKey: string) => {
    if (targetOptionsCache[fieldKey]) return;
    const cf = selectTypeFields.find((f) => f.fieldKey === fieldKey);
    if (!cf) return;
    try {
      const res = await fieldApi.getCustomFieldOptions(cf.id);
      setTargetOptionsCache((prev) => ({ ...prev, [fieldKey]: res.data }));
    } catch {
      setTargetOptionsCache((prev) => ({ ...prev, [fieldKey]: [] }));
    }
  };

  const sourceValuesFor = useMemo(() => {
    const cache: Record<string, string[]> = {};
    if (!previewRows || previewRows.length < 2) return cache;
    const headers = previewRows[0];
    for (let col = 0; col < headers.length; col++) {
      const key = headers[col];
      const unique = new Set<string>();
      for (let row = 1; row < previewRows.length; row++) {
        const v = previewRows[row]?.[col];
        if (v && v.trim()) unique.add(v.trim());
      }
      cache[key] = Array.from(unique).sort();
    }
    return cache;
  }, [previewRows]);

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
                <select
                  className="border rounded px-2 py-1 w-full"
                  value={row.sourceFieldKey}
                  onChange={(e) => updateRow(i, { sourceFieldKey: e.target.value, sourceOptionValue: '' })}
                >
                  <option value="">-- Select source field --</option>
                  {sourceHeaders.map((h) => (
                    <option key={h} value={h}>{h}</option>
                  ))}
                </select>
              </td>
              <td className="py-1 pr-2">
                <select
                  className="border rounded px-2 py-1 w-full"
                  value={row.sourceOptionValue}
                  onChange={(e) => updateRow(i, { sourceOptionValue: e.target.value })}
                >
                  <option value="">-- Select source value --</option>
                  {(sourceValuesFor[row.sourceFieldKey] ?? []).map((v) => (
                    <option key={v} value={v}>{v}</option>
                  ))}
                </select>
              </td>
              <td className="py-1 pr-2">
                <select
                  className="border rounded px-2 py-1 w-full"
                  value={row.targetFieldKey}
                  onChange={(e) => {
                    const key = e.target.value;
                    updateRow(i, { targetFieldKey: key, targetOptionValue: '' });
                    if (key) fetchTargetOptions(key);
                  }}
                >
                  <option value="">-- Select target field --</option>
                  {selectTypeFields.map((f) => (
                    <option key={f.fieldKey} value={f.fieldKey}>{f.name}</option>
                  ))}
                </select>
              </td>
              <td className="py-1 pr-2">
                <select
                  className="border rounded px-2 py-1 w-full"
                  value={row.targetOptionValue}
                  onChange={(e) => updateRow(i, { targetOptionValue: e.target.value })}
                >
                  <option value="">-- Select target value --</option>
                  {(targetOptionsCache[row.targetFieldKey] ?? []).map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label || opt.value}</option>
                  ))}
                </select>
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
