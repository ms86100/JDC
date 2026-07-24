import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import CreatePlanProgramSelector from '../../plans/components/CreatePlanProgramSelector';
import { useAuth } from '../../auth/context/AuthContext';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import { sprintApi, SprintResponse } from '../../../api/sprintApi';
import { auditApi, AuditLogResponse } from '../../../api/serviceApi';
import '../DashboardStyles.css';

function statusCategory(status?: string): 'done' | 'inprogress' | 'todo' {
  if (!status) return 'todo';
  const s = status.toLowerCase();
  if (/done|closed|resolved|complete/i.test(s)) return 'done';
  if (/progress|review|development|active/i.test(s)) return 'inprogress';
  return 'todo';
}

function priorityClass(p?: string): string {
  if (!p) return '';
  const l = p.toLowerCase();
  if (l === 'highest' || l === 'critical') return 'jdc-dash-priority--highest';
  if (l === 'high') return 'jdc-dash-priority--high';
  if (l === 'medium') return 'jdc-dash-priority--medium';
  if (l === 'low') return 'jdc-dash-priority--low';
  if (l === 'lowest') return 'jdc-dash-priority--lowest';
  return '';
}

function shortDate(iso?: string): string {
  if (!iso) return '';
  try { return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }); }
  catch { return ''; }
}

function relativeTime(iso?: string): string {
  if (!iso) return '';
  try {
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return `${days}d ago`;
  } catch { return ''; }
}

function activityDotClass(action?: string): string {
  if (!action) return 'jdc-dash-activity-dot--default';
  const a = action.toLowerCase();
  if (a.includes('create') || a.includes('add')) return 'jdc-dash-activity-dot--create';
  if (a.includes('delete') || a.includes('remove')) return 'jdc-dash-activity-dot--delete';
  if (a.includes('update') || a.includes('edit') || a.includes('transition') || a.includes('change'))
    return 'jdc-dash-activity-dot--update';
  return 'jdc-dash-activity-dot--default';
}

function IssueRow({ issue }: { issue: IssueResponse }) {
  return (
    <div className="jdc-dash-issue">
      <span className={`jdc-dash-priority ${priorityClass(issue.priority)}`} />
      <Link className="jdc-dash-issue-key" to={`/issues/${issue.id}`}>
        {issue.issueKey ?? issue.id.slice(0, 8)}
      </Link>
      <span className="jdc-dash-issue-title">{issue.title}</span>
    </div>
  );
}

interface LaneProps {
  label: string;
  kind: 'todo' | 'inprogress' | 'done';
  issues: IssueResponse[];
  defaultOpen?: boolean;
}

