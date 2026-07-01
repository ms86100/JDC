import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import './AdminReportsInsights.css';

type ReportFilter = 'all' | 'standard' | 'scheduled' | 'recent';

interface ReportDefinition {
  id: string;
  title: string;
  description: string;
  icon: string;
  category: 'standard' | 'scheduled';
  lastRun?: string;
  format: string;
}

const REPORT_CATALOG: ReportDefinition[] = [
  {
    id: 'issue-stats',
    title: 'Issue statistics',
    description: 'Created vs resolved issues by project, type, and priority over time.',
    icon: '📊',
    category: 'standard',
    lastRun: '20 May 2026, 09:15',
    format: 'CSV, PDF',
  },
  {
    id: 'user-workload',
    title: 'User workload',
    description: 'Assigned and unresolved issues per assignee for capacity planning.',
    icon: '👤',
    category: 'standard',
    lastRun: '19 May 2026, 14:30',
    format: 'CSV',
  },
  {
    id: 'project-activity',
    title: 'Project activity',
    description: 'Events and changes grouped by project for the selected period.',
    icon: '📁',
    category: 'standard',
    format: 'CSV, PDF',
  },
  {
    id: 'time-tracking',
    title: 'Time tracking',
    description: 'Logged work hours by user, issue, and project.',
    icon: '⏱',
    category: 'standard',
    lastRun: '18 May 2026, 08:00',
    format: 'CSV',
  },
  {
    id: 'sla',
    title: 'SLA goals',
    description: 'Service level agreement breaches and time-to-resolution metrics.',
    icon: '🎯',
    category: 'scheduled',
    lastRun: 'Weekly — Mondays 06:00',
    format: 'PDF',
  },
  {
    id: 'audit-export',
    title: 'Audit log export',
    description: 'Compliance export of administration and security events.',
    icon: '📋',
    category: 'scheduled',
    lastRun: 'Daily — 00:00',
    format: 'CSV',
  },
];

const RECENT_RUNS = [
  { name: 'Issue statistics', runBy: 'admin', when: 'Today, 09:15', status: 'ready' as const, size: '1.2 MB' },
  { name: 'User workload', runBy: 'jsmith', when: 'Yesterday', status: 'ready' as const, size: '840 KB' },
  { name: 'SLA goals', runBy: 'System', when: 'Mon 06:00', status: 'ready' as const, size: '2.1 MB' },
  { name: 'Project activity', runBy: 'admin', when: 'Running…', status: 'running' as const, size: '—' },
];

