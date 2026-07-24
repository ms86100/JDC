import { useEffect, useState } from 'react';
import apiClient from '../api/axiosClient';

export interface FieldDirective {
  fieldName: string;
  visible?: boolean;
  required?: boolean;
  readOnly?: boolean;
  defaultValue?: unknown;
  options?: Array<{ value: string; label: string }>;
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

  return {
    directives,
    loading,
    isFieldVisible,
    isFieldRequired,
    isFieldReadOnly,
    getFieldDefault,
    getFieldOptions,
  };
}
