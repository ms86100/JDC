import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { epicApi, EpicResponse, CreateEpicRequest } from '../../../api/epicApi';

const STATUS_COLORS: Record<string, string> = {
  OPEN: '#0052CC',
  IN_PROGRESS: '#FF991F',
  COMPLETE: '#36B37E',
};

export default function EpicsPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  const [form, setForm] = useState<CreateEpicRequest>({ name: '', summary: '', color: '#0052CC' });

  const { data: epics = [], isLoading } = useQuery({
    queryKey: ['epics', statusFilter],
    queryFn: async () => {
      const res = await epicApi.getAll(statusFilter ? { status: statusFilter } : undefined);
      return Array.isArray(res.data) ? res.data : [];
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateEpicRequest) => epicApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['epics'] });
      setShowCreate(false);
      setForm({ name: '', summary: '', color: '#0052CC' });
    },
  });

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Epics</h1>
          <p className="text-sm text-gray-500 mt-1">Manage epics and track progress across linked issues.</p>
        </div>
        <button
          type="button"
          className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700"
          onClick={() => setShowCreate(!showCreate)}
        >
          {showCreate ? 'Cancel' : '+ Create Epic'}
        </button>
      </div>

      <div className="flex gap-3 mb-4">
        <select
          className="border rounded-md px-3 py-2 text-sm"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="">All statuses</option>
          <option value="OPEN">Open</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="COMPLETE">Complete</option>
        </select>
      </div>

      {showCreate && (
        <div className="border rounded-lg p-4 mb-6 bg-white shadow-sm">
          <h2 className="font-semibold mb-3">New epic</h2>
          <div className="grid gap-3 max-w-md">
            <input
              className="border rounded px-3 py-2 text-sm"
              placeholder="Epic name *"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
            <input
              className="border rounded px-3 py-2 text-sm"
              placeholder="Summary"
              value={form.summary || ''}
              onChange={(e) => setForm({ ...form, summary: e.target.value })}
            />
            <button
              type="button"
              className="px-4 py-2 bg-blue-600 text-white rounded text-sm disabled:opacity-50"
              disabled={!form.name.trim() || createMutation.isPending}
              onClick={() => createMutation.mutate(form)}
            >
              {createMutation.isPending ? 'Creating…' : 'Create'}
            </button>
          </div>
        </div>
      )}

      {isLoading ? (
        <p className="text-gray-500">Loading epics…</p>
      ) : epics.length === 0 ? (
        <div className="border rounded-lg p-8 text-center text-gray-500 bg-gray-50">
          No epics yet. Create one to group related work.
        </div>
      ) : (
        <div className="grid gap-3">
          {epics.map((epic: EpicResponse) => (
            <Link
              key={epic.id}
              to={`/epics/${epic.id}`}
              className="block border rounded-lg p-4 bg-white hover:border-blue-400 hover:shadow-sm transition"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-center gap-3 min-w-0">
                  <span
                    className="w-3 h-3 rounded-full shrink-0"
                    style={{ background: epic.color || STATUS_COLORS.OPEN }}
                  />
                  <div className="min-w-0">
                    <h3 className="font-semibold text-gray-900 truncate">{epic.name}</h3>
                    {epic.summary && (
                      <p className="text-sm text-gray-500 truncate">{epic.summary}</p>
                    )}
                  </div>
                </div>
                <span className="text-xs font-medium px-2 py-1 rounded bg-gray-100 text-gray-700 shrink-0">
                  {epic.status || 'OPEN'}
                </span>
              </div>
              {epic.progressPercentage != null && (
                <div className="mt-3">
                  <div className="flex justify-between text-xs text-gray-500 mb-1">
                    <span>Progress</span>
                    <span>{Number(epic.progressPercentage).toFixed(0)}%</span>
                  </div>
                  <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-blue-500 rounded-full"
                      style={{ width: `${Math.min(100, Number(epic.progressPercentage))}%` }}
                    />
                  </div>
                </div>
              )}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
