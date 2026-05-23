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

async function loadCustomFields(
  issueId: string,
  projectId?: string,
  issueTypeId?: string,
): Promise<VisibleFieldDto[]> {
  try {
    const visible = await fieldApi.getVisibleIssueFields(issueId, {
      screen: 'VIEW',
      projectId,
      issueTypeId,
    });
    return visible.data.fields ?? [];
  } catch {
    const values = await fieldApi.getIssueFieldValues(issueId);
    const custom = values.data.customFields ?? {};
    const names = new Map(
      (values.data.allFieldValues ?? []).map((v) => [v.fieldKey, v.fieldDisplayName]),
    );
    return Object.entries(custom).map(([fieldKey, value]) => ({
      fieldKey,
      displayName: names.get(fieldKey) ?? fieldKey,
      value,
    }));
  }
}

export default function IssueCustomFieldsPanel({
  issueId,
  issueKey,
  projectId,
  issueTypeId,
  variant = 'inline',
}: IssueCustomFieldsPanelProps) {
  const { data: fields = [], isLoading, isError, error } = useQuery({
    queryKey: ['issue-custom-fields', issueId, projectId, issueTypeId],
    queryFn: () => loadCustomFields(issueId, projectId, issueTypeId),
    enabled: !!issueId,
    retry: 1,
  });

  const withValues = fields.filter((f) => f.value != null && String(f.value).trim() !== '');

  if (isLoading) {
    return (
      <p className={`icf-muted icf-${variant}`} data-testid="issue-custom-fields-loading">
        Loading custom fields…
      </p>
    );
  }

  if (isError) {
    const status = (error as { response?: { status?: number } })?.response?.status;
    const isNetwork =
      !(error as { response?: unknown }).response &&
      (error as { code?: string }).code === 'ERR_NETWORK';
    const msg = isNetwork
      ? 'Cannot reach migration-service. Start jira-migration-service on port 8094 (or gateway on 8080 with /api/fields routed).'
      : status === 404
        ? 'Issue not found for custom field lookup.'
        : `Custom fields request failed (${status ?? 'error'}). Check migration-service logs.`;
    return (
      <div className={`icf-error icf-${variant}`} data-testid="issue-custom-fields-error">
        <p>{msg}</p>
        {issueKey && (
          <p className="icf-hint">
            Issue <strong>{issueKey}</strong> (id: {issueId.slice(0, 8)}…).
          </p>
        )}
      </div>
    );
  }

  if (fields.length === 0) {
    return (
      <div className={`icf-empty icf-${variant}`} data-testid="issue-custom-fields-empty">
        <p className="icf-muted">No custom field values for this issue.</p>
        <p className="icf-hint">
          Re-import CSV with custom columns, or map fields under Admin → Custom fields. Values are stored in
          migration-service, not on the core issue record.
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
