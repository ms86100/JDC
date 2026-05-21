import React from 'react';

interface ProjectOption {
  id: string;
  name: string;
  projectKey: string;
}

interface Props {
  projects: ProjectOption[];
  sourceProjectId: string;
  targetProjectId: string;
  onSourceChange: (id: string) => void;
  onTargetChange: (id: string) => void;
}

export default function ProjectImportPanel({
  projects,
  sourceProjectId,
  targetProjectId,
  onSourceChange,
  onTargetChange,
}: Props) {
  return (
    <div className="bg-white rounded-lg border p-6 space-y-4">
      <h3 className="text-lg font-semibold">Project-to-project import</h3>
      <p className="text-sm text-gray-600">
        Copies workflows, issues, components, schemes, and related entities from the source project into the
        target project. Progress is tracked per entity type in the job detail view.
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Source project *</label>
          <select
            value={sourceProjectId}
            onChange={(e) => onSourceChange(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg"
          >
            <option value="">Select source...</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.projectKey})
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Target project *</label>
          <select
            value={targetProjectId}
            onChange={(e) => onTargetChange(e.target.value)}
            className="w-full px-3 py-2 border rounded-lg"
          >
            <option value="">Select target...</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id} disabled={p.id === sourceProjectId}>
                {p.name} ({p.projectKey})
              </option>
            ))}
          </select>
        </div>
      </div>
      {sourceProjectId && targetProjectId && sourceProjectId === targetProjectId && (
        <p className="text-sm text-red-600">Source and target must be different projects.</p>
      )}
    </div>
  );
}
