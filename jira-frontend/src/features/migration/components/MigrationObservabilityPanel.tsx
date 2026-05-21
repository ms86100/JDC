import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

export default function MigrationObservabilityPanel() {
  const { data } = useQuery({
    queryKey: ['migration', 'observability'],
    queryFn: async () => {
      const res = await migrationApi.getObservability();
      return res.data as {
        healthUrl?: string;
        metricsUrl?: string;
        prometheusUrl?: string;
        notes?: string;
        probes?: Record<string, { status?: string; url?: string }>;
      };
    },
    staleTime: 120000,
  });

  if (!data) return null;

  return (
    <div className="rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-sm">
      <p className="font-medium text-gray-800 mb-2">Observability</p>
      <ul className="space-y-1 text-xs">
        {data.healthUrl && (
          <li>
            <a href={data.healthUrl} target="_blank" rel="noreferrer" className="text-jira-blue underline">
              Actuator health
            </a>
          </li>
        )}
        {data.prometheusUrl && (
          <li>
            <a href={data.prometheusUrl} target="_blank" rel="noreferrer" className="text-jira-blue underline">
              Prometheus metrics
            </a>
          </li>
        )}
        {data.metricsUrl && (
          <li>
            <a href={data.metricsUrl} target="_blank" rel="noreferrer" className="text-jira-blue underline">
              Micrometer metrics
            </a>
          </li>
        )}
      </ul>
      {data.notes && <p className="text-xs text-gray-500 mt-2">{data.notes}</p>}
    </div>
  );
}
