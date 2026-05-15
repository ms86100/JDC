import React, { useState, useCallback, useRef, useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMigrationJob } from '../hooks/useMigrationJob';
import { useValidation } from '../hooks/useValidation';
import CsvUploader from '../components/CsvUploader';
import ValidationResults from '../components/ValidationResults';
import FieldMappingPanel from '../components/FieldMappingPanel';
import ImportProgress from '../components/ImportProgress';
import JobHistoryTable from '../components/JobHistoryTable';
import ImportTypeSelector, { ImportType } from '../components/ImportTypeSelector';
import type {
  MigrationState,
  ValidationResult,
  FieldMapping,
  JobProgress,
  ImportResult,
} from '../types/migration';
import { projectApi } from '../../../api/projectApi';

// Step definitions
type MigrationStep = 'select' | 'upload' | 'validate' | 'map' | 'preview' | 'importing' | 'complete';

const STEP_ORDER: MigrationStep[] = ['select', 'upload', 'validate', 'map', 'preview', 'importing', 'complete'];

const STEP_LABELS: Record<MigrationStep, string> = {
  select: 'Select Type',
  upload: 'Upload File',
  validate: 'Validate',
  map: 'Map Fields',
  preview: 'Preview',
  importing: 'Importing',
  complete: 'Complete',
};

// Target fields for mapping (standard Jira fields)
const TARGET_FIELDS = [
  { field: 'summary', displayName: 'Summary', dataType: 'STRING', required: true, description: 'Issue title or summary' },
  { field: 'description', displayName: 'Description', dataType: 'TEXT', required: false, description: 'Detailed description' },
  { field: 'issuetype', displayName: 'Issue Type', dataType: 'ENUM', required: true, description: 'Bug, Story, Task, etc.' },
  { field: 'priority', displayName: 'Priority', dataType: 'ENUM', required: false, description: 'Critical, High, Medium, Low' },
  { field: 'status', displayName: 'Status', dataType: 'ENUM', required: false, description: 'To Do, In Progress, Done' },
  { field: 'project', displayName: 'Project Key', dataType: 'STRING', required: true, description: 'Target project key' },
  { field: 'assignee', displayName: 'Assignee', dataType: 'USER', required: false, description: 'User assigned to the issue' },
  { field: 'reporter', displayName: 'Reporter', dataType: 'USER', required: false, description: 'User who reported the issue' },
  { field: 'labels', displayName: 'Labels', dataType: 'ARRAY', required: false, description: 'Comma-separated labels' },
  { field: 'components', displayName: 'Components', dataType: 'ARRAY', required: false, description: 'Project components' },
  { field: 'fixVersion', displayName: 'Fix Version', dataType: 'VERSION', required: false, description: 'Version to fix in' },
  { field: 'affectedVersion', displayName: 'Affects Version', dataType: 'VERSION', required: false, description: 'Version affected' },
  { field: 'duedate', displayName: 'Due Date', dataType: 'DATE', required: false, description: 'Issue due date' },
  { field: 'created', displayName: 'Created Date', dataType: 'DATETIME', required: false, description: 'Creation timestamp' },
  { field: 'updated', displayName: 'Updated Date', dataType: 'DATETIME', required: false, description: 'Last update timestamp' },
  { field: 'securitylevel', displayName: 'Security Level', dataType: 'ENUM', required: false, description: 'Issue security level' },
  { field: 'environment', displayName: 'Environment', dataType: 'TEXT', required: false, description: 'Operating environment' },
  { field: 'parent', displayName: 'Parent Issue', dataType: 'ISSUE', required: false, description: 'Parent issue key' },
  { field: 'epic', displayName: 'Epic Link', dataType: 'ISSUE', required: false, description: 'Epic issue key' },
  { field: 'sprint', displayName: 'Sprint', dataType: 'STRING', required: false, description: 'Sprint name or ID' },
];

