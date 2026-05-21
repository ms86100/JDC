interface DcImportInsightsPanelProps {
  validationResult?: {
    valid?: boolean;
    format?: string;
    riskScore?: number;
    entitiesByType?: Record<string, number>;
    hasIssue?: boolean;
    hasComment?: boolean;
    hasAttachment?: boolean;
    message?: string;
  } | null;
  fieldWarnings?: string[];
  relationshipEdges?: Array<{ from: string; to: string; type: string }>;
}

export function DcImportInsightsPanel({
  validationResult,
  fieldWarnings = [],
  relationshipEdges = [],
}: DcImportInsightsPanelProps) {
  if (!validationResult && fieldWarnings.length === 0 && relationshipEdges.length === 0) {
    return null;
  }

  return (
    <div className="mt-4 rounded-lg border border-gray-200 bg-gray-50 p-4 space-y-4">
      <h4 className="text-sm font-semibold text-gray-800">Jira DC import insights</h4>

      {validationResult && (
        <div className="text-sm text-gray-700 space-y-1">
          <p>
            Format: <span className="font-medium">{validationResult.format ?? '—'}</span>
            {' · '}
            Risk score: <span className="font-medium">{validationResult.riskScore ?? '—'}</span>
          </p>
          <p className={validationResult.valid ? 'text-green-700' : 'text-amber-700'}>
            {validationResult.message}
          </p>
          {validationResult.entitiesByType && (
            <ul className="list-disc list-inside text-xs text-gray-600">
              {Object.entries(validationResult.entitiesByType).map(([type, count]) => (
                <li key={type}>
                  {type}: {count}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {fieldWarnings.length > 0 && (
        <div>
          <p className="text-xs font-medium text-amber-800 mb-1">Custom field mapping warnings</p>
          <ul className="text-xs text-amber-700 list-disc list-inside">
            {fieldWarnings.map((w) => (
              <li key={w}>{w}</li>
            ))}
          </ul>
        </div>
      )}

      {relationshipEdges.length > 0 && (
        <div>
          <p className="text-xs font-medium text-gray-800 mb-1">Relationship graph (preview)</p>
          <ul className="text-xs text-gray-600 font-mono space-y-0.5">
            {relationshipEdges.map((e, i) => (
              <li key={`${e.from}-${e.to}-${i}`}>
                {e.from} —[{e.type}]→ {e.to}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
