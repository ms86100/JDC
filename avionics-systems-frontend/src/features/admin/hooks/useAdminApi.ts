import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';

// ==================== Types ====================

export interface User {
  id: string;
  username: string;
  email: string;
  displayName: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING';
  role: string;
  avatarUrl?: string;
  emailVerified: boolean;
  timezone: string;
  language: string;
  lastLogin?: string;
  createdAt: string;
}

export interface Group {
  id: string;
  groupName: string;
  description: string;
  groupType: string;
  users: User[];
}

export interface IssueType {
  id: string;
  name: string;
  description: string;
  iconUrl: string;
  issueTypeKey: string;
  isSubtask: boolean;
}

export interface Priority {
  id: string;
  name: string;
  description: string;
  iconUrl: string;
  statusColor: string;
  sequence: number;
  isDefault: boolean;
}

export interface Status {
  id: string;
  name: string;
  statusKey?: string;
  description: string;
  statusCategory: string;
  iconUrl: string;
  statusColor: string;
  sequence: number;
  isSystem?: boolean;
}

export interface Workflow {
  id: string;
  name: string;
  description: string;
  isSystem: boolean;
  isActive: boolean;
  isDraft: boolean;
  version: number;
}

export interface Screen {
  id: string;
  name: string;
  description: string;
  tabs: ScreenTab[];
}

export interface ScreenTab {
  tabName: string;
  fieldIds: string[];
}

export interface ScreenScheme {
  id: string;
  name: string;
  description: string;
  screenMappings?: { operationType: string; screenId: string }[];
}

export interface ClusterNode {
  id: string;
  nodeId: string;
  nodeName: string;
  nodeIp: string;
  nodeType: string;
  nodeState: string;
  cpuUsage: number;
  memoryUsage: number;
  startupTime: string;
  lastHeartbeat: string;
}

export interface ScheduledJob {
  id: string;
  jobId: string;
  jobName: string;
  description: string;
  jobType: string;
  triggerType: string;
  cronExpression?: string;
  intervalMs?: number;
  isEnabled: boolean;
  isRunning: boolean;
  lastRunAt?: string;
  nextRunAt?: string;
  lastDurationMs?: number;
}

export interface AuditLog {
  id: string;
  timestamp: string;
  userId: string;
  userName: string;
  userIp: string;
  action: string;
  category: string;
  entityType: string;
  entityId: string;
  entityName: string;
  changedValues?: string;
  details: string;
  result: string;
  severity: string;
}

// ==================== Schemes ====================

export interface PermissionScheme {
  id: string;
  name: string;
  description: string;
  projectCount: number;
  permissionCount: number;
  isDefault: boolean;
}

export interface NotificationScheme {
  id: string;
  name: string;
  description: string;
  projectCount: number;
  eventCount: number;
  isDefault: boolean;
}

export interface SecurityScheme {
  id: string;
  name: string;
  description: string;
  projectCount: number;
  securityLevelCount: number;
  isDefault: boolean;
}

// ==================== Roles ====================

export interface ProjectRole {
  id: string;
  name: string;
  description: string;
  isDefault: boolean;
  memberCount: number;
}

// ==================== Password Policy ====================

export interface PasswordPolicy {
  id: string;
  name: string;
  minLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireDigit: boolean;
  requireSpecial: boolean;
  maxAge: number; // days, 0 = never
  preventReuse: number; // count, 0 = allow reuse
  isDefault: boolean;
}

// ==================== Sessions ====================

export interface UserSession {
  id: string;
  userId: string;
  userName: string;
  displayName: string;
  ipAddress: string;
  userAgent: string;
  loginTime: string;
  lastActive: string;
  expiresAt: string;
  isCurrent: boolean;
}

export interface SessionPolicy {
  sessionTimeout: number; // minutes
  maxSessions: number; // 0 = unlimited
  allowMultipleSessions: boolean;
}

// ==================== API Functions ====================

