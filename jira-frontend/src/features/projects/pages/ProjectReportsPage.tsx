import { Link, useOutletContext, useParams } from 'react-router-dom';
import { ProjectResponse } from '../../../api/projectApi';
import '../../tests/styles/xray-hub.css';

interface LayoutContext {
  project?: ProjectResponse;
}

const REPORT_LINKS = [
  {
    title: 'Sprint reports & velocity',
    description: 'Burndown, sprint report, and velocity metrics for Scrum projects',
    path: (pid: string) => `/sprints?projectId=${pid}`,
  },
  {
    title: 'Cumulative flow diagram',
    description: 'Track how issues flow through columns over time — identify bottlenecks',
    path: (pid: string) => `/sprints?projectId=${pid}&tab=cfd`,
  },
  {
    title: 'Control chart',
    description: 'Cycle time and lead time scatter plot with mean and standard deviation',
    path: (pid: string) => `/sprints?projectId=${pid}&tab=control`,
  },
  {
    title: 'Time tracking report',
    description: 'Logged work across issues (global report, filter by project)',
    path: () => '/reports/time-tracking',
  },
  {
    title: 'Issue navigator',
    description: 'Filter, export, and bulk operations on project issues',
    path: (pid: string) => `/projects/${pid}/issues`,
  },
  {
    title: 'Xray test reports',
    description: 'Test execution and coverage dashboards (Xray plugin)',
    path: (pid: string) => `/tests/reporting/${pid}`,
  },
  {
    title: 'Xray traceability',
    description: 'Requirement ↔ test matrix',
    path: (pid: string) => `/tests/traceability/${pid}`,
  },
];

export default function ProjectReportsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const ctx = useOutletContext<LayoutContext>();

  return (
    <div className="sa-project-subpage">
      <header className="sa-project-subpage__header">
        <h1 className="sa-project-subpage__title">Reports</h1>
        <p className="sa-project-subpage__lead">
          Reports and analytics for <strong>{ctx.project?.name ?? 'project'}</strong>
        </p>
      </header>

      <div className="xray-hub-grid">
        {REPORT_LINKS.map((r) => (
          <Link
            key={r.title}
            to={projectId ? r.path(projectId) : '#'}
            className="xray-hub-card"
          >
            <span className="xray-hub-card-label">{r.title}</span>
            <span className="xray-hub-card-desc">{r.description}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
