import { Link } from 'react-router-dom';
import type { ProjectResponse } from '../../../api/projectApi';
import type { IssueResponse } from '../../../api/issueApi';
import type { SprintResponse } from '../../../api/sprintApi';
import type { WorkMetrics } from '../../../components/workspace/metrics';
import { formatShortDate } from '../../../components/workspace/metrics';
import {
  SectionPanel,
  ProgressBar,
  WorkDistribution,
} from '../../../components/workspace/WorkspaceComponents';
import {
  useScrumProjectData,
  useKanbanProjectData,
  groupIssuesByStatus,
  groupIssuesByAssignee,
  getUpcomingDeadlines,
} from '../../../components/workspace/useProjectTypeData';
import type { AgileBoard } from '../../../api/boardApi';
import BurndownChart from '../../sprints/components/BurndownChart';
import VelocityChart from '../../sprints/components/VelocityChart';
import { defaultBoardPath, boardPath } from '../../../components/workspace/boardLinks';

export interface ProjectTypeOverviewProps {
  project: ProjectResponse;
  projectId: string;
  issues: IssueResponse[];
  metrics: WorkMetrics;
  sprints: SprintResponse[];
  activeSprint?: SprintResponse;
  boards: AgileBoard[];
}

export function ProjectTypeOverview(props: ProjectTypeOverviewProps) {
  const template = props.project.template ?? 'BASIC';

  switch (template) {
    case 'SCRUM':
      return <ScrumOverview {...props} />;
    case 'KANBAN':
      return <KanbanOverview {...props} />;
    case 'TASK_MANAGEMENT':
      return <TaskManagementOverview {...props} />;
    case 'PROCESS_MANAGEMENT':
      return <ProcessManagementOverview {...props} />;
    case 'PROJECT_MANAGEMENT':
      return <ProjectManagementOverview {...props} />;
    default:
      return <DefaultOverview {...props} />;
  }
}

function ScrumOverview({
  projectId,
  issues,
  metrics,
  activeSprint,
  boards,
  sprints,
}: ProjectTypeOverviewProps) {
  const { burndown, velocity, isLoading } = useScrumProjectData(projectId, activeSprint, boards);
  const totalPoints = burndown?.totalPoints ?? burndown?.remainingPoints ?? metrics.total;

  return (
    <div className="ws-type-overview ws-type-overview--scrum">
      <div className="ws-dashboard-row ws-dashboard-row--2">
        <SectionPanel title="Sprint burndown" subtitle={activeSprint ? activeSprint.name : 'No active sprint'}>
          {isLoading ? (
            <p className="ws-muted">Loading burndown…</p>
          ) : activeSprint && burndown?.dailyData?.length ? (
            <BurndownChart data={burndown.dailyData} totalPoints={totalPoints} />
          ) : activeSprint ? (
            <ScrumSprintFallback sprint={activeSprint} issues={issues} />
          ) : (
            <p className="ws-muted">Start a sprint to track burndown and iteration progress.</p>
          )}
        </SectionPanel>

        <SectionPanel title="Velocity" subtitle="Committed vs completed across sprints">
          {velocity?.sprintVelocities?.length ? (
            <>
              <div className="ws-entity-card-metrics" style={{ marginBottom: 12 }}>
                <div className="ws-mini-metric">
                  <span>Current</span>
                  <strong>{velocity.currentVelocity}</strong>
                </div>
                <div className="ws-mini-metric">
                  <span>Average</span>
                  <strong>{(velocity.averageVelocity ?? 0).toFixed(1)}</strong>
                </div>
                <div className="ws-mini-metric">
                  <span>Sprints</span>
                  <strong>{velocity.completedSprints}/{velocity.totalSprints}</strong>
                </div>
              </div>
              <VelocityChart
                data={velocity.sprintVelocities}
                averageVelocity={velocity.averageVelocity}
              />
            </>
          ) : (
            <p className="ws-muted">Complete sprints to build velocity history.</p>
          )}
        </SectionPanel>
      </div>

      <div className="ws-dashboard-row ws-dashboard-row--2">
        <SectionPanel title="Backlog health" subtitle="Work ready for sprint planning">
          <ProgressBar value={metrics.completionPct} label="Release progress" />
          <div style={{ marginTop: 16 }}><WorkDistribution metrics={metrics} /></div>
        </SectionPanel>

        <SectionPanel title="Sprints" subtitle={`${sprints.length} total`}>
          <ul className="ws-sprint-list">
            {sprints.slice(0, 6).map((s) => (
              <li key={s.id} className="ws-sprint-list-item">
                <span className="ws-sprint-list-name">{s.name}</span>
                <span className={`ws-sprint-status ws-sprint-status--${s.status.toLowerCase()}`}>{s.status}</span>
                <span className="ws-muted">{s.issueCount ?? 0} issues</span>
              </li>
            ))}
          </ul>
          <Link to={`/sprints?projectId=${projectId}`} className="ws-link-action">View all sprints →</Link>
        </SectionPanel>
      </div>
    </div>
  );
}