const adminApi = {
  // Users
  getUsers: (params?: { search?: string; status?: string; role?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: User[]; totalElements: number }>('/api/admin/users', { params }),
  getUser: (userId: string) => apiClient.get<User>(`/api/admin/users/${userId}`),
  createUser: (data: Partial<User>) => apiClient.post<User>('/api/admin/users', data),
  updateUser: (userId: string, data: Partial<User>) => apiClient.put<User>(`/api/admin/users/${userId}`, data),
  deleteUser: (userId: string) => apiClient.delete(`/api/admin/users/${userId}`),
  deactivateUser: (userId: string) => apiClient.post(`/api/admin/users/${userId}/deactivate`),
  activateUser: (userId: string) => apiClient.post(`/api/admin/users/${userId}/activate`),

  // Groups
  getGroups: () => apiClient.get<Group[]>('/api/admin/users/groups'),
  createGroup: (name: string, description?: string) =>
    apiClient.post<Group>('/api/admin/users/groups', null, { params: { name, description } }),

  // Issue Types
  getIssueTypes: () => apiClient.get<IssueType[]>('/api/admin/issues/issue-types'),
  createIssueType: (data: Partial<IssueType>) => apiClient.post<IssueType>('/api/admin/issues/issue-types', data),

  // Priorities
  getPriorities: () => apiClient.get<Priority[]>('/api/admin/issues/priorities'),
  createPriority: (data: Partial<Priority>) => apiClient.post<Priority>('/api/admin/master-data/priorities', data),
  updatePriority: (id: string, data: Partial<Priority>) => apiClient.put<Priority>(`/api/admin/master-data/priorities/${id}`, data),
  deletePriority: (id: string) => apiClient.delete(`/api/admin/master-data/priorities/${id}`),

  // Statuses
  getStatuses: () => apiClient.get<Status[]>('/api/admin/issues/statuses'),
  createStatus: (data: Partial<Status>) => apiClient.post<Status>('/api/admin/master-data/statuses', data),
  updateStatus: (id: string, data: Partial<Status>) => apiClient.put<Status>(`/api/admin/master-data/statuses/${id}`, data),
  deleteStatus: (id: string) => apiClient.delete(`/api/admin/master-data/statuses/${id}`),

  // Workflows
  getWorkflows: () => apiClient.get<Workflow[]>('/api/admin/issues/workflows'),
  createWorkflow: (data: Partial<Workflow>) => apiClient.post<Workflow>('/api/admin/issues/workflows', data),
  publishWorkflow: (workflowId: string) => apiClient.post<Workflow>(`/api/admin/issues/workflows/${workflowId}/publish`),

  // Screens
  getScreens: () => apiClient.get<Screen[]>('/api/admin/issues/screens'),
  createScreen: (data: Partial<Screen>) => apiClient.post<Screen>('/api/admin/issues/screens', data),
  updateScreen: (id: string, data: Partial<Screen>) => apiClient.put<Screen>(`/api/admin/issues/screens/${id}`, data),
  deleteScreen: (id: string) => apiClient.delete(`/api/admin/issues/screens/${id}`),

  // Screen Schemes
  getScreenSchemes: () => apiClient.get<ScreenScheme[]>('/api/admin/screen-schemes'),
  createScreenScheme: (data: Partial<ScreenScheme>) => apiClient.post<ScreenScheme>('/api/admin/screen-schemes', data),
  updateScreenScheme: (id: string, data: Partial<ScreenScheme>) => apiClient.put<ScreenScheme>(`/api/admin/screen-schemes/${id}`, data),
  deleteScreenScheme: (id: string) => apiClient.delete(`/api/admin/screen-schemes/${id}`),

  // Cluster Nodes
  getClusterNodes: () => apiClient.get<ClusterNode[]>('/api/admin/datacenter/cluster/nodes'),
  getClusterHealth: () => apiClient.get('/api/admin/datacenter/cluster/health'),

  // Scheduled Jobs
  getScheduledJobs: () => apiClient.get<ScheduledJob[]>('/api/admin/datacenter/jobs'),
  runJob: (jobId: string) => apiClient.post<ScheduledJob>(`/api/admin/datacenter/jobs/${jobId}/run`),
  enableJob: (jobId: string) => apiClient.post<ScheduledJob>(`/api/admin/datacenter/jobs/${jobId}/enable`),
  disableJob: (jobId: string) => apiClient.post<ScheduledJob>(`/api/admin/datacenter/jobs/${jobId}/disable`),

  // System Info
  getSystemInfo: () => apiClient.get('/api/admin/datacenter/system-info'),

  // Audit Logs
  getAuditLogs: (params?: { userId?: string; category?: string; action?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: AuditLog[]; totalElements: number }>('/api/admin/audit', { params }),
  getAuditStatistics: () => apiClient.get('/api/admin/audit/statistics'),

  // Permission Schemes
  getPermissionSchemes: () => apiClient.get<PermissionScheme[]>('/api/admin/permission-schemes'),
  createPermissionScheme: (data: Partial<PermissionScheme>) => apiClient.post<PermissionScheme>('/api/admin/permission-schemes', data),
  updatePermissionScheme: (id: string, data: Partial<PermissionScheme>) => apiClient.put<PermissionScheme>(`/api/admin/permission-schemes/${id}`, data),
  deletePermissionScheme: (id: string) => apiClient.delete(`/api/admin/permission-schemes/${id}`),
  copyPermissionScheme: (id: string) => apiClient.post<PermissionScheme>(`/api/admin/permission-schemes/${id}/copy`),

  // Notification Schemes
  getNotificationSchemes: () => apiClient.get<NotificationScheme[]>('/api/admin/notification-schemes'),
  createNotificationScheme: (data: Partial<NotificationScheme>) => apiClient.post<NotificationScheme>('/api/admin/notification-schemes', data),
  updateNotificationScheme: (id: string, data: Partial<NotificationScheme>) => apiClient.put<NotificationScheme>(`/api/admin/notification-schemes/${id}`, data),
  deleteNotificationScheme: (id: string) => apiClient.delete(`/api/admin/notification-schemes/${id}`),
  copyNotificationScheme: (id: string) => apiClient.post<NotificationScheme>(`/api/admin/notification-schemes/${id}/copy`),

  // Security Schemes
  getSecuritySchemes: () => apiClient.get<SecurityScheme[]>('/api/admin/security-schemes'),
  createSecurityScheme: (data: Partial<SecurityScheme>) => apiClient.post<SecurityScheme>('/api/admin/security-schemes', data),
  updateSecurityScheme: (id: string, data: Partial<SecurityScheme>) => apiClient.put<SecurityScheme>(`/api/admin/security-schemes/${id}`, data),
  deleteSecurityScheme: (id: string) => apiClient.delete(`/api/admin/security-schemes/${id}`),
  copySecurityScheme: (id: string) => apiClient.post<SecurityScheme>(`/api/admin/security-schemes/${id}/copy`),

  // Project Roles (under UserManagementController: /api/admin/users/project-roles)
  getProjectRoles: () => apiClient.get<ProjectRole[]>('/api/admin/users/project-roles'),
  createProjectRole: (data: Partial<ProjectRole>) => apiClient.post<ProjectRole>('/api/admin/users/project-roles', null, { params: { name: data.name, description: data.description, roleType: data.roleType ?? 'PROJECT' } }),
  updateProjectRole: (id: string, data: Partial<ProjectRole>) => apiClient.put<ProjectRole>(`/api/admin/users/project-roles/${id}`, data),
  deleteProjectRole: (id: string) => apiClient.delete(`/api/admin/users/project-roles/${id}`),

  // Password Policies (under UserManagementController: /api/admin/users/password-policy)
  getPasswordPolicies: () => apiClient.get<PasswordPolicy[]>('/api/admin/users/password-policy'),
  createPasswordPolicy: (data: Partial<PasswordPolicy>) => apiClient.post<PasswordPolicy>('/api/admin/users/password-policy', data),
  updatePasswordPolicy: (id: string, data: Partial<PasswordPolicy>) => apiClient.put<PasswordPolicy>(`/api/admin/users/password-policy/${id}`, data),
  deletePasswordPolicy: (id: string) => apiClient.delete(`/api/admin/users/password-policy/${id}`),
  setDefaultPasswordPolicy: (id: string) => apiClient.post(`/api/admin/users/password-policy/${id}/default`),

  // Link Types
  getLinkTypes: () => apiClient.get<LinkType[]>('/api/admin/master-data/link-types'),
  createLinkType: (data: Partial<LinkType>) => apiClient.post<LinkType>('/api/admin/master-data/link-types', data),
  updateLinkType: (id: string, data: Partial<LinkType>) => apiClient.put<LinkType>(`/api/admin/master-data/link-types/${id}`, data),
  deleteLinkType: (id: string) => apiClient.delete(`/api/admin/master-data/link-types/${id}`),

  // Quick Filter Presets
  getQuickFilterPresets: () => apiClient.get<QuickFilterPreset[]>('/api/admin/master-data/quick-filters'),
  createQuickFilterPreset: (data: Partial<QuickFilterPreset>) => apiClient.post<QuickFilterPreset>('/api/admin/master-data/quick-filters', data),
  updateQuickFilterPreset: (id: string, data: Partial<QuickFilterPreset>) => apiClient.put<QuickFilterPreset>(`/api/admin/master-data/quick-filters/${id}`, data),
  deleteQuickFilterPreset: (id: string) => apiClient.delete(`/api/admin/master-data/quick-filters/${id}`),

  // Notification Events
  getNotificationEvents: () => apiClient.get<NotificationEvent[]>('/api/admin/master-data/notification-events'),
  createNotificationEvent: (data: Partial<NotificationEvent>) => apiClient.post<NotificationEvent>('/api/admin/master-data/notification-events', data),
  updateNotificationEvent: (id: string, data: Partial<NotificationEvent>) => apiClient.put<NotificationEvent>(`/api/admin/master-data/notification-events/${id}`, data),
  deleteNotificationEvent: (id: string) => apiClient.delete(`/api/admin/master-data/notification-events/${id}`),

  // Sessions (no backend endpoint yet — return empty for now)
  getUserSessions: () => Promise.resolve({ data: [] as UserSession[] }),
  revokeSession: (_sessionId: string) => Promise.resolve({ data: {} }),
  revokeAllSessions: (_userId?: string) => Promise.resolve({ data: {} }),
  getSessionPolicy: () => Promise.resolve({ data: { sessionTimeout: 30, maxSessions: 5, allowMultipleSessions: true } as SessionPolicy }),
  updateSessionPolicy: (policy: SessionPolicy) => Promise.resolve({ data: policy }),
};

// ==================== React Query Hooks ====================

// User Hooks
export const useUsers = (params?: { search?: string; status?: string; role?: string; page?: number; size?: number }) => {
  return useQuery({
    queryKey: ['admin', 'users', params],
    queryFn: () => adminApi.getUsers(params),
    select: (res) => res.data ?? { content: [], totalElements: 0, totalPages: 0 },
  });
};

export const useUser = (userId: string) => {
  return useQuery({
    queryKey: ['admin', 'user', userId],
    queryFn: () => adminApi.getUser(userId),
    select: (res) => res.data,
    enabled: !!userId,
  });
};

export const useCreateUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<User>) => adminApi.createUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create user:', error.message);
    },
  });
};

