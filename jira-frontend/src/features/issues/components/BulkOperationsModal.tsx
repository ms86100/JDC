import React, { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { bulkApi, BulkOperationType } from '../../../api/bulkApi';
import { workflowApi } from '../../../api/workflowApi';
import { issueApi, IssueResponse } from '../../../api/issueApi';

interface BulkOperationsModalProps {
  issues: IssueResponse[];
  projectId?: string;
  onClose: () => void;
  onComplete?: () => void;
}

type Step = 'select' | 'configure' | 'result';

const OPERATIONS: { type: BulkOperationType; label: string; description: string; danger?: boolean }[] = [
  { type: 'UPDATE_FIELDS', label: 'Edit fields', description: 'Assignee, priority' },
  { type: 'UPDATE_STATUS', label: 'Workflow transition', description: 'Execute workflow transition (validated)' },
  { type: 'ADD_LABELS', label: 'Add labels', description: 'Comma-separated labels' },
  { type: 'CLONE', label: 'Clone', description: 'Duplicate selected issues' },
  { type: 'DELETE', label: 'Delete', description: 'Permanently delete', danger: true },
];

export default function BulkOperationsModal({
  issues,
  projectId,
  onClose,
  onComplete,
}: BulkOperationsModalProps) {
  const [step, setStep] = useState<Step>('select');
  const [op, setOp] = useState<BulkOperationType | null>(null);
  const [config, setConfig] = useState({
    transitionId: '',
    assigneeId: '',
    priority: '',
    labels: '',
  });

  const resolvedProjectId = projectId ?? issues[0]?.projectId;
  const sampleIssueId = issues[0]?.id;

  const { data: availableTransitions } = useQuery({
    queryKey: ['bulk-available-transitions', sampleIssueId, resolvedProjectId],
    queryFn: () =>
      workflowApi
        .getAvailableTransitions(sampleIssueId!, resolvedProjectId!)
        .then((r) => r.data.transitions ?? []),
    enabled: op === 'UPDATE_STATUS' && !!sampleIssueId && !!resolvedProjectId,
  });

  const { data: priorities = [] } = useQuery({
    queryKey: ['priorities-bulk'],
    queryFn: async () => (await issueApi.getPriorities()).data,
  });

  const workflowBulkMutation = useMutation({
    mutationFn: () =>
      workflowApi.executeBulkTransitions({
        projectId: resolvedProjectId!,
        items: issues.map((i) => ({
          issueId: i.id,
          transitionId: config.transitionId,
        })),
      }),
  });

  const executeMutation = useMutation({
    mutationFn: async () => {
      if (op === 'UPDATE_STATUS' && config.transitionId) {
        const res = await workflowBulkMutation.mutateAsync();
        const data = res.data;
        return {
          data: {
            status: data.failed === 0 ? 'COMPLETED' : 'PARTIAL_SUCCESS',
            successCount: data.succeeded,
            failedCount: data.failed,
            results: data.results.map((row) => ({
              issueKey: issues.find((i) => i.id === row.issueId)?.issueKey ?? row.issueId,
              success: row.success,
              message: row.success ? 'Transition executed' : (row.error ?? 'Failed'),
            })),
          },
        };
      }
      return bulkApi.execute({
        issueIds: issues.map((i) => i.id),
        operationType: op!,
        projectId: resolvedProjectId,
        assigneeId: config.assigneeId || undefined,
        priority: config.priority || undefined,
        labels: config.labels || undefined,
      });
    },
    onSuccess: () => setStep('result'),
  });

  const result = executeMutation.data?.data;

  return (
    <div className="ab-bulk-overlay" onClick={onClose}>
      <div className="ab-bulk-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ab-bulk-header">
          <h2>Bulk change</h2>
          <button type="button" className="ab-close-btn" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="ab-bulk-content">
          {step === 'select' && (
            <>
              <p className="ab-bulk-info">
                <strong>{issues.length}</strong> issue(s) selected
              </p>
              <div className="ab-operation-grid">
                {OPERATIONS.map((o) => (
                  <button
                    key={o.type}
                    type="button"
                    className={`ab-operation-card ${o.danger ? 'danger' : ''}`}
                    onClick={() => {
                      setOp(o.type);
                      setStep('configure');
                    }}
                  >
                    <span className="ab-op-label">{o.label}</span>
                    <span className="ab-op-desc">{o.description}</span>
                  </button>
                ))}
              </div>
            </>
          )}

          {step === 'configure' && op && (
            <>
              <h3>Configure: {OPERATIONS.find((x) => x.type === op)?.label}</h3>
              {op === 'UPDATE_STATUS' && (
                <div className="ab-form-group">
                  <label className="ab-label">Workflow transition</label>
                  <p className="ab-bulk-info" style={{ marginBottom: 8 }}>
                    From first issue ({issues[0]?.issueKey}). Same transition applied when legal per issue.
                  </p>
                  {!resolvedProjectId && (
                    <p className="ab-bulk-info" style={{ color: '#b45309' }}>
                      Select issues from one project for workflow transitions.
                    </p>
                  )}
                  <select
                    className="ab-select"
                    value={config.transitionId}
                    onChange={(e) => setConfig({ ...config, transitionId: e.target.value })}
                    disabled={!resolvedProjectId}
                  >
                    <option value="">Select transition…</option>
                    {(availableTransitions ?? []).map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name}
                        {t.toStatusName ? ` → ${t.toStatusName}` : ''}
                      </option>
                    ))}
                  </select>
                </div>
              )}
              {op === 'UPDATE_FIELDS' && (
                <>
                  <div className="ab-form-group">
                    <label className="ab-label">Assignee ID (UUID)</label>
                    <input
                      className="ab-input"
                      value={config.assigneeId}
                      onChange={(e) => setConfig({ ...config, assigneeId: e.target.value })}
                    />
                  </div>
                  <div className="ab-form-group">
                    <label className="ab-label">Priority</label>
                    <select
                      className="ab-select"
                      value={config.priority}
                      onChange={(e) => setConfig({ ...config, priority: e.target.value })}
                    >
                      <option value="">No change</option>
                      {priorities.map((p) => (
                        <option key={p.id} value={p.name}>
                          {p.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </>
              )}
              {op === 'ADD_LABELS' && (
                <div className="ab-form-group">
                  <label className="ab-label">Labels</label>
                  <input
                    className="ab-input"
                    placeholder="bug, frontend"
                    value={config.labels}
                    onChange={(e) => setConfig({ ...config, labels: e.target.value })}
                  />
                </div>
              )}
              {op === 'DELETE' && (
                <div className="ab-danger-warning">
                  <p>This cannot be undone.</p>
                </div>
              )}
              <div className="ab-config-actions">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setStep('select')}>
                  Back
                </button>
                <button
                  type="button"
                  className="ab-btn ab-btn-primary"
                  disabled={
                    executeMutation.isPending
                    || (op === 'UPDATE_STATUS' && (!config.transitionId || !resolvedProjectId))
                  }
                  onClick={() => executeMutation.mutate()}
                >
                  {executeMutation.isPending ? 'Applying…' : 'Apply'}
                </button>
              </div>
              {executeMutation.isError && (
                <p style={{ color: '#dc2626', marginTop: 12 }} role="alert">
                  Bulk operation failed. Check workflow and issue service logs.
                </p>
              )}
            </>
          )}

          {step === 'result' && result && (
            <>
              <p className="ab-bulk-info">
                {'status' in result ? String(result.status) : 'Done'}:{' '}
                {'successCount' in result ? result.successCount : 0} succeeded,{' '}
                {'failedCount' in result ? result.failedCount : 0} failed
              </p>
              {'results' in result && Array.isArray(result.results) && (
                <ul style={{ maxHeight: 200, overflow: 'auto', fontSize: 13 }}>
                  {result.results.slice(0, 15).map((r, i) => (
                    <li key={i}>
                      <strong>{r.issueKey}</strong> — {r.message}
                    </li>
                  ))}
                </ul>
              )}
              <div className="ab-config-actions">
                <button
                  type="button"
                  className="ab-btn ab-btn-primary"
                  onClick={() => {
                    onComplete?.();
                    onClose();
                  }}
                >
                  Done
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
