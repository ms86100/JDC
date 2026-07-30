import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { issueApi, IssueResponse } from '../../../api/issueApi';

export default function IssueBatchPage() {
  const [idsInput, setIdsInput] = useState('');
  const [submittedIds, setSubmittedIds] = useState<string[]>([]);

  const { data: issues = [], isLoading, error, refetch } = useQuery<IssueResponse[]>({
    queryKey: ['issues-batch', submittedIds.join(',')],
    queryFn: async () => {
      const res = await issueApi.getBatch(submittedIds);
      return res.data;
    },
    enabled: submittedIds.length > 0,
  });

  const handleLoad = () => {
    const ids = idsInput
      .split(/[\s,]+/)
      .map((s) => s.trim())
      .filter(Boolean);
    setSubmittedIds(ids);
  };

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <h1 className="text-2xl font-bold mb-2">Batch issue lookup</h1>
      <p className="text-sm text-gray-500 mb-4">
        Load multiple issues at once using{' '}
        <code className="text-xs bg-gray-100 px-1 rounded">GET /api/issues/batch?ids=...</code>
      </p>

      <div className="border rounded-lg p-4 bg-white mb-4">
        <label className="block text-sm font-medium mb-2">Issue IDs (comma or newline separated)</label>
        <textarea
          className="w-full border rounded px-3 py-2 text-sm font-mono"
          rows={4}
          placeholder="uuid-1, uuid-2, uuid-3"
          value={idsInput}
          onChange={(e) => setIdsInput(e.target.value)}
        />
        <button
          type="button"
          className="mt-3 px-4 py-2 bg-blue-600 text-white rounded text-sm"
          onClick={handleLoad}
        >
          Load issues
        </button>
      </div>

      {isLoading && <p className="text-gray-500">Loading…</p>}
      {error && (
        <p className="text-red-600 text-sm">
          Failed to load batch: {(error as Error).message}
          <button type="button" className="ml-2 underline" onClick={() => refetch()}>
            Retry
          </button>
        </p>
      )}

      {submittedIds.length > 0 && !isLoading && (
        <p className="text-sm text-gray-600 mb-3">
          Requested {submittedIds.length} ID(s) · returned {issues.length} issue(s)
        </p>
      )}

      {issues.length > 0 && (
        <div className="border rounded-lg overflow-hidden bg-white">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left p-3">Key</th>
                <th className="text-left p-3">Summary</th>
                <th className="text-left p-3">Status</th>
                <th className="text-left p-3">Priority</th>
              </tr>
            </thead>
            <tbody>
              {issues.map((issue) => (
                <tr key={issue.id} className="border-b last:border-0 hover:bg-gray-50">
                  <td className="p-3">
                    <Link to={`/issues/${issue.id}`} className="text-blue-600 hover:underline">
                      {issue.issueKey || issue.id.slice(0, 8)}
                    </Link>
                  </td>
                  <td className="p-3">{issue.title}</td>
                  <td className="p-3">{issue.status}</td>
                  <td className="p-3">{issue.priority}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
