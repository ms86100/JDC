import { Fragment, useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowApi, WorkflowTransitionDetail } from '../../../api/workflowApi';
import { scriptApi, ScriptDefinition } from '../../../api/scriptApi';

interface Props {
  transition: WorkflowTransitionDetail;
  onClose: () => void;
}

const SCRIPT_CONDITION_TYPES = ['SCRIPT', 'custom_script'];
const SCRIPT_VALIDATOR_TYPES = ['SCRIPT', 'custom_validator'];
const SCRIPT_POSTFN_TYPES = ['SCRIPT_POST_FUNCTION', 'custom_script', 'script_post_function'];

export function TransitionConfigPanel({ transition, onClose }: Props) {
  const queryClient = useQueryClient();
  const [newCondition, setNewCondition] = useState('');
  const [newValidator, setNewValidator] = useState('');
  const [newPostFn, setNewPostFn] = useState('');
  const [selectedConditionScript, setSelectedConditionScript] = useState('');
  const [selectedValidatorScript, setSelectedValidatorScript] = useState('');
  const [selectedPostFnScript, setSelectedPostFnScript] = useState('');

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

  const { data: conditionScripts = [] } = useQuery({
    queryKey: ['available-scripts', 'CONDITION'],
    queryFn: () => scriptApi.getAvailable('CONDITION').then((r) => r.data).catch(() => [] as ScriptDefinition[]),
  });
  const { data: validatorScripts = [] } = useQuery({
    queryKey: ['available-scripts', 'VALIDATOR'],
    queryFn: () => scriptApi.getAvailable('VALIDATOR').then((r) => r.data).catch(() => [] as ScriptDefinition[]),
  });
  const { data: postFnScripts = [] } = useQuery({
    queryKey: ['available-scripts', 'POST_FUNCTION'],
    queryFn: () => scriptApi.getAvailable('POST_FUNCTION').then((r) => r.data).catch(() => [] as ScriptDefinition[]),
  });

  const getDefType = (d: { type?: string; id?: string }) => d.type || d.id || '';
  const getDefName = (d: { name?: string; type?: string; id?: string }) => d.name || d.type || d.id || '';

  useEffect(() => {
    if (!newCondition && conditionDefs.length) setNewCondition(getDefType(conditionDefs[0]));
  }, [conditionDefs, newCondition]);
  useEffect(() => {
    if (!newValidator && validatorDefs.length) setNewValidator(getDefType(validatorDefs[0]));
  }, [validatorDefs, newValidator]);
  useEffect(() => {
    if (!newPostFn && postFnDefs.length) setNewPostFn(getDefType(postFnDefs[0]));
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

  const isScriptCondition = SCRIPT_CONDITION_TYPES.includes(newCondition);
  const isScriptValidator = SCRIPT_VALIDATOR_TYPES.includes(newValidator);
  const isScriptPostFn = SCRIPT_POSTFN_TYPES.includes(newPostFn);

  const addCondition = useMutation({
    mutationFn: () => {
      const conditionType = isScriptCondition ? 'SCRIPT' : newCondition;
      const data: Record<string, unknown> = { conditionType };
      if (isScriptCondition && selectedConditionScript) {
        data.value = selectedConditionScript;
      }
      return workflowApi.addCondition(transition.id, data);
    },
    onSuccess: invalidate,
  });
  const addValidator = useMutation({
    mutationFn: () => {
      const validatorType = isScriptValidator ? 'SCRIPT' : newValidator;
      const data: Record<string, unknown> = { validatorType };
      if (isScriptValidator && selectedValidatorScript) {
        data.validatorData = selectedValidatorScript;
      }
      return workflowApi.addValidator(transition.id, data);
    },
    onSuccess: invalidate,
  });
  const addPostFn = useMutation({
    mutationFn: () => {
      const functionType = isScriptPostFn ? 'SCRIPT_POST_FUNCTION' : newPostFn;
      const data: Record<string, unknown> = { functionType };
      if (isScriptPostFn && selectedPostFnScript) {
        data.functionData = JSON.stringify({ scriptKey: selectedPostFnScript });
      }
      return workflowApi.addPostFunction(transition.id, data);
    },
    onSuccess: invalidate,
  });

  const renderScriptPicker = (
    scripts: ScriptDefinition[],
    selected: string,
    onChange: (val: string) => void,
    label: string
  ) => (
    <select value={selected} onChange={(e) => onChange(e.target.value)} className="ab-select" style={{ marginLeft: 4 }}>
      <option value="">Select {label}...</option>
      {scripts.map((s) => (
        <option key={s.scriptKey} value={s.scriptKey}>
          {s.name} ({s.scriptKey})
        </option>
      ))}
    </select>
  );

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
              <span>
                {c.type}
                {c.value && <span className="wf-script-badge"> ({c.value})</span>}
              </span>
              <button type="button" className="ab-btn ab-btn-ghost ab-btn-sm"
                onClick={() => workflowApi.deleteCondition(transition.id, c.id).then(invalidate)}>
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div className="wf-config-add">
          <select value={newCondition} onChange={(e) => setNewCondition(e.target.value)} className="ab-select">
            {conditionDefs.length === 0 && <option value="user_in_group">User in group (fallback)</option>}
            {conditionDefs.map((d) => (
              <option key={getDefType(d)} value={getDefType(d)}>{getDefName(d)}</option>
            ))}
          </select>
          {isScriptCondition && renderScriptPicker(conditionScripts, selectedConditionScript, setSelectedConditionScript, 'script')}
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary"
            disabled={isScriptCondition && !selectedConditionScript}
            onClick={() => addCondition.mutate()}>
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
              <span>
                {v.type}
                {v.validatorData && <span className="wf-script-badge"> ({v.validatorData})</span>}
              </span>
              <button type="button" className="ab-btn ab-btn-ghost ab-btn-sm"
                onClick={() => workflowApi.deleteValidator(transition.id, v.id).then(invalidate)}>
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div className="wf-config-add">
          <select value={newValidator} onChange={(e) => setNewValidator(e.target.value)} className="ab-select">
            {validatorDefs.length === 0 && <option value="field_required">Field required (fallback)</option>}
            {validatorDefs.map((d) => (
              <option key={getDefType(d)} value={getDefType(d)}>{getDefName(d)}</option>
            ))}
          </select>
          {isScriptValidator && renderScriptPicker(validatorScripts, selectedValidatorScript, setSelectedValidatorScript, 'script')}
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary"
            disabled={isScriptValidator && !selectedValidatorScript}
            onClick={() => addValidator.mutate()}>
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
              <span>
                {p.type}
                {p.functionData && <span className="wf-script-badge"> ({p.functionData})</span>}
              </span>
              <button type="button" className="ab-btn ab-btn-ghost ab-btn-sm"
                onClick={() => workflowApi.deletePostFunction(transition.id, p.id).then(invalidate)}>
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
              <option key={getDefType(d)} value={getDefType(d)}>{getDefName(d)}</option>
            ))}
          </select>
          {isScriptPostFn && renderScriptPicker(postFnScripts, selectedPostFnScript, setSelectedPostFnScript, 'script')}
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary"
            disabled={isScriptPostFn && !selectedPostFnScript}
            onClick={() => addPostFn.mutate()}>
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
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary" disabled={!screenId}
            onClick={() => workflowApi.assignScreenToTransition(transition.id, screenId).then(invalidate)}>
            Assign
          </button>
          <button type="button" className="ab-btn ab-btn-sm ab-btn-ghost"
            onClick={() => workflowApi.removeScreenFromTransition(transition.id).then(invalidate)}>
            Remove
          </button>
        </div>
      </section>
    </div>
  );
}
