import React, { useState } from 'react';
import { ALL_CONFIGURABLE_FIELDS } from './EditIssueModal';
import './ConfigureFieldsPopover.css';

interface ConfigureFieldsPopoverProps {
  visibleFields: Set<string>;
  onApply: (fields: Set<string>) => void;
  onCancel: () => void;
}

type FilterType = 'all' | 'system' | 'custom';

export default function ConfigureFieldsPopover({ visibleFields, onApply, onCancel }: ConfigureFieldsPopoverProps) {
  const [filter, setFilter] = useState<FilterType>('all');
  const [selected, setSelected] = useState<Set<string>>(new Set(visibleFields));

  const filteredFields = ALL_CONFIGURABLE_FIELDS.filter((f) => {
    if (filter === 'all') return true;
    return f.type === filter;
  });

  const toggleField = (fieldId: string) => {
    const next = new Set(selected);
    if (next.has(fieldId)) {
      next.delete(fieldId);
    } else {
      next.add(fieldId);
    }
    setSelected(next);
  };

  const selectAll = () => {
    setSelected(new Set(ALL_CONFIGURABLE_FIELDS.map((f) => f.id)));
  };

  const selectNone = () => {
    setSelected(new Set());
  };

  return (
    <div className="cf-popover">
      {/* Header */}
      <div className="cf-header">
        <span className="cf-title">Configure Fields</span>
        <a href="#" className="cf-help-link">Where is my field?</a>
      </div>

      {/* Toolbar */}
      <div className="cf-toolbar">
        <div className="cf-show-dropdown">
          <label className="cf-show-label">Show:</label>
          <select
            className="cf-select"
            value={filter}
            onChange={(e) => setFilter(e.target.value as FilterType)}
          >
            <option value="all">All Fields</option>
            <option value="system">System Fields</option>
            <option value="custom">Custom Fields</option>
          </select>
        </div>
        <div className="cf-select-actions">
          <button className="cf-select-action" onClick={selectAll}>Select All</button>
          <button className="cf-select-action" onClick={selectNone}>Select None</button>
        </div>
      </div>

      {/* Field Grid */}
      <div className="cf-body">
        <div className="cf-grid">
          {filteredFields.map((field) => (
            <label key={field.id} className="cf-field-item">
              <input
                type="checkbox"
                className="cf-checkbox"
                checked={selected.has(field.id)}
                onChange={() => toggleField(field.id)}
              />
              <span className="cf-field-label">{field.label}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Footer */}
      <div className="cf-footer">
        <button
          className="cf-btn cf-btn-secondary"
          onClick={onCancel}
        >
          Cancel
        </button>
        <button
          className="cf-btn cf-btn-primary"
          onClick={() => onApply(selected)}
        >
          Apply changes
        </button>
      </div>
    </div>
  );
}