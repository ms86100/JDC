import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePlans, usePrograms } from '../hooks/usePlans';
import CreatePlanProgramSelector from '../components/CreatePlanProgramSelector';
import '../styles/plans.css';

export default function ManagePlansPage() {
  const navigate = useNavigate();
  const [showCreate, setShowCreate] = useState(false);
  const { data: plans, isLoading: plansLoading } = usePlans();
  const { data: programs, isLoading: programsLoading } = usePrograms();

  const isLoading = plansLoading || programsLoading;
  const hasPlans = plans && plans.length > 0;
  const hasPrograms = programs && programs.length > 0;
  const hasContent = hasPlans || hasPrograms;

  const handleCreateClick = () => {
    setShowCreate(true);
  };

  return (
    <div className="ab-manage-page">
      <div className="ab-page-header">
        <div>
          <h1 className="ab-page-title">Manage Plans</h1>
          <p className="ab-page-subtitle">Create and manage your plans and programs</p>
        </div>
        <button className="ab-btn ab-btn-primary" onClick={handleCreateClick}>
          <span className="ab-icon-plus"></span>
          Create Plan or Program
        </button>
      </div>

      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
        </div>
      ) : !hasContent ? (
        <div className="ab-card">
          <div className="ab-empty-state">
            <div className="ab-empty-state-icon">📊</div>
            <h3 className="ab-empty-state-title">No plans yet</h3>
            <p className="ab-empty-state-description">
              Create your first plan to start organizing your work and roadmaps.
            </p>
            <button className="ab-btn ab-btn-primary" onClick={handleCreateClick}>
              Create Plan or Program
            </button>
          </div>
        </div>
      ) : (
        <div className="ab-manage-content">
          {hasPlans && (
            <section className="ab-manage-section">
              <h2 className="ab-section-title">Plans</h2>
              <div className="ab-grid ab-grid-3">
                {plans.map((plan) => (
                  <div key={plan.id} className="ab-card ab-plan-card" onClick={() => navigate(`/plans/${plan.id}`)}>
                    <div className="ab-plan-card-header">
                      <div className="ab-plan-icon">
                        📋
                      </div>
                      <div className="ab-plan-counts">
                        <span className="ab-badge ab-badge-info">{plan.itemCount} issues</span>
                        <span className="ab-badge ab-badge-secondary">{plan.teamCount} teams</span>
                      </div>
                    </div>
                    <h3 className="ab-plan-name">{plan.name}</h3>
                    <div className="ab-plan-meta">
                      <span className="ab-text-sm ab-text-muted">Click to view details</span>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {hasPrograms && (
            <section className="ab-manage-section">
              <h2 className="ab-section-title">Programs</h2>
              <div className="ab-grid ab-grid-3">
                {programs.map((program) => (
                  <div key={program.id} className="ab-card ab-program-card" onClick={() => navigate(`/programs/${program.id}`)}>
                    <div className="ab-program-card-header">
                      <div className="ab-program-icon">
                        {program.name.charAt(0).toUpperCase()}
                      </div>
                      <span className={`ab-badge ${program.accessType === 'OPEN' ? 'ab-badge-success' : 'ab-badge-warning'}`}>
                        {program.accessType}
                      </span>
                    </div>
                    <h3 className="ab-program-name">{program.name}</h3>
                    <p className="ab-program-description">{program.description || 'No description'}</p>
                    <div className="ab-program-meta">
                      <span className="ab-text-sm ab-text-muted">
                        {program.planCount} {program.planCount === 1 ? 'plan' : 'plans'}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      )}

      {showCreate && (
        <CreatePlanProgramSelector
          onSelect={(type) => {
            setShowCreate(false);
            if (type === 'plan') {
              navigate('/plans/create');
            } else {
              navigate('/programs/create');
            }
          }}
          onClose={() => setShowCreate(false)}
        />
      )}
    </div>
  );
}
