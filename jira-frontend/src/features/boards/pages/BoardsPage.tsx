import React, { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sprintApi, SprintResponse } from '../../../api/sprintApi';
import EnhancedKanbanBoard from '../components/EnhancedKanbanBoard';

const BoardsPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const boardIdFromUrl = searchParams.get('boardId');
  const projectIdFromUrl = searchParams.get('project');
  const [view, setView] = useState<'board' | 'sprint'>(boardIdFromUrl ? 'board' : 'sprint');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [form, setForm] = useState({ name: '', goal: '', startDate: '', endDate: '' });
  const queryClient = useQueryClient();

  const { data: sprints = [], isLoading } = useQuery<SprintResponse[]>({
    queryKey: ['sprints'],
    queryFn: async () => {
      return await sprintApi.getAll();
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: { name: string; goal?: string; startDate?: string; endDate?: string; projectId: string }) =>
      sprintApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      setShowCreateForm(false);
      setForm({ name: '', goal: '', startDate: '', endDate: '' });
    },
  });

  const startMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.start(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
  });

  const completeMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.complete(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.delete(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
  });

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return 'Not set';
    return new Date(dateStr).toLocaleDateString();
  };

  const activeSprints = sprints?.filter(s => s.status === 'ACTIVE') || [];
  const planningSprints = sprints?.filter(s => s.status === 'PLANNING') || [];
  const completedSprints = sprints?.filter(s => s.status === 'COMPLETED') || [];

  if (isLoading) {
    return (
      <div style={{ padding: 'var(--sa-space-6)' }}>
        <div style={{ marginBottom: 'var(--sa-space-4)' }}>
          <div className="ab-skeleton" style={{ height: 28, width: 200, marginBottom: 'var(--sa-space-3)' }} />
          <div className="ab-skeleton" style={{ height: 16, width: 360 }} />
        </div>
        <div style={{ display: 'flex', gap: 'var(--sa-space-3)', marginBottom: 'var(--sa-space-6)' }}>
          <div className="ab-skeleton" style={{ height: 36, width: 140, borderRadius: 'var(--sa-radius-md)' }} />
          <div className="ab-skeleton" style={{ height: 36, width: 100, borderRadius: 'var(--sa-radius-md)' }} />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--sa-space-4)' }}>
          {[...Array(4)].map((_, i) => (
            <div key={i} className="ab-skeleton" style={{ height: 100, borderRadius: 'var(--sa-radius-md)' }} />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="ab-page">
      <div className="ab-page-header">
        <div>
          <h1 className="ab-page-title">Agile Boards</h1>
          <p className="ab-text-muted">Manage sprints, kanban boards, and track team velocity</p>
        </div>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="ab-btn ab-btn-primary"
        >
          Create Sprint
        </button>
      </div>

      {showCreateForm && (
        <div className="ab-card" style={{ marginBottom: 'var(--ab-spacing-lg, 24px)' }}>
          <div className="ab-card-header"><h3>Create New Sprint</h3></div>
          <div className="ab-card-body">
            <div className="ab-form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--ab-spacing-md, 16px)' }}>
              <div className="ab-form-group">
                <label className="ab-label">Sprint Name *</label>
                <input
                  type="text"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="ab-input"
                  placeholder="e.g., Sprint 1"
                />
              </div>
              <div className="ab-form-group">
                <label className="ab-label">Sprint Goal</label>
                <input
                  type="text"
                  value={form.goal}
                  onChange={(e) => setForm({ ...form, goal: e.target.value })}
                  className="ab-input"
                  placeholder="What do you want to achieve?"
                />
              </div>
              <div className="ab-form-group">
                <label className="ab-label">Start Date</label>
                <input
                  type="date"
                  value={form.startDate}
                  onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                  className="ab-input"
                />
              </div>
              <div className="ab-form-group">
                <label className="ab-label">End Date</label>
                <input
                  type="date"
                  value={form.endDate}
                  onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                  className="ab-input"
                />
              </div>
            </div>
            <div className="ab-form-actions" style={{ display: 'flex', justifyContent: 'flex-end', gap: 'var(--ab-spacing-sm, 8px)', marginTop: 'var(--ab-spacing-md, 16px)' }}>
              <button
                onClick={() => setShowCreateForm(false)}
                className="ab-btn ab-btn-secondary"
              >
                Cancel
              </button>
              <button
                onClick={() => createMutation.mutate({
                  ...form,
                  projectId: projectIdFromUrl || '',
                })}
                disabled={!form.name || createMutation.isPending}
                className="ab-btn ab-btn-primary"
              >
                {createMutation.isPending ? 'Creating...' : 'Create Sprint'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* View Toggle */}
      <div className="ab-tab-bar" style={{ display: 'flex', gap: 'var(--ab-spacing-sm, 8px)', marginBottom: 'var(--ab-spacing-lg, 24px)' }}>
        <button
          className={`ab-btn ${view === 'sprint' ? 'ab-btn-primary' : 'ab-btn-secondary'}`}
          onClick={() => setView('sprint')}
        >
          Sprint Management
        </button>
        <button
          className={`ab-btn ${view === 'board' ? 'ab-btn-primary' : 'ab-btn-secondary'}`}
          onClick={() => setView('board')}
        >
          Board View
        </button>
      </div>

      {view === 'board' ? (
        <div className="ab-card" style={{ height: 'calc(100vh - 300px)' }}>
          <EnhancedKanbanBoard
            projectId={projectIdFromUrl ?? undefined}
            initialBoardId={boardIdFromUrl ?? undefined}
          />
        </div>
      ) : (
        <div className="space-y-6">
          {/* Active Sprints */}
          <div className="ab-sprint-section">
            <div className="ab-sprint-section-header" style={{ display: 'flex', alignItems: 'center', gap: 'var(--ab-spacing-sm, 8px)', marginBottom: 'var(--ab-spacing-sm, 12px)' }}>
              <span className="ab-status-dot" style={{ width: 10, height: 10, borderRadius: '50%', background: '#22c55e', flexShrink: 0 }}></span>
              <h3 style={{ margin: 0, fontWeight: 600, color: 'var(--ab-gray-700, #374151)' }}>Active Sprints ({activeSprints.length})</h3>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--ab-spacing-sm, 12px)' }}>
              {activeSprints.length === 0 ? (
                <div className="ab-card ab-empty-state" style={{ padding: '32px 20px', textAlign: 'center' }}>
                  <p className="ab-text-muted">No active sprint. Start one from the planning section below.</p>
                </div>
              ) : (
                activeSprints.map((sprint) => (
                  <div key={sprint.id} className="ab-card ab-sprint-active" style={{ borderLeft: '3px solid #22c55e' }}>
                    <div className="ab-card-body">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--ab-spacing-sm, 8px)' }}>
                            <h4 style={{ margin: 0, fontWeight: 600, fontSize: '1.05rem' }}>{sprint.name}</h4>
                            <span className="ab-badge ab-badge-success">ACTIVE</span>
                          </div>
                          <p className="ab-text-muted" style={{ marginTop: 4 }}>{sprint.goal || 'No goal set'}</p>
                        </div>
                        <div style={{ display: 'flex', gap: 'var(--ab-spacing-sm, 8px)' }}>
                          <button
                            onClick={() => completeMutation.mutate(sprint.id)}
                            className="ab-btn ab-btn-secondary ab-btn-sm"
                          >
                            Complete Sprint
                          </button>
                        </div>
                      </div>
                      <div className="ab-sprint-meta" style={{ display: 'flex', gap: 'var(--ab-spacing-md, 16px)', marginTop: 'var(--ab-spacing-md, 16px)', fontSize: '0.85rem', color: 'var(--ab-gray-500, #6b7280)' }}>
                        <span>📅 {formatDate(sprint.startDate)} → {formatDate(sprint.endDate)}</span>
                        <span>📊 {sprint.issueCount || 0} issues</span>
                        <span>✓ {sprint.completedIssueCount || 0} completed</span>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Planning Sprints */}
          <div className="ab-sprint-section">
            <div className="ab-sprint-section-header" style={{ display: 'flex', alignItems: 'center', gap: 'var(--ab-spacing-sm, 8px)', marginBottom: 'var(--ab-spacing-sm, 12px)' }}>
              <span className="ab-status-dot" style={{ width: 10, height: 10, borderRadius: '50%', background: '#eab308', flexShrink: 0 }}></span>
              <h3 style={{ margin: 0, fontWeight: 600, color: 'var(--ab-gray-700, #374151)' }}>Planning ({planningSprints.length})</h3>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--ab-spacing-sm, 12px)' }}>
              {planningSprints.length === 0 ? (
                <div className="ab-card ab-empty-state" style={{ padding: '32px 20px', textAlign: 'center' }}>
                  <p className="ab-text-muted">No sprints in planning. Click "Create Sprint" to create one.</p>
                </div>
              ) : (
                planningSprints.map((sprint) => (
                  <div key={sprint.id} className="ab-card">
                    <div className="ab-card-body">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--ab-spacing-sm, 8px)' }}>
                            <h4 style={{ margin: 0, fontWeight: 600, fontSize: '1.05rem' }}>{sprint.name}</h4>
                            <span className="ab-badge ab-badge-primary">PLANNING</span>
                          </div>
                          <p className="ab-text-muted" style={{ marginTop: 4 }}>{sprint.goal || 'No goal set'}</p>
                        </div>
                        <div style={{ display: 'flex', gap: 'var(--ab-spacing-sm, 8px)' }}>
                          <button
                            onClick={() => startMutation.mutate(sprint.id)}
                            className="ab-btn ab-btn-primary ab-btn-sm"
                          >
                            Start Sprint
                          </button>
                          <button
                            onClick={() => deleteMutation.mutate(sprint.id)}
                            className="ab-btn ab-btn-danger ab-btn-sm"
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                      <div className="ab-sprint-meta" style={{ display: 'flex', gap: 'var(--ab-spacing-md, 16px)', marginTop: 'var(--ab-spacing-md, 16px)', fontSize: '0.85rem', color: 'var(--ab-gray-500, #6b7280)' }}>
                        <span>📅 {formatDate(sprint.startDate)} → {formatDate(sprint.endDate)}</span>
                        <span>📊 {sprint.issueCount || 0} issues</span>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Completed Sprints */}
          {completedSprints.length > 0 && (
            <div className="ab-sprint-section">
              <div className="ab-sprint-section-header" style={{ display: 'flex', alignItems: 'center', gap: 'var(--ab-spacing-sm, 8px)', marginBottom: 'var(--ab-spacing-sm, 12px)' }}>
                <span className="ab-status-dot" style={{ width: 10, height: 10, borderRadius: '50%', background: '#6b7280', flexShrink: 0 }}></span>
                <h3 style={{ margin: 0, fontWeight: 600, color: 'var(--ab-gray-700, #374151)' }}>Completed ({completedSprints.length})</h3>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--ab-spacing-sm, 12px)' }}>
                {completedSprints.slice(0, 5).map((sprint) => (
                  <div key={sprint.id} className="ab-card ab-sprint-completed" style={{ opacity: 0.75 }}>
                    <div className="ab-card-body">
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--ab-spacing-sm, 8px)' }}>
                            <h4 style={{ margin: 0, fontWeight: 600, fontSize: '1.05rem' }}>{sprint.name}</h4>
                            <span className="ab-badge ab-badge-secondary">COMPLETED</span>
                          </div>
                        </div>
                        <div className="ab-text-muted" style={{ fontSize: '0.85rem' }}>
                          <span>{sprint.completedIssueCount || 0}/{sprint.issueCount || 0} issues</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default BoardsPage;