export const useUpdateUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: Partial<User> }) =>
      adminApi.updateUser(userId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'user', variables.userId] });
    },
    onError: (error: Error) => {
      console.error('Failed to update user:', error.message);
    },
  });
};

export const useDeleteUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => adminApi.deleteUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete user:', error.message);
    },
  });
};

// Group Hooks
export const useGroups = () => {
  return useQuery({
    queryKey: ['admin', 'groups'],
    queryFn: () => adminApi.getGroups(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ name, description }: { name: string; description?: string }) =>
      adminApi.createGroup(name, description),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'groups'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create group:', error.message);
    },
  });
};

// Issue Type Hooks
export const useIssueTypes = () => {
  return useQuery({
    queryKey: ['admin', 'issueTypes'],
    queryFn: () => adminApi.getIssueTypes(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

// Priority Hooks
export const usePriorities = () => {
  return useQuery({
    queryKey: ['admin', 'priorities'],
    queryFn: () => adminApi.getPriorities(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreatePriority = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Priority>) => adminApi.createPriority(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'priorities'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create priority:', error.message);
    },
  });
};

export const useUpdatePriority = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Priority> }) =>
      adminApi.updatePriority(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'priorities'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update priority:', error.message);
    },
  });
};

export const useDeletePriority = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deletePriority(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'priorities'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete priority:', error.message);
    },
  });
};

