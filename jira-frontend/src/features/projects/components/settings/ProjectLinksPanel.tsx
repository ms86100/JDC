import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { integrationApi } from '../../../../api/integrationApi';

export default function ProjectLinksPanel() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [url, setUrl] = useState('');

  const { data: links = [], isLoading } = useQuery({
    queryKey: ['integration', 'applinks'],
    queryFn: () => integrationApi.listApplicationLinks().then((r) => r.data),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      integrationApi.createApplicationLink({
        name: name.trim(),
        url: url.trim(),
        applicationType: 'generic',
        direction: 'two-way',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['integration', 'applinks'] });
      setShowCreate(false);
      setName('');
      setUrl('');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => integrationApi.deleteApplicationLink(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['integration', 'applinks'] }),
  });

  return (
    <section>
      <h2 className="jdc-page-title">Project links</h2>
      <p className="jdc-muted">
        Application links connect this Systems instance to other applications (Confluence, Bitbucket, etc.).
      </p>
      {isLoading ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : links.length === 0 ? (
        <div className="jdc-card jdc-empty-state-card" style={{ padding: 32, textAlign: 'center', marginTop: 16 }}>
          <h3 style={{ marginTop: 0 }}>No application links configured</h3>
          <p className="jdc-muted">
            Link Systems to other Atlassian products or external tools so issues, pages, and builds stay connected.
          </p>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'center', flexWrap: 'wrap' }}>
            <button type="button" className="jdc-btn jdc-btn-primary" onClick={() => setShowCreate(true)}>
              Create application link
            </button>
            <a
              href="https://confluence.atlassian.com/adminjiraserver/configuring-application-links-938846918.html"
              target="_blank"
              rel="noopener noreferrer"
              className="jdc-btn jdc-btn-secondary"
            >
              Learn about application links
            </a>
            <Link to="/admin/application-links" className="jdc-btn jdc-btn-secondary">
              Administration
            </Link>
          </div>
        </div>
      ) : (
        <>
          <div style={{ marginBottom: 12 }}>
            <button type="button" className="jdc-btn jdc-btn-secondary" onClick={() => setShowCreate(!showCreate)}>
              Add link
            </button>
            <Link to="/admin/application-links" className="jdc-link" style={{ marginLeft: 12 }}>
              Manage in administration →
            </Link>
          </div>
          {showCreate && (
            <div className="jdc-card" style={{ padding: 16, marginBottom: 16 }}>
              <div className="jdc-form-row">
                <label className="jdc-label">Name</label>
                <input className="jdc-input" value={name} onChange={(e) => setName(e.target.value)} />
              </div>
              <div className="jdc-form-row">
                <label className="jdc-label">URL</label>
                <input className="jdc-input" value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://" />
              </div>
              <button
                type="button"
                className="jdc-btn jdc-btn-primary"
                disabled={!name.trim() || !url.trim() || createMutation.isPending}
                onClick={() => createMutation.mutate()}
              >
                Create
              </button>
            </div>
          )}
          <table className="jdc-settings-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>URL</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {links.map((l) => (
                <tr key={l.id}>
                  <td>{l.name}</td>
                  <td>
                    <a href={l.url} target="_blank" rel="noopener noreferrer" className="jdc-link">
                      {l.url}
                    </a>
                  </td>
                  <td>{l.status}</td>
                  <td>
                    <button
                      type="button"
                      className="jdc-link jdc-link-btn"
                      onClick={() => {
                        if (window.confirm(`Delete link "${l.name}"?`)) deleteMutation.mutate(l.id);
                      }}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </section>
  );
}
