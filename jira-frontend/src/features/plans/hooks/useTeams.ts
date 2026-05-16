import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { planApi } from '../../../api/planApi';

export const useTeams = (planId: string) => {
  return useQuery({
    queryKey: ['teams', planId],
    queryFn: () => planApi.getTeams(planId),
    select: (res) => res.data,
    enabled: !!planId,
  });
};

export const useTeam = (planId: string, teamId: string) => {
  return useQuery({
    queryKey: ['teams', planId, teamId],
    queryFn: () => planApi.getTeamById(planId, teamId),
    select: (res) => res.data,
    enabled: !!planId && !!teamId,
  });
};

export const useCreateTeam = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, data }: { planId: string; data: Parameters<typeof planApi.createTeam>[1] }) =>
      planApi.createTeam(planId, data),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['teams', planId] });
      queryClient.invalidateQueries({ queryKey: ['plans', planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to create team:', error);
      alert(error.message || 'Failed to create team');
    },
  });
};

export const useUpdateTeam = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, teamId, data }: { planId: string; teamId: string; data: Parameters<typeof planApi.updateTeam>[2] }) =>
      planApi.updateTeam(planId, teamId, data),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['teams', planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to update team:', error);
      alert(error.message || 'Failed to update team');
    },
  });
};

export const useDeleteTeam = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, teamId }: { planId: string; teamId: string }) =>
      planApi.deleteTeam(planId, teamId),
    onSuccess: (_, { planId }) => {
      queryClient.invalidateQueries({ queryKey: ['teams', planId] });
      queryClient.invalidateQueries({ queryKey: ['plans', planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete team:', error);
      alert(error.message || 'Failed to delete team');
    },
  });
};

export const useAddTeamMember = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, teamId, data }: { planId: string; teamId: string; data: Parameters<typeof planApi.addTeamMember>[2] }) =>
      planApi.addTeamMember(planId, teamId, data),
    onSuccess: (_, { planId, teamId }) => {
      queryClient.invalidateQueries({ queryKey: ['teams', planId] });
      queryClient.invalidateQueries({ queryKey: ['teams', planId, teamId] });
    },
    onError: (error: Error) => {
      console.error('Failed to add team member:', error);
      alert(error.message || 'Failed to add team member');
    },
  });
};

export const useRemoveTeamMember = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ planId, teamId, memberId }: { planId: string; teamId: string; memberId: string }) =>
      planApi.removeTeamMember(planId, teamId, memberId),
    onSuccess: (_, { planId, teamId }) => {
      queryClient.invalidateQueries({ queryKey: ['teams', planId] });
      queryClient.invalidateQueries({ queryKey: ['teams', planId, teamId] });
    },
    onError: (error: Error) => {
      console.error('Failed to remove team member:', error);
      alert(error.message || 'Failed to remove team member');
    },
  });
};
