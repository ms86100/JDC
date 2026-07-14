import React, { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { integrationApi, ApplicationLink } from '../../../api/integrationApi';
import './ApplicationLinksPage.css';
import './AdminReportsInsights.css';
import './IssueTypesPage.css';

export default function ApplicationLinksPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('Confluence');
  const [url, setUrl] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);

  const { data: links, isLoading, isError, refetch } = useQuery({
    queryKey: ['integration', 'applinks'],
    queryFn: () => integrationApi.listApplicationLinks().then((r) => Array.isArray(r.data) ? r.data : []),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      integrationApi.createApplicationLink({
        name,
        url,
        applicationType: 'confluence',
        direction: 'two-way',
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['integration', 'applinks'] });
      setShowCreate(false);
      setUrl('');
      setActionError(null);
    },
    onError: (err: unknown) => setActionError(extractError(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => integrationApi.deleteApplicationLink(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['integration', 'applinks'] }),
    onError: (err: unknown) => setActionError(extractError(err)),
  });

  const primaryMutation = useMutation({
    mutationFn: (id: string) => integrationApi.setPrimary(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['integration', 'applinks'] }),
  });

  const testMutation = useMutation({
    mutationFn: (id: string) => integrationApi.testConnection(id),
    onSuccess: (res) => setActionError(res.data.message),
  });

  return (
    <div className="dc-page ab-applinks-page">
      <header className="ab-analytics-hero">
        <h1>Application links</h1>
        <p>
          Connect Systems and Avionics to Confluence (OAuth 2.0 planned). Links are persisted and
          match Systems DC <strong>Administration → Applications → Application links</strong>.
        </p>
      </header>

      {actionError && (
        <div className="ab-applinks-error" style={{ marginBottom: 16 }}>
          {actionError}
          <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary" onClick={() => setActionError(null)}>
            Dismiss
          </button>
        </div>
      )}

      <div className="ab-applinks-toolbar">
        <button type="button" className="dc-btn dc-btn-secondary" onClick={() => setShowCreate(true)}>
          Create link
        </button>
        <a
          href="https://confluence.atlassian.com/adminjiraserver/configuring-application-links-938846918.html"
          target="_blank"
          rel="noopener noreferrer"
          className="dc-btn dc-btn-sm dc-btn-secondary"
          style={{ textDecoration: 'none' }}
        >
          Systems DC documentation
        </a>
      </div>

      {showCreate && (
        <div className="ab-applinks-create-panel">
          <h2>Create application link</h2>
          <label>
            Application name
            <input type="text" className="it-input" value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <label>
            Remote URL
            <input
              type="url"
              className="it-input"
              placeholder="https://confluence.example.com"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
            />
          </label>
          <div className="ab-applinks-create-actions">
            <button type="button" className="dc-btn dc-btn-secondary" onClick={() => setShowCreate(false)}>
              Cancel
            </button>
            <button
              type="button"
              className="dc-btn dc-btn-secondary"
              disabled={!url.trim() || createMutation.isPending}
              onClick={() => createMutation.mutate()}
            >
              {createMutation.isPending ? 'Creating…' : 'Create link'}
            </button>
          </div>
        </div>
      )}

      {isLoading && <p className="ab-applinks-muted">Loading application links…</p>}
      {isError && (
        <div className="ab-applinks-error">
          <p>Could not reach /api/integration/applinks.</p>
          <button type="button" className="dc-btn dc-btn-secondary" onClick={() => refetch()}>
            Retry
          </button>
        </div>
      )}

      {!isLoading && !isError && (!links || links.length === 0) && (
        <div className="ab-applinks-empty">
          <span className="ab-applinks-empty-icon" aria-hidden="true">
            🔗
          </span>
          <h3>No application links configured</h3>
          <p>Create a link to Confluence to enable wiki integration on issues (OAuth handshake in a later phase).</p>
        </div>
      )}

      {links && links.length > 0 && (
        <div className="ab-recent-table-wrap">
          <table className="ab-recent-table">
            <thead>
              <tr>
                <th>Application</th>
                <th>URL</th>
                <th>Direction</th>
                <th>Status</th>
                <th>Primary</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {links.map((link: ApplicationLink) => (
                <tr key={link.id}>
                  <td>
                    <strong>{link.name}</strong>
                    <div style={{ fontSize: 12, color: '#5e6c84' }}>{link.applicationType}</div>
                  </td>
                  <td>{link.url}</td>
                  <td>{link.direction}</td>
                  <td>
                    <span className={`ab-status-pill ${link.status === 'connected' ? 'ready' : 'scheduled'}`}>
                      {link.status}
                    </span>
                  </td>
                  <td>{link.primary ? 'Yes' : '—'}</td>
                  <td>
                    <div className="ab-ops-list">
                      {!link.primary && (
                        <button type="button" onClick={() => primaryMutation.mutate(link.id)}>
                          Make primary
                        </button>
                      )}
                      <button type="button" onClick={() => testMutation.mutate(link.id)}>
                        Test
                      </button>
                      <button
                        type="button"
                        className="danger"
                        onClick={() => {
                          if (window.confirm(`Delete link to ${link.name}?`)) {
                            deleteMutation.mutate(link.id);
                          }
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function extractError(err: unknown): string {
  return (
    (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
    (err instanceof Error ? err.message : 'Request failed')
  );
}
