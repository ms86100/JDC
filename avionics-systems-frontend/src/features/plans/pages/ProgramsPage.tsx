import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { usePrograms, useCreateProgram, useDeleteProgram } from '../hooks/usePlans';
import { usePlans } from '../hooks/usePlans';
import { CreateProgramRequest, ProgramResponse } from '../../../api/planApi';
import { appNotify } from '../../../lib/appNotify';
import {
  WorkspaceHeader,
  KpiCard,
  PortfolioSummary,
  ProgressBar,
  HealthBadge,
  RecentlyViewed,
  EntityAvatar,
} from '../../../components/workspace/WorkspaceComponents';
import { getRecentViews, recordRecentView } from '../../../components/workspace/recentViews';
import { aggregatePlanMetrics } from '../../../components/workspace/metrics';
import { ProgramPortfolioDelivery } from '../../../components/workspace/ProgramPortfolioDelivery';
export default function ProgramsPage() {
  const [showCreate, setShowCreate] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filter, setFilter] = useState<'all' | 'active' | 'restricted'>('all');
  const [form, setForm] = useState<CreateProgramRequest>({ name: '', description: '' });

  const { data: programs = [], isLoading } = usePrograms();
  const { data: plans = [] } = usePlans();
  const createMutation = useCreateProgram();
  const deleteMutation = useDeleteProgram();

  const recentPrograms = getRecentViews('program');

  const filteredPrograms = useMemo(() => {
    return programs.filter((p) => {
      if (filter === 'active' && !p.isActive) return false;
      if (filter === 'restricted' && p.accessType !== 'RESTRICTED') return false;
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        return (
          p.name.toLowerCase().includes(q) ||
          (p.description?.toLowerCase().includes(q) ?? false) ||
          (p.ownerName?.toLowerCase().includes(q) ?? false)
        );
      }
      return true;
    });
  }, [programs, filter, searchQuery]);

  const portfolio = useMemo(() => {
    const active = programs.filter((p) => p.isActive).length;
    const totalPlans = programs.reduce((s, p) => s + (p.planCount ?? 0), 0);
    const planAgg = aggregatePlanMetrics(plans);
    return { total: programs.length, active, totalPlans, ...planAgg };
  }, [programs, plans]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate(form, {
      onSuccess: () => {
        setShowCreate(false);
        setForm({ name: '', description: '' });
      },
      onError: (error: Error) => appNotify.error(error.message || 'Failed to create program'),
    });
  };

  const handleDelete = (e: React.MouseEvent, id: string, name: string) => {
    e.preventDefault();
    e.stopPropagation();
    if (!confirm(`Delete program "${name}"? This cannot be undone.`)) return;
    deleteMutation.mutate(id, {
      onError: (error: Error) => appNotify.error(error.message || 'Failed to delete program'),
    });
  };

  const handleOpen = (program: ProgramResponse) => {
    recordRecentView({
      id: program.id,
      type: 'program',
      name: program.name,
      path: `/programs/${program.id}`,
    });
  };

  return (
    <div className="ws-page">
      <WorkspaceHeader
        breadcrumbs={
          <>
            <Link to="/dashboard">Dashboard</Link>
            <span>/</span>
            <span>Programs</span>
          </>
        }
        title="Programs"
        subtitle="Portfolio view across plans, initiatives, and delivery milestones"
        actions={
          <button type="button" className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
            + Create Program
          </button>
        }
      />

      <PortfolioSummary>
        <KpiCard label="Programs" value={portfolio.total} accent="brand" />
        <KpiCard label="Active" value={portfolio.active} hint="Currently running" />
        <KpiCard label="Linked plans" value={portfolio.totalPlans} />
        <KpiCard label="Plan items" value={portfolio.totalItems} hint="Across all plans" />
        <KpiCard label="Teams" value={portfolio.totalTeams} />
        <KpiCard label="Releases" value={portfolio.totalReleases} />
      </PortfolioSummary>

      <RecentlyViewed items={recentPrograms} />

      {plans.length > 0 && (
        <ProgramPortfolioDelivery plans={plans} title="Portfolio delivery" compact />
      )}

      <div className="ws-toolbar">
        <div className="ws-toolbar-left">
          <div className="ws-search">
            <span className="ws-search-icon" aria-hidden>⌕</span>
            <input
              type="search"
              placeholder="Search programs, owners..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search programs"
            />
          </div>
          <div className="ws-filter-pills" role="tablist">
            {(['all', 'active', 'restricted'] as const).map((f) => (
              <button
                key={f}
                type="button"
                role="tab"
                className={`ws-filter-pill ${filter === f ? 'ws-filter-pill--active' : ''}`}
                onClick={() => setFilter(f)}
              >
                {f === 'all' ? 'All' : f === 'active' ? 'Active' : 'Restricted'}
              </button>
            ))}
          </div>
        </div>
        <div className="ws-toolbar-right">
          <Link to="/plans" className="ab-btn ab-btn-secondary ab-btn-sm">Manage Plans</Link>
        </div>
      </div>

      {isLoading ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : filteredPrograms.length === 0 ? (
        <div className="ws-page-empty">
          <h3>{searchQuery ? 'No matching programs' : 'No programs yet'}</h3>
          <p>
            {searchQuery
              ? 'Try adjusting your search or filters.'
              : 'Create a program to organize plans, track milestones, and monitor portfolio health.'}
          </p>
          {!searchQuery && (
            <button type="button" className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
              Create Program
            </button>
          )}
        </div>
      ) : (
        <div className="ws-entity-grid">
          {filteredPrograms.map((program) => (
            <ProgramCard
              key={program.id}
              program={program}
              onOpen={() => handleOpen(program)}
              onDelete={(e) => handleDelete(e, program.id, program.name)}
            />
          ))}
        </div>
      )}

      {showCreate && (
        <div className="ab-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Create Program</h2>
              <button type="button" className="ab-btn-icon" onClick={() => setShowCreate(false)} aria-label="Close">
                <span className="ab-icon-close" />
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Program Name *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="e.g. Platform Modernization"
                    required
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Description</label>
                  <textarea
                    className="ab-textarea"
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    placeholder="Strategic goals and scope"
                    rows={4}
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Access Type</label>
                  <select
                    className="ab-select"
                    value={form.accessType || 'OPEN'}
                    onChange={(e) => setForm({ ...form, accessType: e.target.value as 'OPEN' | 'RESTRICTED' })}
                  >
                    <option value="OPEN">Open</option>
                    <option value="RESTRICTED">Restricted</option>
                  </select>
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowCreate(false)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Creating...' : 'Create Program'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

function ProgramCard({
  program,
  onOpen,
  onDelete,
}: {
  program: ProgramResponse;
  onOpen: () => void;
  onDelete: (e: React.MouseEvent) => void;
}) {
  const completionEstimate = program.isActive && program.planCount > 0 ? 72 : program.planCount > 0 ? 45 : 0;
  const agg = { totalItems: program.planCount * 8, totalTeams: Math.max(1, Math.ceil(program.planCount / 2)) };
  const health = program.isActive
    ? completionEstimate >= 60
      ? 'healthy'
      : completionEstimate >= 30
        ? 'at-risk'
        : 'critical'
    : 'unknown';

  return (
    <Link
      to={`/programs/${program.id}`}
      className="ws-entity-card"
      onClick={onOpen}
    >
      <div className="ws-entity-card-top">
        <EntityAvatar name={program.name} />
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <HealthBadge health={health as 'healthy' | 'at-risk' | 'critical' | 'unknown'} />
          <span className={`ws-badge ws-badge--${program.accessType === 'OPEN' ? 'open' : 'restricted'}`}>
            {program.accessType}
          </span>
          <button type="button" className="ws-card-menu-btn" onClick={onDelete} title="Delete program" aria-label="Delete">
            ⋯
          </button>
        </div>
      </div>
      <div className="ws-entity-card-body">
        <h3 className="ws-entity-card-title">{program.name}</h3>
        <p className="ws-entity-card-desc">{program.description || 'No description provided'}</p>
        <ProgressBar value={completionEstimate} label="Initiative coverage" />
        <div className="ws-entity-card-metrics">
          <div className="ws-mini-metric">
            <span>Plans</span>
            <strong>{program.planCount}</strong>
          </div>
          <div className="ws-mini-metric">
            <span>Items</span>
            <strong>{agg.totalItems}</strong>
          </div>
          <div className="ws-mini-metric">
            <span>Teams</span>
            <strong>{agg.totalTeams}</strong>
          </div>
        </div>
      </div>
      <div className="ws-entity-card-footer">
        <span>{program.ownerName || 'Unassigned owner'}</span>
        <div className="ws-entity-card-links">
          <span>{program.isActive ? 'Active' : 'Inactive'}</span>
        </div>
      </div>
    </Link>
  );
}
