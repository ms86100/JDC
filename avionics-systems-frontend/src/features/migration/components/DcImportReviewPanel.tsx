import React from 'react';

import type { LegacyDcValidateResponse } from '../../../api/serviceApi';

import type { DcImportOptions } from './DcImportOptionsPanel';

import DcImportConflictPanel from './DcImportConflictPanel';

import DcImportUnknownFieldsPanel from './DcImportUnknownFieldsPanel';

import type { DcConflictResolution } from '../types/dcConflictResolution';



interface Props {

  validation: LegacyDcValidateResponse | null;

  options: DcImportOptions;

  targetProjectName?: string;

  fileName?: string;

  warningsAcknowledged: boolean;

  onWarningsAcknowledgeChange: (v: boolean) => void;

  conflictResolutions?: Record<string, DcConflictResolution>;

  onConflictResolutionChange?: (conflictId: string, resolution: DcConflictResolution) => void;

}



export default function DcImportReviewPanel({

  validation,

  options,

  targetProjectName,

  fileName,

  warningsAcknowledged,

  onWarningsAcknowledgeChange,

  conflictResolutions,

  onConflictResolutionChange,

}: Props) {

  const entities = validation?.entitiesByType ?? {};

  const total = validation?.totalEntities ?? 0;



  return (

    <div className="space-y-4" data-testid="dc-import-review-panel">

      <div className="bg-white rounded-lg border p-6">

        <h3 className="text-lg font-semibold mb-4">Avionics Systems import — review</h3>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mb-4">

          <div>

            <span className="text-gray-500">Source</span>

            <p className="font-medium">{fileName ?? '—'}</p>

          </div>

          <div>

            <span className="text-gray-500">Format</span>

            <p className="font-medium">{validation?.format ?? '—'}</p>

          </div>

          <div>

            <span className="text-gray-500">Target project</span>

            <p className="font-medium">{targetProjectName ?? '—'}</p>

          </div>

          <div>

            <span className="text-gray-500">Entities</span>

            <p className="font-medium">{total.toLocaleString()}</p>

          </div>

        </div>

        <div className="flex flex-wrap gap-2 mb-4">

          {Object.entries(entities).map(([type, count]) => (

            <span key={type} className="text-xs bg-gray-100 border rounded px-2 py-1">

              {type}: {count}

            </span>

          ))}

        </div>

        <ul className="text-xs text-gray-600 space-y-1">

          <li>Backup ZIP: {options.backupZip ? 'yes' : 'no'}</li>

          <li>Dry run: {options.dryRun ? 'yes' : 'no'}</li>

          <li>Incremental: {options.incrementalDelta ? 'yes' : 'no'}</li>

          <li>History-only: {options.historyOnlyImport ? 'yes' : 'no'}</li>

          <li>History replay (no new issues): {options.historyReplayOnly ? 'yes' : 'no'}</li>

          <li>Parallel workers: {options.parallelWorkers}</li>

          <li>

            Downstream writes:{' '}

            {options.stubDownstream ? (

              <span className="text-amber-700 font-medium">stub (no issue-service)</span>

            ) : (

              <span className="text-green-700 font-medium">live</span>

            )}

          </li>

        </ul>

      </div>

      <DcImportConflictPanel

        conflicts={validation?.conflicts}

        acknowledged={warningsAcknowledged}

        onAcknowledgeChange={onWarningsAcknowledgeChange}

        resolutions={conflictResolutions}

        onResolutionChange={onConflictResolutionChange}

      />

      <DcImportUnknownFieldsPanel unknownFields={validation?.unknownCustomFields} />

    </div>

  );

}

