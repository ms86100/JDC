import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string;
}

export default function ConfigImportSummaryPanel({ jobId }: Props) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['migration-config-import-summary', jobId],
    queryFn: () => migrationApi.getConfigImportSummary(jobId).then((r) => r.data),
  });

  if (isLoading) return <p className="text-sm text-gray-500">Loading import summary…</p>;
  if (isError || !data) return <p className="text-sm text-gray-500">Summary not available for this job.</p>;

  const reindex = data.reindex as Record<string, unknown> | undefined;
  const verification = data.verification as Record<string, unknown> | undefined;

  return (
    <div className="bg-gray-50 border rounded-lg p-4 space-y-3" data-testid="config-import-summary">
      <h3 className="font-semibold text-gray-900">Config import summary</h3>
      <div className="grid grid-cols-2 gap-3 text-sm">
        <div>
          <span className="text-gray-500">Issue results</span>
          <p className="font-medium">{String(data.issueResults ?? 0)}</p>
        </div>
        <div>
          <span className="text-gray-500">Attachment results</span>
          <p className="font-medium">{String(data.attachmentResults ?? 0)}</p>
        </div>
      </div>
      {reindex && (
        <details className="text-sm">
          <summary className="cursor-pointer text-gray-700 font-medium">Reindex</summary>
          <pre className="mt-2 text-xs bg-white border rounded p-2 overflow-auto max-h-32">
            {JSON.stringify(reindex, null, 2)}
          </pre>
        </details>
      )}
      {verification && (
        <details className="text-sm">
          <summary className="cursor-pointer text-gray-700 font-medium">Verification</summary>
          <pre className="mt-2 text-xs bg-white border rounded p-2 overflow-auto max-h-32">
            {JSON.stringify(verification, null, 2)}
          </pre>
        </details>
      )}
    </div>
  );
}
