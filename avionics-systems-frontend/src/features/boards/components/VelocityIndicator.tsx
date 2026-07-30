import React, { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import boardApi from '../../../api/boardApi';

interface Props {
  boardId: string;
  sprints?: Array<{
    id: string;
    name: string;
    state: string;
    completeDate?: string;
  }>;
  issues: Array<{
    id: string;
    storyPoints?: number;
    statusCategory?: string;
  }>;
}

interface VelocityData {
  current: number;
  average: number;
  projected: number;
  completedThisSprint: number;
  committed: number;
  trend: 'up' | 'down' | 'stable';
  sprintCompletion: number;
}

export default function VelocityIndicator({ boardId, sprints = [], issues }: Props) {
  const activeSprint = sprints.find((s) => s.state === 'ACTIVE');

  const { data: velocityData } = useQuery({
    queryKey: ['board-velocity', boardId],
    queryFn: () => boardApi.getVelocity(boardId),
    enabled: !!boardId,
    staleTime: 5 * 60 * 1000,
  });

  const computed = useMemo((): VelocityData => {
    const committed = issues.reduce((sum, i) => sum + (i.storyPoints || 0), 0);
    const completed = issues
      .filter((i) => i.statusCategory === 'done')
      .reduce((sum, i) => sum + (i.storyPoints || 0), 0);

    const historicalVelocity = velocityData?.velocity || [];
    const avg = historicalVelocity.length > 0
      ? historicalVelocity.reduce((a, b) => a + b, 0) / historicalVelocity.length
      : committed;

    const sprintCompletion = committed > 0 ? Math.round((completed / committed) * 100) : 0;

    let trend: 'up' | 'down' | 'stable' = 'stable';
    if (historicalVelocity.length >= 2) {
      const recent = historicalVelocity.slice(-3);
      const older = historicalVelocity.slice(-6, -3);
      if (older.length > 0) {
        const recentAvg = recent.reduce((a, b) => a + b, 0) / recent.length;
        const olderAvg = older.reduce((a, b) => a + b, 0) / older.length;
        if (recentAvg > olderAvg * 1.1) trend = 'up';
        else if (recentAvg < olderAvg * 0.9) trend = 'down';
      }
    }

    return {
      current: completed,
      average: Math.round(avg),
      projected: committed,
      completedThisSprint: completed,
      committed,
      trend,
      sprintCompletion,
    };
  }, [issues, velocityData]);

  const trendIcon = computed.trend === 'up' ? '↑' : computed.trend === 'down' ? '↓' : '→';
  const trendColor = computed.trend === 'up' ? '#00875a' : computed.trend === 'down' ? '#de350b' : '#6b778c';

  return (
    <div className="sa-velocity-indicator" aria-label="Team velocity">
      {activeSprint && (
        <div className="sa-velocity-sprint">
          <span className="sa-velocity-sprint-name">{activeSprint.name}</span>
        </div>
      )}

      <div className="sa-velocity-metrics">
        <div className="sa-velocity-metric">
          <span className="sa-velocity-value">{computed.completedThisSprint}</span>
          <span className="sa-velocity-label">Completed</span>
        </div>

        <div className="sa-velocity-metric">
          <span className="sa-velocity-value">{computed.committed}</span>
          <span className="sa-velocity-label">Committed</span>
        </div>

        <div className="sa-velocity-metric">
          <span className="sa-velocity-value">{computed.average}</span>
          <span className="sa-velocity-label">Avg Velocity</span>
        </div>
      </div>

      {activeSprint && (
        <div className="sa-velocity-progress">
          <div className="sa-velocity-progress-bar">
            <div
              className="sa-velocity-progress-fill"
              style={{ width: `${Math.min(computed.sprintCompletion, 100)}%` }}
            />
          </div>
          <span className="sa-velocity-progress-label">
            {computed.sprintCompletion}% complete
          </span>
        </div>
      )}

      <div className="sa-velocity-trend" title="Velocity trend">
        <span style={{ color: trendColor }}>{trendIcon}</span>
        <span className="sa-velocity-trend-label">
          {computed.trend === 'up' ? 'Improving' : computed.trend === 'down' ? 'Declining' : 'Stable'}
        </span>
      </div>
    </div>
  );
}
