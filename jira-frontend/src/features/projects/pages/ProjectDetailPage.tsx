import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import CreateIssueModal from '../../issues/components/CreateIssueModal';
import {
  WorkspaceHeader,
  QuickNavTabs,
  ContextActionBar,
  KpiCard,
  PortfolioSummary,
  HealthBadge,
  SectionPanel,
  ActivityFeed,
  RisksBlockers,
} from '../../../components/workspace/WorkspaceComponents';
import { ProjectTypeOverview } from '../components/ProjectTypeOverviews';
import { useProjectWorkspaceData } from '../../../components/workspace/useWorkspaceData';
import { defaultBoardPath } from '../../../components/workspace/boardLinks';
import { recordRecentView } from '../../../components/workspace/recentViews';
import { formatShortDate } from '../../../components/workspace/metrics';
import '../../../components/workspace/workspace-dashboard.css';
import '../styles/ProjectDetailPage.css';

type DetailTab = 'overview' | 'issues' | 'activity';

const TEMPLATE_NAV: Record<string, { label: string; emphasis: string }> = {
  SCRUM: { label: 'Scrum', emphasis: 'Sprint velocity, backlog, and burndown' },
  KANBAN: { label: 'Kanban', emphasis: 'Workflow columns and WIP limits' },
  TASK_MANAGEMENT: { label: 'Tasks', emphasis: 'Assignments and due dates' },
  PROCESS_MANAGEMENT: { label: 'Process', emphasis: 'Lifecycle stages and approvals' },
  PROJECT_MANAGEMENT: { label: 'Project Mgmt', emphasis: 'Milestones and cross-team coordination' },
};

