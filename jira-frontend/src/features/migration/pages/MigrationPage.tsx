import React, { useState, useCallback, useRef, useEffect, useLayoutEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMigrationJob } from '../hooks/useMigrationJob';
import { useMigrationSse } from '../hooks/useMigrationSse';
import { useMigrationWizard } from '../hooks/useMigrationWizard';
import { useTargetFields } from '../hooks/useTargetFields';
import {
  migrationApi,
  migrationWizardApi,
  type JiraDcValidateResponse,
} from '../../../api/serviceApi';
import { migrationMappingApi } from '../../../api/fieldApi';
import ConfigureImportPanel from '../components/ConfigureImportPanel';
import { useValidation } from '../hooks/useValidation';
import MigrationFileUploader from '../components/MigrationFileUploader';
import ValidationResults from '../components/ValidationResults';
import FieldMappingPanel from '../components/FieldMappingPanel';
import ImportProgress from '../components/ImportProgress';
import JobHistoryTable from '../components/JobHistoryTable';
import ImportTypeSelector, { ImportType } from '../components/ImportTypeSelector';
import { DcImportInsightsPanel } from '../components/DcImportInsightsPanel';
import DcImportOptionsPanel, { DcImportOptions } from '../components/DcImportOptionsPanel';
import DcImportValidationPanel from '../components/DcImportValidationPanel';
import DcImportJobOperationsPanel from '../components/DcImportJobOperationsPanel';
import DcImportParityReportPanel from '../components/DcImportParityReportPanel';
import DcImportSlaProofPanel from '../components/DcImportSlaProofPanel';
import DcImportAcSignoffPanel from '../components/DcImportAcSignoffPanel';
import DcImportConflictPanel from '../components/DcImportConflictPanel';
import DcImportUnknownFieldsPanel from '../components/DcImportUnknownFieldsPanel';
import DcImportReviewPanel from '../components/DcImportReviewPanel';
import DcRelationshipGraphPanel from '../components/DcRelationshipGraphPanel';
import MigrationServiceHealthPanel from '../components/MigrationServiceHealthPanel';
import WorkflowXmlImportPanel, {
  WorkflowXmlImportOutcome,
  WorkflowXmlValidationPayload,
} from '../components/WorkflowXmlImportPanel';
import MigrationJobDetailPanel from '../components/MigrationJobDetailPanel';
import MigrationRoleSelector from '../components/MigrationRoleSelector';
import ImportedIssuesPanel from '../components/ImportedIssuesPanel';
import DcStagingInsightsPanel from '../components/DcStagingInsightsPanel';
import ProjectImportPanel from '../components/ProjectImportPanel';
import ProjectExportPanel, { type ExportFormat } from '../components/ProjectExportPanel';
import ClusterHealthBanner from '../components/ClusterHealthBanner';
import MigrationCapabilityIndex from '../components/MigrationCapabilityIndex';
import MigrationWizardStepper from '../components/MigrationWizardStepper';
import MigrationVerificationPanel from '../components/MigrationVerificationPanel';
import MigrationReindexPanel from '../components/MigrationReindexPanel';
import ImportedAttachmentsPanel from '../components/ImportedAttachmentsPanel';
import MigrationCenterNav, { type MigrationCenterView } from '../components/MigrationCenterNav';
import MigrationFeatureCatalog from '../components/MigrationFeatureCatalog';
import MigrationPlatformHealthView from '../components/MigrationPlatformHealthView';
import { PageHeader } from '../../../components/ui/PageHeader';
import type {
  MigrationState,
  ValidationResult,
  FieldMapping,
  JobProgress,
  ImportResult,
} from '../types/migration';
import { projectApi } from '../../../api/projectApi';
import {
  type MigrationStep,
  STEP_ORDER,
  JIRA_DC_STEP_ORDER,
  WORKFLOW_XML_STEP_ORDER,
  PROJECT_IMPORT_STEP_ORDER,
  PROJECT_EXPORT_STEP_ORDER,
} from '../constants/wizardSteps';
import {
  buildConflictResolutionsPayload,
  conflictRowId,
  type DcConflictResolution,
} from '../types/dcConflictResolution';
import { mapMigrationImportSource } from '../utils/mapMigrationImportSource';
import '../styles/migration-tokens.css';

