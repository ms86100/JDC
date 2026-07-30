/**
 * Systems and Avionics — EmptyState primitive (Phase 1 stub).
 */
import React from 'react';

export interface EmptyStateProps {
  title: React.ReactNode;
  description?: React.ReactNode;
  action?: React.ReactNode;
  icon?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({ title, description, action, icon }) => (
  <div role="status" style={{
    display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
    padding: 'var(--sa-space-9) var(--sa-space-6)', textAlign: 'center', color: 'var(--sa-n600)',
  }}>
    {icon && <div style={{ marginBottom: 'var(--sa-space-3)', color: 'var(--sa-n400)' }}>{icon}</div>}
    <h2 style={{ margin: 0, fontFamily: 'var(--sa-font-sans)', fontSize: 'var(--sa-fs-lg)', color: 'var(--sa-n800)' }}>{title}</h2>
    {description && <p style={{ margin: '8px 0 16px', maxWidth: 420 }}>{description}</p>}
    {action}
  </div>
);
