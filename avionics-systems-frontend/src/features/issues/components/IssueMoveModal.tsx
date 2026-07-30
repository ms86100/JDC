import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { projectApi, Project } from '../../../api/issueApi';

interface IssueMoveModalProps {
  currentProjectId: string;
  onClose: () => void;
  onMove: (targetProjectId: string) => void;
  isPending?: boolean;
}

export default function IssueMoveModal({
  currentProjectId,
  onClose,
  onMove,
  isPending,
}: IssueMoveModalProps) {
  const [targetProjectId, setTargetProjectId] = useState('');

  const { data: projects = [] } = useQuery({
    queryKey: ['projects-for-move'],
    queryFn: async () => {
      const res = await projectApi.getAll();
      return res.data?.content ?? [];
    },
  });

  return (
    <div className="ab-modal-overlay" onClick={onClose}>
      <div className="ab-modal ab-modal-sm" onClick={(e) => e.stopPropagation()}>
        <div className="ab-modal-header">
          <h2>Move issue</h2>
          <button type="button" className="ab-modal-close" onClick={onClose}>×</button>
        </div>
        <div className="ab-modal-body">
          <label className="ab-label">Target project</label>
          <select
            className="ab-input"
            value={targetProjectId}
            onChange={(e) => setTargetProjectId(e.target.value)}
          >
            <option value="">Select project…</option>
            {projects
              .filter((p) => p.id !== currentProjectId)
              .map((p) => (
                <option key={p.id} value={p.id}>
                  {p.key} — {p.name}
                </option>
              ))}
          </select>
        </div>
        <div className="ab-modal-footer">
          <button type="button" className="ab-btn ab-btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-primary"
            disabled={!targetProjectId || isPending}
            onClick={() => onMove(targetProjectId)}
          >
            {isPending ? 'Moving…' : 'Move'}
          </button>
        </div>
      </div>
    </div>
  );
}
