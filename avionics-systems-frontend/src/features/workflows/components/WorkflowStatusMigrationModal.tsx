import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { workflowApi, type WorkflowStatusLink } from '../../../api/workflowApi';

interface Props {
  workflowId: string;
  statuses: WorkflowStatusLink[];
  onClose: () => void;
}

export default function WorkflowStatusMigrationModal({ workflowId, statuses, onClose }: Props) {
  const [oldStatusId, setOldStatusId] = useState('');
  const [newStatusId, setNewStatusId] = useState('');
  const [migrationId, setMigrationId] = useState<string | null>(null);

  const createMigration = useMutation({
    mutationFn: () =>
      workflowApi
        .createStatusMigration({
          workflowId,
          oldStatusId,
          newStatusId,
          migrationType: 'STATUS_REMAP',
        })
        .then((r) => r.data),
    onSuccess: (data) => setMigrationId(data.id),
  });

  const { data: preview } = useQuery({
    queryKey: ['status-migration-preview', migrationId, oldStatusId, newStatusId],
    queryFn: () =>
      workflowApi
        .previewStatusMigration(migrationId!, oldStatusId, newStatusId)
        .then((r) => r.data),
    enabled: !!migrationId && !!oldStatusId && !!newStatusId,
  });

  const executeMigration = useMutation({
    mutationFn: () => workflowApi.executeStatusMigration(migrationId!).then((r) => r.data),
  });

  const migrationStatus = useQuery({
    queryKey: ['status-migration', migrationId],
    queryFn: () => workflowApi.getStatusMigration(migrationId!).then((r) => r.data),
    enabled: !!migrationId && executeMigration.isSuccess,
    refetchInterval: executeMigration.isSuccess ? 2000 : false,
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-lg w-full p-6 space-y-4">
        <h2 className="text-lg font-semibold">Status migration wizard</h2>
        <p className="text-sm text-gray-600">
          Remap issues from one workflow status to another. Preview affected issues before executing.
        </p>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs text-gray-600 mb-1">From status</label>
            <select className="ab-select w-full" value={oldStatusId} onChange={(e) => setOldStatusId(e.target.value)}>
              <option value="">Select…</option>
              {statuses.map((s) => (
                <option key={s.statusId} value={s.statusId}>
                  {s.statusName ?? s.statusId}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs text-gray-600 mb-1">To status</label>
            <select className="ab-select w-full" value={newStatusId} onChange={(e) => setNewStatusId(e.target.value)}>
              <option value="">Select…</option>
              {statuses.map((s) => (
                <option key={s.statusId} value={s.statusId}>
                  {s.statusName ?? s.statusId}
                </option>
              ))}
            </select>
          </div>
        </div>

        {!migrationId && (
          <button
            type="button"
            className="ab-btn ab-btn-primary"
            disabled={!oldStatusId || !newStatusId || createMigration.isPending}
            onClick={() => createMigration.mutate()}
          >
            {createMigration.isPending ? 'Creating…' : 'Create migration & preview'}
          </button>
        )}

        {preview && (
          <div className="bg-gray-50 border rounded p-3 text-sm">
            <p>
              <strong>{preview.affectedIssueCount ?? 0}</strong> issue(s) will be affected.
            </p>
            {preview.sampleIssues && preview.sampleIssues.length > 0 && (
              <ul className="mt-2 text-xs text-gray-600">
                {preview.sampleIssues.slice(0, 5).map((i) => (
                  <li key={i.issueId}>
                    {i.issueKey} — {i.currentStatus}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        {migrationId && !executeMigration.isSuccess && (
          <button
            type="button"
            className="ab-btn ab-btn-primary"
            disabled={executeMigration.isPending}
            onClick={() => executeMigration.mutate()}
          >
            {executeMigration.isPending ? 'Executing…' : 'Execute migration'}
          </button>
        )}

        {migrationStatus.data && (
          <p className="text-sm">
            Status: <strong>{migrationStatus.data.status}</strong>
            {migrationStatus.data.migratedIssues != null && (
              <span>
                {' '}
                — {migrationStatus.data.migratedIssues}/{migrationStatus.data.totalIssues} migrated
              </span>
            )}
          </p>
        )}

        <div className="flex justify-end gap-2">
          {migrationStatus.data?.status === 'FAILED' && (
            <button
              type="button"
              className="ab-btn ab-btn-warning"
              onClick={() => workflowApi.retryStatusMigration(migrationId!).then(() => migrationStatus.refetch())}
            >
              Retry failed items
            </button>
          )}
          {migrationStatus.data?.status === 'IN_PROGRESS' && (
            <button
              type="button"
              className="ab-btn ab-btn-danger"
              onClick={() => workflowApi.cancelStatusMigration(migrationId!).then(() => migrationStatus.refetch())}
            >
              Cancel migration
            </button>
          )}
          <button type="button" className="ab-btn ab-btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
