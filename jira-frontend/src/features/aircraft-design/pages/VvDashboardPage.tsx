import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { vvReportApi } from '../../../api/vvReportApi';
import '../AircraftDesignStyles.css';

interface MetricsBlock {
  total?: number;
  newCount?: number;
  verifiedCount?: number;
  releasedCount?: number;
  openCount?: number;
  blockingCount?: number;
}

interface DashboardData {
  projectId?: string;
  vvoMetrics?: MetricsBlock;
  techEventMetrics?: MetricsBlock;
  benchDefectMetrics?: MetricsBlock;
  problemReportMetrics?: MetricsBlock;
  generatedAt?: string;
  // Legacy flat fields (for backward compat)
  totalVvos?: number;
  verifiedCount?: number;
  releasedCount?: number;
  openTechEvents?: number;
  blockingBenchDefects?: number;
  openProblemReports?: number;
  coveragePercentage?: number;
  vvoStatusDistribution?: Record<string, number>;
  techEventTrend?: { label: string; count: number }[];
  benchDefectSeverity?: Record<string, number>;
  recentActivity?: { id: string; type: string; summary: string; timestamp: string; user: string }[];
}

const STATUS_COLORS: Record<string, string> = {
  NEW: '#0052cc',
  TO_BE_VERIFIED: '#ff8b00',
  VERIFIED: '#00875a',
  RELEASED: '#36b37e',
  CANCELLED: '#de350b',
  SUPERSEDED: '#6b778c',
};

