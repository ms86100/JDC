import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { migrationApi } from '../../../api/serviceApi';
import { canWriteMigration } from '../utils/migrationRoleUtils';

export default function SavedMappingTemplatesPanel() {
  const queryClient = useQueryClient();
  const canWrite = canWriteMigration();
  const [name, setName] = useState('');
  const [mappingJson, setMappingJson] = useState('[]');

  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['migration-saved-mappings'],
    queryFn: () => migrationApi.getMappings().then((r) => r.data),
  });

  const save = useMutation({
    mutationFn: () => {
      let mappings: unknown;
      try {
        mappings = JSON.parse(mappingJson);
      } catch {
        throw new Error('Mappings must be valid JSON array');
      }
      return migrationApi.createMapping({
        mappingName: name,
        mappingType: 'FIELD',
        sourceType: 'CSV',
        targetType: 'ISSUE',
        mappings: JSON.stringify(mappings),
        isShared: true,
      });
    },
    onSuccess: () => {
      setName('');
      setMappingJson('[]');
      queryClient.invalidateQueries({ queryKey: ['migration-saved-mappings'] });
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => migrationApi.deleteMapping(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['migration-saved-mappings'] }),
  });

  return (
    <div className="bg-white rounded-lg border p-6 space-y-4" data-testid="saved-mapping-templates">
      <h2 className="text-lg font-semibold">Saved field mapping templates</h2>
      <p className="text-sm text-gray-600">
        Reusable column mappings stored via <code>/api/migration/mappings</code>.
      </p>

      {canWrite && (
        <div className="space-y-2 border rounded p-4 bg-gray-50">
          <input
            className="border rounded px-3 py-2 w-full"
            placeholder="Template name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <textarea
            className="border rounded px-3 py-2 w-full font-mono text-xs h-24"
            value={mappingJson}
            onChange={(e) => setMappingJson(e.target.value)}
          />
          <button
            type="button"
            className="px-4 py-2 bg-avisys-blue text-white rounded text-sm disabled:opacity-50"
            disabled={!name || save.isPending}
            onClick={() => save.mutate()}
          >
            Save template
          </button>
          {save.isError && (
            <p className="text-sm text-red-600">
              {save.error instanceof Error ? save.error.message : 'Save failed'}
            </p>
          )}
        </div>
      )}

      {isLoading && <p className="text-gray-500 text-sm">Loading…</p>}
      <ul className="divide-y text-sm">
        {(templates as Array<{ id?: string; mappingName?: string; mappingType?: string; createdAt?: string }>).map(
          (t) => (
            <li key={t.id} className="py-3 flex justify-between items-center gap-4">
              <span>
                <strong>{t.mappingName ?? t.id}</strong>
                <span className="text-gray-500 ml-2">{t.mappingType}</span>
              </span>
              {canWrite && t.id && (
                <button
                  type="button"
                  className="text-red-600 text-xs hover:underline"
                  onClick={() => remove.mutate(t.id!)}
                >
                  Delete
                </button>
              )}
            </li>
          ),
        )}
        {templates.length === 0 && !isLoading && (
          <li className="py-4 text-gray-500">No saved templates yet.</li>
        )}
      </ul>
    </div>
  );
}
