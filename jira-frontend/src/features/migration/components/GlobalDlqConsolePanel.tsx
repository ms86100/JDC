import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';
import { canAdminMigration, canWriteMigration } from '../utils/migrationRoleUtils';

export default function GlobalDlqConsolePanel() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const canWrite = canWriteMigration();
  const canAdmin = canAdminMigration();

  const { data: stats } = useQuery({
    queryKey: ['migration-global-dlq-stats'],
    queryFn: () => migrationApi.getGlobalDlqStats().then((r) => r.data),
  });

  const { data: dlqPage, isLoading } = useQuery({
    queryKey: ['migration-global-dlq', page],
    queryFn: () => migrationApi.listGlobalDlq(page, 20).then((r) => r.data),
  });

  const retryOne = useMutation({
    mutationFn: (id: string) => migrationApi.retryGlobalDlq(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['migration-global-dlq'] });
      queryClient.invalidateQueries({ queryKey: ['migration-global-dlq-stats'] });
    },
  });

  const retryAll = useMutation({
    mutationFn: () => migrationApi.retryAllGlobalDlq(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['migration-global-dlq'] });
      queryClient.invalidateQueries({ queryKey: ['migration-global-dlq-stats'] });
    },
  });

  const purge = useMutation({
    mutationFn: () => migrationApi.purgeGlobalDlq(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['migration-global-dlq'] });
      queryClient.invalidateQueries({ queryKey: ['migration-global-dlq-stats'] });
    },
  });

  const entries = dlqPage?.content ?? [];

  return (
    <div className="bg-white rounded-lg border p-6 space-y-4" data-testid="global-dlq-console">
      <h2 className="text-lg font-semibold">Global dead-letter queue</h2>
      {!canWrite && (
        <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded px-3 py-2">
          Viewer role: retry and purge actions are disabled. Switch role to Operator or Admin.
        </p>
      )}

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
          <div className="border rounded p-3">
            <span className="text-gray-500">Total</span>
            <p className="font-semibold text-lg">{String(stats.totalEntries ?? 0)}</p>
          </div>
          <div className="border rounded p-3">
            <span className="text-gray-500">Pending</span>
            <p className="font-semibold text-lg">{String(stats.pendingCount ?? 0)}</p>
          </div>
          <div className="border rounded p-3">
            <span className="text-gray-500">Queue usage</span>
            <p className="font-semibold text-lg">
              {typeof stats.queueUsagePercentage === 'number'
                ? `${stats.queueUsagePercentage.toFixed(1)}%`
                : '—'}
            </p>
          </div>
          <div className="border rounded p-3 flex flex-col gap-2">
            {canWrite && (
              <button
                type="button"
                className="px-3 py-1 text-sm border rounded hover:bg-gray-50 disabled:opacity-50"
                disabled={retryAll.isPending}
                onClick={() => retryAll.mutate()}
              >
                Retry all pending
              </button>
            )}
            {canAdmin && (
              <button
                type="button"
                className="px-3 py-1 text-sm text-red-700 border border-red-200 rounded hover:bg-red-50 disabled:opacity-50"
                disabled={purge.isPending}
                onClick={() => purge.mutate()}
              >
                Purge completed
              </button>
            )}
          </div>
        </div>
      )}

      {isLoading && <p className="text-gray-500">Loading DLQ entries…</p>}
      <table className="min-w-full text-sm border-collapse">
        <thead>
          <tr className="border-b text-left text-gray-600">
            <th className="py-2 pr-3">Operation</th>
            <th className="py-2 pr-3">Entity</th>
            <th className="py-2 pr-3">Key</th>
            <th className="py-2 pr-3">Error</th>
            <th className="py-2" />
          </tr>
        </thead>
        <tbody>
          {entries.length === 0 && (
            <tr>
              <td colSpan={5} className="py-4 text-gray-500">
                No DLQ entries.
              </td>
            </tr>
          )}
          {entries.map((row) => {
            const id = String(row.id ?? '');
            return (
              <tr key={id} className="border-b border-gray-100">
                <td className="py-2 pr-3">{String(row.operationType ?? '—')}</td>
                <td className="py-2 pr-3">{String(row.entityType ?? '—')}</td>
                <td className="py-2 pr-3 font-mono text-xs">{String(row.entityKey ?? '—')}</td>
                <td className="py-2 pr-3 text-xs text-gray-600 max-w-xs truncate">
                  {String(row.errorMessage ?? row.lastError ?? '—')}
                </td>
                <td className="py-2">
                  {canWrite && id && (
                    <button
                      type="button"
                      className="text-jira-blue text-xs hover:underline"
                      disabled={retryOne.isPending}
                      onClick={() => retryOne.mutate(id)}
                    >
                      Retry
                    </button>
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <div className="flex gap-2">
        <button
          type="button"
          className="px-3 py-1 border rounded text-sm disabled:opacity-50"
          disabled={page === 0}
          onClick={() => setPage((p) => Math.max(0, p - 1))}
        >
          Previous
        </button>
        <button
          type="button"
          className="px-3 py-1 border rounded text-sm"
          onClick={() => setPage((p) => p + 1)}
          disabled={entries.length < 20}
        >
          Next
        </button>
      </div>
    </div>
  );
}
