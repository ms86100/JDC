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
  description: string;
  statusCategory: string;
  iconUrl: string;
  statusColor: string;
  sequence: number;
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
    apiClient.get<{ content: User[]; totalElements: number }>('/admin/users', { params }),
  getUser: (userId: string) => apiClient.get<User>(`/api/admin/users/${userId}`),
  createUser: (data: Partial<User>) => apiClient.post<User>('/admin/users', data),
  updateUser: (userId: string, data: Partial<User>) => apiClient.put<User>(`/api/admin/users/${userId}`, data),
  deleteUser: (userId: string) => apiClient.delete(`/api/admin/users/${userId}`),
  deactivateUser: (userId: string) => apiClient.post(`/api/admin/users/${userId}/deactivate`),
  activateUser: (userId: string) => apiClient.post(`/api/admin/users/${userId}/activate`),

  // Groups
  getGroups: () => apiClient.get<Group[]>('/admin/users/groups'),
  createGroup: (name: string, description?: string) =>
    apiClient.post<Group>('/admin/users/groups', null, { params: { name, description } }),

  // Issue Types
  getIssueTypes: () => apiClient.get<IssueType[]>('/admin/issues/issue-types'),
  createIssueType: (data: Partial<IssueType>) => apiClient.post<IssueType>('/admin/issues/issue-types', data),

  // Priorities
  getPriorities: () => apiClient.get<Priority[]>('/admin/issues/priorities'),
  createPriority: (data: Partial<Priority>) => apiClient.post<Priority>('/admin/issues/priorities', data),

  // Statuses
  getStatuses: () => apiClient.get<Status[]>('/admin/issues/statuses'),
  createStatus: (data: Partial<Status>) => apiClient.post<Status>('/admin/issues/statuses', data),

  // Workflows
  getWorkflows: () => apiClient.get<Workflow[]>('/admin/issues/workflows'),
  createWorkflow: (data: Partial<Workflow>) => apiClient.post<Workflow>('/admin/issues/workflows', data),
  publishWorkflow: (workflowId: string) => apiClient.post<Workflow>(`/api/admin/issues/workflows/${workflowId}/publish`),

  // Screens
  getScreens: () => apiClient.get<Screen[]>('/admin/issues/screens'),
  createScreen: (data: Partial<Screen>) => apiClient.post<Screen>('/admin/issues/screens', data),

  // Cluster Nodes
  getClusterNodes: () => apiClient.get<ClusterNode[]>('/admin/datacenter/cluster/nodes'),
  getClusterHealth: () => apiClient.get('/admin/datacenter/cluster/health'),

  // Scheduled Jobs
  getScheduledJobs: () => apiClient.get<ScheduledJob[]>('/admin/datacenter/jobs'),
  runJob: (jobId: string) => apiClient.post<ScheduledJob>(`/api/admin/datacenter/jobs/${jobId}/run`),
  enableJob: (jobId: string) => apiClient.post<ScheduledJob>(`/api/admin/datacenter/jobs/${jobId}/enable`),
  disableJob: (jobId: string) => apiClient.post<ScheduledJob>(`/api/admin/datacenter/jobs/${jobId}/disable`),

  // System Info
  getSystemInfo: () => apiClient.get('/admin/datacenter/system-info'),

  // Audit Logs
  getAuditLogs: (params?: { userId?: string; category?: string; action?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: AuditLog[]; totalElements: number }>('/admin/audit', { params }),
  getAuditStatistics: () => apiClient.get('/admin/audit/statistics'),

  // Permission Schemes
  getPermissionSchemes: () => apiClient.get<PermissionScheme[]>('/admin/permission-schemes'),
  createPermissionScheme: (data: Partial<PermissionScheme>) => apiClient.post<PermissionScheme>('/admin/permission-schemes', data),
  updatePermissionScheme: (id: string, data: Partial<PermissionScheme>) => apiClient.put<PermissionScheme>(`/api/admin/permission-schemes/${id}`, data),
  deletePermissionScheme: (id: string) => apiClient.delete(`/api/admin/permission-schemes/${id}`),
  copyPermissionScheme: (id: string) => apiClient.post<PermissionScheme>(`/api/admin/permission-schemes/${id}/copy`),

  // Notification Schemes
  getNotificationSchemes: () => apiClient.get<NotificationScheme[]>('/admin/notification-schemes'),
  createNotificationScheme: (data: Partial<NotificationScheme>) => apiClient.post<NotificationScheme>('/admin/notification-schemes', data),
  updateNotificationScheme: (id: string, data: Partial<NotificationScheme>) => apiClient.put<NotificationScheme>(`/api/admin/notification-schemes/${id}`, data),
  deleteNotificationScheme: (id: string) => apiClient.delete(`/api/admin/notification-schemes/${id}`),
  copyNotificationScheme: (id: string) => apiClient.post<NotificationScheme>(`/api/admin/notification-schemes/${id}/copy`),

  // Security Schemes
  getSecuritySchemes: () => apiClient.get<SecurityScheme[]>('/admin/security-schemes'),
  createSecurityScheme: (data: Partial<SecurityScheme>) => apiClient.post<SecurityScheme>('/admin/security-schemes', data),
  updateSecurityScheme: (id: string, data: Partial<SecurityScheme>) => apiClient.put<SecurityScheme>(`/api/admin/security-schemes/${id}`, data),
  deleteSecurityScheme: (id: string) => apiClient.delete(`/api/admin/security-schemes/${id}`),
  copySecurityScheme: (id: string) => apiClient.post<SecurityScheme>(`/api/admin/security-schemes/${id}/copy`),

  // Project Roles
  getProjectRoles: () => apiClient.get<ProjectRole[]>('/admin/project-roles'),
  createProjectRole: (data: Partial<ProjectRole>) => apiClient.post<ProjectRole>('/admin/project-roles', data),
  updateProjectRole: (id: string, data: Partial<ProjectRole>) => apiClient.put<ProjectRole>(`/api/admin/project-roles/${id}`, data),
  deleteProjectRole: (id: string) => apiClient.delete(`/api/admin/project-roles/${id}`),

  // Password Policies
  getPasswordPolicies: () => apiClient.get<PasswordPolicy[]>('/admin/password-policies'),
  createPasswordPolicy: (data: Partial<PasswordPolicy>) => apiClient.post<PasswordPolicy>('/admin/password-policies', data),
  updatePasswordPolicy: (id: string, data: Partial<PasswordPolicy>) => apiClient.put<PasswordPolicy>(`/api/admin/password-policies/${id}`, data),
  deletePasswordPolicy: (id: string) => apiClient.delete(`/api/admin/password-policies/${id}`),
  setDefaultPasswordPolicy: (id: string) => apiClient.post(`/api/admin/password-policies/${id}/default`),

  // Sessions
  getUserSessions: () => apiClient.get<UserSession[]>('/admin/sessions'),
  revokeSession: (sessionId: string) => apiClient.post(`/api/admin/sessions/${sessionId}/revoke`),
  revokeAllSessions: (userId?: string) => apiClient.post(`/api/admin/sessions/revoke-all`, { userId }),
  getSessionPolicy: () => apiClient.get<SessionPolicy>('/admin/sessions/policy'),
  updateSessionPolicy: (policy: SessionPolicy) => apiClient.put<SessionPolicy>('/admin/sessions/policy', policy),
};

