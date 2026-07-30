import React from 'react';
import { Link } from 'react-router-dom';
import type { IssueResponse } from '../../api/issueApi';
import type { HealthLevel, WorkMetrics } from './metrics';
import { HEALTH_LABELS, formatRelativeDate, issueStatusLabel } from './metrics';
import type { RecentView } from './recentViews';

/* ── Health badge ── */
export function HealthBadge({ health }: { health: HealthLevel }) {
  return (
    <span className={`ws-health ws-health--${health}`} title={HEALTH_LABELS[health]}>
      <span className="ws-health-dot" aria-hidden />
      {HEALTH_LABELS[health]}
    </span>
  );
}

/* ── KPI metric card ── */
export interface KpiCardProps {
  label: string;
  value: React.ReactNode;
  hint?: string;
  trend?: 'up' | 'down' | 'neutral';
  accent?: 'default' | 'success' | 'warning' | 'danger' | 'brand';
}

export function KpiCard({ label, value, hint, accent = 'default' }: KpiCardProps) {
  return (
    <div className={`ws-kpi ws-kpi--${accent}`}>
      <span className="ws-kpi-label">{label}</span>
      <span className="ws-kpi-value">{value}</span>
      {hint && <span className="ws-kpi-hint">{hint}</span>}
    </div>
  );
}

