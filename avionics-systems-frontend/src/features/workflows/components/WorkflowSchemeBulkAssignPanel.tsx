import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useQuery } from '@tanstack/react-query';
import { workflowApi } from '../../../api/workflowApi';
import { projectApi } from '../../../api/projectApi';

interface Props {
  schemeId: string;
  schemeName: string;
}

export default function WorkflowSchemeBulkAssignPanel({ schemeId, schemeName }: Props) {
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const { data: projects = [] } = useQuery({
    queryKey: ['projects-bulk-assign'],
    queryFn: async () => {
      const res = await projectApi.getAll();
      return res.data?.content ?? res.data ?? [];
    },
  });

  const assign = useMutation({
    mutationFn: () => workflowApi.assignSchemeBulk(schemeId, Array.from(selected)),
  });

  const toggle = (id: string) => {
    const next = new Set(selected);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setSelected(next);
  };

  return (
    <div className="wf-panel" style={{ marginTop: 16 }}>
      <h3>Bulk assign scheme: {schemeName}</h3>
      <p className="wf-muted">Select projects to receive this workflow scheme.</p>
      <ul style={{ maxHeight: 200, overflow: 'auto', listStyle: 'none', padding: 0 }}>
        {(projects as Array<{ id: string; name?: string; projectKey?: string }>).map((p) => (
          <li key={p.id} style={{ padding: '4px 0' }}>
            <label style={{ display: 'flex', gap: 8, cursor: 'pointer' }}>
              <input type="checkbox" checked={selected.has(p.id)} onChange={() => toggle(p.id)} />
              <span>
                {p.projectKey} — {p.name}
              </span>
            </label>
          </li>
        ))}
      </ul>
      <button
        type="button"
        className="ab-btn ab-btn-primary ab-btn-sm"
        style={{ marginTop: 8 }}
        disabled={selected.size === 0 || assign.isPending}
        onClick={() => assign.mutate()}
      >
        {assign.isPending ? 'Assigning…' : `Assign to ${selected.size} project(s)`}
      </button>
      {assign.isSuccess && (
        <p className="wf-muted" style={{ marginTop: 8 }}>
          Updated {(assign.data?.data as { updatedProjects?: number })?.updatedProjects ?? selected.size} project(s).
        </p>
      )}
    </div>
  );
}