export default function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [tab, setTab] = useState<DetailTab>('overview');

  const { data: project, isLoading: projectLoading } = useQuery<ProjectResponse>({
    queryKey: ['project', projectId],
    queryFn: async () => {
      const response = await projectApi.getById(projectId!);
      return response.data;
    },
  });

  const { issues, metrics, sprints, activeSprint, boards, isLoading: wsLoading } =
    useProjectWorkspaceData(projectId);

  useEffect(() => {
    if (project && projectId) {
      recordRecentView({
        id: projectId,
        type: 'project',
        name: project.name,
        path: `/projects/${projectId}`,
      });
    }
  }, [project, projectId]);

  if (projectLoading) {
    return <div className="ab-loading"><div className="ab-spinner" /></div>;
  }

  if (!project) {
    return (
      <div className="ws-page ws-page-empty">
        <h3>Project not found</h3>
        <Link to="/projects" className="ab-btn ab-btn-primary">Back to Projects</Link>
      </div>
    );
  }

  const templateInfo = TEMPLATE_NAV[project.template ?? ''] ?? { label: project.template ?? 'Standard', emphasis: 'Issues and delivery tracking' };
  const inProgressSprints = sprints.filter((s) => s.status === 'ACTIVE').length;
  const boardHref = defaultBoardPath(boards);

  return (
    <div className="ws-page ab-project-detail">
      <WorkspaceHeader
        breadcrumbs={
          <>
            <Link to="/projects">Projects</Link>
            <span>/</span>
            <span>{project.name}</span>
          </>
        }
        title={project.name}
        subtitle={
          <>
            <span className="ws-entity-card-key">{project.projectKey}</span>
            <span> · {templateInfo.label} — {templateInfo.emphasis}</span>
          </>
        }
        badges={
          <>
            <HealthBadge health={metrics.health} />
            <span className={`ws-badge ws-badge--${project.projectType === 'SOFTWARE' ? 'software' : 'business'}`}>
              {project.projectType}
            </span>
            {project.archived && <span className="ws-badge ws-badge--archived">Archived</span>}
          </>
        }
        meta={
          <>
            <div className="ws-header-meta-item">
              <span>Lead</span>
              <strong>{project.leadName || 'Unassigned'}</strong>
            </div>
            <div className="ws-header-meta-item">
              <span>Created</span>
              <strong>{formatShortDate(project.createdAt)}</strong>
            </div>
            <div className="ws-header-meta-item">
              <span>Active sprint</span>
              <strong>{activeSprint?.name ?? 'None'}</strong>
            </div>
          </>
        }
        actions={
          <>
            <button type="button" className="ab-btn ab-btn-secondary" onClick={() => navigate(`/projects/${projectId}/settings`)}>
              Settings
            </button>
            <button type="button" className="ab-btn ab-btn-primary" onClick={() => setShowCreateModal(true)}>
              + Create Issue
            </button>
          </>
        }
      />

      <QuickNavTabs
        items={[
          { label: 'Overview', active: tab === 'overview', onClick: () => setTab('overview') },
          { label: 'Issues', active: tab === 'issues', onClick: () => setTab('issues') },
          { label: 'Activity', active: tab === 'activity', onClick: () => setTab('activity') },
        ]}
      />

      <ContextActionBar>
        <Link to={boardHref}>Board</Link>
        <Link to="/issues">Backlog</Link>
        <Link to="/sprints">Sprints ({inProgressSprints} active)</Link>
        <Link to="/workflows">Workflows</Link>
        <button type="button" onClick={() => navigate(`/projects/${projectId}/settings`)}>Project settings</button>
      </ContextActionBar>

      <PortfolioSummary>
        <KpiCard label="Completion" value={`${metrics.completionPct}%`} accent="success" />
        <KpiCard label="Total issues" value={metrics.total} />
        <KpiCard label="In progress" value={metrics.inProgress} accent="brand" />
        <KpiCard label="Blockers" value={metrics.blocked} accent={metrics.blocked > 0 ? 'danger' : 'default'} />
        <KpiCard label="Overdue" value={metrics.overdue} accent={metrics.overdue > 0 ? 'warning' : 'default'} />
        <KpiCard label="Boards" value={boards.length} hint={`${boards.filter((b) => b.isDefault).length} default`} />
      </PortfolioSummary>

      {wsLoading ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : tab === 'overview' ? (
        <div className="ws-dashboard">
          <ProjectTypeOverview
            project={project}
            projectId={projectId!}
            issues={issues}
            metrics={metrics}
            sprints={sprints}
            activeSprint={activeSprint}
            boards={boards}
          />

          <div className="ws-dashboard-row ws-dashboard-row--main" style={{ marginTop: 16 }}>
            <div className="ws-dashboard-col">
              <SectionPanel title="Project details">
                <p className="ws-muted" style={{ margin: '0 0 12px' }}>{project.description || 'No description provided.'}</p>
                <div className="ws-entity-card-metrics">
                  <div className="ws-mini-metric">
                    <span>Classification</span>
                    <strong style={{ fontSize: 11 }}>{project.classification ?? '—'}</strong>
                  </div>
                  <div className="ws-mini-metric">
                    <span>Category</span>
                    <strong style={{ fontSize: 12 }}>{project.category ?? '—'}</strong>
                  </div>
                  <div className="ws-mini-metric">
                    <span>Template</span>
                    <strong style={{ fontSize: 12 }}>{templateInfo.label}</strong>
                  </div>
                </div>
              </SectionPanel>
            </div>

            <div className="ws-dashboard-col">
              <SectionPanel title="Risks & blockers" subtitle="High-priority and overdue work">
                <RisksBlockers issues={issues} />
              </SectionPanel>

              <SectionPanel title="Recent activity" subtitle="Latest issue updates" action={<Link to="/issues">View all</Link>}>
                <ActivityFeed issues={issues} limit={8} />
              </SectionPanel>
            </div>
          </div>
        </div>
      ) : tab === 'issues' ? (
        <SectionPanel title={`Issues (${issues.length})`}>
          {issues.length > 0 ? (
            <div className="ws-table-wrap">
              <table className="ws-table">
                <thead>
                  <tr>
                    <th>Key</th>
                    <th>Summary</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Assignee</th>
                    <th>Updated</th>
                  </tr>
                </thead>
                <tbody>
                  {issues.map((issue) => (
                    <tr key={issue.id}>
                      <td>
                        <Link to={`/issues/${issue.id}`} className="ws-activity-key">{issue.issueKey}</Link>
                      </td>
                      <td>{issue.title}</td>
                      <td><span className="ws-status-pill">{issue.status}</span></td>
                      <td>{issue.priority || '—'}</td>
                      <td>{issue.assigneeName || '—'}</td>
                      <td>{formatShortDate(issue.updatedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="ws-muted">No issues in this project yet.</p>
          )}
        </SectionPanel>
      ) : (
        <SectionPanel title="Activity feed">
          <ActivityFeed issues={issues} limit={20} />
        </SectionPanel>
      )}

      {showCreateModal && (
        <CreateIssueModal
          projectId={projectId!}
          projectKey={project.projectKey}
          onClose={() => setShowCreateModal(false)}
        />
      )}
    </div>
  );
}