/* ── Progress bar ── */
export function ProgressBar({ value, label }: { value: number; label?: string }) {
  const pct = Math.min(100, Math.max(0, value));
  return (
    <div className="ws-progress">
      {label && (
        <div className="ws-progress-header">
          <span>{label}</span>
          <span className="ws-progress-pct">{pct}%</span>
        </div>
      )}
      <div className="ws-progress-track" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className="ws-progress-fill" style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

/* ── Work distribution ── */
export function WorkDistribution({ metrics }: { metrics: WorkMetrics }) {
  const total = metrics.total || 1;
  const segments = [
    { key: 'done', count: metrics.done, className: 'ws-dist-done', label: 'Done' },
    { key: 'ip', count: metrics.inProgress, className: 'ws-dist-ip', label: 'In progress' },
    { key: 'todo', count: metrics.todo, className: 'ws-dist-todo', label: 'To do' },
  ];
  return (
    <div className="ws-distribution">
      <div className="ws-distribution-bar">
        {segments.map((s) =>
          s.count > 0 ? (
            <div
              key={s.key}
              className={`ws-distribution-seg ${s.className}`}
              style={{ width: `${(s.count / total) * 100}%` }}
              title={`${s.label}: ${s.count}`}
            />
          ) : null
        )}
      </div>
      <div className="ws-distribution-legend">
        {segments.map((s) => (
          <span key={s.key} className="ws-distribution-legend-item">
            <span className={`ws-distribution-dot ${s.className}`} />
            {s.label} <strong>{s.count}</strong>
          </span>
        ))}
      </div>
    </div>
  );
}

/* ── Section panel ── */
export interface SectionPanelProps {
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
  children: React.ReactNode;
  collapsible?: boolean;
  defaultOpen?: boolean;
}

export function SectionPanel({ title, subtitle, action, children, collapsible, defaultOpen = true }: SectionPanelProps) {
  const [open, setOpen] = React.useState(defaultOpen);
  return (
    <section className="ws-panel">
      <header className="ws-panel-header">
        <div className="ws-panel-header-text">
          {collapsible ? (
            <button type="button" className="ws-panel-toggle" onClick={() => setOpen(!open)} aria-expanded={open}>
              <span className={`ws-chevron ${open ? 'ws-chevron--open' : ''}`} aria-hidden>›</span>
              <h3>{title}</h3>
            </button>
          ) : (
            <h3>{title}</h3>
          )}
          {subtitle && <p>{subtitle}</p>}
        </div>
        {action && <div className="ws-panel-action">{action}</div>}
      </header>
      {(!collapsible || open) && <div className="ws-panel-body">{children}</div>}
    </section>
  );
}

/* ── Quick nav tabs (project/program context) ── */
export interface QuickNavItem {
  label: string;
  path?: string;
  onClick?: () => void;
  active?: boolean;
  icon?: React.ReactNode;
}

export function QuickNavTabs({ items }: { items: QuickNavItem[] }) {
  return (
    <nav className="ws-quick-nav" aria-label="Workspace sections">
      {items.map((item) =>
        item.path ? (
          <Link key={item.label} to={item.path} className={`ws-quick-nav-item ${item.active ? 'ws-quick-nav-item--active' : ''}`}>
            {item.icon}
            {item.label}
          </Link>
        ) : (
          <button
            key={item.label}
            type="button"
            className={`ws-quick-nav-item ${item.active ? 'ws-quick-nav-item--active' : ''}`}
            onClick={item.onClick}
          >
            {item.icon}
            {item.label}
          </button>
        )
      )}
    </nav>
  );
}

/* ── Context action bar ── */
export function ContextActionBar({ children }: { children: React.ReactNode }) {
  return <div className="ws-context-actions">{children}</div>;
}

/* ── Activity feed ── */
export function ActivityFeed({ issues, limit = 6 }: { issues: IssueResponse[]; limit?: number }) {
  const sorted = [...issues]
    .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
    .slice(0, limit);

  if (!sorted.length) {
    return <p className="ws-muted">No recent activity yet.</p>;
  }

  return (
    <ul className="ws-activity-list">
      {sorted.map((issue) => (
        <li key={issue.id}>
          <Link to={`/issues/${issue.id}`} className="ws-activity-item">
            <span className="ws-activity-key">{issue.issueKey}</span>
            <span className="ws-activity-title">{issue.title}</span>
            <span className="ws-activity-meta">
              <span className="ws-status-pill">{issueStatusLabel(issue) || '—'}</span>
              <time dateTime={issue.updatedAt}>{formatRelativeDate(issue.updatedAt)}</time>
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}

/* ── Risks & blockers list ── */
function isIssueDone(issue: IssueResponse): boolean {
  const status = issueStatusLabel(issue).toLowerCase();
  if (!status) return false;
  return ['done', 'closed', 'resolved', 'complete', 'completed'].some((d) => status.includes(d));
}

export function RisksBlockers({ issues }: { issues: IssueResponse[] }) {
  const blockers = issues.filter((i) => {
    const p = (i.priority || '').toLowerCase();
    return !isIssueDone(i) && ['highest', 'critical', 'blocker', 'high'].includes(p);
  }).slice(0, 5);

  const overdue = issues.filter((i) => {
    if (!i.dueDate) return false;
    return !isIssueDone(i) && new Date(i.dueDate) < new Date();
  }).slice(0, 5);

  if (!blockers.length && !overdue.length) {
    return (
      <div className="ws-empty-inline">
        <span className="ws-empty-inline-icon">✓</span>
        <span>No active blockers or overdue work</span>
      </div>
    );
  }

  return (
    <div className="ws-risks">
      {blockers.length > 0 && (
        <div className="ws-risks-group">
          <h4>Blockers</h4>
          <ul>
            {blockers.map((i) => (
              <li key={i.id}>
                <Link to={`/issues/${i.id}`}>
                  <span className="ws-risk-key">{i.issueKey}</span>
                  {i.title}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
      {overdue.length > 0 && (
        <div className="ws-risks-group">
          <h4>Overdue</h4>
          <ul>
            {overdue.map((i) => (
              <li key={i.id}>
                <Link to={`/issues/${i.id}`}>
                  <span className="ws-risk-key">{i.issueKey}</span>
                  {i.title}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

/* ── Recently viewed strip ── */
export function RecentlyViewed({ items, title = 'Recently viewed' }: { items: RecentView[]; title?: string }) {
  if (!items.length) return null;
  return (
    <div className="ws-recent">
      <span className="ws-recent-label">{title}</span>
      <div className="ws-recent-items">
        {items.map((item) => (
          <Link key={`${item.type}-${item.id}`} to={item.path} className="ws-recent-chip">
            <span className={`ws-recent-type ws-recent-type--${item.type}`}>{item.type === 'project' ? 'P' : 'G'}</span>
            {item.name}
          </Link>
        ))}
      </div>
    </div>
  );
}

/* ── Sticky workspace header ── */
export interface WorkspaceHeaderProps {
  breadcrumbs?: React.ReactNode;
  title: string;
  subtitle?: React.ReactNode;
  badges?: React.ReactNode;
  meta?: React.ReactNode;
  actions?: React.ReactNode;
}

export function WorkspaceHeader({ breadcrumbs, title, subtitle, badges, meta, actions }: WorkspaceHeaderProps) {
  return (
    <header className="ws-header ws-header--sticky">
      {breadcrumbs && <div className="ws-breadcrumbs">{breadcrumbs}</div>}
      <div className="ws-header-main">
        <div className="ws-header-info">
          <div className="ws-header-title-row">
            <h1>{title}</h1>
            {badges}
          </div>
          {subtitle && <p className="ws-header-subtitle">{subtitle}</p>}
          {meta && <div className="ws-header-meta">{meta}</div>}
        </div>
        {actions && <div className="ws-header-actions">{actions}</div>}
      </div>
    </header>
  );
}

/* ── Portfolio summary row ── */
export function PortfolioSummary({ children }: { children: React.ReactNode }) {
  return <div className="ws-portfolio-summary">{children}</div>;
}

/* ── Entity avatar ── */
export function EntityAvatar({ name, size = 'md' }: { name: string; size?: 'sm' | 'md' | 'lg' }) {
  return (
    <div className={`ws-avatar ws-avatar--${size}`} aria-hidden>
      {name.charAt(0).toUpperCase()}
    </div>
  );
}
