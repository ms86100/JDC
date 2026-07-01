import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';
import ImportedIssuesPanel from './ImportedIssuesPanel';
import ImportedAttachmentsPanel from './ImportedAttachmentsPanel';
import DcStagingInsightsPanel from './DcStagingInsightsPanel';
import MigrationVerificationPanel from './MigrationVerificationPanel';
import MigrationReindexPanel from './MigrationReindexPanel';
import JobPauseResumeControls from './JobPauseResumeControls';
import DcImportJobOperationsPanel from './DcImportJobOperationsPanel';
import DcImportParityReportPanel from './DcImportParityReportPanel';
import DcImportSlaProofPanel from './DcImportSlaProofPanel';
import DcImportAcSignoffPanel from './DcImportAcSignoffPanel';
import ConfigImportSummaryPanel from './ConfigImportSummaryPanel';
import { isJiraDcIssueImport } from '../utils/importTypeHelpers';

interface Props {
  jobId: string;
  onClose: () => void;
  /** When set, show DC enterprise panels (parity, SLA, AC, operations). */
  importType?: string | null;
  resultMetadata?: Record<string, unknown> | null;
}

export default function MigrationJobDetailPanel({ jobId, onClose, importType, resultMetadata }: Props) {
  const isDc = isJiraDcIssueImport(importType ?? undefined);
  const queryClient = useQueryClient();
  const [retrying, setRetrying] = useState<string | null>(null);

  const { data: audit } = useQuery({
    queryKey: ['migration', 'audit', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobAuditTrail(jobId);
      return res.data as Array<{ action?: string; entityType?: string; entityKey?: string; performedAt?: string }>;
    },
  });

  const { data: dlq } = useQuery({
    queryKey: ['migration', 'dlq', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobDlq(jobId);
      return res.data as Array<{ id?: string; operationType?: string; entityType?: string; lastError?: string }>;
    },
  });

  const { data: logs } = useQuery({
    queryKey: ['migration', 'logs', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobLogs(jobId);
      return res.data as Array<{ timestamp?: string; level?: string; message?: string }>;
    },
    refetchInterval: 5000,
  });

  const { data: jobProgress } = useQuery({
    queryKey: ['migration', 'progress', jobId],
    queryFn: async () => {
      const res = await migrationApi.getJobProgress(jobId);
      return res.data as { jobStatus?: string; status?: string };
    },
    refetchInterval: 3000,
  });

  const jobStatus = jobProgress?.jobStatus ?? jobProgress?.status;

  const handleRetryDlq = async (dlqId: string) => {
    setRetrying(dlqId);
    try {
      await migrationApi.retryJobDlqEntry(jobId, dlqId);
      queryClient.invalidateQueries({ queryKey: ['migration', 'dlq', jobId] });
    } finally {
      setRetrying(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between px-6 py-4 border-b sticky top-0 bg-white gap-4">
          <h2 className="text-lg font-semibold">Job details — {jobId}</h2>
          <JobPauseResumeControls
            jobId={jobId}
            jobStatus={jobStatus}
            onUpdated={() => {
              queryClient.invalidateQueries({ queryKey: ['migration', 'progress', jobId] });
              queryClient.invalidateQueries({ queryKey: ['migration', 'logs', jobId] });
            }}
          />
          <button type="button" onClick={onClose} className="text-gray-500 hover:text-gray-800">
            ✕
          </button>
        </div>
        <div className="p-6 space-y-6">
          <ConfigImportSummaryPanel jobId={jobId} />
          <MigrationVerificationPanel jobId={jobId} />
          <MigrationReindexPanel jobId={jobId} />
          <ImportedIssuesPanel jobId={jobId} />
          <ImportedAttachmentsPanel jobId={jobId} />
          <DcStagingInsightsPanel jobId={jobId} />

          {isDc && (
            <>
              <DcImportParityReportPanel resultMetadata={resultMetadata} jobStatus={jobStatus} />
              <DcImportSlaProofPanel
                jobId={jobId}
                embeddedSla={resultMetadata?.slaProof as Record<string, unknown> | undefined}
              />
              <DcImportAcSignoffPanel
                jobId={jobId}
                embeddedSignoff={resultMetadata?.acSignoff as Record<string, unknown> | undefined}
              />
              <DcImportJobOperationsPanel jobId={jobId} />
            </>
          )}

          {(dlq?.length ?? 0) > 0 && (
            <div className="border rounded-lg p-4">
              <h4 className="font-semibold mb-2">Dead letter queue ({dlq!.length})</h4>
              <ul className="space-y-2 text-sm">
                {dlq!.map((entry) => (
                  <li key={entry.id} className="flex justify-between items-start gap-2 border-b pb-2">
                    <span>
                      {entry.operationType} / {entry.entityType}
                      {entry.lastError && (
                        <span className="block text-red-600 text-xs">{entry.lastError}</span>
                      )}
                    </span>
                    {entry.id && (
                      <button
                        type="button"
                        disabled={retrying === entry.id}
                        onClick={() => handleRetryDlq(entry.id!)}
                        className="text-xs px-2 py-1 bg-jira-blue text-white rounded"
                      >
                        {retrying === entry.id ? 'Retrying…' : 'Retry'}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="border rounded-lg p-4">
            <div className="flex justify-between items-center mb-2">
              <h4 className="font-semibold">Live logs</h4>
              <button
                type="button"
                className="text-xs px-2 py-1 border rounded hover:bg-gray-50"
                onClick={async () => {
                  const res = await migrationApi.downloadJobLogs(jobId);
                  const blob = new Blob([res.data], { type: 'text/plain' });
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `migration-logs-${jobId}.txt`;
                  a.click();
                  URL.revokeObjectURL(url);
                }}
              >
                Download logs (.txt)
              </button>
            </div>
            <div className="max-h-40 overflow-y-auto font-mono text-xs bg-gray-900 text-green-400 p-3 rounded">
              {(logs || []).map((l, i) => (
                <div key={i}>
                  [{l.timestamp}] {l.level}: {l.message}
                </div>
              ))}
              {(!logs || logs.length === 0) && <div className="text-gray-500">No logs yet</div>}
            </div>
          </div>

          <div className="border rounded-lg p-4">
            <h4 className="font-semibold mb-2">Audit trail</h4>
            <ul className="text-xs max-h-32 overflow-y-auto">
              {(audit || []).map((a, i) => (
                <li key={i}>
                  {a.performedAt} — {a.action} {a.entityType} {a.entityKey}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
