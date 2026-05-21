import React, { useEffect, useRef, useState } from 'react';
import type { JobProgress, EntityProgress } from '../types/migration';
import JobPauseResumeControls from './JobPauseResumeControls';

interface ImportProgressProps {
  progress: JobProgress;
  isPolling?: boolean;
  onCancel?: () => void;
  onViewLogs?: () => void;
  showLogs?: boolean;
  onOpenJobConsole?: () => void;
}

const STATUS_ICONS: Record<string, string> = {
  PENDING: '⏳',
  VALIDATING: '🔍',
  MAPPING: '🔗',
  IMPORTING: '📥',
  INDEXING: '🔎',
  COMPLETED: '✅',
  FAILED: '❌',
  CANCELLED: '🚫',
};

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-gray-100 text-gray-600',
  VALIDATING: 'bg-blue-100 text-blue-600',
  MAPPING: 'bg-purple-100 text-purple-600',
  IMPORTING: 'bg-blue-100 text-blue-600',
  INDEXING: 'bg-indigo-100 text-indigo-600',
  COMPLETED: 'bg-green-100 text-green-600',
  FAILED: 'bg-red-100 text-red-600',
  CANCELLED: 'bg-gray-100 text-gray-600',
};

export default function ImportProgress({
  progress,
  isPolling = false,
  onCancel,
  onViewLogs,
  showLogs = false,
  onOpenJobConsole,
}: ImportProgressProps) {
  const [elapsedTime, setElapsedTime] = useState<number>(0);
  const [logs, setLogs] = useState<Array<{ timestamp: Date; message: string; type: 'info' | 'error' | 'success' }>>([]);
  const logsEndRef = useRef<HTMLDivElement>(null);

  // Calculate elapsed time
  useEffect(() => {
    if (!progress.startedAt) return;

    const startTime = new Date(progress.startedAt).getTime();
    const interval = setInterval(() => {
      setElapsedTime(Math.floor((Date.now() - startTime) / 1000));
    }, 1000);

    return () => clearInterval(interval);
  }, [progress.startedAt]);

  useEffect(() => {
    if (progress.recentLogs?.length) {
      setLogs(
        progress.recentLogs.map((l) => ({
          timestamp: l.timestamp ? new Date(l.timestamp) : new Date(),
          message: l.message || '',
          type: (l.level === 'ERROR' ? 'error' : l.level === 'WARN' ? 'info' : 'success') as
            | 'info'
            | 'error'
            | 'success',
        }))
      );
    }
  }, [progress.recentLogs]);

  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  const formatBytes = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  // Format elapsed time
  const formatElapsedTime = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
      return `${hours}h ${minutes}m ${secs}s`;
    }
    if (minutes > 0) {
      return `${minutes}m ${secs}s`;
    }
    return `${secs}s`;
  };

  // Calculate ETA
  const calculateETA = (): string | null => {
    if (!progress.startedAt || !progress.totalEntities || progress.processedEntities === 0) {
      return null;
    }

    const startTime = new Date(progress.startedAt).getTime();
    const elapsed = (Date.now() - startTime) / 1000;
    const rate = progress.processedEntities / elapsed;
    const remaining = progress.totalEntities - progress.processedEntities;

    if (rate > 0) {
      const etaSeconds = remaining / rate;
      if (etaSeconds < 60) return 'Less than a minute';
      if (etaSeconds < 3600) return `~${Math.ceil(etaSeconds / 60)} minutes`;
      return `~${Math.ceil(etaSeconds / 3600)} hours`;
    }

    return null;
  };

  const eta = calculateETA();
  const isInProgress = ['PENDING', 'VALIDATING', 'MAPPING', 'IMPORTING', 'INDEXING'].includes(progress.jobStatus);
  const isComplete = ['COMPLETED', 'FAILED', 'CANCELLED'].includes(progress.jobStatus);

  return (
    <div className="space-y-6">
      {/* Status Header */}
      <div className="bg-white rounded-lg border p-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className={`w-12 h-12 rounded-full flex items-center justify-center text-2xl ${
              STATUS_COLORS[progress.jobStatus] || 'bg-gray-100'
            }`}>
              {STATUS_ICONS[progress.jobStatus] || '❓'}
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900">
                {progress.jobStatus === 'IMPORTING' && 'Importing Data...'}
                {progress.jobStatus === 'VALIDATING' && 'Validating Data...'}
                {progress.jobStatus === 'MAPPING' && 'Mapping Fields...'}
                {progress.jobStatus === 'INDEXING' && 'Building Search Index...'}
                {progress.jobStatus === 'PENDING' && 'Job Queued...'}
                {progress.jobStatus === 'COMPLETED' && 'Import Complete'}
                {progress.jobStatus === 'FAILED' && 'Import Failed'}
                {progress.jobStatus === 'CANCELLED' && 'Import Cancelled'}
              </h3>
              <p className="text-sm text-gray-500 mt-1">
                Job ID: {progress.jobId}
                {progress.currentPhase && (
                  <span className="ml-2 text-indigo-600">Phase: {progress.currentPhase}</span>
                )}
                {isPolling && <span className="ml-2 text-blue-500">● Live</span>}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <JobPauseResumeControls jobId={progress.jobId} jobStatus={progress.jobStatus} />
            {onOpenJobConsole && (
              <button
                type="button"
                data-testid="import-progress-job-console"
                onClick={onOpenJobConsole}
                className="px-3 py-2 text-sm bg-indigo-100 text-indigo-800 hover:bg-indigo-200 rounded-md transition-colors"
              >
                Job console
              </button>
            )}
            {onViewLogs && (
              <button
                onClick={onViewLogs}
                className="px-3 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
              >
                {showLogs ? 'Hide Logs' : 'View Logs'}
              </button>
            )}
            {isInProgress && onCancel && (
              <button
                onClick={onCancel}
                className="px-4 py-2 text-sm bg-red-100 text-red-600 hover:bg-red-200 rounded-md transition-colors"
              >
                Cancel Import
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Overall Progress */}
      <div className="bg-white rounded-lg border p-6">
        <div className="flex items-center justify-between mb-4">
          <h4 className="text-sm font-medium text-gray-700">Overall Progress</h4>
          <div className="flex items-center gap-4 text-sm">
            {(progress.attachmentBytesWritten ?? 0) > 0 && (
              <span className="text-gray-600">
                Attachments: {formatBytes(progress.attachmentBytesWritten ?? 0)}
                {progress.attachmentsCompleted != null &&
                  ` (${progress.attachmentsCompleted} files)`}
              </span>
            )}
            {(progress.incrementalSkipped ?? 0) > 0 && (
              <span className="text-gray-600">
                Incremental skipped: {progress.incrementalSkipped}
              </span>
            )}
            {progress.attachmentChunked && progress.attachmentChunkTotal != null && (
              <span className="text-indigo-600">
                Chunk {progress.attachmentChunkIndex ?? 0}/{progress.attachmentChunkTotal}
                {progress.attachmentCurrentFile && (
                  <span className="text-gray-500 ml-1 truncate max-w-[140px] inline-block align-bottom">
                    {progress.attachmentCurrentFile}
                  </span>
                )}
              </span>
            )}
            {progress.processedEntities > 0 && (
              <span className="text-gray-500">
                {progress.processedEntities.toLocaleString()} / {progress.totalEntities.toLocaleString()} entities
              </span>
            )}
            <span className="font-semibold text-jira-blue">{Math.round(progress.progressPercentage)}%</span>
          </div>
        </div>

        <div className="w-full bg-gray-200 rounded-full h-3 mb-4">
          <div
            className={`h-3 rounded-full transition-all ${
              progress.jobStatus === 'FAILED' ? 'bg-red-500' :
              progress.jobStatus === 'CANCELLED' ? 'bg-gray-500' :
              'bg-jira-blue'
            }`}
            style={{ width: `${progress.progressPercentage}%` }}
          />
        </div>

        {progress.stages && Object.keys(progress.stages).length > 0 && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
            {Object.entries(progress.stages).map(([name, stage]) => {
              const pct = stage.total > 0 ? Math.round((stage.completed / stage.total) * 100) : 0;
              return (
                <div key={name} className="rounded-md border border-gray-100 bg-gray-50 px-3 py-2">
                  <p className="text-xs font-medium text-gray-600 uppercase tracking-wide">{name}</p>
                  <p className="text-sm font-semibold text-gray-900 mt-1">
                    {stage.completed} / {stage.total}
                    <span className="text-gray-500 font-normal ml-1">({pct}%)</span>
                  </p>
                </div>
              );
            })}
          </div>
        )}

        <div className="flex items-center justify-between text-sm">
          <div className="flex items-center gap-4">
            {isInProgress && (
              <>
                <span className="text-gray-500">Elapsed: {formatElapsedTime(elapsedTime)}</span>
                {eta && <span className="text-gray-500">ETA: {eta}</span>}
              </>
            )}
            {isComplete && progress.startedAt && (
              <span className="text-gray-500">
                Total time: {formatElapsedTime(elapsedTime)}
              </span>
            )}
          </div>
          <div className="flex items-center gap-4">
            {progress.failedEntities > 0 && (
              <span className="text-red-600">
                {progress.failedEntities.toLocaleString()} failed
              </span>
            )}
            {progress.processedEntities > 0 && progress.totalEntities > progress.processedEntities && (
              <span className="text-gray-500">
                {(progress.progressPercentage).toFixed(1)}% complete
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Entity Breakdown */}
      {progress.entityProgress && progress.entityProgress.length > 0 && (
        <div className="bg-white rounded-lg border p-6">
          <h4 className="text-sm font-medium text-gray-700 mb-4">Progress by Entity Type</h4>
          <div className="space-y-4">
            {progress.entityProgress.map((entity: EntityProgress, index: number) => {
              const entityPercentage = entity.total > 0 ? (entity.completed / entity.total) * 100 : 0;
              const isEntityComplete = entity.completed === entity.total;

              return (
                <div key={index} className="space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-gray-700">
                        {entity.entityType}
                      </span>
                      {isEntityComplete && entity.failed === 0 && (
                        <span className="text-green-500 text-sm">✓</span>
                      )}
                      {entity.failed > 0 && (
                        <span className="text-red-500 text-sm">⚠ {entity.failed}</span>
                      )}
                    </div>
                    <span className="text-sm text-gray-500">
                      {entity.completed.toLocaleString()} / {entity.total.toLocaleString()}
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full transition-all ${
                        entity.failed > 0 ? 'bg-yellow-500' :
                        isEntityComplete ? 'bg-green-500' :
                        'bg-jira-blue'
                      }`}
                      style={{ width: `${entityPercentage}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Live Logs */}
      {showLogs && (
        <div className="bg-gray-900 rounded-lg border overflow-hidden">
          <div className="px-4 py-2 bg-gray-800 border-b border-gray-700 flex items-center justify-between">
            <span className="text-sm font-medium text-gray-300">Live Logs</span>
            <span className="text-xs text-gray-500">{logs.length} entries</span>
          </div>
          <div className="p-4 max-h-64 overflow-y-auto font-mono text-sm">
            {logs.length === 0 ? (
              <div className="text-gray-500 text-center py-4">
                No log entries yet...
              </div>
            ) : (
              logs.map((log, index) => (
                <div key={index} className={`flex gap-2 ${
                  log.type === 'error' ? 'text-red-400' :
                  log.type === 'success' ? 'text-green-400' :
                  'text-gray-300'
                }`}>
                  <span className="text-gray-500">
                    {log.timestamp.toLocaleTimeString()}
                  </span>
                  <span>{log.message}</span>
                </div>
              ))
            )}
            <div ref={logsEndRef} />
          </div>
        </div>
      )}

      {/* Completion Status */}
      {isComplete && (
        <div className={`rounded-lg border p-6 ${
          progress.jobStatus === 'COMPLETED' ? 'bg-green-50 border-green-200' :
          progress.jobStatus === 'FAILED' ? 'bg-red-50 border-red-200' :
          'bg-gray-50 border-gray-200'
        }`}>
          <div className="flex items-center gap-3">
            <span className="text-2xl">
              {progress.jobStatus === 'COMPLETED' ? '🎉' :
               progress.jobStatus === 'FAILED' ? '😞' : '🚫'}
            </span>
            <div>
              <p className={`font-medium ${
                progress.jobStatus === 'COMPLETED' ? 'text-green-800' :
                progress.jobStatus === 'FAILED' ? 'text-red-800' :
                'text-gray-800'
              }`}>
                {progress.jobStatus === 'COMPLETED' && 'Import completed successfully!'}
                {progress.jobStatus === 'FAILED' && 'Import failed with errors'}
                {progress.jobStatus === 'CANCELLED' && 'Import was cancelled'}
              </p>
              <p className={`text-sm mt-1 ${
                progress.jobStatus === 'COMPLETED' ? 'text-green-600' :
                progress.jobStatus === 'FAILED' ? 'text-red-600' :
                'text-gray-600'
              }`}>
                {progress.processedEntities.toLocaleString()} entities processed
                {progress.failedEntities > 0 && `, ${progress.failedEntities.toLocaleString()} failed`}
                {(progress.incrementalSkipped ?? 0) > 0 &&
                  `, ${progress.incrementalSkipped} skipped (incremental delta)`}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