// Status Hooks
export const useStatuses = () => {
  return useQuery({
    queryKey: ['admin', 'statuses'],
    queryFn: () => adminApi.getStatuses(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateStatus = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Status>) => adminApi.createStatus(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'statuses'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create status:', error.message);
    },
  });
};

export const useUpdateStatus = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Status> }) =>
      adminApi.updateStatus(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'statuses'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update status:', error.message);
    },
  });
};

export const useDeleteStatus = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteStatus(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'statuses'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete status:', error.message);
    },
  });
};

// Workflow Hooks
export const useWorkflows = () => {
  return useQuery({
    queryKey: ['admin', 'workflows'],
    queryFn: () => adminApi.getWorkflows(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

// Screen Hooks
export const useScreens = () => {
  return useQuery({
    queryKey: ['admin', 'screens'],
    queryFn: () => adminApi.getScreens(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateScreen = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Screen>) => adminApi.createScreen(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'screens'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create screen:', error.message);
    },
  });
};

export const useUpdateScreen = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Screen> }) =>
      adminApi.updateScreen(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'screens'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update screen:', error.message);
    },
  });
};

export const useDeleteScreen = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteScreen(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'screens'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete screen:', error.message);
    },
  });
};

// Screen Scheme Hooks
export const useScreenSchemes = () => {
  return useQuery({
    queryKey: ['admin', 'screenSchemes'],
    queryFn: () => adminApi.getScreenSchemes(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateScreenScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<ScreenScheme>) => adminApi.createScreenScheme(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'screenSchemes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create screen scheme:', error.message);
    },
  });
};

