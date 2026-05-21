import React, { useState, useCallback } from 'react';
import { migrationApi } from '../../../api/serviceApi';
import WorkflowGraphVisualizer, { WorkflowGraphData } from './WorkflowGraphVisualizer';

export interface WorkflowXmlValidationPayload {
  valid?: boolean;
  workflowName?: string;
  errors?: string[];
  warnings?: string[];
  unsupportedFeatures?: string[];
  graph?: WorkflowGraphData;
  scheme?: { name?: string; mappings?: unknown[] };
}

export interface WorkflowXmlImportOutcome {
  importId?: string;
  workflowName?: string;
  targetWorkflowId?: string;
  targetSchemeId?: string;
  stubDownstream?: boolean;
  schemeImport?: Record<string, unknown>;
  validation?: WorkflowXmlValidationPayload;
  simulation?: unknown;
  unsupportedFeatures?: string[];
}

interface Props {
  workflowFile: File | null;
  schemeFile: File | null;
  onWorkflowFileChange: (file: File | null) => void;
  onSchemeFileChange: (file: File | null) => void;
  targetProjectId?: string;
  mode: 'upload' | 'validate' | 'review' | 'complete';
  onValidationChange?: (payload: WorkflowXmlValidationPayload | null) => void;
  importOutcome?: WorkflowXmlImportOutcome | null;
  stubDownstream: boolean;
  makeDefault: boolean;
  onStubDownstreamChange: (v: boolean) => void;
  onMakeDefaultChange: (v: boolean) => void;
}

