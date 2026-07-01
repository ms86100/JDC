import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowApi, WorkflowTransitionDetail } from '../../../api/workflowApi';
import { issueApi } from '../../../api/issueApi';
import { TransitionConfigPanel } from '../components/TransitionConfigPanel';
import WorkflowVersionHistoryPanel from '../components/WorkflowVersionHistoryPanel';
import WorkflowStatusMigrationModal from '../components/WorkflowStatusMigrationModal';
import './workflow-management.css';

export default function WorkflowDetailPage() {
  const { workflowId } = useParams<{ workflowId: string }>();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'statuses' | 'transitions' | 'versions'>('transitions');
  const [selectedTransition, setSelectedTransition] = useState<WorkflowTransitionDetail | null>(null);
  const [showAddTransition, setShowAddTransition] = useState(false);
  const [transitionForm, setTransitionForm] = useState({
    name: '',
    fromStatusId: '',
    toStatusId: '',
  });
  const [addStatusId, setAddStatusId] = useState('');
  const [showStatusMigration, setShowStatusMigration] = useState(false);

  const { data: detail, isLoading } = useQuery({
    queryKey: ['workflow-detail', workflowId],
    queryFn: () => workflowApi.getWorkflowDetail(workflowId!).then((r) => r.data),
    enabled: !!workflowId,
  });

  const { data: globalStatuses = [] } = useQuery({
    queryKey: ['issue-statuses'],
    queryFn: () => issueApi.getStatuses().then((r) => r.data),
  });

  const publishMutation = useMutation({
    mutationFn: () => workflowApi.publishWorkflow(workflowId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow-detail', workflowId] }),
  });

  const cloneMutation = useMutation({
    mutationFn: (name: string) => workflowApi.clone(workflowId!, name),
    onSuccess: (res) => {
      window.location.href = `/workflows/${res.data.id}`;
    },
  });

  const addStatusMutation = useMutation({
    mutationFn: () => workflowApi.addStatusToWorkflow(workflowId!, addStatusId),
    onSuccess: () => {
      setAddStatusId('');
      queryClient.invalidateQueries({ queryKey: ['workflow-detail', workflowId] });
    },
  });

  const addTransitionMutation = useMutation({
    mutationFn: () =>
      workflowApi.createTransition({
        workflowId: workflowId!,
        ...transitionForm,
      }),
    onSuccess: () => {
      setShowAddTransition(false);
      setTransitionForm({ name: '', fromStatusId: '', toStatusId: '' });
      queryClient.invalidateQueries({ queryKey: ['workflow-detail', workflowId] });
      queryClient.invalidateQueries({ queryKey: ['workflow-layout', workflowId] });
      queryClient.invalidateQueries({ queryKey: ['workflow-transitions', workflowId] });
    },
  });

  if (isLoading || !detail) {
    return <div className="wf-page"><div className="ab-loading"><div className="ab-spinner" /></div></div>;
  }

  const { workflow, statuses, transitions, versions } = detail;

  return (
    <div className="wf-page wf-detail-page">
      <header className="wf-page-header">
        <div>
          <Link to="/workflows" className="wf-back">← Workflows</Link>
          <h1>{workflow.name}</h1>
          <p className="wf-muted">{workflow.description || 'No description'}</p>
          <div className="wf-badges">
            {workflow.isDraft && <span className="wf-badge wf-badge-draft">Draft</span>}
            {workflow.isActive ? (
              <span className="wf-badge wf-badge-active">Active</span>
            ) : (
              <span className="wf-badge wf-badge-inactive">Inactive</span>
            )}
            <span className="wf-badge">{statuses.length} statuses</span>
            <span className="wf-badge">{transitions.length} transitions</span>
          </div>
        </div>
        <div className="wf-header-actions">
          <button
            type="button"
            className="ab-btn ab-btn-secondary"
            onClick={() => {
              const name = prompt('Clone as:', `${workflow.name} (copy)`);
              if (name) cloneMutation.mutate(name);
            }}
          >
            Clone
          </button>
          <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowStatusMigration(true)}>
            Status migration
          </button>
          <Link to={`/workflows/${workflowId}/designer`} className="ab-btn ab-btn-primary">
            Open designer
          </Link>
          {workflow.isDraft && (
            <button
              type="button"
              className="ab-btn ab-btn-primary"
              disabled={publishMutation.isPending}
              onClick={() => publishMutation.mutate()}
            >
              Publish
            </button>
          )}
        </div>
      </header>

      <nav className="wf-tabs">
        {(['statuses', 'transitions', 'versions'] as const).map((t) => (
          <button
            key={t}
            type="button"
            className={`wf-tab ${tab === t ? 'wf-tab--active' : ''}`}
            onClick={() => setTab(t)}
          >
            {t.charAt(0).toUpperCase() + t.slice(1)}
          </button>
        ))}
      </nav>

      <div className={`wf-detail-body ${selectedTransition ? 'wf-detail-body--split' : ''}`}>
        <div className="wf-detail-main">
          {tab === 'statuses' && (
            <section className="wf-panel">
              <div className="wf-panel-toolbar">
                <h2>Workflow statuses</h2>
                <div className="wf-inline-form">
                  <select
                    className="ab-select"
                    value={addStatusId}
                    onChange={(e) => setAddStatusId(e.target.value)}
                  >
                    <option value="">Add global status…</option>
                    {globalStatuses.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} ({s.category})
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="ab-btn ab-btn-sm ab-btn-primary"
                    disabled={!addStatusId}
                    onClick={() => addStatusMutation.mutate()}
                  >
                    Add status
                  </button>
                </div>
              </div>
              <table className="wf-table">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Category</th>
                    <th>Order</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {statuses.map((s) => (
                    <tr key={s.id}>
                      <td><strong>{s.statusName}</strong></td>
                      <td><span className={`wf-cat wf-cat--${(s.statusCategory ?? 'todo').toLowerCase()}`}>{s.statusCategory}</span></td>
                      <td>{s.sequence}</td>
                      <td>
                        <button
                          type="button"
                          className="ab-btn ab-btn-ghost ab-btn-sm"
                          onClick={() =>
                            workflowApi.removeStatusFromWorkflow(workflowId!, s.id).then(() =>
                              queryClient.invalidateQueries({ queryKey: ['workflow-detail', workflowId] })
                            )
                          }
                        >
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}

          {tab === 'transitions' && (
            <section className="wf-panel">
              <div className="wf-panel-toolbar">
                <h2>Transitions</h2>
                <button type="button" className="ab-btn ab-btn-sm ab-btn-primary" onClick={() => setShowAddTransition(true)}>
                  + Add transition
                </button>
              </div>
              {showAddTransition && (
                <div className="wf-inline-form wf-add-transition">
                  <input
                    className="ab-input"
                    placeholder="Transition name (e.g. Start Progress)"
                    value={transitionForm.name}
                    onChange={(e) => setTransitionForm({ ...transitionForm, name: e.target.value })}
                  />
                  <select
                    className="ab-select"
                    value={transitionForm.fromStatusId}
                    onChange={(e) => setTransitionForm({ ...transitionForm, fromStatusId: e.target.value })}
                  >
                    <option value="">From status</option>
                    {statuses.map((s) => (
                      <option key={s.statusId} value={s.statusId}>{s.statusName}</option>
                    ))}
                  </select>
                  <select
                    className="ab-select"
                    value={transitionForm.toStatusId}
                    onChange={(e) => setTransitionForm({ ...transitionForm, toStatusId: e.target.value })}
                  >
                    <option value="">To status</option>
                    {statuses.map((s) => (
                      <option key={s.statusId} value={s.statusId}>{s.statusName}</option>
                    ))}
                  </select>
                  <button type="button" className="ab-btn ab-btn-primary ab-btn-sm" onClick={() => addTransitionMutation.mutate()}>
                    Save
                  </button>
                  <button type="button" className="ab-btn ab-btn-secondary ab-btn-sm" onClick={() => setShowAddTransition(false)}>
                    Cancel
                  </button>
                </div>
              )}
              <table className="wf-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Rules</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {transitions.map((t) => (
                    <tr
                      key={t.id}
                      className={selectedTransition?.id === t.id ? 'wf-row-selected' : ''}
                      onClick={() => setSelectedTransition(t)}
                    >
                      <td><strong>{t.name}</strong></td>
                      <td>{t.fromStatusName}</td>
                      <td>{t.toStatusName}</td>
                      <td>
                        {(t.conditions?.length ?? 0) + (t.validators?.length ?? 0) + (t.postFunctions?.length ?? 0)} rules
                      </td>
                      <td>
                        <button
                          type="button"
                          className="ab-btn ab-btn-ghost ab-btn-sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            workflowApi.deleteTransition(t.id).then(() => {
                              queryClient.invalidateQueries({ queryKey: ['workflow-detail', workflowId] });
                              queryClient.invalidateQueries({ queryKey: ['workflow-layout', workflowId] });
                              queryClient.invalidateQueries({ queryKey: ['workflow-transitions', workflowId] });
                            });
                          }}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}

          {tab === 'versions' && workflowId && (
            <WorkflowVersionHistoryPanel workflowId={workflowId} versions={versions} />
          )}
        </div>

        {selectedTransition && (
          <TransitionConfigPanel
            transition={selectedTransition}
            onClose={() => setSelectedTransition(null)}
          />
        )}

        {showStatusMigration && workflowId && (
          <WorkflowStatusMigrationModal
            workflowId={workflowId}
            statuses={statuses}
            onClose={() => setShowStatusMigration(false)}
          />
        )}
      </div>
    </div>
  );
}
