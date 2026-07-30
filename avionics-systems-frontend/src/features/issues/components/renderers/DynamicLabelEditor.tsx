import React, { useState } from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicLabelEditorProps {
  fieldDef: FieldDefinition;
  value?: string[];
  onChange?: (value: string[]) => void;
  readOnly?: boolean;
}

export default function DynamicLabelEditor({
  fieldDef,
  value = [],
  onChange,
  readOnly = false
}: DynamicLabelEditorProps) {
  const [inputValue, setInputValue] = useState('');
  const [isOpen, setIsOpen] = useState(false);

  const addLabel = (label: string) => {
    const trimmed = label.trim().toLowerCase();
    if (trimmed && !value.includes(trimmed)) {
      onChange?.([...value, trimmed]);
    }
    setInputValue('');
    setIsOpen(false);
  };

  const removeLabel = (label: string) => {
    onChange?.(value.filter(l => l !== label));
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && inputValue.trim()) {
      e.preventDefault();
      addLabel(inputValue);
    }
  };

  if (readOnly) {
    if (value.length === 0) {
      return <span className="no-value">None</span>;
    }
    return (
      <div className="dynamic-labels-view">
        {value.map((label) => (
          <span key={label} className="dynamic-label-tag">
            {label}
          </span>
        ))}
      </div>
    );
  }

  return (
    <div className="dynamic-label-editor">
      <div className="dynamic-labels-container">
        {value.map((label) => (
          <span key={label} className="dynamic-label-tag">
            {label}
            <button
              type="button"
              className="dynamic-label-remove"
              onClick={() => removeLabel(label)}
            >
              ×
            </button>
          </span>
        ))}

        <input
          type="text"
          className="dynamic-label-input"
          placeholder={value.length === 0 ? 'Add labels...' : ''}
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => setIsOpen(true)}
        />
      </div>

      {isOpen && inputValue && (
        <div className="dynamic-label-suggestions">
          <button
            type="button"
            className="dynamic-label-suggestion"
            onClick={() => addLabel(inputValue)}
          >
            Create "{inputValue}"
          </button>
        </div>
      )}
    </div>
  );
}