import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

export default function MigrationServiceHealthPanel() {
  const { data, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['migration', 'health', 'services'],
    queryFn: async () => {
      const res = await migrationApi.getServicesHealth();
      return res.data as {
        overallStatus?: string;
        services?: Array<{ name: string; status: string; baseUrl?: string; error?: string }>;
      };
    },
    refetchInterval: 30000,
  });

  if (isLoading) {
    return <p className="text-sm text-gray-500">Checking downstream services…</p>;
  }

  if (isError) {
    return (
      <div className="text-sm text-red-600">
        Failed to load service health.{' '}
        <button type="button" onClick={() => refetch()} className="underline">
          Retry
        </button>
      </div>
    );
  }

  const services = data?.services || [];
  const overall = data?.overallStatus || 'UNKNOWN';

  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-semibold">Downstream services</h3>
        <div className="flex items-center gap-2">
          <span
            className={`text-xs font-medium px-2 py-0.5 rounded ${
              overall === 'UP'
                ? 'bg-green-100 text-green-800'
                : overall === 'DEGRADED'
                ? 'bg-amber-100 text-amber-800'
                : 'bg-red-100 text-red-800'
            }`}
          >
            {overall}
          </span>
          <button
            type="button"
            onClick={() => refetch()}
            disabled={isFetching}
            className="text-xs text-jira-blue hover:underline"
          >
            Refresh
          </button>
        </div>
      </div>
      <ul className="space-y-2">
        {services.map((s) => (
          <li key={s.name} className="flex items-center justify-between text-sm">
            <span>{s.name}</span>
            <span
              className={
                s.status === 'UP' ? 'text-green-700 font-medium' : 'text-red-600 font-medium'
              }
            >
              {s.status}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
