import React, { useState } from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicDateTimePickerProps {
  fieldDef: FieldDefinition;
  value?: string;
  onChange?: (value: string) => void;
  readOnly?: boolean;
}

export default function DynamicDateTimePicker({
  fieldDef,
  value,
  onChange,
  readOnly = false
}: DynamicDateTimePickerProps) {
  const isDateOnly = fieldDef.fieldType === 'DATE';

  const formatValue = () => {
    if (!value) return '';
    try {
      const date = new Date(value);
      if (isDateOnly) {
        return date.toISOString().split('T')[0];
      }
      return date.toISOString().slice(0, 16);
    } catch {
      return value;
    }
  };

  const parseValue = (input: string) => {
    if (!input) return '';
    try {
      const date = new Date(input);
      return date.toISOString();
    } catch {
      return input;
    }
  };

  if (readOnly) {
    if (!value) return <span className="no-value">-</span>;
    const date = new Date(value);
    return (
      <span className="dynamic-datetime-view">
        {date.toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
          ...(isDateOnly ? {} : { hour: 'numeric', minute: '2-digit' })
        })}
      </span>
    );
  }

  return (
    <input
      type={isDateOnly ? 'date' : 'datetime-local'}
      className="dynamic-datetime-input"
      value={formatValue()}
      onChange={(e) => onChange?.(parseValue(e.target.value))}
    />
  );
}