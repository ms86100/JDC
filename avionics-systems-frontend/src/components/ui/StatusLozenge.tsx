/**
 * Systems and Avionics — Workflow status lozenge (Phase 1 stub).
 * Maps standard workflow categories to consistent colour tokens.
 */
import React from 'react';

type Status = 'todo' | 'inprogress' | 'done' | 'blocked';

const map: Record<Status, { bg: string; fg: string; label: string }> = {
  todo:       { bg: 'var(--sa-status-todo-bg)',       fg: 'var(--sa-status-todo-fg)',       label: 'To Do' },
  inprogress: { bg: 'var(--sa-status-inprogress-bg)', fg: 'var(--sa-status-inprogress-fg)', label: 'In Progress' },
  done:       { bg: 'var(--sa-status-done-bg)',       fg: 'var(--sa-status-done-fg)',       label: 'Done' },
  blocked:    { bg: 'var(--sa-status-blocked-bg)',    fg: 'var(--sa-status-blocked-fg)',    label: 'Blocked' },
};

export interface StatusLozengeProps {
  status: Status;
  children?: React.ReactNode;
}

export const StatusLozenge: React.FC<StatusLozengeProps> = ({ status, children }) => {
  const s = map[status];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center',
      background: s.bg, color: s.fg,
      fontSize: 'var(--sa-fs-xs)', fontWeight: 700, letterSpacing: 0.4,
      textTransform: 'uppercase',
      padding: '2px 6px', borderRadius: 'var(--sa-radius-sm)',
    }}>{children ?? s.label}</span>
  );
};
