import React, { useState } from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicMultiSelectProps {
  fieldDef: FieldDefinition;
  value?: string[];
  onChange?: (value: string[]) => void;
  readOnly?: boolean;
  placeholder?: string;
}

export default function DynamicMultiSelect({
  fieldDef,
  value = [],
  onChange,
  readOnly = false,
  placeholder = 'Select...'
}: DynamicMultiSelectProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const options = fieldDef.options || [];

  const filteredOptions = options.filter(opt =>
    opt.label.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const toggleOption = (optValue: string) => {
    if (value.includes(optValue)) {
      onChange?.(value.filter(v => v !== optValue));
    } else {
      onChange?.([...value, optValue]);
    }
  };

  const removeValue = (optValue: string) => {
    onChange?.(value.filter(v => v !== optValue));
  };

  const getLabel = (optValue: string) => {
    const opt = options.find(o => o.value === optValue);
    return opt?.label || optValue;
  };

  if (readOnly) {
    if (value.length === 0) {
      return <span className="no-value">None</span>;
    }
    return (
      <div className="dynamic-multi-view">
        {value.map((v) => (
          <span key={v} className="dynamic-multi-tag">
            {getLabel(v)}
          </span>
        ))}
      </div>
    );
  }

  return (
    <div className="dynamic-multi-select">
      {value.length > 0 && (
        <div className="dynamic-multi-selected">
          {value.map((v) => (
            <span key={v} className="dynamic-multi-tag">
              {getLabel(v)}
              <button
                className="dynamic-multi-remove"
                onClick={() => removeValue(v)}
                type="button"
              >
                ×
              </button>
            </span>
          ))}
        </div>
      )}

      <div className="dynamic-multi-dropdown">
        <input
          type="text"
          className="dynamic-multi-search"
          placeholder={placeholder}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          onFocus={() => setIsOpen(true)}
        />

        {isOpen && (
          <div className="dynamic-multi-options">
            {filteredOptions.length === 0 ? (
              <div className="dynamic-multi-empty">No options found</div>
            ) : (
              filteredOptions.map((opt) => (
                <div
                  key={opt.value}
                  className={`dynamic-multi-option ${value.includes(opt.value) ? 'selected' : ''}`}
                  onClick={() => toggleOption(opt.value)}
                >
                  <span className="dynamic-multi-check">
                    {value.includes(opt.value) ? '✓' : ''}
                  </span>
                  <span>{opt.label}</span>
                </div>
              ))
            )}
          </div>
        )}
      </div>

      {!isOpen && value.length === 0 && (
        <input
          type="text"
          className="dynamic-multi-search"
          placeholder={placeholder}
          onFocus={() => setIsOpen(true)}
          readOnly
        />
      )}
    </div>
  );
}