export default function MigrationPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const importTypeRef = useRef<ImportType | null>(null);

  // Migration state
  const [state, setState] = useState<MigrationState>({
    step: 'source',
    importType: null,
    selectedFile: null,
    validationResult: null,
    fieldMappings: [],
    importOptions: {
      importMode: 'CREATE_UPDATE',
    },
    currentJobId: null,
    jobProgress: null,
    importResult: null,
  });

  useLayoutEffect(() => {
    importTypeRef.current = state.importType;
  }, [state.importType]);

  // Additional state
  const [targetProjectId, setTargetProjectId] = useState<string>('');
  const [sourceProjectId, setSourceProjectId] = useState<string>('');
  const [centerView, setCenterView] = useState<MigrationCenterView>('wizard');
  const [showLogs, setShowLogs] = useState(false);
  const [wizardError, setWizardError] = useState<string | null>(null);
  const [typeWarnings, setTypeWarnings] = useState<string[]>([]);
  const [fieldDefaults, setFieldDefaults] = useState<Record<string, string>>({});
  const [workflowStatusMappings, setWorkflowStatusMappings] = useState<Record<string, string>>({});
  const [schemeFile, setSchemeFile] = useState<File | null>(null);
  const [workflowXmlValidation, setWorkflowXmlValidation] =
    useState<WorkflowXmlValidationPayload | null>(null);
  const [workflowXmlOutcome, setWorkflowXmlOutcome] = useState<WorkflowXmlImportOutcome | null>(null);
  const [workflowXmlStub, setWorkflowXmlStub] = useState(true);
  const [workflowXmlMakeDefault, setWorkflowXmlMakeDefault] = useState(false);
  const [detailJobId, setDetailJobId] = useState<string | null>(null);
  const [dcImportOptions, setDcImportOptions] = useState<DcImportOptions>({
    dryRun: false,
    resume: false,
    parallelWorkers: 4,
    attachmentBundlePath: '',
    blockOnValidationErrors: true,
    backupZip: false,
    incrementalDelta: false,
    historyOnlyImport: false,
    historyReplayOnly: false,
    stubDownstream: false,
  });
  const [dcConflictResolutions, setDcConflictResolutions] = useState<
    Record<string, DcConflictResolution>
  >({});
  const [dcWarningsAcknowledged, setDcWarningsAcknowledged] = useState(false);
  const [dcAttachmentBundleFile, setDcAttachmentBundleFile] = useState<File | null>(null);
  const [dcValidationResult, setDcValidationResult] = useState<JiraDcValidateResponse | null>(null);
  const [dcRelationshipEdges, setDcRelationshipEdges] = useState<
    Array<{ from: string; to: string; type: string }>
  >([]);
  const [virusScanStatus, setVirusScanStatus] = useState<string | null>(null);
  const [exportFormat, setExportFormat] = useState<ExportFormat>('xml');
  const [detailJobImportType, setDetailJobImportType] = useState<ImportType | null>(null);
  const [detailJobResultMetadata, setDetailJobResultMetadata] = useState<Record<
    string,
    unknown
  > | null>(null);

  const wizard = useMigrationWizard();

  const refreshVirusScanStatus = useCallback(async (uploadId?: string, initial?: string | null) => {
    if (initial) {
      setVirusScanStatus(initial);
    }
    if (!uploadId) {
      return;
    }
    try {
      const res = await migrationApi.scanUpload(uploadId);
      setVirusScanStatus(res.data.virusScanStatus ?? null);
    } catch {
      // scan endpoint optional when upload still processing
    }
  }, []);

  const activeStepOrder =
    state.importType === 'workflow-xml'
      ? WORKFLOW_XML_STEP_ORDER
      : state.importType === 'jira-dc'
      ? JIRA_DC_STEP_ORDER
      : state.importType === 'project-import'
      ? PROJECT_IMPORT_STEP_ORDER
      : state.importType === 'project-export'
      ? PROJECT_EXPORT_STEP_ORDER
      : STEP_ORDER;

  const needsTargetFields =
    state.step === 'map' ||
    state.step === 'validate' ||
    state.step === 'review' ||
    !!state.validationResult?.headers?.length;
  const { targetFields, isLoading: loadingTargetFields, autoMapFromHeaders } = useTargetFields(needsTargetFields);

  // Hooks
  const {
    templates,
    isLoadingTemplates,
    startImport,
    startExport,
    pollJobProgress,
    stopPolling,
    downloadTemplate,
    cancelJob,
    useJobQuery,
    getImportResult,
    getJobProgress,
  } = useMigrationJob({
    onJobComplete: (result: ImportResult) => {
      setState((prev) => ({
        ...prev,
        importResult: result,
        jobProgress: prev.jobProgress
          ? { ...prev.jobProgress, jobStatus: result.jobStatus as JobProgress['jobStatus'], progressPercentage: 100 }
          : null,
      }));
    },
    onProgressUpdate: (progress: JobProgress) => {
      setState((prev) => ({ ...prev, jobProgress: progress }));
    },
  });

  // Projects query for target selection
  const { data: projects = [] } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      return await projectApi.getAll();
    },
    enabled:
      state.step === 'targetProject' ||
      state.step === 'review' ||
      state.step === 'source' ||
      state.importType === 'csv' ||
      state.importType === 'jira-dc' ||
      state.importType === 'workflow-xml' ||
      state.importType === 'project-import' ||
      state.importType === 'project-export',
  });

  // Validation hook
  const {
    validateCsvClientSide,
    isValidating,
    validationResult,
    parseError,
    generateFieldMappings,
    exportErrorsToCsv,
    resetValidation,
  } = useValidation({
    onValidationComplete: (result: ValidationResult) => {
      const mappings = generateFieldMappings(result.headers, targetFields);
      setState((prev) => ({
        ...prev,
        validationResult: result,
        fieldMappings: mappings,
      }));
    },
  });

  // Job query for current job
  const { data: currentJob } = useJobQuery(state.currentJobId);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopPolling();
    };
  }, [stopPolling]);

  // Navigation helpers
  const getStepIndex = (step: MigrationStep): number => activeStepOrder.indexOf(step);

  const canGoBack = (): boolean => {
    const currentIndex = getStepIndex(state.step);
    return currentIndex > 0;
  };

  const handleDcConflictResolution = (conflictId: string, resolution: DcConflictResolution) => {
    setDcConflictResolutions((prev) => ({ ...prev, [conflictId]: resolution }));
  };

  const dcValidationReady = (): boolean => {
    if (!dcValidationResult) return false;
    const blockerConflicts =
      dcValidationResult.conflicts?.filter((c) => c.severity === 'BLOCKER') ?? [];
    const unresolvedBlockers = blockerConflicts.some((c, i) => {
      const id = conflictRowId(c, i);
      const action = dcConflictResolutions[id]?.action ?? 'SKIP_ENTITY';
      return action !== 'SKIP_ENTITY' && action !== 'USE_DEFAULT' && action !== 'OVERRIDE_VALUE';
    });
    const hasBlockers =
      (dcValidationResult.blockerCount ?? 0) > 0 || blockerConflicts.length > 0;
    if (hasBlockers && unresolvedBlockers && dcImportOptions.blockOnValidationErrors) {
      return false;
    }
    const warnings =
      (dcValidationResult.warningCount ?? 0) > 0 ||
      (dcValidationResult.conflicts?.some((c) => c.severity !== 'BLOCKER') ?? false);
    if (warnings && !dcWarningsAcknowledged) return false;
    return dcValidationResult.valid !== false;
  };

  const canGoNext = (): boolean => {
    switch (state.step) {
      case 'source':
        if (state.importType === 'project-import' || state.importType === 'project-export') {
          return state.importType !== null;
        }
        return state.importType !== null && state.selectedFile !== null;
      case 'targetProject':
        if (state.importType === 'workflow-xml') return true;
        if (state.importType === 'project-import') {
          return !!sourceProjectId && !!targetProjectId && sourceProjectId !== targetProjectId;
        }
        if (state.importType === 'project-export') {
          return !!targetProjectId;
        }
        return !!targetProjectId;
      case 'map':
        if (state.importType === 'jira-dc') {
          return !!dcValidationResult || !!state.selectedFile;
        }
        return state.fieldMappings.some((m) => m.mapped);
      case 'validate':
        if (state.importType === 'workflow-xml') {
          return (
            workflowXmlValidation !== null &&
            workflowXmlValidation.valid !== false &&
            (workflowXmlValidation.errors?.length ?? 0) === 0
          );
        }
        if (state.importType === 'jira-dc') {
          return dcValidationReady();
        }
        return (
          state.validationResult !== null &&
          (state.validationResult.errors?.length ?? 0) === 0
        );
      case 'configure':
        if (state.importType === 'jira-dc') {
          return dcValidationReady();
        }
        return !!state.importOptions.importMode;
      case 'review':
        if (state.importType === 'workflow-xml') return !!state.selectedFile;
        if (state.importType === 'project-import') {
          return !!sourceProjectId && !!targetProjectId;
        }
        if (state.importType === 'jira-dc') {
          return !!targetProjectId && dcValidationReady();
        }
        if (state.importType === 'project-export') {
          return !!targetProjectId;
        }
        return !!targetProjectId;
      default:
        return false;
    }
  };

  const goToStep = (step: MigrationStep) => {
    setState((prev) => ({ ...prev, step }));
  };

  const goBack = () => {
    const currentIndex = getStepIndex(state.step);
    if (currentIndex > 0) {
      goToStep(activeStepOrder[currentIndex - 1]);
    }
  };

  const goNext = async () => {
    setWizardError(null);
    const currentIndex = getStepIndex(state.step);

    try {
      const usesWizardSession =
        state.importType === 'csv' ||
        state.importType === 'jira-dc' ||
        state.importType === 'workflow-xml';

      if (state.step === 'source' && state.importType && usesWizardSession) {
        await wizard.ensureSession(state.importType, targetProjectId || undefined);
      }
      if (state.step === 'targetProject' && targetProjectId && wizard.sessionId && usesWizardSession) {
        await wizard.updateSession.mutateAsync({
          step: 'TARGET_PROJECT',
          targetProjectId,
        });
      }
      if (state.step === 'map' && state.fieldMappings.length > 0 && wizard.sessionId) {
        await wizard.saveFieldMappings.mutateAsync(state.fieldMappings);
      }
      if (state.step === 'validate' && state.importType === 'csv') {
        const entityType = wizard.session?.detectedEntityType || 'ISSUE';
        const serverResult = await wizard.validateSession.mutateAsync(entityType);
        if (serverResult) {
          setState((prev) => ({ ...prev, validationResult: serverResult }));
        }
        if (serverResult && (serverResult.errors?.length ?? 0) > 0) {
          setWizardError('Fix validation errors before continuing');
          return;
        }
      }
      if (state.step === 'validate' && state.importType === 'jira-dc' && !dcValidationReady()) {
        setWizardError('Complete DC validation and resolve conflicts before continuing');
        return;
      }
      if (state.step === 'configure' && wizard.sessionId) {
        await wizard.updateSession.mutateAsync({
          step: 'CONFIGURE',
          importOptions: {
            importMode: state.importOptions.importMode,
            fieldDefaults,
            workflowStatusMappings: { status: workflowStatusMappings },
          },
        });
        await migrationMappingApi.saveSessionFieldDefaults(wizard.sessionId, fieldDefaults);
        await migrationMappingApi.saveSessionWorkflowMappings(wizard.sessionId, {
          status: workflowStatusMappings,
        });
      }
      if (state.step === 'review' && targetProjectId && wizard.sessionId && usesWizardSession) {
        await wizard.updateSession.mutateAsync({
          step: 'REVIEW',
          targetProjectId,
          importOptions: { importMode: state.importOptions.importMode },
        });
      }
    } catch (e) {
      setWizardError(e instanceof Error ? e.message : 'Wizard step failed');
      return;
    }

    if (currentIndex < activeStepOrder.length - 1) {
      goToStep(activeStepOrder[currentIndex + 1]);
    }
  };

  // Handlers
  const handleTypeSelect = (type: ImportType) => {
    importTypeRef.current = type;
    setState((prev) => ({ ...prev, importType: type }));
  };

  const handleFileSelect = async (file: File) => {
    const importType = importTypeRef.current;
    setState((prev) => ({ ...prev, selectedFile: file }));
    setWizardError(null);

    if (!importType || importType === 'project-export') return;

    try {
      await wizard.ensureSession(importType, targetProjectId || undefined);

      if (importType === 'workflow-xml') {
        return;
      }

      if (importType === 'jira-dc') {
        const isZip = file.name.toLowerCase().endsWith('.zip');
        try {
          const validateRes = await migrationApi.validateJiraDcImport({
            file,
            attachmentBundle: dcAttachmentBundleFile,
            backupZip: dcImportOptions.backupZip || isZip,
            options: {
              dryRun: dcImportOptions.dryRun,
              blockOnValidationErrors: dcImportOptions.blockOnValidationErrors,
            },
          });
          setDcValidationResult(validateRes.data);
          setDcRelationshipEdges(validateRes.data.relationshipEdges ?? []);
          setDcWarningsAcknowledged(false);
          setDcConflictResolutions({});
          setState((prev) => ({
            ...prev,
            validationResult: {
              valid: validateRes.data.valid,
              totalRows: validateRes.data.totalEntities ?? 0,
              validRows: validateRes.data.valid ? (validateRes.data.totalEntities ?? 0) : 0,
              errors: (validateRes.data.errors ?? []).map((e, i) => ({
                row: i,
                field: e.field,
                message: e.message,
                errorCode: e.code,
              })),
              warnings: [],
              headers: Object.keys(validateRes.data.entitiesByType ?? {}),
              fileName: file.name,
              previewRows: [],
            },
          }));
        } catch (e) {
          setWizardError(e instanceof Error ? e.message : 'DC validation failed');
        }
        return;
      }

      if (importType === 'csv') {
        const upload = await wizard.uploadFile.mutateAsync({
          file,
          importType: 'CSV',
        });
        if (!upload.success) {
          setWizardError(upload.errorMessage || 'Upload failed');
          return;
        }
        await refreshVirusScanStatus(upload.uploadId, upload.virusScanStatus);

        const headers = upload.detectedHeaders || [];
        const clientResult = await validateCsvClientSide(file);
        if (headers.length > 0 && clientResult) {
          let mappings = generateFieldMappings(headers, targetFields);
          try {
            const { mappings: serverMappings, typeWarnings: warnings } = await autoMapFromHeaders(
              headers,
              targetFields
            );
            if (serverMappings.some((m) => m.mapped)) {
              mappings = serverMappings;
            }
            if (warnings.length > 0) {
              setTypeWarnings(warnings);
            }
          } catch {
            // Client-side mapping fallback
          }
          setState((prev) => ({
            ...prev,
            validationResult: clientResult,
            fieldMappings: mappings,
          }));
        }

        return;
      }

      if (file.name.endsWith('.csv') || file.name.endsWith('.xlsx')) {
        await validateCsvClientSide(file);
      }
    } catch (e) {
      setWizardError(e instanceof Error ? e.message : 'Upload failed');
    }
  };

  const handleTemplateDownload = async (templateId: string) => {
    try {
      await downloadTemplate(templateId);
    } catch {
      // Error handled in hook
    }
  };

  const handleMappingsChange = (mappings: FieldMapping[]) => {
    setState((prev) => ({ ...prev, fieldMappings: mappings }));
  };

  useMigrationSse(state.currentJobId, {
    onProgress: (progress) => {
      setState((prev) => ({
        ...prev,
        jobProgress: progress,
        step: ['COMPLETED', 'FAILED', 'CANCELLED'].includes(progress.jobStatus) ? 'complete' : prev.step,
      }));
    },
    onComplete: async () => {
      if (state.currentJobId) {
        try {
          const result = await getImportResult(state.currentJobId);
          setState((prev) => ({ ...prev, importResult: result, step: 'complete' }));
        } catch {
          // polling fallback handles result
        }
      }
    },
  });

  const handleStartImport = async () => {
    if (!state.importType) return;
    setWizardError(null);

    try {
      let job: { id: string; jobStatus: string };

      if (state.importType === 'workflow-xml') {
        if (!state.selectedFile) return;
        const res = await migrationApi.importWorkflowXml(
          state.selectedFile,
          schemeFile || undefined,
          workflowXmlStub,
          workflowXmlMakeDefault,
          targetProjectId || undefined
        );
        const jobData = res.data as { id: string; jobStatus?: string };
        job = { id: jobData.id, jobStatus: jobData.jobStatus || 'IN_PROGRESS' };
      } else if (state.importType === 'project-import') {
        if (!sourceProjectId || !targetProjectId) return;
        const res = await migrationApi.startProjectImport(sourceProjectId, targetProjectId);
        const jobData = res.data as { id: string; jobStatus?: string };
        job = { id: jobData.id, jobStatus: jobData.jobStatus || 'IN_PROGRESS' };
      } else if (state.importType === 'project-export') {
        if (!targetProjectId) return;
        const res = await migrationApi.startProjectExport(targetProjectId, exportFormat);
        const jobData = res.data as { id: string; jobStatus?: string };
        job = { id: jobData.id, jobStatus: jobData.jobStatus || 'IN_PROGRESS' };
      } else if (state.importType === 'jira-dc' && state.selectedFile) {
        const isZip = state.selectedFile.name.toLowerCase().endsWith('.zip');
        job = await startImport('jira-dc', {
          file: state.selectedFile,
          targetProjectId,
          attachmentBundle: dcAttachmentBundleFile,
          backupZip: dcImportOptions.backupZip || isZip,
          options: {
            blockOnValidationErrors: dcImportOptions.blockOnValidationErrors,
            dryRun: dcImportOptions.dryRun,
            resume: dcImportOptions.resume,
            parallelWorkers: dcImportOptions.parallelWorkers,
            attachmentBundlePath: dcImportOptions.attachmentBundlePath || undefined,
            incrementalDelta: dcImportOptions.incrementalDelta,
            rollbackOnFailure: true,
            historyOnlyImport: dcImportOptions.historyOnlyImport,
            historyReplayOnly: dcImportOptions.historyReplayOnly,
            stubDownstream: dcImportOptions.stubDownstream,
            conflictResolutions: buildConflictResolutionsPayload(dcConflictResolutions),
          },
        });
      } else if (wizard.sessionId && state.importType === 'csv') {
        await wizard.saveFieldMappings.mutateAsync(state.fieldMappings);
        job = await wizard.executeImport.mutateAsync({
          targetProjectId: targetProjectId || undefined,
          options: {
            importMode: state.importOptions.importMode,
          },
        });
      } else {
        if (!state.selectedFile) return;
        job = await startImport(state.importType as 'csv' | 'jira-dc', {
          file: state.selectedFile,
          targetProjectId,
          fieldMappings: state.fieldMappings,
        });
      }

      setState((prev) => ({
        ...prev,
        currentJobId: job.id,
        jobProgress: {
          jobId: job.id,
          jobStatus: job.jobStatus,
          progressPercentage: 0,
          totalEntities: 0,
          processedEntities: 0,
          failedEntities: 0,
          entityProgress: [],
        },
      }));

      goToStep('importing');

      // Start polling
      pollJobProgress(job.id, (progress) => {
        setState((prev) => ({
          ...prev,
          jobProgress: progress,
          step: ['COMPLETED', 'FAILED', 'CANCELLED'].includes(progress.jobStatus) ? 'complete' : prev.step,
        }));
      });
    } catch {
      // Error handled in hook
    }
  };

  const handleCancelImport = async () => {
    if (!state.currentJobId) return;

    try {
      await cancelJob.mutateAsync(state.currentJobId);
      stopPolling();
      goToStep('complete');
    } catch {
      // Error handled in hook
    }
  };

  const jobTypeToImportType = (jobType?: string): ImportType | null => {
    switch (jobType?.toUpperCase()) {
      case 'JIRA_DC':
        return 'jira-dc';
      case 'CSV':
        return 'csv';
      case 'WORKFLOW_XML':
        return 'workflow-xml';
      case 'PROJECT_IMPORT':
        return 'project-import';
      case 'PROJECT_EXPORT':
      case 'EXPORT':
        return 'project-export';
      default:
        return null;
    }
  };

  const handleViewDetails = (jobId: string, jobType?: string) => {
    setDetailJobId(jobId);
    setDetailJobImportType(jobTypeToImportType(jobType));
  };

  const handleRetryJob = async (jobId: string) => {
    try {
      await migrationApi.retryJob(jobId);
      queryClient.invalidateQueries({ queryKey: ['migration-job-history'] });
    } catch (error) {
      console.error('Failed to retry job:', error);
    }
  };

  const handleDownloadValidationReport = async () => {
    try {
      const response = wizard.sessionId
        ? await migrationWizardApi.downloadValidationReport(wizard.sessionId)
        : state.currentJobId
          ? await migrationApi.downloadValidationReport(state.currentJobId)
          : null;
      if (!response) return;
      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `validation-report-${wizard.sessionId || state.currentJobId}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download validation report:', error);
    }
  };

  const handleRollbackJob = async (jobId: string) => {
    if (!window.confirm('Rollback this import? Created issues, comments, and attachments will be removed.')) {
      return;
    }
    try {
      const response = await migrationApi.rollbackJob(jobId);
      alert(
        `Rollback finished: ${response.data.rolledBackCount} entities removed, ${response.data.failedCount} failures.`
      );
      queryClient.invalidateQueries({ queryKey: ['migration-job-history'] });
    } catch (error) {
      console.error('Rollback failed:', error);
      alert('Rollback failed. See console for details.');
    }
  };

  const handleDownloadReport = async (jobId: string) => {
    try {
      const response = await migrationApi.downloadJobReport(jobId);
      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `migration-report-${jobId}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download report:', error);
    }
  };

  const handleOpenImportedProject = () => {
    if (targetProjectId) {
      navigate(`/projects/${targetProjectId}`);
    }
  };

  const handleReset = () => {
    wizard.resetWizard();
    setWizardError(null);
    resetValidation();
    setState({
      step: 'source',
      importType: null,
      selectedFile: null,
      validationResult: null,
      fieldMappings: [],
      importOptions: { importMode: 'CREATE_UPDATE' },
      currentJobId: null,
      jobProgress: null,
      importResult: null,
    });
    setTargetProjectId('');
    setSourceProjectId('');
    setSchemeFile(null);
    setWorkflowXmlValidation(null);
    setWorkflowXmlOutcome(null);
    setWorkflowXmlStub(true);
    setWorkflowXmlMakeDefault(false);
    stopPolling();
  };

  // Render step content
  const renderContent = () => {
    switch (state.step) {
      case 'source':
        return (
          <div className="space-y-6">
            <MigrationCapabilityIndex />
            <MigrationServiceHealthPanel />
            <ImportTypeSelector
              selectedType={state.importType}
              onTypeSelect={handleTypeSelect}
            />
            {state.importType === 'workflow-xml' && (
              <WorkflowXmlImportPanel
                mode="upload"
                workflowFile={state.selectedFile}
                schemeFile={schemeFile}
                onWorkflowFileChange={(f) =>
                  setState((prev) => ({ ...prev, selectedFile: f }))
                }
                onSchemeFileChange={setSchemeFile}
                targetProjectId={targetProjectId}
                onValidationChange={setWorkflowXmlValidation}
                importOutcome={workflowXmlOutcome}
                stubDownstream={workflowXmlStub}
                makeDefault={workflowXmlMakeDefault}
                onStubDownstreamChange={setWorkflowXmlStub}
                onMakeDefaultChange={setWorkflowXmlMakeDefault}
              />
            )}
            {state.importType === 'project-import' && (
              <div className="bg-white rounded-lg border p-4 text-sm text-gray-600">
                Project-to-project import copies entities from a source project into a target project.
                Continue to select source and target on the next step.
              </div>
            )}
            {state.importType === 'project-export' && (
              <ProjectExportPanel
                projects={(projects as { id: string; name: string; projectKey: string }[]) || []}
                projectId={targetProjectId}
                format={exportFormat}
                onProjectChange={setTargetProjectId}
                onFormatChange={setExportFormat}
              />
            )}
            {state.importType && state.importType !== 'project-export' && state.importType !== 'workflow-xml' && state.importType !== 'project-import' && (
              <MigrationFileUploader
                onFileSelect={handleFileSelect}
                onUploadCancel={wizard.cancelUpload}
                onTemplateDownload={handleTemplateDownload}
                templates={templates}
                isLoading={wizard.uploadFile.isPending || isLoadingTemplates}
                uploadProgress={wizard.uploadProgress}
                validationResult={state.validationResult}
                accept={
                  state.importType === 'jira-dc' || state.importType === 'workflow-xml'
                    ? '.xml'
                    : '.csv,.xlsx,.xml'
                }
                importTypeLabel={
                  state.importType === 'workflow-xml'
                    ? 'Jira DC workflow-descriptor XML'
                    : state.importType === 'jira-dc'
                    ? 'Jira DC XML export'
                    : 'CSV or Excel (.xlsx)'
                }
                virusScanStatus={virusScanStatus}
              />
            )}
          </div>
        );

      case 'targetProject':
        if (state.importType === 'project-import') {
          return (
            <ProjectImportPanel
              projects={(projects as { id: string; name: string; projectKey: string }[]) || []}
              sourceProjectId={sourceProjectId}
              targetProjectId={targetProjectId}
              onSourceChange={setSourceProjectId}
              onTargetChange={setTargetProjectId}
            />
          );
        }
        if (state.importType === 'project-export') {
          return (
            <ProjectExportPanel
              projects={(projects as { id: string; name: string; projectKey: string }[]) || []}
              projectId={targetProjectId}
              format={exportFormat}
              onProjectChange={setTargetProjectId}
              onFormatChange={setExportFormat}
            />
          );
        }
        return (
          <div className="bg-white rounded-lg border p-6">
            <h3 className="text-lg font-semibold mb-4">Select Target Project</h3>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Target Project *
            </label>
            <select
              data-testid="migration-target-project-select"
              value={targetProjectId}
              onChange={(e) => setTargetProjectId(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
            >
              <option value="">Select a project...</option>
              {projects?.map((project: { id: string; name: string; projectKey: string }) => (
                <option key={project.id} value={project.id}>
                  {project.name} ({project.projectKey})
                </option>
              ))}
            </select>
            {state.selectedFile && (
              <p className="text-sm text-gray-500 mt-4">
                Source file: <span className="font-medium">{state.selectedFile.name}</span>
                {wizard.session?.totalRows != null && (
                  <> · {wizard.session.totalRows} rows detected</>
                )}
              </p>
            )}
          </div>
        );

      case 'validate':
        if (state.importType === 'workflow-xml') {
          return (
            <WorkflowXmlImportPanel
              mode="validate"
              workflowFile={state.selectedFile}
              schemeFile={schemeFile}
              onWorkflowFileChange={(f) =>
                setState((prev) => ({ ...prev, selectedFile: f }))
              }
              onSchemeFileChange={setSchemeFile}
              targetProjectId={targetProjectId}
              onValidationChange={setWorkflowXmlValidation}
              importOutcome={workflowXmlOutcome}
              stubDownstream={workflowXmlStub}
              makeDefault={workflowXmlMakeDefault}
              onStubDownstreamChange={setWorkflowXmlStub}
              onMakeDefaultChange={setWorkflowXmlMakeDefault}
            />
          );
        }
        if (state.importType === 'jira-dc' && dcValidationResult) {
          return (
            <div className="space-y-4">
              <DcImportValidationPanel
                xmlOrZipFile={state.selectedFile}
                attachmentBundleFile={dcAttachmentBundleFile}
                backupZip={dcImportOptions.backupZip}
                options={dcImportOptions}
                initialResult={dcValidationResult}
                onValidated={(r) => {
                  setDcValidationResult(r);
                  setDcRelationshipEdges(r.relationshipEdges ?? []);
                }}
              />
              <DcImportInsightsPanel
                validationResult={dcValidationResult}
                relationshipEdges={dcRelationshipEdges}
              />
              <DcImportConflictPanel
                conflicts={dcValidationResult.conflicts}
                acknowledged={dcWarningsAcknowledged}
                onAcknowledgeChange={setDcWarningsAcknowledged}
                resolutions={dcConflictResolutions}
                onResolutionChange={handleDcConflictResolution}
              />
              <DcImportUnknownFieldsPanel unknownFields={dcValidationResult.unknownCustomFields} />
              <DcRelationshipGraphPanel edges={dcRelationshipEdges} />
            </div>
          );
        }

        return (
          <div className="space-y-4">
            {isValidating ? (
              <div className="bg-white rounded-lg border p-8 text-center">
                <div className="w-12 h-12 border-4 border-jira-blue border-t-transparent rounded-full animate-spin mx-auto mb-4" />
                <p className="text-gray-600">Validating file...</p>
              </div>
            ) : parseError ? (
              <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                <p className="text-red-800 font-medium">Validation Error</p>
                <p className="text-red-600 mt-1">{parseError}</p>
              </div>
            ) : validationResult ? (
              <>
                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={handleDownloadValidationReport}
                    className="px-4 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded-lg"
                  >
                    Download validation report (CSV)
                  </button>
                </div>
                <ValidationResults
                  result={validationResult}
                  onExportErrors={exportErrorsToCsv}
                  onRowClick={(row, column) => {
                    console.log(`Navigate to row ${row}, column ${column}`);
                  }}
                />
                {state.importType === 'jira-dc' && (
                  <>
                    <DcImportValidationPanel
                      xmlOrZipFile={state.selectedFile}
                      attachmentBundleFile={dcAttachmentBundleFile}
                      backupZip={dcImportOptions.backupZip}
                      options={dcImportOptions}
                      onValidated={(r) => {
                        setDcValidationResult(r);
                        setDcRelationshipEdges(r.relationshipEdges ?? []);
                      }}
                    />
                    <DcImportInsightsPanel
                      validationResult={
                        dcValidationResult ?? {
                          valid: (validationResult.errors?.length ?? 0) === 0,
                          message:
                            (validationResult.errors?.length ?? 0) === 0
                              ? 'DC XML structure validated'
                              : 'Fix validation errors before import',
                          riskScore:
                            (validationResult.errors?.length ?? 0) === 0 ? 92 : 45,
                        }
                      }
                      relationshipEdges={dcRelationshipEdges}
                    />
                    <DcImportConflictPanel
                      conflicts={dcValidationResult?.conflicts}
                      acknowledged={dcWarningsAcknowledged}
                      onAcknowledgeChange={setDcWarningsAcknowledged}
                      resolutions={dcConflictResolutions}
                      onResolutionChange={handleDcConflictResolution}
                    />
                    <DcImportUnknownFieldsPanel
                      unknownFields={dcValidationResult?.unknownCustomFields}
                    />
                    <DcRelationshipGraphPanel edges={dcRelationshipEdges} />
                  </>
                )}
              </>
            ) : (
              <div className="bg-white rounded-lg border p-8 text-center">
                <p className="text-gray-500">No validation results. Please upload a file first.</p>
              </div>
            )}
          </div>
        );

      case 'map':
        return loadingTargetFields ? (
          <div className="bg-white rounded-lg border p-8 text-center">
            <div className="w-12 h-12 border-4 border-jira-blue border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-gray-600">Loading target fields from platform catalog…</p>
          </div>
        ) : (
          <FieldMappingPanel
            sourceHeaders={state.validationResult?.headers || wizard.session?.detectedHeaders || []}
            targetFields={targetFields}
            initialMappings={state.fieldMappings}
            onMappingsChange={handleMappingsChange}
            typeWarnings={typeWarnings}
          />
        );

      case 'configure':
        if (state.importType === 'jira-dc') {
          return (
            <div className="space-y-6">
              <DcImportOptionsPanel
                options={dcImportOptions}
                onChange={setDcImportOptions}
                attachmentBundleFile={dcAttachmentBundleFile}
                onAttachmentBundleFileChange={setDcAttachmentBundleFile}
              />
              <DcImportValidationPanel
                xmlOrZipFile={state.selectedFile}
                attachmentBundleFile={dcAttachmentBundleFile}
                backupZip={dcImportOptions.backupZip}
                options={dcImportOptions}
                onValidated={(r) => {
                  setDcValidationResult(r);
                  setDcRelationshipEdges(r.relationshipEdges ?? []);
                  setDcWarningsAcknowledged(false);
                }}
              />
              <DcImportConflictPanel
                conflicts={dcValidationResult?.conflicts}
                acknowledged={dcWarningsAcknowledged}
                onAcknowledgeChange={setDcWarningsAcknowledged}
                resolutions={dcConflictResolutions}
                onResolutionChange={handleDcConflictResolution}
              />
              <DcImportUnknownFieldsPanel unknownFields={dcValidationResult?.unknownCustomFields} />
              <DcRelationshipGraphPanel edges={dcRelationshipEdges} />
              <ConfigureImportPanel
                importMode={state.importOptions.importMode}
                onImportModeChange={(mode) =>
                  setState((prev) => ({
                    ...prev,
                    importOptions: {
                      ...prev.importOptions,
                      importMode: mode as 'CREATE_ONLY' | 'UPDATE_ONLY' | 'CREATE_UPDATE',
                    },
                  }))
                }
                fieldDefaults={fieldDefaults}
                onFieldDefaultsChange={setFieldDefaults}
                workflowStatusMappings={workflowStatusMappings}
                onWorkflowStatusMappingsChange={setWorkflowStatusMappings}
                requiredTargetFields={targetFields}
              />
            </div>
          );
        }
        return (
          <ConfigureImportPanel
            importMode={state.importOptions.importMode}
            onImportModeChange={(mode) =>
              setState((prev) => ({
                ...prev,
                importOptions: {
                  ...prev.importOptions,
                  importMode: mode as 'CREATE_ONLY' | 'UPDATE_ONLY' | 'CREATE_UPDATE',
                },
              }))
            }
            fieldDefaults={fieldDefaults}
            onFieldDefaultsChange={setFieldDefaults}
            workflowStatusMappings={workflowStatusMappings}
            onWorkflowStatusMappingsChange={setWorkflowStatusMappings}
            requiredTargetFields={targetFields}
          />
        );

      case 'review':
        if (state.importType === 'jira-dc') {
          const targetName = projects?.find((p: { id: string }) => p.id === targetProjectId)?.name;
          return (
            <DcImportReviewPanel
              validation={dcValidationResult}
              options={dcImportOptions}
              targetProjectName={targetName}
              fileName={state.selectedFile?.name}
              warningsAcknowledged={dcWarningsAcknowledged}
              onWarningsAcknowledgeChange={setDcWarningsAcknowledged}
              conflictResolutions={dcConflictResolutions}
              onConflictResolutionChange={handleDcConflictResolution}
            />
          );
        }
        if (state.importType === 'project-export') {
          const exportProject = projects?.find((p: { id: string }) => p.id === targetProjectId);
          return (
            <div className="bg-white rounded-lg border p-6 space-y-4" data-testid="project-export-review">
              <h3 className="text-lg font-semibold">Project export — review</h3>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-gray-500">Project</span>
                  <p className="font-medium">
                    {exportProject
                      ? `${exportProject.projectKey} — ${exportProject.name}`
                      : targetProjectId || '—'}
                  </p>
                </div>
                <div>
                  <span className="text-gray-500">Format</span>
                  <p className="font-medium uppercase">{exportFormat}</p>
                </div>
              </div>
              <p className="text-xs text-gray-600">
                Export runs as a background job. Download the report from Job history when complete.
              </p>
            </div>
          );
        }
        if (state.importType === 'workflow-xml') {
          return (
            <WorkflowXmlImportPanel
              mode="review"
              workflowFile={state.selectedFile}
              schemeFile={schemeFile}
              onWorkflowFileChange={(f) =>
                setState((prev) => ({ ...prev, selectedFile: f }))
              }
              onSchemeFileChange={setSchemeFile}
              targetProjectId={targetProjectId}
              onValidationChange={setWorkflowXmlValidation}
              importOutcome={workflowXmlOutcome}
              stubDownstream={workflowXmlStub}
              makeDefault={workflowXmlMakeDefault}
              onStubDownstreamChange={setWorkflowXmlStub}
              onMakeDefaultChange={setWorkflowXmlMakeDefault}
            />
          );
        }
        return (
          <div className="space-y-6">
            <div className="bg-white rounded-lg border p-6">
              <h3 className="text-lg font-semibold mb-4">Review & Execute</h3>
              <div className="grid grid-cols-2 gap-4 text-sm mb-6">
                <div>
                  <span className="text-gray-500">Import type</span>
                  <p className="font-medium">{state.importType}</p>
                </div>
                <div>
                  <span className="text-gray-500">Target project</span>
                  <p className="font-medium">
                    {projects?.find((p: { id: string }) => p.id === targetProjectId)?.name ||
                      targetProjectId ||
                      '—'}
                  </p>
                </div>
                <div>
                  <span className="text-gray-500">Source file</span>
                  <p className="font-medium">{state.selectedFile?.name || '—'}</p>
                </div>
                <div>
                  <span className="text-gray-500">Import mode</span>
                  <p className="font-medium">{state.importOptions.importMode}</p>
                </div>
              </div>
              <div className="grid grid-cols-4 gap-4">
                <div className="bg-gray-50 rounded-lg p-4">
                  <div className="text-2xl font-bold text-gray-900">
                    {state.validationResult?.totalRows || wizard.session?.totalRows || 0}
                  </div>
                  <div className="text-sm text-gray-500">Total Rows</div>
                </div>
                <div className="bg-green-50 rounded-lg p-4">
                  <div className="text-2xl font-bold text-green-600">
                    {state.validationResult?.validRows || 0}
                  </div>
                  <div className="text-sm text-gray-500">Valid Rows</div>
                </div>
                <div className="bg-blue-50 rounded-lg p-4">
                  <div className="text-2xl font-bold text-blue-600">
                    {state.fieldMappings.filter((m) => m.mapped).length}
                  </div>
                  <div className="text-sm text-gray-500">Mapped Fields</div>
                </div>
                <div className="bg-yellow-50 rounded-lg p-4">
                  <div className="text-2xl font-bold text-yellow-600">
                    {(state.validationResult?.warnings?.length || 0) +
                      (state.validationResult?.errors?.length || 0)}
                  </div>
                  <div className="text-sm text-gray-500">Issues Found</div>
                </div>
              </div>
            </div>
          </div>
        );

      case 'importing':
        return state.jobProgress ? (
          <ImportProgress
            progress={state.jobProgress}
            onCancel={handleCancelImport}
            onViewLogs={() => setShowLogs(!showLogs)}
            showLogs={showLogs}
            onOpenJobConsole={
              state.currentJobId ? () => setDetailJobId(state.currentJobId) : undefined
            }
          />
        ) : (
          <div className="bg-white rounded-lg border p-8 text-center">
            <div className="w-12 h-12 border-4 border-jira-blue border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-gray-600">Starting import...</p>
          </div>
        );

      case 'complete':
        if (state.importType === 'workflow-xml' && workflowXmlOutcome) {
          return (
            <WorkflowXmlImportPanel
              mode="complete"
              workflowFile={state.selectedFile}
              schemeFile={schemeFile}
              onWorkflowFileChange={() => {}}
              onSchemeFileChange={() => {}}
              importOutcome={workflowXmlOutcome}
              stubDownstream={workflowXmlStub}
              makeDefault={workflowXmlMakeDefault}
              onStubDownstreamChange={setWorkflowXmlStub}
              onMakeDefaultChange={setWorkflowXmlMakeDefault}
            />
          );
        }
        return (
          <div className="space-y-6">
            {state.currentJobId && (
              <>
                <MigrationVerificationPanel jobId={state.currentJobId} />
                <MigrationReindexPanel jobId={state.currentJobId} />
                <ImportedIssuesPanel jobId={state.currentJobId} />
                <ImportedAttachmentsPanel jobId={state.currentJobId} />
                <DcStagingInsightsPanel jobId={state.currentJobId} />
              </>
            )}
            {/* Result summary */}
            <div
              className={`rounded-lg border p-6 ${
                state.importResult?.jobStatus === 'COMPLETED'
                  ? 'bg-green-50 border-green-200'
                  : state.importResult?.jobStatus === 'FAILED'
                  ? 'bg-red-50 border-red-200'
                  : 'bg-gray-50 border-gray-200'
              }`}
            >
              <div className="flex items-center gap-4">
                <span className="text-4xl">
                  {state.importResult?.jobStatus === 'COMPLETED'
                    ? '🎉'
                    : state.importResult?.jobStatus === 'FAILED'
                    ? '😞'
                    : '🚫'}
                </span>
                <div>
                  <h3 className="text-xl font-semibold text-gray-900">
                    {state.importResult?.jobStatus === 'COMPLETED'
                      ? 'Import Completed Successfully!'
                      : state.importResult?.jobStatus === 'FAILED'
                      ? 'Import Failed'
                      : 'Import Cancelled'}
                  </h3>
                  <p className="text-gray-600 mt-1">
                    {state.importResult
                      ? `${state.importResult.successCount.toLocaleString()} succeeded, ${state.importResult.failedEntities.toLocaleString()} failed${
                          state.importResult.durationMs
                            ? ` · ${Math.round(state.importResult.durationMs / 1000)}s`
                            : ''
                        }`
                      : 'No result data available'}
                  </p>
                </div>
              </div>
            </div>

            {/* Statistics */}
            {state.importResult && (
              <div className="grid grid-cols-4 gap-4">
                <div className="bg-white rounded-lg border p-4 text-center">
                  <div className="text-2xl font-bold text-gray-900">
                    {state.importResult.totalEntities.toLocaleString()}
                  </div>
                  <div className="text-sm text-gray-500">Total Entities</div>
                </div>
                <div className="bg-green-50 rounded-lg border p-4 text-center">
                  <div className="text-2xl font-bold text-green-600">
                    {state.importResult.successCount.toLocaleString()}
                  </div>
                  <div className="text-sm text-gray-500">Successful</div>
                </div>
                <div className="bg-red-50 rounded-lg border p-4 text-center">
                  <div className="text-2xl font-bold text-red-600">
                    {state.importResult.failedEntities.toLocaleString()}
                  </div>
                  <div className="text-sm text-gray-500">Failed</div>
                </div>
                <div className="bg-yellow-50 rounded-lg border p-4 text-center">
                  <div className="text-2xl font-bold text-yellow-600">
                    {state.importResult.warningCount.toLocaleString()}
                  </div>
                  <div className="text-sm text-gray-500">Warnings</div>
                </div>
              </div>
            )}

            {/* Errors list */}
            {state.importResult?.errors && state.importResult.errors.length > 0 && (
              <div className="bg-white rounded-lg border overflow-hidden">
                <div className="px-4 py-3 bg-red-50 border-b border-red-200">
                  <h4 className="font-medium text-red-800">
                    Import Errors ({state.importResult.errors.length})
                  </h4>
                </div>
                <div className="max-h-64 overflow-y-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Entity</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Field</th>
                        <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Error</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                      {state.importResult.errors.slice(0, 20).map((error, index) => (
                        <tr key={index}>
                          <td className="px-4 py-2 text-sm text-gray-900">{error.entityKey}</td>
                          <td className="px-4 py-2 text-sm text-gray-600">{error.field || '-'}</td>
                          <td className="px-4 py-2 text-sm text-red-600">{error.errorMessage}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {state.importResult.errors.length > 20 && (
                    <div className="px-4 py-2 text-center text-sm text-gray-500 bg-gray-50">
                      ...and {state.importResult.errors.length - 20} more errors
                    </div>
                  )}
                </div>
              </div>
            )}

            {state.importType === 'jira-dc' && (
              <>
                <DcImportParityReportPanel
                  resultMetadata={state.importResult?.resultMetadata}
                  validationEntitiesByType={dcValidationResult?.entitiesByType}
                  jobStatus={state.importResult?.jobStatus}
                />
                <DcImportSlaProofPanel
                  jobId={state.currentJobId}
                  embeddedSla={
                    state.importResult?.resultMetadata?.slaProof as Record<string, unknown> | undefined
                  }
                />
                <DcImportAcSignoffPanel
                  jobId={state.currentJobId}
                  embeddedSignoff={
                    state.importResult?.resultMetadata?.acSignoff as Record<string, unknown> | undefined
                  }
                />
                <DcImportInsightsPanel
                  validationResult={dcValidationResult}
                  relationshipEdges={dcRelationshipEdges}
                />
                <DcImportJobOperationsPanel
                  jobId={state.currentJobId}
                  relationshipEdges={dcRelationshipEdges}
                />
              </>
            )}

            {/* Actions */}
            <div className="flex flex-wrap gap-4">
              {state.currentJobId && (
                <button
                  type="button"
                  data-testid="open-job-console-button"
                  onClick={() => setDetailJobId(state.currentJobId)}
                  className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors"
                >
                  Open job console
                </button>
              )}
              {targetProjectId && state.importResult?.jobStatus === 'COMPLETED' && (
                <button
                  onClick={handleOpenImportedProject}
                  className="px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors"
                >
                  Open Imported Project
                </button>
              )}
              {state.currentJobId && (
                <button
                  onClick={() => handleDownloadReport(state.currentJobId!)}
                  className="px-6 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
                >
                  Download Import Report
                </button>
              )}
              <button
                onClick={handleReset}
                className="px-6 py-2 bg-jira-blue text-white rounded-lg hover:bg-blue-600 transition-colors"
              >
                Start New Import
              </button>
              <button
                onClick={() => setCenterView('history')}
                className="px-6 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
              >
                View Job History
              </button>
            </div>
          </div>
        );
    }
  };

  return (
    <div
      className="min-h-screen"
      style={{
        maxWidth: 1280,
        margin: '0 auto',
        padding: 'var(--sa-space-6)',
        background: 'var(--sa-n50)',
      }}
    >
      <PageHeader
        title="Migration Center"
        subtitle="Import data from CSV, Excel, or Jira Data Center backups into your target project"
        actions={
          centerView === 'wizard' ? (
            <MigrationRoleSelector />
          ) : undefined
        }
      />

      <div className="mb-6">
        <MigrationCenterNav active={centerView} onChange={setCenterView} />
      </div>

      <ClusterHealthBanner />

      {wizardError && centerView === 'wizard' && (
        <p className="text-sm mt-2 mb-4" role="alert" style={{ color: 'var(--sa-status-blocked-fg)' }}>
          {wizardError}
        </p>
      )}

      {centerView === 'history' && (
        <div className="space-y-4" data-testid="migration-history-view">
          <JobHistoryTable
            onViewDetails={handleViewDetails}
            onRetryJob={handleRetryJob}
            onRollbackJob={handleRollbackJob}
            onDownloadReport={handleDownloadReport}
            showPagination
            limit={10}
          />
        </div>
      )}

      {centerView === 'health' && <MigrationPlatformHealthView />}

      {centerView === 'catalog' && (
        <MigrationFeatureCatalog
          onNavigate={(view) => {
            setCenterView(view);
            if (view === 'wizard') {
              setState((prev) => ({ ...prev, step: 'source' }));
            }
          }}
        />
      )}

      {centerView === 'wizard' && (
        <>
      {wizard.sessionId && (
        <p className="text-xs mb-4" style={{ color: 'var(--sa-n500)' }}>
          Wizard session: {wizard.sessionId} · step: {wizard.session?.step ?? '—'}
        </p>
      )}

      <MigrationWizardStepper
        activeStepOrder={activeStepOrder}
        currentStep={state.step}
        getStepIndex={getStepIndex}
      />

      {/* Main content */}
      <div className="min-h-[400px]">{renderContent()}</div>

      {/* Navigation */}
      {state.step !== 'complete' && state.step !== 'importing' && (
        <div className="flex items-center justify-between mt-8 pt-6 border-t">
          <button
            onClick={canGoBack() ? goBack : undefined}
            disabled={!canGoBack()}
            className="px-4 py-2 text-gray-600 hover:text-gray-900 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Back
          </button>

          <div className="flex gap-4">
            {state.step === 'source' && (
              <button
                type="button"
                onClick={() => setCenterView('history')}
                className="px-4 py-2 text-gray-600 hover:text-gray-900"
              >
                View job history
              </button>
            )}

            {state.step === 'review' && (
              <button
                data-testid="import-execute-button"
                onClick={handleStartImport}
                disabled={
                  state.importType === 'workflow-xml'
                    ? !state.selectedFile
                    : state.importType === 'jira-dc'
                    ? !targetProjectId || !dcValidationReady()
                    : !targetProjectId
                }
                className="migration-btn-primary"
              >
                {state.importType === 'workflow-xml'
                  ? 'Import Workflow'
                  : state.importType === 'project-export'
                  ? 'Start Export'
                  : 'Start Import'}
              </button>
            )}

            {state.step !== 'review' && canGoNext() && (
              <button
                data-testid="step-continue-button"
                onClick={goNext}
                className="migration-btn-primary"
              >
                Continue
              </button>
            )}
          </div>
        </div>
      )}

        </>
      )}

      {detailJobId && (
        <MigrationJobDetailPanel
          jobId={detailJobId}
          importType={detailJobImportType ?? state.importType}
          resultMetadata={
            detailJobResultMetadata ?? state.importResult?.resultMetadata ?? null
          }
          onClose={() => {
            setDetailJobId(null);
            setDetailJobImportType(null);
            setDetailJobResultMetadata(null);
          }}
        />
      )}
    </div>
  );
}
