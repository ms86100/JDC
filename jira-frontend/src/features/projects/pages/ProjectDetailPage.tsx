import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import { issueApi, IssueResponse } from '../../../api/issueApi';

export default function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>();

  const { data: project, isLoading: projectLoading } = useQuery<ProjectResponse>({
    queryKey: ['project', projectId],
    queryFn: async () => {
      const response = await projectApi.getById(projectId!);
      return response.data;
    },
  });

  const { data: issues } = useQuery<IssueResponse[]>({
    queryKey: ['projectIssues', projectId],
    queryFn: async () => {
      const response = await issueApi.getAll({ projectId: projectId || '' });
      return response.data;
    },
    enabled: !!projectId,
  });

  if (projectLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  return (
    <div className="ab-project-detail">
      <div className="ab-page-header">
        <div>
          <div className="ab-breadcrumb">
            <Link to="/projects" className="ab-link">Projects</Link>
            <span className="ab-text-muted"> / </span>
            <span>{project?.name}</span>
          </div>
          <h1 className="ab-page-title">{project?.name}</h1>
          <p className="ab-page-subtitle">
            Key: <span className="ab-badge ab-badge-primary">{project?.projectKey}</span>
          </p>
        </div>
        <button className="ab-btn ab-btn-primary">
          <span>+</span> Create Issue
        </button>
      </div>

      <div className="ab-card">
        <div className="ab-card-header">
          <h3 className="ab-card-title">Project Details</h3>
        </div>
        <div className="ab-card-body">
          <p><strong>Description:</strong> {project?.description || 'No description'}</p>
          <p><strong>Lead:</strong> {project?.leadUserId || 'Unassigned'}</p>
          <p><strong>Created:</strong> {project?.createdAt ? new Date(project.createdAt).toLocaleDateString() : '-'}</p>
        </div>
      </div>

      <div className="ab-card">
        <div className="ab-card-header">
          <h3 className="ab-card-title">Issues ({issues?.length || 0})</h3>
        </div>
        <div className="ab-card-body">
          {issues && issues.length > 0 ? (
            <table className="ab-table">
              <thead>
                <tr>
                  <th>Key</th>
                  <th>Summary</th>
                  <th>Status</th>
                  <th>Priority</th>
                </tr>
              </thead>
              <tbody>
                {issues.map((issue) => (
                  <tr key={issue.id}>
                    <td className="ab-text-muted">{issue.issueKey}</td>
                    <td>{issue.title}</td>
                    <td><span className="ab-badge ab-badge-primary">{issue.status}</span></td>
                    <td>{issue.priority || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="ab-empty-state">
              <p>No issues in this project yet.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}