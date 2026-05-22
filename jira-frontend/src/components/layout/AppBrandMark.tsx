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
  className?: string;
}

export const AppBrandMark: React.FC<AppBrandMarkProps> = ({
  size = 24, showWordmark = true, inverted = false, className,
}) => {
  const fg = inverted ? 'var(--ab-white, #fff)' : 'var(--ab-primary-600, var(--sa-brand-600))';
  const accent = inverted ? 'var(--ab-primary-200, #99c2ff)' : 'var(--ab-primary-400, #3385ff)';
  return (
    <span className={`sa-brand-mark ${className ?? ''}`.trim()} style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
      <svg width={size} height={size} viewBox="0 0 32 32" role="img" aria-label="Systems and Avionics" className="sa-brand-icon" style={{ flexShrink: 0 }}>
        <rect x="2" y="2" width="28" height="28" rx="6" fill={fg} />
        <path d="M9 22 L16 9 L23 22 Z" fill={accent} opacity="0.9" />
        <circle cx="16" cy="20" r="2.2" fill="var(--ab-white, #fff)" />
      </svg>
      {showWordmark && (
        <span className={`sa-brand-wordmark ${inverted ? 'sa-brand-wordmark--inverted' : ''}`}>
          <span className="sa-brand-wordmark-full">Systems and Avionics</span>
          <span className="sa-brand-wordmark-short" aria-hidden="true">S&amp;A</span>
        </span>
      )}
    </span>
  );
};
