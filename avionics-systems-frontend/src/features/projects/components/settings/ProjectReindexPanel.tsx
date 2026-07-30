import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { projectApi } from '../../../../api/projectApi';

interface Props {
  projectId: string;
  projectName: string;
}

export default function ProjectReindexPanel({ projectId, projectName }: Props) {
  const [result, setResult] = useState<{ indexed: number; total: number } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reindexMutation = useMutation({
    mutationFn: () => projectApi.reindexSearch(projectId),
    onSuccess: (data) => {
      setResult(data);
      setError(null);
    },
    onError: (e: unknown) => {
      setError(e instanceof Error ? e.message : 'Re-index failed');
      setResult(null);
    },
  });

  return (
    <section>
      <h2 className="jdc-page-title">Re-index project</h2>
      <p className="jdc-muted">
        Rebuild the search index for all issues in <strong>{projectName}</strong>. This may take a moment
        for large projects (Systems Data Center project maintenance).
      </p>
      <div className="jdc-card" style={{ padding: 20, maxWidth: 520, marginTop: 16 }}>
        <p style={{ marginTop: 0 }}>
          Re-indexing updates search results, boards, and filters that rely on the search service.
        </p>
        <button
          type="button"
          className="jdc-btn jdc-btn-primary"
          disabled={reindexMutation.isPending}
          onClick={() => {
            if (
              window.confirm(
                `Start re-index for ${projectName}? The operation runs in the background.`,
              )
            ) {
              reindexMutation.mutate();
            }
          }}
        >
          {reindexMutation.isPending ? 'Re-indexing…' : 'Start project re-index'}
        </button>
        {result && (
          <p className="jdc-muted" style={{ marginTop: 12 }}>
            Indexed <strong>{result.indexed}</strong> of <strong>{result.total}</strong> issues.
          </p>
        )}
        {error && (
          <p style={{ color: '#de350b', marginTop: 12 }}>{error}</p>
        )}
      </div>
    </section>
  );
}
