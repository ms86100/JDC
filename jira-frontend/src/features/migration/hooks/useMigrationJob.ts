import { useCallback, useRef, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';
import type {
  MigrationJob,
  JobProgress,
  ImportResult,
  CsvTemplate,
  ImportOptions,
} from '../types/migration';

const POLL_INTERVAL = 2000;
const MAX_POLL_INTERVAL = 10000;

interface UseMigrationJobOptions {
  onJobComplete?: (result: ImportResult) => void;
  onJobError?: (error: Error) => void;
  onProgressUpdate?: (progress: JobProgress) => void;
}

export function useMigrationJob(options: UseMigrationJobOptions = {}) {
  const queryClient = useQueryClient();
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const currentJobIdRef = useRef<string | null>(null);

  // Fetch templates
  const {
    data: templates,
    isLoading: isLoadingTemplates,
    error: templatesError,
    refetch: refetchTemplates,
  } = useQuery<CsvTemplate[]>({
    queryKey: ['migration-templates'],
    queryFn: async () => {
      const response = await migrationApi.getTemplates();
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  // Fetch job status
  const getJobStatus = useCallback(
    async (jobId: string): Promise<MigrationJob> => {
      const response = await migrationApi.getJobStatus(jobId);
      return response.data as unknown as MigrationJob;
    },
    []
  );

  // Get job progress with entity breakdown
  const getJobProgress = useCallback(async (jobId: string): Promise<JobProgress> => {
    const response = await migrationApi.getJobProgress(jobId);
    return response.data as unknown as JobProgress;
  }, []);

  // Query for single job status
  const useJobQuery = (jobId: string | null) => {
    return useQuery({
      queryKey: ['migration-job', jobId],
      queryFn: () => getJobStatus(jobId!),
      enabled: !!jobId,
      refetchInterval: (query) => {
        const status = query.state.data?.jobStatus;
        // Stop polling if job is in terminal state
        if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(status)) {
          return false;
        }
        return POLL_INTERVAL;
      },
      retry: 2,
    });
  };

  // CSV Import mutation
  const csvImportMutation = useMutation({
    mutationFn: async ({
      file,
      options,
      fieldMappings,
    }: {
      file: File;
      options?: ImportOptions;
      fieldMappings?: unknown[];
    }): Promise<MigrationJob> => {
      const response = await migrationApi.startCsvImport(
        file,
        options?.targetProjectId,
        fieldMappings,
        { importMode: options?.importMode, blockOnValidationErrors: true }
      );
      return response.data as unknown as MigrationJob;
    },
    onSuccess: (job) => {
      currentJobIdRef.current = job.id;
      queryClient.setQueryData(['migration-job', job.id], job);
    },
    onError: (error: Error) => {
      options.onJobError?.(error);
    },
  });

  // Systems and Avionics Import mutation
  const jiraDcImportMutation = useMutation({
    mutationFn: async (params: {
      file: File;
      targetProjectId?: string;
      attachmentBundle?: File | null;
      backupZip?: boolean;
      options?: Record<string, unknown>;
    }): Promise<MigrationJob> => {
      const response = await migrationApi.startJiraDcImport(params);
      return response.data as unknown as MigrationJob;
    },
    onSuccess: (job) => {
      currentJobIdRef.current = job.id;
      queryClient.setQueryData(['migration-job', job.id], job);
    },
    onError: (error: Error) => {
      options.onJobError?.(error);
    },
  });

  // Project Import mutation
  const projectImportMutation = useMutation({
    mutationFn: async ({
      sourceProjectId,
      targetProjectId,
    }: {
      sourceProjectId: string;
      targetProjectId: string;
    }): Promise<MigrationJob> => {
      const response = await migrationApi.startProjectImport(sourceProjectId, targetProjectId);
      return response.data as unknown as MigrationJob;
    },
    onSuccess: (job) => {
      currentJobIdRef.current = job.id;
      queryClient.setQueryData(['migration-job', job.id], job);
    },
    onError: (error: Error) => {
      options.onJobError?.(error);
    },
  });

  // Project Export mutation
  const projectExportMutation = useMutation({
    mutationFn: async ({
      projectId,
      format,
    }: {
      projectId: string;
      format?: string;
    }): Promise<MigrationJob> => {
      const response = await migrationApi.startProjectExport(projectId, format);
      return response.data as unknown as MigrationJob;
    },
    onSuccess: (job) => {
      currentJobIdRef.current = job.id;
      queryClient.setQueryData(['migration-job', job.id], job);
    },
    onError: (error: Error) => {
      options.onJobError?.(error);
    },
  });

  // Cancel job mutation
  const cancelJobMutation = useMutation({
    mutationFn: async (jobId: string): Promise<void> => {
      await migrationApi.cancelJob(jobId);
    },
    onSuccess: () => {
      if (currentJobIdRef.current) {
        queryClient.setQueryData(
          ['migration-job', currentJobIdRef.current],
          (old: MigrationJob | undefined) =>
            old ? { ...old, jobStatus: 'CANCELLED' as const } : old
        );
      }
    },
    onError: (error: Error) => {
      console.error('Failed to cancel job:', error.message);
    },
  });

  // Get import result
  const getImportResult = useCallback(async (jobId: string): Promise<ImportResult> => {
    const response = await migrationApi.getImportResult(jobId);
    return response.data as unknown as ImportResult;
  }, []);

  // Poll for job completion
  const pollJobProgress = useCallback(
    (jobId: string, onProgress?: (progress: JobProgress) => void): Promise<ImportResult> => {
      return new Promise((resolve, reject) => {
        let pollCount = 0;
        let kickAttempted = false;

        const poll = async () => {
          try {
            const progress = await getJobProgress(jobId);
            pollCount++;

            // Stuck PENDING = import worker never started (schedule after-commit fix handles new jobs)
            if (progress.jobStatus === 'PENDING' && !kickAttempted && pollCount >= 2) {
              kickAttempted = true;
              try {
                await migrationApi.kickStalledJob(jobId);
              } catch (e) {
                console.warn('Failed to kick stalled migration job:', e);
              }
            }

            // Update progress callback
            onProgress?.(progress);
            options.onProgressUpdate?.(progress);

            // Check if job is complete
            if (['COMPLETED', 'FAILED', 'CANCELLED'].includes(progress.jobStatus)) {
              const result = await getImportResult(jobId);
              options.onJobComplete?.(result);
              resolve(result);
              return;
            }

            // Schedule next poll with exponential backoff
            const nextInterval = Math.min(POLL_INTERVAL * Math.pow(1.2, Math.floor(pollCount / 5)), MAX_POLL_INTERVAL);
            pollIntervalRef.current = setTimeout(poll, nextInterval);
          } catch (error) {
            reject(error);
          }
        };

        poll();
      });
    },
    [getJobProgress, getImportResult, options]
  );

  // Stop polling
  const stopPolling = useCallback(() => {
    if (pollIntervalRef.current) {
      clearTimeout(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopPolling();
    };
  }, [stopPolling]);

  // Download template
  const downloadTemplate = useCallback(async (templateId: string): Promise<void> => {
    try {
      const response = await migrationApi.downloadTemplate(templateId);
      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `template_${templateId}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error downloading template:', error);
      throw error;
    }
  }, []);

  // Start import based on type
  const startImport = useCallback(
    async (
      importType: 'csv' | 'jira-dc' | 'project',
      params: {
        file?: File;
        sourceProjectId?: string;
        targetProjectId?: string;
        format?: string;
        fieldMappings?: unknown[];
        attachmentBundle?: File | null;
        backupZip?: boolean;
        options?: Record<string, unknown>;
      }
    ): Promise<MigrationJob> => {
      let job: MigrationJob;

      switch (importType) {
        case 'csv':
          if (!params.file) throw new Error('File is required for CSV import');
          job = await csvImportMutation.mutateAsync({
            file: params.file,
            options: { targetProjectId: params.targetProjectId, importMode: 'CREATE_UPDATE' },
            fieldMappings: params.fieldMappings,
          });
          break;
        case 'jira-dc':
          if (!params.file) throw new Error('File is required for Systems and Avionics import');
          job = await jiraDcImportMutation.mutateAsync({
            file: params.file,
            targetProjectId: params.targetProjectId,
            attachmentBundle: params.attachmentBundle,
            backupZip: params.backupZip,
            options: params.options,
          });
          break;
        case 'project':
          if (!params.sourceProjectId || !params.targetProjectId) {
            throw new Error('Source and target project IDs are required');
          }
          job = await projectImportMutation.mutateAsync({
            sourceProjectId: params.sourceProjectId,
            targetProjectId: params.targetProjectId,
          });
          break;
        default:
          throw new Error(`Unknown import type: ${importType}`);
      }

      currentJobIdRef.current = job.id;
      return job;
    },
    [csvImportMutation, jiraDcImportMutation, projectImportMutation]
  );

  // Start export
  const startExport = useCallback(
    async (projectId: string, format = 'xml'): Promise<MigrationJob> => {
      const job = await projectExportMutation.mutateAsync({ projectId, format });
      currentJobIdRef.current = job.id;
      return job;
    },
    [projectExportMutation]
  );

  return {
    // Data
    templates,
    isLoadingTemplates,
    templatesError,
    refetchTemplates,

    // Mutations
    csvImport: csvImportMutation,
    jiraDcImport: jiraDcImportMutation,
    projectImport: projectImportMutation,
    projectExport: projectExportMutation,
    cancelJob: cancelJobMutation,

    // Functions
    startImport,
    startExport,
    getJobStatus,
    getJobProgress,
    getImportResult,
    pollJobProgress,
    stopPolling,
    downloadTemplate,

    // Query hook
    useJobQuery,

    // Current job ID
    currentJobId: currentJobIdRef.current,
  };
}

export type UseMigrationJobReturn = ReturnType<typeof useMigrationJob>;
