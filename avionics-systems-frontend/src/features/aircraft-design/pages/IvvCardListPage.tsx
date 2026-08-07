import { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { DEMO_IVV_CARDS } from '../demoData';
import '../AircraftDesignStyles.css';

const TYPE_FILTERS = ['ALL', 'VALIDATION', 'VERIFICATION'] as const;
const STATUS_FILTERS = ['ALL', 'BACKLOG', 'PLANNED', 'DONE', 'CANCELLED'] as const;

export default function IvvCardListPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';
  const [cards, setCards] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [search, setSearch] = useState('');

  useEffect(() => {
    setCards(DEMO_IVV_CARDS);
    setLoading(false);
  }, [projectId]);

  const filtered = useMemo(() => {
    let result = cards;
    if (typeFilter !== 'ALL') result = result.filter(c => c.ivvType === typeFilter);
    if (statusFilter !== 'ALL') result = result.filter(c => c.status === statusFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(c => c.issueKey?.toLowerCase().includes(q) || c.summary?.toLowerCase().includes(q) || c.requirementImpact?.toLowerCase().includes(q));
    }
    return result;
  }, [cards, typeFilter, statusFilter, search]);

  const valCount = cards.filter(c => c.ivvType === 'VALIDATION').length;
  const verCount = cards.filter(c => c.ivvType === 'VERIFICATION').length;
  const coveredCount = cards.filter(c => c.testsStatus === 'COVERED').length;

  if (loading) return <div className="ads-page"><div className="ads-loading"><div className="ab-spinner" /> Loading IVV Cards...</div></div>;

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">IVV Cards (Validation & Verification)</h1>
          <p className="ads-page-subtitle">{filtered.length} IVV Card{filtered.length !== 1 ? 's' : ''} — auto-generated from VVM Table Grids</p>
        </div>
        <div className="ads-toolbar">
          <input className="ads-search-input" placeholder="Search by key, requirement..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
      </div>

      <div className="ads-stats">
        <div className="ads-stat ads-stat--brand"><span className="ads-stat-value">{cards.length}</span><span className="ads-stat-label">Total IVVs</span></div>
        <div className="ads-stat ads-stat--warning"><span className="ads-stat-value">{valCount}</span><span className="ads-stat-label">Validation</span></div>
        <div className="ads-stat"><span className="ads-stat-value">{verCount}</span><span className="ads-stat-label">Verification</span></div>
        <div className="ads-stat ads-stat--success"><span className="ads-stat-value">{coveredCount}</span><span className="ads-stat-label">Covered</span></div>
        <div className="ads-stat ads-stat--danger"><span className="ads-stat-value">{cards.length - coveredCount}</span><span className="ads-stat-label">Uncovered</span></div>
      </div>

      <div className="ads-filters" style={{ marginBottom: 8 }}>
        <span style={{ fontSize: 12, fontWeight: 600, color: '#6b778c', marginRight: 8 }}>Type:</span>
        {TYPE_FILTERS.map(t => (
          <button key={t} className={`ads-filter-pill${typeFilter === t ? ' ads-filter-pill--active' : ''}`} onClick={() => setTypeFilter(t)}>{t}</button>
        ))}
      </div>
      <div className="ads-filters">
        <span style={{ fontSize: 12, fontWeight: 600, color: '#6b778c', marginRight: 8 }}>Status:</span>
        {STATUS_FILTERS.map(s => (
          <button key={s} className={`ads-filter-pill${statusFilter === s ? ' ads-filter-pill--active' : ''}`} onClick={() => setStatusFilter(s)}>{s}</button>
        ))}
      </div>

      <div className="ads-table-wrap">
        <table className="ads-table">
          <thead>
            <tr>
              <th>Key</th>
              <th>Type</th>
              <th>Requirement Impact</th>
              <th>Level</th>
              <th>Partition</th>
              <th>Tests Status</th>
              <th>Status</th>
              <th>Assignee</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={8} className="ads-table-empty">No IVV Cards match filters.</td></tr>
            ) : filtered.map(c => (
              <tr key={c.id}>
                <td><span className="ads-table-link" style={{ cursor: 'default' }}>{c.issueKey}</span></td>
                <td><span className={`ads-badge ads-badge--${c.ivvType === 'VALIDATION' ? 'to_be_verified' : 'new'}`}>{c.ivvType}</span></td>
                <td style={{ fontFamily: 'monospace', fontSize: 11 }}>{c.requirementImpact}</td>
                <td>{c.level}</td>
                <td>{c.partitionName}</td>
                <td><span className={`ads-badge ads-badge--${c.testsStatus === 'COVERED' ? 'verified' : 'cancelled'}`}>{c.testsStatus}</span></td>
                <td><span className={`ads-badge ads-badge--${c.status === 'DONE' ? 'released' : c.status === 'PLANNED' ? 'to_be_verified' : 'new'}`}>{c.status}</span></td>
                <td>{c.assigneeName}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