export const useUpdateScreenScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<ScreenScheme> }) =>
      adminApi.updateScreenScheme(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'screenSchemes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update screen scheme:', error.message);
    },
  });
};

export const useDeleteScreenScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteScreenScheme(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'screenSchemes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete screen scheme:', error.message);
    },
  });
};

// Cluster Hooks
export const useClusterNodes = () => {
  return useQuery({
    queryKey: ['admin', 'clusterNodes'],
    queryFn: () => adminApi.getClusterNodes(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useClusterHealth = () => {
  return useQuery({
    queryKey: ['admin', 'clusterHealth'],
    queryFn: () => adminApi.getClusterHealth(),
    select: (res) => res.data,
  });
};

// Scheduled Job Hooks
export const useScheduledJobs = () => {
  return useQuery({
    queryKey: ['admin', 'scheduledJobs'],
    queryFn: () => adminApi.getScheduledJobs(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useRunJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (jobId: string) => adminApi.runJob(jobId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'scheduledJobs'] });
    },
    onError: (error: Error) => {
      console.error('Failed to run job:', error.message);
    },
  });
};

export const useToggleJob = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ jobId, enabled }: { jobId: string; enabled: boolean }) =>
      enabled ? adminApi.enableJob(jobId) : adminApi.disableJob(jobId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'scheduledJobs'] });
    },
    onError: (error: Error) => {
      console.error('Failed to toggle job:', error.message);
    },
  });
};

// System Info Hooks
export const useSystemInfo = () => {
  return useQuery({
    queryKey: ['admin', 'systemInfo'],
    queryFn: () => adminApi.getSystemInfo(),
    select: (res) => res.data,
  });
};

// Audit Log Hooks
export const useAuditLogs = (params?: { userId?: string; category?: string; action?: string; page?: number; size?: number }) => {
  return useQuery({
    queryKey: ['admin', 'auditLogs', params],
    queryFn: () => adminApi.getAuditLogs(params),
    select: (res) => res.data ?? { content: [], totalElements: 0 },
  });
};

// ==================== Systems and Avionics User Management API ====================

export interface AviSysUser {
  id: string;
  userName: string;
  emailAddress: string;
  displayName: string;
  firstName: string;
  lastName: string;
  active: boolean;
  createdDate: string;
  updatedDate: string;
  directoryId: string;
  directoryName: string;
  groups: AviSysGroupInfo[];
  applications: string[];
  loginInfo: AviSysLoginInfo;
}

export interface AviSysGroupInfo {
  id: string;
  name: string;
  isAdmin: boolean;
  isAviSysSoftware: boolean;
}

export interface AviSysLoginInfo {
  loginCount: number;
  lastLogin: string | null;
}

export interface AviSysGroup {
  id: string;
  name: string;
  description: string;
  active: boolean;
  createdDate: string;
  isSystem: boolean;
  userCount: number;
  permissionSchemes: AviSysSchemeInfo[];
  notificationSchemes: AviSysSchemeInfo[];
  securitySchemes: AviSysSchemeInfo[];
}

export interface AviSysSchemeInfo {
  id: string;
  name: string;
}

