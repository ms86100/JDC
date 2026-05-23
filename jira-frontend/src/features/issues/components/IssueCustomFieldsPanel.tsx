import { useQuery } from '@tanstack/react-query';
import { fieldApi, VisibleFieldDto } from '../../../api/fieldApi';
import './IssueCustomFieldsPanel.css';

export interface IssueCustomFieldsPanelProps {
  /** Issue UUID (required for API calls) */
  issueId: string;
  issueKey?: string;
  projectId?: string;
  issueTypeId?: string;
  /** sidebar = right column; inline = Details tab body */
  variant?: 'sidebar' | 'inline';
}

function formatValue(value: unknown): string {
  if (value == null) return '—';
  if (Array.isArray(value)) return value.join(', ');
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

export default function IssueCustomFieldsPanel({
  issueId,
  issueKey,
  projectId,
  issueTypeId,
  variant = 'inline',
}: IssueCustomFieldsPanelProps) {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['issue-visible-fields', issueId, projectId, issueTypeId],
    queryFn: () =>
      fieldApi
        .getVisibleIssueFields(issueId, {
          screen: 'VIEW',
          projectId,
          issueTypeId,
        })
        .then((r) => r.data),
    enabled: !!issueId,
    retry: 1,
  });

  const fields: VisibleFieldDto[] = data?.fields ?? [];
  const withValues = fields.filter((f) => f.value != null && String(f.value).trim() !== '');

  if (isLoading) {
    return (
      <p className={`icf-muted icf-${variant}`} data-testid="issue-custom-fields-loading">
        Loading custom fields…
      </p>
    );
  }

  if (isError) {
    const msg =
      (error as { response?: { status?: number } })?.response?.status === 404
        ? 'Issue not found for custom field lookup.'
        : 'Custom fields unavailable. Ensure migration-service is running on port 8094.';
    return (
      <div className={`icf-error icf-${variant}`} data-testid="issue-custom-fields-error">
        <p>{msg}</p>
        {issueKey && (
          <p className="icf-hint">
            Tip: values are loaded for issue <strong>{issueKey}</strong> (id: {issueId.slice(0, 8)}…).
          </p>
        )}
      </div>
    );
  }

  if (fields.length === 0) {
    return (
      <div className={`icf-empty icf-${variant}`} data-testid="issue-custom-fields-empty">
        <p className="icf-muted">No custom fields visible for this issue.</p>
        <p className="icf-hint">
          Import CSV columns via Migration, or add fields under Admin → Custom fields and map them to
          this project&apos;s screens.
        </p>
      </div>
    );
  }

  const grid = (
    <div
      className={variant === 'sidebar' ? 'icf-sidebar-grid' : 'idc-details-grid'}
      data-testid="issue-custom-fields"
    >
      {fields.map((f) => (
        <div
          key={f.fieldKey}
          className={variant === 'sidebar' ? 'icf-sidebar-item' : 'idc-detail-item'}
        >
          <span className={variant === 'sidebar' ? 'icf-label' : 'idc-detail-label'}>
            {f.displayName || f.fieldKey}
          </span>
          <span className={variant === 'sidebar' ? 'icf-value' : 'idc-detail-value'}>
            {formatValue(f.value)}
          </span>
        </div>
      ))}
    </div>
  );

  return (
    <div className={`icf-panel icf-${variant}`}>
      {variant === 'inline' && (
        <p className="icf-count">
          {withValues.length} of {fields.length} field{fields.length !== 1 ? 's' : ''} with values
        </p>
      )}
      {grid}
    </div>
  );
}
