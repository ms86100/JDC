import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
}

export default function ImportedIssuesPanel({ jobId }: Props) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['migration', 'issue-results', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobIssueResults(jobId!);
      return res.data as Array<{
        sourceIssueKey?: string;
        targetIssueKey?: string;
        status?: string;
        errorMessage?: string;
        rowNumber?: number;
      }>;
    },
    enabled: !!jobId,
  });

  if (!jobId) return null;
  if (isLoading) return <p className="text-sm text-gray-500">Loading imported issues…</p>;
  if (isError) return <p className="text-sm text-red-600">Failed to load issue results.</p>;

  const rows = data || [];

  return (
    <div className="bg-white rounded-lg border overflow-hidden">
      <div className="px-4 py-3 border-b">
        <h4 className="font-semibold">Imported issues ({rows.length})</h4>
      </div>
      <div className="max-h-64 overflow-y-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-2 text-left">Source</th>
              <th className="px-3 py-2 text-left">Target</th>
              <th className="px-3 py-2 text-left">Status</th>
            </tr>
          </thead>
          <tbody>
            {rows.slice(0, 100).map((r, i) => (
              <tr key={i} className="border-t">
                <td className="px-3 py-1 font-mono">{r.sourceIssueKey || '—'}</td>
                <td className="px-3 py-1 font-mono">{r.targetIssueKey || '—'}</td>
                <td className="px-3 py-1">
                  <span
                    className={
                      r.status === 'SUCCESS' ? 'text-green-700' : 'text-red-600'
                    }
                  >
                    {r.status}
                  </span>
                  {r.errorMessage && (
                    <span className="block text-xs text-red-500">{r.errorMessage}</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
