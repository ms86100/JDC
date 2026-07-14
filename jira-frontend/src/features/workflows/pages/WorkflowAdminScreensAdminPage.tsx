import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowAdminApi } from '../../../api/workflowAdminApi';

export default function WorkflowAdminScreensAdminPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [screenType, setScreenType] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const { data: screens = [], isLoading } = useQuery({
    queryKey: ['workflow-admin', 'screens'],
    queryFn: () =>
      workflowAdminApi
        .listScreens(screenType || undefined)
        .then((r) => Array.isArray(r.data) ? r.data : []),
  });

  const { data: screenDetail } = useQuery({
    queryKey: ['workflow-admin', 'screen', selectedId],
    queryFn: () => workflowAdminApi.getScreen(selectedId!).then((r) => r.data),
    enabled: !!selectedId,
  });

  const createScreen = useMutation({
    mutationFn: () =>
      workflowAdminApi.createScreen({
        name,
        description: description || undefined,
        screenType: screenType || undefined,
      }),
    onSuccess: () => {
      setName('');
      setDescription('');
      setMessage('Screen created');
      queryClient.invalidateQueries({ queryKey: ['workflow-admin', 'screens'] });
    },
    onError: (e: Error) => setMessage(e.message),
  });

  const deleteScreen = useMutation({
    mutationFn: (id: string) => workflowAdminApi.deleteScreen(id),
    onSuccess: () => {
      setSelectedId(null);
      setMessage('Screen deleted');
      queryClient.invalidateQueries({ queryKey: ['workflow-admin', 'screens'] });
    },
    onError: (e: Error) => setMessage(e.message),
  });

  return (
    <div className="space-y-4" data-testid="workflow-admin-screens">
      {message && (
        <p className="text-sm bg-blue-50 border border-blue-200 text-blue-800 rounded p-2">{message}</p>
      )}

      <div className="grid lg:grid-cols-2 gap-4">
        <div className="bg-white border rounded-lg p-4 space-y-3">
          <h2 className="font-semibold">Create transition screen</h2>
          <input
            className="w-full border rounded px-3 py-2 text-sm"
            placeholder="Screen name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <input
            className="w-full border rounded px-3 py-2 text-sm"
            placeholder="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <input
            className="w-full border rounded px-3 py-2 text-sm"
            placeholder="Screen type filter (optional)"
            value={screenType}
            onChange={(e) => setScreenType(e.target.value)}
          />
          <button
            type="button"
            className="px-4 py-2 bg-jira-blue text-white text-sm rounded disabled:opacity-50"
            disabled={!name || createScreen.isPending}
            onClick={() => createScreen.mutate()}
          >
            POST /api/admin/workflows/screens
          </button>
        </div>

        <div className="bg-white border rounded-lg p-4">
          <h2 className="font-semibold mb-2">Screens ({screens.length})</h2>
          {isLoading ? (
            <p className="text-sm text-gray-500">Loading…</p>
          ) : (
            <ul className="divide-y max-h-64 overflow-auto text-sm">
              {screens.map((s) => {
                const id = String(s.id ?? '');
                const label = String(s.name ?? id);
                return (
                  <li key={id} className="py-2 flex justify-between gap-2">
                    <button
                      type="button"
                      className="text-jira-blue hover:underline text-left"
                      onClick={() => setSelectedId(id)}
                    >
                      {label}
                    </button>
                    <button
                      type="button"
                      className="text-xs text-red-600"
                      onClick={() => {
                        if (confirm(`Delete screen "${label}"?`)) deleteScreen.mutate(id);
                      }}
                    >
                      Delete
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>

      {selectedId && (
        <div className="bg-white border rounded-lg p-4">
          <h2 className="font-semibold text-sm mb-2">Screen JSON</h2>
          <pre className="text-xs overflow-auto max-h-80 bg-gray-50 p-3 rounded">
            {JSON.stringify(screenDetail ?? {}, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
