import React from 'react';
import MigrationServiceHealthPanel from './MigrationServiceHealthPanel';
import ClusterHealthBanner from './ClusterHealthBanner';
import MigrationObservabilityPanel from './MigrationObservabilityPanel';
import MigrationRoleSelector from './MigrationRoleSelector';

/** Dedicated Platform health view — always exposes ops panels (UI-Hardening). */
export default function MigrationPlatformHealthView() {
  return (
    <div className="space-y-6" data-testid="migration-platform-health-view">
      <div
        className="rounded-lg border p-4"
        style={{ borderColor: 'var(--sa-n200)', background: 'var(--sa-n0)' }}
      >
        <h2 style={{ margin: 0, fontSize: 'var(--sa-fs-lg)', fontWeight: 600, color: 'var(--sa-n900)' }}>
          Platform health & observability
        </h2>
        <p style={{ margin: '6px 0 0', fontSize: 'var(--sa-fs-sm)', color: 'var(--sa-n600)' }}>
          Downstream services, cluster mode, metrics endpoints, and migration role for RBAC testing.
        </p>
      </div>
      <ClusterHealthBanner />
      <MigrationServiceHealthPanel />
      <MigrationObservabilityPanel />
      <div className="rounded-lg border p-4" style={{ borderColor: 'var(--sa-n200)' }}>
        <p className="text-sm font-medium text-gray-800 mb-2">Migration role (RBAC)</p>
        <MigrationRoleSelector />
      </div>
    </div>
  );
}
