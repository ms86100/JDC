import { useQuery } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { Link } from 'react-router-dom';
import '../DashboardStyles.css';

/* Inline SVG icons — no dependency added. */
const IconFolder = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7z" />
  </svg>
);
const IconList = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" />
    <line x1="8" y1="18" x2="21" y2="18" /><circle cx="4" cy="6" r="1" />
    <circle cx="4" cy="12" r="1" /><circle cx="4" cy="18" r="1" />
  </svg>
);
const IconPlay = () => (
  <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path d="M8 5v14l11-7z" />
  </svg>
);
const IconCheck = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <polyline points="20 6 9 17 4 12" />
  </svg>
);

export default function DashboardPage() {
  const { data: projects = [], isLoading: projectsLoading } = useQuery<ProjectResponse[]>({
    queryKey: ['dashboard-projects'],
    queryFn: async () => {
      // projectApi.getAll() returns Promise<ProjectResponse[]> after transformation
      return await projectApi.getAll();
    },
    staleTime: 30000,
  });

  const { data: recentIssues = [], isLoading: issuesLoading } = useQuery<IssueResponse[]>({
    queryKey: ['dashboard-recent-issues'],
    queryFn: async () => {
      const response = await issueApi.getAll();
      // Handle paginated response - issueApi returns { content: IssueResponse[] }
      if (response && response.data && 'content' in response.data) {
        return response.data.content || [];
      }
      return [];
    },
    staleTime: 30000,
  });

  const stats = {
    totalProjects: projects?.length || 0,
    totalIssues: recentIssues?.length || 0,
    inProgress: recentIssues?.filter(i => i.status === 'In Progress').length || 0,
    completed: recentIssues?.filter(i => i.status === 'Done').length || 0,
  };

  return (
    <div className="ab-dashboard">
      <header className="ab-page-header">
        <h1 className="ab-page-title">Dashboard</h1>
        <p className="ab-page-subtitle">Overview of your projects and recent activity</p>
      </header>

      {/* Stats Cards */}
      <section className="ab-dashboard-stats" aria-label="Key metrics">
        <div className="ab-stat-card">
          <div className="ab-stat-icon"><IconFolder /></div>
          <div className="ab-stat-content">
            <span className="ab-stat-value">{stats.totalProjects}</span>
            <span className="ab-stat-label">Projects</span>
          </div>
        </div>
        <div className="ab-stat-card">
          <div className="ab-stat-icon"><IconList /></div>
          <div className="ab-stat-content">
            <span className="ab-stat-value">{stats.totalIssues}</span>
            <span className="ab-stat-label">Total Issues</span>
          </div>
        </div>
        <div className="ab-stat-card ab-stat-progress">
          <div className="ab-stat-icon"><IconPlay /></div>
          <div className="ab-stat-content">
            <span className="ab-stat-value">{stats.inProgress}</span>
            <span className="ab-stat-label">In Progress</span>
          </div>
        </div>
        <div className="ab-stat-card ab-stat-done">
          <div className="ab-stat-icon"><IconCheck /></div>
          <div className="ab-stat-content">
            <span className="ab-stat-value">{stats.completed}</span>
            <span className="ab-stat-label">Completed</span>
          </div>
        </div>
      </section>

      <div className="ab-dashboard-grid">
        {/* Recent Issues */}
        <section className="ab-card ab-dashboard-section" aria-label="Recent issues">
          <div className="ab-card-header">
            <h2 className="ab-card-title">Recent Issues</h2>
            <Link to="/issues" className="ab-link">View all</Link>
          </div>
          <div className="ab-card-body">
            {issuesLoading ? (
              <div aria-busy="true">
                <div className="ab-skeleton-row" />
                <div className="ab-skeleton-row" />
                <div className="ab-skeleton-row" />
              </div>
            ) : recentIssues && recentIssues.length > 0 ? (
              <div className="ab-issue-list">
                {recentIssues.slice(0, 5).map((issue) => (
                  <Link to={`/issues/${issue.id}`} key={issue.id} className="ab-issue-item">
                    <div className="ab-issue-key">{issue.issueKey}</div>
                    <div className="ab-issue-title">{issue.title}</div>
                    <div className="ab-issue-meta">
                      <span className={`ab-badge ab-badge-${getStatusVariant(issue.status)}`}>
                        {issue.status || 'To Do'}
                      </span>
                      {issue.priority && (
                        <span className="ab-priority">
                          {issue.priority}
                        </span>
                      )}
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="ab-empty-state">
                <span className="ab-empty-icon" aria-hidden="true">📋</span>
                <p>No recent issues</p>
              </div>
            )}
          </div>
        </section>

        {/* Projects Overview */}
        <section className="ab-card ab-dashboard-section" aria-label="Projects">
          <div className="ab-card-header">
            <h2 className="ab-card-title">Projects</h2>
            <Link to="/projects" className="ab-link">View all</Link>
          </div>
          <div className="ab-card-body">
            {projectsLoading ? (
              <div aria-busy="true">
                <div className="ab-skeleton-row" />
                <div className="ab-skeleton-row" />
                <div className="ab-skeleton-row" />
              </div>
            ) : projects && projects.length > 0 ? (
              <div className="ab-project-list">
                {projects.slice(0, 5).map((project) => (
                  <Link to={`/projects/${project.id}`} key={project.id} className="ab-project-item">
                    <div className="ab-project-icon">
                      {project.projectKey?.charAt(0) || 'P'}
                    </div>
                    <div className="ab-project-info">
                      <span className="ab-project-name">{project.name}</span>
                      <span className="ab-project-key">{project.projectKey}</span>
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="ab-empty-state">
                <span className="ab-empty-icon" aria-hidden="true">📁</span>
                <p>No projects yet</p>
                <Link to="/projects" className="ab-btn ab-btn-primary">Create Project</Link>
              </div>
            )}
          </div>
        </section>

        {/* Activity Feed */}
        <section className="ab-card ab-dashboard-section ab-activity-feed" aria-label="Recent activity">
          <div className="ab-card-header">
            <h2 className="ab-card-title">Recent Activity</h2>
          </div>
          <div className="ab-card-body">
            <div className="ab-activity-list">
              <div className="ab-activity-item">
                <div className="ab-activity-dot ab-activity-create"></div>
                <div className="ab-activity-content">
                  <span className="ab-activity-text">
                    <strong>System</strong> is ready for use
                  </span>
                  <span className="ab-activity-time">Just now</span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

function getStatusVariant(status: string | undefined): string {
  switch (status?.toLowerCase()) {
    case 'done':
    case 'resolved':
    case 'closed':
      return 'success';
    case 'in progress':
    case 'in review':
      return 'primary';
    case 'to do':
    case 'open':
      return 'secondary';
    default:
      return 'secondary';
  }
}
