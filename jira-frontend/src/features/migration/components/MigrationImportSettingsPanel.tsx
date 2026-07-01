import { useQuery } from '@tanstack/react-query';
import { migrationSettingsApi } from '../../../api/serviceApi';
import { Link } from 'react-router-dom';

export default function MigrationImportSettingsPanel() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['migration-import-settings'],
    queryFn: () => migrationSettingsApi.getSettings().then((r) => r.data),
  });

  if (isLoading) {
    return <p className="text-gray-500 p-4">Loading migration settings…</p>;
  }
  if (isError || !data) {
    return (
      <p className="text-red-600 p-4">
        Could not load settings. Ensure migration-service is running on port 8094.
      </p>
    );
  }

  const profiles = data.csvImportProfiles as Record<string, string> | undefined;

  return (
    <div className="space-y-6" data-testid="migration-import-settings-panel">
      <div>
        <h2 className="text-xl font-semibold">Migration import settings</h2>
        <p className="text-sm text-gray-600 mt-1">
          Jira DC parity: attachment limits, FILE: directory, API guidance (G-05, G-07, G-08).
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <div className="bg-white border rounded-lg p-4">
          <h3 className="font-semibold">Attachments (G-05)</h3>
          <dl className="mt-3 space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-500">Max size</dt>
              <dd className="font-medium">{data.maxAttachmentSizeMb} MB</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-500">DC cap</dt>
              <dd>{Math.round((data.maxAttachmentSizeCapBytes as number) / (1024 * 1024))} MB</dd>
            </div>
            <div>
              <dt className="text-gray-500">FILE: directory</dt>
              <dd className="font-mono text-xs mt-1 break-all">
                {(data.attachmentsImportDir as string) || '(not set — use MIGRATION_IMPORT_ATTACHMENTS_DIR)'}
              </dd>
            </div>
            <div>
              <dt className="text-gray-500">Storage</dt>
              <dd className="text-xs mt-1">{data.storageNote as string}</dd>
            </div>
          </dl>
          <p className="text-xs text-gray-500 mt-3">
            Change max size via env <code>MIGRATION_ATTACHMENT_MAX_BYTES</code> and restart migration-service.
          </p>
          <Link to="/admin/system/attachments" className="text-sm text-jira-blue underline mt-2 inline-block">
            Admin → System → Attachments
          </Link>
        </div>

        <div className="bg-white border rounded-lg p-4">
          <h3 className="font-semibold">CSV import profiles (G-03)</h3>
          <ul className="mt-3 space-y-2 text-sm">
            {profiles &&
              Object.entries(profiles).map(([k, v]) => (
                <li key={k}>
                  <span className="font-medium">{k}</span>: {v}
                </li>
              ))}
          </ul>
        </div>

        <div className="bg-white border rounded-lg p-4 md:col-span-2">
          <h3 className="font-semibold">API notes (G-07, G-08)</h3>
          <ul className="mt-2 text-sm text-gray-700 space-y-1 list-disc list-inside">
            <li>
              Legacy <code>/api/migration/fields</code> is deprecated — use{' '}
              <code>/api/migration/mappings</code> and <code>/api/fields/custom</code>.
            </li>
            <li>{data.provisionApiNote as string}</li>
            <li>
              Issue custom values: <code>GET /api/fields/issues/&#123;issueId&#125;/values</code> (shown on issue
              Details tab).
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
