import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { planApi } from '../../../api/planApi';
import { appNotify } from '../../../lib/appNotify';
import { asArray } from '../../../utils/apiList';

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
    onError: (error: Error) => {
      console.error('Failed to create program:', error);
      appNotify.error(error.message || 'Failed to create program');
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
    onError: (error: Error) => {
      console.error('Failed to update program:', error);
      appNotify.error(error.message || 'Failed to update program');
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
    onError: (error: Error) => {
      console.error('Failed to delete program:', error);
      appNotify.error(error.message || 'Failed to delete program');
    },
  });
};

export const usePlans = () => {
  return useQuery({
    queryKey: ['plans'],
    queryFn: async () => {
      const res = await planApi.getPlans();
      return asArray(res.data);
    },
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
    onError: (error: Error) => {
      console.error('Failed to create plan:', error);
      appNotify.error(error.message || 'Failed to create plan');
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
    onError: (error: Error) => {
      console.error('Failed to update plan:', error);
      appNotify.error(error.message || 'Failed to update plan');
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
    onError: (error: Error) => {
      console.error('Failed to delete plan:', error);
      appNotify.error(error.message || 'Failed to delete plan');
    },
  });
};
