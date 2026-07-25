import { useCallback, useState } from 'react';

export interface LiveFieldDirective {
  fieldName: string;
  visible?: boolean;
  required?: boolean;
  readOnly?: boolean;
  description?: string;
  message?: string;
  cssClass?: string;
  defaultValue?: unknown;
  options?: Array<{ value: string; label: string }>;
  label?: string;
}

/**
 * Client-side Live Fields engine — provides SIL-equivalent functions
 * (makeRequired, hideField, showField, etc.) that scripts can call
 * via the field behavior evaluation API. The directives returned by
 * the server are applied here on the client side.
 *
 * This is the browser-side complement to the server-side
 * useFieldBehaviors hook. While useFieldBehaviors evaluates scripts
 * on the server and returns directives, useLiveFields provides
 * imperative functions that mirror SIL's Live Fields API.
 */
export function useLiveFields() {
  const [directives, setDirectives] = useState<Map<string, LiveFieldDirective>>(new Map());

  const getOrCreate = useCallback((fieldName: string): LiveFieldDirective => {
    const existing = directives.get(fieldName);
    if (existing) return existing;
    return { fieldName };
  }, [directives]);

  const updateDirective = useCallback((fieldName: string, updates: Partial<LiveFieldDirective>) => {
    setDirectives(prev => {
      const next = new Map(prev);
      const existing = next.get(fieldName) || { fieldName };
      next.set(fieldName, { ...existing, ...updates });
      return next;
    });
  }, []);

  const makeRequired = useCallback((fieldName: string) => {
    updateDirective(fieldName, { required: true });
  }, [updateDirective]);

  const makeOptional = useCallback((fieldName: string) => {
    updateDirective(fieldName, { required: false });
  }, [updateDirective]);

  const makeReadOnly = useCallback((fieldName: string) => {
    updateDirective(fieldName, { readOnly: true });
  }, [updateDirective]);

  const makeEditable = useCallback((fieldName: string) => {
    updateDirective(fieldName, { readOnly: false });
  }, [updateDirective]);

  const hideField = useCallback((fieldName: string) => {
    updateDirective(fieldName, { visible: false });
  }, [updateDirective]);

  const showField = useCallback((fieldName: string) => {
    updateDirective(fieldName, { visible: true });
  }, [updateDirective]);

  const setFieldOptions = useCallback((fieldName: string, options: Array<{ value: string; label: string }>) => {
    updateDirective(fieldName, { options });
  }, [updateDirective]);

  const setFieldDescription = useCallback((fieldName: string, description: string) => {
    updateDirective(fieldName, { description });
  }, [updateDirective]);

  const setMessage = useCallback((fieldName: string, message: string) => {
    updateDirective(fieldName, { message });
  }, [updateDirective]);

  const setFieldCssClass = useCallback((fieldName: string, cssClass: string) => {
    updateDirective(fieldName, { cssClass });
  }, [updateDirective]);

  const setFieldDefault = useCallback((fieldName: string, defaultValue: unknown) => {
    updateDirective(fieldName, { defaultValue });
  }, [updateDirective]);

  const setFieldLabel = useCallback((fieldName: string, label: string) => {
    updateDirective(fieldName, { label });
  }, [updateDirective]);

  const applyServerDirectives = useCallback((serverDirectives: LiveFieldDirective[]) => {
    setDirectives(prev => {
      const next = new Map(prev);
      for (const d of serverDirectives) {
        const existing = next.get(d.fieldName) || { fieldName: d.fieldName };
        next.set(d.fieldName, { ...existing, ...d });
      }
      return next;
    });
  }, []);

  const isFieldVisible = useCallback((fieldName: string): boolean => {
    const d = directives.get(fieldName);
    return d?.visible !== false;
  }, [directives]);

  const isFieldRequired = useCallback((fieldName: string): boolean => {
    const d = directives.get(fieldName);
    return d?.required === true;
  }, [directives]);

  const isFieldReadOnly = useCallback((fieldName: string): boolean => {
    const d = directives.get(fieldName);
    return d?.readOnly === true;
  }, [directives]);

  const getFieldDescription = useCallback((fieldName: string): string | undefined => {
    return directives.get(fieldName)?.description;
  }, [directives]);

  const getFieldMessage = useCallback((fieldName: string): string | undefined => {
    return directives.get(fieldName)?.message;
  }, [directives]);

  const getFieldCssClass = useCallback((fieldName: string): string | undefined => {
    return directives.get(fieldName)?.cssClass;
  }, [directives]);

  const getFieldDefault = useCallback((fieldName: string): unknown => {
    return directives.get(fieldName)?.defaultValue;
  }, [directives]);

  const getFieldOptions = useCallback((fieldName: string): Array<{ value: string; label: string }> | undefined => {
    return directives.get(fieldName)?.options;
  }, [directives]);

  const getFieldLabel = useCallback((fieldName: string): string | undefined => {
    return directives.get(fieldName)?.label;
  }, [directives]);

  const clearAll = useCallback(() => {
    setDirectives(new Map());
  }, []);

  return {
    makeRequired,
    makeOptional,
    makeReadOnly,
    makeEditable,
    hideField,
    showField,
    setFieldOptions,
    setFieldDescription,
    setMessage,
    setFieldCssClass,
    setFieldDefault,
    setFieldLabel,
    applyServerDirectives,
    isFieldVisible,
    isFieldRequired,
    isFieldReadOnly,
    getFieldDescription,
    getFieldMessage,
    getFieldCssClass,
    getFieldDefault,
    getFieldOptions,
    getFieldLabel,
    clearAll,
    directives,
  };
}
