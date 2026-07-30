import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { dashboardFieldApi } from '../../../api/fieldApi';
import DashboardGadgetFieldPicker from './DashboardGadgetFieldPicker';

const GADGET_KEY = 'custom-field-chart';

export default function CustomFieldChartGadget() {
  const [projectId, setProjectId] = useState('');
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['dashboard-gadget', GADGET_KEY, projectId],
    queryFn: () =>
      dashboardFieldApi.getGadget(GADGET_KEY, { projectId: projectId || undefined }).then((r) => r.data),
  });

  const chartData = useMemo(() => {
    const field = data?.configuredFields?.[0];
    if (!field || !data?.statistics) return null;
    const stat = data.statistics[field.fieldKey] as { distribution?: Record<string, number> } | undefined;
    if (!stat?.distribution) return null;
    const entries = Object.entries(stat.distribution).sort((a, b) => b[1] - a[1]).slice(0, 8);
    const max = Math.max(...entries.map(([, v]) => v), 1);
    return { field, entries, max };
  }, [data]);

  return (
    <section className="jdc-gadget" aria-label="Custom field chart">
      <div className="jdc-gadget-header">
        <span>Custom field chart</span>
        <DashboardGadgetFieldPicker
          gadgetKey={GADGET_KEY}
          projectId={projectId || undefined}
          onSaved={() => refetch()}
        />
      </div>
      <div className="jdc-gadget-body">
        <label style={{ fontSize: 12, display: 'block', marginBottom: 8 }}>
          Project ID (optional)
          <input
            type="text"
            value={projectId}
            onChange={(e) => setProjectId(e.target.value)}
            style={{ display: 'block', width: '100%', marginTop: 4 }}
          />
        </label>
        {isLoading ? (
          <p>Loading…</p>
        ) : !chartData ? (
          <p style={{ color: 'var(--jdc-text-subtle)' }}>
            Select one chart-compatible custom field (gear icon).
          </p>
        ) : (
          <div>
            <p style={{ fontWeight: 600, marginBottom: 8 }}>{chartData.field.displayName}</p>
            {chartData.entries.map(([label, count]) => (
              <div key={label} className="jdc-cf-bar-row">
                <span className="jdc-cf-bar-label" title={label}>
                  {label}
                </span>
                <div className="jdc-cf-bar-track">
                  <div
                    className="jdc-cf-bar-fill"
                    style={{ width: `${(count / chartData.max) * 100}%` }}
                  />
                </div>
                <span className="jdc-cf-bar-count">{count}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
