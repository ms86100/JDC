import { canAdminMigration } from '../utils/migrationRoleUtils';

export type CsvImportProfile = 'LIGHTWEIGHT' | 'EXTERNAL';

export interface CsvImportOptions {
  csvImportProfile: CsvImportProfile;
  attachmentColumn: string;
  attachmentsImportDir: string;
}

interface Props {
  value: CsvImportOptions;
  onChange: (next: CsvImportOptions) => void;
  hasAttachmentColumn?: boolean;
  maxAttachmentSizeMb?: number;
}

export default function CsvImportOptionsPanel({
  value,
  onChange,
  hasAttachmentColumn = false,
  maxAttachmentSizeMb = 10,
}: Props) {
  const isExternal = value.csvImportProfile === 'EXTERNAL';
  const isAdmin = canAdminMigration();

  return (
    <div
      className="bg-white border rounded-lg p-4 space-y-4"
      data-testid="csv-import-options-panel"
    >
      <div>
        <h3 className="font-semibold text-gray-900">Jira DC CSV import profile</h3>
        <p className="text-sm text-gray-600 mt-1">
          Mirrors Jira Data Center: lightweight issue CSV vs External System Import (attachments, subtasks).
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <label
          className={`border rounded-lg p-3 cursor-pointer ${value.csvImportProfile === 'LIGHTWEIGHT' ? 'border-jira-blue bg-blue-50' : ''}`}
        >
          <input
            type="radio"
            name="csvImportProfile"
            className="mr-2"
            checked={value.csvImportProfile === 'LIGHTWEIGHT'}
            onChange={() => onChange({ ...value, csvImportProfile: 'LIGHTWEIGHT' })}
          />
          <span className="font-medium">Lightweight</span>
          <p className="text-xs text-gray-600 mt-1">Issues only — no attachment column import (G-03).</p>
        </label>
        <label
          className={`border rounded-lg p-3 cursor-pointer ${value.csvImportProfile === 'EXTERNAL' ? 'border-jira-blue bg-blue-50' : ''} ${!isAdmin ? 'opacity-60' : ''}`}
        >
          <input
            type="radio"
            name="csvImportProfile"
            className="mr-2"
            disabled={!isAdmin}
            checked={value.csvImportProfile === 'EXTERNAL'}
            onChange={() => onChange({ ...value, csvImportProfile: 'EXTERNAL' })}
          />
          <span className="font-medium">External System Import</span>
          <p className="text-xs text-gray-600 mt-1">
            HTTP/HTTPS URLs and FILE: paths (G-01, G-02). Requires migration admin role.
          </p>
        </label>
      </div>

      {isExternal && (
        <div className="space-y-3 border-t pt-3">
          <div>
            <label className="block text-sm font-medium text-gray-700">Attachments column name</label>
            <input
              type="text"
              className="mt-1 w-full border rounded px-3 py-2 text-sm"
              value={value.attachmentColumn}
              onChange={(e) => onChange({ ...value, attachmentColumn: e.target.value })}
              placeholder="Attachments"
            />
            {hasAttachmentColumn && (
              <p className="text-xs text-green-700 mt-1">Detected in uploaded CSV.</p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">FILE: import directory (optional)</label>
            <input
              type="text"
              className="mt-1 w-full border rounded px-3 py-2 text-sm font-mono"
              value={value.attachmentsImportDir}
              onChange={(e) => onChange({ ...value, attachmentsImportDir: e.target.value })}
              placeholder="C:\jira-home\import\attachments"
            />
            <p className="text-xs text-gray-500 mt-1">
              Jira DC: <code>FILE:filename</code> resolves here. Also set server env{' '}
              <code>MIGRATION_IMPORT_ATTACHMENTS_DIR</code>.
            </p>
          </div>
          <p className="text-xs text-amber-800 bg-amber-50 border border-amber-200 rounded p-2">
            Max attachment size: {maxAttachmentSizeMb} MB (configurable in Migration settings / env). URLs are
            downloaded at import time (G-01).
          </p>
        </div>
      )}
    </div>
  );
}
