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
  createPriority: (data: Partial<Priority>) => apiClient.post<Priority>('/api/admin/issues/priorities', data),

  // Statuses
  getStatuses: () => apiClient.get<Status[]>('/api/admin/issues/statuses'),
  createStatus: (data: Partial<Status>) => apiClient.post<Status>('/api/admin/issues/statuses', data),

  // Workflows
  getWorkflows: () => apiClient.get<Workflow[]>('/api/admin/issues/workflows'),
  createWorkflow: (data: Partial<Workflow>) => apiClient.post<Workflow>('/api/admin/issues/workflows', data),
  publishWorkflow: (workflowId: string) => apiClient.post<Workflow>(`/api/admin/issues/workflows/${workflowId}/publish`),

  // Screens
  getScreens: () => apiClient.get<Screen[]>('/api/admin/issues/screens'),
  createScreen: (data: Partial<Screen>) => apiClient.post<Screen>('/api/admin/issues/screens', data),

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

// ==================== Jira User Management API ====================

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

// Jira User Management API
const jiraUserApi = {
  // Users
  getUsers: (params?: { search?: string; status?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: JiraUser[]; totalElements: number; totalPages: number }>('/user-service/rest/admin/1.0/users/search', { params }),
  getUser: (userId: string) => apiClient.get<JiraUser>(`/user-service/rest/admin/1.0/users/${userId}`),
  createUser: (data: { email: string; fullName: string; userName: string; password?: string; sendNotification?: boolean }) =>
    apiClient.post<JiraUser>('/user-service/rest/admin/1.0/users', data),
  deleteUser: (userId: string) => apiClient.delete(`/user-service/rest/admin/1.0/users/${userId}`),

  // Groups
  getGroups: (params?: { search?: string; page?: number; size?: number }) =>
    apiClient.get<{ content: JiraGroup[]; totalElements: number; totalPages: number }>('/user-service/rest/admin/1.0/groups', { params }),
  getGroupByName: (name: string) => apiClient.get<JiraGroup>(`/user-service/rest/admin/1.0/groups/name/${name}`),
  createGroup: (data: { name: string; description?: string }) =>
    apiClient.post<JiraGroup>('/user-service/rest/admin/1.0/groups', data),
  deleteGroup: (groupId: string) => apiClient.delete(`/user-service/rest/admin/1.0/groups/${groupId}`),
};

// Jira User Management Hooks
export const useJiraUsers = (params?: { search?: string; status?: string; page?: number; size?: number }) => {
  return useQuery({
    queryKey: ['jira', 'users', params],
    queryFn: () => jiraUserApi.getUsers(params),
    select: (res) => res.data,
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
      console.error('Failed to create Jira user:', error.message);
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
      console.error('Failed to create Jira group:', error.message);
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
      console.error('Failed to delete Jira group:', error.message);
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
      console.error('Failed to delete Jira user:', error.message);
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
