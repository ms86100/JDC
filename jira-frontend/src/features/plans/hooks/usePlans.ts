import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { planApi } from '../../../api/planApi';

export const usePrograms = () => {
  return useQuery({
    queryKey: ['programs'],
    queryFn: () => planApi.getPrograms(),
    select: (res) => res.data,
  });
};

export const useProgram = (id: string) => {
  return useQuery({
    queryKey: ['programs', id],
    queryFn: () => planApi.getProgramById(id),
    select: (res) => res.data,
    enabled: !!id,
  });
};

export const useCreateProgram = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: planApi.createProgram,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['programs'] });
    },
  });
};

export const useUpdateProgram = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Parameters<typeof planApi.updateProgram>[1] }) =>
      planApi.updateProgram(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['programs'] });
      queryClient.invalidateQueries({ queryKey: ['programs', id] });
    },
  });
};

export const useDeleteProgram = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: planApi.deleteProgram,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['programs'] });
    },
  });
};

export const usePlans = () => {
  return useQuery({
    queryKey: ['plans'],
    queryFn: () => planApi.getPlans(),
    select: (res) => res.data,
  });
};

export const usePlan = (id: string) => {
  return useQuery({
    queryKey: ['plans', id],
    queryFn: () => planApi.getPlanById(id),
    select: (res) => res.data,
    enabled: !!id,
  });
};

export const useCreatePlan = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: planApi.createPlan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plans'] });
    },
  });
};

export const useUpdatePlan = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Parameters<typeof planApi.updatePlan>[1] }) =>
      planApi.updatePlan(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['plans'] });
      queryClient.invalidateQueries({ queryKey: ['plans', id] });
    },
  });
};

export const useDeletePlan = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: planApi.deletePlan,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plans'] });
    },
  });
};
