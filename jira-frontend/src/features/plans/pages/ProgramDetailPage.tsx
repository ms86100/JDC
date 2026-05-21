import { useEffect, useState } from 'react';
import { Link, useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useProgram, useUpdateProgram, useDeleteProgram, useCreatePlan } from '../hooks/usePlans';
import { planApi, CreatePlanRequest } from '../../../api/planApi';
import {
  WorkspaceHeader,
  QuickNavTabs,
  ContextActionBar,
  KpiCard,
  PortfolioSummary,
  ProgressBar,
  HealthBadge,
  SectionPanel,
  EntityAvatar,
} from '../../../components/workspace/WorkspaceComponents';
import { recordRecentView } from '../../../components/workspace/recentViews';
import { aggregatePlanMetrics, formatShortDate } from '../../../components/workspace/metrics';
import type { HealthLevel } from '../../../components/workspace/metrics';
import { ProgramPortfolioDelivery } from '../../../components/workspace/ProgramPortfolioDelivery';
import '../../../components/workspace/workspace-dashboard.css';

type ProgramTab = 'overview' | 'plans' | 'initiatives';

export default function ProgramDetailPage() {
  const { programId } = useParams<{ programId: string }>();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const tabParam = searchParams.get('tab') as ProgramTab | null;
  const tab: ProgramTab = tabParam && ['overview', 'plans', 'initiatives'].includes(tabParam) ? tabParam : 'overview';
  const setTab = (next: ProgramTab) => setSearchParams({ tab: next }, { replace: true });
  const [showCreatePlan, setShowCreatePlan] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [form, setForm] = useState<{ name: string; description: string }>({ name: '', description: '' });

  const { data: program, isLoading: programLoading } = useProgram(programId || '');
  const { data: plans = [], isLoading: plansLoading } = useQuery({
    queryKey: ['program-plans', programId],
    queryFn: async () => {
      const res = await planApi.getPlansByProgram(programId!);
      return res.data ?? [];
    },
    enabled: !!programId,
  });

  const createPlanMutation = useCreatePlan();
  const updateMutation = useUpdateProgram();
  const deleteProgramMutation = useDeleteProgram();

  const planMetrics = aggregatePlanMetrics(plans);
  const health: HealthLevel = program?.isActive
    ? planMetrics.activePlans >= planMetrics.totalItems / 10
      ? 'healthy'
      : program.planCount > 0
        ? 'at-risk'
        : 'unknown'
    : 'critical';

  const coveragePct =
    plans.length > 0
      ? Math.round((planMetrics.activePlans / plans.length) * 100)
      : 0;

  useEffect(() => {
    if (program && programId) {
      recordRecentView({
        id: programId,
        type: 'program',
        name: program.name,
        path: `/programs/${programId}`,
      });
    }
  }, [program, programId]);

  const handleCreatePlan = (e: React.FormEvent) => {
    e.preventDefault();
    if (!programId) return;
    createPlanMutation.mutate(
      { ...form, ownerId: program?.ownerId } as CreatePlanRequest,
      {
        onSuccess: (data) => {
          planApi.linkPlanToProgram(programId, data.data.id);
          setShowCreatePlan(false);
          setForm({ name: '', description: '' });
          queryClient.invalidateQueries({ queryKey: ['programs', programId] });
          queryClient.invalidateQueries({ queryKey: ['program-plans', programId] });
          navigate(`/plans/${data.data.id}`);
        },
      }
    );
  };

  const handleDelete = () => {
    if (!programId) return;
    if (confirm('Delete this program? Linked plans will remain but lose program association.')) {
      deleteProgramMutation.mutate(programId, {
        onSuccess: () => navigate('/programs'),
      });
    }
  };

  if (programLoading) {
    return <div className="ab-loading"><div className="ab-spinner" /></div>;
  }

  if (!program) {
    return (
      <div className="ws-page ws-page-empty">
        <h3>Program not found</h3>
        <Link to="/programs" className="ab-btn ab-btn-primary">Back to Programs</Link>
      </div>
    );
  }

  return (
    <div className="ws-page">
      <WorkspaceHeader
        breadcrumbs={
          <>
            <Link to="/programs">Programs</Link>
            <span>/</span>
            <span>{program.name}</span>
          </>
        }
        title={program.name}
        subtitle={program.description || 'Portfolio program — cross-plan visibility and initiative tracking'}
        badges={
          <>
            <HealthBadge health={health} />
            <span className={`ws-badge ws-badge--${program.accessType === 'OPEN' ? 'open' : 'restricted'}`}>
              {program.accessType}
            </span>
            <span className={`ws-badge ${program.isActive ? 'ws-badge--open' : 'ws-badge--archived'}`}>
              {program.isActive ? 'Active' : 'Inactive'}
            </span>
          </>
        }
        meta={
          <>
            <div className="ws-header-meta-item">
              <span>Owner</span>
              <strong>{program.ownerName || 'Unassigned'}</strong>
            </div>
            <div className="ws-header-meta-item">
              <span>Plans</span>
              <strong>{program.planCount}</strong>
            </div>
            <div className="ws-header-meta-item">
              <span>Next milestone</span>
              <strong>{formatShortDate(planMetrics.nearestEndDate)}</strong>
            </div>
          </>
        }
        actions={
          <>
            <button
              type="button"
              className="ab-btn ab-btn-secondary"
              onClick={() => {
                setForm({ name: program.name, description: program.description || '' });
                setShowEdit(true);
              }}
            >
              Edit
            </button>
            <button type="button" className="ab-btn ab-btn-primary" onClick={() => setShowCreatePlan(true)}>
              + Add Plan
            </button>
          </>
        }
      />

      <QuickNavTabs
        items={[
          { label: 'Overview', active: tab === 'overview', onClick: () => setTab('overview') },
          { label: `Plans (${plans.length})`, active: tab === 'plans', onClick: () => setTab('plans') },
          { label: 'Initiatives', active: tab === 'initiatives', onClick: () => setTab('initiatives') },
        ]}
      />

      <ContextActionBar>
        <Link to="/plans">All plans</Link>
        <button type="button" onClick={() => setShowCreatePlan(true)}>Create plan</button>
        <Link to="/dashboard">Portfolio dashboard</Link>
      </ContextActionBar>

      <PortfolioSummary>
        <KpiCard label="Linked plans" value={plans.length} accent="brand" />
        <KpiCard label="Plan items" value={planMetrics.totalItems} />
        <KpiCard label="Teams" value={planMetrics.totalTeams} />
        <KpiCard label="Releases" value={planMetrics.totalReleases} />
        <KpiCard label="Active plans" value={planMetrics.activePlans} accent="success" />
        <KpiCard label="Coverage" value={`${coveragePct}%`} hint="Active vs total plans" />
      </PortfolioSummary>

      {tab === 'overview' && (
        <div className="ws-dashboard">
          {plans.length > 0 && (
            <ProgramPortfolioDelivery plans={plans} title="Program delivery" />
          )}

          <div className="ws-dashboard-row ws-dashboard-row--main">
            <div>
              <SectionPanel title="Program health" subtitle="Delivery coverage across linked plans">
                <ProgressBar value={coveragePct} label="Initiative coverage" />
                <div className="ws-entity-card-metrics" style={{ marginTop: 16 }}>
                  <div className="ws-mini-metric">
                    <span>Items tracked</span>
                    <strong>{planMetrics.totalItems}</strong>
                  </div>
                  <div className="ws-mini-metric">
                    <span>Teams allocated</span>
                    <strong>{planMetrics.totalTeams}</strong>
                  </div>
                  <div className="ws-mini-metric">
                    <span>Releases</span>
                    <strong>{planMetrics.totalReleases}</strong>
                  </div>
                </div>
              </SectionPanel>

              <SectionPanel
                title="Linked plans"
                subtitle="Roadmaps and backlogs in this program"
                action={
                  <button type="button" className="ab-btn ab-btn-sm ab-btn-primary" onClick={() => setShowCreatePlan(true)}>
                    + Plan
                  </button>
                }
              >
                {plansLoading ? (
                  <div className="ab-loading"><div className="ab-spinner" /></div>
                ) : plans.length > 0 ? (
                  <ul className="ws-plan-list">
                    {plans.map((plan) => (
                      <li key={plan.id}>
                        <Link to={`/plans/${plan.id}`} className="ws-plan-item">
                          <div>
                            <div className="ws-plan-item-title">{plan.name}</div>
                            <div className="ws-plan-item-meta">
                              {plan.description || 'No description'}
                              {plan.endDate && ` · Due ${formatShortDate(plan.endDate)}`}
                            </div>
                          </div>
                          <div className="ws-plan-item-stats">
                            <span>{plan.itemCount} items</span>
                            <span>{plan.teamCount} teams</span>
                            <span>{plan.isActive ? 'Active' : 'Inactive'}</span>
                          </div>
                        </Link>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="ws-muted">No plans linked yet. Add a plan to start tracking initiatives.</p>
                )}
              </SectionPanel>
            </div>

            <div>
              <SectionPanel title="Resource allocation" subtitle="Teams and releases across plans">
                <div className="ws-entity-card-metrics">
                  <div className="ws-mini-metric">
                    <span>Total teams</span>
                    <strong>{planMetrics.totalTeams}</strong>
                  </div>
                  <div className="ws-mini-metric">
                    <span>Releases planned</span>
                    <strong>{planMetrics.totalReleases}</strong>
                  </div>
                  <div className="ws-mini-metric">
                    <span>Program owner</span>
                    <strong style={{ fontSize: 12 }}>{program.ownerName?.split(' ')[0] ?? '—'}</strong>
                  </div>
                </div>
              </SectionPanel>

              <SectionPanel title="Program details">
                <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                  <EntityAvatar name={program.name} size="lg" />
                  <div>
                    <p className="ws-muted" style={{ margin: '0 0 8px' }}>
                      Created {formatShortDate(program.createdAt)}
                      {program.updatedAt && ` · Updated ${formatShortDate(program.updatedAt)}`}
                    </p>
                    <p style={{ margin: 0, fontSize: 14, color: 'var(--sa-n700)' }}>
                      {program.description || 'Add a description to clarify program goals and scope.'}
                    </p>
                  </div>
                </div>
              </SectionPanel>

              <SectionPanel title="Dependencies & forecasting" subtitle="Cross-plan coordination">
                <p className="ws-muted">
                  Link plans to surface cross-project dependencies, milestone alignment, and delivery forecasting.
                  Open individual plans for dependency graphs and release trains.
                </p>
              </SectionPanel>
            </div>
          </div>
        </div>
      )}

      {tab === 'plans' && (
        <SectionPanel title="All plans in program">
          {plans.length > 0 ? (
            <div className="ws-entity-grid">
              {plans.map((plan) => (
                <Link key={plan.id} to={`/plans/${plan.id}`} className="ws-entity-card">
                  <div className="ws-entity-card-top">
                    <EntityAvatar name={plan.name} />
                    <span className="ws-badge ws-badge--open">{plan.isActive ? 'Active' : 'Inactive'}</span>
                  </div>
                  <div className="ws-entity-card-body">
                    <h3 className="ws-entity-card-title">{plan.name}</h3>
                    <p className="ws-entity-card-desc">{plan.description || 'No description'}</p>
                    <div className="ws-entity-card-metrics">
                      <div className="ws-mini-metric"><span>Items</span><strong>{plan.itemCount}</strong></div>
                      <div className="ws-mini-metric"><span>Teams</span><strong>{plan.teamCount}</strong></div>
                      <div className="ws-mini-metric"><span>Releases</span><strong>{plan.releaseCount}</strong></div>
                    </div>
                  </div>
                  <div className="ws-entity-card-footer">
                    <span>{formatShortDate(plan.startDate)} — {formatShortDate(plan.endDate)}</span>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <p className="ws-muted">No plans in this program.</p>
          )}
        </SectionPanel>
      )}

      {tab === 'initiatives' && (
        <SectionPanel title="Initiative tracking" subtitle="Portfolio milestones and outcomes">
          <p className="ws-muted">
            Initiatives roll up from plan items and epics. Use linked plans to define milestones, then track
            completion and risks at the program level.
          </p>
          {plans.length > 0 && (
            <ul className="ws-plan-list" style={{ marginTop: 16 }}>
              {plans.map((plan) => (
                <li key={plan.id}>
                  <Link to={`/plans/${plan.id}`} className="ws-plan-item">
                    <div>
                      <div className="ws-plan-item-title">{plan.name}</div>
                      <div className="ws-plan-item-meta">{plan.itemCount} work items · {plan.releaseCount} releases</div>
                    </div>
                    <span className="ws-plan-item-stats">{plan.isActive ? 'On track' : 'Paused'}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </SectionPanel>
      )}

      {showCreatePlan && (
        <div className="ab-modal-overlay" onClick={() => setShowCreatePlan(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Create Plan</h2>
              <button type="button" className="ab-btn-icon" onClick={() => setShowCreatePlan(false)} aria-label="Close">
                <span className="ab-icon-close" />
              </button>
            </div>
            <form onSubmit={handleCreatePlan}>
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Plan Name *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    required
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Description</label>
                  <textarea
                    className="ab-textarea"
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    rows={4}
                  />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowCreatePlan(false)}>Cancel</button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={createPlanMutation.isPending}>
                  {createPlanMutation.isPending ? 'Creating...' : 'Create Plan'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showEdit && (
        <div className="ab-modal-overlay" onClick={() => setShowEdit(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Edit Program</h2>
              <button type="button" className="ab-btn-icon" onClick={() => setShowEdit(false)} aria-label="Close">
                <span className="ab-icon-close" />
              </button>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (!programId) return;
                updateMutation.mutate(
                  { id: programId, data: { name: form.name, description: form.description } },
                  {
                    onSuccess: () => {
                      setShowEdit(false);
                      queryClient.invalidateQueries({ queryKey: ['programs', programId] });
                    },
                    onError: (error: Error) => alert(error.message || 'Failed to update'),
                  }
                );
              }}
            >
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Program Name</label>
                  <input type="text" className="ab-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Description</label>
                  <textarea className="ab-textarea" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowEdit(false)}>Cancel</button>
                <button type="submit" className="ab-btn ab-btn-primary">Save</button>
                <button type="button" className="ab-btn ab-btn-danger" onClick={handleDelete}>Delete Program</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
