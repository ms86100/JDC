import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationWizardApi } from '../../../api/serviceApi';

interface Props {
  sessionId: string;
}

export default function UploadPreviewTable({ sessionId }: Props) {
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['wizard-upload-preview', sessionId, page],
    queryFn: () => migrationWizardApi.getPreview(sessionId, page, pageSize).then((r) => r.data),
    enabled: !!sessionId,
    retry: 1,
  });

  const rows = data?.previewRows ?? [];
  const headers = rows[0] ?? [];
  const bodyRows = rows.slice(1);
  const total =
    typeof data?.totalRows === 'number'
      ? data.totalRows
      : ((data?.sessionData?.previewTotalRows as number) ?? Math.max(0, rows.length - 1));
  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  return (
    <div className="bg-white rounded-lg border p-4 space-y-3" data-testid="upload-preview-table">
      <h3 className="font-semibold">Upload preview (server)</h3>
      {isLoading && <p className="text-sm text-gray-500">Loading preview…</p>}
      {isError && (
        <div className="text-sm text-amber-800 bg-amber-50 border border-amber-200 rounded p-2 space-y-2">
          <p>
            Server preview unavailable
            {error instanceof Error ? `: ${error.message}` : ''}. Column mapping still works from
            client-side parse if upload succeeded.
          </p>
          <button type="button" className="underline" onClick={() => refetch()}>
            Retry preview
          </button>
        </div>
      )}
      {!isLoading && !isError && rows.length === 0 && (
        <p className="text-sm text-gray-500">
          No preview rows yet. Re-upload the CSV or ensure migration-service is running on port 8094.
        </p>
      )}
      {!isLoading && !isError && headers.length > 0 && bodyRows.length === 0 && (
        <p className="text-sm text-gray-600">
          Detected {headers.length} columns; no data rows in this page (header-only preview).
        </p>
      )}
      {bodyRows.length > 0 && (
        <div className="overflow-x-auto">
          <table className="min-w-full text-xs border-collapse">
            <thead>
              <tr className="bg-gray-50">
                {headers.map((h, i) => (
                  <th key={i} className="border px-2 py-1 text-left">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {bodyRows.map((row, ri) => (
                <tr key={ri}>
                  {row.map((cell, ci) => (
                    <td key={ci} className="border px-2 py-1 max-w-[200px] truncate">
                      {cell}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="flex items-center gap-2 text-sm text-gray-600">
        <button
          type="button"
          className="px-2 py-1 border rounded disabled:opacity-50"
          disabled={page === 0}
          onClick={() => setPage((p) => p - 1)}
        >
          Prev
        </button>
        <span>
          Page {page + 1} of {totalPages} ({total} rows)
        </span>
        <button
          type="button"
          className="px-2 py-1 border rounded disabled:opacity-50"
          disabled={page + 1 >= totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}