function ScrumSprintFallback({ sprint, issues }: { sprint: SprintResponse; issues: IssueResponse[] }) {
  const total = sprint.issueCount ?? issues.length;
  const done = sprint.completedIssueCount ?? 0;
  const remaining = Math.max(0, total - done);
  return (
    <div>
      <ProgressBar value={total ? Math.round((done / total) * 100) : 0} label="Sprint completion" />
      <div className="ws-entity-card-metrics" style={{ marginTop: 12 }}>
        <div className="ws-mini-metric"><span>Remaining</span><strong>{remaining}</strong></div>
        <div className="ws-mini-metric"><span>Completed</span><strong>{done}</strong></div>
        <div className="ws-mini-metric"><span>End date</span><strong style={{ fontSize: 11 }}>{formatShortDate(sprint.endDate)}</strong></div>
      </div>
    </div>
  );
}

function KanbanOverview({ projectId, issues, metrics, boards }: ProjectTypeOverviewProps) {
  const { kanbanBoard, boardData, isLoading } = useKanbanProjectData(projectId, boards);
  const openBoardHref = kanbanBoard ? boardPath(kanbanBoard.id) : defaultBoardPath(boards);
  const columns = boardData?.columns ?? [];
  const columnIssues = columns.map((col) => ({
    ...col,
    issues: (boardData?.issues ?? issues).filter((i) => i.status === col.name),
  }));

  const fallbackGroups = groupIssuesByStatus(issues);

  return (
    <div className="ws-type-overview ws-type-overview--kanban">
      <SectionPanel
        title="Workflow & WIP"
        subtitle={kanbanBoard ? kanbanBoard.name : 'Continuous flow'}
        action={boards.length ? <Link to={openBoardHref}>Open board</Link> : undefined}
      >
        {isLoading ? (
          <p className="ws-muted">Loading board columns…</p>
        ) : columnIssues.length > 0 ? (
          <div className="ws-wip-board">
            {columnIssues.map((col) => {
              const count = col.issues.length || col.currentIssues;
              const max = col.maxIssues;
              const pct = max ? Math.min(100, (count / max) * 100) : 0;
              const exceeded = max && count > max;
              const warning = max && count >= max * 0.85 && !exceeded;
              return (
                <div key={col.id} className="ws-wip-column">
                  <div className="ws-wip-column-header">
                    <span className="ws-wip-column-name">{col.name}</span>
                    <span className={`ws-wip-count ${exceeded ? 'ws-wip-count--exceeded' : warning ? 'ws-wip-count--warning' : ''}`}>
                      {count}{max ? ` / ${max}` : ''}
                    </span>
                  </div>
                  {max && (
                    <div className="ws-wip-bar">
                      <div
                        className={`ws-wip-bar-fill ${exceeded ? 'ws-wip-bar-fill--exceeded' : warning ? 'ws-wip-bar-fill--warning' : ''}`}
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  )}
                  <span className="ws-wip-category">{col.statusCategory}</span>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="ws-wip-board">
            {fallbackGroups.map((g) => (
              <div key={g.status} className="ws-wip-column">
                <div className="ws-wip-column-header">
                  <span className="ws-wip-column-name">{g.status}</span>
                  <span className="ws-wip-count">{g.count}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </SectionPanel>

      <div className="ws-dashboard-row ws-dashboard-row--2">
        <SectionPanel title="Flow metrics">
          <div className="ws-entity-card-metrics">
            <div className="ws-mini-metric"><span>In progress</span><strong>{metrics.inProgress}</strong></div>
            <div className="ws-mini-metric"><span>Done</span><strong>{metrics.done}</strong></div>
            <div className="ws-mini-metric"><span>Throughput</span><strong>{metrics.done}</strong></div>
          </div>
          <div style={{ marginTop: 16 }}><WorkDistribution metrics={metrics} /></div>
        </SectionPanel>

        <SectionPanel title="Cycle time focus" subtitle="Items currently in flight">
          <p className="ws-muted">
            {metrics.inProgress} items in progress. Monitor WIP limits to reduce bottlenecks and improve flow efficiency.
          </p>
        </SectionPanel>
      </div>
    </div>
  );
}

function TaskManagementOverview({ issues, metrics }: ProjectTypeOverviewProps) {
  const byAssignee = groupIssuesByAssignee(issues);
  const deadlines = getUpcomingDeadlines(issues);

  return (
    <div className="ws-type-overview ws-type-overview--task">
      <div className="ws-dashboard-row ws-dashboard-row--2">
        <SectionPanel title="Assignments" subtitle="Workload by team member">
          <ul className="ws-assignee-list">
            {byAssignee.slice(0, 8).map((a) => (
              <li key={a.assignee} className="ws-assignee-row">
                <span className="ws-assignee-name">{a.assignee}</span>
                <span className="ws-assignee-bar-wrap">
                  <span
                    className="ws-assignee-bar"
                    style={{ width: `${Math.min(100, (a.count / Math.max(issues.length, 1)) * 100 * 3)}%` }}
                  />
                </span>
                <span className="ws-assignee-count">{a.count}</span>
                {a.overdue > 0 && <span className="ws-assignee-overdue">{a.overdue} overdue</span>}
              </li>
            ))}
          </ul>
        </SectionPanel>

        <SectionPanel title="Upcoming deadlines" subtitle="Tasks due soon">
          {deadlines.length > 0 ? (
            <ul className="ws-deadline-list">
              {deadlines.map((i) => (
                <li key={i.id}>
                  <Link to={`/issues/${i.id}`}>
                    <span className="ws-activity-key">{i.issueKey}</span>
                    <span>{i.title}</span>
                    <time>{formatShortDate(i.dueDate)}</time>
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <p className="ws-muted">No upcoming due dates.</p>
          )}
        </SectionPanel>
      </div>

      <SectionPanel title="Completion" subtitle="Task closure rate">
        <ProgressBar value={metrics.completionPct} label="Tasks completed" />
        <div className="ws-entity-card-metrics" style={{ marginTop: 12 }}>
          <div className="ws-mini-metric"><span>Overdue</span><strong>{metrics.overdue}</strong></div>
          <div className="ws-mini-metric"><span>Open</span><strong>{metrics.todo + metrics.inProgress}</strong></div>
          <div className="ws-mini-metric"><span>Done</span><strong>{metrics.done}</strong></div>
        </div>
      </SectionPanel>
    </div>
  );
}

function ProcessManagementOverview({ issues, metrics }: ProjectTypeOverviewProps) {
  const stages = groupIssuesByStatus(issues);
  const maxCount = Math.max(...stages.map((s) => s.count), 1);

  return (
    <div className="ws-type-overview ws-type-overview--process">
      <SectionPanel title="Lifecycle pipeline" subtitle="Work by process stage">
        <div className="ws-lifecycle">
          {stages.map((stage, idx) => (
            <div key={stage.status} className="ws-lifecycle-stage">
              <div className="ws-lifecycle-connector" aria-hidden>
                {idx > 0 && <span className="ws-lifecycle-arrow">→</span>}
              </div>
              <div className="ws-lifecycle-card">
                <span className="ws-lifecycle-name">{stage.status}</span>
                <span className="ws-lifecycle-count">{stage.count}</span>
                <div className="ws-lifecycle-bar">
                  <div className="ws-lifecycle-bar-fill" style={{ width: `${(stage.count / maxCount) * 100}%` }} />
                </div>
              </div>
            </div>
          ))}
        </div>
      </SectionPanel>

      <div className="ws-dashboard-row ws-dashboard-row--2">
        <SectionPanel title="Approvals & blockers" subtitle="Items needing attention">
          <div className="ws-entity-card-metrics">
            <div className="ws-mini-metric"><span>Blocked</span><strong>{metrics.blocked}</strong></div>
            <div className="ws-mini-metric"><span>In review</span><strong>{metrics.inProgress}</strong></div>
            <div className="ws-mini-metric"><span>Completed</span><strong>{metrics.done}</strong></div>
          </div>
        </SectionPanel>

        <SectionPanel title="Process efficiency">
          <ProgressBar value={metrics.completionPct} label="Stages completed" />
        </SectionPanel>
      </div>
    </div>
  );
}

function ProjectManagementOverview({ projectId, issues, metrics, sprints, activeSprint }: ProjectTypeOverviewProps) {
  return (
    <div className="ws-type-overview ws-type-overview--pm">
      <div className="ws-dashboard-row ws-dashboard-row--2">
        <SectionPanel title="Delivery progress" subtitle="Cross-team coordination">
          <ProgressBar value={metrics.completionPct} label="Milestone completion" />
          <div style={{ marginTop: 16 }}><WorkDistribution metrics={metrics} /></div>
        </SectionPanel>
        <SectionPanel title="Active iteration" subtitle={activeSprint?.name ?? 'No active sprint'}>
          {activeSprint ? (
            <ScrumSprintFallback sprint={activeSprint} issues={issues} />
          ) : (
            <p className="ws-muted">Link sprints to track iteration progress across teams.</p>
          )}
          <Link to={`/sprints?projectId=${projectId}`} className="ws-link-action">Manage sprints →</Link>
        </SectionPanel>
      </div>
      <SectionPanel title="Program alignment" subtitle="Portfolio milestones">
        <p className="ws-muted">
          {sprints.length} sprints configured. Use Programs to align milestones and dependencies across projects.
        </p>
        <Link to="/programs" className="ws-link-action">View programs →</Link>
      </SectionPanel>
    </div>
  );
}

function DefaultOverview({ issues, metrics }: ProjectTypeOverviewProps) {
  return (
    <SectionPanel title="Work summary">
      <ProgressBar value={metrics.completionPct} label="Overall completion" />
      <div style={{ marginTop: 16 }}><WorkDistribution metrics={metrics} /></div>
      <p className="ws-muted" style={{ marginTop: 12 }}>
        {issues.length} issues tracked. Select a project template for specialized dashboards (Scrum, Kanban, etc.).
      </p>
    </SectionPanel>
  );
}
