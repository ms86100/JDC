import { Outlet, useLocation, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { projectApi } from '../../../api/projectApi';
import { isProjectDcSubRoute } from '../projectDcNav';
import { resolveProjectTemplate } from '../../../lib/projectTemplate';
import ProjectDcSidebar from './ProjectDcSidebar';
import ProjectLoadError from './ProjectLoadError';
import '../styles/project-dc.css';
import '../styles/project-subpages.css';

export default function ProjectDcLayout() {
  const { projectId } = useParams<{ projectId: string }>();
  const location = useLocation();

  const {
    data: project,
    isPending,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId!).then((r) => r.data),
    enabled: !!projectId,
    staleTime: 60000,
    retry: 1,
  });

  const template = resolveProjectTemplate(project);
  const boardHref = projectId ? `/projects/${projectId}/board/active` : undefined;
  const showSidebar = projectId && isProjectDcSubRoute(location.pathname, projectId);

  if (!projectId) {
    return <Outlet />;
  }

  return (
    <div className="jdc-project-layout">
      {showSidebar && (
        <ProjectDcSidebar
          projectId={projectId}
          projectKey={project?.projectKey}
          projectName={project?.name}
          template={template}
          category={project?.category}
          activeBoardPath={boardHref}
        />
      )}
      <div className="jdc-project-main">
        <div className="jdc-project-content">
          <div className="sa-project-route-body">
            {isPending && !project ? (
              <div className="ab-loading" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <div className="ab-spinner" />
              </div>
            ) : isError && !project ? (
              <ProjectLoadError
                title="Project could not be loaded"
                message={
                  error instanceof Error
                    ? error.message
                    : 'The project API did not respond. Confirm the gateway and project service are running.'
                }
                onRetry={() => refetch()}
              />
            ) : (
              <Outlet context={{ project, projectId, template }} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
