import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowApi, WorkflowTransitionDetail, type WorkflowDefinition } from '../../../api/workflowApi';

interface Props {
  transition: WorkflowTransitionDetail;
  onClose: () => void;
}

export function TransitionConfigPanel({ transition, onClose }: Props) {
  const queryClient = useQueryClient();
  const [newCondition, setNewCondition] = useState('');
  const [newValidator, setNewValidator] = useState('');
  const [newPostFn, setNewPostFn] = useState('');

  const { data: conditionDefs = [] } = useQuery({
    queryKey: ['wf-condition-definitions'],
    queryFn: () => workflowApi.getConditionDefinitions().then((r) => Array.isArray(r.data) ? r.data : []),
  });
  const { data: validatorDefs = [] } = useQuery({
    queryKey: ['wf-validator-definitions'],
    queryFn: () => workflowApi.getValidatorDefinitions().then((r) => Array.isArray(r.data) ? r.data : []),
  });
  const { data: postFnDefs = [] } = useQuery({
    queryKey: ['wf-postfn-definitions'],
    queryFn: () => workflowApi.getPostFunctionDefinitions().then((r) => Array.isArray(r.data) ? r.data : []),
  });

  useEffect(() => {
    if (!newCondition && conditionDefs.length) setNewCondition(conditionDefs[0].type);
  }, [conditionDefs, newCondition]);
  useEffect(() => {
    if (!newValidator && validatorDefs.length) setNewValidator(validatorDefs[0].type);
  }, [validatorDefs, newValidator]);
  useEffect(() => {
    if (!newPostFn && postFnDefs.length) setNewPostFn(postFnDefs[0].type);
  }, [postFnDefs, newPostFn]);

  const { data: screens = [] } = useQuery({
    queryKey: ['wf-transition-screens'],
    queryFn: () => workflowApi.listTransitionScreens().then((r) => r.data),
  });
  const [screenId, setScreenId] = useState('');

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
            {conditionDefs.length === 0 && <option value="user_in_group">User in group (fallback)</option>}
            {conditionDefs.map((d) => (
              <option key={d.type} value={d.type}>
                {d.name ?? d.type}
              </option>
            ))}
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
            {validatorDefs.length === 0 && <option value="field_required">Field required (fallback)</option>}
            {validatorDefs.map((d) => (
              <option key={d.type} value={d.type}>
                {d.name ?? d.type}
              </option>
            ))}
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
            {postFnDefs.length === 0 && (
              <>
                <option value="update_issue_field">Update issue field</option>
                <option value="assign_issue">Assign issue</option>
                <option value="fire_event">Fire event</option>
                <option value="create_comment">Create comment</option>
              </>
            )}
            {postFnDefs.map((d) => (
              <option key={d.type} value={d.type}>
                {d.name ?? d.type}
              </option>
            ))}
          </select>
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary" onClick={() => addPostFn.mutate()}>
            Add
          </button>
        </div>
      </section>

      <section className="wf-config-section">
        <h4>Transition screen</h4>
        <p className="wf-muted">Screen shown when executing this transition</p>
        <div className="wf-config-add">
          <select value={screenId} onChange={(e) => setScreenId(e.target.value)} className="ab-select">
            <option value="">No screen</option>
            {screens.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
          <button
            type="button"
            className="ab-btn ab-btn-sm ab-btn-secondary"
            disabled={!screenId}
            onClick={() => workflowApi.assignScreenToTransition(transition.id, screenId).then(invalidate)}
          >
            Assign
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-sm ab-btn-ghost"
            onClick={() => workflowApi.removeScreenFromTransition(transition.id).then(invalidate)}
          >
            Remove
          </button>
        </div>
      </section>
    </div>
  );
}
