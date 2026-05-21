import React from 'react';
import type { MigrationTargetField } from '../types/migration';

interface ConfigureImportPanelProps {
  importMode: string;
  onImportModeChange: (mode: string) => void;
  fieldDefaults: Record<string, string>;
  onFieldDefaultsChange: (defaults: Record<string, string>) => void;
  workflowStatusMappings: Record<string, string>;
  onWorkflowStatusMappingsChange: (mappings: Record<string, string>) => void;
  requiredTargetFields: MigrationTargetField[];
}

export default function ConfigureImportPanel({
  importMode,
  onImportModeChange,
  fieldDefaults,
  onFieldDefaultsChange,
  workflowStatusMappings,
  onWorkflowStatusMappingsChange,
  requiredTargetFields,
}: ConfigureImportPanelProps) {
  const requiredFields = requiredTargetFields.filter((f) => f.required);

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg border p-6">
        <h3 className="text-lg font-semibold mb-4">Import Mode</h3>
        <select
          value={importMode}
          onChange={(e) => onImportModeChange(e.target.value)}
          className="w-full max-w-md px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
        >
          <option value="CREATE_UPDATE">Create New + Update Existing</option>
          <option value="CREATE_ONLY">Create New Only</option>
          <option value="UPDATE_ONLY">Update Existing Only</option>
        </select>
      </div>

      <div className="bg-white rounded-lg border p-6">
        <h3 className="text-lg font-semibold mb-2">Default Values</h3>
        <p className="text-sm text-gray-500 mb-4">
          Applied when source cells are empty for mandatory target fields.
        </p>
        {requiredFields.length === 0 ? (
          <p className="text-sm text-gray-400">No required fields in catalog.</p>
        ) : (
          <div className="space-y-3 max-w-lg">
            {requiredFields.map((field) => (
              <div key={field.field}>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  {field.displayName}
                  <span className="text-red-500 ml-1">*</span>
                </label>
                <input
                  type="text"
                  value={fieldDefaults[field.field] ?? ''}
                  onChange={(e) =>
                    onFieldDefaultsChange({ ...fieldDefaults, [field.field]: e.target.value })
                  }
                  placeholder={`Default for ${field.field}`}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm"
                />
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="bg-white rounded-lg border p-6">
        <h3 className="text-lg font-semibold mb-2">Status Mapping</h3>
        <p className="text-sm text-gray-500 mb-4">Map source status values to target workflow statuses.</p>
        <div className="grid grid-cols-2 gap-3 max-w-lg">
          <input
            type="text"
            placeholder="Source status (e.g. Open)"
            id="wf-source"
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
          />
          <input
            type="text"
            placeholder="Target status (e.g. To Do)"
            id="wf-target"
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
          />
        </div>
        <button
          type="button"
          className="mt-3 px-3 py-1.5 text-sm bg-gray-100 rounded hover:bg-gray-200"
          onClick={() => {
            const source = (document.getElementById('wf-source') as HTMLInputElement)?.value?.trim();
            const target = (document.getElementById('wf-target') as HTMLInputElement)?.value?.trim();
            if (source && target) {
              onWorkflowStatusMappingsChange({ ...workflowStatusMappings, [source]: target });
              (document.getElementById('wf-source') as HTMLInputElement).value = '';
              (document.getElementById('wf-target') as HTMLInputElement).value = '';
            }
          }}
        >
          Add mapping
        </button>
        {Object.keys(workflowStatusMappings).length > 0 && (
          <ul className="mt-4 space-y-1 text-sm">
            {Object.entries(workflowStatusMappings).map(([src, tgt]) => (
              <li key={src} className="flex justify-between bg-gray-50 px-3 py-2 rounded">
                <span>
                  {src} → {tgt}
                </span>
                <button
                  type="button"
                  className="text-red-600"
                  onClick={() => {
                    const next = { ...workflowStatusMappings };
                    delete next[src];
                    onWorkflowStatusMappingsChange(next);
                  }}
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
