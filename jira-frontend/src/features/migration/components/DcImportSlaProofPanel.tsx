import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
  embeddedSla?: Record<string, unknown> | null;
}

export default function DcImportSlaProofPanel({ jobId, embeddedSla }: Props) {
  const { data, isLoading } = useQuery({
    queryKey: ['migration', 'dc-sla-proof', jobId],
    queryFn: async () => {
      const res = await migrationApi.getDcSlaProof(jobId!);
      return res.data as Record<string, unknown>;
    },
    enabled: !!jobId && !embeddedSla,
  });

  const sla = embeddedSla ?? data;
  if (!jobId && !sla) return null;
  if (isLoading && !sla) {
    return <p className="text-sm text-gray-500">Loading SLA proof…</p>;
  }
  if (!sla) return null;

  const met = sla.slaMet === true;
  const stub = sla.stubDownstream === true;

  return (
    <div className="bg-white rounded-lg border p-6" data-testid="dc-import-sla-panel">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-lg font-semibold">Live import SLA proof</h3>
        <span
          className={`text-xs font-semibold px-2 py-1 rounded border ${
            met ? 'bg-green-50 border-green-200 text-green-800' : 'bg-amber-50 border-amber-200 text-amber-900'
          }`}
        >
          {met ? 'SLA MET' : stub ? 'STUB (not counted)' : 'SLA NOT MET'}
        </span>
      </div>
      <p className="text-sm text-gray-600 mb-4">
        Measures real import job duration vs tier budget (1k = 5 min, 10k = 30 min). Parse-only tests use a
        separate SAX gate.
      </p>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
        <Cell label="Tier" value={String(sla.slaTier ?? '—')} />
        <Cell label="Issues" value={String(sla.issueCount ?? '—')} />
        <Cell label="Duration" value={formatMs(sla.durationMs)} />
        <Cell label="Budget" value={formatMs(sla.maxAllowedMs)} />
        <Cell label="Throughput" value={`${sla.issuesPerSecond ?? '—'} issues/s`} />
        <Cell label="Failed" value={String(sla.failedEntities ?? 0)} />
        <Cell label="Proof type" value={String(sla.proofType ?? '—')} />
        <Cell label="Note" value={String(sla.slaNote ?? '—')} />
      </div>
    </div>
  );
}

function Cell({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded border bg-gray-50 px-3 py-2">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="font-medium text-gray-900 truncate" title={value}>
        {value}
      </p>
    </div>
  );
}

function formatMs(v: unknown): string {
  if (v == null) return '—';
  const n = Number(v);
  if (!Number.isFinite(n)) return String(v);
  if (n < 1000) return `${n} ms`;
  return `${(n / 1000).toFixed(1)} s`;
}
