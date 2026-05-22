import { useQuery } from '@tanstack/react-query';
import { fieldApi } from '../../../api/fieldApi';

interface Props {
  issueId: string;
}

export default function IssueMigratedFieldsPanel({ issueId }: Props) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['issue-field-values', issueId],
    queryFn: () => fieldApi.getIssueFieldValues(issueId).then((r) => r.data),
    enabled: !!issueId,
  });

  if (isLoading) {
    return <p className="text-sm text-gray-500">Loading migrated custom fields…</p>;
  }
  if (isError) {
    return (
      <p className="text-sm text-amber-700">
        Custom field values unavailable (migration-service may be offline).
      </p>
    );
  }

  const custom = (data?.customFields ?? {}) as Record<string, unknown>;
  const entries = Object.entries(custom);

  if (entries.length === 0) {
    return (
      <p className="text-sm text-gray-500" data-testid="issue-migrated-fields-empty">
        No custom field values from migration import for this issue.
      </p>
    );
  }

  return (
    <div className="idc-details-grid" data-testid="issue-migrated-fields">
      {entries.map(([key, value]) => (
        <div key={key} className="idc-detail-item">
          <span className="idc-detail-label">{key}</span>
          <span className="idc-detail-value">
            {value == null ? '—' : Array.isArray(value) ? value.join(', ') : String(value)}
          </span>
        </div>
      ))}
    </div>
  );
}
