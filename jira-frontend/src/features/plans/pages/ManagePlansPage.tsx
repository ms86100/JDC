import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { usePlans, usePrograms } from '../hooks/usePlans';
import type { PlanResponse, ProgramResponse } from '../../../api/planApi';
import CreatePlanProgramSelector from '../components/CreatePlanProgramSelector';
import '../styles/manage-plans.css';

function displayName(name: string | undefined, fallback: string) {
  const t = (name ?? '').trim();
  return t.length > 0 ? t : fallback;
}

function PlanIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="3" y="4" width="18" height="16" rx="2" stroke="currentColor" strokeWidth="1.75" />
      <path d="M3 9h18M8 4v16" stroke="currentColor" strokeWidth="1.75" />
    </svg>
  );
}

function ProgramIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M4 6h6v6H4V6zm10 0h6v6h-6V6zM4 14h6v6H4v-6zm10 0h6v6h-6v-6z"
        stroke="currentColor"
        strokeWidth="1.75"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function PlanTile({ plan }: { plan: PlanResponse }) {
  return (
    <Link
      to={`/plans/${plan.id}`}
      className="sa-plan-tile sa-plan-tile--plan"
      aria-label={`Open plan ${displayName(plan.name, 'Untitled plan')}`}
    >
      <div className="sa-plan-tile-icon">
        <PlanIcon />
      </div>
      <div className="sa-plan-tile-body">
        <h3 className="sa-plan-tile-title">{displayName(plan.name, 'Untitled plan')}</h3>
        {plan.description?.trim() ? (
          <p className="sa-plan-tile-desc">{plan.description}</p>
        ) : (
          <p className="sa-plan-tile-desc">Roadmap and issue planning workspace</p>
        )}
        <div className="sa-plan-tile-metrics">
          <span className="sa-plan-tile-metric sa-plan-tile-metric--accent">
            {plan.itemCount ?? 0} issues
          </span>
          <span className="sa-plan-tile-metric">{plan.teamCount ?? 0} teams</span>
          {(plan.releaseCount ?? 0) > 0 && (
            <span className="sa-plan-tile-metric">{plan.releaseCount} releases</span>
          )}
        </div>
      </div>
      <span className="sa-plan-tile-arrow" aria-hidden="true">
        →
      </span>
    </Link>
  );
}

function ProgramTile({ program }: { program: ProgramResponse }) {
  const access = program.accessType === 'RESTRICTED' ? 'restricted' : 'open';
  return (
    <Link
      to={`/programs/${program.id}`}
      className="sa-plan-tile sa-plan-tile--program"
      aria-label={`Open program ${program.name}`}
    >
      <div className="sa-plan-tile-icon">
        <ProgramIcon />
      </div>
      <div className="sa-plan-tile-body">
        <h3 className="sa-plan-tile-title">{program.name}</h3>
        <p className="sa-plan-tile-desc">
          {program.description?.trim() || 'Cross-plan program for advanced roadmaps'}
        </p>
        <div className="sa-plan-tile-metrics">
          <span className={`sa-plan-tile-badge sa-plan-tile-badge--${access}`}>{program.accessType}</span>
          <span className="sa-plan-tile-metric sa-plan-tile-metric--accent">
            {program.planCount ?? 0} {program.planCount === 1 ? 'plan' : 'plans'}
          </span>
        </div>
      </div>
      <span className="sa-plan-tile-arrow" aria-hidden="true">
        →
      </span>
    </Link>
  );
}

function SkeletonTiles({ count = 3 }: { count?: number }) {
  return (
    <>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="sa-plan-tile sa-plan-tile--skeleton" aria-hidden="true">
          <div className="sa-plan-tile-icon" />
          <div className="sa-plan-tile-body">
            <div className="sa-plan-tile-title" />
            <div className="sa-plan-tile-desc" />
          </div>
        </div>
      ))}
    </>
  );
}

