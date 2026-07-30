import React from 'react';

export interface DcParitySummary {
  entitiesExpected?: number;
  entitiesProcessed?: number;
  entitiesSucceeded?: number;
  entitiesFailed?: number;
  coveragePercent?: number;
  historyReplayed?: number;
  incrementalSkipped?: number;
  attachmentBytesWritten?: number;
  attachmentsCompleted?: number;
  referenceCatalogSize?: number;
  format?: string;
  riskScore?: number;
  historyOnlyImport?: boolean;
  stubDownstream?: boolean;
  parityStatus?: string;
  processedByType?: Record<string, number>;
}

interface Props {
  resultMetadata?: Record<string, unknown> | null;
  validationEntitiesByType?: Record<string, number>;
  jobStatus?: string;
}

function readSummary(meta: Record<string, unknown> | null | undefined): DcParitySummary {
  if (!meta) {
    return {};
  }
  const parity = (meta.paritySummary as DcParitySummary | undefined) ?? meta;
  return {
    entitiesExpected: num(parity.entitiesExpected ?? meta.totalEntities),
    entitiesProcessed: num(parity.entitiesProcessed ?? meta.totalProcessed),
    entitiesSucceeded: num(parity.entitiesSucceeded),
    entitiesFailed: num(parity.entitiesFailed ?? meta.totalFailed),
    coveragePercent: num(parity.coveragePercent),
    historyReplayed: num(parity.historyReplayed ?? meta.historyReplayed),
    incrementalSkipped: num(parity.incrementalSkipped ?? meta.incrementalSkipped),
    attachmentBytesWritten: num(parity.attachmentBytesWritten ?? meta.attachmentBytesWritten),
    attachmentsCompleted: num(parity.attachmentsCompleted ?? meta.attachmentCount),
    referenceCatalogSize: num(parity.referenceCatalogSize ?? meta.referenceCatalogSize),
    format: str(parity.format ?? meta.format),
    riskScore: num(parity.riskScore ?? meta.riskScore),
    historyOnlyImport: bool(parity.historyOnlyImport ?? meta.historyOnlyImport),
    stubDownstream: bool(parity.stubDownstream ?? meta.stubDownstream),
    parityStatus: str(parity.parityStatus),
    processedByType:
      (parity.processedByType as Record<string, number> | undefined) ??
      (meta.processedByType as Record<string, number> | undefined),
  };
}

function num(v: unknown): number | undefined {
  if (v == null) return undefined;
  if (typeof v === 'number') return v;
  const n = Number(v);
  return Number.isFinite(n) ? n : undefined;
}

function str(v: unknown): string | undefined {
  return v != null ? String(v) : undefined;
}

function bool(v: unknown): boolean | undefined {
  if (v == null) return undefined;
  if (typeof v === 'boolean') return v;
  return 'true' === String(v).toLowerCase();
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

export default function DcImportParityReportPanel({
  resultMetadata,
  validationEntitiesByType,
  jobStatus,
}: Props) {
  const s = readSummary(resultMetadata);
  if (!s.entitiesExpected && !s.entitiesProcessed) {
    return null;
  }

  const statusColor =
    s.parityStatus === 'PASS'
      ? 'bg-green-50 border-green-200 text-green-900'
      : s.parityStatus === 'WARN'
        ? 'bg-amber-50 border-amber-200 text-amber-900'
        : 'bg-red-50 border-red-200 text-red-900';

  return (
    <div className="bg-white rounded-lg border p-6 space-y-4" data-testid="dc-import-parity-panel">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">DC import parity report</h3>
        <span className={`text-xs font-semibold px-2 py-1 rounded border ${statusColor}`}>
          {s.parityStatus ?? (jobStatus === 'COMPLETED' ? 'DONE' : jobStatus ?? '—')}
        </span>
      </div>

      <p className="text-sm text-gray-600">
        Compares source entity counts from validation against what the import job processed.
        {s.stubDownstream && (
          <span className="block text-amber-700 mt-1">
            Stub downstream was enabled — issue-service writes may be simulated.
          </span>
        )}
      </p>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <Stat label="Coverage" value={s.coveragePercent != null ? `${s.coveragePercent}%` : '—'} />
        <Stat label="Expected" value={s.entitiesExpected?.toLocaleString() ?? '—'} />
        <Stat label="Succeeded" value={s.entitiesSucceeded?.toLocaleString() ?? '—'} />
        <Stat label="Failed" value={s.entitiesFailed?.toLocaleString() ?? '—'} />
        <Stat label="History replayed" value={s.historyReplayed?.toLocaleString() ?? '—'} />
        <Stat label="Incremental skipped" value={s.incrementalSkipped?.toLocaleString() ?? '0'} />
        <Stat
          label="Attachments"
          value={
            s.attachmentsCompleted != null
              ? `${s.attachmentsCompleted}${s.attachmentBytesWritten ? ` · ${formatBytes(s.attachmentBytesWritten)}` : ''}`
              : '—'
          }
        />
        <Stat label="Format / risk" value={`${s.format ?? '—'} / ${s.riskScore ?? '—'}`} />
      </div>

      {validationEntitiesByType && Object.keys(validationEntitiesByType).length > 0 && (
        <div>
          <p className="text-xs font-semibold text-gray-700 mb-2">Source entities (validate)</p>
          <div className="flex flex-wrap gap-2">
            {Object.entries(validationEntitiesByType).map(([type, count]) => (
              <span key={type} className="text-xs bg-gray-100 border rounded px-2 py-1">
                {type}: {count}
              </span>
            ))}
          </div>
        </div>
      )}

      {s.processedByType && Object.keys(s.processedByType).length > 0 && (
        <div>
          <p className="text-xs font-semibold text-gray-700 mb-2">Processed by type</p>
          <div className="flex flex-wrap gap-2">
            {Object.entries(s.processedByType).map(([type, count]) => (
              <span key={type} className="text-xs bg-blue-50 border border-blue-100 rounded px-2 py-1">
                {type}: {count}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded border bg-gray-50 px-3 py-2">
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-sm font-semibold text-gray-900">{value}</p>
    </div>
  );
}
