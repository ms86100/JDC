import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { planApi } from '../../../api/planApi';

export const useBacklog = (planId: string) => {
  return useQuery({
    queryKey: ['backlog', planId],
    queryFn: () => planApi.getBacklog(planId),
    select: (res) => res.data,
    enabled: !!planId,
  });
};

export const useAddItemToBacklog = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, data }: { planId: string; data: Parameters<typeof planApi.addItemToBacklog>[1] }) =>
      planApi.addItemToBacklog(planId, data),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['backlog', planId] });
      queryClient.invalidateQueries({ queryKey: ['plans', planId] });
    },
  });
};

export const useUpdateBacklogItem = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, itemId, data }: { planId: string; itemId: string; data: Parameters<typeof planApi.updateBacklogItem>[2] }) =>
      planApi.updateBacklogItem(planId, itemId, data),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['backlog', planId] });
    },
  });
};

export const useRemoveItemFromBacklog = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, itemId }: { planId: string; itemId: string }) =>
      planApi.removeItemFromBacklog(planId, itemId),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['backlog', planId] });
      queryClient.invalidateQueries({ queryKey: ['plans', planId] });
    },
  });
};

export const useReorderBacklog = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, data }: { planId: string; data: Parameters<typeof planApi.reorderBacklog>[1] }) =>
      planApi.reorderBacklog(planId, data),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['backlog', planId] });
    },
  });
};