// ==================== React Query Hooks ====================

// User Hooks
export const useUsers = (params?: { search?: string; status?: string; role?: string; page?: number; size?: number }) => {
  return useQuery({
    queryKey: ['admin', 'users', params],
    queryFn: () => adminApi.getUsers(params),
    select: (res) => res.data,
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
    select: (res) => res.data,
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
    select: (res) => res.data,
  });
};

// Priority Hooks
export const usePriorities = () => {
  return useQuery({
    queryKey: ['admin', 'priorities'],
    queryFn: () => adminApi.getPriorities(),
    select: (res) => res.data,
  });
};

// Status Hooks
export const useStatuses = () => {
  return useQuery({
    queryKey: ['admin', 'statuses'],
    queryFn: () => adminApi.getStatuses(),
    select: (res) => res.data,
  });
};

// Workflow Hooks
export const useWorkflows = () => {
  return useQuery({
    queryKey: ['admin', 'workflows'],
    queryFn: () => adminApi.getWorkflows(),
    select: (res) => res.data,
  });
};

// Screen Hooks
export const useScreens = () => {
  return useQuery({
    queryKey: ['admin', 'screens'],
    queryFn: () => adminApi.getScreens(),
    select: (res) => res.data,
  });
};

// Cluster Hooks
export const useClusterNodes = () => {
  return useQuery({
    queryKey: ['admin', 'clusterNodes'],
    queryFn: () => adminApi.getClusterNodes(),
    select: (res) => res.data,
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
    select: (res) => res.data,
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
    select: (res) => res.data,
  });
};

