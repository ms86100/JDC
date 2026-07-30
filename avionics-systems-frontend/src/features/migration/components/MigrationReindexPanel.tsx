import React from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
}

export default function MigrationReindexPanel({ jobId }: Props) {
  const { data, refetch, isFetching } = useQuery({
    queryKey: ['migration', 'reindex', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobReindexStatus(jobId!);
      return res.data as {
        status?: string;
        success?: boolean;
        statusByType?: Record<string, { status?: string; errorMessage?: string }>;
      };
    },
    enabled: !!jobId,
    refetchInterval: (q) =>
      q.state.data?.status === 'STARTED' || q.state.data?.status === 'NOT_STARTED' ? false : 5000,
  });

  const trigger = useMutation({
    mutationFn: async () => {
      await migrationApi.triggerJobReindex(jobId!);
    },
    onSuccess: () => refetch(),
  });

  if (!jobId) return null;

  const statusByType = data?.statusByType || {};

  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-center justify-between mb-2">
        <h4 className="font-semibold">Search reindex</h4>
        <button
          type="button"
          onClick={() => trigger.mutate()}
          disabled={trigger.isPending}
          className="text-xs px-2 py-1 bg-indigo-600 text-white rounded hover:bg-indigo-700 disabled:opacity-50"
        >
          {trigger.isPending ? 'Starting…' : 'Run reindex'}
        </button>
      </div>
      {isFetching && !data ? (
        <p className="text-sm text-gray-500">Loading reindex status…</p>
      ) : (
        <>
          <p className="text-sm mb-2">
            Status: <span className="font-medium">{data?.status || 'NOT_STARTED'}</span>
            {data?.success === false && <span className="text-red-600 ml-2">(partial failure)</span>}
          </p>
          {Object.keys(statusByType).length > 0 && (
            <ul className="text-xs space-y-1 text-gray-600">
              {Object.entries(statusByType).map(([type, row]) => (
                <li key={type}>
                  {type}: {row.status}
                  {row.errorMessage && <span className="text-red-500"> — {row.errorMessage}</span>}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
}
