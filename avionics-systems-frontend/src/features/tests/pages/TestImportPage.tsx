import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ImportPanel } from '../components/ImportPanel';
import { importApi, ImportBatchResponse } from '../../../api/importApi';

export default function TestImportPage() {
  const { projectId: routeProjectId } = useParams<{ projectId?: string }>();
  const [projectId, setProjectId] = useState(routeProjectId || '');
  const activeProjectId = routeProjectId || projectId;

  const { data: history = [], refetch: refetchHistory } = useQuery<ImportBatchResponse[]>({
    queryKey: ['import-history', activeProjectId],
    queryFn: async () => {
      const res = await importApi.getHistory(activeProjectId);
      return res.data;
    },
    enabled: !!activeProjectId,
  });

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold mb-2">Import Tests</h1>
      <p className="text-sm text-gray-500 mb-4">
        Import test results from Cucumber, JUnit, TestNG, NUnit, or Robot Framework. View batch history per project.
      </p>
      {!routeProjectId && (
        <input
          className="w-full max-w-md border rounded px-3 py-2 text-sm mb-4"
          placeholder="Project UUID"
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
        />
      )}
      {activeProjectId ? (
        <>
          <ImportPanel projectId={activeProjectId} onImportComplete={() => refetchHistory()} />
          <section className="mt-8">
            <h2 className="text-lg font-semibold mb-3">Import history</h2>
            {history.length === 0 ? (
              <p className="text-sm text-gray-500">No import batches yet for this project.</p>
            ) : (
              <div className="border rounded-lg overflow-hidden bg-white">
                <table className="w-full text-sm">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      <th className="text-left p-2">Type</th>
                      <th className="text-left p-2">Status</th>
                      <th className="text-left p-2">Tests</th>
                      <th className="text-left p-2">Started</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((batch) => (
                      <tr key={batch.id} className="border-b last:border-0">
                        <td className="p-2">{batch.importType || '—'}</td>
                        <td className="p-2">{batch.status || '—'}</td>
                        <td className="p-2">
                          {batch.testsCreated ?? 0} created · {batch.executionsCreated ?? 0} runs
                        </td>
                        <td className="p-2 text-gray-500">
                          {batch.startedAt
                            ? new Date(batch.startedAt).toLocaleString()
                            : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      ) : (
        <p className="text-gray-500 text-sm">Enter a project ID to import tests.</p>
      )}
    </div>
  );
}
