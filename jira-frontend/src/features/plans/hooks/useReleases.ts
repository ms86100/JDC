import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { planApi } from '../../../api/planApi';

export const useReleases = (planId: string) => {
  return useQuery({
    queryKey: ['releases', planId],
    queryFn: () => planApi.getReleases(planId),
    select: (res) => res.data,
    enabled: !!planId,
  });
};

export const useCreateRelease = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, data }: { planId: string; data: Parameters<typeof planApi.createRelease>[1] }) =>
      planApi.createRelease(planId, data),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['releases', planId] });
      queryClient.invalidateQueries({ queryKey: ['plans', planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to create release:', error);
      alert(error.message || 'Failed to create release');
    },
  });
};

export const useUpdateRelease = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, releaseId, data }: { planId: string; releaseId: string; data: Parameters<typeof planApi.updateRelease>[2] }) =>
      planApi.updateRelease(planId, releaseId, data),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['releases', planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to update release:', error);
      alert(error.message || 'Failed to update release');
    },
  });
};

export const useApproveRelease = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, releaseId, approvedBy }: { planId: string; releaseId: string; approvedBy: string }) =>
      planApi.approveRelease(planId, releaseId, approvedBy),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['releases', planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to approve release:', error);
      alert(error.message || 'Failed to approve release');
    },
  });
};

export const useReleaseVersion = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, releaseId }: { planId: string; releaseId: string }) =>
      planApi.releaseVersion(planId, releaseId),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['releases', planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to release version:', error);
      alert(error.message || 'Failed to release');
    },
  });
};

export const useDeleteRelease = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, releaseId }: { planId: string; releaseId: string }) =>
      planApi.deleteRelease(planId, releaseId),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['releases', planId] });
      queryClient.invalidateQueries({ queryKey: ['plans', planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete release:', error);
      alert(error.message || 'Failed to delete release');
    },
  });
};
