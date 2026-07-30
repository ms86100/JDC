import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowApi } from '../../../api/workflowApi';
import './workflow-management.css';

export default function WorkflowTransitionScreensPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const { data: screens = [], isLoading } = useQuery({
    queryKey: ['wf-transition-screens-admin'],
    queryFn: () => workflowApi.listTransitionScreens().then((r) => r.data),
  });

  const createScreen = useMutation({
    mutationFn: () => workflowApi.createTransitionScreen({ name, description: description || undefined }),
    onSuccess: () => {
      setName('');
      setDescription('');
      queryClient.invalidateQueries({ queryKey: ['wf-transition-screens-admin'] });
      queryClient.invalidateQueries({ queryKey: ['wf-transition-screens'] });
    },
  });

  const deleteScreen = useMutation({
    mutationFn: (id: string) => workflowApi.deleteTransitionScreen(id),
    onSuccess: () => {
      setSelectedId(null);
      queryClient.invalidateQueries({ queryKey: ['wf-transition-screens-admin'] });
      queryClient.invalidateQueries({ queryKey: ['wf-transition-screens'] });
    },
  });

  const selected = screens.find((s) => s.id === selectedId);

  return (
    <div className="wf-page">
      <header className="wf-page-header">
        <div>
          <Link to="/workflows" className="wf-back">
            ← Workflows
          </Link>
          <h1>Workflow transition screens</h1>
          <p className="wf-muted">
            Admin screens for workflow transitions (distinct from issue field screens). Assign from transition
            config panel or designer.
          </p>
        </div>
      </header>

      <div className="wf-schemes-layout">
        <aside className="wf-schemes-list wf-panel">
          <h2>Screens</h2>
          {isLoading && <p className="wf-muted">Loading…</p>}
          <ul style={{ listStyle: 'none', padding: 0 }}>
            {screens.map((s) => (
              <li key={s.id}>
                <button
                  type="button"
                  className={`wf-scheme-item ${selectedId === s.id ? 'active' : ''}`}
                  style={{ width: '100%', textAlign: 'left', padding: 8, marginBottom: 4 }}
                  onClick={() => setSelectedId(s.id)}
                >
                  {s.name}
                </button>
              </li>
            ))}
            {screens.length === 0 && !isLoading && (
              <li className="wf-muted" style={{ padding: 8 }}>
                No screens yet.
              </li>
            )}
          </ul>
        </aside>

        <section className="wf-panel wf-scheme-detail space-y-4">
          <h2>Create screen</h2>
          <div className="wf-inline-form">
            <input
              className="ab-input"
              placeholder="Screen name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <input
              className="ab-input"
              placeholder="Description (optional)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
            <button
              type="button"
              className="ab-btn ab-btn-primary ab-btn-sm"
              disabled={!name.trim() || createScreen.isPending}
              onClick={() => createScreen.mutate()}
            >
              {createScreen.isPending ? 'Creating…' : 'Create'}
            </button>
          </div>

          {selected && (
            <div className="border-t pt-4">
              <h3>{selected.name}</h3>
              {selected.description && <p className="wf-muted">{selected.description}</p>}
              <p className="text-xs font-mono text-gray-500">ID: {selected.id}</p>
              <button
                type="button"
                className="ab-btn ab-btn-ghost ab-btn-sm"
                style={{ marginTop: 12, color: '#de350b' }}
                disabled={deleteScreen.isPending}
                onClick={() => {
                  if (window.confirm(`Delete screen "${selected.name}"?`)) deleteScreen.mutate(selected.id);
                }}
              >
                Delete screen
              </button>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
