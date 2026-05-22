import { useCallback, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { migrationWizardApi } from '../../../api/serviceApi';
import type { FieldMapping } from '../types/migration';
import type { ServerValidationPayload } from '../utils/mapWizardValidationResult';

export interface WizardSession {
  sessionId: string;
  step: string;
  importType: string;
  status: string;
  targetProjectId?: string;
  migrationJobId?: string;
  fileName?: string;
  detectedHeaders?: string[];
  detectedEntityType?: string;
  attachmentColumn?: string;
  parentColumn?: string;
  epicColumn?: string;
  totalRows?: number;
  validationResult?: Record<string, unknown>;
  fieldMappings?: FieldMapping[];
  previewRows?: string[][];
  sessionData?: Record<string, unknown>;
}

function mapImportType(type: string): string {
  switch (type) {
    case 'csv':
      return 'CSV';
    case 'issue-xml':
      return 'ISSUE_XML';
    case 'jira-dc':
      return 'JIRA_DC';
    case 'project-import':
      return 'PROJECT';
    default:
      return type.toUpperCase();
  }
}

function feMappingsToApi(mappings: FieldMapping[]): Record<string, unknown>[] {
  return mappings.map((m) => ({
    sourceColumn: m.sourceColumn,
    targetField: m.targetField,
    dataType: m.dataType,
    required: m.required,
    mapped: m.mapped,
    transformer: m.transformer,
  }));
}

function apiMappingsToFe(mappings?: Record<string, unknown>[]): FieldMapping[] {
  if (!mappings) return [];
  return mappings.map((m) => ({
    sourceColumn: String(m.sourceColumn ?? m.sourceField ?? ''),
    targetField: String(m.targetField ?? ''),
    dataType: String(m.dataType ?? 'STRING'),
    required: Boolean(m.required),
    mapped: Boolean(m.mapped),
    transformer: m.transformer as string | undefined,
  }));
}

export function useMigrationWizard() {
  const queryClient = useQueryClient();
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const uploadAbortRef = useRef<AbortController | null>(null);

  const sessionQuery = useQuery({
    queryKey: ['migration-wizard-session', sessionId],
    queryFn: async () => {
      if (!sessionId) return null;
      const res = await migrationWizardApi.getSession(sessionId);
      const data = res.data;
      return {
        sessionId: data.sessionId,
        step: data.step,
        importType: data.importType,
        status: data.status,
        targetProjectId: data.targetProjectId,
        migrationJobId: data.migrationJobId,
        fileName: data.fileName,
        detectedHeaders: data.detectedHeaders,
        detectedEntityType: data.detectedEntityType,
        attachmentColumn: data.attachmentColumn,
        parentColumn: data.parentColumn,
        epicColumn: data.epicColumn,
        totalRows: data.totalRows,
        validationResult: data.validationResult,
        fieldMappings: apiMappingsToFe(data.fieldMappings),
        previewRows: data.previewRows,
        sessionData: data.sessionData,
      } as WizardSession;
    },
    enabled: !!sessionId,
    staleTime: 5_000,
  });

  const createSession = useMutation({
    mutationFn: async (params: { importType: string; targetProjectId?: string }) => {
      const res = await migrationWizardApi.createSession({
        importType: mapImportType(params.importType),
        targetProjectId: params.targetProjectId,
      });
      return res.data;
    },
    onSuccess: (data) => {
      setSessionId(data.sessionId);
      queryClient.setQueryData(['migration-wizard-session', data.sessionId], data);
    },
  });

  const uploadFile = useMutation({
    mutationFn: async ({
      file,
      importType,
      sessionId: sessionIdOverride,
    }: {
      file: File;
      importType?: string;
      /** Pass when session was just created (hook state may not have updated yet). */
      sessionId?: string;
    }) => {
      const sid = sessionIdOverride ?? sessionId;
      if (!sid) throw new Error('Wizard session not created');
      uploadAbortRef.current?.abort();
      const controller = new AbortController();
      uploadAbortRef.current = controller;
      setUploadProgress(0);
      try {
        const res = await migrationWizardApi.uploadFileWithProgress(
          sid,
          file,
          importType,
          (pct) => setUploadProgress(pct),
          controller.signal
        );
        return res.data;
      } finally {
        setUploadProgress(null);
        uploadAbortRef.current = null;
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['migration-wizard-session', sessionId] });
    },
  });

  const cancelUpload = useCallback(() => {
    uploadAbortRef.current?.abort();
    uploadAbortRef.current = null;
    setUploadProgress(null);
  }, []);

  const validateSession = useMutation({
    mutationFn: async (entityType?: string) => {
      if (!sessionId) throw new Error('Wizard session not created');
      const res = await migrationWizardApi.validateSession(sessionId, entityType);
      return res.data as ServerValidationPayload;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['migration-wizard-session', sessionId] });
    },
  });

  const saveFieldMappings = useMutation({
    mutationFn: async (mappings: FieldMapping[]) => {
      if (!sessionId) throw new Error('Wizard session not created');
      const res = await migrationWizardApi.saveFieldMappings(sessionId, feMappingsToApi(mappings));
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['migration-wizard-session', sessionId] });
    },
  });

  const updateSession = useMutation({
    mutationFn: async (update: {
      step?: string;
      targetProjectId?: string;
      importOptions?: Record<string, unknown>;
      userMappings?: Array<Record<string, unknown>>;
    }) => {
      if (!sessionId) throw new Error('Wizard session not created');
      const res = await migrationWizardApi.updateSession(sessionId, update);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['migration-wizard-session', sessionId] });
    },
  });

  const executeImport = useMutation({
    mutationFn: async (params: { targetProjectId?: string; options?: Record<string, unknown> }) => {
      if (!sessionId) throw new Error('Wizard session not created');
      const res = await migrationWizardApi.executeImport(sessionId, params);
      return res.data;
    },
  });

  const resetWizard = useCallback(() => {
    setSessionId(null);
    queryClient.removeQueries({ queryKey: ['migration-wizard-session'] });
  }, [queryClient]);

  const ensureSession = useCallback(
    async (importType: string, targetProjectId?: string) => {
      if (sessionId) return sessionId;
      const created = await createSession.mutateAsync({ importType, targetProjectId });
      // React Query v5 removed mutation onSuccess — set session id before upload/validate.
      setSessionId(created.sessionId);
      queryClient.setQueryData(['migration-wizard-session', created.sessionId], created);
      return created.sessionId;
    },
    [sessionId, createSession, queryClient]
  );

  return {
    sessionId,
    session: sessionQuery.data,
    isLoadingSession: sessionQuery.isLoading,
    uploadProgress,
    createSession,
    uploadFile,
    cancelUpload,
    validateSession,
    saveFieldMappings,
    updateSession,
    executeImport,
    resetWizard,
    ensureSession,
    refetchSession: sessionQuery.refetch,
  };
}