export default function WorkflowXmlImportPanel({
  workflowFile,
  schemeFile,
  onWorkflowFileChange,
  onSchemeFileChange,
  targetProjectId,
  mode,
  onValidationChange,
  importOutcome,
  stubDownstream,
  makeDefault,
  onStubDownstreamChange,
  onMakeDefaultChange,
}: Props) {
  const [validating, setValidating] = useState(false);
  const [simulating, setSimulating] = useState(false);
  const [importing, setImporting] = useState(false);
  const [rollingBack, setRollingBack] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validation, setValidation] = useState<WorkflowXmlValidationPayload | null>(null);
  const [simulation, setSimulation] = useState<Record<string, unknown> | null>(null);
  const [startStepId, setStartStepId] = useState('1');
  const [transitionPath, setTransitionPath] = useState('');
  const [lastImportId, setLastImportId] = useState<string | null>(null);

  const runValidate = useCallback(async () => {
    if (!workflowFile) return;
    setValidating(true);
    setError(null);
    try {
      const res = await migrationApi.validateWorkflowXml(workflowFile, schemeFile || undefined);
      const data = res.data as WorkflowXmlValidationPayload;
      setValidation(data);
      onValidationChange?.(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Validation failed');
    } finally {
      setValidating(false);
    }
  }, [workflowFile, schemeFile, onValidationChange]);

  const runSimulate = async () => {
    if (!workflowFile) return;
    setSimulating(true);
    setError(null);
    try {
      const res = await migrationApi.simulateWorkflowXml(
        workflowFile,
        startStepId,
        transitionPath || undefined
      );
      setSimulation(res.data as Record<string, unknown>);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Simulation failed');
    } finally {
      setSimulating(false);
    }
  };

  const runImport = async () => {
    if (!workflowFile) return;
    setImporting(true);
    setError(null);
    try {
      const res = await migrationApi.importWorkflowXml(
        workflowFile,
        schemeFile || undefined,
        stubDownstream,
        makeDefault,
        targetProjectId
      );
      const data = res.data as WorkflowXmlImportOutcome;
      if (data.importId) setLastImportId(data.importId);
      return data;
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Import failed');
      return null;
    } finally {
      setImporting(false);
    }
  };

  const runRollback = async () => {
    const id = lastImportId || importOutcome?.importId;
    if (!id) return;
    setRollingBack(true);
    setError(null);
    try {
      await migrationApi.rollbackWorkflowXmlImport(id);
      setLastImportId(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Rollback failed');
    } finally {
      setRollingBack(false);
    }
  };

  const downloadValidationReport = async () => {
    if (!workflowFile) return;
    try {
      const res = await migrationApi.downloadWorkflowValidationReport(
        workflowFile,
        schemeFile || undefined
      );
      const blob = new Blob([res.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'workflow-validation-report.csv';
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Report download failed');
    }
  };

  const outcome = importOutcome;
  const unsupported = validation?.unsupportedFeatures || outcome?.unsupportedFeatures || [];

  return (
    <div className="space-y-6">
      {(mode === 'upload' || mode === 'validate') && (
        <div className="bg-white rounded-lg border p-6 space-y-4">
          <h3 className="text-lg font-semibold">Workflow XML files</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Workflow descriptor XML *
              </label>
              <input
                type="file"
                accept=".xml"
                onChange={(e) => onWorkflowFileChange(e.target.files?.[0] || null)}
                className="block w-full text-sm"
              />
              {workflowFile && (
                <p className="text-xs text-gray-500 mt-1">{workflowFile.name}</p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Workflow scheme XML (optional)
              </label>
              <input
                type="file"
                accept=".xml"
                onChange={(e) => onSchemeFileChange(e.target.files?.[0] || null)}
                className="block w-full text-sm"
              />
              {schemeFile && (
                <p className="text-xs text-gray-500 mt-1">{schemeFile.name}</p>
              )}
            </div>
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={runValidate}
              disabled={!workflowFile || validating}
              className="px-4 py-2 bg-jira-blue text-white rounded-lg text-sm disabled:opacity-50"
            >
              {validating ? 'Validating…' : 'Validate workflow'}
            </button>
            <button
              type="button"
              onClick={downloadValidationReport}
              disabled={!workflowFile}
              className="px-4 py-2 bg-gray-100 rounded-lg text-sm"
            >
              Download validation CSV
            </button>
          </div>
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-800">
          {error}
        </div>
      )}

      {validation && (mode === 'validate' || mode === 'review' || mode === 'upload') && (
        <div className="bg-white rounded-lg border p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold">Validation results</h3>
            <span
              className={`text-sm font-medium px-2 py-0.5 rounded ${
                validation.valid ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              }`}
            >
              {validation.valid ? 'Valid' : 'Invalid'}
            </span>
          </div>
          {validation.workflowName && (
            <p className="text-sm text-gray-600">
              Workflow: <strong>{validation.workflowName}</strong>
              {validation.scheme?.name && (
                <> · Scheme: <strong>{validation.scheme.name}</strong></>
              )}
            </p>
          )}
          {(validation.errors?.length ?? 0) > 0 && (
            <ul className="text-sm text-red-700 list-disc pl-5">
              {validation.errors!.map((e, i) => (
                <li key={i}>{e}</li>
              ))}
            </ul>
          )}
          {(validation.warnings?.length ?? 0) > 0 && (
            <ul className="text-sm text-amber-800 list-disc pl-5">
              {validation.warnings!.map((w, i) => (
                <li key={i}>{w}</li>
              ))}
            </ul>
          )}
          <WorkflowGraphVisualizer graph={validation.graph || null} />
        </div>
      )}

      {(mode === 'validate' || mode === 'review') && (
        <div className="bg-white rounded-lg border p-6 space-y-4">
          <h3 className="text-lg font-semibold">Simulate transition path</h3>
          <div className="flex flex-wrap gap-3 items-end">
            <div>
              <label className="block text-xs text-gray-600 mb-1">Start step ID</label>
              <input
                value={startStepId}
                onChange={(e) => setStartStepId(e.target.value)}
                className="w-24 px-2 py-1 border rounded"
              />
            </div>
            <div className="flex-1 min-w-[200px]">
              <label className="block text-xs text-gray-600 mb-1">
                Transition path (comma-separated action IDs)
              </label>
              <input
                value={transitionPath}
                onChange={(e) => setTransitionPath(e.target.value)}
                placeholder="e.g. 2,5,7"
                className="w-full px-2 py-1 border rounded font-mono text-sm"
              />
            </div>
            <button
              type="button"
              onClick={runSimulate}
              disabled={!workflowFile || simulating}
              className="px-4 py-2 bg-gray-800 text-white rounded-lg text-sm"
            >
              {simulating ? 'Simulating…' : 'Run simulation'}
            </button>
          </div>
          {simulation && (
            <pre className="text-xs bg-gray-50 p-3 rounded overflow-auto max-h-48">
              {JSON.stringify(simulation, null, 2)}
            </pre>
          )}
        </div>
      )}

      {unsupported.length > 0 && (
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
          <h4 className="font-medium text-amber-900 mb-2">Unsupported / plugin features</h4>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-amber-800">
                <th className="pb-1">Feature</th>
              </tr>
            </thead>
            <tbody>
              {unsupported.map((f, i) => (
                <tr key={i}>
                  <td className="py-0.5">{f}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {mode === 'review' && (
        <div className="bg-white rounded-lg border p-6 space-y-3">
          <h3 className="text-lg font-semibold">Import options</h3>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={stubDownstream}
              onChange={(e) => onStubDownstreamChange(e.target.checked)}
            />
            Stub downstream (skip workflow-service; local record only)
          </label>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={makeDefault}
              onChange={(e) => onMakeDefaultChange(e.target.checked)}
              disabled={stubDownstream}
            />
            Set imported workflow as project default
          </label>
          {!stubDownstream && (
            <p className="text-xs text-amber-700">
              Workflow service must be UP (see health panel). Uncheck stub to push descriptor to
              workflow-service.
            </p>
          )}
        </div>
      )}

      {mode === 'complete' && outcome && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-6 space-y-2">
          <h3 className="text-lg font-semibold text-green-900">Workflow import complete</h3>
          <dl className="text-sm grid grid-cols-2 gap-2">
            <dt className="text-gray-600">Import ID</dt>
            <dd className="font-mono">{outcome.importId}</dd>
            <dt className="text-gray-600">Workflow</dt>
            <dd>{outcome.workflowName}</dd>
            <dt className="text-gray-600">Target workflow ID</dt>
            <dd className="font-mono">{outcome.targetWorkflowId}</dd>
            {outcome.schemeImport && (
              <>
                <dt className="text-gray-600">Scheme import</dt>
                <dd>{String((outcome.schemeImport as { status?: string }).status)}</dd>
              </>
            )}
          </dl>
          <button
            type="button"
            onClick={runRollback}
            disabled={rollingBack || !(lastImportId || outcome.importId)}
            className="mt-2 px-4 py-2 bg-red-600 text-white rounded-lg text-sm"
          >
            {rollingBack ? 'Rolling back…' : 'Rollback this import'}
          </button>
        </div>
      )}

      {/* Expose import runner for parent via ref-less pattern: parent calls import via handleStartImport */}
      {mode === 'review' && (
        <input type="hidden" data-workflow-import-runner="pending" />
      )}
    </div>
  );
}

/** Called from MigrationPage execute step */
export async function executeWorkflowXmlImport(
  workflowFile: File,
  schemeFile: File | null | undefined,
  options: {
    stubDownstream: boolean;
    makeDefault: boolean;
    targetProjectId?: string;
  }
): Promise<WorkflowXmlImportOutcome | null> {
  const res = await migrationApi.importWorkflowXml(
    workflowFile,
    schemeFile || undefined,
    options.stubDownstream,
    options.makeDefault,
    options.targetProjectId
  );
  return res.data as WorkflowXmlImportOutcome;
}
