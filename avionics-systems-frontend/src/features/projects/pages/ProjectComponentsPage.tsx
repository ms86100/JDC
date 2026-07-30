import { useOutletContext, useParams } from 'react-router-dom';
import { ProjectResponse } from '../../../api/projectApi';
import ComponentsManager from '../components/ComponentsManager';
import '../styles/project-releases-components.css';

interface LayoutContext {
  project?: ProjectResponse;
}

export default function ProjectComponentsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const ctx = useOutletContext<LayoutContext>();

  if (!projectId) return null;

  return (
    <div className="sa-project-subpage">
      <header className="sa-project-subpage__header">
        <h1 className="sa-project-subpage__title">Components</h1>
        <p className="sa-project-subpage__lead">
          Logical subsystems for <strong>{ctx.project?.projectKey ?? 'project'}</strong>
          {ctx.project?.name ? ` — ${ctx.project.name}` : ''}.
          Set component leads and default assignees for new issues.
        </p>
      </header>

      <ComponentsManager projectId={projectId} variant="hub" />
    </div>
  );
}
