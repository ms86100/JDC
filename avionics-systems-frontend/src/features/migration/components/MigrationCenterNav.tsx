import React from 'react';

export type MigrationCenterView = 'wizard' | 'history' | 'health' | 'catalog' | 'dlq' | 'templates' | 'settings';

interface Props {
  active: MigrationCenterView;
  onChange: (view: MigrationCenterView) => void;
}

const TABS: { id: MigrationCenterView; label: string; description: string }[] = [
  { id: 'wizard', label: 'Import wizard', description: 'Start and run migrations' },
  { id: 'history', label: 'Job history', description: 'Audit, retry, rollback, reports' },
  { id: 'health', label: 'Platform health', description: 'Services, cluster, observability' },
  { id: 'catalog', label: 'Capability map', description: 'All features & where to find them' },
  { id: 'dlq', label: 'Global DLQ', description: 'Dead-letter queue, retry, purge' },
  { id: 'templates', label: 'Mapping templates', description: 'Saved field mapping CRUD' },
  { id: 'settings', label: 'Import settings', description: 'Attachments, FILE: dir, API notes' },
];

export default function MigrationCenterNav({ active, onChange }: Props) {
  return (
    <nav
      aria-label="Migration Center sections"
      className="migration-center-nav"
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: 'var(--sa-space-2)',
        padding: 'var(--sa-space-2)',
        background: 'var(--sa-n0)',
        border: '1px solid var(--sa-n200)',
        borderRadius: 'var(--sa-radius-md)',
      }}
    >
      {TABS.map((tab) => {
        const isActive = active === tab.id;
        return (
          <button
            key={tab.id}
            type="button"
            data-testid={`migration-nav-${tab.id}`}
            onClick={() => onChange(tab.id)}
            aria-current={isActive ? 'page' : undefined}
            style={{
              flex: '1 1 140px',
              minWidth: 120,
              textAlign: 'left',
              padding: 'var(--sa-space-3) var(--sa-space-4)',
              borderRadius: 'var(--sa-radius-sm)',
              border: isActive ? '1px solid var(--sa-brand-500)' : '1px solid transparent',
              background: isActive ? 'var(--sa-brand-50)' : 'transparent',
              cursor: 'pointer',
              fontFamily: 'var(--sa-font-sans)',
            }}
          >
            <span
              style={{
                display: 'block',
                fontSize: 'var(--sa-fs-sm)',
                fontWeight: isActive ? 600 : 500,
                color: isActive ? 'var(--sa-brand-700)' : 'var(--sa-n800)',
              }}
            >
              {tab.label}
            </span>
            <span style={{ display: 'block', fontSize: 'var(--sa-fs-xs)', color: 'var(--sa-n500)', marginTop: 2 }}>
              {tab.description}
            </span>
          </button>
        );
      })}
    </nav>
  );
}
