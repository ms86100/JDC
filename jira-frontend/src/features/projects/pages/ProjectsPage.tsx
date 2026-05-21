import React, { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import CreateProjectWizard from '../components/CreateProjectWizard';
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
import { useProjectsPortfolioData } from '../../../components/workspace/useWorkspaceData';
import { formatShortDate } from '../../../components/workspace/metrics';
import type { HealthLevel } from '../../../components/workspace/metrics';
import '../../../components/workspace/workspace-dashboard.css';

type ViewMode = 'grid' | 'list';
type FilterType = 'all' | 'software' | 'business' | 'archived';

const TEMPLATE_LABELS: Record<string, string> = {
  SCRUM: 'Scrum',
  KANBAN: 'Kanban',
  BASIC: 'Basic',
  PROJECT_MANAGEMENT: 'Project Mgmt',
  TASK_MANAGEMENT: 'Task Mgmt',
  PROCESS_MANAGEMENT: 'Process',
};

export default function ProjectsPage() {
  const navigate = useNavigate();
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [filter, setFilter] = useState<FilterType>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateWizard, setShowCreateWizard] = useState(false);

  const { data: projects = [], isLoading } = useQuery<ProjectResponse[]>({
    queryKey: ['projects', filter, searchQuery],
    queryFn: async () => {
      const params: Record<string, string> = {};
      if (filter === 'archived') params.archived = 'true';
      return await projectApi.getAll(params);
    },
  });

  const { metricsByProject, isLoading: metricsLoading } = useProjectsPortfolioData(
    projects.map((p) => p.id)
  );

  const filteredProjects = useMemo(() => {
    return projects.filter((project) => {
      if (filter === 'software' && project.projectType !== 'SOFTWARE') return false;
      if (filter === 'business' && project.projectType !== 'BUSINESS') return false;
      if (filter === 'archived' && !project.archived) return false;
      if (filter === 'all' && project.archived) return false;
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        return (
          project.name.toLowerCase().includes(q) ||
          project.projectKey.toLowerCase().includes(q) ||
          (project.description?.toLowerCase().includes(q) ?? false) ||
          (project.leadName?.toLowerCase().includes(q) ?? false)
        );
      }
      return true;
    });
  }, [projects, filter, searchQuery]);

  const portfolio = useMemo(() => {
    let totalIssues = 0;
    let avgCompletion = 0;
    let atRisk = 0;
    filteredProjects.forEach((p) => {
      const m = metricsByProject.get(p.id);
      if (m) {
        totalIssues += m.total;
        avgCompletion += m.completionPct;
        if (m.health === 'at-risk' || m.health === 'critical') atRisk += 1;
      }
    });
    const n = filteredProjects.length || 1;
    return {
      count: filteredProjects.length,
      totalIssues,
      avgCompletion: Math.round(avgCompletion / n),
      atRisk,
    };
  }, [filteredProjects, metricsByProject]);

  const recentProjects = getRecentViews('project');

  const handleOpen = (project: ProjectResponse) => {
    recordRecentView({
      id: project.id,
      type: 'project',
      name: project.name,
      path: `/projects/${project.id}`,
    });
  };

  return (
    <div className="ws-page">
      <WorkspaceHeader
        breadcrumbs={
          <>
            <Link to="/dashboard">Dashboard</Link>
            <span>/</span>
            <span>Projects</span>
          </>
        }
        title="Projects"
        subtitle="Operational workspace for delivery health, sprints, and team workload"
        actions={
          <button type="button" className="ab-btn ab-btn-primary" onClick={() => setShowCreateWizard(true)}>
            + Create Project
          </button>
        }
      />

      <PortfolioSummary>
        <KpiCard label="Projects" value={portfolio.count} accent="brand" />
        <KpiCard label="Total issues" value={portfolio.totalIssues} />
        <KpiCard label="Avg completion" value={`${portfolio.avgCompletion}%`} accent="success" />
        <KpiCard label="Needs attention" value={portfolio.atRisk} accent={portfolio.atRisk > 0 ? 'warning' : 'default'} hint="At risk or critical" />
      </PortfolioSummary>

      <RecentlyViewed items={recentProjects} />

      <div className="ws-toolbar">
        <div className="ws-toolbar-left">
          <div className="ws-search">
            <span className="ws-search-icon" aria-hidden>⌕</span>
            <input
              type="search"
              placeholder="Search projects, keys, leads..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search projects"
            />
          </div>
          <div className="ws-filter-pills" role="tablist">
            {(
              [
                ['all', 'All Projects'],
                ['software', 'Software'],
                ['business', 'Business'],
                ['archived', 'Archived'],
              ] as const
            ).map(([f, label]) => (
              <button
                key={f}
                type="button"
                role="tab"
                className={`ws-filter-pill ${filter === f ? 'ws-filter-pill--active' : ''}`}
                onClick={() => setFilter(f)}
              >
                {label}
              </button>
            ))}
          </div>
        </div>
        <div className="ws-toolbar-right">
          <div className="ws-view-toggle">
            <button
              type="button"
              className={`ws-view-btn ${viewMode === 'grid' ? 'ws-view-btn--active' : ''}`}
              onClick={() => setViewMode('grid')}
              title="Grid view"
            >
              ▦
            </button>
            <button
              type="button"
              className={`ws-view-btn ${viewMode === 'list' ? 'ws-view-btn--active' : ''}`}
              onClick={() => setViewMode('list')}
              title="List view"
            >
              ☰
            </button>
          </div>
        </div>
      </div>

      {isLoading || metricsLoading ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : filteredProjects.length === 0 ? (
        <div className="ws-page-empty">
          <h3>No projects found</h3>
          <p>
            {searchQuery
              ? 'No projects match your search.'
              : filter === 'archived'
                ? 'No archived projects.'
                : 'Create a project to start tracking work, sprints, and delivery.'}
          </p>
          {!searchQuery && filter !== 'archived' && (
            <button type="button" className="ab-btn ab-btn-primary" onClick={() => setShowCreateWizard(true)}>
              Create Project
            </button>
          )}
        </div>
      ) : viewMode === 'grid' ? (
        <div className="ws-entity-grid">
          {filteredProjects.map((project) => (
            <ProjectGridCard
              key={project.id}
              project={project}
              health={metricsByProject.get(project.id)?.health ?? 'unknown'}
              completion={metricsByProject.get(project.id)?.completionPct ?? 0}
              blocked={metricsByProject.get(project.id)?.blocked ?? 0}
              onOpen={() => handleOpen(project)}
            />
          ))}
        </div>
      ) : (
        <div className="ws-table-wrap">
          <table className="ws-table">
            <thead>
              <tr>
                <th>Project</th>
                <th>Key</th>
                <th>Health</th>
                <th>Progress</th>
                <th>Type</th>
                <th>Template</th>
                <th>Issues</th>
                <th>Lead</th>
                <th>Created</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {filteredProjects.map((project) => {
                const m = metricsByProject.get(project.id);
                return (
                  <tr key={project.id} className={project.archived ? 'archived-row' : ''}>
                    <td>
                      <Link to={`/projects/${project.id}`} className="project-link" onClick={() => handleOpen(project)}>
                        <EntityAvatar name={project.name} size="sm" />
                        <span className="project-link-name">{project.name}</span>
                      </Link>
                    </td>
                    <td><span className="ws-entity-card-key">{project.projectKey}</span></td>
                    <td><HealthBadge health={m?.health ?? 'unknown'} /></td>
                    <td>
                      <div style={{ minWidth: 100 }}>
                        <ProgressBar value={m?.completionPct ?? 0} />
                      </div>
                    </td>
                    <td>
                      <span className={`ws-badge ws-badge--${project.projectType === 'SOFTWARE' ? 'software' : 'business'}`}>
                        {project.projectType}
                      </span>
                    </td>
                    <td>{TEMPLATE_LABELS[project.template ?? ''] ?? project.template ?? '—'}</td>
                    <td><strong>{project.issueCounter ?? m?.total ?? 0}</strong></td>
                    <td>{project.leadName || '—'}</td>
                    <td>{formatShortDate(project.createdAt)}</td>
                    <td>
                      <button
                        type="button"
                        className="ab-btn ab-btn-ghost ab-btn-sm"
                        onClick={() => navigate(`/projects/${project.id}/settings`)}
                      >
                        Settings
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {showCreateWizard && <CreateProjectWizard onClose={() => setShowCreateWizard(false)} />}
    </div>
  );
}

function ProjectGridCard({
  project,
  health,
  completion,
  blocked,
  onOpen,
}: {
  project: ProjectResponse;
  health: HealthLevel;
  completion: number;
  blocked: number;
  onOpen: () => void;
}) {
  const template = TEMPLATE_LABELS[project.template ?? ''] ?? 'Standard';

  return (
    <Link to={`/projects/${project.id}`} className="ws-entity-card" onClick={onOpen}>
      <div className="ws-entity-card-top">
        <EntityAvatar name={project.name} />
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
          <HealthBadge health={health} />
          <span className={`ws-badge ws-badge--${project.projectType === 'SOFTWARE' ? 'software' : 'business'}`}>
            {project.projectType}
          </span>
          {project.archived && <span className="ws-badge ws-badge--archived">Archived</span>}
        </div>
      </div>
      <div className="ws-entity-card-body">
        <h3 className="ws-entity-card-title">{project.name}</h3>
        <span className="ws-entity-card-key">{project.projectKey}</span>
        {project.description && <p className="ws-entity-card-desc">{project.description}</p>}
        <ProgressBar value={completion} label="Completion" />
        <div className="ws-entity-card-metrics">
          <div className="ws-mini-metric">
            <span>Issues</span>
            <strong>{project.issueCounter ?? 0}</strong>
          </div>
          <div className="ws-mini-metric">
            <span>Blockers</span>
            <strong>{blocked}</strong>
          </div>
          <div className="ws-mini-metric">
            <span>Method</span>
            <strong style={{ fontSize: 12 }}>{template}</strong>
          </div>
        </div>
      </div>
      <div className="ws-entity-card-footer">
        <span>{project.leadName || 'No lead assigned'}</span>
        <div className="ws-entity-card-links">
          <span>Board</span>
          <span>·</span>
          <span>Issues</span>
        </div>
      </div>
    </Link>
  );
}
