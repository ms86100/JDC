import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowApi, Workflow, WorkflowScheme } from '../../../api/workflowApi';
import { issueApi } from '../../../api/issueApi';
import WorkflowSchemeBulkAssignPanel from '../components/WorkflowSchemeBulkAssignPanel';
import WorkflowDcTableView from '../components/WorkflowDcTableView';
import './workflow-management.css';

type HubTab = 'workflows' | 'schemes' | 'guide';

const TEMPLATES = [
  { name: 'Scrum Software Workflow', description: 'Backlog → In Progress → Done with sprint transitions' },
  { name: 'Kanban Flow', description: 'Continuous flow with WIP-friendly columns' },
  { name: 'Bug Tracking Workflow', description: 'Open → In Review → Resolved → Closed' },
  { name: 'Task Management', description: 'Simple To Do → In Progress → Done' },
];

export default function WorkflowManagementPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<HubTab>('workflows');
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ name: '', description: '' });
  const [schemeForm, setSchemeForm] = useState({ name: '', description: '' });
  const [selectedScheme, setSelectedScheme] = useState<WorkflowScheme | null>(null);
  const [mappingForm, setMappingForm] = useState({ issueTypeId: '', workflowId: '' });

  const { data: workflows = [], isLoading } = useQuery({
    queryKey: ['workflows'],
    queryFn: () => workflowApi.getAll().then((r) => r.data),
  });

  const { data: schemes = [] } = useQuery({
    queryKey: ['workflow-schemes'],
    queryFn: () => workflowApi.getSchemes().then((r) => r.data),
    enabled: tab === 'schemes',
  });

  const { data: issueTypes = [] } = useQuery({
    queryKey: ['issue-types'],
    queryFn: () => issueApi.getTypes().then((r) => r.data),
    enabled: tab === 'schemes',
  });

  const createWorkflow = useMutation({
    mutationFn: () => workflowApi.create(form),
    onSuccess: () => {
      setShowCreate(false);
      setForm({ name: '', description: '' });
      queryClient.invalidateQueries({ queryKey: ['workflows'] });
    },
  });

  const createScheme = useMutation({
    mutationFn: () => workflowApi.createScheme(schemeForm),
    onSuccess: () => {
      setSchemeForm({ name: '', description: '' });
      queryClient.invalidateQueries({ queryKey: ['workflow-schemes'] });
    },
  });

  const addMapping = useMutation({
    mutationFn: () => workflowApi.addSchemeMapping(selectedScheme!.id, mappingForm),
    onSuccess: async () => {
      setMappingForm({ issueTypeId: '', workflowId: '' });
      const res = await workflowApi.getScheme(selectedScheme!.id);
      setSelectedScheme(res.data);
      queryClient.invalidateQueries({ queryKey: ['workflow-schemes'] });
    },
  });

  const filtered = workflows.filter((w) =>
    w.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="wf-page">
      <header className="wf-page-header">
        <div>
          <h1>Workflow management</h1>
          <p className="wf-muted">
            Create, version, and publish workflows. Map them to issue types via workflow schemes — Jira Data Center style.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Link to="/workflows/screens" className="ab-btn ab-btn-secondary">
            Transition screens
          </Link>
          <Link to="/workflows/admin" className="ab-btn ab-btn-secondary">
            Administration
          </Link>
          <button type="button" className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
            + Create workflow
          </button>
        </div>
      </header>

      <nav className="wf-tabs">
        {(['workflows', 'schemes', 'guide'] as const).map((t) => (
          <button
            key={t}
            type="button"
            className={`wf-tab ${tab === t ? 'wf-tab--active' : ''}`}
            onClick={() => setTab(t)}
          >
            {t === 'workflows' ? 'Workflows' : t === 'schemes' ? 'Workflow schemes' : 'Architecture'}
          </button>
        ))}
      </nav>

      {tab === 'workflows' && (
        <>
          <div className="wf-toolbar">
            <input
              type="search"
              className="ab-input wf-search"
              placeholder="Search workflows…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {showCreate && (
            <div className="wf-panel wf-create-panel">
              <h3>Create workflow</h3>
              <input
                className="ab-input"
                placeholder="Workflow name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
              <textarea
                className="ab-textarea"
                placeholder="Description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                rows={2}
              />
              <div className="wf-template-row">
                <span className="wf-muted">Quick start:</span>
                {TEMPLATES.map((t) => (
                  <button
                    key={t.name}
                    type="button"
                    className="wf-template-chip"
                    onClick={() => setForm({ name: t.name, description: t.description })}
                  >
                    {t.name}
                  </button>
                ))}
              </div>
              <div className="wf-form-actions">
                <button type="button" className="ab-btn ab-btn-primary" onClick={() => createWorkflow.mutate()}>
                  Create
                </button>
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowCreate(false)}>
                  Cancel
                </button>
              </div>
            </div>
          )}

          {isLoading ? (
            <div className="ab-loading"><div className="ab-spinner" /></div>
          ) : (
            <WorkflowDcTableView
              workflows={filtered}
              schemes={schemes}
              search={search}
              mode="workflows"
            />
          )}
        </>
      )}

      {tab === 'schemes' && (
        <>
          <div className="wf-toolbar" style={{ marginBottom: 12 }}>
            <input
              type="search"
              className="ab-input wf-search"
              placeholder="Search schemes…"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <WorkflowDcTableView
            workflows={workflows}
            schemes={schemes}
            search={search}
            mode="schemes"
          />
        <div className="wf-schemes-layout" style={{ marginTop: 16 }}>
          <aside className="wf-schemes-list">
            <div className="wf-panel-toolbar">
              <h2>Schemes</h2>
            </div>
            <div className="wf-inline-form wf-scheme-create">
              <input
                className="ab-input"
                placeholder="New scheme name"
                value={schemeForm.name}
                onChange={(e) => setSchemeForm({ ...schemeForm, name: e.target.value })}
              />
              <button
                type="button"
                className="ab-btn ab-btn-sm ab-btn-primary"
                disabled={!schemeForm.name}
                onClick={() => createScheme.mutate()}
              >
                Add
              </button>
            </div>
            <ul>
              {schemes.map((s) => (
                <li key={s.id}>
                  <button
                    type="button"
                    className={`wf-scheme-item ${selectedScheme?.id === s.id ? 'wf-scheme-item--active' : ''}`}
                    onClick={async () => {
                      const res = await workflowApi.getScheme(s.id);
                      setSelectedScheme(res.data);
                    }}
                  >
                    <strong>{s.name}</strong>
                    {s.isDraft && <span className="wf-badge wf-badge-draft">Draft</span>}
                    <span className="wf-muted">{s.mappings?.length ?? 0} mappings</span>
                  </button>
                </li>
              ))}
            </ul>
          </aside>

          <section className="wf-panel wf-scheme-detail">
            {selectedScheme ? (
              <>
                <header className="wf-panel-toolbar">
                  <div>
                    <h2>{selectedScheme.name}</h2>
                    <p className="wf-muted">{selectedScheme.description}</p>
                  </div>
                  <div style={{ display: 'flex', gap: 8 }}>
                    {!selectedScheme.isDraft && (
                      <button
                        type="button"
                        className="ab-btn ab-btn-secondary ab-btn-sm"
                        onClick={() =>
                          workflowApi.createSchemeDraft(selectedScheme.id).then(async () => {
                            queryClient.invalidateQueries({ queryKey: ['workflow-schemes'] });
                            const res = await workflowApi.getScheme(selectedScheme.id);
                            setSelectedScheme(res.data);
                          })
                        }
                      >
                        Edit (create draft)
                      </button>
                    )}
                    {selectedScheme.isDraft && (
                      <button
                        type="button"
                        className="ab-btn ab-btn-primary ab-btn-sm"
                        onClick={() =>
                          workflowApi.publishScheme(selectedScheme.id).then(async () => {
                            queryClient.invalidateQueries({ queryKey: ['workflow-schemes'] });
                            const res = await workflowApi.getScheme(selectedScheme.id);
                            setSelectedScheme(res.data);
                          })
                        }
                      >
                        Publish scheme
                      </button>
                    )}
                  </div>
                </header>
                <p className="wf-muted wf-scheme-hint">
                  Map each issue type to a workflow. Projects assign this scheme to control how issues move through statuses.
                </p>
                <div className="wf-inline-form">
                  <select
                    className="ab-select"
                    value={mappingForm.issueTypeId}
                    onChange={(e) => setMappingForm({ ...mappingForm, issueTypeId: e.target.value })}
                  >
                    <option value="">Issue type</option>
                    {issueTypes.map((t) => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                  </select>
                  <select
                    className="ab-select"
                    value={mappingForm.workflowId}
                    onChange={(e) => setMappingForm({ ...mappingForm, workflowId: e.target.value })}
                  >
                    <option value="">Workflow</option>
                    {workflows.map((w) => (
                      <option key={w.id} value={w.id}>{w.name}</option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="ab-btn ab-btn-sm ab-btn-primary"
                    disabled={!mappingForm.issueTypeId || !mappingForm.workflowId}
                    onClick={() => addMapping.mutate()}
                  >
                    Add mapping
                  </button>
                </div>
                <table className="wf-table">
                  <thead>
                    <tr>
                      <th>Issue type</th>
                      <th>Workflow</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {(selectedScheme.mappings ?? []).map((m) => (
                      <tr key={m.id}>
                        <td>{m.issueTypeName ?? m.issueTypeId}</td>
                        <td>{m.workflowName ?? m.workflowId}</td>
                        <td>
                          <button
                            type="button"
                            className="ab-btn ab-btn-ghost ab-btn-sm"
                            onClick={() =>
                              workflowApi.removeSchemeMapping(selectedScheme.id, m.id).then(async () => {
                                const res = await workflowApi.getScheme(selectedScheme.id);
                                setSelectedScheme(res.data);
                              })
                            }
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <WorkflowSchemeBulkAssignPanel
                  schemeId={selectedScheme.id}
                  schemeName={selectedScheme.name}
                />
              </>
            ) : (
              <p className="wf-muted">Select a scheme to edit issue type → workflow mappings.</p>
            )}
          </section>
        </div>
        </>
      )}

      {tab === 'guide' && (
        <section className="wf-panel wf-architecture">
          <h2>How workflows connect to your projects</h2>
          <ol className="wf-arch-list">
            <li>
              <strong>Workflow</strong> — Reusable definition of statuses and transitions (stored in workflow-service DB).
            </li>
            <li>
              <strong>Workflow scheme</strong> — Maps issue types to workflows (e.g. Bug → Bug Workflow, Story → Scrum).
            </li>
            <li>
              <strong>Project assignment</strong> — Each project gets a scheme via project settings or template.
            </li>
            <li>
              <strong>Runtime</strong> — Issues execute transitions through the workflow engine; boards reflect status columns.
            </li>
          </ol>
          <p className="wf-muted">
            Use the visual designer to drag statuses, publish drafts, and configure transition conditions, validators, and post-functions per Jira DC.
          </p>
        </section>
      )}
    </div>
  );
}
