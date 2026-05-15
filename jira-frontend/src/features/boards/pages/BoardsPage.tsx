import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sprintApi, SprintResponse } from '../../../api/sprintApi';
import EnhancedKanbanBoard from '../components/EnhancedKanbanBoard';

const BoardsPage: React.FC = () => {
  const [view, setView] = useState<'board' | 'sprint'>('sprint');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [form, setForm] = useState({ name: '', goal: '', startDate: '', endDate: '' });
  const queryClient = useQueryClient();

  const { data: sprints = [], isLoading } = useQuery<SprintResponse[]>({
    queryKey: ['sprints'],
    queryFn: async () => {
      return await sprintApi.getAll();
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: { name: string; goal?: string; startDate?: string; endDate?: string; projectId: string }) =>
      sprintApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
      setShowCreateForm(false);
      setForm({ name: '', goal: '', startDate: '', endDate: '' });
    },
  });

  const startMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.start(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
  });

  const completeMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.complete(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.delete(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints'] });
    },
  });

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return 'Not set';
    return new Date(dateStr).toLocaleDateString();
  };

  const activeSprints = sprints?.filter(s => s.status === 'ACTIVE') || [];
  const planningSprints = sprints?.filter(s => s.status === 'PLANNING') || [];
  const completedSprints = sprints?.filter(s => s.status === 'COMPLETED') || [];

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Agile Boards</h1>
          <p className="text-gray-500 mt-1">Manage sprints, kanban boards, and track team velocity</p>
        </div>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="px-4 py-2 bg-jira-blue text-white rounded hover:bg-blue-600"
        >
          Create Sprint
        </button>
      </div>

      {showCreateForm && (
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h3 className="text-lg font-semibold mb-4">Create New Sprint</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sprint Name *</label>
              <input
                type="text"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full border rounded px-3 py-2"
                placeholder="e.g., Sprint 1"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Sprint Goal</label>
              <input
                type="text"
                value={form.goal}
                onChange={(e) => setForm({ ...form, goal: e.target.value })}
                className="w-full border rounded px-3 py-2"
                placeholder="What do you want to achieve?"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Start Date</label>
              <input
                type="date"
                value={form.startDate}
                onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                className="w-full border rounded px-3 py-2"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">End Date</label>
              <input
                type="date"
                value={form.endDate}
                onChange={(e) => setForm({ ...form, endDate: e.target.value })}
                className="w-full border rounded px-3 py-2"
              />
            </div>
          </div>
          <div className="flex justify-end gap-3 mt-4">
            <button
              onClick={() => setShowCreateForm(false)}
              className="px-4 py-2 border rounded hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onClick={() => createMutation.mutate({
                ...form,
                projectId: 'default-project',
              })}
              disabled={!form.name || createMutation.isPending}
              className="px-4 py-2 bg-jira-blue text-white rounded hover:bg-blue-600 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Sprint'}
            </button>
          </div>
        </div>
      )}

      {/* View Toggle */}
      <div className="flex gap-4 mb-6">
        <button
          className={`px-4 py-2 rounded ${view === 'sprint' ? 'bg-jira-blue text-white' : 'bg-gray-100'}`}
          onClick={() => setView('sprint')}
        >
          Sprint Management
        </button>
        <button
          className={`px-4 py-2 rounded ${view === 'board' ? 'bg-jira-blue text-white' : 'bg-gray-100'}`}
          onClick={() => setView('board')}
        >
          Board View
        </button>
      </div>

      {view === 'board' ? (
        <div className="bg-white rounded-lg shadow" style={{ height: 'calc(100vh - 300px)' }}>
          <EnhancedKanbanBoard />
        </div>
      ) : (
        <div className="space-y-6">
          {/* Active Sprints */}
          <div>
            <div className="flex items-center gap-2 mb-3">
              <span className="w-3 h-3 bg-green-500 rounded-full"></span>
              <h3 className="font-semibold text-gray-700">Active Sprints ({activeSprints.length})</h3>
            </div>
            <div className="space-y-3">
              {activeSprints.length === 0 ? (
                <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
                  No active sprint. Start one from the planning section below.
                </div>
              ) : (
                activeSprints.map((sprint) => (
                  <div key={sprint.id} className="bg-white rounded-lg shadow border-l-4 border-green-500">
                    <div className="px-6 py-4">
                      <div className="flex justify-between items-start">
                        <div>
                          <div className="flex items-center gap-2">
                            <h4 className="font-semibold text-lg">{sprint.name}</h4>
                            <span className="px-2 py-0.5 bg-green-100 text-green-800 text-xs rounded-full">
                              ACTIVE
                            </span>
                          </div>
                          <p className="text-gray-500 text-sm mt-1">{sprint.goal || 'No goal set'}</p>
                        </div>
                        <div className="flex gap-2">
                          <button
                            onClick={() => completeMutation.mutate(sprint.id)}
                            className="px-3 py-1 border rounded text-sm hover:bg-gray-50"
                          >
                            Complete Sprint
                          </button>
                        </div>
                      </div>
                      <div className="flex gap-6 mt-4 text-sm text-gray-600">
                        <span>📅 {formatDate(sprint.startDate)} → {formatDate(sprint.endDate)}</span>
                        <span>📊 {sprint.issueCount || 0} issues</span>
                        <span>✓ {sprint.completedIssueCount || 0} completed</span>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Planning Sprints */}
          <div>
            <div className="flex items-center gap-2 mb-3">
              <span className="w-3 h-3 bg-yellow-500 rounded-full"></span>
              <h3 className="font-semibold text-gray-700">Planning ({planningSprints.length})</h3>
            </div>
            <div className="space-y-3">
              {planningSprints.length === 0 ? (
                <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
                  No sprints in planning. Click "Create Sprint" to create one.
                </div>
              ) : (
                planningSprints.map((sprint) => (
                  <div key={sprint.id} className="bg-white rounded-lg shadow">
                    <div className="px-6 py-4">
                      <div className="flex justify-between items-start">
                        <div>
                          <div className="flex items-center gap-2">
                            <h4 className="font-semibold text-lg">{sprint.name}</h4>
                            <span className="px-2 py-0.5 bg-yellow-100 text-yellow-800 text-xs rounded-full">
                              PLANNING
                            </span>
                          </div>
                          <p className="text-gray-500 text-sm mt-1">{sprint.goal || 'No goal set'}</p>
                        </div>
                        <div className="flex gap-2">
                          <button
                            onClick={() => startMutation.mutate(sprint.id)}
                            className="px-3 py-1 bg-green-600 text-white rounded text-sm hover:bg-green-700"
                          >
                            Start Sprint
                          </button>
                          <button
                            onClick={() => deleteMutation.mutate(sprint.id)}
                            className="px-3 py-1 text-red-600 hover:bg-red-50 rounded text-sm"
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                      <div className="flex gap-6 mt-4 text-sm text-gray-600">
                        <span>📅 {formatDate(sprint.startDate)} → {formatDate(sprint.endDate)}</span>
                        <span>📊 {sprint.issueCount || 0} issues</span>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Completed Sprints */}
          {completedSprints.length > 0 && (
            <div>
              <div className="flex items-center gap-2 mb-3">
                <span className="w-3 h-3 bg-gray-400 rounded-full"></span>
                <h3 className="font-semibold text-gray-700">Completed ({completedSprints.length})</h3>
              </div>
              <div className="space-y-3">
                {completedSprints.slice(0, 5).map((sprint) => (
                  <div key={sprint.id} className="bg-white rounded-lg shadow opacity-75">
                    <div className="px-6 py-4">
                      <div className="flex justify-between items-start">
                        <div>
                          <div className="flex items-center gap-2">
                            <h4 className="font-semibold text-lg">{sprint.name}</h4>
                            <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full">
                              COMPLETED
                            </span>
                          </div>
                        </div>
                        <div className="flex gap-4 text-sm text-gray-500">
                          <span>{sprint.completedIssueCount || 0}/{sprint.issueCount || 0} issues</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default BoardsPage;