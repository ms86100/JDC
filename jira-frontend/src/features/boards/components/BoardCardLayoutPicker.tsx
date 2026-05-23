import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { boardFieldApi } from '../../../api/fieldApi';
import { appNotify } from '../../../lib/appNotify';

interface Props {
  boardId: string;
  projectId?: string;
}

export default function BoardCardLayoutPicker({ boardId, projectId }: Props) {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['board-card-layout', boardId, projectId],
    queryFn: () => boardFieldApi.getCardLayout(boardId, projectId).then((r) => r.data),
    enabled: !!boardId,
  });

  const [selected, setSelected] = useState<string[]>([]);

  useEffect(() => {
    if (data?.selectedFields) {
      setSelected(data.selectedFields.map((f) => f.fieldKey));
    }
  }, [data?.selectedFields]);

  const saveMutation = useMutation({
    mutationFn: () =>
      boardFieldApi.saveCardLayout(boardId, {
        projectId,
        fields: selected.map((fieldKey, i) => ({ fieldKey, displayOrder: i })),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board-card-layout', boardId] });
      queryClient.invalidateQueries({ queryKey: ['board-card-values', boardId] });
      appNotify.success('Card layout saved');
    },
    onError: () => appNotify.error('Failed to save card layout'),
  });

  const toggle = (fieldKey: string) => {
    setSelected((prev) =>
      prev.includes(fieldKey) ? prev.filter((k) => k !== fieldKey) : [...prev, fieldKey],
    );
  };

  if (isLoading) {
    return <p className="ab-help-text">Loading eligible custom fields…</p>;
  }

  const eligible = data?.eligibleFields ?? [];

  return (
    <div className="ab-config-section">
      <p className="ab-help-text" style={{ marginBottom: 12 }}>
        Custom fields do not appear on cards until you select them here (Jira DC board configuration).
        Only fields valid for this project&apos;s contexts are listed.
      </p>
      {eligible.length === 0 ? (
        <p className="ab-help-text">
          No eligible custom fields. Create fields in Admin → Custom fields or import via Migration.
        </p>
      ) : (
        <ul className="ab-card-layout-list">
          {eligible.map((f) => (
            <li key={f.fieldKey}>
              <label className="ab-card-layout-option">
                <input
                  type="checkbox"
                  checked={selected.includes(f.fieldKey)}
                  onChange={() => toggle(f.fieldKey)}
                />
                <span>
                  <strong>{f.displayName}</strong>
                  <span className="ab-field-key-hint">{f.fieldKey}</span>
                </span>
              </label>
            </li>
          ))}
        </ul>
      )}
      <button
        type="button"
        className="ab-btn ab-btn-primary"
        style={{ marginTop: 12 }}
        disabled={saveMutation.isPending}
        onClick={() => saveMutation.mutate()}
      >
        {saveMutation.isPending ? 'Saving…' : 'Save card fields'}
      </button>
      <style>{`
        .ab-card-layout-list { list-style: none; padding: 0; margin: 0; max-height: 280px; overflow-y: auto; }
        .ab-card-layout-option { display: flex; gap: 8px; align-items: flex-start; padding: 6px 0; cursor: pointer; }
        .ab-field-key-hint { display: block; font-size: 11px; color: #6b778c; font-family: monospace; }
      `}</style>
    </div>
  );
}
