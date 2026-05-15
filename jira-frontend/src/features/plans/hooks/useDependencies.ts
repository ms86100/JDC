import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { planApi } from '../../../api/planApi';

export const useDependencies = (planId: string) => {
  return useQuery({
    queryKey: ['dependencies', planId],
    queryFn: () => planApi.getDependencies(planId),
    select: (res) => res.data,
    enabled: !!planId,
  });
};

export const useCreateDependency = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, data }: { planId: string; data: Parameters<typeof planApi.createDependency>[1] }) =>
      planApi.createDependency(planId, data),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['dependencies', planId] });
    },
  });
};

export const useDeleteDependency = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, dependencyId }: { planId: string; dependencyId: string }) =>
      planApi.deleteDependency(planId, dependencyId),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['dependencies', planId] });
    },
  });
};

export const useWarnings = (planId: string) => {
  return useQuery({
    queryKey: ['warnings', planId],
    queryFn: () => planApi.getWarnings(planId),
    select: (res) => res.data,
    enabled: !!planId,
  });
};

export const useDismissWarning = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, warningId }: { planId: string; warningId: string }) =>
      planApi.dismissWarning(planId, warningId),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['warnings', planId] });
    },
  });
};
