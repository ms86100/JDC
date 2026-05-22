import { Link } from 'react-router-dom';
import type { PlanResponse } from '../../../../api/planApi';

interface PlanActionBarProps {
  plan: PlanResponse;
  pendingCount: number;
  viewLabel: string;
  onReviewChanges: () => void;
  onDiscardChanges?: () => void;
  onAutoSchedule: () => void;
  onShare?: () => void;
  warningCount?: number;
}

export default function PlanActionBar({
  plan,
  pendingCount,
  viewLabel,
  onReviewChanges,
  onDiscardChanges,
  onAutoSchedule,
  onShare,
  warningCount = 0,
}: PlanActionBarProps) {
  return (
    <div className="sa-plan-action-bar">
      <div className="sa-plan-action-bar__cluster">
        <span className="sa-plan-action-bar__soon" title="Feedback collection will be available in a future release">
          Give feedback
        </span>
        <button type="button" className="jdc-btn" style={{ fontSize: 12 }} disabled title="No warnings for this plan">
          {warningCount} warnings ▾
        </button>
        <button type="button" className="jdc-btn" onClick={onAutoSchedule}>
          Auto-schedule
        </button>
        <button type="button" className="jdc-btn" onClick={onShare}>
          Share
        </button>
        <button
          type="button"
          className="jdc-btn sa-plan-action-bar__disabled-action"
          disabled
          title="Plan export will be available in a future release"
        >
          Export ▾
        </button>
        {pendingCount > 0 && onDiscardChanges && (
          <button type="button" className="jdc-btn" onClick={onDiscardChanges}>
            Discard
          </button>
        )}
        <button
          type="button"
          className="jdc-btn jdc-review-btn"
          onClick={onReviewChanges}
          disabled={pendingCount === 0}
          title={pendingCount === 0 ? 'No pending changes' : 'Commit staged changes to Jira'}
        >
          Review changes
          {pendingCount > 0 && <span className="jdc-review-badge">{pendingCount}</span>}
        </button>
      </div>
      <div className="sa-plan-action-bar__meta">
        <span>
          View: <strong>{viewLabel}</strong>
          {pendingCount > 0 && (
            <span style={{ marginLeft: 6, color: '#ff991f', fontWeight: 700 }}>EDITED</span>
          )}
        </span>
        <Link
          to={`/plans/${plan.id}/settings`}
          className="jdc-btn"
          title="Plan settings"
          style={{ textDecoration: 'none' }}
        >
          ⚙
        </Link>
      </div>
    </div>
  );
}
