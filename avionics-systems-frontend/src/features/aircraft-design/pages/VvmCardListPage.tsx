import { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { DEMO_VVM_CARDS, DEMO_IVV_CARDS } from '../demoData';
import '../AircraftDesignStyles.css';

const STATUSES = ['ALL', 'TO_DO', 'IN_PROGRESS', 'CIA_FROZEN', 'DONE', 'CANCELLED'] as const;

export default function VvmCardListPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';
  const [cards, setCards] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [search, setSearch] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);

  useEffect(() => {
    setCards(DEMO_VVM_CARDS);
    setLoading(false);
  }, [projectId]);

  const filtered = useMemo(() => {
    let result = cards;
    if (statusFilter !== 'ALL') result = result.filter(c => c.status === statusFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(c => c.issueKey?.toLowerCase().includes(q) || c.summary?.toLowerCase().includes(q) || c.scope?.toLowerCase().includes(q));
    }
    return result;
  }, [cards, statusFilter, search]);

  if (loading) return <div className="ads-page"><div className="ads-loading"><div className="ab-spinner" /> Loading VVM Cards...</div></div>;

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">VVM Cards (V&V Management)</h1>
          <p className="ads-page-subtitle">{filtered.length} VVM Card{filtered.length !== 1 ? 's' : ''} — V&V strategy per design update</p>
        </div>
        <div className="ads-toolbar">
          <input className="ads-search-input" placeholder="Search by key, summary, scope..." value={search} onChange={e => setSearch(e.target.value)} />
        </div>
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
              <th style={{ width: 30 }} />
              <th>Key</th>
              <th>Summary</th>
              <th>Status</th>
              <th>Team</th>
              <th>Scope</th>
              <th>Pipeline</th>
              <th>Expert</th>
              <th>Val/Ver</th>
              <th>Assignee</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr><td colSpan={10} className="ads-table-empty">No VVM Cards match filters.</td></tr>
            ) : filtered.map(c => (
              <>
                <tr key={c.id}>
                  <td>
                    <button className="ads-btn ads-btn--sm ads-btn--ghost" onClick={() => setExpandedId(expandedId === c.id ? null : c.id)} style={{ padding: '2px 6px' }}>
                      {expandedId === c.id ? '▼' : '▶'}
                    </button>
                  </td>
                  <td><span className="ads-table-link" style={{ cursor: 'default' }}>{c.issueKey}</span></td>
                  <td>{c.summary}</td>
                  <td><span className={`ads-badge ads-badge--${c.status === 'CIA_FROZEN' ? 'verified' : c.status === 'DONE' ? 'released' : c.status === 'IN_PROGRESS' ? 'to_be_verified' : 'new'}`}>{c.status.replace(/_/g, ' ')}</span></td>
                  <td>{c.team}</td>
                  <td style={{ fontSize: 12 }}>{c.scope}</td>
                  <td style={{ fontSize: 11 }}>{c.pipelineStatus || '-'}</td>
                  <td>{c.expertReview}</td>
                  <td>{c.validationCount}/{c.verificationCount}</td>
                  <td>{c.assigneeName}</td>
                </tr>
                {expandedId === c.id && (
                  <tr key={`${c.id}-ivvs`}>
                    <td colSpan={10} style={{ background: '#fafbfc', padding: '8px 14px 8px 40px' }}>
                      <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 8, color: '#172b4d' }}>Linked IVV Cards ({DEMO_IVV_CARDS.filter(iv => iv.vvmCardId === c.id).length})</div>
                      {DEMO_IVV_CARDS.filter(iv => iv.vvmCardId === c.id).length === 0 ? (
                        <span style={{ color: '#97a0af', fontStyle: 'italic', fontSize: 13 }}>No IVV Cards generated yet. Use &quot;Update IVVs&quot; button.</span>
                      ) : (
                        <table className="ads-table" style={{ border: 'none' }}>
                          <thead><tr><th>Key</th><th>Type</th><th>Requirement</th><th>Level</th><th>Tests</th><th>Status</th></tr></thead>
                          <tbody>
                            {DEMO_IVV_CARDS.filter(iv => iv.vvmCardId === c.id).map(iv => (
                              <tr key={iv.id}>
                                <td>{iv.issueKey}</td>
                                <td><span className={`ads-badge ads-badge--${iv.ivvType === 'VALIDATION' ? 'to_be_verified' : 'new'}`}>{iv.ivvType}</span></td>
                                <td style={{ fontFamily: 'monospace', fontSize: 11 }}>{iv.requirementImpact}</td>
                                <td>{iv.level}</td>
                                <td><span className={`ads-badge ads-badge--${iv.testsStatus === 'COVERED' ? 'verified' : 'cancelled'}`}>{iv.testsStatus}</span></td>
                                <td><span className={`ads-badge ads-badge--${iv.status === 'DONE' ? 'released' : 'new'}`}>{iv.status}</span></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      )}
                      <div style={{ marginTop: 12, display: 'flex', gap: 12 }}>
                        <div className="ads-card" style={{ flex: 1, padding: 12 }}>
                          <div style={{ fontSize: 11, color: '#6b778c', fontWeight: 600 }}>Pipeline Status</div>
                          <div style={{ fontSize: 12, marginTop: 4 }}>{c.pipelineStatus || 'Not run'}</div>
                        </div>
                        <div className="ads-card" style={{ flex: 1, padding: 12 }}>
                          <div style={{ fontSize: 11, color: '#6b778c', fontWeight: 600 }}>LTR Reference</div>
                          <div style={{ fontSize: 12, marginTop: 4 }}>{c.ltrReference || '-'}</div>
                        </div>
                        <div className="ads-card" style={{ flex: 1, padding: 12 }}>
                          <div style={{ fontSize: 11, color: '#6b778c', fontWeight: 600 }}>Reviews</div>
                          <div style={{ fontSize: 12, marginTop: 4 }}>Expert: {c.expertReview} | Testing: {c.testingReview} | Safety: {c.safetyReview}</div>
                        </div>
                      </div>
                    </td>
                  </tr>
                )}
              </>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