// ==================== Systems and Avionics User Management API ====================

export interface JiraUser {
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
  groups: JiraGroupInfo[];
  applications: string[];
  loginInfo: JiraLoginInfo;
}

export interface JiraGroupInfo {
  id: string;
  name: string;
  isAdmin: boolean;
  isJiraSoftware: boolean;
}

export interface JiraLoginInfo {
  loginCount: number;
  lastLogin: string | null;
}

export interface JiraGroup {
  id: string;
  name: string;
  description: string;
  active: boolean;
  createdDate: string;
  isSystem: boolean;
  userCount: number;
  permissionSchemes: JiraSchemeInfo[];
  notificationSchemes: JiraSchemeInfo[];
  securitySchemes: JiraSchemeInfo[];
}

export interface JiraSchemeInfo {
  id: string;
  name: string;
}

// Systems and Avionics User Management API
const jiraUserApi = {
  // Users
  getUsers: (params?: { search?: string; status?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: JiraUser[]; totalElements: number; totalPages: number }>('/user-service/rest/admin/1.0/users/search', { params }),
  getUser: (userId: string) => apiClient.get<JiraUser>(`/user-service/rest/admin/1.0/users/${userId}`),
  createUser: (data: { email: string; fullName: string; userName: string; password?: string; sendNotification?: boolean }) =>
    apiClient.post<JiraUser>('/user-service/rest/admin/1.0/users', data),
  deleteUser: (userId: string) => apiClient.delete(`/user-service/rest/admin/1.0/users/${userId}`),

  updateUser: (userId: string, data: { email?: string; fullName?: string; firstName?: string; lastName?: string; active?: boolean }) =>
    apiClient.put<JiraUser>(`/user-service/rest/admin/1.0/users/${userId}`, data),

  // Groups
  getGroups: (params?: { search?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: JiraGroup[]; totalElements: number; totalPages: number }>('/user-service/rest/admin/1.0/groups', { params }),
  getGroupByName: (name: string) => apiClient.get<JiraGroup>(`/user-service/rest/admin/1.0/groups/name/${name}`),
  createGroup: (data: { name: string; description?: string }) =>
    apiClient.post<JiraGroup>('/user-service/rest/admin/1.0/groups', data),
  updateGroup: (groupId: string, data: { name?: string; description?: string }) =>
    apiClient.put<JiraGroup>(`/user-service/rest/admin/1.0/groups/${groupId}`, data),
  deleteGroup: (groupId: string) => apiClient.delete(`/user-service/rest/admin/1.0/groups/${groupId}`),

  // Group Members
  getGroupMembers: (groupId: string) =>
    apiClient.get<JiraUser[]>(`/user-service/rest/admin/1.0/groups/${groupId}/members`),
  addUserToGroup: (groupId: string, userId: string) =>
    apiClient.post(`/user-service/rest/admin/1.0/groups/${groupId}/members/${userId}`),
  removeUserFromGroup: (groupId: string, userId: string) =>
    apiClient.delete(`/user-service/rest/admin/1.0/groups/${groupId}/members/${userId}`),
};

// Systems and Avionics User Management Hooks
export const useJiraUsers = (params?: { search?: string; status?: string; page?: number; size?: number }) => {
  return useQuery({
    queryKey: ['jira', 'users', params],
    queryFn: () => jiraUserApi.getUsers(params),
    select: (res) => res.data,
  });
};

export const useJiraUser = (userId: string) => {
  return useQuery({
    queryKey: ['jira', 'user', userId],
    queryFn: () => jiraUserApi.getUser(userId),
    select: (res) => res.data,
    enabled: !!userId,
  });
};

export const useJiraGroups = (params?: { search?: string; page?: number; size?: number }) => {
  return useQuery({
    queryKey: ['jira', 'groups', params],
    queryFn: () => jiraUserApi.getGroups(params),
    select: (res) => res.data,
  });
};

export const useJiraGroupByName = (name: string) => {
  return useQuery({
    queryKey: ['jira', 'group', name],
    queryFn: () => jiraUserApi.getGroupByName(name),
    select: (res) => res.data,
    enabled: !!name,
  });
};

export const useCreateJiraUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { email: string; fullName: string; userName: string; password?: string; sendNotification?: boolean }) =>
      jiraUserApi.createUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jira', 'users'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create Systems and Avionics user:', error.message);
    },
  });
};