export default function VvDashboardPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';

  const [dashboard, setDashboard] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [projectInput, setProjectInput] = useState(projectId);

  useEffect(() => {
    if (projectId) loadDashboard(projectId);
  }, [projectId]);

  async function loadDashboard(pid: string) {
    setLoading(true);
    setError('');
    try {
      const res = await vvReportApi.getProjectDashboard(pid);
      setDashboard(res.data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  }

  function handleLoad() {
    if (projectInput) loadDashboard(projectInput);
  }

  function renderStatusChart(distribution: Record<string, number>) {
    const entries = Object.entries(distribution);
    const total = entries.reduce((sum, [, count]) => sum + count, 0);
    if (total === 0) return <p style={{ color: '#97a0af', fontStyle: 'italic' }}>No data</p>;

    const maxCount = Math.max(...entries.map(([, c]) => c));

    return (
      <div>
        <div className="ads-chart-placeholder" style={{ height: 140, alignItems: 'flex-end' }}>
          {entries.map(([status, count]) => (
            <div key={status} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
              <span style={{ fontSize: 11, fontWeight: 600, color: '#172b4d' }}>{count}</span>
              <div
                className="ads-chart-bar"
                style={{
                  height: maxCount > 0 ? `${(count / maxCount) * 100}px` : '0px',
                  background: STATUS_COLORS[status] || '#c1c7d0',
                  width: '100%',
                }}
              />
              <span style={{ fontSize: 9, color: '#6b778c', textAlign: 'center', wordBreak: 'break-all' }}>
                {status.replace(/_/g, ' ')}
              </span>
            </div>
          ))}
        </div>
      </div>
    );
  }

  function renderTrendChart(trend: { label: string; count: number }[]) {
    if (!trend || trend.length === 0) return <p style={{ color: '#97a0af', fontStyle: 'italic' }}>No trend data</p>;
    const maxCount = Math.max(...trend.map(t => t.count));

    return (
      <div className="ads-chart-placeholder" style={{ height: 120, alignItems: 'flex-end' }}>
        {trend.map((t, i) => (
          <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
            <span style={{ fontSize: 10, fontWeight: 600, color: '#172b4d' }}>{t.count}</span>
            <div
              className="ads-chart-bar"
              style={{
                height: maxCount > 0 ? `${(t.count / maxCount) * 100}px` : '0px',
                background: '#0052cc',
                width: '100%',
              }}
            />
            <span style={{ fontSize: 9, color: '#6b778c' }}>{t.label}</span>
          </div>
        ))}
      </div>
    );
  }

  function renderSeverityDonut(severity: Record<string, number>) {
    const entries = Object.entries(severity);
    const total = entries.reduce((sum, [, c]) => sum + c, 0);
    if (total === 0) return <p style={{ color: '#97a0af', fontStyle: 'italic' }}>No defects</p>;

    const colors = ['#de350b', '#ff5630', '#ff8b00', '#ffab00', '#36b37e'];

    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
        {/* Simple visual representation */}
        <div style={{ position: 'relative', width: 100, height: 100 }}>
          <svg viewBox="0 0 36 36" style={{ width: 100, height: 100 }}>
            {(() => {
              let offset = 0;
              return entries.map(([label, count], i) => {
                const pct = (count / total) * 100;
                const dashArray = `${pct} ${100 - pct}`;
                const el = (
                  <circle
                    key={label}
                    cx="18" cy="18" r="15.915"
                    fill="none"
                    stroke={colors[i % colors.length]}
                    strokeWidth="3"
                    strokeDasharray={dashArray}
                    strokeDashoffset={-offset}
                    transform="rotate(-90 18 18)"
                  />
                );
                offset += pct;
                return el;
              });
            })()}
          </svg>
          <div style={{
            position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)',
            fontSize: 16, fontWeight: 700, color: '#172b4d',
          }}>
            {total}
          </div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {entries.map(([label, count], i) => (
            <span key={label} style={{ fontSize: 12, display: 'flex', alignItems: 'center', gap: 6 }}>
              <span style={{ width: 10, height: 10, borderRadius: 2, background: colors[i % colors.length], display: 'inline-block' }} />
              {label}: {count}
            </span>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">V&V Dashboard</h1>
          <p className="ads-page-subtitle">Project overview and key metrics</p>
        </div>
        <div className="ads-toolbar">
          <input
            className="ads-search-input"
            placeholder="Project ID"
            value={projectInput}
            onChange={e => setProjectInput(e.target.value)}
          />
          <button className="ads-btn ads-btn--primary" onClick={handleLoad}>Load</button>
        </div>
      </div>

      {error && <div className="ads-alert ads-alert--error">{error}</div>}

      {loading && projectId && (
        <div className="ads-loading"><div className="ab-spinner" /> Loading dashboard...</div>
      )}

      {!loading && !dashboard && !error && (
        <div className="ads-card" style={{ textAlign: 'center', padding: 40 }}>
          <p style={{ color: '#6b778c' }}>Enter a Project ID and click Load to view the dashboard.</p>
        </div>
      )}

      {dashboard && (() => {
        const vvo = dashboard.vvoMetrics || {};
        const te = dashboard.techEventMetrics || {};
        const bd = dashboard.benchDefectMetrics || {};
        const pr = dashboard.problemReportMetrics || {};
        const totalVvos = vvo.total ?? dashboard.totalVvos ?? 0;
        const verifiedCount = vvo.verifiedCount ?? dashboard.verifiedCount ?? 0;
        const releasedCount = vvo.releasedCount ?? dashboard.releasedCount ?? 0;
        const openTechEvents = te.openCount ?? dashboard.openTechEvents ?? 0;
        const blockingBenchDefects = bd.blockingCount ?? dashboard.blockingBenchDefects ?? 0;
        const openProblemReports = pr.openCount ?? dashboard.openProblemReports ?? 0;
        const coveragePercentage = dashboard.coveragePercentage ?? 0;
        return (
        <>
          {/* KPI Cards */}
          <div className="ads-stats">
            <div className="ads-stat ads-stat--brand">
              <span className="ads-stat-value">{totalVvos}</span>
              <span className="ads-stat-label">Total VVOs</span>
            </div>
            <div className="ads-stat ads-stat--success">
              <span className="ads-stat-value">{verifiedCount}</span>
              <span className="ads-stat-label">Verified</span>
            </div>
            <div className="ads-stat ads-stat--success">
              <span className="ads-stat-value">{releasedCount}</span>
              <span className="ads-stat-label">Released</span>
            </div>
            <div className="ads-stat ads-stat--warning">
              <span className="ads-stat-value">{openTechEvents}</span>
              <span className="ads-stat-label">Open Tech Events</span>
            </div>
            <div className="ads-stat ads-stat--danger">
              <span className="ads-stat-value">{blockingBenchDefects}</span>
              <span className="ads-stat-label">Blocking Defects</span>
            </div>
            <div className="ads-stat ads-stat--danger">
              <span className="ads-stat-value">{openProblemReports}</span>
              <span className="ads-stat-label">Open PRs</span>
            </div>
          </div>

          {/* Coverage Gauge */}
          <div className="ads-card" style={{ marginBottom: 16 }}>
            <h3 className="ads-card-title">Coverage</h3>
            <div className="ads-gauge">
              <div className="ads-gauge-bar">
                <div
                  className={`ads-gauge-fill${coveragePercentage >= 80 ? ' ads-gauge-fill--success' : coveragePercentage >= 50 ? ' ads-gauge-fill--warning' : ' ads-gauge-fill--danger'}`}
                  style={{ width: `${Math.min(coveragePercentage, 100)}%` }}
                />
              </div>
              <span className="ads-gauge-label">{coveragePercentage}%</span>
            </div>
          </div>

          {/* Charts */}
          <div className="ads-grid-3">
            <div className="ads-card">
              <h3 className="ads-card-title">VVO Status Distribution</h3>
              {dashboard.vvoStatusDistribution
                ? renderStatusChart(dashboard.vvoStatusDistribution)
                : <p style={{ color: '#97a0af', fontStyle: 'italic' }}>No data</p>
              }
            </div>
            <div className="ads-card">
              <h3 className="ads-card-title">Tech Event Trend</h3>
              {dashboard.techEventTrend
                ? renderTrendChart(dashboard.techEventTrend)
                : <p style={{ color: '#97a0af', fontStyle: 'italic' }}>No data</p>
              }
            </div>
            <div className="ads-card">
              <h3 className="ads-card-title">Bench Defect Severity</h3>
              {dashboard.benchDefectSeverity
                ? renderSeverityDonut(dashboard.benchDefectSeverity)
                : <p style={{ color: '#97a0af', fontStyle: 'italic' }}>No data</p>
              }
            </div>
          </div>

          {/* Recent Activity */}
          <div className="ads-card" style={{ marginTop: 16 }}>
            <h3 className="ads-card-title">Recent Activity</h3>
            {dashboard.recentActivity && dashboard.recentActivity.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {dashboard.recentActivity.map(activity => (
                  <div key={activity.id} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '6px 0', borderBottom: '1px solid #f4f5f7' }}>
                    <span className={`ads-badge ads-badge--${activity.type === 'VVO' ? 'new' : activity.type === 'TECH_EVENT' ? 'to_be_verified' : 'cancelled'}`}>
                      {activity.type}
                    </span>
                    <span style={{ flex: 1, fontSize: 13, color: '#172b4d' }}>{activity.summary}</span>
                    <span style={{ fontSize: 12, color: '#6b778c' }}>{activity.user}</span>
                    <span style={{ fontSize: 11, color: '#97a0af' }}>
                      {activity.timestamp ? new Date(activity.timestamp).toLocaleString() : ''}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ color: '#97a0af', fontStyle: 'italic', fontSize: 13 }}>No recent activity.</p>
            )}
          </div>

          {/* Quick Links */}
          <div className="ads-card" style={{ marginTop: 16 }}>
            <h3 className="ads-card-title">Quick Links</h3>
            <div className="ads-toolbar">
              <Link className="ads-btn" to={`/aircraft-design/vvos?projectId=${projectInput}`}>View All VVOs</Link>
              <Link className="ads-btn" to={`/aircraft-design/tech-events?projectId=${projectInput}`}>View Tech Events</Link>
              <Link className="ads-btn" to={`/aircraft-design/baselines?projectId=${projectInput}`}>Manage Baselines</Link>
              <Link className="ads-btn" to={`/aircraft-design/master-data`}>Master Data Admin</Link>
            </div>
          </div>
        </>
        );
      })()}
    </div>
  );
}
