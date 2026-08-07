import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowAdminApi } from '../../../api/workflowAdminApi';
import { workflowApi } from '../../../api/workflowApi';

export default function WorkflowAdminSchemesPage() {
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [newMappingIssueTypeId, setNewMappingIssueTypeId] = useState('');
  const [newMappingWorkflowId, setNewMappingWorkflowId] = useState('');

  const { data: schemes = [], isLoading } = useQuery({
    queryKey: ['workflow-admin', 'schemes'],
    queryFn: () => workflowAdminApi.listSchemes().then((r) => Array.isArray(r.data) ? r.data : []),
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

  const { data: workflows = [] } = useQuery({
    queryKey: ['workflows'],
    queryFn: () => workflowApi.getAll().then((r) => Array.isArray(r.data) ? r.data : []),
  });

  const addMapping = useMutation({
    mutationFn: () =>
      workflowApi.addSchemeMapping(selectedId!, {
        issueTypeId: newMappingIssueTypeId,
        workflowId: newMappingWorkflowId,
      }),
    onSuccess: () => {
      setNewMappingIssueTypeId('');
      setNewMappingWorkflowId('');
      setMessage('Mapping added');
      queryClient.invalidateQueries({ queryKey: ['workflow-admin', 'scheme', selectedId] });
    },
    onError: (e: Error) => setMessage(e.message),
  });

  const removeMapping = useMutation({
    mutationFn: (mappingId: string) => workflowApi.removeSchemeMapping(selectedId!, mappingId),
    onSuccess: () => {
      setMessage('Mapping removed');
      queryClient.invalidateQueries({ queryKey: ['workflow-admin', 'scheme', selectedId] });
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
            className="px-4 py-2 bg-avisys-blue text-white text-sm rounded disabled:opacity-50"
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
                      className="text-left text-avisys-blue hover:underline flex-1"
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

      {selectedId && schemeDetail && (
        <div className="bg-white border rounded-lg p-4 space-y-4">
          <div>
            <h2 className="font-semibold text-sm mb-1">Scheme detail</h2>
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
              <dt className="text-gray-500">Name</dt>
              <dd>{String(schemeDetail.name ?? '-')}</dd>
              <dt className="text-gray-500">Description</dt>
              <dd>{String(schemeDetail.description ?? '-')}</dd>
              <dt className="text-gray-500">Default workflow</dt>
              <dd>{String(schemeDetail.defaultWorkflowId ?? 'None')}</dd>
            </dl>
          </div>

          <div>
            <h3 className="font-semibold text-sm mb-2">Issue-type to workflow mappings</h3>
            {Array.isArray(schemeDetail.mappings) && schemeDetail.mappings.length > 0 ? (
              <table className="w-full text-sm border rounded">
                <thead>
                  <tr className="bg-gray-50 text-left">
                    <th className="px-3 py-2 border-b">Issue type</th>
                    <th className="px-3 py-2 border-b">Workflow</th>
                    <th className="px-3 py-2 border-b w-20"></th>
                  </tr>
                </thead>
                <tbody>
                  {schemeDetail.mappings.map((m: Record<string, unknown>) => {
                    const mId = String(m.id ?? '');
                    return (
                      <tr key={mId} className="border-b last:border-b-0">
                        <td className="px-3 py-2">{String(m.issueTypeName ?? m.issueTypeId ?? '-')}</td>
                        <td className="px-3 py-2">{String(m.workflowName ?? m.workflowId ?? '-')}</td>
                        <td className="px-3 py-2 text-right">
                          <button
                            type="button"
                            className="text-xs text-red-600 hover:underline"
                            disabled={removeMapping.isPending}
                            onClick={() => removeMapping.mutate(mId)}
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            ) : (
              <p className="text-sm text-gray-500">No mappings configured.</p>
            )}

            <div className="mt-3 flex items-end gap-2">
              <div>
                <label className="block text-xs text-gray-600 mb-1">Issue type ID</label>
                <input
                  className="border rounded px-2 py-1 text-sm w-48"
                  placeholder="Issue type ID"
                  value={newMappingIssueTypeId}
                  onChange={(e) => setNewMappingIssueTypeId(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-xs text-gray-600 mb-1">Workflow</label>
                <select
                  className="border rounded px-2 py-1 text-sm w-48"
                  value={newMappingWorkflowId}
                  onChange={(e) => setNewMappingWorkflowId(e.target.value)}
                >
                  <option value="">Select workflow...</option>
                  {workflows.map((w) => (
                    <option key={w.id} value={w.id}>
                      {w.name}
                    </option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                className="px-3 py-1 bg-avisys-blue text-white text-sm rounded disabled:opacity-50"
                disabled={!newMappingIssueTypeId || !newMappingWorkflowId || addMapping.isPending}
                onClick={() => addMapping.mutate()}
              >
                Add mapping
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
