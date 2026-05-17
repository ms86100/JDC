/**
 * Systems and Avionics — Priority icon (Phase 1 stub).
 * Stacked-bar glyph driven by --sa-priority-* tokens.
 */
import React from 'react';

type Priority = 'critical' | 'high' | 'medium' | 'low' | 'lowest';

const config: Record<Priority, { color: string; bars: number; label: string }> = {
  critical: { color: 'var(--sa-priority-critical)', bars: 5, label: 'Critical' },
  high:     { color: 'var(--sa-priority-high)',     bars: 4, label: 'High' },
  medium:   { color: 'var(--sa-priority-medium)',   bars: 3, label: 'Medium' },
  low:      { color: 'var(--sa-priority-low)',      bars: 2, label: 'Low' },
  lowest:   { color: 'var(--sa-priority-lowest)',   bars: 1, label: 'Lowest' },
};

export interface PriorityIconProps {
  priority: Priority;
  size?: number;
}

export const PriorityIcon: React.FC<PriorityIconProps> = ({ priority, size = 14 }) => {
  const c = config[priority];
  return (
    <span title={c.label} aria-label={`Priority: ${c.label}`} style={{ display: 'inline-flex', alignItems: 'flex-end', gap: 1, height: size }}>
      {[1, 2, 3, 4, 5].map(i => (
        <span key={i} style={{
          width: 2, height: (size / 5) * i,
          background: i <= c.bars ? c.color : 'var(--sa-n200)',
          borderRadius: 1,
        }} />
      ))}
    </span>
  );
};
