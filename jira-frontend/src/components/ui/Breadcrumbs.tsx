/**
 * Systems and Avionics — Breadcrumbs primitive (Phase 1 stub).
 * Renders an accessible <nav> list. Wire to a routeMeta map in Phase 2.
 */
import React from 'react';

export interface Crumb { label: string; href?: string; }
export interface BreadcrumbsProps { items: Crumb[]; }

export const Breadcrumbs: React.FC<BreadcrumbsProps> = ({ items }) => (
  <nav aria-label="Breadcrumb" style={{ fontSize: 'var(--sa-fs-xs)', color: 'var(--sa-n600)' }}>
    <ol style={{ display: 'flex', gap: 'var(--sa-space-2)', margin: 0, padding: 0, listStyle: 'none' }}>
      {items.map((c, i) => (
        <li key={i} style={{ display: 'flex', gap: 'var(--sa-space-2)', alignItems: 'center' }}>
          {c.href ? <a href={c.href} style={{ color: 'var(--sa-brand-600)', textDecoration: 'none' }}>{c.label}</a> : <span>{c.label}</span>}
          {i < items.length - 1 && <span aria-hidden="true">/</span>}
        </li>
      ))}
    </ol>
  </nav>
);