export default function ManagePlansPage() {
  const navigate = useNavigate();
  const [showCreate, setShowCreate] = useState(false);
  const { data: plans = [], isLoading: plansLoading } = usePlans();
  const { data: programs = [], isLoading: programsLoading } = usePrograms();

  const isLoading = plansLoading || programsLoading;
  const planCount = plans.length;
  const programCount = programs.length;
  const hasContent = planCount > 0 || programCount > 0;

  return (
    <div className="sa-manage-plans">
      <header className="sa-manage-plans-hero">
        <div>
          <p className="sa-manage-plans-hero-eyebrow">Advanced Roadmaps</p>
          <h1>Plans &amp; programs</h1>
          <p className="sa-manage-plans-hero-sub">
            Coordinate delivery across teams with plans for roadmaps and programs that group related work.
          </p>
        </div>
        <div className="sa-manage-plans-hero-actions">
          <button
            type="button"
            className="sa-manage-plans-btn sa-manage-plans-btn--primary"
            onClick={() => setShowCreate(true)}
          >
            <span aria-hidden="true">+</span>
            Create plan or program
          </button>
          <Link to="/programs" className="sa-manage-plans-btn sa-manage-plans-btn--secondary">
            Browse programs
          </Link>
        </div>
      </header>

      {isLoading ? (
        <div className="sa-manage-plans-loading" role="status" aria-live="polite">
          <div className="sa-manage-plans-spinner" />
          <span>Loading your plans…</span>
        </div>
      ) : !hasContent ? (
        <div className="sa-manage-plans-empty">
          <div className="sa-manage-plans-empty-icon" aria-hidden="true">
            <PlanIcon />
          </div>
          <h2>Start with your first plan</h2>
          <p>
            Plans organize issues, teams, and timelines. Programs connect multiple plans for portfolio-level
            roadmaps.
          </p>
          <button
            type="button"
            className="sa-manage-plans-btn sa-manage-plans-btn--primary"
            onClick={() => setShowCreate(true)}
          >
            Create plan or program
          </button>
        </div>
      ) : (
        <>
          <div className="sa-manage-plans-summary">
            <div className="sa-manage-plans-stat">
              <div className="sa-manage-plans-stat-value">{planCount}</div>
              <div className="sa-manage-plans-stat-label">{planCount === 1 ? 'Plan' : 'Plans'}</div>
            </div>
            <div className="sa-manage-plans-stat">
              <div className="sa-manage-plans-stat-value">{programCount}</div>
              <div className="sa-manage-plans-stat-label">
                {programCount === 1 ? 'Program' : 'Programs'}
              </div>
            </div>
          </div>

          <div className="sa-manage-plans-body">
            <section aria-labelledby="plans-section-title">
              <div className="sa-manage-plans-section-head">
                <div>
                  <h2 id="plans-section-title" className="sa-manage-plans-section-title">
                    Plans
                    <span className="sa-manage-plans-section-count">{planCount}</span>
                  </h2>
                  <p className="sa-manage-plans-section-desc">
                    Team roadmaps with issues, releases, and dependencies
                  </p>
                </div>
                {planCount > 0 && (
                  <button
                    type="button"
                    className="sa-manage-plans-btn sa-manage-plans-btn--secondary"
                    onClick={() => navigate('/plans/create')}
                  >
                    New plan
                  </button>
                )}
              </div>
              {plansLoading ? (
                <div className="sa-manage-plans-grid">
                  <SkeletonTiles />
                </div>
              ) : planCount === 0 ? (
                <p className="sa-manage-plans-section-desc">No plans yet — create one to get started.</p>
              ) : (
                <div className="sa-manage-plans-grid">
                  {plans.map((plan) => (
                    <PlanTile key={plan.id} plan={plan} />
                  ))}
                </div>
              )}
            </section>

            <section aria-labelledby="programs-section-title">
              <div className="sa-manage-plans-section-head">
                <div>
                  <h2 id="programs-section-title" className="sa-manage-plans-section-title">
                    Programs
                    <span className="sa-manage-plans-section-count">{programCount}</span>
                  </h2>
                  <p className="sa-manage-plans-section-desc">
                    Portfolio view across multiple plans and initiatives
                  </p>
                </div>
                {programCount > 0 && (
                  <button
                    type="button"
                    className="sa-manage-plans-btn sa-manage-plans-btn--secondary"
                    onClick={() => navigate('/programs/create')}
                  >
                    New program
                  </button>
                )}
              </div>
              {programsLoading ? (
                <div className="sa-manage-plans-grid">
                  <SkeletonTiles />
                </div>
              ) : programCount === 0 ? (
                <p className="sa-manage-plans-section-desc">
                  No programs yet —{' '}
                  <Link to="/programs/create">create a program</Link> to group plans.
                </p>
              ) : (
                <div className="sa-manage-plans-grid">
                  {programs.map((program) => (
                    <ProgramTile key={program.id} program={program} />
                  ))}
                </div>
              )}
            </section>
          </div>
        </>
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
