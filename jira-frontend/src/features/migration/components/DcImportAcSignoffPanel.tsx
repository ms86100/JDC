import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
  embeddedSignoff?: Record<string, unknown> | null;
}

interface Criterion {
  id: string;
  title: string;
  status: string;
  evidence: string;
  signoffReady: boolean;
}

export default function DcImportAcSignoffPanel({ jobId, embeddedSignoff }: Props) {
  const { data, isLoading } = useQuery({
    queryKey: ['migration', 'dc-ac-signoff', jobId],
    queryFn: async () => {
      const res = await migrationApi.getDcAcSignoff(jobId!);
      return res.data as Record<string, unknown>;
    },
    enabled: !!jobId && !embeddedSignoff,
  });

  const signoff = embeddedSignoff ?? data;
  if (!jobId && !signoff) return null;
  if (isLoading && !signoff) {
    return <p className="text-sm text-gray-500">Loading AC sign-off checklist…</p>;
  }
  if (!signoff) return null;

  const criteria = (signoff.criteria as Criterion[] | undefined) ?? [];
  const ready = signoff.signoffReadyCount as number | undefined;
  const formal = signoff.formalSignoffComplete === true;

  return (
    <div className="bg-white rounded-lg border p-6" data-testid="dc-import-ac-signoff-panel">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-lg font-semibold">Enterprise AC sign-off</h3>
        <span
          className={`text-xs font-semibold px-2 py-1 rounded border ${
            formal
              ? 'bg-green-50 border-green-200 text-green-800'
              : 'bg-gray-50 border-gray-200 text-gray-700'
          }`}
        >
          {formal ? '10/10 READY' : `${ready ?? 0}/10 ready`}
        </span>
      </div>
      <p className="text-sm text-gray-600 mb-4">
        Formal production gate (AC-1–AC-10). See{' '}
        <code className="text-xs bg-gray-100 px-1 rounded">docs/issue_xml_ac_signoff_checklist.md</code>.
      </p>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-2 text-left">ID</th>
              <th className="px-3 py-2 text-left">Criterion</th>
              <th className="px-3 py-2 text-left">Status</th>
              <th className="px-3 py-2 text-left">Evidence</th>
            </tr>
          </thead>
          <tbody>
            {criteria.map((c) => (
              <tr key={c.id} className="border-t">
                <td className="px-3 py-2 font-mono text-xs">{c.id}</td>
                <td className="px-3 py-2">{c.title}</td>
                <td className="px-3 py-2">
                  <StatusBadge status={c.status} ready={c.signoffReady} />
                </td>
                <td className="px-3 py-2 text-gray-600 text-xs max-w-md">{c.evidence}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatusBadge({ status, ready }: { status: string; ready: boolean }) {
  const color =
    status === 'PASS'
      ? 'bg-green-100 text-green-800'
      : status === 'PARTIAL'
        ? 'bg-amber-100 text-amber-900'
        : status === 'NOT_RUN'
          ? 'bg-gray-100 text-gray-600'
          : 'bg-red-100 text-red-800';
  return (
    <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium ${color}`}>
      {status}
      {ready ? ' ✓' : ''}
    </span>
  );
}
