import React from 'react';



export interface DcImportOptions {

  dryRun: boolean;

  resume: boolean;

  parallelWorkers: number;

  attachmentBundlePath: string;

  blockOnValidationErrors: boolean;

  backupZip: boolean;

  incrementalDelta: boolean;

  historyOnlyImport: boolean;

  /** Skip creating issues; replay history/comments on prior mappings only. */
  historyReplayOnly: boolean;

  /** When true, skips real issue-service writes (dev only). */
  stubDownstream: boolean;

}



interface Props {

  options: DcImportOptions;

  onChange: (options: DcImportOptions) => void;

  attachmentBundleFile: File | null;

  onAttachmentBundleFileChange: (file: File | null) => void;

}



export default function DcImportOptionsPanel({

  options,

  onChange,

  attachmentBundleFile,

  onAttachmentBundleFileChange,

}: Props) {

  const set = <K extends keyof DcImportOptions>(key: K, value: DcImportOptions[K]) =>

    onChange({ ...options, [key]: value });



  return (

    <div className="bg-white rounded-lg border p-6 space-y-4" data-testid="dc-import-options-panel">

      <h3 className="text-lg font-semibold">Jira DC import options</h3>

      <p className="text-sm text-gray-600">

        Options are sent to the migration API on validate and execute. Use UI uploads — not hidden server paths.

      </p>



      <label className="flex items-center gap-2">

        <input

          type="checkbox"

          checked={options.backupZip}

          onChange={(e) => set('backupZip', e.target.checked)}

          className="rounded border-gray-300"

        />

        <span className="text-sm">Uploaded file is a native DC backup ZIP (entities.xml inside)</span>

      </label>



      <label className="flex items-center gap-2">

        <input

          type="checkbox"

          checked={options.dryRun}

          onChange={(e) => set('dryRun', e.target.checked)}

          className="rounded border-gray-300"

        />

        <span className="text-sm">Dry run (validate + stage only, no writes)</span>

      </label>



      <label className="flex items-center gap-2">

        <input

          type="checkbox"

          checked={options.resume}

          onChange={(e) => set('resume', e.target.checked)}

          className="rounded border-gray-300"

        />

        <span className="text-sm">Resume previous import job</span>

      </label>



      <label className="flex items-center gap-2">

        <input

          type="checkbox"

          checked={options.incrementalDelta}

          onChange={(e) => set('incrementalDelta', e.target.checked)}

          className="rounded border-gray-300"

        />

        <span className="text-sm">Incremental delta (skip entities already imported in target)</span>

      </label>



      <label className="flex items-center gap-2">

        <input

          type="checkbox"

          checked={options.blockOnValidationErrors}

          onChange={(e) => set('blockOnValidationErrors', e.target.checked)}

          className="rounded border-gray-300"

        />

        <span className="text-sm">Block import when validation has errors</span>

      </label>



      <label className="flex items-center gap-2">

        <input

          type="checkbox"

          checked={options.historyOnlyImport}

          onChange={(e) => set('historyOnlyImport', e.target.checked)}

          className="rounded border-gray-300"

        />

        <span className="text-sm">History-only import (skip workflow status transitions)</span>

      </label>



      <label className="flex items-center gap-2">

        <input

          type="checkbox"

          checked={options.historyReplayOnly}

          onChange={(e) => set('historyReplayOnly', e.target.checked)}

          className="rounded border-gray-300"

          data-testid="dc-history-replay-only"

        />

        <span className="text-sm">History replay on existing issues (do not create new issues)</span>

      </label>



      <button

        type="button"

        className="text-xs px-3 py-1.5 border border-blue-200 bg-blue-50 text-blue-900 rounded hover:bg-blue-100"

        data-testid="dc-history-replay-preset"

        onClick={() =>

          onChange({

            ...options,

            historyOnlyImport: true,

            historyReplayOnly: true,

            dryRun: false,

            incrementalDelta: true,

          })

        }

      >

        Apply history-replay preset (existing project)

      </button>



      {options.historyReplayOnly && (

        <p className="text-xs text-blue-800 border border-blue-100 bg-blue-50 rounded p-2" role="status">

          Issues and sub-tasks are skipped unless a prior successful import mapping exists for each key.

          Run an initial import or enable incremental delta first.

        </p>

      )}



      <label className="flex items-start gap-2 border border-amber-200 bg-amber-50 rounded-lg p-3">

        <input

          type="checkbox"

          checked={options.stubDownstream}

          onChange={(e) => set('stubDownstream', e.target.checked)}

          className="rounded border-gray-300 mt-0.5"

        />

        <span className="text-sm text-amber-900">

          <span className="font-medium">Stub downstream</span> — simulate persist without calling issue-service

          (SLA proof will not count as met)

        </span>

      </label>



      <div>

        <label className="block text-sm font-medium text-gray-700 mb-1">

          Parallel workers (comments / attachments / worklogs)

        </label>

        <input

          type="number"

          min={1}

          max={8}

          value={options.parallelWorkers}

          onChange={(e) => set('parallelWorkers', Number(e.target.value) || 1)}

          className="w-32 px-3 py-2 border rounded-lg"

        />

      </div>



      <div>

        <label className="block text-sm font-medium text-gray-700 mb-1">

          Attachment bundle ZIP (optional)

        </label>

        <input

          type="file"

          accept=".zip"

          onChange={(e) => onAttachmentBundleFileChange(e.target.files?.[0] ?? null)}

          className="block w-full text-sm"

        />

        {attachmentBundleFile && (

          <p className="text-xs text-gray-500 mt-1">{attachmentBundleFile.name}</p>

        )}

      </div>



      <div>

        <label className="block text-sm font-medium text-gray-700 mb-1">

          Attachment bundle path (server-side fallback)

        </label>

        <input

          type="text"

          value={options.attachmentBundlePath}

          onChange={(e) => set('attachmentBundlePath', e.target.value)}

          placeholder="Optional host path if bundle already on server"

          className="w-full px-3 py-2 border rounded-lg font-mono text-sm"

        />

      </div>

    </div>

  );

}

