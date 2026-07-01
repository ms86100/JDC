import { useState } from 'react';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { projectApi, ProjectResponse, ProjectScheme } from '../../../api/projectApi';
import { auditApi } from '../../../api/serviceApi';
import { adminApi } from '../../../api/adminApi';
import { issueApi } from '../../../api/issueApi';
import ProjectWorkflowSchemePanel from '../components/ProjectWorkflowSchemePanel';
import ProjectReindexPanel from '../components/settings/ProjectReindexPanel';
import ProjectLinksPanel from '../components/settings/ProjectLinksPanel';
import VersionsManager from '../components/VersionsManager';
import ComponentsManager from '../components/ComponentsManager';
import '../styles/project-releases-components.css';
import {
  PROJECT_SETTINGS_SECTIONS,
  projectSettingsPath,
  type ProjectSettingsSection,
} from '../projectDcNav';
import '../styles/ProjectSettingsPage.css';

interface LayoutContext {
  project?: ProjectResponse;
}

export default function ProjectSettingsDcLayout() {
  const { projectId, section } = useParams<{ projectId: string; section?: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const ctx = useOutletContext<LayoutContext>();

  const activeSection = (section as ProjectSettingsSection) || 'summary';

  const { data: project, isLoading } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId!).then((r) => r.data),
    enabled: !!projectId,
  });

  const { data: scheme } = useQuery({
    queryKey: ['project-scheme', projectId],
    queryFn: () => projectApi.getProjectScheme(projectId!).then((r) => r.data),
    enabled: !!projectId && (activeSection === 'summary' || activeSection === 'issue-types'),
  });

  if (!projectId) {
    return null;
  }

  if (isLoading || !project) {
    return (
      <div className="ab-loading" style={{ padding: 48 }}>
        <div className="ab-spinner" />
      </div>
    );
  }

  const displayProject = ctx.project ?? project;
  let lastGroup: string | undefined;

  return (
    <div className="jdc-settings-layout">
      <aside className="jdc-settings-sidebar">
        <div style={{ padding: '12px 16px', borderBottom: '1px solid var(--jdc-border)' }}>
          <Link to={`/projects/${projectId}`} className="jdc-link" style={{ fontSize: 12 }}>
            ← {displayProject.name}
          </Link>
          <div style={{ fontWeight: 700, marginTop: 8 }}>Project settings</div>
        </div>
        {PROJECT_SETTINGS_SECTIONS.map((s) => {
          const showGroup = s.group && s.group !== lastGroup;
          if (s.group) lastGroup = s.group;
          return (
            <div key={s.id}>
              {showGroup && (
                <div className="jdc-settings-nav-section">{s.group}</div>
              )}
              <Link
                to={projectSettingsPath(projectId, s.id)}
                className={`jdc-settings-nav-item${activeSection === s.id ? ' active' : ''}`}
              >
                {s.label}
              </Link>
            </div>
          );
        })}
      </aside>
      <main className="jdc-settings-main">
        {activeSection === 'summary' && <SettingsSummary project={displayProject} scheme={scheme} projectId={projectId} />}
        {activeSection === 'details' && <SettingsDetails project={displayProject} projectId={projectId} />}
        {activeSection === 'issue-types' && <SettingsIssueTypes scheme={scheme} projectId={projectId} />}
        {activeSection === 'workflows' && (
          <section>
            <h2 className="jdc-page-title">Workflows</h2>
            <ProjectWorkflowSchemePanel projectId={projectId} />
            <p style={{ marginTop: 16 }}>
              <Link to="/workflows/admin/schemes" className="jdc-link">Workflow scheme administration →</Link>
            </p>
          </section>
        )}
        {activeSection === 'users' && <SettingsUsersRoles projectId={projectId} project={displayProject} />}
        {activeSection === 'components' && <SettingsComponents projectId={projectId} />}
        {activeSection === 'versions' && <SettingsVersions projectId={projectId} />}
        {activeSection === 'permissions' && <SettingsPermissions projectId={projectId} />}
        {activeSection === 'screens' && <SettingsAdminDeepLink title="Screens" href="/workflows/admin/screens" hint="Configure issue screens used on create, view, and edit." />}
        {activeSection === 'fields' && <SettingsAdminDeepLink title="Fields" href="/admin/custom-fields" hint="Manage custom fields and field configurations." />}
        {activeSection === 'priorities' && <SettingsAdminDeepLink title="Priorities" href="/admin/priorities" hint="Set default and available priorities for this project." />}
        {activeSection === 'project-links' && <ProjectLinksPanel />}
        {activeSection === 'reindex' && (
          <ProjectReindexPanel projectId={projectId} projectName={displayProject.name} />
        )}
        {activeSection === 'audit' && <SettingsAudit projectId={projectId} />}
        {!PROJECT_SETTINGS_SECTIONS.some((s) => s.id === activeSection) && (
          <div>
            <p>Unknown settings section.</p>
            <button type="button" className="jdc-btn jdc-btn-primary" onClick={() => navigate(projectSettingsPath(projectId, 'summary'))}>
              Go to Summary
            </button>
          </div>
        )}
      </main>
    </div>
  );
}

