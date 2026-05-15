import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { worklogApi, WorklogResponse } from '../../../api/worklogApi';

interface WorklogsTabProps {
  issueId: string;
}

interface TimeEntry {
  date: string;
  minutes: number;
}

export default function WorklogsTab({ issueId }: WorklogsTabProps) {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [showReports, setShowReports] = useState(false);
  const [minutes, setMinutes] = useState(60);
  const [description, setDescription] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const { data: worklogs, isLoading } = useQuery<WorklogResponse[]>({
    queryKey: ['worklogs', issueId],
    queryFn: async () => {
      const response = await worklogApi.getAll(issueId);
      return response.data;
    },
    enabled: !!issueId,
  });

  const { data: totalTime } = useQuery<number>({
    queryKey: ['worklogs-total', issueId],
    queryFn: async () => {
      const response = await worklogApi.getTotalTime(issueId);
      return response.data;
    },
    enabled: !!issueId,
  });

  const createMutation = useMutation({
    mutationFn: (data: { timeWorkedMinutes: number; description?: string }) =>
      worklogApi.create(issueId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['worklogs', issueId] });
      queryClient.invalidateQueries({ queryKey: ['worklogs-total', issueId] });
      setShowForm(false);
      setMinutes(60);
      setDescription('');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (worklogId: string) => worklogApi.delete(issueId, worklogId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['worklogs', issueId] });
      queryClient.invalidateQueries({ queryKey: ['worklogs-total', issueId] });
    },
  });

  const formatDuration = (mins: number) => {
    const hours = Math.floor(mins / 60);
    const remainingMins = mins % 60;
    if (hours === 0) return `${remainingMins}m`;
    if (remainingMins === 0) return `${hours}h`;
    return `${hours}h ${remainingMins}m`;
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
    });
  };

  const formatDateTime = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit',
    });
  };

  const getRelativeDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    return formatDate(dateStr);
  };

  // Calculate stats
  const calculateStats = () => {
    if (!worklogs || worklogs.length === 0) return null;

    const totalMinutes = worklogs.reduce((sum, w) => sum + w.timeWorkedMinutes, 0);
    const avgPerDay = totalMinutes / 7; // Last 7 days
    const thisWeek = worklogs.filter(w => {
      const date = new Date(w.createdAt);
      const weekAgo = new Date();
      weekAgo.setDate(weekAgo.getDate() - 7);
      return date >= weekAgo;
    });
    const thisWeekMinutes = thisWeek.reduce((sum, w) => sum + w.timeWorkedMinutes, 0);

    // Group by day
    const byDay: Record<string, number> = {};
    worklogs.forEach(w => {
      const day = new Date(w.createdAt).toLocaleDateString();
      byDay[day] = (byDay[day] || 0) + w.timeWorkedMinutes;
    });

    return { totalMinutes, avgPerDay, thisWeekMinutes, byDay };
  };

  const stats = calculateStats();

  // Render mini chart
  const renderMiniChart = () => {
    if (!stats || !stats.byDay || Object.keys(stats.byDay).length === 0) return null;

    const entries = Object.entries(stats.byDay).slice(-7);
    const maxVal = Math.max(...entries.map(([, v]) => v), 1);

    return (
      <div className="ab-time-chart">
        <div className="ab-chart-bars">
          {entries.map(([date, mins]) => {
            const height = (mins / maxVal) * 60;
            return (
              <div key={date} className="ab-chart-bar-container" title={`${formatDuration(mins)}`}>
                <div className="ab-chart-bar" style={{ height: `${height}px` }} />
                <span className="ab-chart-label">
                  {new Date(date).toLocaleDateString('en-US', { weekday: 'short' })}
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
          {totalTime !== undefined && totalTime > 0 && (
            <span className="ab-total-time">
              Total: <strong>{formatDuration(totalTime)}</strong>
            </span>
          )}
        </div>
        <div className="ab-section-actions">
          <button
            className={`ab-btn ab-btn-ghost ab-btn-sm ${showReports ? 'active' : ''}`}
            onClick={() => setShowReports(!showReports)}
          >
            📊 Reports
          </button>
          <button
            className="ab-btn ab-btn-primary ab-btn-sm"
            onClick={() => setShowForm(!showForm)}
          >
            {showForm ? 'Cancel' : '⏱️ Log Work'}
          </button>
        </div>
      </div>

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
            <input
              type="date"
              className="ab-input ab-input-sm"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              placeholder="Start date"
            />
            <span className="ab-date-sep">to</span>
            <input
              type="date"
              className="ab-input ab-input-sm"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              placeholder="End date"
            />
          </div>
        </div>
      )}

      {showForm && (
        <div className="ab-worklog-form ab-card">
          <div className="ab-card-body">
            <div className="ab-form-grid">
              <div className="ab-form-group">
                <label className="ab-label">Time Worked</label>
                <div className="ab-time-input-group">
                  <div className="ab-stepper">
                    <button
                      className="ab-stepper-btn"
                      onClick={() => setMinutes(Math.max(15, minutes - 15))}
                    >
                      −
                    </button>
                    <span className="ab-stepper-value">{formatDuration(minutes)}</span>
                    <button
                      className="ab-stepper-btn"
                      onClick={() => setMinutes(minutes + 15)}
                    >
                      +
                    </button>
                  </div>
                  <div className="ab-quick-times">
                    {[
                      { label: '15m', val: 15 },
                      { label: '30m', val: 30 },
                      { label: '1h', val: 60 },
                      { label: '2h', val: 120 },
                      { label: '4h', val: 240 },
                      { label: '8h', val: 480 },
                    ].map(({ label, val }) => (
                      <button
                        key={val}
                        type="button"
                        className="ab-btn ab-btn-ghost ab-btn-sm"
                        onClick={() => setMinutes(val)}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Work Period</label>
                <input
                  type="date"
                  className="ab-input"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Description</label>
              <textarea
                className="ab-textarea"
                placeholder="What did you work on? Be specific about the tasks completed..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
              />
            </div>

            <div className="ab-form-actions">
              <button className="ab-btn ab-btn-secondary" onClick={() => setShowForm(false)}>
                Cancel
              </button>
              <button
                className="ab-btn ab-btn-primary"
                onClick={() => createMutation.mutate({ timeWorkedMinutes: minutes, description })}
                disabled={createMutation.isPending || minutes < 15}
              >
                {createMutation.isPending ? 'Logging...' : 'Log Work'}
              </button>
            </div>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
        </div>
      ) : worklogs && worklogs.length > 0 ? (
        <div className="ab-worklog-list">
          {/* Group by date */}
          {Object.entries(
            worklogs.reduce((acc, w) => {
              const date = new Date(w.createdAt).toLocaleDateString();
              if (!acc[date]) acc[date] = [];
              acc[date].push(w);
              return acc;
            }, {} as Record<string, WorklogResponse[]>)
          )
            .sort(([a], [b]) => new Date(b).getTime() - new Date(a).getTime())
            .map(([date, dayWorklogs]) => (
              <div key={date} className="ab-worklog-group">
                <div className="ab-worklog-date-header">
                  <span className="ab-date-label">{getRelativeDate(date)}</span>
                  <span className="ab-date-total">
                    {formatDuration(dayWorklogs.reduce((s, w) => s + w.timeWorkedMinutes, 0))}
                  </span>
                </div>
                {dayWorklogs.map((worklog) => (
                  <div key={worklog.id} className="ab-worklog-item">
                    <div className="ab-worklog-time">
                      <span className="ab-time-badge">{formatDuration(worklog.timeWorkedMinutes)}</span>
                    </div>
                    <div className="ab-worklog-details">
                      <div className="ab-worklog-meta">
                        <span className="ab-worklog-author">{worklog.authorName || 'Unknown'}</span>
                        <span className="ab-worklog-time-start">{formatDateTime(worklog.createdAt)}</span>
                      </div>
                      {worklog.description && (
                        <div className="ab-worklog-description">{worklog.description}</div>
                      )}
                    </div>
                    <button
                      className="ab-btn-icon"
                      onClick={() => {
                        if (confirm('Delete this worklog?')) {
                          deleteMutation.mutate(worklog.id);
                        }
                      }}
                      title="Delete"
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            ))}
        </div>
      ) : (
        <div className="ab-empty-state">
          <div className="ab-empty-state-icon">⏱️</div>
          <h4>No time logged</h4>
          <p className="ab-empty-state-description">Track your time by logging work on this issue.</p>
          <button
            className="ab-btn ab-btn-primary"
            onClick={() => setShowForm(true)}
          >
            Log Your First Entry
          </button>
        </div>
      )}

      <style>{`
        .ab-worklogs-tab {
          padding: var(--ab-spacing-md) 0;
        }

        .ab-section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-section-info {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
        }

        .ab-section-info h3 {
          font-size: var(--ab-font-size-base);
          font-weight: 600;
          margin: 0;
        }

        .ab-section-actions {
          display: flex;
          gap: var(--ab-spacing-sm);
        }

        .ab-section-actions .ab-btn.active {
          background: var(--ab-primary-100);
          color: var(--ab-primary-700);
        }

        .ab-total-time {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
        }

        .ab-time-reports {
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-md);
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-report-summary {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--ab-spacing-md);
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-stat-card {
          text-align: center;
          padding: var(--ab-spacing-md);
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-md);
        }

        .ab-stat-value {
          display: block;
          font-size: var(--ab-font-size-xl);
          font-weight: 700;
          color: var(--ab-primary-600);
        }

        .ab-stat-label {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
        }

        .ab-time-chart {
          margin: var(--ab-spacing-md) 0;
        }

        .ab-chart-bars {
          display: flex;
          justify-content: space-around;
          align-items: flex-end;
          height: 70px;
          padding: var(--ab-spacing-sm);
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-sm);
        }

        .ab-chart-bar-container {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--ab-spacing-xs);
        }

        .ab-chart-bar {
          width: 24px;
          background: var(--ab-primary-400);
          border-radius: var(--ab-radius-sm) var(--ab-radius-sm) 0 0;
          transition: height 0.3s ease;
        }

        .ab-chart-bar:hover {
          background: var(--ab-primary-500);
        }

        .ab-chart-label {
          font-size: 10px;
          color: var(--ab-gray-500);
        }

        .ab-report-filters {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
        }

        .ab-date-sep {
          color: var(--ab-gray-400);
          font-size: var(--ab-font-size-sm);
        }

        .ab-input-sm {
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          font-size: var(--ab-font-size-sm);
        }

        .ab-worklog-form {
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-form-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: var(--ab-spacing-md);
        }

        .ab-time-input-group {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
        }

        .ab-stepper {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-sm);
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-md);
        }

        .ab-stepper-btn {
          width: 32px;
          height: 32px;
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-sm);
          background: var(--ab-white);
          cursor: pointer;
          font-size: 18px;
          font-weight: 600;
          transition: all var(--ab-transition-fast);
        }

        .ab-stepper-btn:hover {
          background: var(--ab-primary-50);
          border-color: var(--ab-primary-400);
        }

        .ab-stepper-value {
          flex: 1;
          text-align: center;
          font-size: var(--ab-font-size-lg);
          font-weight: 600;
          color: var(--ab-primary-600);
        }

        .ab-quick-times {
          display: flex;
          flex-wrap: wrap;
          gap: var(--ab-spacing-xs);
        }

        .ab-form-actions {
          display: flex;
          justify-content: flex-end;
          gap: var(--ab-spacing-sm);
          margin-top: var(--ab-spacing-md);
        }

        .ab-worklog-list {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-lg);
        }

        .ab-worklog-group {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-xs);
        }

        .ab-worklog-date-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: var(--ab-spacing-xs) 0;
          border-bottom: 1px solid var(--ab-gray-100);
        }

        .ab-date-label {
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-700);
        }

        .ab-date-total {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-primary-600);
          font-weight: 600;
        }

        .ab-worklog-item {
          display: flex;
          align-items: flex-start;
          gap: var(--ab-spacing-md);
          padding: var(--ab-spacing-md);
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
        }

        .ab-worklog-time {
          flex-shrink: 0;
        }

        .ab-time-badge {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          background: var(--ab-primary-100);
          color: var(--ab-primary-700);
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
          border-radius: var(--ab-radius-md);
        }

        .ab-worklog-details {
          flex: 1;
        }

        .ab-worklog-meta {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-worklog-author {
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-700);
        }

        .ab-worklog-time-start {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-400);
        }

        .ab-worklog-description {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-600);
          line-height: 1.5;
        }

        .ab-btn-icon {
          background: none;
          border: none;
          font-size: 20px;
          color: var(--ab-gray-400);
          cursor: pointer;
          padding: var(--ab-spacing-xs);
        }

        .ab-btn-icon:hover {
          color: var(--ab-danger-500);
        }

        .ab-empty-state {
          text-align: center;
          padding: var(--ab-spacing-xl);
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
        }

        .ab-empty-state-icon {
          font-size: 48px;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-empty-state h4 {
          font-size: var(--ab-font-size-base);
          font-weight: 600;
          margin: 0 0 var(--ab-spacing-xs);
        }

        .ab-empty-state-description {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
          margin: 0 0 var(--ab-spacing-md);
        }
      `}</style>
    </div>
  );
}