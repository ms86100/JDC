import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { dashboardFieldApi } from '../../../api/fieldApi';
import { appNotify } from '../../../lib/appNotify';

interface Props {
  gadgetKey: string;
  projectId?: string;
  onSaved?: () => void;
}

export default function DashboardGadgetFieldPicker({ gadgetKey, projectId, onSaved }: Props) {
  const [open, setOpen] = useState(false);
  const { data, refetch } = useQuery({
    queryKey: ['dashboard-gadget-config', gadgetKey, projectId],
    queryFn: () =>
      dashboardFieldApi.getGadget(gadgetKey, { projectId }).then((r) => r.data),
    enabled: open,
  });

  const [selected, setSelected] = useState<string[]>([]);

  useEffect(() => {
    if (data?.configuredFields) {
      setSelected(data.configuredFields.map((f) => f.fieldKey));
    }
  }, [data?.configuredFields]);

  const saveMutation = useMutation({
    mutationFn: () =>
      dashboardFieldApi.saveGadget(
        gadgetKey,
        {
          dashboardKey: 'system',
          fields: selected.map((fieldKey, i) => ({ fieldKey, displayOrder: i })),
        },
        projectId,
      ),
    onSuccess: () => {
      appNotify.success('Gadget fields saved');
      setOpen(false);
      onSaved?.();
      refetch();
    },
    onError: () => appNotify.error('Failed to save gadget configuration'),
  });

  const toggle = (key: string) => {
    setSelected((prev) => {
      if (gadgetKey === 'custom-field-chart') {
        return prev.includes(key) ? [] : [key];
      }
      return prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key];
    });
  };

  return (
    <>
      <button
        type="button"
        className="jdc-gadget-config-btn"
        title="Configure gadget fields"
        onClick={() => setOpen(true)}
      >
        ⚙
      </button>
      {open && (
        <div className="jdc-gadget-modal-overlay" onClick={() => setOpen(false)}>
          <div className="jdc-gadget-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Configure gadget: {gadgetKey}</h3>
            <p style={{ fontSize: 12, color: '#6b778c' }}>
              Only custom fields compatible with this gadget type are shown.
            </p>
            <ul style={{ listStyle: 'none', padding: 0, maxHeight: 240, overflow: 'auto' }}>
              {(data?.eligibleFields ?? []).map((f) => (
                <li key={f.fieldKey} style={{ marginBottom: 6 }}>
                  <label style={{ display: 'flex', gap: 8, cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={selected.includes(f.fieldKey)}
                      onChange={() => toggle(f.fieldKey)}
                    />
                    {f.displayName}
                  </label>
                </li>
              ))}
            </ul>
            <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
              <button type="button" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
                Save
              </button>
              <button type="button" onClick={() => setOpen(false)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