function SettingsAdminDeepLink({
  title,
  href,
  hint,
}: {
  title: string;
  href: string;
  hint: string;
}) {
  return (
    <section>
      <h2 className="jdc-page-title">{title}</h2>
      <p className="jdc-muted">{hint}</p>
      <p style={{ marginTop: 16 }}>
        <Link to={href} className="jdc-btn jdc-btn-primary" style={{ display: 'inline-block' }}>
          Open {title} administration
        </Link>
      </p>
    </section>
  );
}

function SettingsSummary({
  project,
  scheme,
  projectId,
}: {
  project: ProjectResponse;
  scheme?: ProjectScheme;
  projectId: string;
}) {
  return (
    <section>
      <h2 className="jdc-page-title">Summary</h2>
      <p className="jdc-muted">Schemes and configuration for {project.name} ({project.projectKey})</p>
      <table className="jdc-settings-table" style={{ marginTop: 16 }}>
        <tbody>
          <tr>
            <th style={{ width: 180 }}>Issue type scheme</th>
            <td>
              {scheme?.issueTypeScheme?.name ?? '—'}
              <Link to={projectSettingsPath(projectId, 'issue-types')} className="jdc-link" style={{ marginLeft: 8 }}>
                Configure
              </Link>
            </td>
          </tr>
          <tr>
            <th>Workflow scheme</th>
            <td>
              {scheme?.workflowScheme?.name ?? '—'}
              <Link to={projectSettingsPath(projectId, 'workflows')} className="jdc-link" style={{ marginLeft: 8 }}>
                Configure
              </Link>
            </td>
          </tr>
          <tr>
            <th>Permission scheme</th>
            <td>{scheme?.permissionScheme?.name ?? '—'}</td>
          </tr>
          <tr>
            <th>Notification scheme</th>
            <td>{scheme?.notificationScheme?.name ?? '—'}</td>
          </tr>
          <tr>
            <th>Screen scheme</th>
            <td>{scheme?.screenScheme?.name ?? '—'}</td>
          </tr>
        </tbody>
      </table>
    </section>
  );
}

function SettingsDetails({ project, projectId }: { project: ProjectResponse; projectId: string }) {
  const queryClient = useQueryClient();
  const [editMode, setEditMode] = useState(false);
  const [form, setForm] = useState({
    name: project.name,
    description: project.description ?? '',
  });

  const updateMutation = useMutation({
    mutationFn: () => projectApi.update(projectId, form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      setEditMode(false);
    },
  });

  return (
    <section>
      <h2 className="jdc-page-title">Details</h2>
      {editMode ? (
        <div className="jdc-card" style={{ padding: 16, maxWidth: 480 }}>
          <div className="jdc-form-row">
            <label className="jdc-label">Name</label>
            <input className="jdc-input" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
          </div>
          <div className="jdc-form-row">
            <label className="jdc-label">Description</label>
            <textarea
              className="jdc-input"
              rows={4}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button type="button" className="jdc-btn jdc-btn-primary" onClick={() => updateMutation.mutate()} disabled={updateMutation.isPending}>
              Save
            </button>
            <button type="button" className="jdc-btn jdc-btn-secondary" onClick={() => setEditMode(false)}>Cancel</button>
          </div>
        </div>
      ) : (
        <div>
          <dl className="jdc-review-dl">
            <div><dt>Name</dt><dd>{project.name}</dd></div>
            <div><dt>Key</dt><dd><code>{project.projectKey}</code></dd></div>
            <div><dt>Lead</dt><dd>{project.leadName ?? '—'}</dd></div>
            <div><dt>Template</dt><dd>{project.template ?? '—'}</dd></div>
            <div><dt>Description</dt><dd>{project.description || '—'}</dd></div>
          </dl>
          <button type="button" className="jdc-btn jdc-btn-secondary" onClick={() => setEditMode(true)}>Edit</button>
        </div>
      )}
    </section>
  );
}

