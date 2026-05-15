import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';

// Types
export interface BoardPermissionResponse {
  id: string;
  boardId: string;
  permissionType: 'VIEW' | 'EDIT' | 'ADMIN' | 'MANAGE_SPRINTS' | 'EDIT_SPRINTS';
  principalType: 'USER' | 'GROUP';
  principalId: string;
  grantedAt: string;
  grantedBy: string | null;
}

export interface ProjectSprintPermissionResponse {
  id: string;
  projectId: string;
  permissionKey: 'MANAGE_SPRINTS' | 'START_STOP_SPRINTS' | 'EDIT_SPRINT_NAME_AND_GOAL';
  principalType: 'USER' | 'GROUP';
  principalId: string;
  createdAt: string;
  createdBy: string | null;
}

export interface CreateBoardPermissionRequest {
  permissionType: 'VIEW' | 'EDIT' | 'ADMIN' | 'MANAGE_SPRINTS' | 'EDIT_SPRINTS';
  principalType: 'USER' | 'GROUP';
  principalId: string;
}

export interface CreateProjectSprintPermissionRequest {
  permissionKey: 'MANAGE_SPRINTS' | 'START_STOP_SPRINTS' | 'EDIT_SPRINT_NAME_AND_GOAL';
  principalType: 'USER' | 'GROUP';
  principalId: string;
}

// API functions
const permissionApi = {
  // Board permissions
  getBoardPermissions: (boardId: string) =>
    apiClient.get<BoardPermissionResponse[]>(`/api/plans/boards/${boardId}/permissions`),
  grantBoardPermission: (boardId: string, data: CreateBoardPermissionRequest, grantedBy?: string) =>
    apiClient.post<BoardPermissionResponse>(
      `/api/plans/boards/${boardId}/permissions${grantedBy ? `?grantedBy=${grantedBy}` : ''}`,
      data
    ),
  revokeBoardPermission: (permissionId: string) =>
    apiClient.delete(`/api/plans/boards/permissions/${permissionId}`),
  checkBoardPermission: (boardId: string, permission: string, userId: string) =>
    apiClient.get<boolean>(`/api/plans/boards/${boardId}/permissions/check?permission=${permission}&userId=${userId}`),
  getEffectivePermissions: (boardId: string, userId: string) =>
    apiClient.get<string[]>(`/api/plans/boards/${boardId}/permissions/effective?userId=${userId}`),
  checkBoardAccess: (boardId: string, userId: string) =>
    apiClient.get<boolean>(`/api/plans/boards/${boardId}/access?userId=${userId}`),

  // Project sprint permissions
  getProjectSprintPermissions: (projectId: string) =>
    apiClient.get<ProjectSprintPermissionResponse[]>(`/api/plans/projects/${projectId}/sprint-permissions`),
  grantProjectSprintPermission: (projectId: string, data: CreateProjectSprintPermissionRequest, grantedBy?: string) =>
    apiClient.post<ProjectSprintPermissionResponse>(
      `/api/plans/projects/${projectId}/sprint-permissions${grantedBy ? `?grantedBy=${grantedBy}` : ''}`,
      data
    ),
  revokeProjectSprintPermission: (permissionId: string) =>
    apiClient.delete(`/api/plans/projects/sprint-permissions/${permissionId}`),
  checkProjectSprintPermission: (projectId: string, permissionKey: string, userId: string) =>
    apiClient.get<boolean>(`/api/plans/projects/${projectId}/sprint-permissions/check?permissionKey=${permissionKey}&userId=${userId}`),

  // Specific permission checks
  canManageSprints: (projectId: string, userId: string) =>
    apiClient.get<boolean>(`/api/plans/projects/${projectId}/can-manage-sprints?userId=${userId}`),
  canStartStopSprints: (projectId: string, userId: string) =>
    apiClient.get<boolean>(`/api/plans/projects/${projectId}/can-start-stop-sprints?userId=${userId}`),
  canEditSprintNameAndGoal: (projectId: string, userId: string) =>
    apiClient.get<boolean>(`/api/plans/projects/${projectId}/can-edit-sprint-name-goal?userId=${userId}`),
};

// Hooks
export const useBoardPermissions = (boardId: string) => {
  return useQuery({
    queryKey: ['boardPermissions', boardId],
    queryFn: () => permissionApi.getBoardPermissions(boardId),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};

export const useGrantBoardPermission = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ boardId, data, grantedBy }: { boardId: string; data: CreateBoardPermissionRequest; grantedBy?: string }) =>
      permissionApi.grantBoardPermission(boardId, data, grantedBy),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['boardPermissions', variables.boardId] });
    },
  });
};

export const useRevokeBoardPermission = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: permissionApi.revokeBoardPermission,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boardPermissions'] });
    },
  });
};

export const useCheckBoardPermission = (boardId: string, permission: string, userId: string) => {
  return useQuery({
    queryKey: ['checkBoardPermission', boardId, permission, userId],
    queryFn: () => permissionApi.checkBoardPermission(boardId, permission, userId),
    select: (res) => res.data,
    enabled: !!boardId && !!permission && !!userId,
  });
};

export const useEffectivePermissions = (boardId: string, userId: string) => {
  return useQuery({
    queryKey: ['effectivePermissions', boardId, userId],
    queryFn: () => permissionApi.getEffectivePermissions(boardId, userId),
    select: (res) => res.data,
    enabled: !!boardId && !!userId,
  });
};

export const useBoardAccess = (boardId: string, userId: string) => {
  return useQuery({
    queryKey: ['boardAccess', boardId, userId],
    queryFn: () => permissionApi.checkBoardAccess(boardId, userId),
    select: (res) => res.data,
    enabled: !!boardId && !!userId,
  });
};

// Project Sprint Permissions
export const useProjectSprintPermissions = (projectId: string) => {
  return useQuery({
    queryKey: ['projectSprintPermissions', projectId],
    queryFn: () => permissionApi.getProjectSprintPermissions(projectId),
    select: (res) => res.data,
    enabled: !!projectId,
  });
};

export const useGrantProjectSprintPermission = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ projectId, data, grantedBy }: { projectId: string; data: CreateProjectSprintPermissionRequest; grantedBy?: string }) =>
      permissionApi.grantProjectSprintPermission(projectId, data, grantedBy),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['projectSprintPermissions', variables.projectId] });
    },
  });
};

export const useRevokeProjectSprintPermission = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: permissionApi.revokeProjectSprintPermission,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projectSprintPermissions'] });
    },
  });
};

export const useCanManageSprints = (projectId: string, userId: string) => {
  return useQuery({
    queryKey: ['canManageSprints', projectId, userId],
    queryFn: () => permissionApi.canManageSprints(projectId, userId),
    select: (res) => res.data,
    enabled: !!projectId && !!userId,
  });
};

export const useCanStartStopSprints = (projectId: string, userId: string) => {
  return useQuery({
    queryKey: ['canStartStopSprints', projectId, userId],
    queryFn: () => permissionApi.canStartStopSprints(projectId, userId),
    select: (res) => res.data,
    enabled: !!projectId && !!userId,
  });
};

export const useCanEditSprintNameAndGoal = (projectId: string, userId: string) => {
  return useQuery({
    queryKey: ['canEditSprintNameAndGoal', projectId, userId],
    queryFn: () => permissionApi.canEditSprintNameAndGoal(projectId, userId),
    select: (res) => res.data,
    enabled: !!projectId && !!userId,
  });
};