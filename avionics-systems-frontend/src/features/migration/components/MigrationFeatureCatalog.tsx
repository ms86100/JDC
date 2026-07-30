import React from 'react';
import { Link } from 'react-router-dom';
import type { MigrationCenterView } from './MigrationCenterNav';
import type { ImportType } from '../types/migration';
import { migrationPath } from '../utils/migrationDeepLinks';

export interface CatalogItem {
  area: string;
  feature: string;
  uiLocation: string;
  wizardStep?: string;
  testable: 'UI' | 'UI+API';
  navigateTo?: MigrationCenterView;
  importType?: ImportType;
  externalHref?: string;
}

const CATALOG: CatalogItem[] = [
  { area: 'Import', feature: 'CSV / Excel import', uiLocation: 'Import wizard → Source', wizardStep: 'source', testable: 'UI+API', navigateTo: 'wizard', importType: 'csv' },
  { area: 'Import', feature: 'Issue XML (Systems DC)', uiLocation: 'Import wizard → Issue XML (Systems DC)', wizardStep: 'source', testable: 'UI+API', navigateTo: 'wizard', importType: 'issue-xml' },
  { area: 'Import', feature: 'Systems and Avionics backup', uiLocation: 'Import wizard → Systems and Avionics Backup', wizardStep: 'source', testable: 'UI+API', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Import', feature: 'Workflow XML import', uiLocation: 'Import wizard → Workflow XML', wizardStep: 'source', testable: 'UI+API', navigateTo: 'wizard', importType: 'workflow-xml' },
  { area: 'Import', feature: 'Project-to-project copy', uiLocation: 'Import wizard → Project Copy', wizardStep: 'source', testable: 'UI+API', navigateTo: 'wizard', importType: 'project-import' },
  { area: 'Import', feature: 'Project export', uiLocation: 'Import wizard → Project Export', wizardStep: 'source', testable: 'UI+API', navigateTo: 'wizard', importType: 'project-export' },
  { area: 'Import', feature: 'Field mapping', uiLocation: 'Wizard → Map Fields', wizardStep: 'map', testable: 'UI', navigateTo: 'wizard' },
  { area: 'Import', feature: 'Provision missing custom fields', uiLocation: 'Wizard → Map → Provision panel', wizardStep: 'map', testable: 'UI+API', navigateTo: 'wizard', importType: 'csv' },
  { area: 'Import', feature: 'CSV External vs Lightweight profile', uiLocation: 'Wizard → Configure → CSV import profile', wizardStep: 'configure', testable: 'UI+API', navigateTo: 'wizard', importType: 'csv' },
  { area: 'Import', feature: 'CSV attachment URLs / FILE:', uiLocation: 'Configure → External profile + Attachments column', wizardStep: 'configure', testable: 'UI+API', navigateTo: 'wizard', importType: 'csv' },
  { area: 'Import', feature: 'Option value mapping matrix', uiLocation: 'Wizard → Map → Option mapping', wizardStep: 'map', testable: 'UI+API', navigateTo: 'wizard' },
  { area: 'Ops', feature: 'Migration import settings', uiLocation: 'Migration Center → Import settings', testable: 'UI+API', navigateTo: 'settings' },
  { area: 'Admin', feature: 'Custom fields CRUD', uiLocation: 'Admin → Custom fields', testable: 'UI+API', externalHref: '/admin/custom-fields' },
  { area: 'Issues', feature: 'Imported custom field values', uiLocation: 'Issue → Details → Imported custom fields', testable: 'UI+API', externalHref: '/issues' },
  { area: 'Import', feature: 'Dry-run validation', uiLocation: 'Wizard → Validate + DC validation panel', wizardStep: 'validate', testable: 'UI+API', navigateTo: 'wizard' },
  { area: 'Import', feature: 'DC AC sign-off preview (configure)', uiLocation: 'Systems DC → Configure → Validate now → Enterprise AC table', wizardStep: 'configure', testable: 'UI+API', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Workflow', feature: 'Workflow admin API tools', uiLocation: 'Sidebar → Workflows → Administration → Tools', testable: 'UI+API', externalHref: '/workflows/admin/tools' },
  { area: 'Import', feature: 'DC options (delta, resume, bundle)', uiLocation: 'Wizard → Configure → DC options', wizardStep: 'configure', testable: 'UI', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Import', feature: 'Conflict & unknown custom fields', uiLocation: 'Validate / Configure / Review → conflict & unknown panels', wizardStep: 'validate', testable: 'UI+API', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Import', feature: 'DC relationship graph', uiLocation: 'Validate / Configure → relationship graph panel', wizardStep: 'validate', testable: 'UI', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Import', feature: 'DC review & execute gate', uiLocation: 'Review → DC review panel (warnings ack)', wizardStep: 'review', testable: 'UI', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Import', feature: 'Stub vs live downstream (DC)', uiLocation: 'Configure → DC options', wizardStep: 'configure', testable: 'UI+API', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Import', feature: 'ClamAV upload scan', uiLocation: 'Source upload → virus scan badge', wizardStep: 'source', testable: 'UI+API', navigateTo: 'wizard' },
  { area: 'Execution', feature: 'Live progress & pause/resume', uiLocation: 'Wizard → Progress', wizardStep: 'importing', testable: 'UI+API', navigateTo: 'wizard' },
  { area: 'Execution', feature: 'Chunked attachments', uiLocation: 'Progress → attachment chunk M/N', wizardStep: 'importing', testable: 'UI', navigateTo: 'wizard' },
  { area: 'Jobs', feature: 'Job history & filters', uiLocation: 'Job history tab', testable: 'UI+API', navigateTo: 'history' },
  { area: 'Jobs', feature: 'Job detail (audit, DLQ, logs)', uiLocation: 'History → View details modal', testable: 'UI+API', navigateTo: 'history' },
  { area: 'Jobs', feature: 'DLQ retry', uiLocation: 'Job detail → Dead letter queue', testable: 'UI+API', navigateTo: 'history' },
  { area: 'Jobs', feature: 'Rollback job', uiLocation: 'History → Rollback action', testable: 'UI+API', navigateTo: 'history' },
  { area: 'Jobs', feature: 'Download report CSV', uiLocation: 'History / Complete → Download report', testable: 'UI+API', navigateTo: 'history' },
  { area: 'Ops', feature: 'Global DLQ console', uiLocation: 'Global DLQ tab', testable: 'UI+API', navigateTo: 'dlq' },
  { area: 'Ops', feature: 'Saved mapping templates', uiLocation: 'Mapping templates tab', testable: 'UI+API', navigateTo: 'templates' },
  { area: 'Post-import', feature: 'Verification report', uiLocation: 'Complete / Job detail → Verification', wizardStep: 'complete', testable: 'UI+API', navigateTo: 'wizard' },
  { area: 'Post-import', feature: 'Search reindex', uiLocation: 'Complete / Job detail → Reindex', wizardStep: 'complete', testable: 'UI+API', navigateTo: 'wizard' },
  { area: 'Post-import', feature: 'Imported issues & attachments', uiLocation: 'Complete step tables', wizardStep: 'complete', testable: 'UI+API', navigateTo: 'wizard' },
  { area: 'Post-import', feature: 'DC staging insights', uiLocation: 'Complete / Job detail', wizardStep: 'complete', testable: 'UI+API', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Post-import', feature: 'Parity / SLA / AC sign-off', uiLocation: 'Complete + Job console (Systems DC)', wizardStep: 'complete', testable: 'UI+API', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Post-import', feature: 'DC rollback / retry / reports', uiLocation: 'Complete → DC operations + Job console', wizardStep: 'complete', testable: 'UI+API', navigateTo: 'wizard', importType: 'legacy-dc' },
  { area: 'Execution', feature: 'Job console (progress)', uiLocation: 'Progress → Job console button', wizardStep: 'importing', testable: 'UI+API', navigateTo: 'wizard' },
  { area: 'Workflow', feature: 'Workflow graph preview', uiLocation: 'Workflow XML → Validate', wizardStep: 'validate', testable: 'UI+API', navigateTo: 'wizard', importType: 'workflow-xml' },
  { area: 'Workflow', feature: 'Simulate transition path', uiLocation: 'Workflow XML panel', testable: 'UI+API', navigateTo: 'wizard', importType: 'workflow-xml' },
  { area: 'Ops', feature: 'Downstream service health', uiLocation: 'Platform health tab', testable: 'UI+API', navigateTo: 'health' },
  { area: 'Ops', feature: 'Cluster degraded banner', uiLocation: 'Header / Platform health', testable: 'UI+API', navigateTo: 'health' },
  { area: 'Ops', feature: 'Observability links', uiLocation: 'Platform health tab', testable: 'UI+API', navigateTo: 'health' },
  { area: 'Security', feature: 'Migration RBAC role', uiLocation: 'Header → role selector', testable: 'UI', navigateTo: 'wizard' },
];

function catalogHref(row: CatalogItem): string | null {
  if (row.externalHref) return row.externalHref;
  if (!row.navigateTo) return null;
  if (row.importType) {
    return migrationPath('wizard', row.importType);
  }
  return migrationPath(row.navigateTo);
}

interface Props {
  onNavigate?: (view: MigrationCenterView) => void;
}

export default function MigrationFeatureCatalog({ onNavigate }: Props) {
  const areas = [...new Set(CATALOG.map((c) => c.area))];

  return (
    <div
      className="bg-white rounded-lg border overflow-hidden"
      data-testid="migration-feature-catalog"
      style={{ borderColor: 'var(--sa-n200)' }}
    >
      <div style={{ padding: 'var(--sa-space-4)', borderBottom: '1px solid var(--sa-n200)', background: 'var(--sa-n50)' }}>
        <h2 style={{ margin: 0, fontSize: 'var(--sa-fs-lg)', fontWeight: 600, color: 'var(--sa-n900)' }}>
          Migration capability map
        </h2>
        <p style={{ margin: '6px 0 0', fontSize: 'var(--sa-fs-sm)', color: 'var(--sa-n600)' }}>
          Every implemented migration feature and where to exercise it in the UI (UI-Hardening audit).
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm" style={{ fontFamily: 'var(--sa-font-sans)' }}>
          <thead style={{ background: 'var(--sa-n50)', position: 'sticky', top: 0 }}>
            <tr>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--sa-n600)' }}>Area</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--sa-n600)' }}>Feature</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--sa-n600)' }}>UI location</th>
              <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--sa-n600)' }}>Test</th>
              <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide" style={{ color: 'var(--sa-n600)' }}>Go</th>
            </tr>
          </thead>
          <tbody className="divide-y" style={{ borderColor: 'var(--sa-n100)' }}>
            {areas.flatMap((area) =>
              CATALOG.filter((c) => c.area === area).map((row) => {
                const href = catalogHref(row);
                return (
                  <tr key={`${row.area}-${row.feature}`} className="hover:bg-gray-50">
                    <td className="px-4 py-2 font-medium" style={{ color: 'var(--sa-n700)' }}>{row.area}</td>
                    <td className="px-4 py-2" style={{ color: 'var(--sa-n900)' }}>{row.feature}</td>
                    <td className="px-4 py-2" style={{ color: 'var(--sa-n600)' }}>
                      {row.uiLocation}
                      {row.wizardStep && (
                        <span className="ml-1 text-xs" style={{ color: 'var(--sa-n500)' }}>
                          ({row.wizardStep})
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-2">
                      <span
                        className="text-xs font-medium px-2 py-0.5 rounded"
                        style={{
                          background: row.testable === 'UI+API' ? 'var(--sa-brand-50)' : 'var(--sa-n100)',
                          color: row.testable === 'UI+API' ? 'var(--sa-brand-700)' : 'var(--sa-n700)',
                        }}
                      >
                        {row.testable}
                      </span>
                    </td>
                    <td className="px-4 py-2 text-right">
                      {href && (
                        <Link
                          to={href}
                          className="text-sm underline"
                          style={{ color: 'var(--sa-brand-600)' }}
                          onClick={() => {
                            if (row.navigateTo && onNavigate && !row.externalHref) {
                              onNavigate(row.navigateTo);
                            }
                          }}
                        >
                          Open
                        </Link>
                      )}
                    </td>
                  </tr>
                );
              }),
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
