import React, { useState } from 'react';
import { FieldDefinition } from '../../context/FieldDefinitionContext';

interface DynamicDurationFieldProps {
  fieldDef: FieldDefinition;
  value?: number;
  onChange?: (value: number) => void;
  readOnly?: boolean;
}

export default function DynamicDurationField({
  fieldDef,
  value,
  onChange,
  readOnly = false
}: DynamicDurationFieldProps) {
  const [mode, setMode] = useState<'input' | 'picker'>('input');

  const [days, setDays] = useState(() => {
    if (!value) return 0;
    const hours = value / 3600;
    return Math.floor(hours / 8);
  });

  const [hours, setHours] = useState(() => {
    if (!value) return 0;
    const totalHours = value / 3600;
    return Math.floor(totalHours % 8);
  });

  const [mins, setMins] = useState(() => {
    if (!value) return 0;
    return Math.floor((value % 3600) / 60);
  });

  const formatDuration = (seconds?: number) => {
    if (!seconds || seconds === 0) return '-';
    const totalHours = seconds / 3600;
    if (totalHours >= 24) {
      const d = Math.floor(totalHours / 8);
      const h = Math.floor(totalHours % 8);
      return `${d}d ${h}h ${Math.floor((seconds % 3600) / 60)}m`;
    }
    return `${Math.floor(totalHours)}h ${Math.floor((seconds % 3600) / 60)}m`;
  };

  const updateSeconds = (d: number, h: number, m: number) => {
    const totalSeconds = (d * 8 + h) * 3600 + m * 60;
    onChange?.(totalSeconds);
  };

  if (readOnly) {
    return (
      <span className="dynamic-duration">
        {formatDuration(value)}
      </span>
    );
  }

  return (
    <div className="dynamic-duration-field">
      {mode === 'input' ? (
        <div className="dynamic-duration-display" onClick={() => setMode('picker')}>
          <span className="dynamic-duration-value">{formatDuration(value)}</span>
          <span className="dynamic-duration-edit">Edit</span>
        </div>
      ) : (
        <div className="dynamic-duration-picker">
          <div className="dynamic-duration-inputs">
            <div className="dynamic-duration-unit">
              <input
                type="number"
                value={days}
                onChange={(e) => {
                  const v = parseInt(e.target.value) || 0;
                  setDays(v);
                  updateSeconds(v, hours, mins);
                }}
                min={0}
              />
              <label>Days</label>
            </div>
            <div className="dynamic-duration-unit">
              <input
                type="number"
                value={hours}
                onChange={(e) => {
                  const v = parseInt(e.target.value) || 0;
                  setHours(v);
                  updateSeconds(days, v, mins);
                }}
                min={0}
                max={7}
              />
              <label>Hours</label>
            </div>
            <div className="dynamic-duration-unit">
              <input
                type="number"
                value={mins}
                onChange={(e) => {
                  const v = parseInt(e.target.value) || 0;
                  setMins(v);
                  updateSeconds(days, hours, v);
                }}
                min={0}
                max={59}
              />
              <label>Minutes</label>
            </div>
          </div>
          <button
            type="button"
            className="dynamic-duration-done"
            onClick={() => setMode('input')}
          >
            Done
          </button>
        </div>
      )}
    </div>
  );
}