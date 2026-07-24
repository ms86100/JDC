import { useEffect, useState } from 'react';
import apiClient from '../api/axiosClient';

export interface FieldDirective {
  fieldName: string;
  visible?: boolean;
  required?: boolean;
  readOnly?: boolean;
  defaultValue?: unknown;
  options?: Array<{ value: string; label: string }>;
  warning?: string;
  label?: string;
  helpText?: string;
}

interface UseFieldBehaviorsProps {
  screenContext: 'CREATE' | 'EDIT' | 'TRANSITION' | 'VIEW';
  projectId?: string;
  issueTypeId?: string;
  issueData?: Record<string, unknown>;
  userId?: string;
  enabled?: boolean;
}

export function useFieldBehaviors({
  screenContext,
  projectId,
  issueTypeId,
  issueData,
  userId,
  enabled = true,
}: UseFieldBehaviorsProps) {
  const [directives, setDirectives] = useState<FieldDirective[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!enabled) return;

    const evaluate = async () => {
      setLoading(true);
      try {
        const res = await apiClient.post<{ fields: FieldDirective[] }>(
          '/api/workflow/scripts/field-behaviors/evaluate',
          {
            screenContext,
            projectId,
            issueTypeId,
            issueData: issueData || {},
            userId,
          }
        );
        setDirectives(res.data.fields || []);
      } catch {
        setDirectives([]);
      } finally {
        setLoading(false);
      }
    };

    evaluate();
  }, [screenContext, projectId, issueTypeId, userId, enabled]);

  const isFieldVisible = (fieldName: string): boolean => {
    const d = directives.find((f) => f.fieldName === fieldName);
    return d?.visible !== false;
  };

  const isFieldRequired = (fieldName: string): boolean => {
    const d = directives.find((f) => f.fieldName === fieldName);
    return d?.required === true;
  };

  const isFieldReadOnly = (fieldName: string): boolean => {
    const d = directives.find((f) => f.fieldName === fieldName);
    return d?.readOnly === true;
  };

  const getFieldDefault = (fieldName: string): unknown => {
    const d = directives.find((f) => f.fieldName === fieldName);
    return d?.defaultValue;
  };

  const getFieldOptions = (fieldName: string): Array<{ value: string; label: string }> | undefined => {
    const d = directives.find((f) => f.fieldName === fieldName);
    return d?.options;
  };

  const getFieldWarning = (fieldName: string): string | undefined => {
    const d = directives.find((f) => f.fieldName === fieldName);
    return d?.warning;
  };

  const getFieldLabel = (fieldName: string): string | undefined => {
    const d = directives.find((f) => f.fieldName === fieldName);
    return d?.label;
  };

  const getFieldHelpText = (fieldName: string): string | undefined => {
    const d = directives.find((f) => f.fieldName === fieldName);
    return d?.helpText;
  };

  return {
    directives,
    loading,
    isFieldVisible,
    isFieldRequired,
    isFieldReadOnly,
    getFieldDefault,
    getFieldOptions,
    getFieldWarning,
    getFieldLabel,
    getFieldHelpText,
  };
}
