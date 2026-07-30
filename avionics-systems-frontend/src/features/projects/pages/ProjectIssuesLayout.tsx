import { useParams } from 'react-router-dom';
import { Outlet } from 'react-router-dom';
import IssuesLayout from '../../issues/pages/IssuesLayout';

/** Project-scoped issue navigator shell. */
export default function ProjectIssuesLayout() {
  const { projectId } = useParams<{ projectId: string }>();
  if (!projectId) return null;

  return (
    <IssuesLayout
      projectId={projectId}
      issuesBasePath={`/projects/${projectId}/issues`}
      detailOutlet={<Outlet context={{ embedded: true }} />}
    />
  );
}
