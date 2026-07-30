import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
}

export default function ImportedAttachmentsPanel({ jobId }: Props) {
  const { data, isLoading } = useQuery({
    queryKey: ['migration', 'attachment-results', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobAttachmentResults(jobId!);
      return res.data as Array<{
        fileName?: string;
        sourceIssueKey?: string;
        status?: string;
        checksum?: string;
        errorMessage?: string;
      }>;
    },
    enabled: !!jobId,
  });

  if (!jobId) return null;
  if (isLoading) return <p className="text-sm text-gray-500">Loading attachments…</p>;

  const rows = data || [];
  const verified = rows.filter((r) => r.status === 'SUCCESS' && r.checksum).length;

  return (
    <div className="bg-white rounded-lg border overflow-hidden">
      <div className="px-4 py-3 border-b flex justify-between items-center">
        <h4 className="font-semibold">Attachments ({rows.length})</h4>
        <span className="text-xs text-gray-500">{verified} with SHA-256 recorded</span>
      </div>
      <div className="max-h-48 overflow-y-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-2 text-left">File</th>
              <th className="px-3 py-2 text-left">Issue</th>
              <th className="px-3 py-2 text-left">Status</th>
              <th className="px-3 py-2 text-left">Checksum</th>
            </tr>
          </thead>
          <tbody>
            {rows.slice(0, 50).map((r, i) => (
              <tr key={i} className="border-t">
                <td className="px-3 py-1">{r.fileName || '—'}</td>
                <td className="px-3 py-1 font-mono text-xs">{r.sourceIssueKey || '—'}</td>
                <td className={`px-3 py-1 ${r.status === 'SUCCESS' ? 'text-green-700' : 'text-red-600'}`}>
                  {r.status}
                </td>
                <td className="px-3 py-1 font-mono text-xs truncate max-w-[120px]" title={r.checksum}>
                  {r.checksum ? `${r.checksum.slice(0, 12)}…` : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
