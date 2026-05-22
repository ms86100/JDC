import { useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { workflowApi } from '../../../api/workflowApi';
import './workflow-management.css';

type OpenView = 'detail' | 'designer';

/**
 * Flyout entry for opening workflow detail or designer without knowing the workflow ID upfront.
 * Routes: /workflows/open?view=detail | /workflows/open?view=designer
 */
export default function WorkflowOpenPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const view: OpenView = searchParams.get('view') === 'designer' ? 'designer' : 'detail';
  const [workflowId, setWorkflowId] = useState('');
  const [filter, setFilter] = useState('');

  const { data: workflows = [], isLoading } = useQuery({
    queryKey: ['workflows'],
    queryFn: () => workflowApi.getAll().then((r) => r.data),
  });

  const filtered = useMemo(
    () =>
      workflows.filter((w) =>
        w.name.toLowerCase().includes(filter.toLowerCase()),
      ),
    [workflows, filter],
  );

  const title = view === 'designer' ? 'Open workflow designer' : 'Open workflow configuration';
  const targetLabel = view === 'designer' ? 'Open designer' : 'Open configuration';

  const go = (id: string) => {
    if (view === 'designer') {
      navigate(`/workflows/${id}/designer`);
    } else {
      navigate(`/workflows/${id}`);
    }
  };

  return (
    <div className="wf-page" data-testid="workflow-open-page">
      <header className="wf-page-header">
        <div>
          <Link to="/workflows" className="wf-back">
            ← Workflow hub
          </Link>
          <h1>{title}</h1>
          <p className="wf-muted">
            Select a workflow to continue. You can also open workflows directly from the{' '}
            <Link to="/workflows">workflow hub</Link> cards.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Link
            to="/workflows/open?view=detail"
            className={`ab-btn ab-btn-sm ${view === 'detail' ? 'ab-btn-primary' : 'ab-btn-secondary'}`}
          >
            Configure
          </Link>
          <Link
            to="/workflows/open?view=designer"
            className={`ab-btn ab-btn-sm ${view === 'designer' ? 'ab-btn-primary' : 'ab-btn-secondary'}`}
          >
            Designer
          </Link>
        </div>
      </header>

      <div className="wf-panel" style={{ maxWidth: 640 }}>
        <label className="block text-sm font-medium text-gray-700 mb-1">Search workflows</label>
        <input
          type="search"
          className="ab-input wf-search"
          placeholder="Filter by name…"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          data-testid="workflow-open-search"
        />
        <label className="block text-sm font-medium text-gray-700 mt-4 mb-1">Workflow</label>
        <select
          className="ab-input w-full"
          value={workflowId}
          onChange={(e) => setWorkflowId(e.target.value)}
          disabled={isLoading}
          data-testid="workflow-open-select"
        >
          <option value="">Select workflow…</option>
          {filtered.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name}
              {w.isDraft ? ' (draft)' : ''}
            </option>
          ))}
        </select>
        <button
          type="button"
          className="ab-btn ab-btn-primary mt-4"
          disabled={!workflowId}
          onClick={() => go(workflowId)}
          data-testid="workflow-open-go"
        >
          {targetLabel}
        </button>
      </div>

      {!isLoading && filtered.length > 0 && (
        <div className="wf-workflow-grid" style={{ marginTop: 24 }}>
          {filtered.slice(0, 12).map((w) => (
            <article key={w.id} className="wf-workflow-card">
              <h3>{w.name}</h3>
              <p className="wf-muted">{w.description || 'No description'}</p>
              <div className="wf-card-actions">
                <button
                  type="button"
                  className="ab-btn ab-btn-primary ab-btn-sm"
                  onClick={() => go(w.id)}
                >
                  {targetLabel}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
