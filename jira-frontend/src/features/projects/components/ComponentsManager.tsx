import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  componentApi,
  ComponentResponse,
  CreateComponentRequest,
  UpdateComponentRequest,
  ASSIGNEE_TYPES,
} from '../../../api/componentApi';
import { adminApi } from '../../../api/adminApi';
import { appNotify } from '../../../lib/appNotify';
import ProjectLoadError from './ProjectLoadError';
import '../styles/project-releases-components.css';

interface ComponentFormState {
  name: string;
  description: string;
  leadUserId: string;
  assigneeType: string;
  defaultAssignee: string;
}

const emptyForm = (): ComponentFormState => ({
  name: '',
  description: '',
  leadUserId: '',
  assigneeType: 'PROJECT_DEFAULT',
  defaultAssignee: '',
});

function assigneeLabel(type: string): string {
  return ASSIGNEE_TYPES.find((t) => t.value === type)?.label ?? type;
}

interface ComponentsManagerProps {
  projectId: string;
  variant?: 'hub' | 'settings';
}

export default function ComponentsManager({
  projectId,
  variant = 'hub',
}: ComponentsManagerProps) {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [showArchived, setShowArchived] = useState(false);
  const [modal, setModal] = useState<'create' | 'edit' | null>(null);
  const [editing, setEditing] = useState<ComponentResponse | null>(null);
  const [form, setForm] = useState<ComponentFormState>(emptyForm());

  const { data: components = [], isPending, isError, refetch } = useQuery({
    queryKey: ['project-components', projectId, showArchived],
    queryFn: () => componentApi.getByProject(projectId, showArchived),
    enabled: !!projectId,
    retry: 1,
  });

  const { data: users = [] } = useQuery({
    queryKey: ['admin-users-component-lead'],
    queryFn: () => adminApi.getUsers().then((r) => r.data),
    staleTime: 60_000,
  });

  const userLabel = (userId?: string) => {
    if (!userId) return '—';
    const u = users.find((x) => x.id === userId);
    return u ? (u.displayName || u.username) : userId.slice(0, 8) + '…';
  };

  const filtered = useMemo(() => {
    let list = components.filter((c) => !c.archived || showArchived);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (c) =>
          c.name.toLowerCase().includes(q) ||
          (c.description ?? '').toLowerCase().includes(q),
      );
    }
    return [...list].sort((a, b) => a.name.localeCompare(b.name));
  }, [components, search, showArchived]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['project-components', projectId] });
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      const lead = form.leadUserId.trim() || undefined;
      const defAssignee = form.defaultAssignee.trim() || undefined;
      if (modal === 'create') {
        const body: CreateComponentRequest = {
          projectId,
          name: form.name.trim(),
          description: form.description.trim() || undefined,
          leadUserId: lead,
          assigneeType: form.assigneeType,
          defaultAssignee: form.assigneeType === 'SPECIFIC_USER' ? defAssignee : undefined,
        };
        return componentApi.create(body);
      }
      if (editing) {
        const body: UpdateComponentRequest = {
          name: form.name.trim(),
          description: form.description.trim() || undefined,
          leadUserId: lead,
          assigneeType: form.assigneeType,
          defaultAssignee: form.assigneeType === 'SPECIFIC_USER' ? defAssignee : undefined,
        };
        return componentApi.update(editing.id, body);
      }
      throw new Error('No component to save');
    },
    onSuccess: () => {
      appNotify.success(modal === 'create' ? 'Component created' : 'Component updated');
      setModal(null);
      setEditing(null);
      setForm(emptyForm());
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to save component'),
  });

  const archiveMutation = useMutation({
    mutationFn: (id: string) => componentApi.archive(id),
    onSuccess: () => {
      appNotify.success('Component archived');
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to archive component'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => componentApi.delete(id),
    onSuccess: () => {
      appNotify.success('Component deleted');
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to delete component'),
  });

  const unarchiveMutation = useMutation({
    mutationFn: (id: string) => componentApi.unarchive(id),
    onSuccess: () => {
      appNotify.success('Component unarchived');
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to unarchive component'),
  });

  const openCreate = () => {
    setForm(emptyForm());
    setEditing(null);
    setModal('create');
  };

  const openEdit = (c: ComponentResponse) => {
    setEditing(c);
    setForm({
      name: c.name,
      description: c.description ?? '',
      leadUserId: c.leadUserId ?? '',
      assigneeType: c.assigneeType || 'PROJECT_DEFAULT',
      defaultAssignee: c.defaultAssignee ?? '',
    });
    setModal('edit');
  };

  if (isPending) {
    return <div className="ab-loading"><div className="ab-spinner" /></div>;
  }

  if (isError) {
    return (
      <ProjectLoadError
        title="Components could not be loaded"
        message="Component service is unavailable. Please check the backend services and retry."
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <>
      <div className="jdc-rc-toolbar">
        <input
          type="search"
          className="jdc-input"
          placeholder="Search components…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Search components"
        />
        <label className="jdc-muted" style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
          <input
            type="checkbox"
            checked={showArchived}
            onChange={(e) => setShowArchived(e.target.checked)}
          />
          Include archived
        </label>
        <button type="button" className="jdc-btn jdc-btn-primary" onClick={openCreate}>
          Create component
        </button>
      </div>

      {filtered.length === 0 ? (
        <div className="sa-project-subpage-empty">
          <p>
            {components.length === 0
              ? 'No components yet. Components group issues and define default assignees (Systems DC parity).'
              : 'No components match your search.'}
          </p>
          {components.length === 0 && (
            <button type="button" className="jdc-btn jdc-btn-primary" onClick={openCreate}>
              Create component
            </button>
          )}
        </div>
      ) : (
        <div className="sa-project-subpage-table-wrap jdc-rc-table">
          <table className="jdc-settings-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Lead</th>
                <th>Default assignee</th>
                <th>Issues</th>
                <th>Description</th>
                <th style={{ width: 200 }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id}>
                  <td>
                    <strong>{c.name}</strong>
                    {c.archived && (
                      <span className="jdc-rc-status jdc-rc-status--archived" style={{ marginLeft: 8 }}>
                        Archived
                      </span>
                    )}
                  </td>
                  <td>{userLabel(c.leadUserId)}</td>
                  <td>{assigneeLabel(c.assigneeType)}</td>
                  <td>
                    {c.openIssueCount != null ? `${c.openIssueCount} open` : '—'}
                    {c.issueCount != null && (
                      <span className="jdc-muted" style={{ fontSize: 11 }}>
                        {' '}
                        / {c.issueCount} total
                      </span>
                    )}
                  </td>
                  <td style={{ maxWidth: 280 }}>{c.description ?? '—'}</td>
                  <td>
                      <div className="jdc-rc-actions">
                        <button
                          type="button"
                          className="jdc-btn jdc-btn-secondary jdc-btn-sm"
                          onClick={() => openEdit(c)}
                        >
                          Edit
                        </button>
                        {c.archived ? (
                          <button
                            type="button"
                            className="jdc-btn jdc-btn-secondary jdc-btn-sm"
                            onClick={() => unarchiveMutation.mutate(c.id)}
                          >
                            Unarchive
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="jdc-btn jdc-btn-secondary jdc-btn-sm"
                            onClick={() => archiveMutation.mutate(c.id)}
                          >
                            Archive
                          </button>
                        )}
                        <button
                          type="button"
                          className="jdc-btn jdc-btn-secondary jdc-btn-sm"
                          onClick={() => {
                            if (window.confirm(`Delete component "${c.name}"?`)) {
                              deleteMutation.mutate(c.id);
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

      {modal && (
        <div className="jdc-rc-modal-overlay" role="dialog" aria-modal="true">
          <div className="jdc-rc-modal">
            <div className="jdc-rc-modal-head">
              <h2>{modal === 'create' ? 'Create component' : 'Edit component'}</h2>
            </div>
            <div className="jdc-rc-modal-body">
              <div className="jdc-rc-form-row">
                <label htmlFor="comp-name">Name *</label>
                <input
                  id="comp-name"
                  className="jdc-input"
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                />
              </div>
              <div className="jdc-rc-form-row">
                <label htmlFor="comp-desc">Description</label>
                <textarea
                  id="comp-desc"
                  className="jdc-input"
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                />
              </div>
              <div className="jdc-rc-form-row">
                <label htmlFor="comp-lead">Component lead</label>
                <select
                  id="comp-lead"
                  className="jdc-input"
                  value={form.leadUserId}
                  onChange={(e) => setForm((f) => ({ ...f, leadUserId: e.target.value }))}
                >
                  <option value="">No lead</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.displayName || u.username}
                    </option>
                  ))}
                </select>
              </div>
              <div className="jdc-rc-form-row">
                <label htmlFor="comp-assignee">Default assignee</label>
                <select
                  id="comp-assignee"
                  className="jdc-input"
                  value={form.assigneeType}
                  onChange={(e) => setForm((f) => ({ ...f, assigneeType: e.target.value }))}
                >
                  {ASSIGNEE_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>
              </div>
              {form.assigneeType === 'SPECIFIC_USER' && (
                <div className="jdc-rc-form-row">
                  <label htmlFor="comp-def">Default assignee</label>
                  <select
                    id="comp-def"
                    className="jdc-input"
                    value={form.defaultAssignee}
                    onChange={(e) => setForm((f) => ({ ...f, defaultAssignee: e.target.value }))}
                  >
                    <option value="">Select user…</option>
                    {users.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.displayName || u.username}
                      </option>
                    ))}
                  </select>
                </div>
              )}
            </div>
            <div className="jdc-rc-modal-foot">
              <button
                type="button"
                className="jdc-btn jdc-btn-secondary"
                onClick={() => {
                  setModal(null);
                  setEditing(null);
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="jdc-btn jdc-btn-primary"
                disabled={!form.name.trim() || saveMutation.isPending}
                onClick={() => saveMutation.mutate()}
              >
                {saveMutation.isPending ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
