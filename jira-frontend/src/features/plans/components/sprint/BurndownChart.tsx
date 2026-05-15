import React from 'react';
import { SprintResponse, SprintBurndownResponse } from '../../hooks/useSprint';

interface BurndownChartProps {
  burndown?: SprintBurndownResponse | null;
  sprint: SprintResponse;
}

export default function BurndownChart({ burndown, sprint }: BurndownChartProps) {
  if (!burndown || burndown.burndownPoints?.length === 0) {
    return (
      <div className="ab-burndown-chart">
        <h3>Sprint Burndown</h3>
        <div className="ab-burndown-empty">
          <p>No burndown data available yet.</p>
          <p className="ab-burndown-hint">
            Burndown data is collected daily during the sprint.
          </p>
        </div>
      </div>
    );
  }

  const maxIssues = Math.max(burndown.totalIssues, 1);
  const today = new Date().toISOString().split('T')[0];

  // Calculate ideal line
  const startDate = sprint.startDate ? new Date(sprint.startDate) : null;
  const endDate = sprint.endDate ? new Date(sprint.endDate) : null;
  const totalDays = startDate && endDate
    ? Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)) + 1
    : 10;

  const getIdealCount = (dayIndex: number) => {
    return Math.round((burndown.totalIssues || 0) * (1 - dayIndex / totalDays));
  };

  return (
    <div className="ab-burndown-chart">
      <div className="ab-burndown-header">
        <h3>Sprint Burndown</h3>
        <div className="ab-burndown-legend">
          <span className="ab-legend-item">
            <span className="ab-legend-color ab-actual"></span>
            Actual
          </span>
          <span className="ab-legend-item">
            <span className="ab-legend-color ab-ideal"></span>
            Ideal
          </span>
        </div>
      </div>

      <div className="ab-burndown-graph">
        {/* Y-axis labels */}
        <div className="ab-burndown-y-axis">
          <span>{maxIssues}</span>
          <span>{Math.round(maxIssues / 2)}</span>
          <span>0</span>
        </div>

        {/* Chart area */}
        <div className="ab-burndown-graph-area">
          {/* Ideal line */}
          <svg className="ab-burndown-ideal-line" viewBox="0 0 100 100" preserveAspectRatio="none">
            <line
              x1="0"
              y1="0"
              x2="100"
              y2="100"
              stroke="var(--ab-color-success)"
              strokeWidth="2"
              strokeDasharray="5,5"
            />
          </svg>

          {/* Bar chart */}
          <div className="ab-burndown-bars">
            {burndown.burndownPoints.map((point, index) => {
              const actualHeight = (point.remainingIssues / maxIssues) * 100;
              const idealHeight = (getIdealCount(index) / maxIssues) * 100;
              const isToday = point.date?.startsWith(today);

              return (
                <div
                  key={index}
                  className={`ab-burndown-bar ${isToday ? 'ab-today' : ''}`}
                >
                  <div
                    className="ab-burndown-actual"
                    style={{ height: `${actualHeight}%` }}
                    title={`${point.remainingIssues} remaining`}
                  />
                  <div
                    className="ab-burndown-ideal-bar"
                    style={{ height: `${idealHeight}%` }}
                  />
                  <span className="ab-burndown-bar-label">
                    {new Date(point.date).getDate()}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* X-axis label */}
        <div className="ab-burndown-x-axis">
          <span>Day 1</span>
          <span>Day {Math.ceil(totalDays / 2)}</span>
          <span>Day {totalDays}</span>
        </div>
      </div>

      {/* Summary stats */}
      <div className="ab-burndown-stats">
        <div className="ab-burndown-stat">
          <span className="ab-stat-value">{burndown.totalIssues}</span>
          <span className="ab-stat-label">Committed</span>
        </div>
        <div className="ab-burndown-stat">
          <span className="ab-stat-value">{burndown.completedIssues}</span>
          <span className="ab-stat-label">Completed</span>
        </div>
        <div className="ab-burndown-stat">
          <span className="ab-stat-value">{burndown.remainingIssues}</span>
          <span className="ab-stat-label">Remaining</span>
        </div>
      </div>
    </div>
  );
}
