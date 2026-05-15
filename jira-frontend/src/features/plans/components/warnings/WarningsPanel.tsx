import { useWarnings, useDismissWarning } from '../../hooks/useDependencies';

interface WarningsPanelProps {
  planId: string;
}

export default function WarningsPanel({ planId }: WarningsPanelProps) {
  const { data: warnings, isLoading } = useWarnings(planId);
  const dismissMutation = useDismissWarning();

  const handleDismiss = (warningId: string) => {
    dismissMutation.mutate({ planId, warningId });
  };

  const getSeverityClass = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'ab-badge-danger';
      case 'WARNING': return 'ab-badge-warning';
      case 'INFO': return 'ab-badge-info';
      default: return 'ab-badge-secondary';
    }
  };

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return '⚠️';
      case 'WARNING': return '⚡';
      case 'INFO': return 'ℹ️';
      default: return '📋';
    }
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  return (
    <div className="ab-warnings-panel">
      <div className="ab-toolbar">
        <h3 className="ab-section-title">Warnings ({warnings?.length || 0})</h3>
      </div>

      {warnings && warnings.length > 0 ? (
        <div className="ab-warnings-list">
          {warnings.map((warning) => (
            <div key={warning.id} className={`ab-card ab-warning-card ab-warning-${warning.severity?.toLowerCase()}`}>
              <div className="ab-warning-icon">{getSeverityIcon(warning.severity)}</div>
              <div className="ab-warning-content">
                <div className="ab-warning-header">
                  <span className={`ab-badge ${getSeverityClass(warning.severity)}`}>
                    {warning.severity}
                  </span>
                  <span className="ab-warning-type">{warning.warningType}</span>
                </div>
                <p className="ab-warning-message">{warning.message || 'No message'}</p>
                <div className="ab-warning-meta">
                  <span className="ab-warning-issue">
                    Issue: {warning.issueKey || warning.issueId}
                  </span>
                  <span className="ab-warning-date">
                    {new Date(warning.createdAt).toLocaleString()}
                  </span>
                </div>
              </div>
              <button className="ab-btn ab-btn-sm" onClick={() => handleDismiss(warning.id)}>
                Dismiss
              </button>
            </div>
          ))}
        </div>
      ) : (
        <div className="ab-empty-state">
          <div className="ab-empty-state-icon">✅</div>
          <h3 className="ab-empty-state-title">No warnings</h3>
          <p className="ab-empty-state-description">
            Your plan is looking good! No warnings at this time.
          </p>
        </div>
      )}
    </div>
  );
}