// Systems and Avionics User Management API
const avisysUserApi = {
  // Users
  getUsers: (params?: { search?: string; status?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: AviSysUser[]; totalElements: number; totalPages: number }>('/user-service/rest/admin/1.0/users/search', { params }),
  getUser: (userId: string) => apiClient.get<AviSysUser>(`/user-service/rest/admin/1.0/users/${userId}`),
  createUser: (data: { email: string; fullName: string; userName: string; password?: string; sendNotification?: boolean }) =>
    apiClient.post<AviSysUser>('/user-service/rest/admin/1.0/users', data),
  deleteUser: (userId: string) => apiClient.delete(`/user-service/rest/admin/1.0/users/${userId}`),

  updateUser: (userId: string, data: { email?: string; fullName?: string; firstName?: string; lastName?: string; active?: boolean }) =>
    apiClient.put<AviSysUser>(`/user-service/rest/admin/1.0/users/${userId}`, data),

  // Groups
  getGroups: (params?: { search?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: AviSysGroup[]; totalElements: number; totalPages: number }>('/user-service/rest/admin/1.0/groups', { params }),
  getGroupByName: (name: string) => apiClient.get<AviSysGroup>(`/user-service/rest/admin/1.0/groups/name/${name}`),
  createGroup: (data: { name: string; description?: string }) =>
    apiClient.post<AviSysGroup>('/user-service/rest/admin/1.0/groups', data),
  updateGroup: (groupId: string, data: { name?: string; description?: string }) =>
    apiClient.put<AviSysGroup>(`/user-service/rest/admin/1.0/groups/${groupId}`, data),
  deleteGroup: (groupId: string) => apiClient.delete(`/user-service/rest/admin/1.0/groups/${groupId}`),

  // Group Members
  getGroupMembers: (groupId: string) =>
    apiClient.get<AviSysUser[]>(`/user-service/rest/admin/1.0/groups/${groupId}/members`),
  addUserToGroup: (groupId: string, userId: string) =>
    apiClient.post(`/user-service/rest/admin/1.0/groups/${groupId}/members/${userId}`),
  removeUserFromGroup: (groupId: string, userId: string) =>
    apiClient.delete(`/user-service/rest/admin/1.0/groups/${groupId}/members/${userId}`),
};

// Systems and Avionics User Management Hooks
export const useAviSysUsers = (params?: { search?: string; status?: string; page?: number; size?: number }) => {
  return useQuery({
    queryKey: ['avisys', 'users', params],
    queryFn: () => avisysUserApi.getUsers(params),
    select: (res) => res.data,
  });
};

export const useAviSysUser = (userId: string) => {
  return useQuery({
    queryKey: ['avisys', 'user', userId],
    queryFn: () => avisysUserApi.getUser(userId),
    select: (res) => res.data,
    enabled: !!userId,
  });
};

export const useAviSysGroups = (params?: { search?: string; page?: number; size?: number }) => {
  return useQuery({
    queryKey: ['avisys', 'groups', params],
    queryFn: () => avisysUserApi.getGroups(params),
    select: (res) => res.data,
  });
};

export const useAviSysGroupByName = (name: string) => {
  return useQuery({
    queryKey: ['avisys', 'group', name],
    queryFn: () => avisysUserApi.getGroupByName(name),
    select: (res) => res.data,
    enabled: !!name,
  });
};

export const useCreateAviSysUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { email: string; fullName: string; userName: string; password?: string; sendNotification?: boolean }) =>
      avisysUserApi.createUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['avisys', 'users'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create Systems and Avionics user:', error.message);
    },
  });
};

export const useCreateAviSysGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { name: string; description?: string }) => avisysUserApi.createGroup(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['avisys', 'groups'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create Systems and Avionics group:', error.message);
    },
  });
};

export const useDeleteAviSysGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: string) => avisysUserApi.deleteGroup(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['avisys', 'groups'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete Systems and Avionics group:', error.message);
    },
  });
};

export const useDeleteAviSysUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => avisysUserApi.deleteUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['avisys', 'users'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete Systems and Avionics user:', error.message);
    },
  });
};

export const useUpdateAviSysUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: { email?: string; fullName?: string; firstName?: string; lastName?: string; active?: boolean } }) =>
      avisysUserApi.updateUser(userId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['avisys', 'users'] });
      queryClient.invalidateQueries({ queryKey: ['avisys', 'user', variables.userId] });
    },
  });
};

export const useAddUserToGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ groupId, userId }: { groupId: string; userId: string }) =>
      avisysUserApi.addUserToGroup(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['avisys', 'groupMembers'] });
      queryClient.invalidateQueries({ queryKey: ['avisys', 'groups'] });
    },
  });
};

export const useRemoveUserFromGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ groupId, userId }: { groupId: string; userId: string }) =>
      avisysUserApi.removeUserFromGroup(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['avisys', 'groupMembers'] });
      queryClient.invalidateQueries({ queryKey: ['avisys', 'groups'] });
    },
  });
};

export const useAviSysGroupMembers = (groupId: string) => {
  return useQuery({
    queryKey: ['avisys', 'groupMembers', groupId],
    queryFn: () => avisysUserApi.getGroupMembers(groupId),
    select: (res) => Array.isArray(res.data) ? res.data : [],
    enabled: !!groupId,
  });
};

// ==================== Permission Scheme Hooks ====================

export const usePermissionSchemes = () => {
  return useQuery({
    queryKey: ['admin', 'permissionSchemes'],
    queryFn: () => adminApi.getPermissionSchemes(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreatePermissionScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<PermissionScheme>) => adminApi.createPermissionScheme(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'permissionSchemes'] });
    },
  });
};

export const useUpdatePermissionScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<PermissionScheme> }) =>
      adminApi.updatePermissionScheme(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'permissionSchemes'] });
    },
  });
};

export const useDeletePermissionScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deletePermissionScheme(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'permissionSchemes'] });
    },
  });
};

