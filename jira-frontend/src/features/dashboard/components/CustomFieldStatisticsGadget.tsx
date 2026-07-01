import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { dashboardFieldApi } from '../../../api/fieldApi';
import DashboardGadgetFieldPicker from './DashboardGadgetFieldPicker';

const GADGET_KEY = 'custom-field-statistics';

export default function CustomFieldStatisticsGadget() {
  const [projectId, setProjectId] = useState<string>('');
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['dashboard-gadget', GADGET_KEY, projectId],
    queryFn: () =>
      dashboardFieldApi
        .getGadget(GADGET_KEY, { projectId: projectId || undefined })
        .then((r) => r.data),
  });

  const stats = data?.statistics ?? {};
  const entries = Object.entries(stats);

  return (
    <section className="jdc-gadget" aria-label="Custom field statistics">
      <div className="jdc-gadget-header">
        <span>Custom field statistics</span>
        <DashboardGadgetFieldPicker
          gadgetKey={GADGET_KEY}
          projectId={projectId || undefined}
          onSaved={() => refetch()}
        />
      </div>
      <div className="jdc-gadget-body">
        <label style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>
          Project ID (optional filter)
          <input
            type="text"
            value={projectId}
            onChange={(e) => setProjectId(e.target.value)}
            placeholder="UUID"
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
        </label>
        {isLoading ? (
          <p>Loading…</p>
        ) : entries.length === 0 ? (
          <p style={{ color: 'var(--jdc-text-subtle)' }}>
            Configure fields via the gear icon. Values are aggregated from migrated custom field data.
          </p>
        ) : (
          entries.map(([key, val]) => {
            const row = val as { displayName?: string; totalValues?: number; distribution?: Record<string, number> };
            return (
              <div key={key} className="jdc-cf-stat-block">
                <h4>{row.displayName ?? key}</h4>
                <p style={{ fontSize: 12, margin: '4px 0' }}>
                  {row.totalValues ?? 0} values recorded
                </p>
                {row.distribution && (
                  <ul className="jdc-cf-dist">
                    {Object.entries(row.distribution)
                      .slice(0, 5)
                      .map(([label, count]) => (
                        <li key={label}>
                          <span>{label}</span>
                          <span>{count}</span>
                        </li>
                      ))}
                  </ul>
                )}
              </div>
            );
          })
        )}
      </div>
    </section>
  );
}
