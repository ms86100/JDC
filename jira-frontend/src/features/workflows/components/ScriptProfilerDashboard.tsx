import { useQuery } from '@tanstack/react-query';
import { scriptApi } from '../../../api/scriptApi';

export default function ScriptProfilerDashboard() {
  const { data: stats, isLoading, isError } = useQuery({
    queryKey: ['script-profiler-stats'],
    queryFn: async () => {
      const res = await scriptApi.getProfilerStats();
      return res.data;
    },
    refetchInterval: 30000,
  });

  if (isLoading) return <p style={{ padding: 16, color: '#6b7280' }}>Loading profiler data...</p>;
  if (isError || !stats) return <p style={{ padding: 16, color: '#ef4444' }}>Failed to load profiler stats.</p>;

  const successRate = typeof stats.successRate === 'number' ? stats.successRate.toFixed(1) : '0';
  const slowest = (stats.slowestScripts || []) as Array<{ scriptKey: string; avgMs: number }>;
  const byMode = (stats.executionsByMode || {}) as Record<string, number>;

  return (
    <div style={{ padding: 16 }}>
      <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: 16 }}>Script Execution Profiler</h3>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <div style={{ background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: 8, padding: 16, textAlign: 'center' }}>
          <div style={{ fontSize: '2rem', fontWeight: 700, color: '#0369a1' }}>{stats.totalExecutions}</div>
          <div style={{ fontSize: '0.8rem', color: '#6b7280', marginTop: 4 }}>Total Executions</div>
        </div>
        <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, padding: 16, textAlign: 'center' }}>
          <div style={{ fontSize: '2rem', fontWeight: 700, color: '#15803d' }}>{successRate}%</div>
          <div style={{ fontSize: '0.8rem', color: '#6b7280', marginTop: 4 }}>Success Rate</div>
        </div>
        <div style={{ background: '#fef3c7', border: '1px solid #fde68a', borderRadius: 8, padding: 16, textAlign: 'center' }}>
          <div style={{ fontSize: '2rem', fontWeight: 700, color: '#92400e' }}>{slowest.length > 0 ? Math.round(slowest[0]?.avgMs || 0) : 0}ms</div>
          <div style={{ fontSize: '0.8rem', color: '#6b7280', marginTop: 4 }}>Slowest Avg</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <div>
          <h4 style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: 8, color: '#374151' }}>Slowest Scripts (Avg Execution Time)</h4>
          {slowest.length === 0 ? (
            <p style={{ color: '#9ca3af', fontSize: '0.85rem' }}>No execution data yet.</p>
          ) : (
            <table style={{ width: '100%', fontSize: '0.85rem', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e5e7eb' }}>
                  <th style={{ textAlign: 'left', padding: '6px 8px', color: '#6b7280' }}>Script Key</th>
                  <th style={{ textAlign: 'right', padding: '6px 8px', color: '#6b7280' }}>Avg (ms)</th>
                  <th style={{ textAlign: 'left', padding: '6px 8px', color: '#6b7280' }}>Performance</th>
                </tr>
              </thead>
              <tbody>
                {slowest.map((s, i) => {
                  const avgMs = Math.round(s.avgMs);
                  const barWidth = Math.min(100, (avgMs / (slowest[0]?.avgMs || 1)) * 100);
                  const barColor = avgMs > 3000 ? '#ef4444' : avgMs > 1000 ? '#f59e0b' : '#22c55e';
                  return (
                    <tr key={s.scriptKey} style={{ borderBottom: '1px solid #f3f4f6', background: i % 2 === 0 ? '#fafafa' : '#fff' }}>
                      <td style={{ padding: '6px 8px', fontFamily: 'monospace', fontSize: '0.8rem' }}>{s.scriptKey}</td>
                      <td style={{ padding: '6px 8px', textAlign: 'right', fontWeight: 500 }}>{avgMs}</td>
                      <td style={{ padding: '6px 8px' }}>
                        <div style={{ width: '100%', height: 8, background: '#e5e7eb', borderRadius: 4 }}>
                          <div style={{ width: `${barWidth}%`, height: '100%', background: barColor, borderRadius: 4 }} />
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        <div>
          <h4 style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: 8, color: '#374151' }}>Executions by Mode</h4>
          {Object.keys(byMode).length === 0 ? (
            <p style={{ color: '#9ca3af', fontSize: '0.85rem' }}>No execution data yet.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {Object.entries(byMode)
                .sort(([, a], [, b]) => (b as number) - (a as number))
                .map(([mode, count]) => {
                  const total = Object.values(byMode).reduce((s, c) => s + (c as number), 0);
                  const pct = total > 0 ? ((count as number) / total * 100).toFixed(0) : '0';
                  const modeColors: Record<string, string> = {
                    WORKFLOW: '#3b82f6', CONSOLE: '#8b5cf6', SCHEDULED: '#f59e0b',
                    LISTENER: '#10b981', FIELD_BEHAVIOR: '#ec4899', API: '#6366f1',
                    CALCULATED_FIELD: '#14b8a6',
                  };
                  return (
                    <div key={mode} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{
                        width: 10, height: 10, borderRadius: '50%',
                        background: modeColors[mode] || '#6b7280', flexShrink: 0,
                      }} />
                      <span style={{ fontSize: '0.85rem', flex: 1 }}>{mode}</span>
                      <span style={{ fontSize: '0.85rem', fontWeight: 500 }}>{count as number}</span>
                      <span style={{ fontSize: '0.75rem', color: '#9ca3af', width: 40, textAlign: 'right' }}>{pct}%</span>
                    </div>
                  );
                })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
