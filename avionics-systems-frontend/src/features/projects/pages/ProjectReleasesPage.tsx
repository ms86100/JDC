import { useOutletContext, useParams } from 'react-router-dom';
import { ProjectResponse } from '../../../api/projectApi';
import VersionsManager from '../components/VersionsManager';
import '../styles/project-releases-components.css';

interface LayoutContext {
  project?: ProjectResponse;
}

export default function ProjectReleasesPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const ctx = useOutletContext<LayoutContext>();

  if (!projectId) return null;

  return (
    <div className="sa-project-subpage">
      <header className="sa-project-subpage__header">
        <h1 className="sa-project-subpage__title">Releases</h1>
        <p className="sa-project-subpage__lead">
          Plan, track, and release versions for{' '}
          <strong>{ctx.project?.name ?? 'project'}</strong>
          {ctx.project?.projectKey ? ` (${ctx.project.projectKey})` : ''}.
          Create versions, monitor progress, and release when ready.
        </p>
      </header>

      <VersionsManager
        projectId={projectId}
        projectKey={ctx.project?.projectKey}
        variant="hub"
      />
    </div>
  );
}
