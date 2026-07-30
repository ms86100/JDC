interface ProjectLoadErrorProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

/** Non-blocking error when project-scoped APIs fail */
export default function ProjectLoadError({
  title = 'Unable to load project data',
  message = 'Check that backend services are running and you are signed in, then try again.',
  onRetry,
}: ProjectLoadErrorProps) {
  return (
    <div className="sa-project-subpage-empty" role="alert">
      <p><strong>{title}</strong></p>
      <p>{message}</p>
      {onRetry && (
        <div className="sa-project-subpage__actions">
          <button type="button" className="jdc-btn jdc-btn-primary" onClick={onRetry}>
            Retry
          </button>
        </div>
      )}
    </div>
  );
}
