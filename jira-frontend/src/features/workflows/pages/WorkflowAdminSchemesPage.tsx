import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowAdminApi } from '../../../api/workflowAdminApi';

export default function WorkflowAdminSchemesPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const { data: schemes = [], isLoading } = useQuery({
    queryKey: ['workflow-admin', 'schemes'],
    queryFn: () => workflowAdminApi.listSchemes().then((r) => r.data as Record<string, unknown>[]),
  });

  const { data: schemeDetail } = useQuery({
    queryKey: ['workflow-admin', 'scheme', selectedId],
    queryFn: () => workflowAdminApi.getScheme(selectedId!).then((r) => r.data),
    enabled: !!selectedId,
  });

  const createScheme = useMutation({
    mutationFn: () =>
      workflowAdminApi.createScheme({
        name,
        description: description || undefined,
      }),
    onSuccess: () => {
      setName('');
      setDescription('');
      setMessage('Scheme created');
      queryClient.invalidateQueries({ queryKey: ['workflow-admin', 'schemes'] });
    },
    onError: (e: Error) => setMessage(e.message),
  });

  const deleteScheme = useMutation({
    mutationFn: (id: string) => workflowAdminApi.deleteScheme(id),
    onSuccess: () => {
      setSelectedId(null);
      setMessage('Scheme deleted');
      queryClient.invalidateQueries({ queryKey: ['workflow-admin', 'schemes'] });
    },
    onError: (e: Error) => setMessage(e.message),
  });

  return (
    <div className="space-y-4" data-testid="workflow-admin-schemes">
      {message && (
        <p className="text-sm bg-blue-50 border border-blue-200 text-blue-800 rounded p-2">{message}</p>
      )}

      <div className="grid lg:grid-cols-2 gap-4">
        <div className="bg-white border rounded-lg p-4 space-y-3">
          <h2 className="font-semibold">Create scheme</h2>
          <input
            className="w-full border rounded px-3 py-2 text-sm"
            placeholder="Scheme name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <input
            className="w-full border rounded px-3 py-2 text-sm"
            placeholder="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <button
            type="button"
            className="px-4 py-2 bg-jira-blue text-white text-sm rounded disabled:opacity-50"
            disabled={!name || createScheme.isPending}
            onClick={() => createScheme.mutate()}
          >
            Create via admin API
          </button>
        </div>

        <div className="bg-white border rounded-lg p-4">
          <h2 className="font-semibold mb-2">Schemes ({schemes.length})</h2>
          {isLoading ? (
            <p className="text-sm text-gray-500">Loading…</p>
          ) : (
            <ul className="divide-y max-h-64 overflow-auto text-sm">
              {schemes.map((s) => {
                const id = String(s.id ?? '');
                const label = String(s.name ?? id);
                return (
                  <li key={id} className="py-2 flex justify-between gap-2 items-center">
                    <button
                      type="button"
                      className="text-left text-jira-blue hover:underline flex-1"
                      onClick={() => setSelectedId(id)}
                    >
                      {label}
                    </button>
                    <button
                      type="button"
                      className="text-xs text-red-600 hover:underline"
                      onClick={() => {
                        if (confirm(`Delete scheme "${label}"?`)) deleteScheme.mutate(id);
                      }}
                    >
                      Delete
                    </button>
                  </li>
                );
              })}
              {schemes.length === 0 && (
                <li className="py-4 text-gray-500">No schemes returned from admin API.</li>
              )}
            </ul>
          )}
        </div>
      </div>

      {selectedId && (
        <div className="bg-white border rounded-lg p-4">
          <h2 className="font-semibold text-sm mb-2">Scheme detail</h2>
          <pre className="text-xs overflow-auto max-h-80 bg-gray-50 p-3 rounded">
            {JSON.stringify(schemeDetail ?? {}, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