function Lane({ label, kind, issues, defaultOpen = true }: LaneProps) {
  const [open, setOpen] = useState(defaultOpen);
  const display = issues.slice(0, 8);

  return (
    <div className="jdc-dash-lane">
      <button type="button" className="jdc-dash-lane-head" onClick={() => setOpen(o => !o)}>
        <span className={`jdc-dash-lane-chevron${open ? ' jdc-dash-lane-chevron--open' : ''}`}>&#9654;</span>
        <span className="jdc-dash-lane-name">{label}</span>
        <span className={`jdc-dash-lane-badge jdc-dash-lane-badge--${kind}`}>{issues.length}</span>
      </button>
      {open && display.map(issue => <IssueRow key={issue.id} issue={issue} />)}
    </div>
  );
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [showCreateSelector, setShowCreateSelector] = useState(false);

  useEffect(() => {
    const onCreate = () => setShowCreateSelector(true);
    window.addEventListener('openCreatePlanProgram', onCreate);
    return () => window.removeEventListener('openCreatePlanProgram', onCreate);
  }, []);

  const { data: allIssues = [], isLoading: issuesLoading } = useQuery({
    queryKey: ['dash-issues'],
    queryFn: () => issueApi.getAll().then(r => {
      const d = r.data;
      return Array.isArray(d) ? d : (d as { content: IssueResponse[] })?.content ?? [];
    }),
    staleTime: 30_000,
  });

  const { data: projects = [], isLoading: projectsLoading } = useQuery({
    queryKey: ['dash-projects'],
    queryFn: () => projectApi.getAll(),
    staleTime: 30_000,
  });

  const { data: sprints = [], isLoading: sprintsLoading } = useQuery({
    queryKey: ['dash-sprints'],
    queryFn: () => sprintApi.getAll(),
    staleTime: 30_000,
  });

  const { data: activityLogs = [], isLoading: activityLoading } = useQuery({
    queryKey: ['dash-activity'],
    queryFn: () => auditApi.getLogs({ page: 0, size: 10 }).then(r => {
      const d = r.data;
      return Array.isArray(d) ? d : (d as { content: AuditLogResponse[] })?.content ?? [];
    }),
    staleTime: 30_000,
  });

  const myIssues = useMemo(() => {
    if (!user) return [];
    return allIssues.filter(
      i => i.assigneeId === user.userId || i.assigneeName === user.username,
    );
  }, [allIssues, user]);

  const myOpen = useMemo(() => myIssues.filter(i => statusCategory(i.status) !== 'done'), [myIssues]);
  const overdue = useMemo(() => {
    const now = Date.now();
    return myOpen.filter(i => i.dueDate && new Date(i.dueDate).getTime() < now);
  }, [myOpen]);

  const lanes = useMemo(() => {
    const todo: IssueResponse[] = [];
    const inprog: IssueResponse[] = [];
    const done: IssueResponse[] = [];
    for (const i of myIssues) {
      const cat = statusCategory(i.status);
      if (cat === 'done') done.push(i);
      else if (cat === 'inprogress') inprog.push(i);
      else todo.push(i);
    }
    return { todo, inprog, done };
  }, [myIssues]);

  const activeSprints = useMemo(
    () => sprints.filter(s => s.status === 'ACTIVE'),
    [sprints],
  );

  const issueDist = useMemo(() => {
    let todo = 0, inprog = 0, done = 0;
    for (const i of allIssues) {
      const c = statusCategory(i.status);
      if (c === 'done') done++;
      else if (c === 'inprogress') inprog++;
      else todo++;
    }
    return { todo, inprog, done, total: allIssues.length };
  }, [allIssues]);

  const priorityDist = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const i of allIssues) {
      const p = (i.priority ?? 'Unset').toLowerCase();
      counts[p] = (counts[p] ?? 0) + 1;
    }
    const order = ['highest', 'high', 'medium', 'low', 'lowest'];
    const result: { label: string; count: number; color: string }[] = [];
    for (const p of order) {
      if (counts[p]) {
        const colors: Record<string, string> = { highest: '#ff0000', high: '#ff6600', medium: '#ffcc00', low: '#0099ff', lowest: '#99cc00' };
        result.push({ label: p.charAt(0).toUpperCase() + p.slice(1), count: counts[p], color: colors[p] ?? '#a5adba' });
      }
    }
    return result;
  }, [allIssues]);

  const upcomingDue = useMemo(() => {
    const now = Date.now();
    const weekFromNow = now + 7 * 86400000;
    return allIssues
      .filter(i => {
        if (!i.dueDate) return false;
        const cat = statusCategory(i.status);
        if (cat === 'done') return false;
        const d = new Date(i.dueDate).getTime();
        return d <= weekFromNow;
      })
      .sort((a, b) => new Date(a.dueDate!).getTime() - new Date(b.dueDate!).getTime())
      .slice(0, 8);
  }, [allIssues]);

  const dateStr = new Date().toLocaleDateString(undefined, {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
  });

  const isLoading = issuesLoading && projectsLoading;

  if (isLoading) {
    return (
      <div className="jdc-dash">
        <div className="jdc-dash-loading">
          <div className="ab-spinner" />
          <span>Loading dashboard…</span>
        </div>
      </div>
    );
  }

  return (
    <div className="jdc-dash">
      {/* ---- Header ---- */}
      <div className="jdc-dash-header">
        <h1 className="jdc-dash-greeting">
          Welcome back, {user?.username ?? 'User'}
        </h1>
        <p className="jdc-dash-date">{dateStr}</p>
      </div>

      {/* ---- Stat Tiles ---- */}
      <div className="jdc-dash-stats">
        <div className="jdc-dash-stat jdc-dash-stat--brand">
          <span className="jdc-dash-stat-value">{myOpen.length}</span>
          <span className="jdc-dash-stat-label">My Open Issues</span>
        </div>
        <div className="jdc-dash-stat">
          <span className="jdc-dash-stat-value">{projects.length}</span>
          <span className="jdc-dash-stat-label">Projects</span>
        </div>
        <div className="jdc-dash-stat jdc-dash-stat--success">
          <span className="jdc-dash-stat-value">{activeSprints.length}</span>
          <span className="jdc-dash-stat-label">Active Sprints</span>
        </div>
        <div className={`jdc-dash-stat${overdue.length > 0 ? ' jdc-dash-stat--danger' : ''}`}>
          <span className="jdc-dash-stat-value">{overdue.length}</span>
          <span className="jdc-dash-stat-label">Overdue</span>
        </div>
      </div>

      {/* ---- Quick Actions ---- */}
      <div className="jdc-dash-actions">
        <Link className="jdc-dash-action" to="/projects">
          <span className="jdc-dash-action-icon">+</span> Create Issue
        </Link>
        <Link className="jdc-dash-action" to="/projects">
          <span className="jdc-dash-action-icon">&#9776;</span> Browse Projects
        </Link>
        <button
          type="button"
          className="jdc-dash-action"
          onClick={() => window.dispatchEvent(new Event('openCreatePlanProgram'))}
        >
          <span className="jdc-dash-action-icon">&#9670;</span> New Plan / Program
        </button>
      </div>

      {/* ---- Issue Distribution ---- */}
      {allIssues.length > 0 && (
        <div className="jdc-dash-distrib">
          <div className="jdc-dash-distrib-card">
            <h3 className="jdc-dash-distrib-title">Issue Distribution</h3>
            <div className="jdc-dash-bar-row">
              {issueDist.todo > 0 && (
                <div
                  className="jdc-dash-bar-seg jdc-dash-bar-seg--todo"
                  style={{ width: `${(issueDist.todo / issueDist.total) * 100}%` }}
                />
              )}
              {issueDist.inprog > 0 && (
                <div
                  className="jdc-dash-bar-seg jdc-dash-bar-seg--inprogress"
                  style={{ width: `${(issueDist.inprog / issueDist.total) * 100}%` }}
                />
              )}
              {issueDist.done > 0 && (
                <div
                  className="jdc-dash-bar-seg jdc-dash-bar-seg--done"
                  style={{ width: `${(issueDist.done / issueDist.total) * 100}%` }}
                />
              )}
            </div>
            <div className="jdc-dash-bar-legend">
              <span className="jdc-dash-bar-legend-item">
                <span className="jdc-dash-bar-legend-dot jdc-dash-bar-legend-dot--todo" />
                To Do <span className="jdc-dash-bar-legend-count">{issueDist.todo}</span>
              </span>
              <span className="jdc-dash-bar-legend-item">
                <span className="jdc-dash-bar-legend-dot jdc-dash-bar-legend-dot--inprogress" />
                In Progress <span className="jdc-dash-bar-legend-count">{issueDist.inprog}</span>
              </span>
              <span className="jdc-dash-bar-legend-item">
                <span className="jdc-dash-bar-legend-dot jdc-dash-bar-legend-dot--done" />
                Done <span className="jdc-dash-bar-legend-count">{issueDist.done}</span>
              </span>
            </div>
            {priorityDist.length > 0 && (
              <div className="jdc-dash-priority-row">
                {priorityDist.map(p => (
                  <span key={p.label} className="jdc-dash-priority-chip">
                    <span className="jdc-dash-priority-dot" style={{ background: p.color }} />
                    {p.label} <span className="jdc-dash-priority-cnt">{p.count}</span>
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ---- Body ---- */}
      <div className="jdc-dash-body">
        {/* Left column */}
        <div className="jdc-dash-col">
          {/* My Work */}
          <div className="jdc-dash-card">
            <h3 className="jdc-dash-card-title">My Work</h3>
            <div className="jdc-dash-card-body">
              {myIssues.length === 0 ? (
                <div className="jdc-dash-card-empty">No issues assigned to you.</div>
              ) : (
                <>
                  <Lane label="To Do" kind="todo" issues={lanes.todo} />
                  <Lane label="In Progress" kind="inprogress" issues={lanes.inprog} />
                  <Lane label="Done" kind="done" issues={lanes.done} defaultOpen={false} />
                </>
              )}
            </div>
          </div>

          {/* Projects */}
          <div className="jdc-dash-card">
            <h3 className="jdc-dash-card-title">Projects</h3>
            <div className="jdc-dash-card-body">
              {projects.length === 0 ? (
                <div className="jdc-dash-card-empty">No projects found.</div>
              ) : (
                (projects as ProjectResponse[]).slice(0, 6).map(p => (
                  <Link key={p.id} className="jdc-dash-project" to={`/projects/${p.id}`}>
                    <span className="jdc-dash-project-key">
                      {(p.projectKey ?? p.name ?? '').slice(0, 3).toUpperCase()}
                    </span>
                    <div className="jdc-dash-project-info">
                      <div className="jdc-dash-project-name">{p.name}</div>
                      <div className="jdc-dash-project-meta">
                        {p.projectKey} · {p.issueCounter ?? 0} issues
                      </div>
                    </div>
                  </Link>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Right column */}
        <div className="jdc-dash-col">
          {/* Active Sprints */}
          <div className="jdc-dash-card">
            <h3 className="jdc-dash-card-title">Active Sprints</h3>
            <div className="jdc-dash-card-body">
              {sprintsLoading ? (
                <div className="jdc-dash-loading"><div className="ab-spinner" /></div>
              ) : activeSprints.length === 0 ? (
                <div className="jdc-dash-card-empty">No active sprints.</div>
              ) : (
                activeSprints.map(s => {
                  const total = s.issueCount ?? 0;
                  const completed = s.completedIssueCount ?? 0;
                  const pct = total > 0 ? Math.round((completed / total) * 100) : 0;
                  return (
                    <div key={s.id} className="jdc-dash-sprint">
                      <div className="jdc-dash-sprint-top">
                        <span className="jdc-dash-sprint-name">{s.name}</span>
                        <span className="jdc-dash-sprint-dates">
                          {shortDate(s.startDate)} – {shortDate(s.endDate)}
                        </span>
                      </div>
                      <div className="jdc-dash-sprint-bar">
                        <div
                          className={`jdc-dash-sprint-fill${pct >= 100 ? ' jdc-dash-sprint-fill--done' : ''}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                      <span className="jdc-dash-sprint-count">
                        {completed}/{total} issues · {pct}%
                      </span>
                    </div>
                  );
                })
              )}
            </div>
          </div>

          {/* Upcoming Due Dates */}
          {upcomingDue.length > 0 && (
            <div className="jdc-dash-card">
              <h3 className="jdc-dash-card-title">Due This Week</h3>
              <div className="jdc-dash-card-body">
                {upcomingDue.map(issue => {
                  const dueMs = new Date(issue.dueDate!).getTime();
                  const nowMs = Date.now();
                  const daysLeft = Math.ceil((dueMs - nowMs) / 86400000);
                  let dateClass = 'jdc-dash-due-date--normal';
                  let dateLabel = shortDate(issue.dueDate);
                  if (daysLeft < 0) { dateClass = 'jdc-dash-due-date--urgent'; dateLabel = `${Math.abs(daysLeft)}d overdue`; }
                  else if (daysLeft <= 2) { dateClass = 'jdc-dash-due-date--soon'; dateLabel = daysLeft === 0 ? 'Today' : daysLeft === 1 ? 'Tomorrow' : dateLabel; }
                  return (
                    <div key={issue.id} className="jdc-dash-due">
                      <span className={`jdc-dash-due-date ${dateClass}`}>{dateLabel}</span>
                      <Link className="jdc-dash-due-key" to={`/issues/${issue.id}`}>
                        {issue.issueKey ?? issue.id.slice(0, 8)}
                      </Link>
                      <span className="jdc-dash-due-title">{issue.title}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Activity Stream */}
          <div className="jdc-dash-card">
            <h3 className="jdc-dash-card-title">Recent Activity</h3>
            <div className="jdc-dash-card-body">
              {activityLoading ? (
                <div className="jdc-dash-loading"><div className="ab-spinner" /></div>
              ) : activityLogs.length === 0 ? (
                <div className="jdc-dash-card-empty">No recent activity.</div>
              ) : (
                activityLogs.map((log: AuditLogResponse) => (
                  <div key={log.id} className="jdc-dash-activity">
                    <span className={`jdc-dash-activity-dot ${activityDotClass(log.action)}`} />
                    <div className="jdc-dash-activity-body">
                      <div className="jdc-dash-activity-text">
                        <span className="jdc-dash-activity-user">{log.username ?? log.userId?.slice(0, 8)}</span>
                        {' '}{log.action?.toLowerCase()}{' '}
                        {log.entityType?.toLowerCase()}
                        {log.entityType === 'ISSUE' && log.entityId && (
                          <>{' '}<Link to={`/issues/${log.entityId}`}>{log.entityId.slice(0, 8)}…</Link></>
                        )}
                      </div>
                      <div className="jdc-dash-activity-time">{relativeTime(log.createdAt)}</div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {showCreateSelector && (
        <CreatePlanProgramSelector
          onClose={() => setShowCreateSelector(false)}
          onSelect={(type) => {
            setShowCreateSelector(false);
            navigate(type === 'plan' ? '/plans/create' : '/programs/create');
          }}
        />
      )}
    </div>
  );
}