export default function MigrationPage() {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Migration state
  const [state, setState] = useState<MigrationState>({
    step: 'select',
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

  // Additional state
  const [targetProjectId, setTargetProjectId] = useState<string>('');
  const [showHistory, setShowHistory] = useState(false);
  const [showLogs, setShowLogs] = useState(false);

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
    enabled: state.importType === 'csv' || state.importType === 'project-import',
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
      const mappings = generateFieldMappings(result.headers, TARGET_FIELDS);
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
  const getStepIndex = (step: MigrationStep): number => STEP_ORDER.indexOf(step);

  const canGoBack = (): boolean => {
    const currentIndex = getStepIndex(state.step);
    return currentIndex > 0;
  };

  const canGoNext = (): boolean => {
    switch (state.step) {
      case 'select':
        return state.importType !== null;
      case 'upload':
        return state.selectedFile !== null;
      case 'validate':
        return state.validationResult !== null && state.validationResult.errors.length === 0;
      case 'map':
        return state.fieldMappings.some((m) => m.mapped);
      case 'preview':
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
      goToStep(STEP_ORDER[currentIndex - 1]);
    }
  };

  const goNext = () => {
    const currentIndex = getStepIndex(state.step);
    if (currentIndex < STEP_ORDER.length - 1) {
      goToStep(STEP_ORDER[currentIndex + 1]);
    }
  };

  // Handlers
  const handleTypeSelect = (type: ImportType) => {
    setState((prev) => ({ ...prev, importType: type }));
  };

  const handleFileSelect = async (file: File) => {
    setState((prev) => ({ ...prev, selectedFile: file }));

    // Auto-validate for CSV
    if (state.importType === 'csv' || file.name.endsWith('.csv')) {
      try {
        await validateCsvClientSide(file);
        goToStep('validate');
      } catch {
        // Error handled in hook
      }
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

  const handleStartImport = async () => {
    if (!state.selectedFile || !state.importType) return;

    try {
      // Map importType to API-expected type
      const apiImportType = state.importType === 'project-import' ? 'project' : state.importType;
      const job = await startImport(apiImportType as 'csv' | 'jira-dc' | 'project', {
        file: state.selectedFile,
        targetProjectId,
      });

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

  const handleViewDetails = async (jobId: string) => {
    try {
      const progress = await getJobProgress(jobId);
      const result = await getImportResult(jobId);
      console.log('Job details:', { progress, result });
      // Would set selected job and show details modal
    } catch (error) {
      console.error('Failed to load job details:', error);
    }
  };

  const handleRetryJob = async (jobId: string) => {
    try {
      const progress = await getJobProgress(jobId);
      console.log('Retry job:', jobId, progress);
      // Would restart import with same configuration
    } catch (error) {
      console.error('Failed to retry job:', error);
    }
  };

  const handleDownloadReport = async (jobId: string) => {
    try {
      // Call report download endpoint
      window.open(`/api/migration/jobs/${jobId}/report`, '_blank');
    } catch (error) {
      console.error('Failed to download report:', error);
    }
  };

  const handleReset = () => {
    resetValidation();
    setState({
      step: 'select',
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
    stopPolling();
  };

  // Render step content
  const renderContent = () => {
    switch (state.step) {
      case 'select':
        return (
          <ImportTypeSelector
            selectedType={state.importType}
            onTypeSelect={handleTypeSelect}
          />
        );

      case 'upload':
        return (
          <CsvUploader
            onFileSelect={handleFileSelect}
            onTemplateDownload={handleTemplateDownload}
            templates={templates}
            isLoading={isLoadingTemplates}
            validationResult={state.validationResult}
          />
        );

      case 'validate':
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
              <ValidationResults
                result={validationResult}
                onExportErrors={exportErrorsToCsv}
                onRowClick={(row, column) => {
                  console.log(`Navigate to row ${row}, column ${column}`);
                }}
              />
            ) : (
              <div className="bg-white rounded-lg border p-8 text-center">
                <p className="text-gray-500">No validation results. Please upload a file first.</p>
              </div>
            )}
          </div>
        );

      case 'map':
        return (
          <FieldMappingPanel
            sourceHeaders={state.validationResult?.headers || []}
            targetFields={TARGET_FIELDS}
            initialMappings={state.fieldMappings}
            onMappingsChange={handleMappingsChange}
          />
        );

      case 'preview':
        return (
          <div className="space-y-6">
            <div className="bg-white rounded-lg border p-6">
              <h3 className="text-lg font-semibold mb-4">Import Configuration</h3>

              <div className="grid grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Target Project *
                  </label>
                  <select
                    value={targetProjectId}
                    onChange={(e) => setTargetProjectId(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
                  >
                    <option value="">Select a project...</option>
                    {projects?.map((project: any) => (
                      <option key={project.id} value={project.id}>
                        {project.name} ({project.projectKey})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Import Mode
                  </label>
                  <select
                    value={state.importOptions.importMode}
                    onChange={(e) =>
                      setState((prev) => ({
                        ...prev,
                        importOptions: {
                          ...prev.importOptions,
                          importMode: e.target.value as 'CREATE_ONLY' | 'UPDATE_ONLY' | 'CREATE_UPDATE',
                        },
                      }))
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
                  >
                    <option value="CREATE_UPDATE">Create New + Update Existing</option>
                    <option value="CREATE_ONLY">Create New Only</option>
                    <option value="UPDATE_ONLY">Update Existing Only</option>
                  </select>
                </div>
              </div>

              {/* Summary */}
              <div className="mt-6 pt-6 border-t">
                <h4 className="text-sm font-medium text-gray-700 mb-3">Import Summary</h4>
                <div className="grid grid-cols-4 gap-4">
                  <div className="bg-gray-50 rounded-lg p-4">
                    <div className="text-2xl font-bold text-gray-900">
                      {state.validationResult?.totalRows || 0}
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
                      {(state.validationResult?.warnings.length || 0) + (state.validationResult?.errors.length || 0)}
                    </div>
                    <div className="text-sm text-gray-500">Issues Found</div>
                  </div>
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
          />
        ) : (
          <div className="bg-white rounded-lg border p-8 text-center">
            <div className="w-12 h-12 border-4 border-jira-blue border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-gray-600">Starting import...</p>
          </div>
        );

      case 'complete':
        return (
          <div className="space-y-6">
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
                      ? `${state.importResult.successCount.toLocaleString()} succeeded, ${state.importResult.failedEntities.toLocaleString()} failed`
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

            {/* Actions */}
            <div className="flex gap-4">
              <button
                onClick={handleReset}
                className="px-6 py-2 bg-jira-blue text-white rounded-lg hover:bg-blue-600 transition-colors"
              >
                Start New Import
              </button>
              <button
                onClick={() => setShowHistory(true)}
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
    <div className="p-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Import / Export</h1>
        <p className="text-gray-500 mt-1">
          Import data from CSV or Jira DC backups, export projects
        </p>
      </div>

      {/* Step indicator */}
      <div className="flex items-center gap-2 mb-8">
        {STEP_ORDER.filter((s) => s !== 'complete').map((step, index) => {
          const currentIndex = getStepIndex(state.step);
          const stepIndex = STEP_ORDER.indexOf(step);
          const isActive = stepIndex <= currentIndex && state.step !== 'complete';
          const isCurrent = step === state.step || (state.step === 'complete' && index === STEP_ORDER.length - 2);

          return (
            <React.Fragment key={step}>
              <div className="flex items-center">
                <div
                  className={`
                    w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium transition-colors
                    ${isCurrent ? 'bg-jira-blue text-white' : isActive ? 'bg-green-500 text-white' : 'bg-gray-200 text-gray-500'}
                  `}
                >
                  {isActive && stepIndex < currentIndex ? '✓' : index + 1}
                </div>
                <span className={`ml-2 text-sm ${isActive ? 'text-gray-900' : 'text-gray-400'}`}>
                  {STEP_LABELS[step]}
                </span>
              </div>
              {index < STEP_ORDER.filter((s) => s !== 'complete').length - 1 && (
                <div className={`flex-1 h-1 mx-4 rounded ${
                  stepIndex < currentIndex ? 'bg-green-500' : 'bg-gray-200'
                }`} />
              )}
            </React.Fragment>
          );
        })}
      </div>

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
            {state.step === 'select' && (
              <button
                onClick={() => setShowHistory(!showHistory)}
                className="px-4 py-2 text-gray-600 hover:text-gray-900"
              >
                {showHistory ? 'Hide History' : 'View History'}
              </button>
            )}

            {state.step === 'preview' && (
              <button
                onClick={handleStartImport}
                disabled={!targetProjectId}
                className="px-6 py-2 bg-jira-blue text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Start Import
              </button>
            )}

            {state.step !== 'preview' && state.step !== 'upload' && canGoNext() && (
              <button
                onClick={goNext}
                className="px-6 py-2 bg-jira-blue text-white rounded-lg hover:bg-blue-600 transition-colors"
              >
                Continue
              </button>
            )}
          </div>
        </div>
      )}

      {/* Job History (collapsible) */}
      {showHistory && (
        <div className="mt-8 pt-8 border-t">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Recent Jobs</h2>
            <button
              onClick={() => setShowHistory(false)}
              className="text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          </div>
          <JobHistoryTable
            onViewDetails={handleViewDetails}
            onRetryJob={handleRetryJob}
            onDownloadReport={handleDownloadReport}
            showPagination={false}
            limit={5}
          />
        </div>
      )}
    </div>
  );
}
