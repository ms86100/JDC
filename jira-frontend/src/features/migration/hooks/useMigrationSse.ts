import { useCallback, useEffect, useRef } from 'react';
import type { JobProgress } from '../types/migration';

interface UseMigrationSseOptions {
  onProgress?: (progress: JobProgress) => void;
  onComplete?: () => void;
  onError?: (error: Error) => void;
}

/**
 * Subscribes to migration job progress via Server-Sent Events.
 * Falls back gracefully if the stream is unavailable.
 */
export function useMigrationSse(jobId: string | null, options: UseMigrationSseOptions = {}) {
  const eventSourceRef = useRef<EventSource | null>(null);
  const optionsRef = useRef(options);
  optionsRef.current = options;

  const close = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
  }, []);

  useEffect(() => {
    if (!jobId) {
      return undefined;
    }

    const base = import.meta.env.VITE_API_GATEWAY_URL ?? '';
    const url = `${base}/api/sse/job/${jobId}/stream`;
    const es = new EventSource(url);
    eventSourceRef.current = es;

    es.addEventListener('progress', (event) => {
      try {
        const data = JSON.parse(event.data);
        const progress: JobProgress = {
          jobId: data.jobId || jobId,
          jobStatus: data.status || data.jobStatus || 'IMPORTING',
          progressPercentage: data.progressPercentage ?? data.progress ?? 0,
          totalEntities: data.totalEntities ?? data.total ?? 0,
          processedEntities: data.processedEntities ?? data.processed ?? 0,
          failedEntities: data.failedEntities ?? data.failed ?? 0,
          entityProgress: data.entityProgress || [],
          stages: data.stages,
          currentPhase: data.currentStage || data.currentPhase || data.phase,
          recentLogs: data.logMessage
            ? [{ timestamp: new Date().toISOString(), level: 'INFO', message: data.logMessage }]
            : data.recentLogs,
          currentStep: data.currentStep || data.currentStage || data.phase,
          attachmentBytesWritten: data.attachmentBytesWritten,
          attachmentsCompleted: data.attachmentsCompleted,
          incrementalSkipped: data.incrementalSkipped,
          attachmentChunkIndex: data.attachmentChunkIndex,
          attachmentChunkTotal: data.attachmentChunkTotal,
          attachmentCurrentFile: data.attachmentCurrentFile,
          attachmentChunked: data.attachmentChunked,
        };
        optionsRef.current.onProgress?.(progress);
        if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(progress.jobStatus)) {
          optionsRef.current.onComplete?.();
          close();
        }
      } catch (e) {
        optionsRef.current.onError?.(e instanceof Error ? e : new Error('SSE parse error'));
      }
    });

    es.onerror = () => {
      close();
    };

    return () => close();
  }, [jobId, close]);

  return { close };
}
