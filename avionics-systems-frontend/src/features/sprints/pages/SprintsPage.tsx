import { useState, useMemo } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { sprintApi, SprintResponse } from '../../../api/sprintApi';
import SprintReportModal from '../components/SprintReportModal';

export default function SprintsPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [reportSprint, setReportSprint] = useState<{ id: string; name: string } | null>(null);
  const [form, setForm] = useState({ name: '', goal: '', startDate: '', endDate: '', projectId: '' });

  const { data: sprints = [], isLoading } = useQuery({
    queryKey: ['sprints'],
    queryFn: () => sprintApi.getAll(),
  });

  const createMutation = useMutation({
    mutationFn: () => sprintApi.create({ ...form, projectId: form.projectId || '00000000-0000-0000-0000-000000000000' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      setShowCreate(false);
      setForm({ name: '', goal: '', startDate: '', endDate: '', projectId: '' });
    },
  });

  const startMutation = useMutation({
    mutationFn: (id: string) => sprintApi.start(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sprints'] }),
  });

  const completeMutation = useMutation({
    mutationFn: (id: string) => sprintApi.complete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sprints'] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => sprintApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['sprints'] }),
  });

  const formatDate = (d?: string) => {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const activeSprints = useMemo(() => sprints.filter(s => s.status === 'ACTIVE'), [sprints]);
  const planningSprints = useMemo(() => sprints.filter(s => s.status === 'PLANNING'), [sprints]);
  const completedSprints = useMemo(() => sprints.filter(s => s.status === 'COMPLETED'), [sprints]);

  const badgeClass = (status: string) => {
    if (status === 'ACTIVE') return 'ab-badge-success';
    if (status === 'PLANNING') return 'ab-badge-primary';
    return 'ab-badge-secondary';
  };

  const renderSprintCard = (sprint: SprintResponse, actions: React.ReactNode) => (
    <div key={sprint.id} className={`ab-card ab-sprint-card ${sprint.status === 'ACTIVE' ? 'ab-sprint-active' : ''} ${sprint.status === 'COMPLETED' ? 'ab-sprint-completed' : ''}`}>
      <div className="ab-card-body">
        <div className="ab-sprint-header-row">
          <h4 className="ab-sprint-name">{sprint.name}</h4>
          <span className={`ab-badge ${badgeClass(sprint.status)}`}>{sprint.status}</span>
        </div>
        {sprint.goal && <p className="ab-sprint-goal">{sprint.goal}</p>}
        <div className="ab-sprint-meta">
          <span>{formatDate(sprint.startDate)} — {formatDate(sprint.endDate)}</span>
          <span>{sprint.issueCount ?? 0} issues</span>
          {sprint.completedIssueCount != null && sprint.completedIssueCount > 0 && (
            <span>{sprint.completedIssueCount} done</span>
          )}
        </div>
        <div className="ab-sprint-actions">{actions}</div>
      </div>
    </div>
  );

  return (
    <div className="ab-sprints-page">
      <div className="ab-page-header">
        <div>
          <h1 className="ab-page-title">Sprints</h1>
          <p className="ab-page-subtitle">Manage sprints and track agile progress</p>
        </div>
        <button className="ab-btn ab-btn-primary" onClick={() => setShowCreate(!showCreate)}>
          Create Sprint
        </button>
      </div>

      {showCreate && (
        <div className="ab-card ab-sprint-form">
          <div className="ab-card-header"><h3>Create New Sprint</h3></div>
          <div className="ab-card-body">
            <div className="ab-form-group">
              <label className="ab-label">Sprint Name *</label>
              <input type="text" className="ab-input" value={form.name}
                onChange={e => setForm({ ...form, name: e.target.value })} placeholder="e.g., Sprint 1" />
            </div>
            <div className="ab-form-group">
              <label className="ab-label">Sprint Goal</label>
              <textarea className="ab-textarea" value={form.goal}
                onChange={e => setForm({ ...form, goal: e.target.value })}
                placeholder="What do you want to achieve?" rows={2} />
            </div>
            <div className="ab-form-row">
              <div className="ab-form-group">
                <label className="ab-label">Start Date</label>
                <input type="date" className="ab-input" value={form.startDate}
                  onChange={e => setForm({ ...form, startDate: e.target.value })} />
              </div>
              <div className="ab-form-group">
                <label className="ab-label">End Date</label>
                <input type="date" className="ab-input" value={form.endDate}
                  onChange={e => setForm({ ...form, endDate: e.target.value })} />
              </div>
            </div>
            <div className="ab-form-actions">
              <button className="ab-btn ab-btn-secondary" onClick={() => setShowCreate(false)}>Cancel</button>
              <button className="ab-btn ab-btn-primary" onClick={() => createMutation.mutate()}
                disabled={!form.name || createMutation.isPending}>
                {createMutation.isPending ? 'Creating...' : 'Create Sprint'}
              </button>
            </div>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="ab-loading"><div className="ab-spinner"></div></div>
      ) : sprints.length === 0 ? (
        <div className="ab-empty-state" style={{ padding: '60px 20px', textAlign: 'center' }}>
          <h3 style={{ color: 'var(--ab-gray-600, #4b5563)' }}>No sprints yet</h3>
          <p style={{ color: 'var(--ab-gray-400, #9ca3af)' }}>
            Create your first sprint or go to a project's Backlog to plan sprints.
          </p>
        </div>
      ) : (
        <div className="ab-sprints-grid">
          {/* Active Sprints */}
          <div className="ab-sprint-section">
            <div className="ab-sprint-section-header">
              <span className="ab-status-dot" style={{ background: '#22c55e' }}></span>
              <h3>Active Sprints ({activeSprints.length})</h3>
            </div>
            {activeSprints.map(s => renderSprintCard(s, <>
              <button className="ab-btn ab-btn-secondary ab-btn-sm"
                onClick={() => setReportSprint({ id: s.id, name: s.name })}>View Report</button>
              <button className="ab-btn ab-btn-secondary ab-btn-sm"
                onClick={() => completeMutation.mutate(s.id)}
                disabled={completeMutation.isPending}>Complete Sprint</button>
            </>))}
            {activeSprints.length === 0 && <div className="ab-empty-state"><p>No active sprint</p></div>}
          </div>

          {/* Planning Sprints */}
          <div className="ab-sprint-section">
            <div className="ab-sprint-section-header">
              <span className="ab-status-dot" style={{ background: '#3b82f6' }}></span>
              <h3>Planning ({planningSprints.length})</h3>
            </div>
            {planningSprints.map(s => renderSprintCard(s, <>
              <button className="ab-btn ab-btn-primary ab-btn-sm"
                onClick={() => startMutation.mutate(s.id)}
                disabled={startMutation.isPending}>Start Sprint</button>
              <button className="ab-btn ab-btn-danger ab-btn-sm"
                onClick={() => { if (confirm('Delete this sprint?')) deleteMutation.mutate(s.id); }}>Delete</button>
            </>))}
            {planningSprints.length === 0 && <div className="ab-empty-state"><p>No sprints in planning</p></div>}
          </div>

          {/* Completed Sprints */}
          <div className="ab-sprint-section">
            <div className="ab-sprint-section-header">
              <span className="ab-status-dot" style={{ background: '#6b7280' }}></span>
              <h3>Completed ({completedSprints.length})</h3>
            </div>
            {completedSprints.slice(0, 10).map(s => renderSprintCard(s, <>
              <button className="ab-btn ab-btn-secondary ab-btn-sm"
                onClick={() => setReportSprint({ id: s.id, name: s.name })}>View Report</button>
            </>))}
            {completedSprints.length === 0 && <div className="ab-empty-state"><p>No completed sprints</p></div>}
          </div>
        </div>
      )}

      {reportSprint && (
        <SprintReportModal sprintId={reportSprint.id} sprintName={reportSprint.name}
          onClose={() => setReportSprint(null)} />
      )}

      <style>{`
        .ab-sprints-page { padding: var(--ab-spacing-lg, 24px); }
        .ab-sprint-form { margin-bottom: var(--ab-spacing-xl, 32px); }
        .ab-form-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--ab-spacing-md, 16px); }
        .ab-form-actions { display: flex; justify-content: flex-end; gap: var(--ab-spacing-sm, 8px); margin-top: var(--ab-spacing-md, 16px); }
        .ab-sprints-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--ab-spacing-lg, 24px); }
        @media (max-width: 1024px) { .ab-sprints-grid { grid-template-columns: 1fr; } }
        .ab-sprint-section { display: flex; flex-direction: column; gap: var(--ab-spacing-sm, 8px); }
        .ab-sprint-section-header { display: flex; align-items: center; gap: var(--ab-spacing-sm, 8px); margin-bottom: var(--ab-spacing-sm, 8px); }
        .ab-sprint-section-header h3 { font-size: var(--ab-font-size-sm, 0.875rem); font-weight: 600; color: var(--ab-gray-700, #374151); margin: 0; }
        .ab-status-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
        .ab-sprint-card { transition: box-shadow 0.15s; }
        .ab-sprint-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
        .ab-sprint-active { border-left: 3px solid #22c55e; }
        .ab-sprint-completed { opacity: 0.75; }
        .ab-sprint-header-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; }
        .ab-sprint-name { font-size: var(--ab-font-size-base, 1rem); font-weight: 600; margin: 0; color: var(--ab-gray-800, #1f2937); }
        .ab-sprint-goal { font-size: var(--ab-font-size-sm, 0.875rem); color: var(--ab-gray-600, #4b5563); margin: 0 0 8px; line-height: 1.5; }
        .ab-sprint-meta { display: flex; gap: var(--ab-spacing-md, 16px); font-size: var(--ab-font-size-xs, 0.75rem); color: var(--ab-gray-500, #6b7280); margin-bottom: 12px; flex-wrap: wrap; }
        .ab-sprint-actions { display: flex; gap: var(--ab-spacing-sm, 8px); flex-wrap: wrap; }
      `}</style>
    </div>
  );
}