function SettingsIssueTypes({ scheme, projectId }: { scheme?: ProjectScheme; projectId: string }) {
  const { data: types } = useQuery({
    queryKey: ['issue-types'],
    queryFn: () => issueApi.getTypes().then((r) => r.data),
  });

  const schemeTypeIds = scheme?.issueTypeScheme?.issueTypeIds ?? [];

  return (
    <section>
      <h2 className="jdc-page-title">Issue types</h2>
      <p className="jdc-muted">
        Scheme: <strong>{scheme?.issueTypeScheme?.name ?? 'Default'}</strong>
      </p>
      <table className="jdc-settings-table">
        <thead>
          <tr>
            <th>Issue type</th>
            <th>In scheme</th>
            <th>Default</th>
            <th>Configuration</th>
          </tr>
        </thead>
        <tbody>
          {(types ?? []).map((t) => (
            <tr key={t.id}>
              <td>{t.name}</td>
              <td>{schemeTypeIds.includes(t.id) ? 'Yes' : '—'}</td>
              <td>{scheme?.issueTypeScheme?.defaultIssueTypeId === t.id ? 'Default' : ''}</td>
              <td className="jdc-settings-ops">
                <Link to={`/admin/issue-types?typeId=${t.id}`} className="jdc-link">Issue type</Link>
                <span className="jdc-ops-sep">|</span>
                <Link to="/workflows/admin/screens" className="jdc-link">Screens</Link>
                <span className="jdc-ops-sep">|</span>
                <Link to={projectSettingsPath(projectId, 'workflows')} className="jdc-link">Workflow</Link>
                <span className="jdc-ops-sep">|</span>
                <Link to="/admin/custom-fields" className="jdc-link">Fields</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="jdc-settings-admin-links" style={{ marginTop: 16 }}>
        <Link to="/admin/issue-types" className="jdc-link">Issue type administration →</Link>
        {' · '}
        <Link to="/workflows/admin/schemes" className="jdc-link">Workflow schemes →</Link>
        {' · '}
        <Link to="/workflows/admin/screens" className="jdc-link">Screens →</Link>
      </div>
    </section>
  );
}

function SettingsUsersRoles({ projectId, project }: { projectId: string; project: ProjectResponse }) {
  const queryClient = useQueryClient();
  const [userId, setUserId] = useState('');
  const [role, setRole] = useState('Developers');

  const { data: members = [], isLoading } = useQuery({
    queryKey: ['project-members', projectId],
    queryFn: () => projectApi.getMembers(projectId).then((r) => r.data),
  });

  const { data: users = [] } = useQuery({
    queryKey: ['admin-users-picker'],
    queryFn: () => adminApi.getUsers().then((r) => r.data),
  });

  const addMutation = useMutation({
    mutationFn: () => projectApi.addMember(projectId, userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project-members', projectId] });
      setUserId('');
    },
  });

  return (
    <section>
      <h2 className="jdc-page-title">Users and roles</h2>
      <div className="jdc-card" style={{ padding: 16, marginBottom: 16 }}>
        <h3 style={{ marginTop: 0, fontSize: 14 }}>Add users to project</h3>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div>
            <label className="jdc-label">User</label>
            <select className="jdc-input" value={userId} onChange={(e) => setUserId(e.target.value)}>
              <option value="">Select user</option>
              {users.map((u: { id: string; username: string }) => (
                <option key={u.id} value={u.id}>{u.username}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="jdc-label">Role</label>
            <select className="jdc-input" value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="Administrators">Administrators</option>
              <option value="Developers">Developers</option>
              <option value="Users">Users</option>
            </select>
          </div>
          <button
            type="button"
            className="jdc-btn jdc-btn-primary"
            disabled={!userId || addMutation.isPending}
            onClick={() => addMutation.mutate()}
          >
            Add
          </button>
        </div>
      </div>
      {isLoading ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : (
        <table className="jdc-settings-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Role</th>
              <th>Joined</th>
            </tr>
          </thead>
          <tbody>
            {project.leadName && (
              <tr>
                <td>{project.leadName}</td>
                <td>Project Lead</td>
                <td>—</td>
              </tr>
            )}
            {members.map((m) => (
              <tr key={m.id}>
                <td>{m.userName ?? m.userId}</td>
                <td>{m.role}</td>
                <td>{m.joinedAt ? new Date(m.joinedAt).toLocaleDateString() : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function SettingsComponents({ projectId }: { projectId: string }) {
  return (
    <section>
      <h2 className="jdc-page-title">Components</h2>
      <p className="jdc-muted" style={{ marginBottom: 16 }}>
        Project components define logical modules and default assignees (Systems DC project settings).
      </p>
      <ComponentsManager projectId={projectId} variant="settings" />
    </section>
  );
}

function SettingsVersions({ projectId }: { projectId: string }) {
  return (
    <section>
      <h2 className="jdc-page-title">Versions</h2>
      <p className="jdc-muted" style={{ marginBottom: 16 }}>
        Versions track releases and fix-version fields on issues.
      </p>
      <VersionsManager projectId={projectId} variant="settings" />
    </section>
  );
}

function SettingsPermissions({ projectId }: { projectId: string }) {
  return (
    <section>
      <h2 className="jdc-page-title">Permissions</h2>
      <p className="jdc-muted">Permission scheme for project {projectId}</p>
      <Link to="/admin/permissions" className="jdc-btn jdc-btn-primary">
        Open permission administration
      </Link>
    </section>
  );
}

function SettingsAudit({ projectId }: { projectId: string }) {
  const [action, setAction] = useState('');

  const { data: logs = [], isLoading } = useQuery({
    queryKey: ['project-audit', projectId],
    queryFn: async () => {
      try {
        const entityRes = await auditApi.getLogsForEntity('PROJECT', projectId);
        if (entityRes.data?.length) return entityRes.data;
      } catch {
        /* fall through to global log filter */
      }
      const res = await auditApi.getLogs({ page: 0, size: 100 });
      const all = res.data?.content ?? [];
      return all.filter(
        (l) => l.entityId === projectId || l.changes?.projectId === projectId,
      );
    },
    enabled: !!projectId,
  });

  return (
    <section>
      <h2 className="jdc-page-title">Audit log</h2>
      <div style={{ marginBottom: 12 }}>
        <input
          className="jdc-input"
          placeholder="Filter by action (client-side)"
          value={action}
          onChange={(e) => setAction(e.target.value)}
          style={{ maxWidth: 280 }}
        />
      </div>
      {isLoading ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : (
        <table className="jdc-settings-table">
          <thead>
            <tr>
              <th>When</th>
              <th>User</th>
              <th>Action</th>
              <th>Entity</th>
            </tr>
          </thead>
          <tbody>
            {logs
              .filter((l) => !action || (l.action ?? '').toLowerCase().includes(action.toLowerCase()))
              .map((l) => (
                <tr key={l.id}>
                  <td>{l.createdAt ? new Date(l.createdAt).toLocaleString() : '—'}</td>
                  <td>{l.username ?? l.userId ?? '—'}</td>
                  <td>{l.action}</td>
                  <td>{l.entityType} {l.entityId}</td>
                </tr>
              ))}
          </tbody>
        </table>
      )}
      {logs.length === 0 && !isLoading && <p className="jdc-muted">No audit entries for this project.</p>}
    </section>
  );
}
