/**
 * Systems and Avionics — PageHeader primitive (Phase 1 stub).
 * Use on every page in Phase 3 re-skin to standardise title + actions.
 */
import React from 'react';

export interface PageHeaderProps {
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  breadcrumbs?: React.ReactNode;
  actions?: React.ReactNode;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ title, subtitle, breadcrumbs, actions }) => (
  <header
    style={{
      display: 'flex',
      flexDirection: 'column',
      gap: 'var(--sa-space-2)',
      padding: 'var(--sa-space-5) var(--sa-space-6)',
      borderBottom: '1px solid var(--sa-n200)',
      background: 'var(--sa-n0)',
    }}
  >
    {breadcrumbs}
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 'var(--sa-space-4)' }}>
      <div>
        <h1 style={{ margin: 0, fontFamily: 'var(--sa-font-sans)', fontSize: 'var(--sa-fs-2xl)', fontWeight: 600, color: 'var(--sa-n900)', lineHeight: 'var(--sa-lh-tight)' }}>
          {title}
        </h1>
        {subtitle && (
          <p style={{ margin: '4px 0 0', color: 'var(--sa-n600)', fontSize: 'var(--sa-fs-sm)' }}>{subtitle}</p>
        )}
      </div>
      {actions && <div style={{ display: 'flex', gap: 'var(--sa-space-2)', alignItems: 'center' }}>{actions}</div>}
    </div>
  </header>
);
