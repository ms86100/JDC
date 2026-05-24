import { useEffect, useMemo, useState } from 'react';
import { resolutionApi, type Resolution } from '../../../api/issueApi';
import { useQuery } from '@tanstack/react-query';
import './TransitionScreenForm.css';

export interface TransitionScreenField {
  fieldId: string;
  fieldName: string;
  fieldType?: string;
  required: boolean;
  defaultValue?: string;
}

export interface AvailableTransition {
  id: string;
  name: string;
  description?: string;
  toStatusId: string;
  /** Resolved target status name from workflow / issue-service fallback */
  toStatusName?: string;
  hasScreen?: boolean;
  screenFields?: TransitionScreenField[];
}

interface TransitionScreenFormProps {
  transition: AvailableTransition;
  comment: string;
  onCommentChange: (value: string) => void;
  screenInput: Record<string, unknown>;
  onScreenInputChange: (value: Record<string, unknown>) => void;
  onConfirm: () => void;
  onCancel: () => void;
  isSubmitting?: boolean;
}

function normalizeFieldKey(name: string): string {
  return name.toLowerCase().replace(/\s+/g, '');
}

function isCommentField(field: TransitionScreenField): boolean {
  const key = normalizeFieldKey(field.fieldName);
  return key === 'comment' || field.fieldType === 'comment';
}

function isResolutionField(field: TransitionScreenField): boolean {
  const key = normalizeFieldKey(field.fieldName);
  return key === 'resolution' || key === 'resolutionid' || field.fieldType === 'resolution';
}

function isAssigneeField(field: TransitionScreenField): boolean {
  const key = normalizeFieldKey(field.fieldName);
  return key === 'assignee' || key === 'assigneeid' || field.fieldType === 'user';
}

export default function TransitionScreenForm({
  transition,
  comment,
  onCommentChange,
  screenInput,
  onScreenInputChange,
  onConfirm,
  onCancel,
  isSubmitting,
}: TransitionScreenFormProps) {
  const { data: resolutions } = useQuery({
    queryKey: ['resolutions'],
    queryFn: () => resolutionApi.getAll().then((r) => r.data),
  });

  const fields = useMemo(() => {
    const configured = transition.screenFields ?? [];
    if (configured.length > 0) {
      return configured;
    }
    if (transition.hasScreen) {
      return [
        { fieldId: 'comment', fieldName: 'comment', required: false },
        { fieldId: 'resolutionId', fieldName: 'resolution', required: false },
      ] as TransitionScreenField[];
    }
    return [{ fieldId: 'comment', fieldName: 'comment', required: false }] as TransitionScreenField[];
  }, [transition]);

  const [localScreen, setLocalScreen] = useState<Record<string, unknown>>(screenInput);

  useEffect(() => {
    setLocalScreen(screenInput);
  }, [transition.id, screenInput]);

  const updateField = (field: TransitionScreenField, value: unknown) => {
    const key = field.fieldId || field.fieldName;
    const next = { ...localScreen, [key]: value, [field.fieldName]: value };
    setLocalScreen(next);
    onScreenInputChange(next);
    if (isCommentField(field) && typeof value === 'string') {
      onCommentChange(value);
    }
  };

  const showDefaultComment = !fields.some(isCommentField);

  return (
    <div className="transition-screen-form">
      <div className="transition-screen-header">
        <strong>{transition.name}</strong>
        {transition.description && <p className="transition-screen-desc">{transition.description}</p>}
      </div>

      {fields.map((field) => {
        if (isCommentField(field)) {
          return (
            <label key={field.fieldId} className="transition-screen-field">
              <span>Comment{field.required ? ' *' : ''}</span>
              <textarea
                rows={3}
                value={(localScreen[field.fieldName] as string) ?? comment}
                onChange={(e) => updateField(field, e.target.value)}
                placeholder="Add a comment for this transition"
              />
            </label>
          );
        }

        if (isResolutionField(field)) {
          return (
            <label key={field.fieldId} className="transition-screen-field">
              <span>Resolution{field.required ? ' *' : ''}</span>
              <select
                value={String(localScreen.resolutionId ?? localScreen[field.fieldName] ?? '')}
                onChange={(e) => updateField(field, e.target.value)}
              >
                <option value="">Select resolution</option>
                {(resolutions ?? []).map((r: Resolution) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </label>
          );
        }

        if (isAssigneeField(field)) {
          return (
            <label key={field.fieldId} className="transition-screen-field">
              <span>Assignee{field.required ? ' *' : ''}</span>
              <input
                type="text"
                placeholder="User ID (UUID)"
                value={String(localScreen.assigneeId ?? localScreen[field.fieldName] ?? '')}
                onChange={(e) => updateField(field, e.target.value)}
              />
            </label>
          );
        }

        return (
          <label key={field.fieldId} className="transition-screen-field">
            <span>
              {field.fieldName}
              {field.required ? ' *' : ''}
            </span>
            <input
              type="text"
              value={String(localScreen[field.fieldName] ?? '')}
              onChange={(e) => updateField(field, e.target.value)}
            />
          </label>
        );
      })}

      {showDefaultComment && (
        <label className="transition-screen-field">
          <span>Comment (optional)</span>
          <textarea
            rows={2}
            value={comment}
            onChange={(e) => onCommentChange(e.target.value)}
            placeholder="Comment for this transition"
          />
        </label>
      )}

      <div className="transition-screen-actions">
        <button type="button" className="idc-btn idc-btn-primary" disabled={isSubmitting} onClick={onConfirm}>
          {isSubmitting ? 'Transitioning…' : 'Confirm transition'}
        </button>
        <button type="button" className="idc-btn idc-btn-secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </div>
  );
}
