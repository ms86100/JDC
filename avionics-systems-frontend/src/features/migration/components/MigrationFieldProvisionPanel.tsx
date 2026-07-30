import { useCallback, useEffect, useMemo, useState } from 'react';
import { migrationWizardApi } from '../../../api/serviceApi';

interface Props {
  sessionId: string;
  sourceHeaders: string[];
  /** G-08: hide provision when lightweight profile and user is not admin */
  allowProvision?: boolean;
  onProvisionComplete?: (summary: {
    totalProvisioned: number;
    totalExisting: number;
    totalFailed: number;
    fieldKeyMapping: Record<string, string>;
  }) => void | Promise<void>;
}

function hasAttachmentColumn(headers: string[]): boolean {
  return headers.some((h) => /^attachments?$/i.test((h ?? '').trim()));
}

export default function MigrationFieldProvisionPanel({
  sessionId,
  sourceHeaders,
  allowProvision = true,
  onProvisionComplete,
}: Props) {
  const [needsProvisioning, setNeedsProvisioning] = useState<number | null>(null);
  const [discoverError, setDiscoverError] = useState<string | null>(null);
  const [provisionStatus, setProvisionStatus] = useState<'idle' | 'working' | 'done' | 'error'>('idle');
  const [provisionMessage, setProvisionMessage] = useState<string | null>(null);

  const attachmentColumnPresent = useMemo(() => hasAttachmentColumn(sourceHeaders), [sourceHeaders]);

  const runDiscover = useCallback(async () => {
    if (!sessionId || sourceHeaders.length === 0) {
      setNeedsProvisioning(0);
      return;
    }
    setDiscoverError(null);
    try {
      const res = await migrationWizardApi.discoverSessionFields(sessionId);
      const count =
        res.data?.discoveredFields?.filter((f) => f.requiresProvisioning && !f.isKnown).length ?? 0;
      setNeedsProvisioning(count);
    } catch (e) {
      setDiscoverError(e instanceof Error ? e.message : 'Discovery failed');
      setNeedsProvisioning(null);
    }
  }, [sessionId, sourceHeaders.length]);

  useEffect(() => {
    runDiscover();
  }, [runDiscover]);

  const handleProvision = async () => {
    setProvisionStatus('working');
    setProvisionMessage(null);
    try {
      const res = await migrationWizardApi.provisionMissingSessionFields(sessionId);
      const data = res.data;
      const msg = `Provisioned ${data?.totalProvisioned ?? 0} new field(s), ${data?.totalExisting ?? 0} already existed${
        (data?.totalFailed ?? 0) > 0 ? `, ${data?.totalFailed} failed` : ''
      }.`;
      setProvisionMessage(msg);
      setProvisionStatus('done');
      setNeedsProvisioning(0);
      await onProvisionComplete?.({
        totalProvisioned: data?.totalProvisioned ?? 0,
        totalExisting: data?.totalExisting ?? 0,
        totalFailed: data?.totalFailed ?? 0,
        fieldKeyMapping: data?.fieldKeyMapping ?? {},
      });
      await runDiscover();
    } catch (e) {
      setProvisionStatus('error');
      setProvisionMessage(e instanceof Error ? e.message : 'Provisioning failed');
    }
  };

  if (sourceHeaders.length === 0) {
    return null;
  }

  return (
    <div
      className="bg-blue-50 border border-blue-200 rounded-lg p-4 space-y-3"
      data-testid="migration-field-provision-panel"
    >
      <div>
        <h3 className="font-semibold text-blue-900">Systems DC field provisioning</h3>
        <p className="text-sm text-blue-800 mt-1">
          Like Systems Data Center <strong>External System Import</strong>, unmapped CSV columns can be auto-created as
          custom fields before you map them to platform targets.
        </p>
      </div>

      {attachmentColumnPresent && (
        <div className="text-sm text-amber-900 bg-amber-50 border border-amber-200 rounded p-2">
          <strong>Attachments column detected.</strong> Map URLs (https://…) per Systems DC External Import, or use{' '}
          <strong>Systems DC XML + attachment bundle</strong> for full binary parity. Lightweight CSV import cannot attach
          files without URLs.
        </div>
      )}

      {discoverError && (
        <p className="text-sm text-red-700">Could not scan columns: {discoverError}</p>
      )}

      {needsProvisioning !== null && needsProvisioning > 0 && (
        <p className="text-sm text-blue-900">
          <strong>{needsProvisioning}</strong> column(s) need new custom field definitions on the platform.
        </p>
      )}

      {!allowProvision && (
        <p className="text-sm text-amber-800">
          Field auto-provision requires External Import profile and migration admin role (G-08).
        </p>
      )}

      {needsProvisioning === 0 && provisionStatus !== 'working' && (
        <p className="text-sm text-green-800">All columns match existing platform fields (or are already provisioned).</p>
      )}

      {provisionMessage && (
        <p className={`text-sm ${provisionStatus === 'error' ? 'text-red-700' : 'text-green-800'}`}>
          {provisionMessage}
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          className="px-3 py-1.5 text-sm bg-avisys-blue text-white rounded hover:opacity-90 disabled:opacity-50"
          disabled={!allowProvision || provisionStatus === 'working' || needsProvisioning === 0}
          onClick={handleProvision}
        >
          {provisionStatus === 'working' ? 'Provisioning…' : 'Provision missing fields'}
        </button>
        <button
          type="button"
          className="px-3 py-1.5 text-sm border border-blue-300 rounded text-blue-900 hover:bg-blue-100"
          onClick={() => runDiscover()}
        >
          Rescan columns
        </button>
      </div>
    </div>
  );
}
