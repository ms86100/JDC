import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';

// Types
export interface LexoRankResponse {
  id: string;
  entityType: string;
  entityId: string;
  bucketId: number;
  rankValue: string;
  locked: boolean;
  lockedAt: string | null;
  lockedBy: string | null;
}

export interface RankItemRequest {
  itemId: string;
  beforeRank?: string;
  afterRank?: string;
  userId?: string;
}

// API functions
const lexoRankApi = {
  rankItem: (planId: string, request: RankItemRequest) =>
    apiClient.post<LexoRankResponse>(`/api/plans/${planId}/backlog/rank`, request),

  getRank: (entityType: string, entityId: string) =>
    apiClient.get<LexoRankResponse>(`/api/plans/backlog/rank/${entityType}/${entityId}`),

  lockRank: (request: RankItemRequest) =>
    apiClient.put<LexoRankResponse>('/api/plans/backlog/rank/lock', request),

  unlockRank: (request: RankItemRequest) =>
    apiClient.put<LexoRankResponse>('/api/plans/backlog/rank/unlock', request),

  validateRebalance: (planId: string, rank1: string, rank2: string) =>
    apiClient.get<boolean>(`/api/plans/${planId}/backlog/rank/validate?rank1=${rank1}&rank2=${rank2}`),

  rebalance: (planId: string) =>
    apiClient.post(`/api/plans/${planId}/backlog/rank/rebalance`),
};

// Hooks
export const useLexoRank = (entityType: string, entityId: string) => {
  return useQuery({
    queryKey: ['lexorank', entityType, entityId],
    queryFn: () => lexoRankApi.getRank(entityType, entityId),
    select: (res) => res.data,
    enabled: !!entityId,
  });
};

export const useRankItem = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ planId, request }: { planId: string; request: RankItemRequest }) =>
      lexoRankApi.rankItem(planId, request),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['lexorank'] });
      queryClient.invalidateQueries({ queryKey: ['backlog', variables.planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to rank item:', error.message);
    },
  });
};

export const useLockRank = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: lexoRankApi.lockRank,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lexorank'] });
    },
    onError: (error: Error) => {
      console.error('Failed to lock rank:', error.message);
    },
  });
};

export const useUnlockRank = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: lexoRankApi.unlockRank,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lexorank'] });
    },
    onError: (error: Error) => {
      console.error('Failed to unlock rank:', error.message);
    },
  });
};

export const useValidateRebalance = (planId: string, rank1: string, rank2: string) => {
  return useQuery({
    queryKey: ['lexorank', 'validate', planId, rank1, rank2],
    queryFn: () => lexoRankApi.validateRebalance(planId, rank1, rank2),
    select: (res) => res.data,
    enabled: !!planId && !!rank1 && !!rank2,
  });
};

export const useRebalance = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: lexoRankApi.rebalance,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['lexorank'] });
    },
    onError: (error: Error) => {
      console.error('Failed to rebalance ranks:', error.message);
    },
  });
};

export const useReorderItem = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ planId, itemId, beforeRank, afterRank }: {
      planId: string;
      itemId: string;
      beforeRank?: string;
      afterRank?: string;
    }) => {
      // Calculate the new rank between beforeRank and afterRank
      const newRank = calculateMidpoint(beforeRank, afterRank);

      return lexoRankApi.rankItem(planId, {
        itemId,
        beforeRank,
        afterRank,
        userId: undefined,
      });
    },
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['backlog', variables.planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to reorder item:', error.message);
    },
  });
};

// Helper function to calculate midpoint rank
function calculateMidpoint(before?: string, after?: string): string {
  const CHARSET = '0123456789abcdefghijklmnopqrstuvwxyz';

  if (!before && !after) {
    return CHARSET[Math.floor(CHARSET.length / 2)]; // 'i'
  }

  if (!before) {
    // Before first item
    if (!after) return CHARSET[Math.floor(CHARSET.length / 2)];
    // Prepend 0 and calculate midpoint
    return CHARSET[0] + after.charAt(0);
  }

  if (!after) {
    // After last item
    return before + CHARSET[Math.floor(CHARSET.length / 2)];
  }

  // Between two items
  const maxLen = Math.max(before.length, after.length);
  const paddedBefore = before.padEnd(maxLen, CHARSET[0]);
  const paddedAfter = after.padEnd(maxLen, CHARSET[0]);

  let result = '';
  for (let i = 0; i < maxLen; i++) {
    const beforeVal = CHARSET.indexOf(paddedBefore[i]);
    const afterVal = CHARSET.indexOf(paddedAfter[i]);

    if (beforeVal < afterVal - 1) {
      result += CHARSET[Math.floor((beforeVal + afterVal) / 2)];
      return result;
    } else {
      result += paddedBefore[i];
    }
  }

  // Adjacent - append midpoint
  return result + CHARSET[Math.floor(CHARSET.length / 2)];
}