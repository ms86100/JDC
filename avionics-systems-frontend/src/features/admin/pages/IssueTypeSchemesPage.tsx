import React, { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import {
  IssueTypeScheme,
  SchemeProjectAssignment,
  issueTypeSchemeApi,
} from '../../../api/issueAdminApi';
import './AdminIssueConfig.css';
import './IssueTypesPage.css';

interface IssueType {
  id: string;
  name: string;
  issueTypeKey: string;
  isSubtask: boolean;
}

const issueTypeApi = {
  list: () => apiClient.get<IssueType[]>('/admin/issues/issue-types'),
};

export default function IssueTypeSchemesPage() {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState('');
  const [configureId, setConfigureId] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);

  const [selectedTypeIds, setSelectedTypeIds] = useState<string[]>([]);
  const [defaultTypeId, setDefaultTypeId] = useState<string>('');
  const [selectedProjectIds, setSelectedProjectIds] = useState<string[]>([]);
  const [projectSearch, setProjectSearch] = useState('');

  const { data: schemes, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin', 'issueTypeSchemes'],
    queryFn: () => issueTypeSchemeApi.list().then((r) => Array.isArray(r.data) ? r.data : []),
  });

  const { data: issueTypes } = useQuery({
    queryKey: ['admin', 'issueTypes'],
    queryFn: () => issueTypeApi.list().then((r) => Array.isArray(r.data) ? r.data : []),
  });

  const configuringScheme = useMemo(
    () => schemes?.find((s) => s.id === configureId) ?? null,
    [schemes, configureId]
  );

  const filtered =
    schemes?.filter(
      (s) =>
        s.name.toLowerCase().includes(search.toLowerCase()) ||
        (s.description ?? '').toLowerCase().includes(search.toLowerCase())
    ) ?? [];

  const createMutation = useMutation({
    mutationFn: () =>
      issueTypeSchemeApi.create({
        name: newName,
        description: newDescription,
        issueTypeIds: [],
      }),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypeSchemes'] });
      setShowCreate(false);
      setNewName('');
      setNewDescription('');
      openConfigure(res.data);
    },
    onError: (err: unknown) => setActionError(extractError(err)),
  });

  const saveMutation = useMutation({
    mutationFn: () =>
      issueTypeSchemeApi.update(configureId!, {
        issueTypeIds: selectedTypeIds,
        defaultIssueType: defaultTypeId || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypeSchemes'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'schemeProjects', configureId] });
      setActionError(null);
    },
    onError: (err: unknown) => setActionError(extractError(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => issueTypeSchemeApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypeSchemes'] });
      setConfigureId(null);
      setActionError(null);
    },
    onError: (err: unknown) => setActionError(extractError(err)),
  });

  const { data: schemeProjects, isLoading: projectsLoading } = useQuery({
    queryKey: ['admin', 'schemeProjects', configureId],
    queryFn: () => issueTypeSchemeApi.listProjects(configureId!).then((r) => Array.isArray(r.data) ? r.data : []),
    enabled: !!configureId,
  });

  const assignProjectsMutation = useMutation({
    mutationFn: () => issueTypeSchemeApi.assignProjects(configureId!, selectedProjectIds),
    onSuccess: (res) => {
      queryClient.setQueryData(['admin', 'schemeProjects', configureId], res.data);
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypeSchemes'] });
      setActionError(null);
    },
    onError: (err: unknown) => setActionError(extractError(err)),
  });

  const filteredProjects = useMemo(() => {
    const list = schemeProjects ?? [];
    if (!projectSearch.trim()) return list;
    const q = projectSearch.toLowerCase();
    return list.filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        p.projectKey.toLowerCase().includes(q)
    );
  }, [schemeProjects, projectSearch]);

  function openConfigure(scheme: IssueTypeScheme) {
    setConfigureId(scheme.id);
    setSelectedTypeIds(scheme.issueTypeIdList ?? []);
    setDefaultTypeId(scheme.defaultIssueType ?? scheme.issueTypeIdList?.[0] ?? '');
    setActionError(null);
    setSearchParams({ schemeId: scheme.id }, { replace: true });
  }

  useEffect(() => {
    const schemeId = searchParams.get('schemeId');
    if (!schemeId || !schemes?.length) return;
    if (configureId === schemeId) return;
    const scheme = schemes.find((s) => s.id === schemeId);
    if (scheme) {
      openConfigure(scheme);
    }
  }, [searchParams, schemes, configureId]);

  useEffect(() => {
    if (!schemeProjects) return;
    setSelectedProjectIds(schemeProjects.filter((p) => p.assigned).map((p) => p.id));
  }, [schemeProjects]);

  function toggleProject(id: string) {
    setSelectedProjectIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  }

  function toggleType(id: string) {
    setSelectedTypeIds((prev) => {
      const next = prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id];
      if (!next.includes(defaultTypeId)) {
        setDefaultTypeId(next[0] ?? '');
      }
      return next;
    });
  }

  return (
    <div className="dc-page ab-issue-config-page">
      <header className="dc-page-header">
        <h1 className="dc-page-title">Issue type schemes</h1>
        <p className="dc-page-subtitle">
          Control which issue types each project can use. Matches Systems DC{' '}
          <strong>Manage issue type schemes</strong>. Types are defined under{' '}
          <Link to="/admin/issue-types">Issue types</Link>.
        </p>
      </header>

      {actionError && (
        <div className="ab-issue-config-error" style={{ marginBottom: 16 }}>
          {actionError}
          <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary" onClick={() => setActionError(null)}>
            Dismiss
          </button>
        </div>
      )}

      <div className="ab-issue-config-toolbar">
        <input
          type="search"
          className="admin-search-input-toolbar"
          placeholder="Search schemes…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button type="button" className="dc-btn dc-btn-secondary" onClick={() => setShowCreate(true)}>
          Add issue type scheme
        </button>
      </div>

      {showCreate && (
        <div className="ab-scheme-configure-panel" style={{ marginBottom: 20 }}>
          <h2>New scheme</h2>
          <label style={{ display: 'block', marginTop: 12 }}>
            Name
            <input
              className="it-input"
              style={{ display: 'block', width: '100%', maxWidth: 400, marginTop: 4 }}
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
            />
          </label>
          <label style={{ display: 'block', marginTop: 12 }}>
            Description
            <textarea
              className="it-textarea"
              style={{ display: 'block', width: '100%', maxWidth: 400, marginTop: 4 }}
              rows={2}
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
            />
          </label>
          <div className="ab-scheme-configure-actions">
            <button type="button" className="dc-btn dc-btn-secondary" onClick={() => setShowCreate(false)}>
              Cancel
            </button>
            <button
              type="button"
              className="dc-btn dc-btn-secondary"
              disabled={!newName.trim() || createMutation.isPending}
              onClick={() => createMutation.mutate()}
            >
              Create & configure
            </button>
          </div>
        </div>
      )}

      {isLoading && <p className="ab-issue-config-muted">Loading schemes…</p>}
      {isError && (
        <div className="ab-issue-config-error">
          <p>Could not load schemes.</p>
          <button type="button" className="dc-btn dc-btn-secondary" onClick={() => refetch()}>
            Retry
          </button>
        </div>
      )}

      {!isLoading && !isError && (
        <div className="ab-recent-table-wrap">
          <table className="ab-recent-table ab-issue-config-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Issue types</th>
                <th>Projects</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={4} className="ab-issue-config-empty-cell">
                    {search ? 'No schemes match.' : 'No schemes yet — add one above.'}
                  </td>
                </tr>
              ) : (
                filtered.map((scheme) => (
                  <tr key={scheme.id}>
                    <td>
                      <strong>{scheme.name}</strong>
                      {scheme.description && (
                        <div className="ab-issue-config-desc">{scheme.description}</div>
                      )}
                    </td>
                    <td>{scheme.issueTypeIdList?.length ?? 0}</td>
                    <td>{scheme.projectCount ?? 0}</td>
                    <td>
                      <div className="ab-ops-list">
                        <button type="button" onClick={() => openConfigure(scheme)}>
                          Configure
                        </button>
                        <button
                          type="button"
                          className="danger"
                          onClick={() => {
                            if (window.confirm(`Delete scheme "${scheme.name}"?`)) {
                              deleteMutation.mutate(scheme.id);
                            }
                          }}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {configureId && configuringScheme && (
        <section className="ab-scheme-configure-panel">
          <h2>Configure: {configuringScheme.name}</h2>
          <p className="ab-issue-config-muted">
            Select issue types included in this scheme and pick the default type for new issues.
          </p>

          <div className="ab-scheme-type-list">
            {(issueTypes ?? []).map((it) => {
              const checked = selectedTypeIds.includes(it.id);
              const isDefault = defaultTypeId === it.id;
              return (
                <label
                  key={it.id}
                  className={`ab-scheme-type-row${isDefault ? ' is-default' : ''}`}
                >
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={() => toggleType(it.id)}
                  />
                  <span>
                    <strong>{it.name}</strong>
                    <span style={{ marginLeft: 8, fontSize: 12, color: '#5e6c84' }}>
                      {it.issueTypeKey}
                      {it.isSubtask ? ' · Sub-task' : ''}
                    </span>
                  </span>
                  {checked && (
                    <button
                      type="button"
                      className="dc-btn dc-btn-sm dc-btn-secondary"
                      style={{ marginLeft: 'auto' }}
                      onClick={(e) => {
                        e.preventDefault();
                        setDefaultTypeId(it.id);
                      }}
                    >
                      {isDefault ? 'Default' : 'Set default'}
                    </button>
                  )}
                </label>
              );
            })}
          </div>

          <h3 className="ab-scheme-section-title">Associated projects</h3>
          <p className="ab-issue-config-muted" style={{ marginBottom: 12 }}>
            Choose which projects use this scheme. Changes sync to project-service for runtime issue creation.
          </p>
          <input
            type="search"
            className="admin-search-input-toolbar"
            placeholder="Filter projects…"
            value={projectSearch}
            onChange={(e) => setProjectSearch(e.target.value)}
            style={{ marginBottom: 12, maxWidth: 320 }}
          />
          {projectsLoading && <p className="ab-issue-config-muted">Loading projects…</p>}
          {!projectsLoading && filteredProjects.length === 0 && (
            <p className="ab-issue-config-muted">
              No projects found. Create a project first, then return here to associate this scheme.
            </p>
          )}
          {!projectsLoading && filteredProjects.length > 0 && (
            <div className="ab-scheme-project-list">
              {filteredProjects.map((p: SchemeProjectAssignment) => (
                <label key={p.id} className="ab-scheme-type-row">
                  <input
                    type="checkbox"
                    checked={selectedProjectIds.includes(p.id)}
                    onChange={() => toggleProject(p.id)}
                  />
                  <span>
                    <strong>{p.name}</strong>
                    <span style={{ marginLeft: 8, fontSize: 12, color: '#5e6c84' }}>
                      {p.projectKey}
                      {p.currentSchemeName &&
                        p.currentSchemeId &&
                        p.currentSchemeId !== configureId && (
                          <span> · was: {p.currentSchemeName}</span>
                        )}
                    </span>
                  </span>
                </label>
              ))}
            </div>
          )}

          <div className="ab-scheme-configure-actions">
            <button
              type="button"
              className="dc-btn dc-btn-secondary"
              onClick={() => {
                setConfigureId(null);
                setSearchParams({}, { replace: true });
              }}
            >
              Close
            </button>
            <button
              type="button"
              className="dc-btn dc-btn-secondary"
              disabled={assignProjectsMutation.isPending}
              onClick={() => assignProjectsMutation.mutate()}
            >
              {assignProjectsMutation.isPending ? 'Saving projects…' : 'Save project associations'}
            </button>
            <button
              type="button"
              className="dc-btn dc-btn-secondary"
              disabled={saveMutation.isPending || selectedTypeIds.length === 0}
              onClick={() => saveMutation.mutate()}
            >
              {saveMutation.isPending ? 'Saving…' : 'Save issue types'}
            </button>
          </div>
        </section>
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