export const useCopyPermissionScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.copyPermissionScheme(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'permissionSchemes'] });
    },
  });
};

// ==================== Notification Scheme Hooks ====================

export const useNotificationSchemes = () => {
  return useQuery({
    queryKey: ['admin', 'notificationSchemes'],
    queryFn: () => adminApi.getNotificationSchemes(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateNotificationScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<NotificationScheme>) => adminApi.createNotificationScheme(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notificationSchemes'] });
    },
  });
};

export const useUpdateNotificationScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<NotificationScheme> }) =>
      adminApi.updateNotificationScheme(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notificationSchemes'] });
    },
  });
};

export const useDeleteNotificationScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteNotificationScheme(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notificationSchemes'] });
    },
  });
};

export const useCopyNotificationScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.copyNotificationScheme(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notificationSchemes'] });
    },
  });
};

// ==================== Security Scheme Hooks ====================

export const useSecuritySchemes = () => {
  return useQuery({
    queryKey: ['admin', 'securitySchemes'],
    queryFn: () => adminApi.getSecuritySchemes(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateSecurityScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<SecurityScheme>) => adminApi.createSecurityScheme(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'securitySchemes'] });
    },
  });
};

export const useUpdateSecurityScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<SecurityScheme> }) =>
      adminApi.updateSecurityScheme(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'securitySchemes'] });
    },
  });
};

export const useDeleteSecurityScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteSecurityScheme(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'securitySchemes'] });
    },
  });
};

export const useCopySecurityScheme = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.copySecurityScheme(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'securitySchemes'] });
    },
  });
};

// ==================== Project Role Hooks ====================

export const useProjectRoles = () => {
  return useQuery({
    queryKey: ['admin', 'projectRoles'],
    queryFn: () => adminApi.getProjectRoles(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateProjectRole = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<ProjectRole>) => adminApi.createProjectRole(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'projectRoles'] });
    },
  });
};

export const useUpdateProjectRole = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<ProjectRole> }) =>
      adminApi.updateProjectRole(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'projectRoles'] });
    },
  });
};

export const useDeleteProjectRole = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteProjectRole(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'projectRoles'] });
    },
  });
};

// ==================== Link Type ====================

export interface LinkType {
  id: string;
  linkKey: string;
  outwardName: string;
  inwardName: string;
  description?: string;
  isSystem: boolean;
  isActive: boolean;
  sortOrder: number;
}

// ==================== Link Type Hooks ====================

export const useLinkTypes = () => {
  return useQuery({
    queryKey: ['admin', 'linkTypes'],
    queryFn: () => adminApi.getLinkTypes(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateLinkType = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<LinkType>) => adminApi.createLinkType(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'linkTypes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create link type:', error.message);
    },
  });
};

export const useUpdateLinkType = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<LinkType> }) =>
      adminApi.updateLinkType(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'linkTypes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update link type:', error.message);
    },
  });
};

export const useDeleteLinkType = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteLinkType(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'linkTypes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete link type:', error.message);
    },
  });
};

// ==================== Notification Events ====================

export interface NotificationEvent {
  id: string;
  eventKey: string;
  displayName: string;
  description: string;
  category: string;
  isSystem: boolean;
  isActive: boolean;
}

// ==================== Quick Filter Presets ====================

export interface QuickFilterPreset {
  id: string;
  filterName: string;
  jqlQuery: string;
  icon: string;
  sortOrder: number;
  isSystem: boolean;
  isActive: boolean;
}

// ==================== Board Types ====================

export interface BoardColumnTemplate {
  id: string;
  boardTypeId: string;
  columnName: string;
  statusCategory: string;
  color: string;
  wipLimit: number | null;
  sortOrder: number;
}

export interface BoardType {
  id: string;
  typeKey: string;
  displayName: string;
  description: string;
  isActive: boolean;
  columnTemplates: BoardColumnTemplate[];
}

const boardTypeApi = {
  getAll: () => apiClient.get<BoardType[]>('/api/admin/master-data/board-types'),
  create: (data: Partial<BoardType>) => apiClient.post<BoardType>('/api/admin/master-data/board-types', data),
  update: (id: string, data: Partial<BoardType>) => apiClient.put<BoardType>(`/api/admin/master-data/board-types/${id}`, data),
  delete: (id: string) => apiClient.delete(`/api/admin/master-data/board-types/${id}`),
};

export const useBoardTypes = () => {
  return useQuery({
    queryKey: ['admin', 'boardTypes'],
    queryFn: () => boardTypeApi.getAll(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateBoardType = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<BoardType>) => boardTypeApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'boardTypes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create board type:', error.message);
    },
  });
};

