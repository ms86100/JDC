import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { projectApi } from '../../../api/projectApi';
import ScreenConfigPage from './ScreenConfigPage';

/**
 * Routes: /tests/screen-config and /tests/screen-config/:projectId
 */
export default function TestScreenConfigHubPage() {
  const { projectId } = useParams<{ projectId?: string }>();
  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: () => projectApi.getAll(),
  });

  if (projectId) {
    return (
      <div className="p-6">
        <div className="mb-4">
          <Link to="/tests/screen-config" className="text-sm text-jira-blue hover:underline">
            ← All projects
          </Link>
        </div>
        <ScreenConfigPage projectId={projectId} />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto p-6" data-testid="test-screen-config-hub">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Test screen configuration</h1>
      <p className="text-sm text-gray-600 mb-6">
        Configure screen schemes, screens, and custom fields for test execution. Select a project to continue.
      </p>
      {isLoading ? (
        <p className="text-gray-500">Loading projects…</p>
      ) : (
        <ul className="bg-white border rounded-lg divide-y">
          {projects.map((p: { id: string; name: string; key?: string }) => (
            <li key={p.id}>
              <Link
                to={`/tests/screen-config/${p.id}`}
                className="block px-4 py-3 hover:bg-gray-50 text-sm font-medium text-jira-blue"
              >
                {p.name}
                {p.key ? <span className="text-gray-500 ml-2">({p.key})</span> : null}
              </Link>
            </li>
          ))}
          {projects.length === 0 && (
            <li className="px-4 py-6 text-sm text-gray-500">No projects available.</li>
          )}
        </ul>
      )}
    </div>
  );
}