export const useCreateJiraGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { name: string; description?: string }) => jiraUserApi.createGroup(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jira', 'groups'] });
    },
    onError: (error: Error) => {
      console.error('Failed to create Systems and Avionics group:', error.message);
    },
  });
};

export const useDeleteJiraGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (groupId: string) => jiraUserApi.deleteGroup(groupId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jira', 'groups'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete Systems and Avionics group:', error.message);
    },
  });
};

export const useDeleteJiraUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => jiraUserApi.deleteUser(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jira', 'users'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete Systems and Avionics user:', error.message);
    },
  });
};

export const useUpdateJiraUser = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: { email?: string; fullName?: string; firstName?: string; lastName?: string; active?: boolean } }) =>
      jiraUserApi.updateUser(userId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['jira', 'users'] });
      queryClient.invalidateQueries({ queryKey: ['jira', 'user', variables.userId] });
    },
  });
};

export const useAddUserToGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ groupId, userId }: { groupId: string; userId: string }) =>
      jiraUserApi.addUserToGroup(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jira', 'groupMembers'] });
      queryClient.invalidateQueries({ queryKey: ['jira', 'groups'] });
    },
  });
};

export const useRemoveUserFromGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ groupId, userId }: { groupId: string; userId: string }) =>
      jiraUserApi.removeUserFromGroup(groupId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jira', 'groupMembers'] });
      queryClient.invalidateQueries({ queryKey: ['jira', 'groups'] });
    },
  });
};

export const useJiraGroupMembers = (groupId: string) => {
  return useQuery({
    queryKey: ['jira', 'groupMembers', groupId],
    queryFn: () => jiraUserApi.getGroupMembers(groupId),
    select: (res) => res.data,
    enabled: !!groupId,
  });
};

// ==================== Permission Scheme Hooks ====================

export const usePermissionSchemes = () => {
  return useQuery({
    queryKey: ['admin', 'permissionSchemes'],
    queryFn: () => adminApi.getPermissionSchemes(),
    select: (res) => res.data,
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
    select: (res) => res.data,
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
    select: (res) => res.data,
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
    select: (res) => res.data,
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

// ==================== Password Policy Hooks ====================

export const usePasswordPolicies = () => {
  return useQuery({
    queryKey: ['admin', 'passwordPolicies'],
    queryFn: () => adminApi.getPasswordPolicies(),
    select: (res) => res.data,
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
    select: (res) => res.data,
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
