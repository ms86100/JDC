import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { sprintApi, SprintReportResponse } from '../../../api/sprintApi';
import BurndownChart from './BurndownChart';
import VelocityChart from './VelocityChart';
import CumulativeFlowChart from '../../plans/components/sprint/CumulativeFlowChart';
import ControlChart from '../../plans/components/sprint/ControlChart';
import { useSprintReport, useCumulativeFlow, useControlChart, SprintReportResponse as PlanSprintReportResponse } from '../../plans/hooks/useSprint';
import { chartColors } from '../../../utils/chartColors';

interface SprintReportModalProps {
  sprintId: string;
  sprintName: string;
  boardId?: string;
  onClose: () => void;
}

type ReportTab = 'overview' | 'burndown' | 'velocity' | 'distribution' | 'cfd' | 'controlChart';

export default function SprintReportModal({ sprintId, sprintName, boardId, onClose }: SprintReportModalProps) {
  const [activeTab, setActiveTab] = useState<ReportTab>('overview');

  const { data: report, isLoading } = useQuery<SprintReportResponse>({
    queryKey: ['sprint-report', sprintId],
    queryFn: () => sprintApi.getReport(sprintId).then(res => res.data),
    enabled: !!sprintId,
  });

  // Gap 10: Enhanced sprint report from plan service
  const { data: planReport } = useSprintReport(sprintId);
  // Gap 13: CFD data
  const { data: cfdData } = useCumulativeFlow(boardId || '');
  // Gap 14: Control chart data
  const { data: controlData } = useControlChart(boardId || '');

  const getProgressColor = (rate: number) => {
    if (rate >= 80) return chartColors.success;
    if (rate >= 50) return chartColors.warning;
    return chartColors.danger;
  };

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="ab-report-overlay" onClick={onClose}>
      <div className="ab-report-modal" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ab-modal-header">
          <div className="ab-header-content">
            <h2>Sprint Report: {sprintName}</h2>
            {report && (
              <span className={`ab-status-badge ab-status-${report.status.toLowerCase()}`}>
                {report.status}
              </span>
            )}
          </div>
          <button className="ab-close-btn" onClick={onClose}>
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
            </svg>
          </button>
        </div>

        {/* Tabs */}
        <div className="ab-report-tabs">
          <button
            className={`ab-report-tab ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            Overview
          </button>
          <button
            className={`ab-report-tab ${activeTab === 'burndown' ? 'active' : ''}`}
            onClick={() => setActiveTab('burndown')}
          >
            Burndown
          </button>
          <button
            className={`ab-report-tab ${activeTab === 'velocity' ? 'active' : ''}`}
            onClick={() => setActiveTab('velocity')}
          >
            Velocity
          </button>
          <button
            className={`ab-report-tab ${activeTab === 'distribution' ? 'active' : ''}`}
            onClick={() => setActiveTab('distribution')}
          >
            Distribution
          </button>
          <button
            className={`ab-report-tab ${activeTab === 'cfd' ? 'active' : ''}`}
            onClick={() => setActiveTab('cfd')}
          >
            CFD
          </button>
          <button
            className={`ab-report-tab ${activeTab === 'controlChart' ? 'active' : ''}`}
            onClick={() => setActiveTab('controlChart')}
          >
            Control Chart
          </button>
        </div>

        {/* Content */}
        <div className="ab-modal-body">
          {isLoading ? (
            <div className="ab-loading-state">
              <div className="ab-spinner"></div>
              <p>Loading report data...</p>
            </div>
          ) : report ? (
            <>
              {activeTab === 'overview' && (
                <div className="ab-overview-tab">
                  {/* Date Range */}
                  <div className="ab-date-range">
                    <span>📅 {formatDate(report.startDate)}</span>
                    <span className="ab-date-separator">→</span>
                    <span>📅 {formatDate(report.endDate)}</span>
                    {report.daysRemaining > 0 && (
                      <span className="ab-days-remaining">{report.daysRemaining} days remaining</span>
                    )}
                  </div>

                  {/* Issue Metrics */}
                  <div className="ab-metrics-grid">
                    <div className="ab-metric-card">
                      <div className="ab-metric-value">{report.totalIssues}</div>
                      <div className="ab-metric-label">Total Issues</div>
                    </div>
                    <div className="ab-metric-card ab-metric-success">
                      <div className="ab-metric-value">{report.completedIssues}</div>
                      <div className="ab-metric-label">Completed</div>
                    </div>
                    <div className="ab-metric-card ab-metric-primary">
                      <div className="ab-metric-value">{report.inProgressIssues}</div>
                      <div className="ab-metric-label">In Progress</div>
                    </div>
                    <div className="ab-metric-card">
                      <div className="ab-metric-value">{report.todoIssues}</div>
                      <div className="ab-metric-label">To Do</div>
                    </div>
                  </div>

                  {/* Progress Bar */}
                  <div className="ab-progress-section">
                    <div className="ab-progress-header">
                      <span>Completion Progress</span>
                      <span>{report.completionRate.toFixed(1)}%</span>
                    </div>
                    <div className="ab-progress-bar">
                      <div
                        className="ab-progress-fill"
                        style={{
                          width: `${report.completionRate}%`,
                          backgroundColor: getProgressColor(report.completionRate)
                        }}
                      />
                    </div>
                  </div>

                  {/* Points Summary */}
                  <div className="ab-points-summary">
                    <div className="ab-point-item">
                      <span className="ab-point-label">Committed</span>
                      <span className="ab-point-value">{report.totalPoints} pts</span>
                    </div>
                    <div className="ab-point-item">
                      <span className="ab-point-label">Completed</span>
                      <span className="ab-point-value ab-point-completed">{report.completedPoints} pts</span>
                    </div>
                    <div className="ab-point-item">
                      <span className="ab-point-label">Remaining</span>
                      <span className="ab-point-value ab-point-remaining">{report.remainingPoints} pts</span>
                    </div>
                  </div>

                  {/* Burn Rate */}
                  {report.dailyBurnRate > 0 && (
                    <div className="ab-burn-rate">
                      <span className="ab-burn-icon">🔥</span>
                      <div className="ab-burn-info">
                        <span className="ab-burn-value">{report.dailyBurnRate.toFixed(1)} pts/day</span>
                        <span className="ab-burn-label">Current burn rate</span>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {activeTab === 'burndown' && report.burndown && (
                <div className="ab-burndown-tab">
                  <BurndownChart
                    data={report.burndown.dailyData}
                    totalPoints={report.burndown.totalPoints}
                  />

                  <div className="ab-burndown-stats">
                    <div className="ab-stat">
                      <span className="ab-stat-label">Sprint Progress</span>
                      <span className="ab-stat-value">{report.burndown.completionRate.toFixed(1)}%</span>
                    </div>
                    <div className="ab-stat">
                      <span className="ab-stat-label">Points Remaining</span>
                      <span className="ab-stat-value">{report.burndown.remainingPoints}</span>
                    </div>
                    <div className="ab-stat">
                      <span className="ab-stat-label">Issues Completed</span>
                      <span className="ab-stat-value">{report.burndown.completedIssues}</span>
                    </div>
                  </div>
                </div>
              )}

              {activeTab === 'velocity' && report.velocity && (
                <div className="ab-velocity-tab">
                  <div className="ab-velocity-summary">
                    <div className="ab-velocity-stat">
                      <span className="ab-velocity-value">{report.velocity.currentVelocity}</span>
                      <span className="ab-velocity-label">Current Velocity</span>
                    </div>
                    <div className="ab-velocity-stat">
                      <span className="ab-velocity-value">{report.velocity.averageVelocity.toFixed(1)}</span>
                      <span className="ab-velocity-label">Average Velocity</span>
                    </div>
                    <div className="ab-velocity-stat">
                      <span className="ab-velocity-value">{report.velocity.velocityTrend > 0 ? '+' : ''}{report.velocity.velocityTrend.toFixed(1)}%</span>
                      <span className="ab-velocity-label">Trend</span>
                    </div>
                  </div>

                  <VelocityChart
                    data={report.velocity.sprintVelocities}
                    averageVelocity={report.velocity.averageVelocity}
                  />
                </div>
              )}

              {/* Gap 10: Enhanced report data — Scope Change & Punted Issues */}
              {activeTab === 'overview' && planReport && (
                <div style={{ marginTop: '16px' }}>
                  {planReport.puntedIssues && planReport.puntedIssues.length > 0 && (
                    <div style={{ marginBottom: '12px', padding: '12px', background: chartColors.dangerLight, borderRadius: '8px', border: `1px solid ${chartColors.danger}` }}>
                      <h4 style={{ margin: '0 0 8px', fontSize: '0.875rem', color: chartColors.danger }}>Punted Issues ({planReport.puntedIssues.length})</h4>
                      {planReport.puntedIssues.slice(0, 5).map(i => (
                        <div key={i.id} style={{ fontSize: '0.813rem', color: chartColors.neutral900, padding: '2px 0' }}>{i.issueId}</div>
                      ))}
                      {planReport.puntedIssues.length > 5 && <div style={{ fontSize: '0.75rem', color: chartColors.neutral400 }}>+{planReport.puntedIssues.length - 5} more</div>}
                    </div>
                  )}
                  {planReport.issueKeysAddedDuringSprint && planReport.issueKeysAddedDuringSprint.length > 0 && (
                    <div style={{ padding: '12px', background: chartColors.neutral50, borderRadius: '8px', border: `1px solid ${chartColors.warning}` }}>
                      <h4 style={{ margin: '0 0 8px', fontSize: '0.875rem', color: chartColors.warningDark }}>Scope Change: {planReport.issueKeysAddedDuringSprint.length} issues added after start</h4>
                      <div style={{ fontSize: '0.813rem', color: chartColors.warningDark }}>Scope change points: {planReport.scopeChangePoints}</div>
                    </div>
                  )}
                </div>
              )}

              {/* Gap 13: CFD tab */}
              {activeTab === 'cfd' && (
                <div style={{ padding: '16px 0' }}>
                  {cfdData ? (
                    <CumulativeFlowChart data={cfdData} />
                  ) : (
                    <div className="ab-empty-state"><p>No cumulative flow data available</p></div>
                  )}
                </div>
              )}

              {/* Gap 14: Control Chart tab */}
              {activeTab === 'controlChart' && (
                <div style={{ padding: '16px 0' }}>
                  {controlData ? (
                    <ControlChart data={controlData} />
                  ) : (
                    <div className="ab-empty-state"><p>No control chart data available</p></div>
                  )}
                </div>
              )}

              {activeTab === 'distribution' && (
                <div className="ab-distribution-tab">
                  {report.issuesByStatus && (
                    <div className="ab-dist-section">
                      <h3>Issues by Status</h3>
                      <div className="ab-dist-bars">
                        {Object.entries(report.issuesByStatus).map(([status, count]) => (
                          <div key={status} className="ab-dist-item">
                            <span className="ab-dist-label">{status}</span>
                            <div className="ab-dist-bar-container">
                              <div
                                className="ab-dist-bar"
                                style={{ width: `${(count / report.totalIssues) * 100}%` }}
                              />
                            </div>
                            <span className="ab-dist-count">{count}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {report.issuesByType && (
                    <div className="ab-dist-section">
                      <h3>Issues by Type</h3>
                      <div className="ab-dist-grid">
                        {Object.entries(report.issuesByType).map(([type, count]) => (
                          <div key={type} className="ab-dist-card">
                            <span className="ab-dist-type-icon">
                              {type === 'Bug' ? '🐛' : type === 'Story' ? '📖' : type === 'Epic' ? '⚡' : '✓'}
                            </span>
                            <span className="ab-dist-type-name">{type}</span>
                            <span className="ab-dist-type-count">{count}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {report.issuesByPriority && (
                    <div className="ab-dist-section">
                      <h3>Issues by Priority</h3>
                      <div className="ab-priority-list">
                        {Object.entries(report.issuesByPriority).map(([priority, count]) => (
                          <div key={priority} className="ab-priority-item">
                            <span className="ab-priority-icon">
                              {priority === 'Highest' ? '🔴' : priority === 'High' ? '🟠' :
                               priority === 'Medium' ? '🟡' : '🟢'}
                            </span>
                            <span className="ab-priority-name">{priority}</span>
                            <span className="ab-priority-count">{count}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </>
          ) : (
            <div className="ab-empty-state">
              <p>No report data available</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="ab-modal-footer">
          <button className="ab-btn ab-btn-secondary" onClick={() => window.print()}>
            Export Report
          </button>
          <button className="ab-btn ab-btn-primary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>

      <style>{`
        .ab-report-overlay {
          position: fixed;
          inset: 0;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }

        .ab-report-modal {
          background: var(--ab-white);
          border-radius: var(--ab-radius-lg);
          width: 800px;
          max-width: 90%;
          max-height: 90vh;
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }

        .ab-modal-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: var(--ab-spacing-lg);
          border-bottom: 1px solid var(--ab-gray-200);
        }

        .ab-header-content {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
        }

        .ab-header-content h2 {
          margin: 0;
          font-size: var(--ab-font-size-lg);
          font-weight: 600;
        }

        .ab-status-badge {
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          border-radius: var(--ab-radius-full);
          font-size: var(--ab-font-size-xs);
          font-weight: 500;
        }

        .ab-status-active { background: ${chartColors.successLight}; color: ${chartColors.success}; }
        .ab-status-planning { background: ${chartColors.primaryBg}; color: ${chartColors.primary}; }
        .ab-status-completed { background: ${chartColors.neutral100}; color: ${chartColors.neutral700}; }

        .ab-close-btn {
          background: none;
          border: none;
          cursor: pointer;
          color: var(--ab-gray-500);
        }

        .ab-report-tabs {
          display: flex;
          border-bottom: 1px solid var(--ab-gray-200);
        }

        .ab-report-tab {
          flex: 1;
          padding: var(--ab-spacing-md);
          background: none;
          border: none;
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-500);
          cursor: pointer;
          border-bottom: 2px solid transparent;
          transition: all var(--ab-transition-fast);
        }

        .ab-report-tab:hover {
          color: var(--ab-gray-700);
          background: var(--ab-gray-50);
        }

        .ab-report-tab.active {
          color: var(--ab-primary-600);
          border-bottom-color: var(--ab-primary-500);
        }

        .ab-modal-body {
          flex: 1;
          overflow-y: auto;
          padding: var(--ab-spacing-lg);
        }

        .ab-date-range {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
          margin-bottom: var(--ab-spacing-lg);
          padding: var(--ab-spacing-md);
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-md);
        }

        .ab-date-separator {
          color: var(--ab-gray-400);
        }

        .ab-days-remaining {
          margin-left: auto;
          font-weight: 600;
          color: var(--ab-primary-600);
        }

        .ab-metrics-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: var(--ab-spacing-md);
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-metric-card {
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-md);
          text-align: center;
        }

        .ab-metric-success { border-left: 3px solid ${chartColors.success}; }
        .ab-metric-primary { border-left: 3px solid ${chartColors.primary}; }

        .ab-metric-value {
          font-size: var(--ab-font-size-2xl);
          font-weight: 700;
          color: var(--ab-gray-800);
        }

        .ab-metric-label {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
          margin-top: var(--ab-spacing-xs);
        }

        .ab-progress-section {
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-progress-header {
          display: flex;
          justify-content: space-between;
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-600);
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-progress-bar {
          height: 8px;
          background: var(--ab-gray-200);
          border-radius: var(--ab-radius-full);
          overflow: hidden;
        }

        .ab-progress-fill {
          height: 100%;
          transition: width 0.3s ease;
        }

        .ab-points-summary {
          display: flex;
          justify-content: space-around;
          padding: var(--ab-spacing-md);
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-md);
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-point-item {
          display: flex;
          flex-direction: column;
          align-items: center;
        }

        .ab-point-label {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
        }

        .ab-point-value {
          font-size: var(--ab-font-size-lg);
          font-weight: 600;
          color: var(--ab-gray-700);
        }

        .ab-point-completed { color: ${chartColors.success}; }
        .ab-point-remaining { color: ${chartColors.warning}; }

        .ab-burn-rate {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
          padding: var(--ab-spacing-md);
          background: linear-gradient(135deg, ${chartColors.neutral50}, ${chartColors.warning});
          border-radius: var(--ab-radius-md);
        }

        .ab-burn-icon { font-size: 24px; }

        .ab-burn-value {
          font-size: var(--ab-font-size-lg);
          font-weight: 600;
          color: ${chartColors.warningDark};
        }

        .ab-burn-label {
          font-size: var(--ab-font-size-xs);
          color: ${chartColors.warningDark};
        }

        .ab-burndown-stats {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--ab-spacing-md);
          margin-top: var(--ab-spacing-lg);
        }

        .ab-stat {
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-md);
          text-align: center;
        }

        .ab-stat-label {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
        }

        .ab-stat-value {
          font-size: var(--ab-font-size-xl);
          font-weight: 700;
          color: var(--ab-gray-800);
          display: block;
          margin-top: var(--ab-spacing-xs);
        }

        .ab-velocity-summary {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--ab-spacing-md);
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-velocity-stat {
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-md);
          text-align: center;
        }

        .ab-velocity-value {
          font-size: var(--ab-font-size-2xl);
          font-weight: 700;
          color: var(--ab-primary-600);
          display: block;
        }

        .ab-velocity-label {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
        }

        .ab-dist-section {
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-dist-section h3 {
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
          color: var(--ab-gray-700);
          margin: 0 0 var(--ab-spacing-md);
        }

        .ab-dist-bars { display: flex; flex-direction: column; gap: var(--ab-spacing-sm); }

        .ab-dist-item {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
        }

        .ab-dist-label {
          width: 80px;
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-600);
        }

        .ab-dist-bar-container {
          flex: 1;
          height: 8px;
          background: var(--ab-gray-200);
          border-radius: var(--ab-radius-full);
          overflow: hidden;
        }

        .ab-dist-bar {
          height: 100%;
          background: var(--ab-primary-500);
          transition: width 0.3s ease;
        }

        .ab-dist-count {
          width: 30px;
          text-align: right;
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-600);
        }

        .ab-dist-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: var(--ab-spacing-sm);
        }

        .ab-dist-card {
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: var(--ab-spacing-md);
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
        }

        .ab-dist-type-icon { font-size: 24px; }
        .ab-dist-type-name { font-size: var(--ab-font-size-sm); font-weight: 500; }
        .ab-dist-type-count { font-size: var(--ab-font-size-lg); font-weight: 600; color: var(--ab-primary-600); }

        .ab-priority-list { display: flex; flex-direction: column; gap: var(--ab-spacing-xs); }

        .ab-priority-item {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-xs);
        }

        .ab-priority-icon { font-size: 16px; }
        .ab-priority-name { flex: 1; font-size: var(--ab-font-size-sm); }
        .ab-priority-count { font-weight: 600; }

        .ab-loading-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: var(--ab-spacing-xl);
        }

        .ab-spinner {
          width: 40px;
          height: 40px;
          border: 3px solid var(--ab-gray-200);
          border-top-color: var(--ab-primary-500);
          border-radius: 50%;
          animation: spin 1s linear infinite;
        }

        @keyframes spin {
          to { transform: rotate(360deg); }
        }

        .ab-modal-footer {
          display: flex;
          justify-content: flex-end;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-lg);
          border-top: 1px solid var(--ab-gray-200);
        }
      `}</style>
    </div>
  );
}