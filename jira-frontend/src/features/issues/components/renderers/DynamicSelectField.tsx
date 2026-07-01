import React from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicSelectFieldProps {
  fieldDef: FieldDefinition;
  value?: string;
  onChange?: (value: string) => void;
  readOnly?: boolean;
  placeholder?: string;
}

export default function DynamicSelectField({
  fieldDef,
  value,
  onChange,
  readOnly = false,
  placeholder = 'Select...'
}: DynamicSelectFieldProps) {
  const options = fieldDef.options || [];

  const selectedOption = options.find(opt => opt.value === value || opt.label === value);

  if (readOnly) {
    return (
      <span className="dynamic-select-view">
        {selectedOption?.label || value || <span className="no-value">None</span>}
      </span>
    );
  }

  return (
    <select
      className="dynamic-select"
      value={value || ''}
      onChange={(e) => onChange?.(e.target.value)}
    >
      <option value="">{placeholder}</option>
      {options.map((opt) => (
        <option
          key={opt.value}
          value={opt.value}
          disabled={opt.disabled}
        >
          {opt.label}
        </option>
      ))}
    </select>
  );
}