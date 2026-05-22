import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { workflowApi, type WorkflowVersion } from '../../../api/workflowApi';

interface Props {
  workflowId: string;
  versions: WorkflowVersion[];
}

export default function WorkflowVersionHistoryPanel({ workflowId, versions }: Props) {
  const queryClient = useQueryClient();
  const [confirmVersion, setConfirmVersion] = useState<number | null>(null);

  const rollback = useMutation({
    mutationFn: (versionNumber: number) => workflowApi.rollbackToVersion(workflowId, versionNumber),
    onSuccess: () => {
      setConfirmVersion(null);
      queryClient.invalidateQueries({ queryKey: ['workflow-detail', workflowId] });
      queryClient.invalidateQueries({ queryKey: ['workflow-layout', workflowId] });
    },
  });

  return (
    <section className="wf-panel">
      <h2>Version history</h2>
      <ul className="wf-version-list">
        {versions.length === 0 ? (
          <li className="wf-muted">No published versions yet.</li>
        ) : (
          versions.map((v) => (
            <li key={v.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
              <span>
                <strong>v{v.versionNumber}</strong> — {v.changeDescription}
                <span className="wf-muted">
                  {' '}
                  ({v.changeType}) · {new Date(v.createdAt).toLocaleString()}
                </span>
              </span>
              {confirmVersion === v.versionNumber ? (
                <span style={{ display: 'flex', gap: 8 }}>
                  <button
                    type="button"
                    className="ab-btn ab-btn-sm ab-btn-primary"
                    disabled={rollback.isPending}
                    onClick={() => rollback.mutate(v.versionNumber)}
                  >
                    {rollback.isPending ? 'Rolling back…' : 'Confirm rollback'}
                  </button>
                  <button type="button" className="ab-btn ab-btn-sm ab-btn-ghost" onClick={() => setConfirmVersion(null)}>
                    Cancel
                  </button>
                </span>
              ) : (
                <button
                  type="button"
                  className="ab-btn ab-btn-sm ab-btn-secondary"
                  onClick={() => setConfirmVersion(v.versionNumber)}
                >
                  Rollback
                </button>
              )}
            </li>
          ))
        )}
      </ul>
      {rollback.isError && (
        <p className="wf-muted" style={{ color: '#de350b', marginTop: 8 }} role="alert">
          Rollback failed. Ensure workflow service is running and you have publish rights.
        </p>
      )}
    </section>
  );
}
