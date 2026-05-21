import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowApi, WorkflowTransitionDetail } from '../../../api/workflowApi';

interface Props {
  transition: WorkflowTransitionDetail;
  onClose: () => void;
}

export function TransitionConfigPanel({ transition, onClose }: Props) {
  const queryClient = useQueryClient();
  const [newCondition, setNewCondition] = useState('user_in_group');
  const [newValidator, setNewValidator] = useState('field_required');
  const [newPostFn, setNewPostFn] = useState('update_issue_field');

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['workflow-detail', transition.workflowId] });
    queryClient.invalidateQueries({ queryKey: ['workflow-transitions', transition.workflowId] });
    queryClient.invalidateQueries({ queryKey: ['workflow-layout', transition.workflowId] });
  };

  const addCondition = useMutation({
    mutationFn: () => workflowApi.addCondition(transition.id, { type: newCondition }),
    onSuccess: invalidate,
  });
  const addValidator = useMutation({
    mutationFn: () => workflowApi.addValidator(transition.id, { type: newValidator }),
    onSuccess: invalidate,
  });
  const addPostFn = useMutation({
    mutationFn: () => workflowApi.addPostFunction(transition.id, { type: newPostFn }),
    onSuccess: invalidate,
  });

  return (
    <div className="wf-transition-panel">
      <header className="wf-transition-panel-header">
        <div>
          <h3>{transition.name}</h3>
          <p className="wf-muted">
            {transition.fromStatusName} → {transition.toStatusName}
          </p>
        </div>
        <button type="button" className="ab-btn ab-btn-ghost ab-btn-sm" onClick={onClose}>
          Close
        </button>
      </header>

      <section className="wf-config-section">
        <h4>Conditions</h4>
        <p className="wf-muted">Must pass before transition is available</p>
        <ul className="wf-config-list">
          {(transition.conditions ?? []).map((c) => (
            <li key={c.id}>
              <span>{c.type}</span>
              <button
                type="button"
                className="ab-btn ab-btn-ghost ab-btn-sm"
                onClick={() => workflowApi.deleteCondition(transition.id, c.id).then(invalidate)}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div className="wf-config-add">
          <select value={newCondition} onChange={(e) => setNewCondition(e.target.value)} className="ab-select">
            <option value="user_in_group">User in group</option>
            <option value="assignee_only">Assignee only</option>
            <option value="reporter_only">Reporter only</option>
            <option value="jql">JQL condition</option>
          </select>
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary" onClick={() => addCondition.mutate()}>
            Add
          </button>
        </div>
      </section>

      <section className="wf-config-section">
        <h4>Validators</h4>
        <p className="wf-muted">Checked when user executes transition</p>
        <ul className="wf-config-list">
          {(transition.validators ?? []).map((v) => (
            <li key={v.id}>
              <span>{v.type}</span>
              <button
                type="button"
                className="ab-btn ab-btn-ghost ab-btn-sm"
                onClick={() => workflowApi.deleteValidator(transition.id, v.id).then(invalidate)}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div className="wf-config-add">
          <select value={newValidator} onChange={(e) => setNewValidator(e.target.value)} className="ab-select">
            <option value="field_required">Field required</option>
            <option value="permission">Permission check</option>
            <option value="comment_required">Comment required</option>
          </select>
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary" onClick={() => addValidator.mutate()}>
            Add
          </button>
        </div>
      </section>

      <section className="wf-config-section">
        <h4>Post-functions</h4>
        <p className="wf-muted">Run after successful transition</p>
        <ul className="wf-config-list">
          {(transition.postFunctions ?? []).map((p) => (
            <li key={p.id}>
              <span>{p.type}</span>
              <button
                type="button"
                className="ab-btn ab-btn-ghost ab-btn-sm"
                onClick={() => workflowApi.deletePostFunction(transition.id, p.id).then(invalidate)}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div className="wf-config-add">
          <select value={newPostFn} onChange={(e) => setNewPostFn(e.target.value)} className="ab-select">
            <option value="update_issue_field">Update issue field</option>
            <option value="assign_issue">Assign issue</option>
            <option value="fire_event">Fire event</option>
            <option value="create_comment">Create comment</option>
          </select>
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary" onClick={() => addPostFn.mutate()}>
            Add
          </button>
        </div>
      </section>
    </div>
  );
}
