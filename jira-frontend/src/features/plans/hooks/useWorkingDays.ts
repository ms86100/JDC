import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';

// Types
export interface WorkingDaysResponse {
  id: string;
  name: string;
  monday: boolean;
  tuesday: boolean;
  wednesday: boolean;
  thursday: boolean;
  friday: boolean;
  saturday: boolean;
  sunday: boolean;
  hoursPerDay: number;
  isDefault: boolean;
  workingDaysPerWeek: number;
  holidays: NonWorkingDayResponse[];
}

export interface NonWorkingDayResponse {
  id: string;
  date: string;
  name: string;
}

export interface TeamAvailabilityResponse {
  id: string;
  teamId: string;
  userId: string | null;
  date: string;
  hours: number | null;
  reason: string | null;
}

export interface CapacityResponse {
  teamId: string;
  startDate: string;
  endDate: string;
  workingDays: number;
  totalCapacityHours: number;
  totalTimeOffHours: number;
  netCapacityHours: number;
  memberCount: number;
}

export interface CreateWorkingDaysRequest {
  name: string;
  monday?: boolean;
  tuesday?: boolean;
  wednesday?: boolean;
  thursday?: boolean;
  friday?: boolean;
  saturday?: boolean;
  sunday?: boolean;
  hoursPerDay?: number;
  isDefault?: boolean;
}

export interface CreateNonWorkingDayRequest {
  date: string;
  name: string;
}

export interface CreateTeamAvailabilityRequest {
  userId?: string;
  date: string;
  hours?: number;
  reason?: string;
}

// API functions
const workingDaysApi = {
  getAll: () => apiClient.get<WorkingDaysResponse[]>('/api/plans/working-days'),
  getDefault: () => apiClient.get<WorkingDaysResponse>('/api/plans/working-days/default'),
  getById: (id: string) => apiClient.get<WorkingDaysResponse>(`/api/plans/working-days/${id}`),
  create: (data: CreateWorkingDaysRequest) =>
    apiClient.post<WorkingDaysResponse>('/api/plans/working-days', data),
  update: (id: string, data: CreateWorkingDaysRequest) =>
    apiClient.put<WorkingDaysResponse>(`/api/plans/working-days/${id}`, data),
  delete: (id: string) => apiClient.delete(`/api/plans/working-days/${id}`),

  getHolidays: (configId: string) =>
    apiClient.get<NonWorkingDayResponse[]>(`/api/plans/working-days/${configId}/holidays`),
  addHoliday: (configId: string, data: CreateNonWorkingDayRequest) =>
    apiClient.post<NonWorkingDayResponse>(`/api/plans/working-days/${configId}/holidays`, data),
  removeHoliday: (configId: string, holidayId: string) =>
    apiClient.delete(`/api/plans/working-days/${configId}/holidays/${holidayId}`),

  getTeamAvailability: (teamId: string, start: string, end: string) =>
    apiClient.get<TeamAvailabilityResponse[]>(`/api/plans/teams/${teamId}/availability?start=${start}&end=${end}`),
  setTeamAvailability: (teamId: string, data: CreateTeamAvailabilityRequest) =>
    apiClient.post<TeamAvailabilityResponse>(`/api/plans/teams/${teamId}/availability`, data),

  getTeamCapacity: (teamId: string, start: string, end: string) =>
    apiClient.get<CapacityResponse>(`/api/plans/teams/${teamId}/capacity?start=${start}&end=${end}`),
};

// Hooks
export const useWorkingDays = () => {
  return useQuery({
    queryKey: ['workingDays'],
    queryFn: workingDaysApi.getAll,
    select: (res) => res.data,
  });
};

export const useWorkingDaysConfig = (id: string) => {
  return useQuery({
    queryKey: ['workingDays', id],
    queryFn: () => workingDaysApi.getById(id),
    select: (res) => res.data,
    enabled: !!id,
  });
};

export const useDefaultWorkingDays = () => {
  return useQuery({
    queryKey: ['workingDays', 'default'],
    queryFn: workingDaysApi.getDefault,
    select: (res) => res.data,
  });
};

export const useCreateWorkingDays = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: workingDaysApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workingDays'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create working days config:', error.message);
    },
  });
};

export const useUpdateWorkingDays = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateWorkingDaysRequest }) =>
      workingDaysApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workingDays'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update working days config:', error.message);
    },
  });
};

export const useDeleteWorkingDays = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: workingDaysApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workingDays'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete working days config:', error.message);
    },
  });
};

// Holidays
export const useHolidays = (configId: string) => {
  return useQuery({
    queryKey: ['holidays', configId],
    queryFn: () => workingDaysApi.getHolidays(configId),
    select: (res) => res.data,
    enabled: !!configId,
  });
};

export const useAddHoliday = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ configId, data }: { configId: string; data: CreateNonWorkingDayRequest }) =>
      workingDaysApi.addHoliday(configId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['holidays', variables.configId] });
    },
    onError: (error: Error) => {
      console.error('Failed to add holiday:', error.message);
    },
  });
};

export const useRemoveHoliday = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ configId, holidayId }: { configId: string; holidayId: string }) =>
      workingDaysApi.removeHoliday(configId, holidayId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['holidays', variables.configId] });
    },
    onError: (error: Error) => {
      console.error('Failed to remove holiday:', error.message);
    },
  });
};

// Team Availability
export const useTeamAvailability = (teamId: string, start: string, end: string) => {
  return useQuery({
    queryKey: ['teamAvailability', teamId, start, end],
    queryFn: () => workingDaysApi.getTeamAvailability(teamId, start, end),
    select: (res) => res.data,
    enabled: !!teamId && !!start && !!end,
  });
};

export const useSetTeamAvailability = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ teamId, data }: { teamId: string; data: CreateTeamAvailabilityRequest }) =>
      workingDaysApi.setTeamAvailability(teamId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['teamAvailability', variables.teamId] });
    },
    onError: (error: Error) => {
      console.error('Failed to set team availability:', error.message);
    },
  });
};

// Capacity
export const useTeamCapacity = (teamId: string, start: string, end: string) => {
  return useQuery({
    queryKey: ['teamCapacity', teamId, start, end],
    queryFn: () => workingDaysApi.getTeamCapacity(teamId, start, end),
    select: (res) => res.data,
    enabled: !!teamId && !!start && !!end,
  });
};