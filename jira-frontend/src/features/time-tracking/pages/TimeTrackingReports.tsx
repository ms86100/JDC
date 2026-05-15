import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { worklogApi, WorklogResponse } from '../../../api/worklogApi';
import { projectApi } from '../../../api/projectApi';

const formatDuration = (mins: number) => {
  const hours = Math.floor(mins / 60);
  const remainingMins = mins % 60;
  if (hours === 0) return `${remainingMins}m`;
  if (remainingMins === 0) return `${hours}h`;
  return `${hours}h ${remainingMins}m`;
};

interface TimeReport {
  userId: string;
  userName: string;
  totalMinutes: number;
  issueCount: number;
  logs: WorklogResponse[];
}

export default function TimeTrackingReports() {
  const [dateRange, setDateRange] = useState({
    start: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    end: new Date().toISOString().split('T')[0],
  });
  const [selectedUser, setSelectedUser] = useState<string>('all');
  const [selectedProject, setSelectedProject] = useState<string>('');

  // Fetch projects for filter dropdown
  const { data: projects } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.getAll().then(res => res.data || []),
  });

  // Fetch all worklogs (in real implementation, this would be filtered by date range and project)
  const { data: allWorklogs, isLoading } = useQuery<WorklogResponse[]>({
    queryKey: ['worklogs', selectedProject, dateRange.start, dateRange.end],
    queryFn: async () => {
      // In production, you'd call a reporting endpoint like /api/worklogs/reports?startDate=&endDate=
      // For now, we'll aggregate from individual issue worklogs
      // This is a simplified implementation - real app would have a dedicated reports endpoint
      const response = await worklogApi.getAll('');
      return response.data || [];
    },
    enabled: false, // Disable auto-fetch, we'll aggregate client-side for demo
  });

  // Aggregate worklogs by user
  const reports: TimeReport[] = useMemo(() => {
    if (!allWorklogs) return [];

    // Filter by date range
    const filteredLogs = allWorklogs.filter(log => {
      const logDate = new Date(log.startedAt).toISOString().split('T')[0];
      return logDate >= dateRange.start && logDate <= dateRange.end;
    });

    // Group by user
    const byUser = new Map<string, WorklogResponse[]>();
    filteredLogs.forEach(log => {
      const existing = byUser.get(log.authorId) || [];
      existing.push(log);
      byUser.set(log.authorId, existing);
    });

    // Convert to TimeReport
    return Array.from(byUser.entries()).map(([userId, logs]) => ({
      userId,
      userName: logs[0]?.authorName || userId.split('-')[0],
      totalMinutes: logs.reduce((sum, l) => sum + l.timeWorkedMinutes, 0),
      issueCount: new Set(logs.map(l => l.issueId)).size,
      logs,
    }));
  }, [allWorklogs, dateRange]);

  // Calculate stats from real data
  const stats = useMemo(() => {
    if (reports.length === 0) {
      return { totalTime: 0, avgPerUser: 0, totalIssues: 0 };
    }
    const totalMins = reports.reduce((sum, r) => sum + r.totalMinutes, 0);
    return {
      totalTime: totalMins,
      avgPerUser: totalMins / reports.length,
      totalIssues: reports.reduce((sum, r) => sum + r.issueCount, 0),
    };
  }, [reports]);

  // Calculate daily totals for chart
  const dailyData = useMemo(() => {
    if (!allWorklogs) return [];
    const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const byDay = new Map<number, number>();

    allWorklogs.forEach(log => {
      const dayIndex = new Date(log.startedAt).getDay();
      byDay.set(dayIndex, (byDay.get(dayIndex) || 0) + log.timeWorkedMinutes);
    });

    return days.map((day, index) => ({
      day,
      minutes: byDay.get(index) || 0,
    }));
  }, [allWorklogs]);

  return (
    <div className="ab-time-reports-page">
      <div className="ab-page-header">
        <div>
          <h1>Time Tracking Reports</h1>
          <p>Track and analyze time spent across your projects</p>
        </div>
      </div>

      {/* Filters */}
      <div className="ab-reports-filters">
        <div className="ab-filter-group">
          <label>Date Range</label>
          <div className="ab-date-range-picker">
            <input
              type="date"
              className="ab-input"
              value={dateRange.start}
              onChange={(e) => setDateRange({ ...dateRange, start: e.target.value })}
            />
            <span>to</span>
            <input
              type="date"
              className="ab-input"
              value={dateRange.end}
              onChange={(e) => setDateRange({ ...dateRange, end: e.target.value })}
            />
          </div>
        </div>

        <div className="ab-filter-group">
          <label>Team Member</label>
          <select
            className="ab-select"
            value={selectedUser}
            onChange={(e) => setSelectedUser(e.target.value)}
          >
            <option value="all">All Members</option>
            {reports.map(r => (
              <option key={r.userId} value={r.userId}>{r.userName}</option>
            ))}
          </select>
        </div>

        <button className="ab-btn ab-btn-secondary">Export Report</button>
      </div>

      {/* Summary Stats */}
      <div className="ab-summary-grid">
        <div className="ab-stat-card">
          <span className="ab-stat-icon">⏱️</span>
          <div className="ab-stat-content">
            <span className="ab-stat-value">{formatDuration(stats.totalTime)}</span>
            <span className="ab-stat-label">Total Time Logged</span>
          </div>
        </div>
        <div className="ab-stat-card">
          <span className="ab-stat-icon">👥</span>
          <div className="ab-stat-content">
            <span className="ab-stat-value">{reports.length}</span>
            <span className="ab-stat-label">Active Members</span>
          </div>
        </div>
        <div className="ab-stat-card">
          <span className="ab-stat-icon">📋</span>
          <div className="ab-stat-content">
            <span className="ab-stat-value">{stats.totalIssues}</span>
            <span className="ab-stat-label">Issues Worked On</span>
          </div>
        </div>
        <div className="ab-stat-card">
          <span className="ab-stat-icon">📊</span>
          <div className="ab-stat-content">
            <span className="ab-stat-value">{formatDuration(Math.round(stats.avgPerUser))}</span>
            <span className="ab-stat-label">Avg per Member</span>
          </div>
        </div>
      </div>

      {/* Team Breakdown */}
      <div className="ab-card">
        <div className="ab-card-header">
          <h3>Time by Team Member</h3>
        </div>
        <div className="ab-card-body">
          {isLoading ? (
            <div className="ab-loading-state">Loading time data...</div>
          ) : reports.length === 0 ? (
            <div className="ab-empty-state">No time logged in this period</div>
          ) : (
            <table className="ab-table">
              <thead>
                <tr>
                  <th>Member</th>
                  <th>Time Logged</th>
                  <th>Issues</th>
                  <th>Avg per Issue</th>
                  <th>Progress</th>
                </tr>
              </thead>
              <tbody>
                {reports
                  .filter(r => selectedUser === 'all' || r.userId === selectedUser)
                  .map((report) => (
                  <tr key={report.userId}>
                    <td>
                      <div className="ab-user-cell">
                        <span className="ab-avatar">
                          {report.userName.charAt(0).toUpperCase()}
                        </span>
                        <span>{report.userName}</span>
                      </div>
                    </td>
                    <td className="ab-time-cell">{formatDuration(report.totalMinutes)}</td>
                    <td>{report.issueCount}</td>
                    <td>{formatDuration(Math.round(report.totalMinutes / (report.issueCount || 1)))}</td>
                    <td>
                      <div className="ab-progress-bar-mini">
                        <div
                          className="ab-progress-fill"
                          style={{ width: `${stats.totalTime > 0 ? (report.totalMinutes / stats.totalTime) * 100 : 0}%` }}
                        />
                      </div>
                      <span className="ab-progress-label">
                        {stats.totalTime > 0 ? ((report.totalMinutes / stats.totalTime) * 100).toFixed(0) : 0}%
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Daily Breakdown */}
      <div className="ab-card">
        <div className="ab-card-header">
          <h3>Daily Activity</h3>
        </div>
        <div className="ab-card-body">
          {dailyData.length === 0 || dailyData.every(d => d.minutes === 0) ? (
            <div className="ab-empty-state">No daily data available</div>
          ) : (
            <div className="ab-daily-chart">
              {dailyData.map((d) => {
                const maxMinutes = Math.max(...dailyData.map(x => x.minutes), 1);
                const height = d.minutes > 0 ? 30 + (d.minutes / maxMinutes) * 70 : 10;
                return (
                  <div key={d.day} className="ab-day-bar">
                    <div
                      className="ab-bar"
                      style={{ height: `${height}%` }}
                      title={`${formatDuration(d.minutes)}`}
                    />
                    <span className="ab-day-label">{d.day}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      <style>{`
        .ab-time-reports-page {
          padding: var(--ab-spacing-lg);
          max-width: 1200px;
          margin: 0 auto;
        }

        .ab-page-header {
          margin-bottom: var(--ab-spacing-xl);
        }

        .ab-page-header h1 {
          font-size: var(--ab-font-size-2xl);
          font-weight: 700;
          margin: 0 0 var(--ab-spacing-xs);
        }

        .ab-page-header p {
          color: var(--ab-gray-500);
          margin: 0;
        }

        .ab-reports-filters {
          display: flex;
          align-items: flex-end;
          gap: var(--ab-spacing-lg);
          margin-bottom: var(--ab-spacing-xl);
          padding: var(--ab-spacing-md);
          background: var(--ab-white);
          border-radius: var(--ab-radius-md);
          border: 1px solid var(--ab-gray-200);
        }

        .ab-filter-group {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-xs);
        }

        .ab-filter-group label {
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-600);
        }

        .ab-date-range-picker {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
        }

        .ab-date-range-picker span {
          color: var(--ab-gray-400);
        }

        .ab-summary-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: var(--ab-spacing-md);
          margin-bottom: var(--ab-spacing-xl);
        }

        .ab-stat-card {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
          padding: var(--ab-spacing-lg);
          background: var(--ab-white);
          border-radius: var(--ab-radius-md);
          border: 1px solid var(--ab-gray-200);
        }

        .ab-stat-icon {
          font-size: 32px;
        }

        .ab-stat-value {
          display: block;
          font-size: var(--ab-font-size-xl);
          font-weight: 700;
          color: var(--ab-gray-800);
        }

        .ab-stat-label {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
        }

        .ab-card {
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-card-header {
          padding: var(--ab-spacing-md) var(--ab-spacing-lg);
          border-bottom: 1px solid var(--ab-gray-100);
        }

        .ab-card-header h3 {
          margin: 0;
          font-size: var(--ab-font-size-base);
          font-weight: 600;
        }

        .ab-card-body {
          padding: var(--ab-spacing-lg);
        }

        .ab-user-cell {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
        }

        .ab-avatar {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          background: var(--ab-primary-500);
          color: white;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
        }

        .ab-time-cell {
          font-family: var(--ab-font-mono);
          font-weight: 500;
        }

        .ab-progress-bar-mini {
          width: 100px;
          height: 6px;
          background: var(--ab-gray-200);
          border-radius: 3px;
          overflow: hidden;
          display: inline-block;
          margin-right: var(--ab-spacing-sm);
        }

        .ab-progress-fill {
          height: 100%;
          background: var(--ab-primary-500);
        }

        .ab-progress-label {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
        }

        .ab-daily-chart {
          display: flex;
          justify-content: space-around;
          align-items: flex-end;
          height: 200px;
          padding: var(--ab-spacing-md) 0;
        }

        .ab-day-bar {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--ab-spacing-sm);
          width: 60px;
        }

        .ab-bar {
          width: 40px;
          background: linear-gradient(180deg, var(--ab-primary-400), var(--ab-primary-600));
          border-radius: var(--ab-radius-sm) var(--ab-radius-sm) 0 0;
          transition: height 0.3s ease;
        }

        .ab-bar:hover {
          background: linear-gradient(180deg, var(--ab-primary-500), var(--ab-primary-700));
        }

        .ab-day-label {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
        }

        @media (max-width: 1024px) {
          .ab-summary-grid {
            grid-template-columns: repeat(2, 1fr);
          }
        }
      `}</style>
    </div>
  );
}