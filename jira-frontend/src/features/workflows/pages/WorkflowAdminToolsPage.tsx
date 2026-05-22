import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { workflowApi } from '../../../api/workflowApi';
import { workflowAdminApi } from '../../../api/workflowAdminApi';

export default function WorkflowAdminToolsPage() {
  const [selectedId, setSelectedId] = useState('');
  const [importJson, setImportJson] = useState('');
  const [actionResult, setActionResult] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const { data: workflows = [] } = useQuery({
    queryKey: ['workflows'],
    queryFn: () => workflowApi.getAll().then((r) => r.data),
  });

  const { data: usage } = useQuery({
    queryKey: ['workflow-admin', 'usage', selectedId],
    queryFn: () => workflowAdminApi.usage(selectedId).then((r) => r.data),
    enabled: !!selectedId,
  });

  const { data: audit } = useQuery({
    queryKey: ['workflow-admin', 'audit', selectedId],
    queryFn: () => workflowAdminApi.auditLog(selectedId, { limit: 50 }).then((r) => r.data),
    enabled: !!selectedId,
  });

  const run = async (label: string, fn: () => Promise<unknown>) => {
    setActionError(null);
    setActionResult(null);
    try {
      const res = await fn();
      setActionResult(`${label}: ${JSON.stringify(res, null, 2)}`);
    } catch (e) {
      setActionError(e instanceof Error ? e.message : `${label} failed`);
    }
  };

  return (
    <div className="max-w-5xl mx-auto p-6 space-y-6" data-testid="workflow-admin-tools-page">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Workflow admin tools</h1>
          <p className="text-sm text-gray-600 mt-1">
            Wired orphan <code className="text-xs bg-gray-100 px-1 rounded">/api/admin/workflows/*</code> endpoints
            (export, import, validate, migrate, usage, audit, revert).
          </p>
        </div>
        <Link to="/workflows/admin" className="text-sm text-jira-blue hover:underline">
          ← Workflow administration
        </Link>
      </div>

      <div className="bg-white border rounded-lg p-4 space-y-3">
        <label className="block text-sm font-medium text-gray-700">Workflow</label>
        <select
          className="w-full border rounded px-3 py-2 text-sm"
          value={selectedId}
          onChange={(e) => setSelectedId(e.target.value)}
          data-testid="workflow-admin-select"
        >
          <option value="">Select workflow…</option>
          {workflows.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name}
            </option>
          ))}
        </select>
      </div>

      {selectedId && (
        <div className="grid md:grid-cols-2 gap-4">
          <div className="bg-white border rounded-lg p-4 space-y-2">
            <h2 className="font-semibold text-sm">Actions</h2>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                className="px-3 py-1.5 text-xs bg-gray-100 rounded hover:bg-gray-200"
                onClick={() => run('Export', () => workflowAdminApi.exportWorkflow(selectedId))}
              >
                Export JSON
              </button>
              <button
                type="button"
                className="px-3 py-1.5 text-xs bg-gray-100 rounded hover:bg-gray-200"
                onClick={() => run('Validate', () => workflowAdminApi.validate(selectedId))}
              >
                Validate
              </button>
              <button
                type="button"
                className="px-3 py-1.5 text-xs bg-gray-100 rounded hover:bg-gray-200"
                onClick={() =>
                  run('Migration preview', () => workflowAdminApi.migrationPreview(selectedId))
                }
              >
                Migration preview
              </button>
              <button
                type="button"
                className="px-3 py-1.5 text-xs bg-gray-100 rounded hover:bg-gray-200"
                onClick={() => run('Transition stats', () => workflowAdminApi.transitionStats(selectedId))}
              >
                Transition stats
              </button>
              <button
                type="button"
                className="px-3 py-1.5 text-xs bg-amber-100 rounded hover:bg-amber-200"
                onClick={() =>
                  run('Revert v1', () => workflowAdminApi.revertVersion(selectedId, 1))
                }
              >
                Revert to v1 (admin)
              </button>
            </div>
          </div>

          <div className="bg-white border rounded-lg p-4">
            <h2 className="font-semibold text-sm mb-2">Usage</h2>
            <pre className="text-xs overflow-auto max-h-40 bg-gray-50 p-2 rounded">
              {JSON.stringify(usage ?? {}, null, 2)}
            </pre>
          </div>

          <div className="bg-white border rounded-lg p-4 md:col-span-2">
            <h2 className="font-semibold text-sm mb-2">Audit log (last 50)</h2>
            <pre className="text-xs overflow-auto max-h-48 bg-gray-50 p-2 rounded">
              {JSON.stringify(audit ?? [], null, 2)}
            </pre>
          </div>
        </div>
      )}

      <div className="bg-white border rounded-lg p-4 space-y-3">
        <h2 className="font-semibold text-sm">Import workflow JSON</h2>
        <textarea
          className="w-full border rounded p-2 text-xs font-mono h-32"
          placeholder='{"name":"Imported workflow",...}'
          value={importJson}
          onChange={(e) => setImportJson(e.target.value)}
        />
        <button
          type="button"
          className="px-4 py-2 bg-jira-blue text-white text-sm rounded"
          onClick={() => {
            const payload = JSON.parse(importJson) as Record<string, unknown>;
            return run('Import', () => workflowAdminApi.importWorkflow(payload));
          }}
        >
          Import via admin API
        </button>
      </div>

      {actionError && (
        <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-3">{actionError}</p>
      )}
      {actionResult && (
        <pre className="text-xs bg-gray-50 border rounded p-3 overflow-auto max-h-64">{actionResult}</pre>
      )}
    </div>
  );
}
