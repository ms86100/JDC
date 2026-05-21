import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
}

export default function MigrationVerificationPanel({ jobId }: Props) {
  const { data, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['migration', 'verification', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobVerification(jobId!);
      return res.data as {
        status?: string;
        checks?: string[];
        issueSuccess?: number;
        issueFailed?: number;
        attachmentSuccess?: number;
        attachmentFailed?: number;
      };
    },
    enabled: !!jobId,
  });

  if (!jobId) return null;

  const status = data?.status || 'PENDING';
  const statusColor =
    status === 'PASSED' ? 'text-green-700 bg-green-50 border-green-200'
    : status === 'WARN' ? 'text-amber-800 bg-amber-50 border-amber-200'
    : 'text-red-700 bg-red-50 border-red-200';

  return (
    <div className={`rounded-lg border p-4 ${statusColor}`}>
      <div className="flex items-center justify-between mb-2">
        <h4 className="font-semibold">Post-migration verification</h4>
        <button
          type="button"
          onClick={() => refetch()}
          disabled={isFetching}
          className="text-xs underline"
        >
          {isFetching ? 'Running…' : 'Re-run checks'}
        </button>
      </div>
      {isLoading ? (
        <p className="text-sm">Running verification…</p>
      ) : (
        <>
          <p className="text-sm font-medium mb-2">Overall: {status}</p>
          <ul className="text-sm space-y-1">
            {(data?.checks || []).map((c, i) => (
              <li key={i}>{c}</li>
            ))}
          </ul>
          {data && (
            <p className="text-xs mt-2 opacity-80">
              Issues {data.issueSuccess}/{Number(data.issueSuccess) + Number(data.issueFailed)} · Attachments{' '}
              {data.attachmentSuccess}/{Number(data.attachmentSuccess) + Number(data.attachmentFailed)}
            </p>
          )}
        </>
      )}
    </div>
  );
}
