import { Link } from 'react-router-dom';
import { SectionPanel } from './WorkspaceComponents';
import { useProgramDeliveryMetrics } from './useProgramDeliveryMetrics';
import type { PlanResponse } from '../../api/planApi';
import BurndownChart from '../../features/sprints/components/BurndownChart';
import VelocityChart from '../../features/sprints/components/VelocityChart';

interface ProgramPortfolioDeliveryProps {
  plans: PlanResponse[];
  title?: string;
  compact?: boolean;
}

export function ProgramPortfolioDelivery({
  plans,
  title = 'Portfolio delivery',
  compact = false,
}: ProgramPortfolioDeliveryProps) {
  const { data, isLoading } = useProgramDeliveryMetrics(plans);

  if (isLoading) {
    return (
      <SectionPanel title={title} subtitle="Aggregated sprint burndown and velocity">
        <p className="ws-muted">Loading delivery metrics…</p>
      </SectionPanel>
    );
  }

  if (!data || (!data.activeSprints.length && !data.velocitySeries.length)) {
    return (
      <SectionPanel title={title} subtitle="Sprint data from linked plan boards">
        <p className="ws-muted">
          No active sprints or velocity history on linked plans yet. Add plans with boards and start sprints to see portfolio burndown and velocity.
        </p>
      </SectionPanel>
    );
  }

  const primary = data.activeSprints[0];

  if (compact) {
    return (
      <div className="ws-dashboard-row ws-dashboard-row--2">
        <SectionPanel title="Active sprint burndown" subtitle={primary ? `${primary.planName} · ${primary.sprint.name}` : 'No active sprint'}>
          {data.burndownChartData.length > 0 ? (
            <BurndownChart data={data.burndownChartData} totalPoints={data.burndownTotalPoints} />
          ) : (
            <p className="ws-muted">Burndown populates once the sprint has daily snapshots.</p>
          )}
        </SectionPanel>
        <SectionPanel title="Portfolio velocity" subtitle={`${data.totalClosedSprints} closed sprints`}>
          {data.velocitySeries.length > 0 ? (
            <VelocityChart data={data.velocitySeries} averageVelocity={data.averageVelocity} />
          ) : (
            <p className="ws-muted">Complete sprints on plan boards to build velocity trends.</p>
          )}
        </SectionPanel>
      </div>
    );
  }

  return (
    <div className="ws-program-delivery">
      <div className="ws-entity-card-metrics" style={{ marginBottom: 16 }}>
        <div className="ws-mini-metric">
          <span>Active sprints</span>
          <strong>{data.activeSprints.length}</strong>
        </div>
        <div className="ws-mini-metric">
          <span>Avg velocity</span>
          <strong>{data.averageVelocity.toFixed(1)}</strong>
        </div>
        <div className="ws-mini-metric">
          <span>Closed sprints</span>
          <strong>{data.totalClosedSprints}</strong>
        </div>
      </div>

      {data.activeSprints.length > 0 && (
        <ul className="ws-sprint-list" style={{ marginBottom: 16 }}>
          {data.activeSprints.slice(0, 5).map((a) => (
            <li key={a.sprint.id} className="ws-sprint-list-item">
              <span className="ws-sprint-list-name">{a.planName}</span>
              <span className="ws-sprint-status ws-sprint-status--active">{a.sprint.name}</span>
              <Link to={`/plans/${a.planId}`} className="ws-link-action">Plan →</Link>
            </li>
          ))}
        </ul>
      )}

      <div className="ws-dashboard-row ws-dashboard-row--2">
        <SectionPanel
          title="Program burndown"
          subtitle={primary ? `${primary.planName} — ${primary.sprint.name}` : 'Primary active sprint'}
        >
          {data.burndownChartData.length > 0 ? (
            <BurndownChart data={data.burndownChartData} totalPoints={data.burndownTotalPoints} />
          ) : primary ? (
            <div className="ws-entity-card-metrics">
              <div className="ws-mini-metric"><span>Total issues</span><strong>{primary.sprint.totalIssues}</strong></div>
              <div className="ws-mini-metric"><span>Completed</span><strong>{primary.sprint.completedIssues}</strong></div>
              <div className="ws-mini-metric"><span>Points</span><strong>{primary.sprint.completedPoints}/{primary.sprint.committedPoints}</strong></div>
            </div>
          ) : (
            <p className="ws-muted">No burndown data available.</p>
          )}
        </SectionPanel>

        <SectionPanel title="Program velocity" subtitle="Completed sprints across plans">
          {data.velocitySeries.length > 0 ? (
            <>
              <div className="ws-entity-card-metrics" style={{ marginBottom: 12 }}>
                <div className="ws-mini-metric">
                  <span>Average</span>
                  <strong>{data.averageVelocity.toFixed(1)} pts</strong>
                </div>
                <div className="ws-mini-metric">
                  <span>Sprints</span>
                  <strong>{data.velocitySeries.length}</strong>
                </div>
              </div>
              <VelocityChart data={data.velocitySeries} averageVelocity={data.averageVelocity} />
            </>
          ) : (
            <p className="ws-muted">Velocity builds as plan-board sprints are completed.</p>
          )}
        </SectionPanel>
      </div>
    </div>
  );
}