export default function ReportsPage() {
  const [filter, setFilter] = useState<ReportFilter>('all');
  const [search, setSearch] = useState('');
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [selectedReport, setSelectedReport] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  const filteredReports = useMemo(() => {
    let list = REPORT_CATALOG;
    if (filter === 'standard') list = list.filter((r) => r.category === 'standard');
    if (filter === 'scheduled') list = list.filter((r) => r.category === 'scheduled');
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (r) => r.title.toLowerCase().includes(q) || r.description.toLowerCase().includes(q)
      );
    }
    return list;
  }, [filter, search]);

  const handleGenerate = (reportId: string) => {
    setSelectedReport(reportId);
    setIsGenerating(true);
    setTimeout(() => {
      setIsGenerating(false);
      alert(`Report ${reportId} generated successfully!`);
    }, 1500);
  };

  const handleSchedule = () => {
    setShowScheduleModal(true);
  };

  const handleNewCustomReport = () => {
    alert('Custom report builder coming soon!');
  };

  return (
    <div className="dc-page ab-analytics-page">
      <header className="ab-analytics-hero">
        <h1>Reports</h1>
        <p>
          Generate standard Systems DC reports, schedule recurring exports, and download
          results for projects, issues, users, and compliance.
        </p>
      </header>

      <div className="ab-kpi-grid">
        <div className="ab-kpi-card">
          <div className="ab-kpi-label">Standard reports</div>
          <div className="ab-kpi-value">{REPORT_CATALOG.filter((r) => r.category === 'standard').length}</div>
        </div>
        <div className="ab-kpi-card">
          <div className="ab-kpi-label">Scheduled</div>
          <div className="ab-kpi-value">{REPORT_CATALOG.filter((r) => r.category === 'scheduled').length}</div>
        </div>
        <div className="ab-kpi-card">
          <div className="ab-kpi-label">Runs this month</div>
          <div className="ab-kpi-value">47</div>
          <span className="ab-kpi-trend up">↑ 12% vs last month</span>
        </div>
        <div className="ab-kpi-card">
          <div className="ab-kpi-label">Last export</div>
          <div className="ab-kpi-value" style={{ fontSize: 18 }}>Today</div>
          <span className="ab-kpi-trend neutral">Issue statistics</span>
        </div>
      </div>

      <div className="ab-analytics-toolbar">
        <div className="ab-analytics-toolbar-left">
          <div className="ab-filter-tabs" role="tablist" aria-label="Report filters">
            {(
              [
                ['all', 'All reports'],
                ['standard', 'Standard'],
                ['scheduled', 'Scheduled'],
                ['recent', 'Recent runs'],
              ] as const
            ).map(([key, label]) => (
              <button
                key={key}
                type="button"
                role="tab"
                aria-selected={filter === key}
                className={filter === key ? 'is-active' : ''}
                onClick={() => setFilter(key)}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
        <div className="ab-analytics-toolbar-right">
          <input
            type="search"
            className="admin-search-input-toolbar"
            placeholder="Search reports…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search reports"
            style={{ width: 220 }}
          />
          <button type="button" className="dc-btn dc-btn-secondary" onClick={handleSchedule}>
            Schedule report
          </button>
          <button type="button" className="dc-btn dc-btn-secondary" onClick={handleNewCustomReport}>
            New custom report
          </button>
        </div>
      </div>

      {filter !== 'recent' && (
        <>
          <h2 className="ab-section-title">Report catalog</h2>
          {filteredReports.length === 0 ? (
            <div className="ab-empty-state">
              <span className="ab-empty-state-icon" aria-hidden="true">📊</span>
              <h3>No reports match your search</h3>
              <p>Try a different keyword or clear the filter to see all available reports.</p>
            </div>
          ) : (
            <div className="ab-report-grid">
              {filteredReports.map((report) => (
                <article key={report.id} className="ab-report-card">
                  <div className="ab-report-card-top">
                    <span className="ab-report-icon" aria-hidden="true">
                      {report.icon}
                    </span>
                    <div>
                      <h3>{report.title}</h3>
                      <p>{report.description}</p>
                      {report.lastRun && (
                        <div className="ab-report-meta">
                          {report.category === 'scheduled' ? 'Schedule: ' : 'Last run: '}
                          {report.lastRun}
                        </div>
                      )}
                      <div className="ab-report-meta">Export: {report.format}</div>
                    </div>
                  </div>
                  <div className="ab-report-actions">
                    <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary" onClick={() => handleGenerate(report.id)} disabled={isGenerating}>
                      {isGenerating ? 'Generating…' : 'Generate'}
                    </button>
                    {report.category === 'scheduled' ? (
                      <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary">
                        Edit schedule
                      </button>
                    ) : (
                      <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary" onClick={handleSchedule}>
                        Schedule
                      </button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
        </>
      )}

      {(filter === 'all' || filter === 'recent') && (
        <>
          <h2 className="ab-section-title">Recent report runs</h2>
          <div className="ab-recent-table-wrap">
            <table className="ab-recent-table">
              <thead>
                <tr>
                  <th>Report</th>
                  <th>Run by</th>
                  <th>When</th>
                  <th>Status</th>
                  <th>Size</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {RECENT_RUNS.map((row) => (
                  <tr key={row.name + row.when}>
                    <td><strong>{row.name}</strong></td>
                    <td>{row.runBy}</td>
                    <td>{row.when}</td>
                    <td>
                      <span className={`ab-status-pill ${row.status}`}>
                        {row.status === 'ready' ? 'Ready' : 'Running'}
                      </span>
                    </td>
                    <td>{row.size}</td>
                    <td>
                      {row.status === 'ready' && (
                        <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary" onClick={() => alert('Download ready!')}>
                          Download
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <p style={{ marginTop: 24, fontSize: 13, color: 'var(--sa-n600)' }}>
        Need audit events? View detailed logs in{' '}
        <Link to="/admin/auditing" style={{ color: 'var(--sa-brand-500)', fontWeight: 500 }}>
          Auditing
        </Link>
        .
      </p>
    </div>
  );
}
