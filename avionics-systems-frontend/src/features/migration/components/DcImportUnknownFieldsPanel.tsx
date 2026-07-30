import React from 'react';
import type { LegacyDcValidateResponse } from '../../../api/serviceApi';

interface Props {
  unknownFields: LegacyDcValidateResponse['unknownCustomFields'];
}

export default function DcImportUnknownFieldsPanel({ unknownFields }: Props) {
  if (!unknownFields?.length) {
    return null;
  }

  return (
    <div
      className="rounded-lg border border-purple-200 bg-purple-50 p-4 space-y-2"
      data-testid="dc-import-unknown-fields-panel"
    >
      <h4 className="text-sm font-semibold text-purple-900">
        Unknown custom fields ({unknownFields.length})
      </h4>
      <p className="text-xs text-purple-800">
        These fields are not in the platform registry. Values may be skipped or stored as text until
        mapped in admin.
      </p>
      <ul className="text-xs text-purple-900 space-y-1 max-h-36 overflow-y-auto">
        {unknownFields.map((f, i) => (
          <li key={`${f.fieldId}-${i}`} className="font-mono">
            {f.fieldId}
            {f.message ? <span className="text-purple-700 font-sans"> — {f.message}</span> : null}
          </li>
        ))}
      </ul>
    </div>
  );
}
