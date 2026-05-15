import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sprintApi, SprintResponse } from '../../../api/sprintApi';
import SprintReportModal from '../components/SprintReportModal';

interface SprintsPageProps {
  projectId?: string;
}

export default function SprintsPage({ projectId }: SprintsPageProps) {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [reportSprint, setReportSprint] = useState<{ id: string; name: string } | null>(null);
  const [form, setForm] = useState({
    name: '',
    goal: '',
    startDate: '',
    endDate: '',
  });

  const { data: sprints, isLoading } = useQuery<SprintResponse[]>({
    queryKey: ['sprints', projectId],
    queryFn: async () => {
      const data = await sprintApi.getAll(projectId);
      return data;
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: typeof form) => sprintApi.create({
      ...data,
      projectId: projectId || '00000000-0000-0000-0000-000000000000',
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints', projectId] });
      setShowCreate(false);
      setForm({ name: '', goal: '', startDate: '', endDate: '' });
    },
  });

  const startMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.start(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints', projectId] });
    },
  });

  const completeMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.complete(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints', projectId] });
    },
  });

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'ab-badge-success';
      case 'PLANNING': return 'ab-badge-primary';
      case 'COMPLETED': return 'ab-badge-secondary';
      default: return 'ab-badge-secondary';
    }
  };

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  };

  const activeSprints = sprints?.filter(s => s.status === 'ACTIVE') || [];
  const planningSprints = sprints?.filter(s => s.status === 'PLANNING') || [];
  const completedSprints = sprints?.filter(s => s.status === 'COMPLETED') || [];

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
          <div className="ab-card-header">
            <h3>Create New Sprint</h3>
          </div>
          <div className="ab-card-body">
            <div className="ab-form-group">
              <label className="ab-label">Sprint Name *</label>
              <input
                type="text"
                className="ab-input"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="e.g., Sprint 1"
                required
              />
            </div>
            <div className="ab-form-group">
              <label className="ab-label">Sprint Goal</label>
              <textarea
                className="ab-textarea"
                value={form.goal}
                onChange={(e) => setForm({ ...form, goal: e.target.value })}
                placeholder="What do you want to achieve in this sprint?"
                rows={3}
              />
            </div>
            <div className="ab-form-row">
              <div className="ab-form-group">
                <label className="ab-label">Start Date</label>
                <input
                  type="date"
                  className="ab-input"
                  value={form.startDate}
                  onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                />
              </div>
              <div className="ab-form-group">
                <label className="ab-label">End Date</label>
                <input
                  type="date"
                  className="ab-input"
                  value={form.endDate}
                  onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                />
              </div>
            </div>
            <div className="ab-form-actions">
              <button className="ab-btn ab-btn-secondary" onClick={() => setShowCreate(false)}>
                Cancel
              </button>
              <button
                className="ab-btn ab-btn-primary"
                onClick={() => createMutation.mutate(form)}
                disabled={!form.name || createMutation.isPending}
              >
                {createMutation.isPending ? 'Creating...' : 'Create Sprint'}
              </button>
            </div>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
        </div>
      ) : (
        <div className="ab-sprints-grid">
          {/* Active Sprints */}
          <div className="ab-sprint-section">
            <div className="ab-sprint-section-header">
              <span className="ab-status-dot" style={{ background: '#28a745' }}></span>
              <h3>Active Sprints ({activeSprints.length})</h3>
            </div>
            {activeSprints.map((sprint) => (
              <div key={sprint.id} className="ab-card ab-sprint-card ab-sprint-active">
                <div className="ab-card-body">
                  <div className="ab-sprint-header">
                    <h4>{sprint.name}</h4>
                    <span className={`ab-badge ${getStatusBadgeClass(sprint.status)}`}>
                      Active
                    </span>
                  </div>
                  {sprint.goal && <p className="ab-sprint-goal">{sprint.goal}</p>}
                  <div className="ab-sprint-meta">
                    <span>📅 {formatDate(sprint.startDate)} - {formatDate(sprint.endDate)}</span>
                    <span>📋 {sprint.issueCount || 0} issues</span>
                  </div>
                  <div className="ab-sprint-actions">
                    <button
                      className="ab-btn ab-btn-secondary ab-btn-sm"
                      onClick={() => setReportSprint({ id: sprint.id, name: sprint.name })}
                    >
                      View Report
                    </button>
                    <button
                      className="ab-btn ab-btn-secondary ab-btn-sm"
                      onClick={() => completeMutation.mutate(sprint.id)}
                    >
                      Complete Sprint
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {activeSprints.length === 0 && (
              <div className="ab-empty-state">
                <p>No active sprint</p>
              </div>
            )}
          </div>

          {/* Planning Sprints */}
          <div className="ab-sprint-section">
            <div className="ab-sprint-section-header">
              <span className="ab-status-dot" style={{ background: '#0066ff' }}></span>
              <h3>Planning ({planningSprints.length})</h3>
            </div>
            {planningSprints.map((sprint) => (
              <div key={sprint.id} className="ab-card ab-sprint-card">
                <div className="ab-card-body">
                  <div className="ab-sprint-header">
                    <h4>{sprint.name}</h4>
                    <span className={`ab-badge ${getStatusBadgeClass(sprint.status)}`}>
                      Planning
                    </span>
                  </div>
                  {sprint.goal && <p className="ab-sprint-goal">{sprint.goal}</p>}
                  <div className="ab-sprint-meta">
                    <span>📅 {formatDate(sprint.startDate)} - {formatDate(sprint.endDate)}</span>
                    <span>📋 {sprint.issueCount || 0} issues</span>
                  </div>
                  <div className="ab-sprint-actions">
                    <button
                      className="ab-btn ab-btn-primary ab-btn-sm"
                      onClick={() => startMutation.mutate(sprint.id)}
                    >
                      Start Sprint
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {planningSprints.length === 0 && (
              <div className="ab-empty-state">
                <p>No sprints in planning</p>
              </div>
            )}
          </div>

          {/* Completed Sprints */}
          <div className="ab-sprint-section">
            <div className="ab-sprint-section-header">
              <span className="ab-status-dot" style={{ background: '#6c757d' }}></span>
              <h3>Completed ({completedSprints.length})</h3>
            </div>
            {completedSprints.slice(0, 5).map((sprint) => (
              <div key={sprint.id} className="ab-card ab-sprint-card ab-sprint-completed">
                <div className="ab-card-body">
                  <div className="ab-sprint-header">
                    <h4>{sprint.name}</h4>
                    <span className={`ab-badge ${getStatusBadgeClass(sprint.status)}`}>
                      Done
                    </span>
                  </div>
                  {sprint.goal && <p className="ab-sprint-goal">{sprint.goal}</p>}
                  <div className="ab-sprint-meta">
                    <span>📅 {formatDate(sprint.startDate)} - {formatDate(sprint.endDate)}</span>
                    <span>📋 {sprint.issueCount || 0} issues</span>
                  </div>
                  <div className="ab-sprint-actions">
                    <button
                      className="ab-btn ab-btn-secondary ab-btn-sm"
                      onClick={() => setReportSprint({ id: sprint.id, name: sprint.name })}
                    >
                      View Report
                    </button>
                  </div>
                </div>
              </div>
            ))}
            {completedSprints.length === 0 && (
              <div className="ab-empty-state">
                <p>No completed sprints</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Report Modal */}
      {reportSprint && (
        <SprintReportModal
          sprintId={reportSprint.id}
          sprintName={reportSprint.name}
          onClose={() => setReportSprint(null)}
        />
      )}

      <style>{`
        .ab-sprints-page {
          padding: var(--ab-spacing-lg);
        }

        .ab-sprint-form {
          margin-bottom: var(--ab-spacing-xl);
        }

        .ab-form-row {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: var(--ab-spacing-md);
        }

        .ab-form-actions {
          display: flex;
          justify-content: flex-end;
          gap: var(--ab-spacing-sm);
          margin-top: var(--ab-spacing-md);
        }

        .ab-sprints-grid {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--ab-spacing-lg);
        }

        @media (max-width: 1024px) {
          .ab-sprints-grid {
            grid-template-columns: 1fr;
          }
        }

        .ab-sprint-section {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
        }

        .ab-sprint-section-header {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          margin-bottom: var(--ab-spacing-sm);
        }

        .ab-sprint-section-header h3 {
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
          color: var(--ab-gray-700);
          margin: 0;
        }

        .ab-status-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
        }

        .ab-sprint-card {
          transition: box-shadow var(--ab-transition-fast);
        }

        .ab-sprint-card:hover {
          box-shadow: var(--ab-shadow-md);
        }

        .ab-sprint-active {
          border-left: 3px solid #28a745;
        }

        .ab-sprint-completed {
          opacity: 0.8;
        }

        .ab-sprint-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: var(--ab-spacing-sm);
        }

        .ab-sprint-header h4 {
          font-size: var(--ab-font-size-base);
          font-weight: 600;
          margin: 0;
          color: var(--ab-gray-800);
        }

        .ab-sprint-goal {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-600);
          margin: 0 0 var(--ab-spacing-sm);
          line-height: 1.5;
        }

        .ab-sprint-meta {
          display: flex;
          gap: var(--ab-spacing-md);
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-sprint-actions {
          display: flex;
          gap: var(--ab-spacing-sm);
        }
      `}</style>
    </div>
  );
}