export const useUpdateBoardType = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<BoardType> }) =>
      boardTypeApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'boardTypes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update board type:', error.message);
    },
  });
};

export const useDeleteBoardType = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => boardTypeApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'boardTypes'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete board type:', error.message);
    },
  });
};

// ==================== Quick Filter Preset Hooks ====================

export const useQuickFilterPresets = () => {
  return useQuery({
    queryKey: ['admin', 'quickFilterPresets'],
    queryFn: () => adminApi.getQuickFilterPresets(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateQuickFilterPreset = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<QuickFilterPreset>) => adminApi.createQuickFilterPreset(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'quickFilterPresets'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create quick filter preset:', error.message);
    },
  });
};

export const useUpdateQuickFilterPreset = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<QuickFilterPreset> }) =>
      adminApi.updateQuickFilterPreset(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'quickFilterPresets'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update quick filter preset:', error.message);
    },
  });
};

export const useDeleteQuickFilterPreset = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteQuickFilterPreset(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'quickFilterPresets'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete quick filter preset:', error.message);
    },
  });
};

// ==================== Notification Event Hooks ====================

export const useNotificationEvents = () => {
  return useQuery({
    queryKey: ['admin', 'notificationEvents'],
    queryFn: () => adminApi.getNotificationEvents(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateNotificationEvent = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<NotificationEvent>) => adminApi.createNotificationEvent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notificationEvents'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create notification event:', error.message);
    },
  });
};

export const useUpdateNotificationEvent = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<NotificationEvent> }) =>
      adminApi.updateNotificationEvent(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notificationEvents'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update notification event:', error.message);
    },
  });
};

export const useDeleteNotificationEvent = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteNotificationEvent(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'notificationEvents'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete notification event:', error.message);
    },
  });
};

// ==================== Resolution Hooks ====================

export type { Resolution } from '../../../api/issueApi';
import { resolutionApi, type Resolution as ResolutionType } from '../../../api/issueApi';

export const useResolutions = () => {
  return useQuery({
    queryKey: ['admin', 'resolutions'],
    queryFn: () => resolutionApi.getAll(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreateResolution = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<ResolutionType>) => resolutionApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'resolutions'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create resolution:', error.message);
    },
  });
};

export const useUpdateResolution = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<ResolutionType> }) =>
      resolutionApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'resolutions'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update resolution:', error.message);
    },
  });
};

export const useDeleteResolution = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => resolutionApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'resolutions'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete resolution:', error.message);
    },
  });
};

// ==================== Password Policy Hooks ====================

export const usePasswordPolicies = () => {
  return useQuery({
    queryKey: ['admin', 'passwordPolicies'],
    queryFn: () => adminApi.getPasswordPolicies(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useCreatePasswordPolicy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<PasswordPolicy>) => adminApi.createPasswordPolicy(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'passwordPolicies'] });
    },
  });
};

export const useUpdatePasswordPolicy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<PasswordPolicy> }) =>
      adminApi.updatePasswordPolicy(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'passwordPolicies'] });
    },
  });
};

export const useDeletePasswordPolicy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deletePasswordPolicy(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'passwordPolicies'] });
    },
  });
};

export const useSetDefaultPasswordPolicy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.setDefaultPasswordPolicy(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'passwordPolicies'] });
    },
  });
};

// ==================== Session Hooks ====================

export const useUserSessions = () => {
  return useQuery({
    queryKey: ['admin', 'userSessions'],
    queryFn: () => adminApi.getUserSessions(),
    select: (res) => Array.isArray(res.data) ? res.data : [],
  });
};

export const useRevokeSession = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (sessionId: string) => adminApi.revokeSession(sessionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'userSessions'] });
    },
  });
};

export const useRevokeAllSessions = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId?: string) => adminApi.revokeAllSessions(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'userSessions'] });
    },
  });
};

export const useSessionPolicy = () => {
  return useQuery({
    queryKey: ['admin', 'sessionPolicy'],
    queryFn: () => adminApi.getSessionPolicy(),
    select: (res) => res.data,
  });
};

export const useUpdateSessionPolicy = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (policy: SessionPolicy) => adminApi.updateSessionPolicy(policy),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'sessionPolicy'] });
    },
  });
};

export const useAuditStatistics = () => {
  return useQuery({
    queryKey: ['admin', 'auditStatistics'],
    queryFn: () => adminApi.getAuditStatistics(),
    select: (res) => res.data,
  });
};
