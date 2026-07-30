import React from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicNumberFieldProps {
  fieldDef: FieldDefinition;
  value?: number;
  onChange?: (value: number) => void;
  readOnly?: boolean;
  placeholder?: string;
}

export default function DynamicNumberField({
  fieldDef,
  value,
  onChange,
  readOnly = false,
  placeholder
}: DynamicNumberFieldProps) {
  const min = fieldDef.schemaDefinition?.min || 0;
  const max = fieldDef.schemaDefinition?.max || 10000;

  if (readOnly) {
    return (
      <span className="dynamic-number">
        {value !== undefined && value !== null ? value : <span className="no-value">-</span>}
      </span>
    );
  }

  return (
    <input
      type="number"
      className="dynamic-number-input"
      value={value ?? ''}
      onChange={(e) => {
        const num = parseFloat(e.target.value);
        if (!isNaN(num)) {
          onChange?.(num);
        } else if (e.target.value === '') {
          onChange?.(0);
        }
      }}
      min={min}
      max={max}
      placeholder={placeholder}
    />
  );
}