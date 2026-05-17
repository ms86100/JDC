/**
 * Systems and Avionics — brand mark.
 * Replaces the legacy Atlassian-style diamond and "J" logo across all shells.
 * Pure SVG, no external assets, scales with `size`.
 */
import React from 'react';

export interface AppBrandMarkProps {
  size?: number;
  showWordmark?: boolean;
  inverted?: boolean;
}

export const AppBrandMark: React.FC<AppBrandMarkProps> = ({
  size = 24, showWordmark = true, inverted = false,
}) => {
  const fg = inverted ? 'var(--sa-n0)' : 'var(--sa-brand-600)';
  const accent = inverted ? 'var(--sa-brand-200)' : 'var(--sa-brand-400)';
  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
      <svg width={size} height={size} viewBox="0 0 32 32" role="img" aria-label="Systems and Avionics">
        <rect x="2" y="2" width="28" height="28" rx="6" fill={fg} />
        <path d="M9 22 L16 9 L23 22 Z" fill={accent} opacity="0.9" />
        <circle cx="16" cy="20" r="2.2" fill="var(--sa-n0)" />
      </svg>
      {showWordmark && (
        <span style={{
          fontFamily: 'var(--sa-font-sans)', fontWeight: 600,
          fontSize: 'var(--sa-fs-base)', letterSpacing: 0.2,
          color: inverted ? 'var(--sa-n0)' : 'var(--sa-n900)', whiteSpace: 'nowrap',
        }}>Systems and Avionics</span>
      )}
    </span>
  );
};
