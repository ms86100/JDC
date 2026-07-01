import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import './AdminReportsInsights.css';

type Range = '7d' | '30d' | '90d';

const ISSUE_TREND = [
  { label: 'Mon', pct: 45 },
  { label: 'Tue', pct: 62 },
  { label: 'Wed', pct: 78 },
  { label: 'Thu', pct: 55 },
  { label: 'Fri', pct: 88 },
  { label: 'Sat', pct: 30 },
  { label: 'Sun', pct: 25 },
];

const TOP_PROJECTS = [
  { name: 'AVN-Platform', issues: 342, trend: '+8%' },
  { name: 'Flight-Ops', issues: 218, trend: '+2%' },
  { name: 'Maintenance', issues: 156, trend: '-4%' },
  { name: 'Compliance', issues: 89, trend: '+12%' },
];

const INSIGHTS = [
  {
    title: 'Resolution rate improved',
    detail: '42% of issues closed within 7 days — up from 36% last period.',
  },
  {
    title: 'Peak activity Wednesday',
    detail: 'Most creates and transitions occur mid-week; consider sprint planning alignment.',
  },
  {
    title: '3 projects without recent updates',
    detail: 'Archive or review inactive projects to keep dashboards accurate.',
  },
];

export default function InsightsPage() {
  const [range, setRange] = useState<Range>('30d');

  const rangeLabel = range === '7d' ? '7 days' : range === '30d' ? '30 days' : '90 days';

  return (
    <div className="dc-page ab-analytics-page">
      <header className="ab-analytics-hero">
        <h1>Insights</h1>
        <p>
          Aggregate analytics for adoption, issue flow, and project health — similar to Jira Data
          Center usage and workload insights for your instance.
        </p>
      </header>

      <div className="ab-analytics-toolbar">
        <div className="ab-analytics-toolbar-left">
          <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--sa-n600)' }}>Time range</span>
          <div className="ab-time-range" role="group" aria-label="Time range">
            {(
              [
                ['7d', '7 days'],
                ['30d', '30 days'],
                ['90d', '90 days'],
              ] as const
            ).map(([key, label]) => (
              <button
                key={key}
                type="button"
                className={range === key ? 'is-active' : ''}
                onClick={() => setRange(key)}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
        <div className="ab-analytics-toolbar-right">
          <button type="button" className="dc-btn dc-btn-secondary" onClick={() => alert('Export ready!')}>
            Export dashboard
          </button>
          <Link to="/admin/reports" className="dc-btn dc-btn-sm dc-btn-secondary" style={{ textDecoration: 'none' }}>
            Open reports
          </Link>
        </div>
      </div>

      <div className="ab-kpi-grid">
        <div className="ab-kpi-card">
          <div className="ab-kpi-label">Active users</div>
          <div className="ab-kpi-value">142</div>
          <span className="ab-kpi-trend up">↑ 6% · {rangeLabel}</span>
        </div>
        <div className="ab-kpi-card">
          <div className="ab-kpi-label">Issues created</div>
          <div className="ab-kpi-value">1,247</div>
          <span className="ab-kpi-trend up">↑ 14%</span>
        </div>
        <div className="ab-kpi-card">
          <div className="ab-kpi-label">Avg. cycle time</div>
          <div className="ab-kpi-value">4.2d</div>
          <span className="ab-kpi-trend down">↓ 0.8d faster</span>
        </div>
        <div className="ab-kpi-card">
          <div className="ab-kpi-label">Open backlog</div>
          <div className="ab-kpi-value">342</div>
          <span className="ab-kpi-trend neutral">Stable</span>
        </div>
      </div>

      <div className="ab-insights-grid">
        <section className="ab-chart-panel">
          <h3>Issues created (last 7 days)</h3>
          <div className="ab-bar-chart" role="img" aria-label="Bar chart of daily issue creation">
            {ISSUE_TREND.map((bar) => (
              <div key={bar.label} className="ab-bar-col">
                <div className="ab-bar-fill" style={{ height: `${bar.pct}%` }} />
                <span className="ab-bar-label">{bar.label}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="ab-chart-panel">
          <h3>Top projects by activity</h3>
          <table className="ab-recent-table">
            <thead>
              <tr>
                <th>Project</th>
                <th>Issues</th>
                <th>Trend</th>
              </tr>
            </thead>
            <tbody>
              {TOP_PROJECTS.map((p) => (
                <tr key={p.name}>
                  <td><strong>{p.name}</strong></td>
                  <td>{p.issues}</td>
                  <td>
                    <span
                      className={`ab-kpi-trend ${p.trend.startsWith('+') ? 'up' : p.trend.startsWith('-') ? 'down' : 'neutral'}`}
                      style={{ marginTop: 0 }}
                    >
                      {p.trend}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>

      <h2 className="ab-section-title">Recommended insights</h2>
      <div className="ab-chart-panel" style={{ marginBottom: 24 }}>
        <ul className="ab-insight-list">
          {INSIGHTS.map((item) => (
            <li key={item.title}>
              <span className="ab-insight-bullet" aria-hidden="true" />
              <div>
                <strong>{item.title}</strong>
                <span>{item.detail}</span>
              </div>
            </li>
          ))}
        </ul>
      </div>

      <div className="ab-insights-grid">
        <section className="ab-chart-panel">
          <h3>Issue status mix</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {[
              { label: 'To Do', pct: 28, color: 'var(--sa-n200)' },
              { label: 'In progress', pct: 35, color: 'var(--sa-brand-500)' },
              { label: 'Done', pct: 37, color: '#006644' },
            ].map((row) => (
              <div key={row.label}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
                  <span style={{ color: 'var(--sa-n700)', fontWeight: 500 }}>{row.label}</span>
                  <span style={{ color: 'var(--sa-n600)' }}>{row.pct}%</span>
                </div>
                <div style={{ height: 8, background: 'var(--sa-n100)', borderRadius: 4, overflow: 'hidden' }}>
                  <div
                    style={{
                      width: `${row.pct}%`,
                      height: '100%',
                      background: row.color,
                      borderRadius: 4,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="ab-chart-panel">
          <h3>Adoption</h3>
          <dl className="ab-info-dl" style={{ margin: 0 }}>
            {[
              ['Daily active users', '89'],
              ['Projects with activity', '18 / 24'],
              ['Boards in use', '12'],
              ['Automation rules fired', '1,204'],
            ].map(([label, value]) => (
              <div key={label} className="ab-info-row" style={{ gridTemplateColumns: '1fr auto' }}>
                <dt style={{ padding: '10px 0', background: 'none', border: 'none' }}>{label}</dt>
                <dd style={{ padding: '10px 0', fontWeight: 600 }}>{value}</dd>
              </div>
            ))}
          </dl>
        </section>
      </div>

      <p style={{ fontSize: 13, color: 'var(--sa-n600)' }}>
        Data is sampled for the selected period ({rangeLabel}). For exports and scheduled digests, use{' '}
        <Link to="/admin/reports" style={{ color: 'var(--sa-brand-500)', fontWeight: 500 }}>
          Reports
        </Link>
        .
      </p>
    </div>
  );
}
