import React, { useState, useMemo, useCallback, useEffect } from 'react';
import type { FieldMapping } from '../types/migration';
import { matchHeaderToTargetField } from '../utils/fieldMappingMatch';

interface TargetField {
  field: string;
  displayName: string;
  dataType: string;
  required: boolean;
  description?: string;
}

interface FieldMappingPanelProps {
  sourceHeaders: string[];
  targetFields: TargetField[];
  initialMappings?: FieldMapping[];
  onMappingsChange?: (mappings: FieldMapping[]) => void;
  onPreview?: (mappings: FieldMapping[]) => void;
  onDiscoverFields?: () => Promise<void>;
  onRefreshTargets?: () => void;
  readOnly?: boolean;
  typeWarnings?: string[];
}

const TRANSFORMERS = [
  { value: '', label: 'None' },
  { value: 'DATE_ISO', label: 'Date (ISO 8601)' },
  { value: 'DATE_US', label: 'Date (US Format)' },
  { value: 'DATE_EU', label: 'Date (EU Format)' },
  { value: 'TRIM', label: 'Trim Whitespace' },
  { value: 'UPPER', label: 'Uppercase' },
  { value: 'LOWER', label: 'Lowercase' },
  { value: 'TITLE_CASE', label: 'Title Case' },
  { value: 'CUSTOM', label: 'Custom Expression' },
];

