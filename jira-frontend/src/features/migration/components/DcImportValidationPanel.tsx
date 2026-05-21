import React, { useEffect, useState } from 'react';
import { migrationApi, type JiraDcValidateResponse } from '../../../api/serviceApi';
import type { DcImportOptions } from './DcImportOptionsPanel';
import { DcImportInsightsPanel } from './DcImportInsightsPanel';
import DcImportConflictPanel from './DcImportConflictPanel';
import DcImportUnknownFieldsPanel from './DcImportUnknownFieldsPanel';

interface Props {
  xmlOrZipFile: File | null;
  attachmentBundleFile: File | null;
  backupZip: boolean;
  options: DcImportOptions;
  initialResult?: JiraDcValidateResponse | null;
  onValidated?: (result: JiraDcValidateResponse) => void;
}

export default function DcImportValidationPanel({
  xmlOrZipFile,
  attachmentBundleFile,
  backupZip,
  options,
  initialResult,
  onValidated,
}: Props) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<JiraDcValidateResponse | null>(initialResult ?? null);
  const [warningsAcknowledged, setWarningsAcknowledged] = useState(false);

  useEffect(() => {
    if (initialResult) {
      setResult(initialResult);
    }
  }, [initialResult]);

  const runValidate = async () => {
    if (!xmlOrZipFile) {
      setError('Upload an XML or DC backup ZIP file first');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const response = await migrationApi.validateJiraDcImport({
        file: xmlOrZipFile,
        attachmentBundle: attachmentBundleFile,
        backupZip: backupZip || xmlOrZipFile.name.toLowerCase().endsWith('.zip'),
        options: {
          dryRun: options.dryRun,
          blockOnValidationErrors: options.blockOnValidationErrors,
        },
      });
      setResult(response.data);
      setWarningsAcknowledged(false);
      onValidated?.(response.data);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Validation request failed');
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-lg border p-6 space-y-4" data-testid="dc-import-validation-panel">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold">DC import validation (server)</h3>
          <p className="text-sm text-gray-600">
            Runs the real orchestrator validation API — same path as dry-run import.
          </p>
        </div>
        <button
          type="button"
          data-testid="dc-validate-button"
          onClick={runValidate}
          disabled={loading || !xmlOrZipFile}
          className="px-4 py-2 bg-jira-blue text-white rounded-lg text-sm disabled:opacity-50"
        >
          {loading ? 'Validating…' : 'Validate now'}
        </button>
      </div>

      {error && (
        <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded p-3">{error}</p>
      )}

      {result && (
        <>
          <div
            className={`rounded-lg p-4 text-sm ${
              result.valid ? 'bg-green-50 border border-green-200' : 'bg-amber-50 border border-amber-200'
            }`}
          >
            <p className="font-medium">{result.message}</p>
            <p className="mt-1 text-gray-700">
              Blockers: {result.blockerCount ?? 0} · Warnings: {result.warningCount ?? 0}
              {result.attachmentsRootResolved ? ' · Attachment root resolved' : ''}
            </p>
          </div>

          {(result.errors?.length ?? 0) > 0 && (
            <div>
              <p className="text-xs font-semibold text-red-800 mb-1">Validation errors</p>
              <ul className="text-xs text-red-700 list-disc list-inside">
                {result.errors!.map((e, i) => (
                  <li key={`${e.code}-${i}`}>
                    [{e.code}] {e.field}: {e.message}
                  </li>
                ))}
              </ul>
            </div>
          )}

          <DcImportConflictPanel
            conflicts={result.conflicts}
            acknowledged={warningsAcknowledged}
            onAcknowledgeChange={setWarningsAcknowledged}
          />
          <DcImportUnknownFieldsPanel unknownFields={result.unknownCustomFields} />

          <DcImportInsightsPanel
            validationResult={{
              valid: result.valid,
              format: result.format,
              riskScore: result.riskScore,
              entitiesByType: result.entitiesByType,
              message: result.message,
            }}
            relationshipEdges={result.relationshipEdges}
          />
        </>
      )}
    </div>
  );
}
