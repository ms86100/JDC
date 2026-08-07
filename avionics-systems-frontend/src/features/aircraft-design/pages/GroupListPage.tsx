import { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { DEMO_GROUPS } from '../demoData';
import '../AircraftDesignStyles.css';

const STATUSES = ['ALL', 'BACKLOG', 'TO_DO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'] as const;

export default function GroupListPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';
  const [groups, setGroups] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [search, setSearch] = useState('');

  useEffect(() => {
    setGroups(DEMO_GROUPS);
    setLoading(false);
  }, [projectId]);

  const filtered = useMemo(() => {
    let result = groups;
    if (statusFilter !== 'ALL') result = result.filter(g => g.status === statusFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(g => g.issueKey?.toLowerCase().includes(q) || g.summary?.toLowerCase().includes(q));
    }
    return result;
  }, [groups, statusFilter, search]);

  const doneCount = groups.filter(g => g.status === 'DONE').length;
  const inProgressCount = groups.filter(g => g.status === 'IN_PROGRESS').length;
  const totalDeliverables = groups.reduce((sum, g) => sum + (g.childDeliverableCount || 0), 0);

  if (loading) return <div className="ads-page"><div className="ads-loading"><div className="ab-spinner" /> Loading Groups...</div></div>;

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">Groups (IFCS Activity Packaging)</h1>
          <p className="ads-page-subtitle">{filtered.length} Group{filtered.length !== 1 ? 's' : ''} — aircraft functionality and expectation grouping</p>
        </div>
        <div className="ads-toolbar">
          <input className="ads-search-input" placeholder="Search groups..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
      </div>

      <div className="ads-stats">
        <div className="ads-stat ads-stat--brand"><span className="ads-stat-value">{groups.length}</span><span className="ads-stat-label">Total Groups</span></div>
        <div className="ads-stat ads-stat--warning"><span className="ads-stat-value">{inProgressCount}</span><span className="ads-stat-label">In Progress</span></div>
        <div className="ads-stat ads-stat--success"><span className="ads-stat-value">{doneCount}</span><span className="ads-stat-label">Done</span></div>
        <div className="ads-stat"><span className="ads-stat-value">{totalDeliverables}</span><span className="ads-stat-label">Deliverables</span></div>
      </div>

      <div className="ads-filters">
        {STATUSES.map(s => (
          <button key={s} className={`ads-filter-pill${statusFilter === s ? ' ads-filter-pill--active' : ''}`} onClick={() => setStatusFilter(s)}>
            {s.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      <div className="ads-table-wrap">
        <table className="ads-table">
          <thead>
            <tr>
              <th>Key</th>
              <th>Summary</th>
              <th>Status</th>
              <th>Impacted Team</th>
              <th>Deliverables</th>
              <th>Assignee</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={7} className="ads-table-empty">No Groups match filters.</td></tr>
            ) : filtered.map(g => (
              <tr key={g.id}>
                <td><span className="ads-table-link" style={{ cursor: 'default' }}>{g.issueKey}</span></td>
                <td>{g.summary}</td>
                <td><span className={`ads-badge ads-badge--${g.status === 'DONE' ? 'released' : g.status === 'IN_PROGRESS' ? 'to_be_verified' : 'new'}`}>{g.status.replace(/_/g, ' ')}</span></td>
                <td>{g.impactedTeam}</td>
                <td style={{ fontWeight: 600 }}>{g.childDeliverableCount}</td>
                <td>{g.assigneeName}</td>
                <td>{g.createdAt ? new Date(g.createdAt).toLocaleDateString() : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="ads-card" style={{ marginTop: 16 }}>
        <h3 className="ads-card-title" style={{ fontSize: 14 }}>About Groups (IFCS)</h3>
        <p style={{ fontSize: 13, color: '#172b4d', lineHeight: 1.6, margin: 0 }}>
          A <strong>Group</strong> ticket represents a specific aircraft functionality or expectation,
          managed by the IFCS Project Leader. Each Group is parent of one or more <strong>Deliverable</strong> tickets
          (handled by Work Package Leaders) and <strong>Task</strong> tickets. Advancement is tracked via
          BigPicture Gantt views. Workflow: BACKLOG &rarr; TO DO &rarr; IN PROGRESS &rarr; IN REVIEW &rarr; DONE / CANCELLED.
        </p>
      </div>
    </div>
  );
}