export default function FieldMappingPanel({
  sourceHeaders,
  targetFields,
  initialMappings = [],
  onMappingsChange,
  onPreview,
  onDiscoverFields,
  onRefreshTargets,
  readOnly = false,
  typeWarnings = [],
}: FieldMappingPanelProps) {
  const [isDiscovering, setIsDiscovering] = useState(false);
  const [mappings, setMappings] = useState<FieldMapping[]>(() => {
    if (initialMappings.length > 0) return initialMappings;

    // Auto-generate mappings
    return sourceHeaders.map((header) => ({
      sourceColumn: header,
      targetField: '',
      dataType: 'STRING',
      required: false,
      mapped: false,
    }));
  });

  const [selectedMapping, setSelectedMapping] = useState<number | null>(null);
  const [searchSource, setSearchSource] = useState('');
  const [searchTarget, setSearchTarget] = useState('');

  useEffect(() => {
    if (sourceHeaders.length === 0) return;
    setMappings((prev) => {
      const bySource = new Map(prev.map((m) => [m.sourceColumn, m]));
      return sourceHeaders.map((header) => {
        const existing = bySource.get(header);
        if (existing) return existing;
        return {
          sourceColumn: header,
          targetField: '',
          dataType: 'STRING',
          required: false,
          mapped: false,
        };
      });
    });
  }, [sourceHeaders.join('\u0001')]);

  // Auto-map suggestions
  const autoMapSuggestions = useMemo(() => {
    const suggestions: Array<{ sourceIndex: number; targetField: string; confidence: number }> = [];

    mappings.forEach((mapping, sourceIndex) => {
      if (mapping.mapped) return;

      const normalizedSource = (mapping.sourceColumn ?? '').toLowerCase().replace(/[\s_-]/g, '');

      targetFields.forEach((target) => {
        const normalizedTarget = target.field.toLowerCase().replace(/[\s_-]/g, '');
        let confidence = 0;

        // Exact match
        if (normalizedSource === normalizedTarget) {
          confidence = 100;
        }
        // Contains match
        else if (normalizedSource.includes(normalizedTarget) || normalizedTarget.includes(normalizedSource)) {
          confidence = 80;
        }
        // Alias match
        else {
          const aliases: Record<string, string[]> = {
            issueKey: ['issuekey', 'issue key', 'key', 'issue id'],
            summary: ['summary', 'title', 'subject', 'headline'],
            description: ['description', 'desc', 'body', 'details', 'content'],
            issuetype: ['issuetype', 'type', 'issue_type', 'issue type'],
            priority: ['priority', 'prio', 'importance', 'severity'],
            project: ['project', 'proj', 'project_key', 'project key'],
            status: ['status', 'state', 'workflow'],
            assignee: ['assignee', 'assigned', 'assignee_name'],
            reporter: ['reporter', 'reported', 'reported_by'],
            labels: ['labels', 'tags', 'categories'],
          };

          const fieldAliases = aliases[mapping.sourceColumn.toLowerCase()] || [];
          if (fieldAliases.some((alias) => normalizedTarget.includes(alias) || alias.includes(normalizedTarget))) {
            confidence = 60;
          }
        }

        if (confidence > 50) {
          suggestions.push({ sourceIndex, targetField: target.field, confidence });
        }
      });
    });

    return suggestions.sort((a, b) => b.confidence - a.confidence);
  }, [mappings, targetFields]);

  // Apply auto-mapping (strict header match — avoids "Project name" → summary)
  const applyAutoMapping = useCallback(() => {
    const targetFieldKeys = targetFields.map((t) => t.field);
    const newMappings = mappings.map((mapping) => {
      if (mapping.mapped) {
        return mapping;
      }
      const matchedField = matchHeaderToTargetField(mapping.sourceColumn, targetFieldKeys);
      if (!matchedField) {
        return mapping;
      }
      const target = targetFields.find((t) => t.field === matchedField);
      return {
        ...mapping,
        targetField: matchedField,
        dataType: target?.dataType ?? mapping.dataType,
        required: target?.required ?? mapping.required,
        mapped: true,
      };
    });

    setMappings(newMappings);
    onMappingsChange?.(newMappings);
  }, [mappings, targetFields, onMappingsChange]);

  // Handle mapping change
  const updateMapping = useCallback(
    (index: number, updates: Partial<FieldMapping>) => {
      const newMappings = [...mappings];
      newMappings[index] = { ...newMappings[index], ...updates };
      setMappings(newMappings);
      onMappingsChange?.(newMappings);
    },
    [mappings, onMappingsChange]
  );

  // Clear mapping
  const clearMapping = useCallback(
    (index: number) => {
      updateMapping(index, {
        targetField: '',
        mapped: false,
        transformer: undefined,
      });
    },
    [updateMapping]
  );

  // Filter mappings for display
  const filteredSourceMappings = useMemo(() => {
    if (!searchSource) return mappings;
    const query = searchSource.toLowerCase();
    return mappings.filter((m) =>
      m.sourceColumn.toLowerCase().includes(query) ||
      m.targetField.toLowerCase().includes(query)
    );
  }, [mappings, searchSource]);

  const filteredTargetFields = useMemo(() => {
    if (!searchTarget) return targetFields;
    const query = searchTarget.toLowerCase();
    return targetFields.filter(
      (t) =>
        t.field.toLowerCase().includes(query) ||
        t.displayName.toLowerCase().includes(query)
    );
  }, [targetFields, searchTarget]);

  const mappedCount = mappings.filter((m) => m.mapped).length;
  const requiredMappedCount = mappings.filter((m) => m.mapped && m.required).length;
  const requiredTotal = targetFields.filter((t) => t.required).length;

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-medium text-gray-700">Field Mapping</h3>
          <p className="text-xs text-gray-500 mt-1">
            {mappedCount} of {mappings.length} columns mapped
            {requiredTotal > 0 && ` • ${requiredMappedCount}/${requiredTotal} required fields mapped`}
          </p>
        </div>

        {!readOnly && (
          <div className="flex items-center gap-2">
            {onDiscoverFields && mappings.filter((m) => !m.mapped).length > 0 && (
              <button
                onClick={async () => {
                  setIsDiscovering(true);
                  try {
                    await onDiscoverFields();
                    onRefreshTargets?.();
                  } finally {
                    setIsDiscovering(false);
                  }
                }}
                disabled={isDiscovering}
                className="px-3 py-2 text-sm bg-purple-600 text-white rounded-md hover:bg-purple-700 disabled:opacity-50 transition-colors"
              >
                {isDiscovering ? 'Discovering...' : `Discover & Provision (${mappings.filter((m) => !m.mapped).length} unmapped)`}
              </button>
            )}
            <button
              onClick={applyAutoMapping}
              disabled={autoMapSuggestions.length === 0}
              className="px-3 py-2 text-sm bg-avisys-blue text-white rounded-md hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Auto-map {autoMapSuggestions.length > 0 && `(${autoMapSuggestions.length})`}
            </button>
          </div>
        )}
      </div>

      {typeWarnings.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-4" role="alert">
          <p className="text-sm font-medium text-amber-800">Type compatibility warnings</p>
          <ul className="mt-2 text-xs text-amber-700 list-disc list-inside max-h-32 overflow-y-auto">
            {typeWarnings.map((w, i) => (
              <li key={i}>{w}</li>
            ))}
          </ul>
        </div>
      )}

      {/* Progress bar */}
      <div className="bg-gray-200 rounded-full h-2">
        <div
          className="bg-avisys-blue h-2 rounded-full transition-all"
          style={{ width: `${(mappedCount / mappings.length) * 100}%` }}
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        {/* Source Column */}
        <div className="border rounded-lg overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 border-b">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700">Source Columns (CSV)</span>
              <span className="text-xs text-gray-500">{mappings.length} columns</span>
            </div>
            <input
              type="text"
              placeholder="Search source columns..."
              value={searchSource}
              onChange={(e) => setSearchSource(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-avisys-blue"
            />
          </div>

          <div className="overflow-y-auto max-h-80">
            {filteredSourceMappings.map((mapping, index) => {
              const originalIndex = mappings.indexOf(mapping);
              const isSelected = selectedMapping === originalIndex;

              return (
                <div
                  key={originalIndex}
                  className={`
                    px-4 py-3 border-b last:border-b-0 cursor-pointer transition-colors
                    ${isSelected ? 'bg-blue-50 border-l-4 border-l-avisys-blue' : 'hover:bg-gray-50'}
                    ${mapping.mapped ? 'bg-green-50' : ''}
                    ${mapping.required && !mapping.mapped ? 'bg-red-50' : ''}
                  `}
                  onClick={() => setSelectedMapping(originalIndex)}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-sm font-medium text-gray-900">{mapping.sourceColumn}</span>
                      <span className="text-xs text-gray-500 ml-2">{mapping.dataType}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      {mapping.required && (
                        <span className="text-xs bg-red-100 text-red-600 px-1.5 py-0.5 rounded">
                          Required
                        </span>
                      )}
                      {mapping.mapped ? (
                        <span className="text-green-600 text-sm">✓</span>
                      ) : (
                        <span className="text-gray-400 text-sm">—</span>
                      )}
                    </div>
                  </div>
                  {mapping.mapped && (
                    <div className="mt-1 text-xs text-gray-500">
                      → {mapping.targetField}
                      {mapping.transformer && (
                        <span className="ml-2 text-blue-600">[{mapping.transformer}]</span>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Target Fields */}
        <div className="border rounded-lg overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 border-b">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700">Target Fields</span>
              <span className="text-xs text-gray-500">{targetFields.length} fields</span>
            </div>
            <input
              type="text"
              placeholder="Search target fields..."
              value={searchTarget}
              onChange={(e) => setSearchTarget(e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-avisys-blue"
            />
          </div>

          <div className="overflow-y-auto max-h-80">
            {filteredTargetFields.map((field) => {
              const isMapped = mappings.some((m) => m.mapped && m.targetField === field.field);
              const isSelectedSource = selectedMapping !== null && !mappings[selectedMapping]?.mapped;

              return (
                <div
                  key={field.field}
                  className={`
                    px-4 py-3 border-b last:border-b-0 transition-colors
                    ${isMapped ? 'bg-green-50' : ''}
                    ${field.required ? 'bg-red-50' : ''}
                  `}
                  onClick={() => {
                    if (selectedMapping !== null && !readOnly) {
                      updateMapping(selectedMapping, {
                        targetField: field.field,
                        dataType: field.dataType,
                        required: field.required,
                        mapped: true,
                      });
                      setSelectedMapping(null);
                    }
                  }}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="text-sm font-medium text-gray-900">{field.displayName}</span>
                      <span className="text-xs text-gray-500 ml-2">{field.dataType}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      {field.required && (
                        <span className="text-xs bg-red-100 text-red-600 px-1.5 py-0.5 rounded">
                          Required
                        </span>
                      )}
                      {isMapped ? (
                        <span className="text-green-600 text-sm">✓</span>
                      ) : (
                        <span className="text-gray-400 text-sm">—</span>
                      )}
                    </div>
                  </div>
                  {field.description && (
                    <p className="mt-1 text-xs text-gray-500">{field.description}</p>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Mapping Detail Panel */}
      {selectedMapping !== null && mappings[selectedMapping] && (
        <div className="bg-white border rounded-lg p-4">
          <div className="flex items-center justify-between mb-4">
            <h4 className="text-sm font-medium text-gray-700">
              Configure Mapping: {mappings[selectedMapping].sourceColumn}
            </h4>
            <button
              onClick={() => setSelectedMapping(null)}
              className="text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-xs text-gray-500 mb-1">Source Column</label>
              <input
                type="text"
                value={mappings[selectedMapping].sourceColumn}
                disabled
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md bg-gray-50"
              />
            </div>

            <div>
              <label className="block text-xs text-gray-500 mb-1">Target Field</label>
              <select
                value={mappings[selectedMapping].targetField}
                onChange={(e) => updateMapping(selectedMapping, { targetField: e.target.value, mapped: !!e.target.value })}
                disabled={readOnly}
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-avisys-blue"
              >
                <option value="">Select target field...</option>
                {targetFields.map((field) => (
                  <option key={field.field} value={field.field}>
                    {field.displayName} {field.required ? '(Required)' : ''}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs text-gray-500 mb-1">Transformer</label>
              <select
                value={mappings[selectedMapping].transformer || ''}
                onChange={(e) => updateMapping(selectedMapping, { transformer: e.target.value || undefined })}
                disabled={readOnly}
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-avisys-blue"
              >
                {TRANSFORMERS.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-2 mt-4">
            <button
              onClick={() => clearMapping(selectedMapping)}
              disabled={readOnly}
              className="px-3 py-2 text-sm text-red-600 hover:bg-red-50 rounded-md transition-colors disabled:opacity-50"
            >
              Remove Mapping
            </button>
            {onPreview && (
              <button
                onClick={() => onPreview(mappings)}
                disabled={readOnly}
                className="px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-md transition-colors disabled:opacity-50"
              >
                Preview
              </button>
            )}
          </div>
        </div>
      )}

      {/* Unmapped columns warning */}
      {mappings.some((m) => !m.mapped) && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-start gap-3">
          <span className="text-yellow-500 text-lg">⚠</span>
          <div>
            <p className="text-yellow-800 font-medium">Unmapped Columns</p>
            <p className="text-yellow-600 text-sm mt-1">
              {mappings.filter((m) => !m.mapped).length} column(s) are not mapped to any target field.
              These columns will be ignored during import.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
