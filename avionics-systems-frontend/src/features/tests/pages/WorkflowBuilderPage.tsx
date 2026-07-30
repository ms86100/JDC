import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import workflowEngineApi, {
  WorkflowBuilderState,
  WorkflowBuilderTransition,
  WorkflowBuilderData,
  WorkflowDefinitionResponse,
  ValidationResult,
} from '../../../api/workflowEngineApi';
import { WorkflowVisualizer } from '../components/WorkflowVisualizer';
import { appNotify } from '../../../lib/appNotify';
import './WorkflowBuilderPage.css';

// ========== Utility Functions ==========

const generateId = () => `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

const createDefaultState = (name: string, stateType: 'INITIAL' | 'INTERMEDIATE' | 'FINAL' = 'INTERMEDIATE'): WorkflowBuilderState => ({
  id: generateId(),
  name,
  description: '',
  stateType,
  position: { x: 0, y: 0 },
  color: stateType === 'INITIAL' ? '#36b37e' : stateType === 'FINAL' ? '#4c9aff' : '#6b778c',
  icon: stateType === 'INITIAL' ? '▶' : stateType === 'FINAL' ? '✓' : '○',
});

const createDefaultTransition = (
  fromStateId: string,
  toStateId: string,
  name: string = ''
): WorkflowBuilderTransition => ({
  id: generateId(),
  fromStateId,
  toStateId,
  name: name || `To ${toStateId}`,
  description: '',
  conditions: [],
  isValid: true,
});

// ========== Cycle Detection ==========

const detectCycles = (
  states: WorkflowBuilderState[],
  transitions: WorkflowBuilderTransition[]
): { hasCycle: boolean; cycleStates: string[] } => {
  const adjacency = new Map<string, string[]>();
  states.forEach(s => adjacency.set(s.id, []));
  transitions.forEach(t => {
    const existing = adjacency.get(t.fromStateId) || [];
    existing.push(t.toStateId);
    adjacency.set(t.fromStateId, existing);
  });

  const visited = new Set<string>();
  const recursionStack = new Set<string>();
  const cyclePath: string[] = [];

  const dfs = (nodeId: string): boolean => {
    visited.add(nodeId);
    recursionStack.add(nodeId);
    cyclePath.push(nodeId);

    const neighbors = adjacency.get(nodeId) || [];
    for (const neighbor of neighbors) {
      if (!visited.has(neighbor)) {
        if (dfs(neighbor)) return true;
      } else if (recursionStack.has(neighbor)) {
        const cycleStart = cyclePath.indexOf(neighbor);
        cyclePath.push(neighbor);
        return true;
      }
    }

    recursionStack.delete(nodeId);
    cyclePath.pop();
    return false;
  };

  for (const state of states) {
    if (!visited.has(state.id)) {
      if (dfs(state.id)) {
        const uniqueCycle = [...new Set(cyclePath)];
        return { hasCycle: true, cycleStates: uniqueCycle };
      }
    }
  }

  return { hasCycle: false, cycleStates: [] };
};

// ========== Validation ==========

const validateWorkflow = (
  states: WorkflowBuilderState[],
  transitions: WorkflowBuilderTransition[],
  initialStateId: string,
  finalStateIds: string[]
): ValidationResult => {
  const errors: string[] = [];
  const warnings: string[] = [];

  // Check for states
  if (states.length === 0) {
    errors.push('Workflow must have at least one state');
    return { errors, warnings, isValid: false, hasWarnings: false };
  }

  // Check for initial state
  if (!initialStateId) {
    errors.push('Workflow must have an initial state');
  } else if (!states.find(s => s.id === initialStateId)) {
    errors.push('Initial state not found');
  }

  // Check for final states
  if (finalStateIds.length === 0) {
    warnings.push('No terminal states defined');
  } else {
    const missingFinals = finalStateIds.filter(id => !states.find(s => s.id === id));
    if (missingFinals.length > 0) {
      errors.push(`Final states not found: ${missingFinals.join(', ')}`);
    }
  }

  // Check for isolated states
  const connectedStates = new Set<string>();
  transitions.forEach(t => {
    connectedStates.add(t.fromStateId);
    connectedStates.add(t.toStateId);
  });
  connectedStates.add(initialStateId);

  states.forEach(s => {
    if (!connectedStates.has(s.id) && s.stateType !== 'FINAL') {
      warnings.push(`State "${s.name}" has no connections`);
    }
  });

  // Check for unreachable states
  const reachable = new Set<string>();
  const queue = [initialStateId];
  while (queue.length > 0) {
    const current = queue.shift()!;
    if (reachable.has(current)) continue;
    reachable.add(current);
    transitions
      .filter(t => t.fromStateId === current)
      .forEach(t => queue.push(t.toStateId));
  }

  states.forEach(s => {
    if (!reachable.has(s.id) && s.id !== initialStateId) {
      warnings.push(`State "${s.name}" is not reachable from initial state`);
    }
  });

  // Check for cycles
  const { hasCycle } = detectCycles(states, transitions);
  if (hasCycle) {
    warnings.push('Workflow contains circular transitions');
  }

  // Check for duplicate state names
  const names = new Map<string, number>();
  states.forEach(s => {
    const count = names.get(s.name) || 0;
    names.set(s.name, count + 1);
  });
  names.forEach((count, name) => {
    if (count > 1) {
      errors.push(`Duplicate state name: "${name}"`);
    }
  });

  // Check for dangling transitions
  transitions.forEach(t => {
    if (!states.find(s => s.id === t.fromStateId)) {
      errors.push(`Transition "${t.name}" has invalid source state`);
    }
    if (!states.find(s => s.id === t.toStateId)) {
      errors.push(`Transition "${t.name}" has invalid target state`);
    }
  });

  return {
    errors,
    warnings,
    isValid: errors.length === 0,
    hasWarnings: warnings.length > 0,
  };
};

// ========== Component Types ==========

interface StateEditorProps {
  state: WorkflowBuilderState;
  onUpdate: (state: WorkflowBuilderState) => void;
  onDelete: () => void;
  canDelete: boolean;
  canSetInitial: boolean;
  canSetFinal: boolean;
  onSetInitial: () => void;
  onToggleFinal: () => void;
}

interface TransitionEditorProps {
  transition: WorkflowBuilderTransition;
  states: WorkflowBuilderState[];
  onUpdate: (transition: WorkflowBuilderTransition) => void;
  onDelete: () => void;
}

// ========== State Editor Component ==========

const StateEditor: React.FC<StateEditorProps> = ({
  state,
  onUpdate,
  onDelete,
  canDelete,
  canSetInitial,
  canSetFinal,
  onSetInitial,
  onToggleFinal,
}) => {
  const [localState, setLocalState] = useState(state);

  useEffect(() => {
    setLocalState(state);
  }, [state]);

  const handleNameChange = (name: string) => {
    const updated = { ...localState, name };
    setLocalState(updated);
    onUpdate(updated);
  };

  const handleDescriptionChange = (description: string) => {
    const updated = { ...localState, description };
    setLocalState(updated);
    onUpdate(updated);
  };

  const handleStateTypeChange = (stateType: 'INITIAL' | 'INTERMEDIATE' | 'FINAL') => {
    const updated = {
      ...localState,
      stateType,
      color: stateType === 'INITIAL' ? '#36b37e' : stateType === 'FINAL' ? '#4c9aff' : '#6b778c',
      icon: stateType === 'INITIAL' ? '▶' : stateType === 'FINAL' ? '✓' : '○',
    };
    setLocalState(updated);
    onUpdate(updated);
  };

  return (
    <div className="state-editor">
      <div className="state-editor-header">
        <span className="state-icon">{localState.icon}</span>
        <span className="state-name">{localState.name}</span>
      </div>

      <div className="state-editor-form">
        <div className="form-group">
          <label>Name</label>
          <input
            type="text"
            value={localState.name}
            onChange={(e) => handleNameChange(e.target.value)}
            placeholder="State name"
            className="form-input"
          />
        </div>

        <div className="form-group">
          <label>Description</label>
          <textarea
            value={localState.description || ''}
            onChange={(e) => handleDescriptionChange(e.target.value)}
            placeholder="Optional description"
            className="form-input"
            rows={2}
          />
        </div>

        <div className="form-group">
          <label>State Type</label>
          <select
            value={localState.stateType}
            onChange={(e) => handleStateTypeChange(e.target.value as 'INITIAL' | 'INTERMEDIATE' | 'FINAL')}
            className="form-input"
          >
            <option value="INITIAL">Initial</option>
            <option value="INTERMEDIATE">Intermediate</option>
            <option value="FINAL">Final</option>
          </select>
        </div>

        <div className="state-editor-actions">
          {canSetInitial && (
            <button
              type="button"
              className="btn btn-sm btn-secondary"
              onClick={onSetInitial}
            >
              Set as Initial
            </button>
          )}
          {canSetFinal && (
            <button
              type="button"
              className="btn btn-sm btn-secondary"
              onClick={onToggleFinal}
            >
              {state.stateType === 'FINAL' ? 'Remove Final' : 'Set as Final'}
            </button>
          )}
          {canDelete && (
            <button
              type="button"
              className="btn btn-sm btn-danger"
              onClick={onDelete}
            >
              Delete
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

// ========== Transition Editor Component ==========

const TransitionEditor: React.FC<TransitionEditorProps> = ({
  transition,
  states,
  onUpdate,
  onDelete,
}) => {
  const [localTransition, setLocalTransition] = useState(transition);

  useEffect(() => {
    setLocalTransition(transition);
  }, [transition]);

  const handleNameChange = (name: string) => {
    const updated = { ...localTransition, name };
    setLocalTransition(updated);
    onUpdate(updated);
  };

  const handleDescriptionChange = (description: string) => {
    const updated = { ...localTransition, description };
    setLocalTransition(updated);
    onUpdate(updated);
  };

  const handleFromStateChange = (fromStateId: string) => {
    const updated = { ...localTransition, fromStateId };
    setLocalTransition(updated);
    onUpdate(updated);
  };

  const handleToStateChange = (toStateId: string) => {
    const updated = { ...localTransition, toStateId };
    setLocalTransition(updated);
    onUpdate(updated);
  };

  const getStateName = (stateId: string) => {
    const state = states.find(s => s.id === stateId);
    return state?.name || stateId;
  };

  return (
    <div className="transition-editor">
      <div className="transition-editor-header">
        <span className="transition-arrow">→</span>
        <span className="transition-name">{localTransition.name || 'Unnamed Transition'}</span>
      </div>

      <div className="transition-editor-form">
        <div className="form-group">
          <label>Name</label>
          <input
            type="text"
            value={localTransition.name}
            onChange={(e) => handleNameChange(e.target.value)}
            placeholder="Transition name"
            className="form-input"
          />
        </div>

        <div className="form-group">
          <label>From State</label>
          <select
            value={localTransition.fromStateId}
            onChange={(e) => handleFromStateChange(e.target.value)}
            className="form-input"
          >
            {states.map(s => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.stateType})
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>To State</label>
          <select
            value={localTransition.toStateId}
            onChange={(e) => handleToStateChange(e.target.value)}
            className="form-input"
          >
            {states.map(s => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.stateType})
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label>Description</label>
          <textarea
            value={localTransition.description || ''}
            onChange={(e) => handleDescriptionChange(e.target.value)}
            placeholder="Optional description"
            className="form-input"
            rows={2}
          />
        </div>

        <div className="transition-editor-actions">
          <button
            type="button"
            className="btn btn-sm btn-danger"
            onClick={onDelete}
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
};

// ========== Main Component ==========

export const WorkflowBuilderPage: React.FC = () => {
  const { projectId, workflowId } = useParams<{ projectId?: string; workflowId?: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Workflow data state
  const [workflowData, setWorkflowData] = useState<WorkflowBuilderData>({
    id: workflowId,
    name: '',
    description: '',
    projectId: projectId || '',
    workflowType: 'TEST_EXECUTION',
    states: [],
    transitions: [],
    initialStateId: '',
    finalStateIds: [],
  });

  const [selectedStateId, setSelectedStateId] = useState<string | null>(null);
  const [selectedTransitionId, setSelectedTransitionId] = useState<string | null>(null);
  const [showValidation, setShowValidation] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  const [activeTab, setActiveTab] = useState<'states' | 'transitions' | 'visualize' | 'settings'>('states');
  const [isDrawingTransition, setIsDrawingTransition] = useState(false);
  const [transitionStartStateId, setTransitionStartStateId] = useState<string | null>(null);

  // Load existing workflow
  const { data: existingWorkflow, isLoading } = useQuery({
    queryKey: ['workflow-definition', workflowId],
    queryFn: () => workflowEngineApi.getDefinition(workflowId!),
    enabled: !!workflowId,
  });

  // Parse existing workflow data
  useEffect(() => {
    if (existingWorkflow) {
      try {
        const stepsJson = JSON.parse(existingWorkflow.workflowStepsJson);
        const rulesJson = JSON.parse(existingWorkflow.transitionRulesJson || '{}');

        const states: WorkflowBuilderState[] = (stepsJson.states || []).map((s: { name: string; description?: string; type?: string }, idx: number) => ({
          id: s.name.toLowerCase().replace(/\s+/g, '-'),
          name: s.name,
          description: s.description || '',
          stateType: s.type === 'INITIAL' ? 'INITIAL' : s.type === 'FINAL' ? 'FINAL' : 'INTERMEDIATE',
          position: { x: 0, y: 0 },
        }));

        const transitions: WorkflowBuilderTransition[] = Object.entries(rulesJson.transitions || {}).flatMap(
          ([fromState, targets]) => {
            const fromStateId = states.find(s => s.name === fromState)?.id || fromState;
            return (targets as string[]).map((toState: string) => ({
              id: generateId(),
              fromStateId,
              toStateId: states.find(s => s.name === toState)?.id || toState,
              name: `To ${toState}`,
              description: '',
              conditions: [],
              isValid: true,
            }));
          }
        );

        setWorkflowData({
          id: existingWorkflow.id,
          name: existingWorkflow.name,
          description: existingWorkflow.description || '',
          projectId: existingWorkflow.projectId,
          workflowType: existingWorkflow.workflowType,
          states,
          transitions,
          initialStateId: stepsJson.initialState
            ? states.find(s => s.name === stepsJson.initialState)?.id || ''
            : states[0]?.id || '',
          finalStateIds: (stepsJson.finalStates || []).map((fs: string) =>
            states.find(s => s.name === fs)?.id || fs
          ),
        });
      } catch (e) {
        console.error('Failed to parse workflow JSON:', e);
      }
    }
  }, [existingWorkflow]);

  // Validation
  const runValidation = useCallback(() => {
    const result = validateWorkflow(
      workflowData.states,
      workflowData.transitions,
      workflowData.initialStateId,
      workflowData.finalStateIds
    );
    setValidationResult(result);
    setShowValidation(true);
    return result;
  }, [workflowData]);

  // Cycle detection for highlighting
  const { hasCycle, cycleStates } = useMemo(
    () => detectCycles(workflowData.states, workflowData.transitions),
    [workflowData.states, workflowData.transitions]
  );

  // State operations
  const addState = useCallback((type: 'INITIAL' | 'INTERMEDIATE' | 'FINAL' = 'INTERMEDIATE') => {
    const existingNames = workflowData.states.map(s => s.name);
    let baseName = type === 'INITIAL' ? 'Start' : type === 'FINAL' ? 'End' : 'New State';
    let name = baseName;
    let counter = 1;
    while (existingNames.includes(name)) {
      name = `${baseName} ${counter++}`;
    }

    const newState = createDefaultState(name, type);
    const updatedData = {
      ...workflowData,
      states: [...workflowData.states, newState],
    };

    if (type === 'INITIAL' && !workflowData.initialStateId) {
      updatedData.initialStateId = newState.id;
    } else if (type === 'FINAL') {
      updatedData.finalStateIds = [...workflowData.finalStateIds, newState.id];
    }

    setWorkflowData(updatedData);
    setSelectedStateId(newState.id);
  }, [workflowData]);

  const updateState = useCallback((updatedState: WorkflowBuilderState) => {
    setWorkflowData(prev => ({
      ...prev,
      states: prev.states.map(s => s.id === updatedState.id ? updatedState : s),
    }));
  }, []);

  const deleteState = useCallback((stateId: string) => {
    setWorkflowData(prev => ({
      ...prev,
      states: prev.states.filter(s => s.id !== stateId),
      transitions: prev.transitions.filter(t => t.fromStateId !== stateId && t.toStateId !== stateId),
      initialStateId: prev.initialStateId === stateId ? '' : prev.initialStateId,
      finalStateIds: prev.finalStateIds.filter(id => id !== stateId),
    }));
    if (selectedStateId === stateId) {
      setSelectedStateId(null);
    }
  }, [selectedStateId]);

  const setInitialState = useCallback((stateId: string) => {
    setWorkflowData(prev => ({
      ...prev,
      initialStateId: stateId,
      states: prev.states.map(s => ({
        ...s,
        stateType: s.id === stateId ? 'INITIAL' : s.stateType === 'INITIAL' ? 'INTERMEDIATE' : s.stateType,
      })),
    }));
  }, []);

  const toggleFinalState = useCallback((stateId: string) => {
    setWorkflowData(prev => {
      const isFinal = prev.finalStateIds.includes(stateId);
      const newStateType = isFinal ? 'INTERMEDIATE' : 'FINAL';
      return {
        ...prev,
        finalStateIds: isFinal
          ? prev.finalStateIds.filter(id => id !== stateId)
          : [...prev.finalStateIds, stateId],
        states: prev.states.map(s => ({
          ...s,
          stateType: s.id === stateId ? newStateType : s.stateType,
        })),
      };
    });
  }, []);

  // Transition operations
  const addTransition = useCallback((fromStateId?: string, toStateId?: string) => {
    const newTransition = createDefaultTransition(
      fromStateId || workflowData.initialStateId || workflowData.states[0]?.id || '',
      toStateId || workflowData.states[0]?.id || ''
    );
    setWorkflowData(prev => ({
      ...prev,
      transitions: [...prev.transitions, newTransition],
    }));
    setSelectedTransitionId(newTransition.id);
  }, [workflowData]);

  const updateTransition = useCallback((updatedTransition: WorkflowBuilderTransition) => {
    setWorkflowData(prev => ({
      ...prev,
      transitions: prev.transitions.map(t => t.id === updatedTransition.id ? updatedTransition : t),
    }));
  }, []);

  const deleteTransition = useCallback((transitionId: string) => {
    setWorkflowData(prev => ({
      ...prev,
      transitions: prev.transitions.filter(t => t.id !== transitionId),
    }));
    if (selectedTransitionId === transitionId) {
      setSelectedTransitionId(null);
    }
  }, [selectedTransitionId]);

  // Drawing transitions
  const handleNodeClick = useCallback((nodeId: string) => {
    if (isDrawingTransition) {
      if (transitionStartStateId && transitionStartStateId !== nodeId) {
        addTransition(transitionStartStateId, nodeId);
        setIsDrawingTransition(false);
        setTransitionStartStateId(null);
      }
    } else {
      setSelectedStateId(nodeId);
      setSelectedTransitionId(null);
    }
  }, [isDrawingTransition, transitionStartStateId, addTransition]);

  const startDrawingTransition = useCallback(() => {
    if (!workflowData.initialStateId) {
      appNotify.warning('Please set an initial state first');
      return;
    }
    setIsDrawingTransition(true);
    setTransitionStartStateId(workflowData.initialStateId);
  }, [workflowData.initialStateId]);

  const cancelDrawingTransition = useCallback(() => {
    setIsDrawingTransition(false);
    setTransitionStartStateId(null);
  }, []);

  // Save workflow
  const saveMutation = useMutation({
    mutationFn: async () => {
      // Build the workflow steps JSON
      const workflowSteps = {
        initialState: workflowData.states.find(s => s.id === workflowData.initialStateId)?.name || 'START',
        states: workflowData.states.map(s => ({
          name: s.name,
          description: s.description,
          type: s.stateType,
        })),
        finalStates: workflowData.finalStateIds
          .map(id => workflowData.states.find(s => s.id === id)?.name)
          .filter(Boolean),
      };

      // Build transition rules JSON
      const transitions: Record<string, string[]> = {};
      workflowData.transitions.forEach(t => {
        const fromName = workflowData.states.find(s => s.id === t.fromStateId)?.name || t.fromStateId;
        if (!transitions[fromName]) {
          transitions[fromName] = [];
        }
        const toName = workflowData.states.find(s => s.id === t.toStateId)?.name || t.toStateId;
        if (!transitions[fromName].includes(toName)) {
          transitions[fromName].push(toName);
        }
      });

      const transitionRules = { transitions };

      const payload = {
        name: workflowData.name,
        description: workflowData.description,
        projectId: workflowData.projectId,
        workflowType: workflowData.workflowType,
        workflowStepsJson: JSON.stringify(workflowSteps),
        transitionRulesJson: JSON.stringify(transitionRules),
        isDefault: false,
      };

      if (workflowId) {
        return workflowEngineApi.updateDefinition(workflowId, payload);
      } else {
        return workflowEngineApi.createDefinition(payload);
      }
    },
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['workflow-definition'] });
      if (!workflowId && result.id) {
        navigate(`/tests/workflows/builder/${result.id}`);
      }
    },
  });

  // Prepare visualizer data
  const visualizerNodes = useMemo(() =>
    workflowData.states.map(s => ({
      id: s.id,
      stateId: s.name,
      label: s.name,
      stateType: s.stateType,
      isActive: workflowData.finalStateIds.includes(s.id),
      isCurrent: s.id === workflowData.initialStateId,
      position: s.position.x > 0 || s.position.y > 0 ? s.position : {
        x: Math.random() * 400 + 50,
        y: Math.random() * 200 + 50,
      },
    })),
    [workflowData.states, workflowData.finalStateIds, workflowData.initialStateId]
  );

  const visualizerEdges = useMemo(() =>
    workflowData.transitions.map(t => ({
      id: t.id,
      fromNodeId: t.fromStateId,
      toNodeId: t.toStateId,
      label: t.name,
      isTraversed: false,
    })),
    [workflowData.transitions]
  );

  const selectedState = workflowData.states.find(s => s.id === selectedStateId);
  const selectedTransition = workflowData.transitions.find(t => t.id === selectedTransitionId);

  // Load workflow if opening existing
  if (isLoading) {
    return (
      <div className="workflow-builder-page">
        <div className="loading-spinner">Loading workflow...</div>
      </div>
    );
  }

  return (
    <div className="workflow-builder-page">
      {/* Header */}
      <header className="workflow-builder-header">
        <div className="header-left">
          <Link to="/tests/workflows" className="back-link">
            ← Workflows
          </Link>
          <h1>{workflowId ? 'Edit Workflow' : 'Create Workflow'}</h1>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={runValidation}
          >
            Validate
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => saveMutation.mutate()}
            disabled={saveMutation.isPending}
          >
            {saveMutation.isPending ? 'Saving...' : 'Save Workflow'}
          </button>
        </div>
      </header>

      {/* Validation Results */}
      {showValidation && validationResult && (
        <div className={`validation-panel ${validationResult.isValid ? 'valid' : 'invalid'}`}>
          <div className="validation-header">
            <h3>Validation Results</h3>
            <button
              type="button"
              className="btn-close"
              onClick={() => setShowValidation(false)}
            >
              ×
            </button>
          </div>
          {validationResult.errors.length > 0 && (
            <div className="validation-errors">
              <h4>Errors</h4>
              <ul>
                {validationResult.errors.map((err, i) => (
                  <li key={i}>{err}</li>
                ))}
              </ul>
            </div>
          )}
          {validationResult.warnings.length > 0 && (
            <div className="validation-warnings">
              <h4>Warnings</h4>
              <ul>
                {validationResult.warnings.map((warn, i) => (
                  <li key={i}>{warn}</li>
                ))}
              </ul>
            </div>
          )}
          {validationResult.isValid && (
            <p className="validation-success">Workflow is valid!</p>
          )}
        </div>
      )}

      {/* Tabs */}
      <div className="workflow-builder-tabs">
        <button
          type="button"
          className={`tab ${activeTab === 'states' ? 'active' : ''}`}
          onClick={() => setActiveTab('states')}
        >
          States ({workflowData.states.length})
        </button>
        <button
          type="button"
          className={`tab ${activeTab === 'transitions' ? 'active' : ''}`}
          onClick={() => setActiveTab('transitions')}
        >
          Transitions ({workflowData.transitions.length})
        </button>
        <button
          type="button"
          className={`tab ${activeTab === 'visualize' ? 'active' : ''}`}
          onClick={() => setActiveTab('visualize')}
        >
          Visualize
        </button>
        <button
          type="button"
          className={`tab ${activeTab === 'settings' ? 'active' : ''}`}
          onClick={() => setActiveTab('settings')}
        >
          Settings
        </button>
      </div>

      {/* Tab Content */}
      <div className="workflow-builder-content">
        {/* States Tab */}
        {activeTab === 'states' && (
          <div className="states-tab">
            <div className="add-state-buttons">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => addState('INITIAL')}
                disabled={!!workflowData.initialStateId}
              >
                + Add Initial State
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => addState('INTERMEDIATE')}
              >
                + Add Intermediate State
              </button>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => addState('FINAL')}
              >
                + Add Final State
              </button>
            </div>

            <div className="states-list">
              {workflowData.states.length === 0 ? (
                <div className="empty-state">
                  <p>No states defined yet. Add states to build your workflow.</p>
                </div>
              ) : (
                workflowData.states.map(state => (
                  <StateEditor
                    key={state.id}
                    state={state}
                    onUpdate={updateState}
                    onDelete={() => deleteState(state.id)}
                    canDelete={workflowData.states.length > 1}
                    canSetInitial={state.stateType !== 'INITIAL' && !workflowData.initialStateId}
                    canSetFinal={state.stateType !== 'FINAL'}
                    onSetInitial={() => setInitialState(state.id)}
                    onToggleFinal={() => toggleFinalState(state.id)}
                  />
                ))
              )}
            </div>
          </div>
        )}

        {/* Transitions Tab */}
        {activeTab === 'transitions' && (
          <div className="transitions-tab">
            <div className="add-transition-buttons">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => addTransition()}
                disabled={workflowData.states.length < 2}
              >
                + Add Transition
              </button>
              <button
                type="button"
                className={`btn ${isDrawingTransition ? 'btn-warning' : 'btn-secondary'}`}
                onClick={isDrawingTransition ? cancelDrawingTransition : startDrawingTransition}
                disabled={workflowData.states.length < 2}
              >
                {isDrawingTransition ? 'Cancel Drawing' : 'Draw Transition'}
              </button>
            </div>

            {isDrawingTransition && (
              <div className="drawing-hint">
                Click on a target state to create a transition from{' '}
                <strong>{workflowData.states.find(s => s.id === transitionStartStateId)?.name}</strong>
              </div>
            )}

            <div className="transitions-list">
              {workflowData.transitions.length === 0 ? (
                <div className="empty-state">
                  <p>No transitions defined yet. Add transitions between states.</p>
                </div>
              ) : (
                workflowData.transitions.map(transition => (
                  <TransitionEditor
                    key={transition.id}
                    transition={transition}
                    states={workflowData.states}
                    onUpdate={updateTransition}
                    onDelete={() => deleteTransition(transition.id)}
                  />
                ))
              )}
            </div>
          </div>
        )}

        {/* Visualize Tab */}
        {activeTab === 'visualize' && (
          <div className="visualize-tab">
            {workflowData.states.length === 0 ? (
              <div className="empty-state">
                <p>Add states and transitions to see the workflow visualization.</p>
              </div>
            ) : (
              <>
                {hasCycle && (
                  <div className="cycle-warning">
                    Warning: Circular transitions detected
                  </div>
                )}
                <WorkflowVisualizer
                  nodes={visualizerNodes}
                  edges={visualizerEdges}
                  currentStateId={workflowData.initialStateId}
                  width={800}
                  height={400}
                  onNodeClick={handleNodeClick}
                  selectedNodeId={selectedStateId || undefined}
                  showLabels={true}
                />
                <div className="visualizer-legend">
                  <div className="legend-item">
                    <span className="legend-color initial"></span>
                    Initial State
                  </div>
                  <div className="legend-item">
                    <span className="legend-color intermediate"></span>
                    Intermediate State
                  </div>
                  <div className="legend-item">
                    <span className="legend-color final"></span>
                    Final State
                  </div>
                  <div className="legend-item">
                    <span className="legend-color current"></span>
                    Current/Initial
                  </div>
                </div>
              </>
            )}
          </div>
        )}

        {/* Settings Tab */}
        {activeTab === 'settings' && (
          <div className="settings-tab">
            <div className="form-group">
              <label>Workflow Name *</label>
              <input
                type="text"
                value={workflowData.name}
                onChange={(e) => setWorkflowData(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Enter workflow name"
                className="form-input"
              />
            </div>

            <div className="form-group">
              <label>Description</label>
              <textarea
                value={workflowData.description}
                onChange={(e) => setWorkflowData(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Enter workflow description"
                className="form-input"
                rows={3}
              />
            </div>

            <div className="form-group">
              <label>Workflow Type</label>
              <select
                value={workflowData.workflowType}
                onChange={(e) => setWorkflowData(prev => ({ ...prev, workflowType: e.target.value }))}
                className="form-input"
              >
                <option value="TEST_EXECUTION">Test Execution</option>
                <option value="TEST_PLAN">Test Plan</option>
                <option value="TEST_SET">Test Set</option>
                <option value="DEFECT">Defect</option>
                <option value="REVIEW">Review</option>
              </select>
            </div>

            <div className="form-group">
              <label>Project ID</label>
              <input
                type="text"
                value={workflowData.projectId}
                onChange={(e) => setWorkflowData(prev => ({ ...prev, projectId: e.target.value }))}
                placeholder="Enter project ID"
                className="form-input"
              />
            </div>

            <div className="workflow-summary">
              <h3>Workflow Summary</h3>
              <ul>
                <li>States: {workflowData.states.length}</li>
                <li>Transitions: {workflowData.transitions.length}</li>
                <li>Initial State: {workflowData.states.find(s => s.id === workflowData.initialStateId)?.name || 'Not set'}</li>
                <li>Final States: {workflowData.finalStateIds.length}</li>
              </ul>
            </div>
          </div>
        )}
      </div>

      {/* Side Panel for Selected Items */}
      {(selectedState || selectedTransition) && (
        <div className="workflow-builder-side-panel">
          <h3>Selected</h3>
          {selectedState && (
            <div className="side-panel-content">
              <h4>State: {selectedState.name}</h4>
              <StateEditor
                state={selectedState}
                onUpdate={updateState}
                onDelete={() => deleteState(selectedState.id)}
                canDelete={workflowData.states.length > 1}
                canSetInitial={selectedState.stateType !== 'INITIAL'}
                canSetFinal={selectedState.stateType !== 'FINAL'}
                onSetInitial={() => setInitialState(selectedState.id)}
                onToggleFinal={() => toggleFinalState(selectedState.id)}
              />
            </div>
          )}
          {selectedTransition && (
            <div className="side-panel-content">
              <h4>Transition: {selectedTransition.name}</h4>
              <TransitionEditor
                transition={selectedTransition}
                states={workflowData.states}
                onUpdate={updateTransition}
                onDelete={() => deleteTransition(selectedTransition.id)}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default WorkflowBuilderPage;