import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  worklogApi,
  WorklogResponse,
  RemainingEstimateStrategy,
  secondsToMinutes,
  minutesToSeconds,
  parseTimeInput,
  formatTimeDisplay,
} from '../../../api/worklogApi';

const worklogMinutes = (w: WorklogResponse) => secondsToMinutes(w.timeSpentSeconds ?? 0);
const worklogDescription = (w: WorklogResponse) => w.workDescription ?? w.description;

interface WorklogsTabProps {
  issueId: string;
  originalEstimate?: number | null;
  remainingEstimate?: number | null;
  timeSpent?: number | null;
}

export default function WorklogsTab({ issueId, originalEstimate, remainingEstimate, timeSpent }: WorklogsTabProps) {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [showReports, setShowReports] = useState(false);
  const [timeInput, setTimeInput] = useState('1h');
  const [description, setDescription] = useState('');
  const [startDateTime, setStartDateTime] = useState('');
  const [adjustEstimate, setAdjustEstimate] = useState<RemainingEstimateStrategy>('AUTO');
  const [adjustmentInput, setAdjustmentInput] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editTimeInput, setEditTimeInput] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editStartDateTime, setEditStartDateTime] = useState('');
  const [editAdjustEstimate, setEditAdjustEstimate] = useState<RemainingEstimateStrategy>('AUTO');
  const [editAdjustmentInput, setEditAdjustmentInput] = useState('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [deleteStrategy, setDeleteStrategy] = useState<RemainingEstimateStrategy>('AUTO');
  const [deleteAdjustmentInput, setDeleteAdjustmentInput] = useState('');
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  const { data: worklogs, isLoading } = useQuery<WorklogResponse[]>({
    queryKey: ['worklogs', issueId],
    queryFn: async () => {
      const response = await worklogApi.getAll(issueId);
      return Array.isArray(response.data) ? response.data : [];
    },
    enabled: !!issueId,
  });

  const { data: totalTimeSeconds } = useQuery<number>({
    queryKey: ['worklogs-total', issueId],
    queryFn: async () => {
      const response = await worklogApi.getTotalTime(issueId);
      return typeof response.data === 'number' ? response.data : 0;
    },
    enabled: !!issueId,
  });

  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: ['worklogs', issueId] });
    queryClient.invalidateQueries({ queryKey: ['worklogs-total', issueId] });
    queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
  };

  const createMutation = useMutation({
    mutationFn: (data: { timeSpentSeconds: number; workDescription?: string; startedAt?: string; adjustEstimate?: RemainingEstimateStrategy; adjustmentSeconds?: number }) =>
      worklogApi.create(issueId, data),
    onSuccess: () => {
      invalidateAll();
      setShowForm(false);
      setTimeInput('1h');
      setDescription('');
      setStartDateTime('');
      setAdjustEstimate('AUTO');
      setAdjustmentInput('');
      setErrorMessage(null);
    },
    onError: (err: any) => setErrorMessage(err?.response?.data?.message || err?.message || 'Failed to log work'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ worklogId, data }: { worklogId: string; data: { timeSpentSeconds: number; workDescription?: string; startedAt?: string; adjustEstimate?: RemainingEstimateStrategy; adjustmentSeconds?: number } }) =>
      worklogApi.update(issueId, worklogId, data),
    onSuccess: () => {
      invalidateAll();
      setEditingId(null);
      setErrorMessage(null);
    },
    onError: (err: any) => setErrorMessage(err?.response?.data?.message || err?.message || 'Failed to update worklog'),
  });

  const deleteMutation = useMutation({
    mutationFn: ({ worklogId, strategy, adjustmentSec }: { worklogId: string; strategy: RemainingEstimateStrategy; adjustmentSec?: number }) =>
      worklogApi.delete(issueId, worklogId, strategy, adjustmentSec),
    onSuccess: () => {
      invalidateAll();
      setConfirmDeleteId(null);
      setErrorMessage(null);
    },
    onError: (err: any) => setErrorMessage(err?.response?.data?.message || err?.message || 'Failed to delete worklog'),
  });

  const handleCreate = () => {
    const seconds = parseTimeInput(timeInput);
    if (!seconds || seconds <= 0) return;
    const adjustSec = adjustmentInput ? parseTimeInput(adjustmentInput) : undefined;
    createMutation.mutate({
      timeSpentSeconds: seconds,
      workDescription: description || undefined,
      startedAt: startDateTime || undefined,
      adjustEstimate,
      adjustmentSeconds: adjustSec ?? undefined,
    });
  };

  const handleUpdate = (worklogId: string) => {
    const seconds = parseTimeInput(editTimeInput);
    if (!seconds || seconds <= 0) return;
    const adjSec = editAdjustmentInput ? parseTimeInput(editAdjustmentInput) : undefined;
    updateMutation.mutate({
      worklogId,
      data: {
        timeSpentSeconds: seconds,
        workDescription: editDescription || undefined,
        startedAt: editStartDateTime || undefined,
        adjustEstimate: editAdjustEstimate,
        adjustmentSeconds: adjSec ?? undefined,
      },
    });
  };

  const startEdit = (w: WorklogResponse) => {
    setEditingId(w.id);
    setEditTimeInput(formatTimeDisplay(w.timeSpentSeconds));
    setEditDescription(worklogDescription(w) || '');
    setEditStartDateTime(w.startedAt ? w.startedAt.slice(0, 16) : '');
    setEditAdjustEstimate('AUTO');
    setEditAdjustmentInput('');
  };

  const formatDuration = (mins: number) => {
    const hours = Math.floor(mins / 60);
    const remainingMins = mins % 60;
    if (hours === 0) return `${remainingMins}m`;
    if (remainingMins === 0) return `${hours}h`;
    return `${hours}h ${remainingMins}m`;
  };

  const formatDateTime = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit',
    });
  };

  const getRelativeDate = (dateStr: string) => {
    const date = new Date(dateStr + (dateStr.includes('T') ? '' : 'T00:00:00'));
    const now = new Date();
    const todayStr = now.toISOString().slice(0, 10);
    const yesterdayDate = new Date(now);
    yesterdayDate.setDate(yesterdayDate.getDate() - 1);
    const yesterdayStr = yesterdayDate.toISOString().slice(0, 10);
    const dateIso = dateStr.slice(0, 10);

    if (dateIso === todayStr) return 'Today';
    if (dateIso === yesterdayStr) return 'Yesterday';
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays < 7) return `${diffDays} days ago`;
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const calculateStats = () => {
    if (!worklogs || worklogs.length === 0) return null;
    const totalMinutes = worklogs.reduce((sum, w) => sum + worklogMinutes(w), 0);
    const weekAgo = new Date();
    weekAgo.setDate(weekAgo.getDate() - 7);
    const thisWeek = worklogs.filter(w => {
      const date = new Date(w.startedAt || w.createdAt);
      return date >= weekAgo;
    });
    const thisWeekMinutes = thisWeek.reduce((sum, w) => sum + worklogMinutes(w), 0);
    const activeDays = new Set(worklogs.map(w => new Date(w.startedAt || w.createdAt).toISOString().slice(0, 10))).size;
    const avgPerDay = activeDays > 0 ? totalMinutes / activeDays : 0;

    const byDay: Record<string, number> = {};
    worklogs.forEach(w => {
      const day = new Date(w.startedAt || w.createdAt).toISOString().slice(0, 10);
      byDay[day] = (byDay[day] || 0) + worklogMinutes(w);
    });

    return { totalMinutes, avgPerDay, thisWeekMinutes, byDay };
  };

  const stats = calculateStats();

  // Three-segment time tracking bar (GAP 10)
  const renderTimeTrackingBar = () => {
    const est = originalEstimate ?? 0;
    const spent = timeSpent ?? 0;
    const rem = remainingEstimate ?? 0;
    const total = Math.max(est, spent + rem, 1);

    const spentPct = Math.min(100, (spent / total) * 100);
    const remPct = Math.min(100 - spentPct, (rem / total) * 100);
    const overBudget = est > 0 && spent > est;

    return (
      <div className="ab-time-tracking-bar">
        <div className="ab-tt-bar-track">
          <div
            className={`ab-tt-bar-logged ${overBudget ? 'ab-tt-over' : ''}`}
            style={{ width: `${spentPct}%` }}
            title={`Logged: ${formatTimeDisplay(spent)}`}
          />
          <div
            className="ab-tt-bar-remaining"
            style={{ width: `${remPct}%` }}
            title={`Remaining: ${formatTimeDisplay(rem)}`}
          />
        </div>
        <div className="ab-tt-bar-labels">
          <div className="ab-tt-label">
            <span className="ab-tt-dot ab-tt-dot-est" />
            Estimated: {formatTimeDisplay(est)}
          </div>
          <div className="ab-tt-label">
            <span className="ab-tt-dot ab-tt-dot-logged" />
            Logged: {formatTimeDisplay(spent)}
          </div>
          <div className="ab-tt-label">
            <span className="ab-tt-dot ab-tt-dot-rem" />
            Remaining: {formatTimeDisplay(rem)}
          </div>
        </div>
      </div>
    );
  };

  const renderMiniChart = () => {
    if (!stats || !stats.byDay || Object.keys(stats.byDay).length === 0) return null;
    const entries = Object.entries(stats.byDay).slice(-7);
    const maxVal = Math.max(...entries.map(([, v]) => v), 1);

    return (
      <div className="ab-time-chart">
        <div className="ab-chart-bars">
          {entries.map(([date, mins]) => {
            const height = (mins / maxVal) * 60;
            const d = new Date(date + 'T00:00:00');
            return (
              <div key={date} className="ab-chart-bar-container" title={`${formatDuration(mins)}`}>
                <div className="ab-chart-bar" style={{ height: `${height}px` }} />
                <span className="ab-chart-label">
                  {d.toLocaleDateString('en-US', { weekday: 'short' })}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <div className="ab-worklogs-tab">
      <div className="ab-section-header">
        <div className="ab-section-info">
          <h3>Time Tracking</h3>
          {totalTimeSeconds !== undefined && totalTimeSeconds > 0 && (
            <span className="ab-total-time">
              Total: <strong>{formatTimeDisplay(totalTimeSeconds)}</strong>
            </span>
          )}
        </div>
        <div className="ab-section-actions">
          <button
            className={`ab-btn ab-btn-ghost ab-btn-sm ${showReports ? 'active' : ''}`}
            onClick={() => setShowReports(!showReports)}
          >
            Reports
          </button>
          <button
            className="ab-btn ab-btn-primary ab-btn-sm"
            onClick={() => setShowForm(!showForm)}
          >
            {showForm ? 'Cancel' : 'Log Work'}
          </button>
        </div>
      </div>

      {errorMessage && (
        <div className="ab-error-banner" role="alert">
          <span>{errorMessage}</span>
          <button className="ab-btn-icon" onClick={() => setErrorMessage(null)} aria-label="Dismiss">&times;</button>
        </div>
      )}

      {/* Three-segment time tracking bar */}
      {renderTimeTrackingBar()}

      {/* Stats Panel */}
      {showReports && stats && (
        <div className="ab-time-reports">
          <div className="ab-report-summary">
            <div className="ab-stat-card">
              <span className="ab-stat-value">{formatDuration(stats.totalMinutes)}</span>
              <span className="ab-stat-label">Total Time</span>
            </div>
            <div className="ab-stat-card">
              <span className="ab-stat-value">{formatDuration(stats.thisWeekMinutes)}</span>
              <span className="ab-stat-label">This Week</span>
            </div>
            <div className="ab-stat-card">
              <span className="ab-stat-value">{formatDuration(Math.round(stats.avgPerDay))}</span>
              <span className="ab-stat-label">Daily Average</span>
            </div>
          </div>
          {renderMiniChart()}
          <div className="ab-report-filters">
            <input type="date" className="ab-input ab-input-sm" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
            <span className="ab-date-sep">to</span>
            <input type="date" className="ab-input ab-input-sm" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </div>
        </div>
      )}

      {/* Log Work Form (GAP 5/6/9) */}
      {showForm && (
        <div className="ab-worklog-form ab-card">
          <div className="ab-card-body">
            <div className="ab-form-grid">
              <div className="ab-form-group">
                <label className="ab-label">Time Spent</label>
                <input
                  type="text"
                  className="ab-input"
                  placeholder="e.g. 2h 30m, 1d, 3h, 1w 2d"
                  value={timeInput}
                  onChange={(e) => setTimeInput(e.target.value)}
                />
                <span className="ab-field-hint">Use w (weeks), d (days=8h), h (hours), m (minutes)</span>
                <div className="ab-quick-times">
                  {[
                    { label: '15m', val: '15m' },
                    { label: '30m', val: '30m' },
                    { label: '1h', val: '1h' },
                    { label: '2h', val: '2h' },
                    { label: '4h', val: '4h' },
                    { label: '1d', val: '1d' },
                  ].map(({ label, val }) => (
                    <button key={val} type="button" className="ab-btn ab-btn-ghost ab-btn-xs" onClick={() => setTimeInput(val)}>
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Date Started</label>
                <input
                  type="datetime-local"
                  className="ab-input"
                  value={startDateTime}
                  onChange={(e) => setStartDateTime(e.target.value)}
                />
              </div>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Remaining Estimate</label>
              <select className="ab-input" value={adjustEstimate} onChange={(e) => setAdjustEstimate(e.target.value as RemainingEstimateStrategy)}>
                <option value="AUTO">Adjust automatically</option>
                <option value="LEAVE">Leave estimate unchanged</option>
                <option value="SET">Set remaining estimate to</option>
                <option value="REDUCE">Reduce remaining estimate by</option>
              </select>
              {(adjustEstimate === 'SET' || adjustEstimate === 'REDUCE') && (
                <input
                  type="text"
                  className="ab-input ab-input-sm"
                  placeholder="e.g. 3h 30m"
                  value={adjustmentInput}
                  onChange={(e) => setAdjustmentInput(e.target.value)}
                  style={{ marginTop: '0.5rem' }}
                />
              )}
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Work Description</label>
              <textarea
                className="ab-textarea"
                placeholder="What did you work on?"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
              />
            </div>

            <div className="ab-form-actions">
              <button className="ab-btn ab-btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
              <button
                className="ab-btn ab-btn-primary"
                onClick={handleCreate}
                disabled={createMutation.isPending || !parseTimeInput(timeInput)}
              >
                {createMutation.isPending ? 'Logging...' : 'Log Work'}
              </button>
            </div>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="ab-loading"><div className="ab-spinner"></div></div>
      ) : worklogs && worklogs.length > 0 ? (
        <div className="ab-worklog-list">
          {Object.entries(
            (startDate || endDate
              ? worklogs.filter(w => {
                  const d = new Date(w.startedAt || w.createdAt).toISOString().slice(0, 10);
                  if (startDate && d < startDate) return false;
                  if (endDate && d > endDate) return false;
                  return true;
                })
              : worklogs
            ).reduce((acc, w) => {
              const date = new Date(w.startedAt || w.createdAt).toISOString().slice(0, 10);
              if (!acc[date]) acc[date] = [];
              acc[date].push(w);
              return acc;
            }, {} as Record<string, WorklogResponse[]>)
          )
            .sort(([a], [b]) => b.localeCompare(a))
            .map(([date, dayWorklogs]) => (
              <div key={date} className="ab-worklog-group">
                <div className="ab-worklog-date-header">
                  <span className="ab-date-label">{getRelativeDate(date)}</span>
                  <span className="ab-date-total">{formatDuration(dayWorklogs.reduce((s, w) => s + worklogMinutes(w), 0))}</span>
                </div>
                {dayWorklogs.map((worklog) => (
                  <div key={worklog.id} className="ab-worklog-item">
                    {editingId === worklog.id ? (
                      /* Inline Edit Form (GAP 5) */
                      <div className="ab-worklog-edit-form">
                        <div className="ab-form-grid">
                          <div className="ab-form-group">
                            <label className="ab-label">Time Spent</label>
                            <input
                              type="text"
                              className="ab-input ab-input-sm"
                              placeholder="e.g. 2h 30m"
                              value={editTimeInput}
                              onChange={(e) => setEditTimeInput(e.target.value)}
                            />
                          </div>
                          <div className="ab-form-group">
                            <label className="ab-label">Date Started</label>
                            <input
                              type="datetime-local"
                              className="ab-input ab-input-sm"
                              value={editStartDateTime}
                              onChange={(e) => setEditStartDateTime(e.target.value)}
                            />
                          </div>
                        </div>
                        <div className="ab-form-group">
                          <label className="ab-label">Description</label>
                          <textarea
                            className="ab-textarea"
                            value={editDescription}
                            onChange={(e) => setEditDescription(e.target.value)}
                            rows={2}
                          />
                        </div>
                        <div className="ab-form-group">
                          <label className="ab-label">Remaining Estimate</label>
                          <select className="ab-input ab-input-sm" value={editAdjustEstimate} onChange={(e) => setEditAdjustEstimate(e.target.value as RemainingEstimateStrategy)}>
                            <option value="AUTO">Adjust automatically</option>
                            <option value="LEAVE">Leave unchanged</option>
                            <option value="SET">Set to</option>
                            <option value="REDUCE">Reduce by</option>
                          </select>
                          {(editAdjustEstimate === 'SET' || editAdjustEstimate === 'REDUCE') && (
                            <input type="text" className="ab-input ab-input-sm" placeholder="e.g. 3h 30m" value={editAdjustmentInput} onChange={(e) => setEditAdjustmentInput(e.target.value)} style={{ marginTop: '0.25rem' }} />
                          )}
                        </div>
                        <div className="ab-form-actions">
                          <button className="ab-btn ab-btn-ghost ab-btn-sm" onClick={() => setEditingId(null)}>Cancel</button>
                          <button
                            className="ab-btn ab-btn-primary ab-btn-sm"
                            onClick={() => handleUpdate(worklog.id)}
                            disabled={updateMutation.isPending || !parseTimeInput(editTimeInput)}
                          >
                            {updateMutation.isPending ? 'Saving...' : 'Save'}
                          </button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <div className="ab-worklog-time">
                          <span className="ab-time-badge">{formatDuration(worklogMinutes(worklog))}</span>
                        </div>
                        <div className="ab-worklog-details">
                          <div className="ab-worklog-meta">
                            <span className="ab-worklog-author">{worklog.authorName || 'Unknown'}</span>
                            <span className="ab-worklog-time-start">{formatDateTime(worklog.startedAt || worklog.createdAt)}</span>
                          </div>
                          {worklogDescription(worklog) && (
                            <div className="ab-worklog-description">{worklogDescription(worklog)}</div>
                          )}
                        </div>
                        <div className="ab-worklog-actions">
                          <button className="ab-btn-icon" onClick={() => startEdit(worklog)} title="Edit">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                          </button>
                          <button className="ab-btn-icon ab-btn-danger" onClick={() => setConfirmDeleteId(worklog.id)} title="Delete">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                          </button>
                        </div>
                      </>
                    )}

                    {/* Delete confirmation with adjustment options (GAP 2) */}
                    {confirmDeleteId === worklog.id && (
                      <div className="ab-delete-confirm">
                        <p className="ab-delete-title">Delete this worklog?</p>
                        <div className="ab-form-group">
                          <label className="ab-label">Adjust remaining estimate:</label>
                          <select className="ab-input ab-input-sm" value={deleteStrategy} onChange={(e) => setDeleteStrategy(e.target.value as RemainingEstimateStrategy)}>
                            <option value="AUTO">Add time back automatically</option>
                            <option value="LEAVE">Leave estimate unchanged</option>
                            <option value="SET">Set remaining estimate to</option>
                            <option value="INCREASE">Increase remaining by</option>
                          </select>
                          {(deleteStrategy === 'SET' || deleteStrategy === 'INCREASE') && (
                            <input
                              type="text"
                              className="ab-input ab-input-sm"
                              placeholder="e.g. 2h 30m"
                              value={deleteAdjustmentInput}
                              onChange={(e) => setDeleteAdjustmentInput(e.target.value)}
                              style={{ marginTop: '0.5rem' }}
                            />
                          )}
                        </div>
                        <div className="ab-form-actions">
                          <button className="ab-btn ab-btn-ghost ab-btn-sm" onClick={() => setConfirmDeleteId(null)}>Cancel</button>
                          <button
                            className="ab-btn ab-btn-danger ab-btn-sm"
                            onClick={() => {
                              const adjSec = deleteAdjustmentInput ? parseTimeInput(deleteAdjustmentInput) : undefined;
                              deleteMutation.mutate({ worklogId: worklog.id, strategy: deleteStrategy, adjustmentSec: adjSec ?? undefined });
                            }}
                            disabled={deleteMutation.isPending}
                          >
                            {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ))}
        </div>
      ) : (
        <div className="ab-empty-state">
          <div className="ab-empty-state-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--ab-gray-300)" strokeWidth="1.5"><circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" /></svg>
          </div>
          <h4>No time logged</h4>
          <p className="ab-empty-state-description">Track your time by logging work on this issue.</p>
          <button className="ab-btn ab-btn-primary" onClick={() => setShowForm(true)}>Log Your First Entry</button>
        </div>
      )}

      <style>{`
        .ab-worklogs-tab { padding: var(--ab-spacing-md, 1rem) 0; }
        .ab-section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--ab-spacing-md, 1rem); }
        .ab-section-info { display: flex; align-items: center; gap: var(--ab-spacing-md, 1rem); }
        .ab-section-info h3 { font-size: var(--ab-font-size-base, 1rem); font-weight: 600; margin: 0; }
        .ab-section-actions { display: flex; gap: var(--ab-spacing-sm, 0.5rem); }
        .ab-section-actions .ab-btn.active { background: var(--ab-primary-100, #e0e7ff); color: var(--ab-primary-700, #4338ca); }
        .ab-total-time { font-size: var(--ab-font-size-sm, 0.875rem); color: var(--ab-gray-500, #6b7280); }

        /* Three-segment bar (GAP 10) */
        .ab-time-tracking-bar { margin-bottom: var(--ab-spacing-md, 1rem); padding: var(--ab-spacing-sm, 0.5rem) var(--ab-spacing-md, 1rem); background: var(--ab-white, #fff); border: 1px solid var(--ab-gray-200, #e5e7eb); border-radius: var(--ab-radius-md, 0.5rem); }
        .ab-tt-bar-track { display: flex; height: 8px; background: var(--ab-gray-100, #f3f4f6); border-radius: 4px; overflow: hidden; }
        .ab-tt-bar-logged { background: #2563eb; transition: width 0.3s ease; }
        .ab-tt-bar-logged.ab-tt-over { background: #dc2626; }
        .ab-tt-bar-remaining { background: #93c5fd; transition: width 0.3s ease; }
        .ab-tt-bar-labels { display: flex; gap: var(--ab-spacing-md, 1rem); margin-top: 6px; flex-wrap: wrap; }
        .ab-tt-label { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--ab-gray-600, #4b5563); }
        .ab-tt-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
        .ab-tt-dot-est { background: var(--ab-gray-100, #f3f4f6); border: 1px solid var(--ab-gray-300, #d1d5db); }
        .ab-tt-dot-logged { background: #2563eb; }
        .ab-tt-dot-rem { background: #93c5fd; }

        .ab-field-hint { font-size: 11px; color: var(--ab-gray-400, #9ca3af); margin-top: 2px; }
        .ab-quick-times { display: flex; flex-wrap: wrap; gap: var(--ab-spacing-xs, 0.25rem); margin-top: 4px; }
        .ab-btn-xs { padding: 2px 8px; font-size: 11px; }

        .ab-time-reports { background: var(--ab-white, #fff); border: 1px solid var(--ab-gray-200, #e5e7eb); border-radius: var(--ab-radius-md, 0.5rem); padding: var(--ab-spacing-md, 1rem); margin-bottom: var(--ab-spacing-lg, 1.5rem); }
        .ab-report-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--ab-spacing-md, 1rem); margin-bottom: var(--ab-spacing-md, 1rem); }
        .ab-stat-card { text-align: center; padding: var(--ab-spacing-md, 1rem); background: var(--ab-gray-50, #f9fafb); border-radius: var(--ab-radius-md, 0.5rem); }
        .ab-stat-value { display: block; font-size: var(--ab-font-size-xl, 1.25rem); font-weight: 700; color: var(--ab-primary-600, #4f46e5); }
        .ab-stat-label { font-size: var(--ab-font-size-xs, 0.75rem); color: var(--ab-gray-500, #6b7280); }
        .ab-time-chart { margin: var(--ab-spacing-md, 1rem) 0; }
        .ab-chart-bars { display: flex; justify-content: space-around; align-items: flex-end; height: 70px; padding: var(--ab-spacing-sm, 0.5rem); background: var(--ab-gray-50, #f9fafb); border-radius: var(--ab-radius-sm, 0.25rem); }
        .ab-chart-bar-container { display: flex; flex-direction: column; align-items: center; gap: var(--ab-spacing-xs, 0.25rem); }
        .ab-chart-bar { width: 24px; background: var(--ab-primary-400, #818cf8); border-radius: var(--ab-radius-sm, 0.25rem) var(--ab-radius-sm, 0.25rem) 0 0; transition: height 0.3s ease; }
        .ab-chart-bar:hover { background: var(--ab-primary-500, #6366f1); }
        .ab-chart-label { font-size: 10px; color: var(--ab-gray-500, #6b7280); }
        .ab-report-filters { display: flex; align-items: center; gap: var(--ab-spacing-sm, 0.5rem); }
        .ab-date-sep { color: var(--ab-gray-400, #9ca3af); font-size: var(--ab-font-size-sm, 0.875rem); }
        .ab-input-sm { padding: var(--ab-spacing-xs, 0.25rem) var(--ab-spacing-sm, 0.5rem); font-size: var(--ab-font-size-sm, 0.875rem); }

        .ab-worklog-form { margin-bottom: var(--ab-spacing-lg, 1.5rem); }
        .ab-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--ab-spacing-md, 1rem); }
        .ab-form-actions { display: flex; justify-content: flex-end; gap: var(--ab-spacing-sm, 0.5rem); margin-top: var(--ab-spacing-md, 1rem); }

        .ab-worklog-list { display: flex; flex-direction: column; gap: var(--ab-spacing-lg, 1.5rem); }
        .ab-worklog-group { display: flex; flex-direction: column; gap: var(--ab-spacing-xs, 0.25rem); }
        .ab-worklog-date-header { display: flex; justify-content: space-between; align-items: center; padding: var(--ab-spacing-xs, 0.25rem) 0; border-bottom: 1px solid var(--ab-gray-100, #f3f4f6); }
        .ab-date-label { font-size: var(--ab-font-size-sm, 0.875rem); font-weight: 500; color: var(--ab-gray-700, #374151); }
        .ab-date-total { font-size: var(--ab-font-size-sm, 0.875rem); color: var(--ab-primary-600, #4f46e5); font-weight: 600; }
        .ab-worklog-item { display: flex; align-items: flex-start; gap: var(--ab-spacing-md, 1rem); padding: var(--ab-spacing-md, 1rem); background: var(--ab-white, #fff); border: 1px solid var(--ab-gray-200, #e5e7eb); border-radius: var(--ab-radius-md, 0.5rem); flex-wrap: wrap; position: relative; }
        .ab-worklog-time { flex-shrink: 0; }
        .ab-time-badge { display: inline-flex; align-items: center; justify-content: center; padding: var(--ab-spacing-xs, 0.25rem) var(--ab-spacing-sm, 0.5rem); background: var(--ab-primary-100, #e0e7ff); color: var(--ab-primary-700, #4338ca); font-size: var(--ab-font-size-sm, 0.875rem); font-weight: 600; border-radius: var(--ab-radius-md, 0.5rem); }
        .ab-worklog-details { flex: 1; }
        .ab-worklog-meta { display: flex; align-items: center; gap: var(--ab-spacing-sm, 0.5rem); margin-bottom: var(--ab-spacing-xs, 0.25rem); }
        .ab-worklog-author { font-size: var(--ab-font-size-sm, 0.875rem); font-weight: 500; color: var(--ab-gray-700, #374151); }
        .ab-worklog-time-start { font-size: var(--ab-font-size-xs, 0.75rem); color: var(--ab-gray-400, #9ca3af); }
        .ab-worklog-description { font-size: var(--ab-font-size-sm, 0.875rem); color: var(--ab-gray-600, #4b5563); line-height: 1.5; }
        .ab-worklog-actions { display: flex; gap: 4px; flex-shrink: 0; }
        .ab-btn-icon { background: none; border: none; color: var(--ab-gray-400, #9ca3af); cursor: pointer; padding: 4px; border-radius: 4px; }
        .ab-btn-icon:hover { color: var(--ab-primary-600, #4f46e5); background: var(--ab-gray-100, #f3f4f6); }
        .ab-btn-icon.ab-btn-danger:hover { color: var(--ab-danger-500, #ef4444); }
        .ab-worklog-edit-form { width: 100%; }

        .ab-delete-confirm { width: 100%; margin-top: var(--ab-spacing-sm, 0.5rem); padding: var(--ab-spacing-sm, 0.5rem); background: var(--ab-gray-50, #f9fafb); border-radius: var(--ab-radius-sm, 0.25rem); border: 1px solid var(--ab-gray-200, #e5e7eb); }
        .ab-delete-title { font-size: var(--ab-font-size-sm, 0.875rem); font-weight: 600; margin: 0 0 var(--ab-spacing-sm, 0.5rem); }
        .ab-btn-danger { background: var(--ab-danger-500, #ef4444); color: #fff; border: none; }
        .ab-btn-danger:hover { background: var(--ab-danger-600, #dc2626); }

        .ab-empty-state { text-align: center; padding: var(--ab-spacing-xl, 2rem); background: var(--ab-white, #fff); border: 1px solid var(--ab-gray-200, #e5e7eb); border-radius: var(--ab-radius-md, 0.5rem); }
        .ab-empty-state-icon { margin-bottom: var(--ab-spacing-md, 1rem); }
        .ab-empty-state h4 { font-size: var(--ab-font-size-base, 1rem); font-weight: 600; margin: 0 0 var(--ab-spacing-xs, 0.25rem); }
        .ab-empty-state-description { font-size: var(--ab-font-size-sm, 0.875rem); color: var(--ab-gray-500, #6b7280); margin: 0 0 var(--ab-spacing-md, 1rem); }
        .ab-error-banner { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; margin-bottom: 12px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; color: #991b1b; font-size: 0.875rem; }
      `}</style>
    </div>
  );
}
