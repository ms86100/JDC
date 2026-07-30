import React, { useState } from 'react';
import MigrationPanel from './MigrationPanel';

const CAPABILITIES = [
  { area: 'Import types', where: 'Step 1 — Source', items: ['CSV/Excel', 'Systems DC XML/ZIP', 'Workflow XML', 'Project copy', 'Project export'] },
  { area: 'Health & ops', where: 'Page header + Source step', items: ['Cluster health', 'Service health', 'Observability metrics', 'Migration role'] },
  { area: 'DC validate', where: 'Validate & Configure steps', items: ['Server validation', 'Conflicts', 'Unknown custom fields', 'Relationship graph'] },
  { area: 'DC execute', where: 'Configure → Review → Start Import', items: ['Backup ZIP', 'Dry run', 'Incremental', 'History-only', 'Parallel workers', 'Live vs stub downstream'] },
  { area: 'During import', where: 'Progress step', items: ['Live progress', 'Pause/resume', 'Attachment bytes', 'Logs'] },
  { area: 'After import', where: 'Complete step + Job history', items: ['Parity report', 'SLA proof', 'AC sign-off', 'Rollback/retry/reports', 'Verification', 'Reindex', 'Job console (DLQ/audit)'] },
];

export default function MigrationCapabilityIndex() {
  const [open, setOpen] = useState(false);

  return (
    <div data-testid="migration-capability-index">
      <MigrationPanel
        title="What you can do in Migration Center"
        subtitle="Quick map of wizard steps and operational panels"
        actions={
          <button type="button" className="migration-btn-secondary" onClick={() => setOpen(!open)}>
            {open ? 'Collapse' : 'Expand'}
          </button>
        }
        noPadding
      >
        {open && (
          <div
            className="migration-panel__body"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))',
              gap: 'var(--sa-space-3)',
            }}
          >
            {CAPABILITIES.map((c) => (
              <div
                key={c.area}
                style={{
                  border: '1px solid var(--sa-n200)',
                  borderRadius: 'var(--sa-radius-sm)',
                  padding: 'var(--sa-space-3)',
                  fontSize: 'var(--sa-fs-xs)',
                  background: 'var(--sa-n0)',
                }}
              >
                <p style={{ margin: 0, fontWeight: 600, color: 'var(--sa-n900)' }}>{c.area}</p>
                <p style={{ margin: '4px 0', color: 'var(--sa-n600)' }}>{c.where}</p>
                <ul style={{ margin: 0, paddingLeft: 16, color: 'var(--sa-n800)' }}>
                  {c.items.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}
      </MigrationPanel>
    </div>
  );
}
