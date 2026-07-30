/**
 * Systems and Avionics — Tag/Lozenge primitive (Phase 1 stub).
 */
import React from 'react';

type Tone = 'neutral' | 'brand' | 'success' | 'warn' | 'danger' | 'info';

const palette: Record<Tone, { bg: string; fg: string }> = {
  neutral: { bg: 'var(--sa-n100)',       fg: 'var(--sa-n700)' },
  brand:   { bg: 'var(--sa-brand-50)',   fg: 'var(--sa-brand-700)' },
  success: { bg: 'var(--sa-success-50)', fg: 'var(--sa-success-700)' },
  warn:    { bg: 'var(--sa-warn-50)',    fg: 'var(--sa-warn-700)' },
  danger:  { bg: 'var(--sa-danger-50)',  fg: 'var(--sa-danger-700)' },
  info:    { bg: 'var(--sa-info-50)',    fg: 'var(--sa-info-700)' },
};

export interface TagProps {
  tone?: Tone;
  children: React.ReactNode;
  uppercase?: boolean;
}

export const Tag: React.FC<TagProps> = ({ tone = 'neutral', children, uppercase }) => {
  const p = palette[tone];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 4,
      background: p.bg, color: p.fg, fontFamily: 'var(--sa-font-sans)',
      fontSize: 'var(--sa-fs-xs)', fontWeight: 600,
      padding: '2px 6px', borderRadius: 'var(--sa-radius-sm)',
      textTransform: uppercase ? 'uppercase' : 'none', letterSpacing: uppercase ? 0.4 : 0,
    }}>{children}</span>
  );
};
