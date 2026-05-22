import { useCallback, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { planApi, type PlanItemResponse } from '../../../api/planApi';

export type PlanDraftChangeType = 'target_start' | 'target_end' | 'summary' | 'add_item';

export interface PlanDraftChange {
  id: string;
  itemId: string;
  issueKey?: string;
  type: PlanDraftChangeType;
  value: string;
  previousValue?: string;
}

function changeKey(itemId: string, type: PlanDraftChangeType) {
  return `${itemId}:${type}`;
}

export function usePlanDraftChanges(planId: string) {
  const [changes, setChanges] = useState<PlanDraftChange[]>([]);
  const queryClient = useQueryClient();

  const pendingCount = changes.length;

  const addChange = useCallback((change: Omit<PlanDraftChange, 'id'>) => {
    const id = changeKey(change.itemId, change.type);
    setChanges((prev) => {
      const filtered = prev.filter((c) => c.id !== id);
      return [...filtered, { ...change, id }];
    });
  }, []);

  const discard = useCallback(() => setChanges([]), []);

  const commit = useCallback(
    async (getItem: (itemId: string) => PlanItemResponse | undefined) => {
      const byItem = new Map<string, { start?: string; end?: string }>();
      for (const ch of changes) {
        const cur = byItem.get(ch.itemId) ?? {};
        if (ch.type === 'target_start') cur.start = ch.value;
        if (ch.type === 'target_end') cur.end = ch.value;
        byItem.set(ch.itemId, cur);
      }
      for (const [itemId, dates] of byItem) {
        const item = getItem(itemId);
        if (!item) continue;
        await planApi.updateBacklogItem(planId, itemId, {
          issueId: item.issueId,
          issueType: item.issueType,
          targetDate: dates.start ?? item.targetDate?.slice(0, 10),
          targetEndDate: dates.end ?? item.targetEndDate?.slice(0, 10),
        });
      }
      setChanges([]);
      await queryClient.invalidateQueries({ queryKey: ['backlog', planId] });
      await queryClient.invalidateQueries({ queryKey: ['plan', planId] });
    },
    [changes, planId, queryClient],
  );

  const getDraftValue = useCallback(
    (itemId: string, type: PlanDraftChangeType): string | undefined => {
      return changes.find((c) => c.id === changeKey(itemId, type))?.value;
    },
    [changes],
  );

  const viewLabel = useMemo(() => (pendingCount > 0 ? 'EDITED' : 'Basic'), [pendingCount]);

  return {
    pendingCount,
    changes,
    addChange,
    discard,
    commit,
    getDraftValue,
    viewLabel,
    hasPending: pendingCount > 0,
  };
}
