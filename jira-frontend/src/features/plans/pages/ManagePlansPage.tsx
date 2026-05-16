import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePlans, usePrograms } from '../hooks/usePlans';
import CreatePlanProgramSelector from '../components/CreatePlanProgramSelector';
import '../styles/plans.css';

export default function ManagePlansPage() {
  const navigate = useNavigate();
  const [showSelector, setShowSelector] = useState(false);
  const { data: plans, isLoading: plansLoading } = usePlans();
  const { data: programs, isLoading: programsLoading } = usePrograms();

  const isLoading = plansLoading || programsLoading;
  const hasContent = (plans && plans.length > 0) || (programs && programs.length > 0);

  const handleCreateClick = () => {
    setShowSelector(true);
  };

  const handleTypeSelect = (type: 'plan' | 'program') => {
    setShowSelector(false);
    if (type === 'plan') {
      navigate('/plans/create');
    } else {
      navigate('/programs/create');
    }
  };

  return (
    <div className="manage-plans-page">
      <div className="page-header">
        <div className="page-header-left">
          <h1 className="page-title">Manage plans</h1>
        </div>
        <div className="page-header-right">
          <button className="create-button" onClick={handleCreateClick}>
            <span className="create-button-icon">+</span>
            Create plan or program
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="loading-state">
          <div className="loading-spinner"></div>
        </div>
      ) : !hasContent ? (
        <EmptyState onCreateClick={handleCreateClick} />
      ) : (
        <div className="plans-content">
          {/* Plans Section */}
          {plans && plans.length > 0 && (
            <section className="content-section">
              <h2 className="section-title">Plans</h2>
              <div className="plans-grid">
                {plans.map((plan) => (
                  <div key={plan.id} className="plan-card" onClick={() => navigate(`/plans/${plan.id}`)}>
                    <div className="plan-card-icon">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" strokeWidth="2"/>
                        <line x1="3" y1="9" x2="21" y2="9" stroke="currentColor" strokeWidth="2"/>
                        <line x1="9" y1="21" x2="9" y2="9" stroke="currentColor" strokeWidth="2"/>
                      </svg>
                    </div>
                    <div className="plan-card-content">
                      <h3 className="plan-card-title">{plan.name}</h3>
                      <div className="plan-card-meta">
                        <span className="plan-card-count">{plan.itemCount} issues</span>
                        <span className="plan-card-teams">{plan.teamCount} teams</span>
                      </div>
                    </div>
                    <div className="plan-card-arrow">
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                        <path d="M6 4L10 8L6 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                      </svg>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Programs Section */}
          {programs && programs.length > 0 && (
            <section className="content-section">
              <h2 className="section-title">Programs</h2>
              <div className="programs-grid">
                {programs.map((program) => (
                  <div key={program.id} className="program-card" onClick={() => navigate(`/programs/${program.id}`)}>
                    <div className="program-card-icon">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <rect x="2" y="6" width="20" height="12" rx="2" stroke="currentColor" strokeWidth="2"/>
                        <path d="M8 6V4M16 6V4" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                      </svg>
                    </div>
                    <div className="program-card-content">
                      <h3 className="program-card-title">{program.name}</h3>
                      <div className="program-card-meta">
                        <span className="program-card-count">{program.planCount} plans</span>
                        <span className={`access-badge ${program.accessType.toLowerCase()}`}>
                          {program.accessType === 'OPEN' ? 'No restrictions' : 'Restricted'}
                        </span>
                      </div>
                    </div>
                    <div className="program-card-arrow">
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                        <path d="M6 4L10 8L6 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
                      </svg>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      )}

      {showSelector && (
        <CreatePlanProgramSelector
          onSelect={handleTypeSelect}
          onClose={() => setShowSelector(false)}
        />
      )}
    </div>
  );
}

function EmptyState({ onCreateClick }: { onCreateClick: () => void }) {
  return (
    <div className="empty-state-container">
      <div className="empty-state-illustration">
        <svg width="280" height="200" viewBox="0 0 280 200" fill="none">
          {/* Dashboard panels */}
          <rect x="30" y="30" width="100" height="70" rx="8" fill="#E8F0FE" stroke="#0066FF" strokeWidth="2"/>
          <rect x="40" y="45" width="60" height="8" rx="2" fill="#0066FF"/>
          <rect x="40" y="58" width="80" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="40" y="66" width="70" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="40" y="74" width="75" height="4" rx="1" fill="#BDC1C6"/>
          <circle cx="115" cy="85" r="8" fill="#FF9200"/>

          <rect x="150" y="20" width="110" height="80" rx="8" fill="#F0F4FF" stroke="#0052CC" strokeWidth="2"/>
          <rect x="160" y="35" width="70" height="8" rx="2" fill="#0052CC"/>
          <rect x="160" y="50" width="90" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="160" y="58" width="85" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="160" y="66" width="88" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="160" y="74" width="80" height="4" rx="1" fill="#BDC1C6"/>

          <rect x="20" y="115" width="130" height="65" rx="8" fill="#F8F9FA" stroke="#BDC1C6" strokeWidth="2"/>
          <rect x="30" y="130" width="50" height="8" rx="2" fill="#495057"/>
          <rect x="30" y="145" width="110" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="30" y="153" width="100" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="30" y="161" width="105" height="4" rx="1" fill="#BDC1C6"/>

          <rect x="170" y="115" width="90" height="65" rx="8" fill="#E8F0FE" stroke="#0066FF" strokeWidth="2"/>
          <rect x="180" y="130" width="60" height="8" rx="2" fill="#0066FF"/>
          <rect x="180" y="145" width="70" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="180" y="153" width="65" height="4" rx="1" fill="#BDC1C6"/>
          <rect x="180" y="161" width="72" height="4" rx="1" fill="#BDC1C6"/>

          {/* Connection lines */}
          <line x1="130" y1="65" x2="150" y2="60" stroke="#BDC1C6" strokeWidth="2" strokeDasharray="4 2"/>
          <line x1="90" y1="100" x2="85" y2="115" stroke="#BDC1C6" strokeWidth="2" strokeDasharray="4 2"/>
          <line x1="200" y1="100" x2="215" y2="115" stroke="#BDC1C6" strokeWidth="2" strokeDasharray="4 2"/>

          {/* Background circle */}
          <circle cx="220" cy="170" r="40" fill="#F0F4FF" opacity="0.6"/>
        </svg>
      </div>

      <h2 className="empty-state-title">A Portfolio plan is an always up-to-date roadmap</h2>

      <p className="empty-state-description">
        Create a plan and connect dynamically to existing boards, projects and filters
        to create a live forecast of your work.
      </p>

      <div className="empty-state-actions">
        <button className="btn-secondary" onClick={() => window.open('https://docs.example.com/portfolio', '_blank')}>
          Learn more about Portfolio
        </button>
        <button className="btn-primary" onClick={onCreateClick}>
          Create a plan
        </button>
      </div>

      <div className="info-banner">
        <div className="info-banner-icon">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <circle cx="10" cy="10" r="9" stroke="#0066FF" strokeWidth="2"/>
            <line x1="10" y1="9" x2="10" y2="14" stroke="#0066FF" strokeWidth="2" strokeLinecap="round"/>
            <circle cx="10" cy="6.5" r="1" fill="#0066FF"/>
          </svg>
        </div>
        <div className="info-banner-content">
          <h4 className="info-banner-title">Can't find the plan you're looking for?</h4>
          <p className="info-banner-text">This could be for various reasons, including:</p>
          <ul className="info-banner-list">
            <li>You may not have the necessary permissions to view the plans in your roadmap.</li>
            <li>The plan you're looking for may be in another program.</li>
          </ul>
          <a href="https://docs.example.com/portfolio" target="_blank" rel="noopener noreferrer" className="info-banner-link">
            See the documentation for more details.
          </a>
        </div>
      </div>
    </div>
  );
}
