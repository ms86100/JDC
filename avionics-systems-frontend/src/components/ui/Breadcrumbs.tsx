/**
 * Systems and Avionics — Breadcrumbs (React Router).
 */
import React from 'react';
import { Link } from 'react-router-dom';

export interface Crumb { label: string; href?: string; }
export interface BreadcrumbsProps { items: Crumb[]; }

export const Breadcrumbs: React.FC<BreadcrumbsProps> = ({ items }) => (
  <nav aria-label="Breadcrumb" style={{ fontSize: 'var(--sa-fs-xs)', color: 'var(--sa-n600)' }}>
    <ol style={{ display: 'flex', gap: 'var(--sa-space-2)', margin: 0, padding: 0, listStyle: 'none', flexWrap: 'wrap' }}>
      {items.map((c, i) => (
        <li key={i} style={{ display: 'flex', gap: 'var(--sa-space-2)', alignItems: 'center' }}>
          {c.href ? (
            <Link to={c.href} style={{ color: 'var(--sa-brand-600)', textDecoration: 'none' }}>
              {c.label}
            </Link>
          ) : (
            <span>{c.label}</span>
          )}
          {i < items.length - 1 && <span aria-hidden="true">/</span>}
        </li>
      ))}
    </ol>
  </nav>
);
