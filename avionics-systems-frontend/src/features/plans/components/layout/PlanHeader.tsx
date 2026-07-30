import { Link } from 'react-router-dom';
import { PlanResponse } from '../../../../api/planApi';

interface PlanHeaderProps {
  plan: PlanResponse;
}

export default function PlanHeader({ plan }: PlanHeaderProps) {
  return (
    <div className="ab-plan-header">
      <div className="ab-breadcrumb">
        <Link to="/programs">Programs</Link>
        <span className="ab-breadcrumb-separator">/</span>
        <span>{plan.name}</span>
      </div>
      <div className="ab-plan-header-content">
        <div className="ab-plan-title-section">
          <h1 className="ab-page-title">{plan.name}</h1>
          <p className="ab-page-subtitle">{plan.description || 'No description'}</p>
        </div>
        <div className="ab-plan-meta">
          {plan.startDate && (
            <div className="ab-plan-meta-item">
              <span className="ab-plan-meta-label">Start Date</span>
              <span className="ab-plan-meta-value">{new Date(plan.startDate).toLocaleDateString()}</span>
            </div>
          )}
          {plan.endDate && (
            <div className="ab-plan-meta-item">
              <span className="ab-plan-meta-label">End Date</span>
              <span className="ab-plan-meta-value">{new Date(plan.endDate).toLocaleDateString()}</span>
            </div>
          )}
          <div className="ab-plan-meta-item">
            <span className="ab-plan-meta-label">Items</span>
            <span className="ab-plan-meta-value">{plan.itemCount}</span>
          </div>
          <div className="ab-plan-meta-item">
            <span className="ab-plan-meta-label">Teams</span>
            <span className="ab-plan-meta-value">{plan.teamCount}</span>
          </div>
          <div className="ab-plan-meta-item">
            <span className="ab-plan-meta-label">Releases</span>
            <span className="ab-plan-meta-value">{plan.releaseCount}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
