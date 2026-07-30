import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
}

export default function DcStagingInsightsPanel({ jobId }: Props) {
  const { data, isLoading } = useQuery({
    queryKey: ['migration', 'staging-summary', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobStagingSummary(jobId!);
      return res.data as {
        totalEntries?: number;
        byEntityType?: Record<string, number>;
        byValidationState?: Record<string, number>;
      };
    },
    enabled: !!jobId,
  });

  if (!jobId) return null;
  if (isLoading) return <p className="text-sm text-gray-500">Loading staging summary…</p>;
  if (!data) return null;

  return (
    <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-4">
      <h4 className="font-semibold text-indigo-900 mb-2">DC staging insights</h4>
      <p className="text-sm text-indigo-800 mb-3">
        {data.totalEntries ?? 0} entities staged for import
      </p>
      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <p className="font-medium text-indigo-800 mb-1">By entity type</p>
          <ul>
            {Object.entries(data.byEntityType || {}).map(([k, v]) => (
              <li key={k}>
                {k}: <strong>{v}</strong>
              </li>
            ))}
          </ul>
        </div>
        <div>
          <p className="font-medium text-indigo-800 mb-1">By validation state</p>
          <ul>
            {Object.entries(data.byValidationState || {}).map(([k, v]) => (
              <li key={k}>
                {k}: <strong>{v}</strong>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
