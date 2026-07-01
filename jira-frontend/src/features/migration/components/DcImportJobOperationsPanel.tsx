import React, { useCallback, useEffect, useState } from 'react';
import { migrationApi } from '../../../api/serviceApi';

interface Props {
  jobId: string | null;
  relationshipEdges?: Array<{ from: string; to: string; type: string }>;
}

export default function DcImportJobOperationsPanel({ jobId, relationshipEdges = [] }: Props) {
  const [rollbackInfo, setRollbackInfo] = useState<{
    canRollback: boolean;
    canRollbackReason: string;
    entitiesToRollback: number;
  } | null>(null);
  const [auditTrail, setAuditTrail] = useState<Array<Record<string, unknown>>>([]);
  const [busy, setBusy] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadOps = useCallback(async () => {
    if (!jobId) return;
    try {
      const [rb, audit] = await Promise.all([
        migrationApi.getRollbackInfo(jobId),
        migrationApi.getJobAuditTrail(jobId),
      ]);
      setRollbackInfo(rb.data);
      setAuditTrail(audit.data ?? []);
    } catch {
      setRollbackInfo(null);
      setAuditTrail([]);
    }
  }, [jobId]);

  useEffect(() => {
    loadOps();
  }, [loadOps]);

  const handleRollback = async () => {
    if (!jobId) return;
    setBusy('rollback');
    setMessage(null);
    try {
      const res = await migrationApi.rollbackJob(jobId);
      setMessage(
        res.data.success
          ? `Rollback complete: ${res.data.rolledBackCount} entities`
          : `Rollback finished with ${res.data.failedCount} failures`
      );
      await loadOps();
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Rollback failed');
    } finally {
      setBusy(null);
    }
  };

  const handleRetry = async () => {
    if (!jobId) return;
    setBusy('retry');
    setMessage(null);
    try {
      const res = await migrationApi.retryJob(jobId);
      setMessage(`Retry queued: ${res.data.retried} entities, ${res.data.succeeded} succeeded`);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Retry failed');
    } finally {
      setBusy(null);
    }
  };

  const handleDownloadReport = async () => {
    if (!jobId) return;
    setBusy('report');
    try {
      const res = await migrationApi.downloadJobReport(jobId);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = `import-report-${jobId}.csv`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Report download failed');
    } finally {
      setBusy(null);
    }
  };

  const handleDownloadValidation = async () => {
    if (!jobId) return;
    setBusy('validation');
    try {
      const res = await migrationApi.downloadValidationReport(jobId);
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = `validation-report-${jobId}.csv`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      setMessage(e instanceof Error ? e.message : 'Validation report download failed');
    } finally {
      setBusy(null);
    }
  };

  if (!jobId) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg border p-6 space-y-4">
      <h3 className="text-lg font-semibold">DC import operations</h3>

      {rollbackInfo && (
        <p className="text-sm text-gray-600">
          Rollback: {rollbackInfo.canRollback ? 'available' : 'unavailable'} —{' '}
          {rollbackInfo.canRollbackReason} ({rollbackInfo.entitiesToRollback} entities)
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          disabled={!!busy || !rollbackInfo?.canRollback}
          onClick={handleRollback}
          className="px-3 py-2 text-sm bg-red-600 text-white rounded-lg disabled:opacity-50"
        >
          Rollback job
        </button>
        <button
          type="button"
          disabled={!!busy}
          onClick={handleRetry}
          className="px-3 py-2 text-sm bg-amber-600 text-white rounded-lg disabled:opacity-50"
        >
          Retry failed entities
        </button>
        <button
          type="button"
          disabled={!!busy}
          onClick={handleDownloadReport}
          className="px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg"
        >
          Download import report
        </button>
        <button
          type="button"
          disabled={!!busy}
          onClick={handleDownloadValidation}
          className="px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg"
        >
          Download validation CSV
        </button>
      </div>

      {message && <p className="text-sm text-gray-700">{message}</p>}

      {relationshipEdges.length > 0 && (
        <div>
          <p className="text-xs font-medium text-gray-800 mb-1">Imported relationship graph</p>
          <ul className="text-xs font-mono text-gray-600 max-h-32 overflow-y-auto space-y-0.5">
            {relationshipEdges.map((e, i) => (
              <li key={`${e.from}-${e.to}-${i}`}>
                {e.from} —[{e.type}]→ {e.to}
              </li>
            ))}
          </ul>
        </div>
      )}

      {auditTrail.length > 0 && (
        <div>
          <p className="text-xs font-medium text-gray-800 mb-1">Audit trail</p>
          <ul className="text-xs text-gray-600 max-h-40 overflow-y-auto space-y-1">
            {auditTrail.map((entry, i) => (
              <li key={i}>
                {(entry.action as string) ?? '—'} · {(entry.entityType as string) ?? ''}{' '}
                {(entry.entityId as string) ?? ''}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
