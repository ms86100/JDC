import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { versionApi } from '../../../../api/versionApi';
import { appNotify } from '../../../../lib/appNotify';
import { useState } from 'react';

interface Props {
  projectId: string;
}

export default function BacklogVersionsPanel({ projectId }: Props) {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [showAdd, setShowAdd] = useState(false);

  const { data: versions = [], isPending } = useQuery({
    queryKey: ['project-versions', projectId],
    queryFn: () => versionApi.getByProject(projectId),
    enabled: !!projectId,
  });

  const createMutation = useMutation({
    mutationFn: (versionName: string) =>
      versionApi.create({ projectId, name: versionName.trim() }),
    onSuccess: () => {
      appNotify.success('Version created');
      setName('');
      setShowAdd(false);
      queryClient.invalidateQueries({ queryKey: ['project-versions', projectId] });
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to create version'),
  });

  const unreleased = versions.filter((v) => !v.released && !v.archived);
  const released = versions.filter((v) => v.released);

  return (
    <div className="jdc-backlog-side-panel">
      <div className="jdc-backlog-side-panel-head">
        <h2 className="jdc-page-title" style={{ fontSize: 16, margin: 0 }}>Versions</h2>
        <button type="button" className="jdc-btn jdc-btn-secondary jdc-btn-sm" onClick={() => setShowAdd(!showAdd)}>
          {showAdd ? 'Cancel' : 'Add version'}
        </button>
      </div>
      {showAdd && (
        <div className="jdc-card" style={{ padding: 12, marginBottom: 12 }}>
          <input
            className="jdc-input"
            placeholder="Version name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <button
            type="button"
            className="jdc-btn jdc-btn-primary jdc-btn-sm"
            style={{ marginTop: 8 }}
            disabled={!name.trim() || createMutation.isPending}
            onClick={() => createMutation.mutate(name)}
          >
            {createMutation.isPending ? 'Creating…' : 'Create'}
          </button>
        </div>
      )}
      {isPending ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : (
        <>
          <section className="jdc-backlog-side-section">
            <h3>Unreleased ({unreleased.length})</h3>
            {unreleased.length === 0 ? (
              <p className="jdc-muted">No unreleased versions.</p>
            ) : (
              <ul className="jdc-backlog-side-list">
                {unreleased.map((v) => (
                  <li key={v.id}>
                    <span className="jdc-backlog-side-item-name">{v.name}</span>
                    {v.releaseDate && (
                      <span className="jdc-muted"> · {new Date(v.releaseDate).toLocaleDateString()}</span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
          <section className="jdc-backlog-side-section">
            <h3>Released ({released.length})</h3>
            {released.length === 0 ? (
              <p className="jdc-muted">No released versions yet.</p>
            ) : (
              <ul className="jdc-backlog-side-list">
                {released.map((v) => (
                  <li key={v.id}>
                    <span className="jdc-backlog-side-item-name">{v.name}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
      <p style={{ marginTop: 12 }}>
        <Link to={`/projects/${projectId}/releases`} className="jdc-link">
          Manage all releases →
        </Link>
      </p>
    </div>
  );
}
