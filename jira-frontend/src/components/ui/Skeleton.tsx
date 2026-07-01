/**
 * Systems and Avionics — Skeleton primitive (Phase 1 stub).
 */
import React from 'react';

export interface SkeletonProps {
  width?: number | string;
  height?: number | string;
  radius?: number | string;
  style?: React.CSSProperties;
}

export const Skeleton: React.FC<SkeletonProps> = ({ width = '100%', height = 12, radius = 'var(--sa-radius-sm)', style }) => (
  <span
    aria-hidden="true"
    style={{
      display: 'inline-block', width, height, borderRadius: radius,
      background: 'linear-gradient(90deg, var(--sa-n100) 25%, var(--sa-n200) 37%, var(--sa-n100) 63%)',
      backgroundSize: '400% 100%',
      animation: 'sa-skel 1.4s ease infinite',
      ...style,
    }}
  />
);

// Inject keyframes once.
if (typeof document !== 'undefined' && !document.getElementById('sa-skel-kf')) {
  const s = document.createElement('style');
  s.id = 'sa-skel-kf';
  s.textContent = '@keyframes sa-skel{0%{background-position:100% 50%}100%{background-position:0 50%}}';
  document.head.appendChild(s);
}
