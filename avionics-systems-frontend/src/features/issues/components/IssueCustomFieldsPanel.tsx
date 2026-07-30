import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fieldApi, VisibleFieldDto } from '../../../api/fieldApi';
import apiClient from '../../../api/axiosClient';
import './IssueCustomFieldsPanel.css';

export interface IssueCustomFieldsPanelProps {
  issueId: string;
  issueKey?: string;
  projectId?: string;
  issueTypeId?: string;
  variant?: 'sidebar' | 'inline';
}

function isEmpty(value: unknown): boolean {
  if (value == null) return true;
  if (typeof value === 'string' && value.trim() === '') return true;
  if (Array.isArray(value) && value.length === 0) return true;
  return false;
}

function formatValue(value: unknown): string {
  if (isEmpty(value)) return 'None';
  if (Array.isArray(value)) return value.join(', ');
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

async function loadFromValues(issueId: string): Promise<VisibleFieldDto[]> {
  const values = await fieldApi.getIssueFieldValues(issueId);
  const custom = values.data.customFields ?? {};
  const names = new Map(
    (values.data.allFieldValues ?? []).map((v: { fieldKey: string; fieldDisplayName?: string; displayName?: string }) => [v.fieldKey, v.fieldDisplayName ?? v.displayName]),
  );
  return Object.entries(custom).map(([fieldKey, value]) => ({
    fieldKey,
    displayName: names.get(fieldKey) ?? fieldKey.replace(/^customfield_/, '').replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase()),
    value,
  }));
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
    const fields = visible.data.fields ?? [];
    if (fields.length > 0) return fields;
    return await loadFromValues(issueId);
  } catch {
    return await loadFromValues(issueId);
  }
}

async function evaluateCalculatedField(
  issueId: string,
  fieldId: string,
): Promise<{ hasScript: boolean; success?: boolean; value?: unknown } | null> {
  try {
    const res = await apiClient.get<{ hasScript: boolean; success?: boolean; value?: unknown }>(
      '/api/workflow/scripts/calculated-fields/evaluate',
      { params: { issueId, fieldId } },
    );
    return res.data;
  } catch {
    return null;
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

  const [calculatedValues, setCalculatedValues] = useState<Map<string, unknown>>(new Map());

  useEffect(() => {
    if (!issueId || fields.length === 0) return;

    const loadCalculated = async () => {
      const overrides = new Map<string, unknown>();
      for (const field of fields) {
        const rawField = field as unknown as { id?: string };
        const fieldId = rawField.id;
        if (!fieldId || !fieldId.match(/^[0-9a-f-]{36}$/i)) continue;

        const result = await evaluateCalculatedField(issueId, fieldId);
        if (result && result.hasScript && result.success && result.value !== undefined) {
          overrides.set(field.fieldKey, result.value);
        }
      }
      if (overrides.size > 0) {
        setCalculatedValues(overrides);
      }
    };

    loadCalculated();
  }, [issueId, fields]);

  const getDisplayValue = (f: VisibleFieldDto): { value: unknown; isCalculated: boolean } => {
    if (calculatedValues.has(f.fieldKey)) {
      return { value: calculatedValues.get(f.fieldKey), isCalculated: true };
    }
    return { value: f.value, isCalculated: false };
  };

  const withValues = fields.filter((f) => !isEmpty(f.value) || calculatedValues.has(f.fieldKey));

  if (isLoading) {
    return (
      <p className="icf-status-msg" data-testid="issue-custom-fields-loading">
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
      ? 'Cannot reach migration-service. Start avionics-systems-migration-service on port 8094 (or gateway on 8080 with /api/fields routed).'
      : status === 404
        ? 'Issue not found for custom field lookup.'
        : `Custom fields request failed (${status ?? 'error'}). Check migration-service logs.`;
    return (
      <div className="icf-error" data-testid="issue-custom-fields-error">
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
      <div className="icf-empty-state" data-testid="issue-custom-fields-empty">
        <p className="icf-status-msg">No custom fields for this issue.</p>
      </div>
    );
  }

  if (variant === 'sidebar') {
    return (
      <div data-testid="issue-custom-fields">
        {fields.map((f) => {
          const { value: displayVal, isCalculated } = getDisplayValue(f);
          return (
            <div key={f.fieldKey} className="idc-sidebar-item">
              <span className="idc-sidebar-label">
                {f.displayName || f.fieldKey}
                {isCalculated && <span title="Calculated by script" style={{ marginLeft: 4, color: '#6366f1', fontSize: '0.7rem' }}>&#9889;</span>}
              </span>
              <div className="idc-sidebar-value">
                {isEmpty(displayVal) ? (
                  <span className="idc-no-value">None</span>
                ) : (
                  <span>{formatValue(displayVal)}</span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  return (
    <div data-testid="issue-custom-fields">
      <p className="icf-count">
        {withValues.length} of {fields.length} field{fields.length !== 1 ? 's' : ''} with values
      </p>
      <div className="idc-details-grid">
        {fields.map((f) => {
          const { value: displayVal, isCalculated } = getDisplayValue(f);
          return (
            <div key={f.fieldKey} className="idc-detail-item">
              <span className="idc-detail-label">
                {f.displayName || f.fieldKey}
                {isCalculated && <span title="Calculated by script" style={{ marginLeft: 4, color: '#6366f1', fontSize: '0.7rem' }}>&#9889;</span>}
              </span>
              <span className="idc-detail-value">
                {isEmpty(displayVal) ? (
                  <span className="idc-no-value">None</span>
                ) : (
                  formatValue(displayVal)
                )}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
