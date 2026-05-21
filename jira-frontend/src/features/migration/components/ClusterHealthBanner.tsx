import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

type ClusterHealth = {
  status?: string;
  warnings?: string[];
  activeNodes?: number;
  totalNodes?: number;
};

function statusStyles(status: string | undefined) {
  switch (status) {
    case 'HEALTHY':
      return {
        background: 'var(--sa-status-done-bg)',
        border: '1px solid var(--sa-status-done-fg)',
        color: 'var(--sa-status-done-fg)',
      };
    case 'DEGRADED':
      return {
        background: 'var(--sa-status-inprogress-bg)',
        border: '1px solid var(--sa-n500)',
        color: 'var(--sa-n800)',
      };
    default:
      return {
        background: 'var(--sa-status-blocked-bg)',
        border: '1px solid var(--sa-status-blocked-fg)',
        color: 'var(--sa-status-blocked-fg)',
      };
  }
}

/** Always visible — including HEALTHY — so ops can confirm cluster state at a glance. */
export default function ClusterHealthBanner() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['migration', 'health', 'cluster'],
    queryFn: async () => {
      const res = await migrationApi.getClusterHealth();
      return res.data as ClusterHealth;
    },
    refetchInterval: 60000,
  });

  const status = data?.status ?? (isLoading ? 'CHECKING' : isError ? 'UNKNOWN' : 'HEALTHY');
  const styles = statusStyles(status === 'CHECKING' || status === 'UNKNOWN' ? undefined : status);

  return (
    <div
      data-testid="cluster-health-banner"
      data-status={status}
      className="rounded-lg px-4 py-3 mb-4 text-sm"
      style={{
        ...styles,
        fontFamily: 'var(--sa-font-sans)',
        borderRadius: 'var(--sa-radius-md)',
      }}
    >
      <strong>
        {isLoading ? 'Checking cluster health…' : `Cluster ${status}`}
      </strong>
      {data?.activeNodes != null && data?.totalNodes != null && (
        <span style={{ marginLeft: 8 }}>
          ({data.activeNodes}/{data.totalNodes} nodes active)
        </span>
      )}
      {status === 'HEALTHY' && !isLoading && (
        <span style={{ marginLeft: 8, opacity: 0.85 }}>
          — all migration nodes reachable
        </span>
      )}
      {(data?.warnings?.length ?? 0) > 0 && (
        <ul style={{ marginTop: 8, paddingLeft: 20, fontSize: 'var(--sa-fs-xs)' }}>
          {data!.warnings!.slice(0, 5).map((w, i) => (
            <li key={i}>{w}</li>
          ))}
        </ul>
      )}
      {isError && (
        <p style={{ margin: '4px 0 0', fontSize: 'var(--sa-fs-xs)' }}>
          Could not reach cluster health endpoint. Check migration-service.
        </p>
      )}
    </div>
  );
}
