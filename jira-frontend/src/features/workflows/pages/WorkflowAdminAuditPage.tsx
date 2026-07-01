import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { workflowAdminApi } from '../../../api/workflowAdminApi';

export default function WorkflowAdminAuditPage() {
  const [limit, setLimit] = useState(50);
  const [workflowId, setWorkflowId] = useState('');

  const { data: globalLog = [], isLoading: globalLoading } = useQuery({
    queryKey: ['workflow-admin', 'audit-global', limit],
    queryFn: () =>
      workflowAdminApi.globalAuditLog({ size: limit }).then((r) => r.data as unknown[]),
  });

  const { data: workflowLog = [], isLoading: wfLoading } = useQuery({
    queryKey: ['workflow-admin', 'audit-wf', workflowId, limit],
    queryFn: () =>
      workflowAdminApi.auditLog(workflowId, { size: limit }).then((r) => r.data as unknown[]),
    enabled: !!workflowId,
  });

  return (
    <div className="space-y-4" data-testid="workflow-admin-audit">
      <div className="bg-white border rounded-lg p-4 space-y-3">
        <h2 className="font-semibold">Per-workflow audit</h2>
        <div className="flex flex-wrap gap-2 items-end">
          <div className="flex-1 min-w-[200px]">
            <label className="text-xs text-gray-600">Workflow ID (UUID)</label>
            <input
              className="w-full border rounded px-3 py-2 text-sm font-mono"
              value={workflowId}
              onChange={(e) => setWorkflowId(e.target.value)}
              placeholder="Paste workflow UUID…"
            />
          </div>
          <div>
            <label className="text-xs text-gray-600">Limit</label>
            <input
              type="number"
              min={1}
              max={200}
              className="w-24 border rounded px-3 py-2 text-sm"
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value) || 50)}
            />
          </div>
        </div>
        {workflowId && (
          <pre className="text-xs overflow-auto max-h-64 bg-gray-50 p-3 rounded">
            {wfLoading ? 'Loading…' : JSON.stringify(workflowLog, null, 2)}
          </pre>
        )}
      </div>

      <div className="bg-white border rounded-lg p-4">
        <h2 className="font-semibold mb-2">Global audit log (last {limit})</h2>
        <pre className="text-xs overflow-auto max-h-96 bg-gray-50 p-3 rounded">
          {globalLoading ? 'Loading…' : JSON.stringify(globalLog, null, 2)}
        </pre>
      </div>
    </div>
  );
}
